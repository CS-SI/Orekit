/* Copyright 2002-2023 CS GROUP
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

import java.io.Serializable;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


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
 *

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
 */
public class CartesianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170414L;

    /** 6x6 identity matrix. */
    private static final double[][] SIX_BY_SIX_IDENTITY = MatrixUtils.createRealIdentityMatrix(6).getData();

    /** Indicator for non-Keplerian derivatives. */
    private final transient boolean hasNonKeplerianAcceleration;

    /** Underlying equinoctial orbit to which high-level methods are delegated. */
    private transient EquinoctialOrbit equinoctial;

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position, velocity and acceleration of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CartesianOrbit(final TimeStampedPVCoordinates pvaCoordinates,
                          final Frame frame, final double mu)
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
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position and velocity of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CartesianOrbit(final PVCoordinates pvaCoordinates, final Frame frame,
                          final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvaCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CartesianOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getMu());
        hasNonKeplerianAcceleration = op.hasDerivatives();
        if (op instanceof EquinoctialOrbit) {
            equinoctial = (EquinoctialOrbit) op;
        } else if (op instanceof CartesianOrbit) {
            equinoctial = ((CartesianOrbit) op).equinoctial;
        } else {
            equinoctial = null;
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
                equinoctial = new EquinoctialOrbit(getPVCoordinates(), getFrame(), getDate(), getMu());
            } else {
                // get rid of Keplerian acceleration so we don't assume
                // we have derivatives when in fact we don't have them
                equinoctial = new EquinoctialOrbit(new PVCoordinates(getPosition(),
                                                                     getPVCoordinates().getVelocity()),
                                                   getFrame(), getDate(), getMu());
            }
        }
    }

    /** Get the position/velocity with derivatives.
     * @return position/velocity with derivatives
     * @since 10.2
     */
    private FieldPVCoordinates<UnivariateDerivative2> getPVDerivatives() {
        // PVA coordinates
        final PVCoordinates pva = getPVCoordinates();
        final Vector3D      p   = pva.getPosition();
        final Vector3D      v   = pva.getVelocity();
        final Vector3D      a   = pva.getAcceleration();
        // Field coordinates
        final FieldVector3D<UnivariateDerivative2> pG = new FieldVector3D<>(new UnivariateDerivative2(p.getX(), v.getX(), a.getX()),
                                                               new UnivariateDerivative2(p.getY(), v.getY(), a.getY()),
                                                               new UnivariateDerivative2(p.getZ(), v.getZ(), a.getZ()));
        final FieldVector3D<UnivariateDerivative2> vG = new FieldVector3D<>(new UnivariateDerivative2(v.getX(), a.getX(), 0.0),
                                                               new UnivariateDerivative2(v.getY(), a.getY(), 0.0),
                                                               new UnivariateDerivative2(v.getZ(), a.getZ(), 0.0));
        return new FieldPVCoordinates<>(pG, vG);
    }

    /** {@inheritDoc} */
    public double getA() {
        final double r  = getPosition().getNorm();
        final double V2 = getPVCoordinates().getVelocity().getNormSq();
        return r / (2 - r * V2 / getMu());
    }

    /** {@inheritDoc} */
    public double getADot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<UnivariateDerivative2> pv = getPVDerivatives();
            final UnivariateDerivative2 r  = pv.getPosition().getNorm();
            final UnivariateDerivative2 V2 = pv.getVelocity().getNormSq();
            final UnivariateDerivative2 a  = r.divide(r.multiply(V2).divide(getMu()).subtract(2).negate());
            return a.getDerivative(1);
        } else {
            return Double.NaN;
        }
    }

    /** {@inheritDoc} */
    public double getE() {
        final double muA = getMu() * getA();
        if (isElliptical()) {
            // elliptic or circular orbit
            final Vector3D pvP   = getPosition();
            final Vector3D pvV   = getPVCoordinates().getVelocity();
            final double rV2OnMu = pvP.getNorm() * pvV.getNormSq() / getMu();
            final double eSE     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
            final double eCE     = rV2OnMu - 1;
            return FastMath.sqrt(eCE * eCE + eSE * eSE);
        } else {
            // hyperbolic orbit
            final Vector3D pvM = getPVCoordinates().getMomentum();
            return FastMath.sqrt(1 - pvM.getNormSq() / muA);
        }
    }

    /** {@inheritDoc} */
    public double getEDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<UnivariateDerivative2> pv = getPVDerivatives();
            final FieldVector3D<UnivariateDerivative2> pvP   = pv.getPosition();
            final FieldVector3D<UnivariateDerivative2> pvV   = pv.getVelocity();
            final UnivariateDerivative2 r       = pvP.getNorm();
            final UnivariateDerivative2 V2      = pvV.getNormSq();
            final UnivariateDerivative2 rV2OnMu = r.multiply(V2).divide(getMu());
            final UnivariateDerivative2 a       = r.divide(rV2OnMu.negate().add(2));
            final UnivariateDerivative2 eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(getMu()).sqrt());
            final UnivariateDerivative2 eCE     = rV2OnMu.subtract(1);
            final UnivariateDerivative2 e       = eCE.multiply(eCE).add(eSE.multiply(eSE)).sqrt();
            return e.getDerivative(1);
        } else {
            return Double.NaN;
        }
    }

    /** {@inheritDoc} */
    public double getI() {
        return Vector3D.angle(Vector3D.PLUS_K, getPVCoordinates().getMomentum());
    }

    /** {@inheritDoc} */
    public double getIDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<UnivariateDerivative2> pv = getPVDerivatives();
            final FieldVector3D<UnivariateDerivative2> momentum =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity());
            final UnivariateDerivative2 i = FieldVector3D.angle(Vector3D.PLUS_K, momentum);
            return i.getDerivative(1);
        } else {
            return Double.NaN;
        }
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        initEquinoctial();
        return equinoctial.getEquinoctialEx();
    }

    /** {@inheritDoc} */
    public double getEquinoctialExDot() {
        initEquinoctial();
        return equinoctial.getEquinoctialExDot();
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        initEquinoctial();
        return equinoctial.getEquinoctialEy();
    }

    /** {@inheritDoc} */
    public double getEquinoctialEyDot() {
        initEquinoctial();
        return equinoctial.getEquinoctialEyDot();
    }

    /** {@inheritDoc} */
    public double getHx() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if ((w.getX() * w.getX() + w.getY() * w.getY()) == 0 && w.getZ() < 0) {
            return Double.NaN;
        }
        return -w.getY() / (1 + w.getZ());
    }

    /** {@inheritDoc} */
    public double getHxDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<UnivariateDerivative2> pv = getPVDerivatives();
            final FieldVector3D<UnivariateDerivative2> w =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue();
            final double y = w.getY().getValue();
            final double z = w.getZ().getValue();
            if ((x * x + y * y) == 0 && z < 0) {
                return Double.NaN;
            }
            final UnivariateDerivative2 hx = w.getY().negate().divide(w.getZ().add(1));
            return hx.getDerivative(1);
        } else {
            return Double.NaN;
        }
    }

    /** {@inheritDoc} */
    public double getHy() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if ((w.getX() * w.getX() + w.getY() * w.getY()) == 0 && w.getZ() < 0) {
            return Double.NaN;
        }
        return  w.getX() / (1 + w.getZ());
    }

    /** {@inheritDoc} */
    public double getHyDot() {
        if (hasDerivatives()) {
            final FieldPVCoordinates<UnivariateDerivative2> pv = getPVDerivatives();
            final FieldVector3D<UnivariateDerivative2> w =
                            FieldVector3D.crossProduct(pv.getPosition(), pv.getVelocity()).normalize();
            // Check for equatorial retrograde orbit
            final double x = w.getX().getValue();
            final double y = w.getY().getValue();
            final double z = w.getZ().getValue();
            if ((x * x + y * y) == 0 && z < 0) {
                return Double.NaN;
            }
            final UnivariateDerivative2 hy = w.getX().divide(w.getZ().add(1));
            return hy.getDerivative(1);
        } else {
            return Double.NaN;
        }
    }

    /** {@inheritDoc} */
    public double getLv() {
        initEquinoctial();
        return equinoctial.getLv();
    }

    /** {@inheritDoc} */
    public double getLvDot() {
        initEquinoctial();
        return equinoctial.getLvDot();
    }

    /** {@inheritDoc} */
    public double getLE() {
        initEquinoctial();
        return equinoctial.getLE();
    }

    /** {@inheritDoc} */
    public double getLEDot() {
        initEquinoctial();
        return equinoctial.getLEDot();
    }

    /** {@inheritDoc} */
    public double getLM() {
        initEquinoctial();
        return equinoctial.getLM();
    }

    /** {@inheritDoc} */
    public double getLMDot() {
        initEquinoctial();
        return equinoctial.getLMDot();
    }

    /** {@inheritDoc} */
    public boolean hasDerivatives() {
        return hasNonKeplerianAcceleration;
    }

    /** {@inheritDoc} */
    protected Vector3D initPosition() {
        // nothing to do here, as the canonical elements are already the Cartesian ones
        return getPVCoordinates().getPosition();
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {
        // nothing to do here, as the canonical elements are already the Cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public CartesianOrbit shiftedBy(final double dt) {
        final PVCoordinates shiftedPV = isElliptical() ? shiftPVElliptic(dt) : shiftPVHyperbolic(dt);
        return new CartesianOrbit(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** Compute shifted position and velocity in elliptic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVElliptic(final double dt) {

        // preliminary computation
        final PVCoordinates pv = getPVCoordinates();
        final Vector3D pvP     = pv.getPosition();
        final Vector3D pvV     = pv.getVelocity();
        final Vector3D pvM     = pv.getMomentum();
        final double r2        = pvP.getNormSq();
        final double r         = FastMath.sqrt(r2);
        final double rV2OnMu   = r * pvV.getNormSq() / getMu();
        final double a         = r / (2 - rV2OnMu);
        final double muA       = getMu() * a;

        // compute mean anomaly
        final double eSE    = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
        final double eCE    = rV2OnMu - 1;
        final double E0     = FastMath.atan2(eSE, eCE);
        final double M0     = E0 - eSE;

        final double e         = FastMath.sqrt(eCE * eCE + eSE * eSE);
        final double sqrt      = FastMath.sqrt((1 + e) / (1 - e));

        // find canonical 2D frame with p pointing to perigee
        final double v0     = 2 * FastMath.atan(sqrt * FastMath.tan(E0 / 2));
        final Vector3D p    = new Rotation(pvM, v0, RotationConvention.FRAME_TRANSFORM).applyTo(pvP).normalize();
        final Vector3D q    = Vector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final double M1     = M0 + getKeplerianMeanMotion() * dt;
        final double E1     = KeplerianAnomalyUtility.ellipticMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final SinCos scE    = FastMath.sinCos(E1);
        final double cE     = scE.cos();
        final double sE     = scE.sin();
        final double sE2m1  = FastMath.sqrt((1 - e) * (1 + e));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cE - e);
        final double y      = a * sE2m1 * sE;
        final double factor = FastMath.sqrt(getMu() / a) / (1 - e * cE);
        final double xDot   = -factor * sE;
        final double yDot   =  factor * sE2m1 * cE;

        final Vector3D shiftedP = new Vector3D(x, p, y, q);
        final Vector3D shiftedV = new Vector3D(xDot, p, yDot, q);
        if (hasNonKeplerianAcceleration) {

            // extract non-Keplerian part of the initial acceleration
            final Vector3D nonKeplerianAcceleration = new Vector3D(1, getPVCoordinates().getAcceleration(),
                                                                   getMu() / (r2 * r), pvP);

            // add the quadratic motion due to the non-Keplerian acceleration to the Keplerian motion
            final Vector3D fixedP   = new Vector3D(1, shiftedP,
                                                   0.5 * dt * dt, nonKeplerianAcceleration);
            final double   fixedR2 = fixedP.getNormSq();
            final double   fixedR  = FastMath.sqrt(fixedR2);
            final Vector3D fixedV  = new Vector3D(1, shiftedV,
                                                  dt, nonKeplerianAcceleration);
            final Vector3D fixedA  = new Vector3D(-getMu() / (fixedR2 * fixedR), shiftedP,
                                                  1, nonKeplerianAcceleration);

            return new PVCoordinates(fixedP, fixedV, fixedA);

        } else {
            // don't include acceleration,
            // so the shifted orbit is not considered to have derivatives
            return new PVCoordinates(shiftedP, shiftedV);
        }

    }

    /** Compute shifted position and velocity in hyperbolic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVHyperbolic(final double dt) {

        final PVCoordinates pv = getPVCoordinates();
        final Vector3D pvP   = pv.getPosition();
        final Vector3D pvV   = pv.getVelocity();
        final Vector3D pvM   = pv.getMomentum();
        final double r2      = pvP.getNormSq();
        final double r       = FastMath.sqrt(r2);
        final double rV2OnMu = r * pvV.getNormSq() / getMu();
        final double a       = getA();
        final double muA     = getMu() * a;
        final double e       = FastMath.sqrt(1 - Vector3D.dotProduct(pvM, pvM) / muA);
        final double sqrt    = FastMath.sqrt((e + 1) / (e - 1));

        // compute mean anomaly
        final double eSH     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
        final double eCH     = rV2OnMu - 1;
        final double H0      = FastMath.log((eCH + eSH) / (eCH - eSH)) / 2;
        final double M0      = e * FastMath.sinh(H0) - H0;

        // find canonical 2D frame with p pointing to perigee
        final double v0      = 2 * FastMath.atan(sqrt * FastMath.tanh(H0 / 2));
        final Vector3D p     = new Rotation(pvM, v0, RotationConvention.FRAME_TRANSFORM).applyTo(pvP).normalize();
        final Vector3D q     = Vector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final double M1      = M0 + getKeplerianMeanMotion() * dt;
        final double H1      = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final double cH     = FastMath.cosh(H1);
        final double sH     = FastMath.sinh(H1);
        final double sE2m1  = FastMath.sqrt((e - 1) * (e + 1));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cH - e);
        final double y      = -a * sE2m1 * sH;
        final double factor = FastMath.sqrt(getMu() / -a) / (e * cH - 1);
        final double xDot   = -factor * sH;
        final double yDot   =  factor * sE2m1 * cH;

        final Vector3D shiftedP = new Vector3D(x, p, y, q);
        final Vector3D shiftedV = new Vector3D(xDot, p, yDot, q);
        if (hasNonKeplerianAcceleration) {

            // extract non-Keplerian part of the initial acceleration
            final Vector3D nonKeplerianAcceleration = new Vector3D(1, getPVCoordinates().getAcceleration(),
                                                                   getMu() / (r2 * r), pvP);

            // add the quadratic motion due to the non-Keplerian acceleration to the Keplerian motion
            final Vector3D fixedP   = new Vector3D(1, shiftedP,
                                                   0.5 * dt * dt, nonKeplerianAcceleration);
            final double   fixedR2 = fixedP.getNormSq();
            final double   fixedR  = FastMath.sqrt(fixedR2);
            final Vector3D fixedV  = new Vector3D(1, shiftedV,
                                                  dt, nonKeplerianAcceleration);
            final Vector3D fixedA  = new Vector3D(-getMu() / (fixedR2 * fixedR), shiftedP,
                                                  1, nonKeplerianAcceleration);

            return new PVCoordinates(fixedP, fixedV, fixedA);

        } else {
            // don't include acceleration,
            // so the shifted orbit is not considered to have derivatives
            return new PVCoordinates(shiftedP, shiftedV);
        }

    }

    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        return SIX_BY_SIX_IDENTITY;
    }

    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        return SIX_BY_SIX_IDENTITY;
    }

    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        return SIX_BY_SIX_IDENTITY;
    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngleType type, final double gm,
                                      final double[] pDot) {

        final PVCoordinates pv = getPVCoordinates();

        // position derivative is velocity
        final Vector3D velocity = pv.getVelocity();
        pDot[0] += velocity.getX();
        pDot[1] += velocity.getY();
        pDot[2] += velocity.getZ();

        // velocity derivative is Newtonian acceleration
        final Vector3D position = pv.getPosition();
        final double r2         = position.getNormSq();
        final double coeff      = -gm / (r2 * FastMath.sqrt(r2));
        pDot[3] += coeff * position.getX();
        pDot[4] += coeff * position.getY();
        pDot[5] += coeff * position.getZ();

    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        // use only the six defining elements, like the other Orbit.toString() methods
        final String comma = ", ";
        final PVCoordinates pv = getPVCoordinates();
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

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes all needed parameters,
     * including position-velocity which are <em>not</em> serialized by parent
     * {@link Orbit} class.
     * </p>
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170414L;

        /** Double values. */
        private double[] d;

        /** Frame in which are defined the orbital parameters. */
        private final Frame frame;

        /** Simple constructor.
         * @param orbit instance to serialize
         */
        private DTO(final CartesianOrbit orbit) {

            final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();

            // decompose date
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(j2000Epoch));
            final double offset = pv.getDate().durationFrom(j2000Epoch.shiftedBy(epoch));

            if (orbit.hasDerivatives()) {
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                    pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                    pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ()
                };
            } else {
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                    pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ()
                };
            }

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link CartesianOrbit}.
         * @return replacement {@link CartesianOrbit}
         */
        private Object readResolve() {
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            if (d.length >= 12) {
                // we have derivatives
                return new CartesianOrbit(new TimeStampedPVCoordinates(j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                       new Vector3D(d[3], d[ 4], d[ 5]),
                                                                       new Vector3D(d[6], d[ 7], d[ 8]),
                                                                       new Vector3D(d[9], d[10], d[11])),
                                          frame, d[2]);
            } else {
                // we don't have derivatives
                return new CartesianOrbit(new TimeStampedPVCoordinates(j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                       new Vector3D(d[3], d[ 4], d[ 5]),
                                                                       new Vector3D(d[6], d[ 7], d[ 8])),
                                          frame, d[2]);
            }
        }

    }

}
