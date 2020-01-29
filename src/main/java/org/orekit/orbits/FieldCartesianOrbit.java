/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.orbits;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/** This class holds Cartesian orbital parameters.

 * <p>
 * The parameters used internally are the Cartesian coordinates:
 *   <ul>
 *     <li>x</li>
 *     <li>y</li>
 *     <li>z</li>
 *     <li>xDot</li>
 *     <li>yDot</li>
 *     <li>zDot</li>
 *   </ul>
 * contained in {@link PVCoordinates}.

 * <p>
 * Note that the implementation of this class delegates all non-Cartesian related
 * computations ({@link #getA()}, {@link #getEquinoctialEx()}, ...) to an underlying
 * instance of the {@link EquinoctialOrbit} class. This implies that using this class
 * only for analytical computations which are always based on non-Cartesian
 * parameters is perfectly possible but somewhat sub-optimal.
 * </p>
 * <p>
 * The instance <code>CartesianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @since 9.0
 */
public class FieldCartesianOrbit<T extends RealFieldElement<T>> extends FieldOrbit<T> {

    /** Factory for first time derivatives. */
    private static final Map<Field<? extends RealFieldElement<?>>, FDSFactory<? extends RealFieldElement<?>>> FACTORIES =
                    new HashMap<>();

    /** Indicator for non-Keplerian acceleration. */
    private final transient boolean hasNonKeplerianAcceleration;

    /** Underlying equinoctial orbit to which high-level methods are delegated. */
    private transient FieldEquinoctialOrbit<T> equinoctial;

    /** Field used by this class.*/
    private final Field<T> field;

    /** Zero. (could be usefull)*/
    private final T zero;

