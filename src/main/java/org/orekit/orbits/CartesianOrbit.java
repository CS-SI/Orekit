/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** This class holds cartesian orbital parameters.

 * <p>
 * The parameters used internally are the cartesian coordinates:
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
 * Note that the implementation of this class delegates all non-cartesian related
 * computations ({@link #getA()}, {@link #getEquinoctialEx()}, ...) to an underlying
 * instance of the {@link EquinoctialOrbit} class. This implies that using this class
 * only for analytical computations which are always based on non-cartesian
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
 */
public class CartesianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

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
            equinoctial = new EquinoctialOrbit(getPVCoordinates(), getFrame(), getDate(), getMu());
        }
    }

    /** {@inheritDoc} */
    public double getA() {
        // lazy evaluation of semi-major axis
        final double r  = getPVCoordinates().getPosition().getNorm();
        final double V2 = getPVCoordinates().getVelocity().getNormSq();
        return r / (2 - r * V2 / getMu());
    }

    /** {@inheritDoc} */
    public double getE() {
        final Vector3D pvP   = getPVCoordinates().getPosition();
        final Vector3D pvV   = getPVCoordinates().getVelocity();
        final double rV2OnMu = pvP.getNorm() * pvV.getNormSq() / getMu();
        final double eSE     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(getMu() * getA());
        final double eCE     = rV2OnMu - 1;
        return FastMath.sqrt(eCE * eCE + eSE * eSE);
    }

    /** {@inheritDoc} */
    public double getI() {
        return Vector3D.angle(Vector3D.PLUS_K, getPVCoordinates().getMomentum());
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        initEquinoctial();
        return equinoctial.getEquinoctialEx();
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        initEquinoctial();
        return equinoctial.getEquinoctialEy();
    }

    /** {@inheritDoc} */
    public double getHx() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX() * w.getX() + w.getY() * w.getY()) == 0) && w.getZ() < 0) {
            return Double.NaN;
        }
        return -w.getY() / (1 + w.getZ());
    }

    /** {@inheritDoc} */
    public double getHy() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX() * w.getX() + w.getY() * w.getY()) == 0) && w.getZ() < 0) {
            return Double.NaN;
        }
        return  w.getX() / (1 + w.getZ());
    }

    /** {@inheritDoc} */
    public double getLv() {
        initEquinoctial();
        return equinoctial.getLv();
    }

    /** {@inheritDoc} */
    public double getLE() {
        initEquinoctial();
        return equinoctial.getLE();
    }

    /** {@inheritDoc} */
    public double getLM() {
        initEquinoctial();
        return equinoctial.getLM();
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {
        // nothing to do here, as the canonical elements are already the cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public CartesianOrbit shiftedBy(final double dt) {
        final PVCoordinates shiftedPV = (getA() < 0) ? shiftPVHyperbolic(dt) : shiftPVElliptic(dt);
        return new CartesianOrbit(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
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
    public CartesianOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {
        final List<TimeStampedPVCoordinates> datedPV = new ArrayList<TimeStampedPVCoordinates>(sample.size());
        for (final Orbit o : sample) {
            datedPV.add(new TimeStampedPVCoordinates(o.getDate(),
                                                     o.getPVCoordinates().getPosition(),
                                                     o.getPVCoordinates().getVelocity(),
                                                     o.getPVCoordinates().getAcceleration()));
        }
        final TimeStampedPVCoordinates interpolated =
                TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_PVA, datedPV);
        return new CartesianOrbit(interpolated, getFrame(), date, getMu());
    }

    /** Compute shifted position and velocity in elliptic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVElliptic(final double dt) {

        // preliminary computation
        final Vector3D pvP   = getPVCoordinates().getPosition();
        final Vector3D pvV   = getPVCoordinates().getVelocity();
        final double r       = pvP.getNorm();
        final double rV2OnMu = r * pvV.getNormSq() / getMu();
        final double a       = getA();
        final double eSE     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(getMu() * a);
        final double eCE     = rV2OnMu - 1;
        final double e2      = eCE * eCE + eSE * eSE;

        // we can use any arbitrary reference 2D frame in the orbital plane
        // in order to simplify some equations below, we use the current position as the u axis
        final Vector3D u     = pvP.normalize();
        final Vector3D v     = Vector3D.crossProduct(getPVCoordinates().getMomentum(), u).normalize();

        // the following equations rely on the specific choice of u explained above,
        // some coefficients that vanish to 0 in this case have already been removed here
        final double ex      = (eCE - e2) * a / r;
        final double ey      = -FastMath.sqrt(1 - e2) * eSE * a / r;
        final double beta    = 1 / (1 + FastMath.sqrt(1 - e2));
        final double thetaE0 = FastMath.atan2(ey + eSE * beta * ex, r / a + ex - eSE * beta * ey);
        final double thetaM0 = thetaE0 - ex * FastMath.sin(thetaE0) + ey * FastMath.cos(thetaE0);

        // compute in-plane shifted eccentric argument
        final double thetaM1 = thetaM0 + getKeplerianMeanMotion() * dt;
        final double thetaE1 = meanToEccentric(thetaM1, ex, ey);
        final double cTE     = FastMath.cos(thetaE1);
        final double sTE     = FastMath.sin(thetaE1);

        // compute shifted in-plane cartesian coordinates
        final double exey   = ex * ey;
        final double exCeyS = ex * cTE + ey * sTE;
        final double x      = a * ((1 - beta * ey * ey) * cTE + beta * exey * sTE - ex);
        final double y      = a * ((1 - beta * ex * ex) * sTE + beta * exey * cTE - ey);
        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xDot   = factor * (-sTE + beta * ey * exCeyS);
        final double yDot   = factor * ( cTE - beta * ex * exCeyS);

        final Vector3D shiftedP = new Vector3D(x, u, y, v);
        final double   r2       = x * x + y * y;
        final Vector3D shiftedV = new Vector3D(xDot, u, yDot, v);
        final Vector3D shiftedA = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), shiftedP);
        return new PVCoordinates(shiftedP, shiftedV, shiftedA);

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
        final double r       = pvP.getNorm();
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
        final double H1      = meanToHyperbolicEccentric(M1, e);

        // compute shifted in-plane cartesian coordinates
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
        final double   r2       = x * x + y * y;
        final Vector3D shiftedV = new Vector3D(xDot, p, yDot, q);
        final Vector3D shiftedA = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), shiftedP);
        return new PVCoordinates(shiftedP, shiftedV, shiftedA);

    }

    /** Computes the eccentric in-plane argument from the mean in-plane argument.
     * @param thetaM = mean in-plane argument (rad)
     * @param ex first component of eccentricity vector
     * @param ey second component of eccentricity vector
     * @return the eccentric in-plane argument.
     */
    private double meanToEccentric(final double thetaM, final double ex, final double ey) {
        // Generalization of Kepler equation to in-plane parameters
        // with thetaE = eta + E and
        //      thetaM = eta + M = thetaE - ex.sin(thetaE) + ey.cos(thetaE)
        // and eta being counted from an arbitrary reference in the orbital plane
        double thetaE        = thetaM;
        double thetaEMthetaM = 0.0;
        int    iter          = 0;
        do {
            final double cosThetaE = FastMath.cos(thetaE);
            final double sinThetaE = FastMath.sin(thetaE);

            final double f2 = ex * sinThetaE - ey * cosThetaE;
            final double f1 = 1.0 - ex * cosThetaE - ey * sinThetaE;
            final double f0 = thetaEMthetaM - f2;

            final double f12 = 2.0 * f1;
            final double shift = f0 * f12 / (f1 * f12 - f0 * f2);

            thetaEMthetaM -= shift;
            thetaE         = thetaM + thetaEMthetaM;

            if (FastMath.abs(shift) <= 1.0e-12) {
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
    private double meanToHyperbolicEccentric(final double M, final double ecc) {

        // Resolution of hyperbolic Kepler equation for keplerian parameters

        // Initial guess
        double H;
        if (ecc < 1.6) {
            if ((-FastMath.PI < M && M < 0.) || M > FastMath.PI) {
                H = M - ecc;
            } else {
                H = M + ecc;
            }
        } else {
            if (ecc < 3.6 && FastMath.abs(M) > FastMath.PI) {
                H = M - FastMath.copySign(ecc, M);
            } else {
                H = M / (ecc - 1.);
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final double f3  = ecc * FastMath.cosh(H);
            final double f2  = ecc * FastMath.sinh(H);
            final double f1  = f3 - 1.;
            final double f0  = f2 - H - M;
            final double f12 = 2. * f1;
            final double d   = f0 / f12;
            final double fdf = f1 - d * f2;
            final double ds  = f0 / fdf;

            final double shift = f0 / (fdf + ds * ds * f3 / 6.);

            H -= shift;

            if (FastMath.abs(shift) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                            iter);
    }

    @Override
    public void getJacobianWrtCartesian(final PositionAngle type, final double[][] jacobian) {
        // this is the fastest way to set the 6x6 identity matrix
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                jacobian[i][j] = 0;
            }
            jacobian[i][i] = 1;
        }
    }

    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        // not used
        return null;
    }
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        // not used
        return null;
    }

    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        // not used
        return null;
    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final double gm,
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
        return "cartesian parameters: " + getPVCoordinates().toString();
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes all needed parameters,
     * including position-velocity which are <em>not</em> serialized by parent
     * {@link Orbit} class.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140723L;

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
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            this.d = new double[] {
                epoch, offset, orbit.getMu(),
                pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ(),
            };

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link CartesianOrbit}.
         * @return replacement {@link CartesianOrbit}
         */
        private Object readResolve() {
            return new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                   new Vector3D(d[3], d[ 4], d[ 5]),
                                                                   new Vector3D(d[6], d[ 7], d[ 8]),
                                                                   new Vector3D(d[9], d[10], d[11])),
                                      frame, d[2]);
        }

    }

}
