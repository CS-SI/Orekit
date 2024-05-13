/* Copyright 2002-2024 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
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
 * @author Andrew Goetz
 * @since 9.0
 * @param <T> type of the field elements
 */
public class FieldCartesianOrbit<T extends CalculusFieldElement<T>> extends FieldOrbit<T> {

    /** Indicator for non-Keplerian acceleration. */
    private final transient boolean hasNonKeplerianAcceleration;

    /** Underlying equinoctial orbit to which high-level methods are delegated. */
    private transient FieldEquinoctialOrbit<T> equinoctial;

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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
    }

    /** Constructor from Field and CartesianOrbit.
     * <p>Build a FieldCartesianOrbit from non-Field CartesianOrbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldCartesianOrbit(final Field<T> field, final CartesianOrbit op) {
        super(new TimeStampedFieldPVCoordinates<>(field, op.getPVCoordinates()), op.getFrame(),
                field.getZero().newInstance(op.getMu()));
        hasNonKeplerianAcceleration = op.hasDerivatives();
        if (op.isElliptical()) {
            equinoctial = new FieldEquinoctialOrbit<>(field, new EquinoctialOrbit(op));
        } else {
            equinoctial = null;
        }
    }

    /** Constructor from Field and Orbit.
     * <p>Build a FieldCartesianOrbit from any non-Field Orbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldCartesianOrbit(final Field<T> field, final Orbit op) {
        this(field, new CartesianOrbit(op));
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
                final FieldPVCoordinates<T> pva = getPVCoordinates();
                equinoctial = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(pva.getPosition(), pva.getVelocity()),
                                                          getFrame(), getDate(), getMu());
            }
        }
    }

    /** Get the position/velocity with derivatives.
     * @return position/velocity with derivatives
     * @since 10.2
     */
    private FieldPVCoordinates<FieldUnivariateDerivative2<T>> getPVDerivatives() {
        // PVA coordinates
        final FieldPVCoordinates<T> pva = getPVCoordinates();
        final FieldVector3D<T>      p   = pva.getPosition();
        final FieldVector3D<T>      v   = pva.getVelocity();
        final FieldVector3D<T>      a   = pva.getAcceleration();
        // Field coordinates
        final FieldVector3D<FieldUnivariateDerivative2<T>> pG = new FieldVector3D<>(new FieldUnivariateDerivative2<>(p.getX(), v.getX(), a.getX()),
                                                             new FieldUnivariateDerivative2<>(p.getY(), v.getY(), a.getY()),
                                                             new FieldUnivariateDerivative2<>(p.getZ(), v.getZ(), a.getZ()));
        final FieldVector3D<FieldUnivariateDerivative2<T>> vG = new FieldVector3D<>(new FieldUnivariateDerivative2<>(v.getX(), a.getX(), getZero()),
                                                             new FieldUnivariateDerivative2<>(v.getY(), a.getY(), getZero()),
                                                             new FieldUnivariateDerivative2<>(v.getZ(), a.getZ(), getZero()));
        return new FieldPVCoordinates<>(pG, vG);
    }

    /** {@inheritDoc} */
    public T getA() {
        // lazy evaluation of semi-major axis
        final FieldPVCoordinates<T> pva = getPVCoordinates();
        final T r  = pva.getPosition().getNorm();
        final T V2 = pva.getVelocity().getNormSq();
        return r.divide(r.negate().multiply(V2).divide(getMu()).add(2));
    }

    /** {@inheritDoc} */
    public T getADot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv = getPVDerivatives();
            final FieldUnivariateDerivative2<T> r  = pv.getPosition().getNorm();
            final FieldUnivariateDerivative2<T> V2 = pv.getVelocity().getNormSq();
            final FieldUnivariateDerivative2<T> a  = r.divide(r.multiply(V2).divide(getMu()).subtract(2).negate());
            return a.getDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getE() {
        final T muA = getA().multiply(getMu());
        if (isElliptical()) {
            // elliptic or circular orbit
            final FieldPVCoordinates<T> pva = getPVCoordinates();
            final FieldVector3D<T> pvP      = pva.getPosition();
            final FieldVector3D<T> pvV      = pva.getVelocity();
            final T rV2OnMu = pvP.getNorm().multiply(pvV.getNormSq()).divide(getMu());
            final T eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE     = rV2OnMu.subtract(1);
            return (eCE.square().add(eSE.square())).sqrt();
        } else {
            // hyperbolic orbit
            final FieldVector3D<T> pvM = getPVCoordinates().getMomentum();
            return pvM.getNormSq().divide(muA).negate().add(1).sqrt();
        }
    }

    /** {@inheritDoc} */
    public T getEDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv = getPVDerivatives();
            final FieldUnivariateDerivative2<T> r       = pv.getPosition().getNorm();
            final FieldUnivariateDerivative2<T> V2      = pv.getVelocity().getNormSq();
            final FieldUnivariateDerivative2<T> rV2OnMu = r.multiply(V2).divide(getMu());
            final FieldUnivariateDerivative2<T> a       = r.divide(rV2OnMu.negate().add(2));
            final FieldUnivariateDerivative2<T> eSE     = FieldVector3D.dotProduct(pv.getPosition(), pv.getVelocity()).divide(a.multiply(getMu()).sqrt());
            final FieldUnivariateDerivative2<T> eCE     = rV2OnMu.subtract(1);
            final FieldUnivariateDerivative2<T> e       = eCE.square().add(eSE.square()).sqrt();
            return e.getDerivative(1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public T getI() {
        return FieldVector3D.angle(new FieldVector3D<>(getZero(), getZero(), getOne()), getPVCoordinates().getMomentum());
    }

    /** {@inheritDoc} */
    public T getIDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv = getPVDerivatives();
            final FieldVector3D<FieldUnivariateDerivative2<T>> momentum =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity());
            final FieldUnivariateDerivative2<T> i = FieldVector3D.angle(Vector3D.PLUS_K, momentum);
            return i.getDerivative(1);
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
        if ((x * x + y * y) == 0 && z < 0) {
            return getZero().add(Double.NaN);
        }
        return w.getY().negate().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getHxDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv = getPVDerivatives();
            final FieldVector3D<FieldUnivariateDerivative2<T>> w =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue().getReal();
            final double y = w.getY().getValue().getReal();
            final double z = w.getZ().getValue().getReal();
            if ((x * x + y * y) == 0 && z < 0) {
                return getZero().add(Double.NaN);
            }
            final FieldUnivariateDerivative2<T> hx = w.getY().negate().divide(w.getZ().add(1));
            return hx.getDerivative(1);
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
        if ((x * x + y * y) == 0 && z < 0) {
            return getZero().add(Double.NaN);
        }
        return  w.getX().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getHyDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv = getPVDerivatives();
            final FieldVector3D<FieldUnivariateDerivative2<T>> w =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue().getReal();
            final double y = w.getY().getValue().getReal();
            final double z = w.getZ().getValue().getReal();
            if ((x * x + y * y) == 0 && z < 0) {
                return getZero().add(Double.NaN);
            }
            final FieldUnivariateDerivative2<T> hy = w.getX().divide(w.getZ().add(1));
            return hy.getDerivative(1);
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
    protected FieldVector3D<T> initPosition() {
        // nothing to do here, as the canonical elements are already the Cartesian ones
        return getPVCoordinates().getPosition();
    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {
        // nothing to do here, as the canonical elements are already the Cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getZero().newInstance(dt));
    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> shiftedBy(final T dt) {
        final FieldPVCoordinates<T> shiftedPV = shiftPV(dt);
        return new FieldCartesianOrbit<>(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** Compute shifted position and velocity.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private FieldPVCoordinates<T> shiftPV(final T dt) {
        final FieldPVCoordinates<T> pvCoordinates = getPVCoordinates();
        final FieldPVCoordinates<T> shiftedPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt,
                pvCoordinates.getPosition(), pvCoordinates.getVelocity(), getMu());

        if (hasNonKeplerianAcceleration) {

            final FieldVector3D<T> pvP = getPosition();
            final T r2 = pvP.getNormSq();
            final T r = r2.sqrt();
            // extract non-Keplerian part of the initial acceleration
            final FieldVector3D<T> nonKeplerianAcceleration = new FieldVector3D<>(getOne(), getPVCoordinates().getAcceleration(),
                                                                                  r.multiply(r2).reciprocal().multiply(getMu()), pvP);

            // add the quadratic motion due to the non-Keplerian acceleration to the Keplerian motion
            final FieldVector3D<T> shiftedP = shiftedPV.getPosition();
            final FieldVector3D<T> shiftedV = shiftedPV.getVelocity();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(getOne(), shiftedP,
                                                                  dt.square().multiply(0.5), nonKeplerianAcceleration);
            final T                fixedR2 = fixedP.getNormSq();
            final T                fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(getOne(), shiftedV,
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR.multiply(fixedR2).reciprocal().multiply(getMu().negate()), shiftedP,
                                                                 getOne(), nonKeplerianAcceleration);

            return new FieldPVCoordinates<>(fixedP, fixedV, fixedA);

        } else {
            // don't include acceleration,
            // so the shifted orbit is not considered to have derivatives
            return shiftedPV;
        }
    }

    /** Create a 6x6 identity matrix.
     * @return 6x6 identity matrix
     */
    private T[][] create6x6Identity() {
        // this is the fastest way to set the 6x6 identity matrix
        final T[][] identity = MathArrays.buildArray(getField(), 6, 6);
        for (int i = 0; i < 6; i++) {
            Arrays.fill(identity[i], getZero());
            identity[i][i] = getOne();
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
    public void addKeplerContribution(final PositionAngleType type, final T gm,
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