    /** One. (could be useful)*/
    private final T one;

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position, velocity and acceleration of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCartesianOrbit(final TimeStampedFieldPVCoordinates<T> pvaCoordinates,
                               final Frame frame, final T mu)
        throws IllegalArgumentException {
        super(pvaCoordinates, frame, mu);
        hasNonKeplerianAcceleration = hasNonKeplerianAcceleration(pvaCoordinates, mu);
        equinoctial = null;
        field = pvaCoordinates.getPosition().getX().getField();
        zero = field.getZero();
        one = field.getOne();

        if (!FACTORIES.containsKey(field)) {
            FACTORIES.put(field, new FDSFactory<>(field, 1, 1));
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position and velocity of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCartesianOrbit(final FieldPVCoordinates<T> pvaCoordinates, final Frame frame,
                               final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<>(date, pvaCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldCartesianOrbit(final FieldOrbit<T> op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getMu());
        hasNonKeplerianAcceleration = op.hasDerivatives();
        if (op instanceof FieldEquinoctialOrbit) {
            equinoctial = (FieldEquinoctialOrbit<T>) op;
        } else if (op instanceof FieldCartesianOrbit) {
            equinoctial = ((FieldCartesianOrbit<T>) op).equinoctial;
        } else {
            equinoctial = null;
        }
        field = op.getA().getField();
        zero = field.getZero();
        one = field.getOne();

        if (!FACTORIES.containsKey(field)) {
            FACTORIES.put(field, new FDSFactory<>(field, 1, 1));
        }

    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.CARTESIAN;
    }

    /** Lazy evaluation of equinoctial parameters. */
    private void initEquinoctial() {
        if (equinoctial == null) {
            if (hasDerivatives()) {
                // getPVCoordinates includes accelerations that will be interpreted as derivatives
                equinoctial = new FieldEquinoctialOrbit<>(getPVCoordinates(), getFrame(), getDate(), getMu());
            } else {
                // get rid of Keplerian acceleration so we don't assume
                // we have derivatives when in fact we don't have them
                equinoctial = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(getPVCoordinates().getPosition(),
                                                                                   getPVCoordinates().getVelocity()),
                                                          getFrame(), getDate(), getMu());
            }
        }
    }

    /** Get position with derivatives.
     * @return position with derivatives
     */
    private FieldVector3D<FieldDerivativeStructure<T>> getPositionDS() {
        final FieldVector3D<T> p = getPVCoordinates().getPosition();
        final FieldVector3D<T> v = getPVCoordinates().getVelocity();
        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(field);
        return new FieldVector3D<>(factory.build(p.getX(), v.getX()),
                                   factory.build(p.getY(), v.getY()),
                                   factory.build(p.getZ(), v.getZ()));
    }

    /** Get velocity with derivatives.
     * @return velocity with derivatives
     */
    private FieldVector3D<FieldDerivativeStructure<T>> getVelocityDS() {
        final FieldVector3D<T> v = getPVCoordinates().getVelocity();
        final FieldVector3D<T> a = getPVCoordinates().getAcceleration();
        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(field);
        return new FieldVector3D<>(factory.build(v.getX(), a.getX()),
                                   factory.build(v.getY(), a.getY()),
                                   factory.build(v.getZ(), a.getZ()));
    }

    /** {@inheritDoc} */
    public T getA() {
        // lazy evaluation of semi-major axis
        final T r  = getPVCoordinates().getPosition().getNorm();
        final T V2 = getPVCoordinates().getVelocity().getNormSq();
        return r.divide(r.negate().multiply(V2).divide(getMu()).add(2));
    }

    /** {@inheritDoc} */
    public T getADot() {
        if (hasDerivatives()) {
            final FieldDerivativeStructure<T> r  = getPositionDS().getNorm();
            final FieldDerivativeStructure<T> V2 = getVelocityDS().getNormSq();
            final FieldDerivativeStructure<T> a  = r.divide(r.multiply(V2).divide(getMu()).subtract(2).negate());
            return a.getPartialDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getE() {
        final T muA = getA().multiply(getMu());
        if (muA.getReal() > 0) {
            // elliptic or circular orbit
            final FieldVector3D<T> pvP   = getPVCoordinates().getPosition();
            final FieldVector3D<T> pvV   = getPVCoordinates().getVelocity();
            final T rV2OnMu = pvP.getNorm().multiply(pvV.getNormSq()).divide(getMu());
            final T eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE     = rV2OnMu.subtract(1);
            return (eCE.multiply(eCE).add(eSE.multiply(eSE))).sqrt();
        } else {
            // hyperbolic orbit
            final FieldVector3D<T> pvM = getPVCoordinates().getMomentum();
            return pvM.getNormSq().divide(muA).negate().add(1).sqrt();
        }
    }

    /** {@inheritDoc} */
    public T getEDot() {
        if (hasDerivatives()) {
            final FieldVector3D<FieldDerivativeStructure<T>> pvP   = getPositionDS();
            final FieldVector3D<FieldDerivativeStructure<T>> pvV   = getVelocityDS();
            final FieldDerivativeStructure<T> r       = getPositionDS().getNorm();
            final FieldDerivativeStructure<T> V2      = getVelocityDS().getNormSq();
            final FieldDerivativeStructure<T> rV2OnMu = r.multiply(V2).divide(getMu());
            final FieldDerivativeStructure<T> a       = r.divide(rV2OnMu.negate().add(2));
            final FieldDerivativeStructure<T> eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(getMu()).sqrt());
            final FieldDerivativeStructure<T> eCE     = rV2OnMu.subtract(1);
            final FieldDerivativeStructure<T> e       = eCE.multiply(eCE).add(eSE.multiply(eSE)).sqrt();
            return e.getPartialDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getI() {
        return FieldVector3D.angle(new FieldVector3D<>(zero, zero, one), getPVCoordinates().getMomentum());
    }

    /** {@inheritDoc} */
    public T getIDot() {
        if (hasDerivatives()) {
            final FieldVector3D<FieldDerivativeStructure<T>> momentum =
                            FieldVector3D.crossProduct(getPositionDS(), getVelocityDS());
            final FieldDerivativeStructure<T> i = FieldVector3D.angle(Vector3D.PLUS_K, momentum);
            return i.getPartialDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        initEquinoctial();
        return equinoctial.getEquinoctialEx();
    }

    /** {@inheritDoc} */
    public T getEquinoctialExDot() {
        initEquinoctial();
        return equinoctial.getEquinoctialExDot();
    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        initEquinoctial();
        return equinoctial.getEquinoctialEy();
    }

    /** {@inheritDoc} */
    public T getEquinoctialEyDot() {
        initEquinoctial();
        return equinoctial.getEquinoctialEyDot();
    }

    /** {@inheritDoc} */
    public T getHx() {
        final FieldVector3D<T> w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        final double x = w.getX().getReal();
        final double y = w.getY().getReal();
        final double z = w.getZ().getReal();
        if (((x * x + y * y) == 0) && z < 0) {
            return zero.add(Double.NaN);
        }
        return w.getY().negate().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getHxDot() {
        if (hasDerivatives()) {
            final FieldVector3D<FieldDerivativeStructure<T>> w =
                            FieldVector3D.crossProduct(getPositionDS(), getVelocityDS()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue().getReal();
            final double y = w.getY().getValue().getReal();
            final double z = w.getZ().getValue().getReal();
            if (((x * x + y * y) == 0) && z < 0) {
                return zero.add(Double.NaN);
            }
            final FieldDerivativeStructure<T> hx = w.getY().negate().divide(w.getZ().add(1));
            return hx.getPartialDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getHy() {
        final FieldVector3D<T> w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        final double x = w.getX().getReal();
        final double y = w.getY().getReal();
        final double z = w.getZ().getReal();
        if (((x * x + y * y) == 0) && z < 0) {
            return zero.add(Double.NaN);
        }
        return  w.getX().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getHyDot() {
        if (hasDerivatives()) {
            final FieldVector3D<FieldDerivativeStructure<T>> w =
                            FieldVector3D.crossProduct(getPositionDS(), getVelocityDS()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue().getReal();
            final double y = w.getY().getValue().getReal();
            final double z = w.getZ().getValue().getReal();
            if (((x * x + y * y) == 0) && z < 0) {
                return zero.add(Double.NaN);
            }
            final FieldDerivativeStructure<T> hy = w.getX().divide(w.getZ().add(1));
            return hy.getPartialDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getLv() {
        initEquinoctial();
        return equinoctial.getLv();
    }

    /** {@inheritDoc} */
    public T getLvDot() {
        initEquinoctial();
        return equinoctial.getLvDot();
    }

    /** {@inheritDoc} */
    public T getLE() {
        initEquinoctial();
        return equinoctial.getLE();
    }

    /** {@inheritDoc} */
    public T getLEDot() {
        initEquinoctial();
        return equinoctial.getLEDot();
    }

    /** {@inheritDoc} */
    public T getLM() {
        initEquinoctial();
        return equinoctial.getLM();
    }

    /** {@inheritDoc} */
    public T getLMDot() {
        initEquinoctial();
        return equinoctial.getLMDot();
    }

    /** {@inheritDoc} */
    public boolean hasDerivatives() {
        return hasNonKeplerianAcceleration;
    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {
        // nothing to do here, as the canonical elements are already the Cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getDate().getField().getZero().add(dt));
    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> shiftedBy(final T dt) {
        final FieldPVCoordinates<T> shiftedPV = (getA().getReal() < 0) ? shiftPVHyperbolic(dt) : shiftPVElliptic(dt);
        return new FieldCartesianOrbit<>(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * <p>
     * If orbit interpolation on large samples is needed, using the {@link
     * org.orekit.propagation.analytical.Ephemeris} class is a better way than using this
     * low-level interpolation. The Ephemeris class automatically handles selection of
     * a neighboring sub-sample with a predefined number of point from a large global sample
     * in a thread-safe way.
     * </p>
     */
    public FieldCartesianOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Stream<FieldOrbit<T>> sample) {
        final TimeStampedFieldPVCoordinates<T> interpolated =
                TimeStampedFieldPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_PVA,
                                                          sample.map(orbit -> orbit.getPVCoordinates()));
        return new FieldCartesianOrbit<>(interpolated, getFrame(), date, getMu());
    }

    /** Compute shifted position and velocity in elliptic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private FieldPVCoordinates<T> shiftPVElliptic(final T dt) {

        // preliminary computation
        final FieldVector3D<T> pvP   = getPVCoordinates().getPosition();
        final FieldVector3D<T> pvV   = getPVCoordinates().getVelocity();
        final T r2      = pvP.getNormSq();
        final T r       = r2.sqrt();
        final T rV2OnMu = r.multiply(pvV.getNormSq()).divide(getMu());
        final T a       = r.divide(rV2OnMu.negate().add(2));
        final T eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(getMu()).sqrt());
        final T eCE     = rV2OnMu.subtract(1);
        final T e2      = eCE.multiply(eCE).add(eSE.multiply(eSE));

        // we can use any arbitrary reference 2D frame in the orbital plane
        // in order to simplify some equations below, we use the current position as the u axis
        final FieldVector3D<T> u     = pvP.normalize();
        final FieldVector3D<T> v     = FieldVector3D.crossProduct(FieldVector3D.crossProduct(pvP, pvV), u).normalize();

        // the following equations rely on the specific choice of u explained above,
        // some coefficients that vanish to 0 in this case have already been removed here
        final T ex      = eCE.subtract(e2).multiply(a).divide(r);
        final T s       = e2.negate().add(1).sqrt();
        final T ey      = s.negate().multiply(eSE).multiply(a).divide(r);
        final T beta    = s.add(1).reciprocal();
        final T thetaE0 = ey.add(eSE.multiply(beta).multiply(ex)).atan2(r.divide(a).add(ex).subtract(eSE.multiply(beta).multiply(ey)));
        final T thetaM0 = thetaE0.subtract(ex.multiply(thetaE0.sin())).add(ey.multiply(thetaE0.cos()));

        // compute in-plane shifted eccentric argument
        final T sqrtMmuOA = a.reciprocal().multiply(getMu()).sqrt();
        final T thetaM1   = thetaM0.add(sqrtMmuOA.divide(a).multiply(dt));
        final T thetaE1   = meanToEccentric(thetaM1, ex, ey);
        final T cTE       = thetaE1.cos();
        final T sTE       = thetaE1.sin();

        // compute shifted in-plane Cartesian coordinates
        final T exey   = ex.multiply(ey);
        final T exCeyS = ex.multiply(cTE).add(ey.multiply(sTE));
        final T x      = a.multiply(beta.multiply(ey).multiply(ey).negate().add(1).multiply(cTE).add(beta.multiply(exey).multiply(sTE)).subtract(ex));
        final T y      = a.multiply(beta.multiply(ex).multiply(ex).negate().add(1).multiply(sTE).add(beta.multiply(exey).multiply(cTE)).subtract(ey));
        final T factor = sqrtMmuOA.divide(exCeyS.negate().add(1));
        final T xDot   = factor.multiply(beta.multiply(ey).multiply(exCeyS).subtract(sTE));
        final T yDot   = factor.multiply(cTE.subtract(beta.multiply(ex).multiply(exCeyS)));

        final FieldVector3D<T> shiftedP = new FieldVector3D<>(x, u, y, v);
        final FieldVector3D<T> shiftedV = new FieldVector3D<>(xDot, u, yDot, v);
        if (hasNonKeplerianAcceleration) {

            // extract non-Keplerian part of the initial acceleration
            final FieldVector3D<T> nonKeplerianAcceleration = new FieldVector3D<>(one, getPVCoordinates().getAcceleration(),
                                                                                  r.multiply(r2).reciprocal().multiply(getMu()), pvP);

            // add the quadratic motion due to the non-Keplerian acceleration to the Keplerian motion
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(one, shiftedP,
                                                                  dt.multiply(dt).multiply(0.5), nonKeplerianAcceleration);
            final T                fixedR2 = fixedP.getNormSq();
            final T                fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(one, shiftedV,
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR.multiply(fixedR2).reciprocal().multiply(getMu().negate()), shiftedP,
                                                                 one, nonKeplerianAcceleration);

            return new FieldPVCoordinates<>(fixedP, fixedV, fixedA);

        } else {
            // don't include acceleration,
            // so the shifted orbit is not considered to have derivatives
            return new FieldPVCoordinates<>(shiftedP, shiftedV);
        }

    }

    /** Compute shifted position and velocity in hyperbolic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private FieldPVCoordinates<T> shiftPVHyperbolic(final T dt) {

        final FieldPVCoordinates<T> pv = getPVCoordinates();
        final FieldVector3D<T> pvP   = pv.getPosition();
        final FieldVector3D<T> pvV   = pv.getVelocity();
        final FieldVector3D<T> pvM   = pv.getMomentum();
        final T r2      = pvP.getNormSq();
        final T r       = r2.sqrt();
        final T rV2OnMu = r.multiply(pvV.getNormSq()).divide(getMu());
        final T a       = getA();
        final T muA     = a.multiply(getMu());
        final T e       = one.subtract(FieldVector3D.dotProduct(pvM, pvM).divide(muA)).sqrt();
        final T sqrt    = e.add(1).divide(e.subtract(1)).sqrt();

        // compute mean anomaly
        final T eSH     = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
        final T eCH     = rV2OnMu.subtract(1);
        final T H0      = eCH.add(eSH).divide(eCH.subtract(eSH)).log().divide(2);
        final T M0      = e.multiply(H0.sinh()).subtract(H0);

        // find canonical 2D frame with p pointing to perigee
        final T v0      = sqrt.multiply(H0.divide(2).tanh()).atan().multiply(2);
        final FieldVector3D<T> p     = new FieldRotation<>(pvM, v0, RotationConvention.FRAME_TRANSFORM).applyTo(pvP).normalize();
        final FieldVector3D<T> q     = FieldVector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final T M1      = M0.add(getKeplerianMeanMotion().multiply(dt));
        final T H1      = meanToHyperbolicEccentric(M1, e);

        // compute shifted in-plane Cartesian coordinates
        final T cH     = H1.cosh();
        final T sH     = H1.sinh();
        final T sE2m1  = e.subtract(1).multiply(e.add(1)).sqrt();

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(cH.subtract(e));
        final T y      = a.negate().multiply(sE2m1).multiply(sH);
        final T factor = getMu().divide(a.negate()).sqrt().divide(e.multiply(cH).subtract(1));
        final T xDot   = factor.negate().multiply(sH);
        final T yDot   =  factor.multiply(sE2m1).multiply(cH);

        final FieldVector3D<T> shiftedP = new FieldVector3D<>(x, p, y, q);
        final FieldVector3D<T> shiftedV = new FieldVector3D<>(xDot, p, yDot, q);
        if (hasNonKeplerianAcceleration) {

            // extract non-Keplerian part of the initial acceleration
            final FieldVector3D<T> nonKeplerianAcceleration = new FieldVector3D<>(one, getPVCoordinates().getAcceleration(),
                                                                                  r.multiply(r2).reciprocal().multiply(getMu()), pvP);

            // add the quadratic motion due to the non-Keplerian acceleration to the Keplerian motion
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(one, shiftedP,
                                                                  dt.multiply(dt).multiply(0.5), nonKeplerianAcceleration);
            final T                fixedR2 = fixedP.getNormSq();
            final T                fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(one, shiftedV,
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR.multiply(fixedR2).reciprocal().multiply(getMu().negate()), shiftedP,
                                                                 one, nonKeplerianAcceleration);

            return new FieldPVCoordinates<>(fixedP, fixedV, fixedA);

        } else {
            // don't include acceleration,
            // so the shifted orbit is not considered to have derivatives
            return new FieldPVCoordinates<>(shiftedP, shiftedV);
        }

    }

    /** Computes the eccentric in-plane argument from the mean in-plane argument.
     * @param thetaM = mean in-plane argument (rad)
     * @param ex first component of eccentricity vector
     * @param ey second component of eccentricity vector
     * @return the eccentric in-plane argument.
     */
    private T meanToEccentric(final T thetaM, final T ex, final T ey) {
        // Generalization of Kepler equation to in-plane parameters
        // with thetaE = eta + E and
        //      thetaM = eta + M = thetaE - ex.sin(thetaE) + ey.cos(thetaE)
        // and eta being counted from an arbitrary reference in the orbital plane
        T thetaE        = thetaM;
        T thetaEMthetaM = zero;
        int    iter          = 0;
        do {
            final T cosThetaE = thetaE.cos();
            final T sinThetaE = thetaE.sin();

            final T f2 = ex.multiply(sinThetaE).subtract(ey.multiply(cosThetaE));
            final T f1 = one.subtract(ex.multiply(cosThetaE)).subtract(ey.multiply(sinThetaE));
            final T f0 = thetaEMthetaM.subtract(f2);

            final T f12 = f1.multiply(2.0);
            final T shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            thetaEMthetaM = thetaEMthetaM.subtract(shift);
            thetaE         = thetaM.add(thetaEMthetaM);

            if (FastMath.abs(shift.getReal()) <= 1.0e-12) {
                return thetaE;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(LocalizedCoreFormats.CONVERGENCE_FAILED);

    }

    /** Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is
     * Danby's iterative method (3rd order) with Vallado's initial guess.
     * </p>
     * @param M mean anomaly (rad)
     * @param ecc eccentricity
     * @return the hyperbolic eccentric anomaly
     */
    private T meanToHyperbolicEccentric(final T M, final T ecc) {

        // Resolution of hyperbolic Kepler equation for Keplerian parameters

        // Initial guess
        T H;
        if (ecc.getReal() < 1.6) {
            if ((-FastMath.PI < M.getReal() && M.getReal() < 0.) || M.getReal() > FastMath.PI) {
                H = M.subtract(ecc);
            } else {
                H = M.add(ecc);
            }
        } else {
            if (ecc.getReal() < 3.6 && FastMath.abs(M.getReal()) > FastMath.PI) {
                H = M.subtract(ecc.copySign(M));
            } else {
                H = M.divide(ecc.subtract(1.));
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final T f3  = ecc.multiply(H.cosh());
            final T f2  = ecc.multiply(H.sinh());
            final T f1  = f3.subtract(1.);
            final T f0  = f2.subtract(H).subtract(M);
            final T f12 = f1.multiply(2);
            final T d   = f0.divide(f12);
            final T fdf = f1.subtract(d.multiply(f2));
            final T ds  = f0.divide(fdf);

            final T shift = f0.divide(fdf.add(ds.multiply(ds).multiply(f3.divide(6.))));

            H = H.subtract(shift);

            if (FastMath.abs(shift.getReal()) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                            iter);
    }

    /** Create a 6x6 identity matrix.
     * @return 6x6 identity matrix
     */
    private T[][] create6x6Identity() {
        // this is the fastest way to set the 6x6 identity matrix
        final T[][] identity = MathArrays.buildArray(field, 6, 6);
        for (int i = 0; i < 6; i++) {
            Arrays.fill(identity[i], zero);
            identity[i][i] = one;
        }
        return identity;
    }

    @Override
    protected T[][] computeJacobianMeanWrtCartesian() {
        return create6x6Identity();
    }

    @Override
    protected T[][] computeJacobianEccentricWrtCartesian() {
        return create6x6Identity();
    }

    @Override
    protected T[][] computeJacobianTrueWrtCartesian() {
        return create6x6Identity();
    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final T gm,
                                      final T[] pDot) {

        final FieldPVCoordinates<T> pv = getPVCoordinates();

        // position derivative is velocity
        final FieldVector3D<T> velocity = pv.getVelocity();
        pDot[0] = pDot[0].add(velocity.getX());
        pDot[1] = pDot[1].add(velocity.getY());
        pDot[2] = pDot[2].add(velocity.getZ());

        // velocity derivative is Newtonian acceleration
        final FieldVector3D<T> position = pv.getPosition();
        final T r2         = position.getNormSq();
        final T coeff      = r2.multiply(r2.sqrt()).reciprocal().negate().multiply(gm);
        pDot[3] = pDot[3].add(coeff.multiply(position.getX()));
        pDot[4] = pDot[4].add(coeff.multiply(position.getY()));
        pDot[5] = pDot[5].add(coeff.multiply(position.getZ()));

    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        // use only the six defining elements, like the other Orbit.toString() methods
        final String comma = ", ";
        final PVCoordinates pv = getPVCoordinates().toPVCoordinates();
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        return "Cartesian parameters: {P(" +
                p.getX() + comma +
                p.getY() + comma +
                p.getZ() + "), V(" +
                v.getX() + comma +
                v.getY() + comma +
                v.getZ() + ")}";
    }

    @Override
    public CartesianOrbit toOrbit() {
        final PVCoordinates pv = getPVCoordinates().toPVCoordinates();
        final AbsoluteDate date = getPVCoordinates().getDate().toAbsoluteDate();
        if (hasDerivatives()) {
            // getPVCoordinates includes accelerations that will be interpreted as derivatives
            return new CartesianOrbit(pv, getFrame(), date, getMu().getReal());
        } else {
            // get rid of Keplerian acceleration so we don't assume
            // we have derivatives when in fact we don't have them
            return new CartesianOrbit(new PVCoordinates(pv.getPosition(), pv.getVelocity()),
                                      getFrame(), date, getMu().getReal());
        }
    }

}
