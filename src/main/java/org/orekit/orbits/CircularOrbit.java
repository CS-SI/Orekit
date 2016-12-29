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
import java.util.Collection;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements which can be
 * related to keplerian elements as follows:
 *   <ul>
 *     <li>a</li>
 *     <li>e<sub>x</sub> = e cos(ω)</li>
 *     <li>e<sub>y</sub> = e sin(ω)</li>
 *     <li>i</li>
 *     <li>Ω</li>
 *     <li>α<sub>v</sub> = v + ω</li>
 *   </ul>
 * where Ω stands for the Right Ascension of the Ascending Node and
 * α<sub>v</sub> stands for the true latitude argument
 *
 * <p>
 * The conversion equations from and to keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is circular (but not equatorial), the circular
 * parameters are still unambiguously defined whereas some keplerian elements
 * (more precisely ω and Ω) become ambiguous. When orbit is equatorial,
 * neither the keplerian nor the circular parameters can be defined unambiguously.
 * {@link EquinoctialOrbit equinoctial orbits} is the recommended way to represent
 * orbits.
 * </p>
 * <p>
 * The instance <code>CircularOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CartesianOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class CircularOrbit
    extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20141228L;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the circular eccentricity vector. */
    private final double ex;

    /** Second component of the circular eccentricity vector. */
    private final double ey;

    /** Inclination (rad). */
    private final double i;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** True latitude argument (rad). */
    private final double alphaV;

    /** Indicator for {@link PVCoordinates} serialization. */
    private final boolean serializePV;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean, eccentric or true latitude argument (rad)
     * @param type type of latitude argument
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final double a, final double ex, final double ey,
                         final double i, final double raan,
                         final double alpha, final PositionAngle type,
                         final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a    =  a;
        this.ex   = ex;
        this.ey   = ey;
        this.i    = i;
        this.raan = raan;

        switch (type) {
            case MEAN :
                this.alphaV = eccentricToTrue(meanToEccentric(alpha));
                break;
            case ECCENTRIC :
                this.alphaV = eccentricToTrue(alpha);
                break;
            case TRUE :
                this.alphaV = alpha;
                break;
            default :
                throw new OrekitInternalError(null);
        }

        serializePV = false;

    }

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean, eccentric or true latitude argument (rad)
     * @param type type of latitude argument
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    private CircularOrbit(final double a, final double ex, final double ey,
                          final double i, final double raan,
                          final double alpha, final PositionAngle type,
                          final TimeStampedPVCoordinates pvCoordinates, final Frame frame,
                          final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a    =  a;
        this.ex   = ex;
        this.ey   = ey;
        this.i    = i;
        this.raan = raan;

        switch (type) {
            case MEAN :
                this.alphaV = eccentricToTrue(meanToEccentric(alpha));
                break;
            case ECCENTRIC :
                this.alphaV = eccentricToTrue(alpha);
                break;
            case TRUE :
                this.alphaV = alpha;
                break;
            default :
                throw new OrekitInternalError(null);
        }

        serializePV = true;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final TimeStampedPVCoordinates pvCoordinates, final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        // compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r  = pvP.getNorm();
        final double V2 = pvV.getNormSq();
        final double rV2OnMu = r * V2 / mu;

        if (rV2OnMu > 2) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        a = r / (2 - rV2OnMu);

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node  = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        raan = FastMath.atan2(node.getY(), node.getX());

        // 2D-coordinates in the canonical frame
        final double cosRaan = FastMath.cos(raan);
        final double sinRaan = FastMath.sin(raan);
        final double cosI    = FastMath.cos(i);
        final double sinI    = FastMath.sin(i);
        final double xP      = pvP.getX();
        final double yP      = pvP.getY();
        final double zP      = pvP.getZ();
        final double x2      = (xP * cosRaan + yP * sinRaan) / a;
        final double y2      = ((yP * cosRaan - xP * sinRaan) * cosI + zP * sinI) / a;

        // compute eccentricity vector
        final double eSE    = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(mu * a);
        final double eCE    = rV2OnMu - 1;
        final double e2     = eCE * eCE + eSE * eSE;
        final double f      = eCE - e2;
        final double g      = FastMath.sqrt(1 - e2) * eSE;
        final double aOnR   = a / r;
        final double a2OnR2 = aOnR * aOnR;
        ex = a2OnR2 * (f * x2 + g * y2);
        ey = a2OnR2 * (f * y2 - g * x2);

        // compute latitude argument
        final double beta = 1 / (1 + FastMath.sqrt(1 - ex * ex - ey * ey));
        alphaV = eccentricToTrue(FastMath.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey));

        serializePV = true;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                         final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CircularOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        i    = op.getI();
        raan = FastMath.atan2(op.getHy(), op.getHx());
        final double cosRaan = FastMath.cos(raan);
        final double sinRaan = FastMath.sin(raan);
        final double equiEx = op.getEquinoctialEx();
        final double equiEy = op.getEquinoctialEy();
        ex   = equiEx * cosRaan + equiEy * sinRaan;
        ey   = equiEy * cosRaan - equiEx * sinRaan;
        this.alphaV = op.getLv() - raan;
        serializePV = false;
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    public double getA() {
        return a;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        return ex * FastMath.cos(raan) - ey * FastMath.sin(raan);
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        return ey * FastMath.cos(raan) + ex * FastMath.sin(raan);
    }

    /** Get the first component of the circular eccentricity vector.
     * @return ex = e cos(ω), first component of the circular eccentricity vector
     */
    public double getCircularEx() {
        return ex;
    }

    /** Get the second component of the circular eccentricity vector.
     * @return ey = e sin(ω), second component of the circular eccentricity vector
     */
    public double getCircularEy() {
        return ey;
    }

    /** {@inheritDoc} */
    public double getHx() {
        return  FastMath.cos(raan) * FastMath.tan(i / 2);
    }

    /** {@inheritDoc} */
    public double getHy() {
        return  FastMath.sin(raan) * FastMath.tan(i / 2);
    }

    /** Get the true latitude argument.
     * @return v + ω true latitude argument (rad)
     */
    public double getAlphaV() {
        return alphaV;
    }

    /** Get the latitude argument.
     * @param type type of the angle
     * @return latitude argument (rad)
     */
    public double getAlpha(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getAlphaM() :
                                              ((type == PositionAngle.ECCENTRIC) ? getAlphaE() :
                                                                                   getAlphaV());
    }

    /** Get the eccentric latitude argument.
     * @return E + ω eccentric latitude argument (rad)
     */
    public double getAlphaE() {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaV = FastMath.cos(alphaV);
        final double sinAlphaV = FastMath.sin(alphaV);
        return alphaV + 2 * FastMath.atan((ey * cosAlphaV - ex * sinAlphaV) /
                                      (epsilon + 1 + ex * cosAlphaV + ey * sinAlphaV));
    }

    /** Computes the true latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @return the true latitude argument.
     */
    private double eccentricToTrue(final double alphaE) {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaE = FastMath.cos(alphaE);
        final double sinAlphaE = FastMath.sin(alphaE);
        return alphaE + 2 * FastMath.atan((ex * sinAlphaE - ey * cosAlphaE) /
                                      (epsilon + 1 - ex * cosAlphaE - ey * sinAlphaE));
    }

    /** Get the mean latitude argument.
     * @return M + ω mean latitude argument (rad)
     */
    public double getAlphaM() {
        final double alphaE = getAlphaE();
        return alphaE - ex * FastMath.sin(alphaE) + ey * FastMath.cos(alphaE);
    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    private double meanToEccentric(final double alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE        = alphaM;
        double shift         = 0.0;
        double alphaEMalphaM = 0.0;
        double cosAlphaE     = FastMath.cos(alphaE);
        double sinAlphaE     = FastMath.sin(alphaE);
        int    iter          = 0;
        do {
            final double f2 = ex * sinAlphaE - ey * cosAlphaE;
            final double f1 = 1.0 - ex * cosAlphaE - ey * sinAlphaE;
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE         = alphaM + alphaEMalphaM;
            cosAlphaE      = FastMath.cos(alphaE);
            sinAlphaE      = FastMath.sin(alphaE);

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return alphaE;

    }

    /** {@inheritDoc} */
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    public double getI() {
        return i;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** {@inheritDoc} */
    public double getLv() {
        return alphaV + raan;
    }

    /** {@inheritDoc} */
    public double getLE() {
        return getAlphaE() + raan;
    }

    /** {@inheritDoc} */
    public double getLM() {
        return getAlphaM() + raan;
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // get equinoctial parameters
        final double equEx = getEquinoctialEx();
        final double equEy = getEquinoctialEy();
        final double hx = getHx();
        final double hy = getHy();
        final double lE = getLE();

        // inclination-related intermediate parameters
        final double hx2   = hx * hx;
        final double hy2   = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy =  2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz =  2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = equEx * equEy;
        final double ex2  = equEx * equEx;
        final double ey2  = equEy * equEy;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + FastMath.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric latitude argument
        final double cLe    = FastMath.cos(lE);
        final double sLe    = FastMath.sin(lE);
        final double exCeyS = equEx * cLe + equEy * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - equEx);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - equEy);

        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * equEy * exCeyS);
        final double ydot   = factor * ( cLe - beta * equEx * exCeyS);

        final Vector3D position =
            new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final double r2         = position.getNormSq();
        final Vector3D velocity =
            new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);
        final Vector3D acceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), position);
        return new TimeStampedPVCoordinates(getDate(), position, velocity, acceleration);

    }

    /** {@inheritDoc} */
    public CircularOrbit shiftedBy(final double dt) {
        return new CircularOrbit(a, ex, ey, i, raan,
                                 getAlphaM() + getKeplerianMeanMotion() * dt,
                                 PositionAngle.MEAN, getFrame(),
                                 getDate().shiftedBy(dt), getMu());
    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on circular elements, without derivatives (which means the interpolation
     * falls back to Lagrange interpolation only).
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
    public CircularOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousRAAN   = Double.NaN;
        double previousAlphaM = Double.NaN;
        for (final Orbit orbit : sample) {
            final CircularOrbit circ = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);
            final double continuousRAAN;
            final double continuousAlphaM;
            if (previousDate == null) {
                continuousRAAN   = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            } else {
                final double dt       = circ.getDate().durationFrom(previousDate);
                final double keplerAM = previousAlphaM + circ.getKeplerianMeanMotion() * dt;
                continuousRAAN   = MathUtils.normalizeAngle(circ.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousAlphaM = MathUtils.normalizeAngle(circ.getAlphaM(), keplerAM);
            }
            previousDate   = circ.getDate();
            previousRAAN   = continuousRAAN;
            previousAlphaM = continuousAlphaM;
            interpolator.addSamplePoint(circ.getDate().durationFrom(date),
                                        new double[] {
                                            circ.getA(),
                                            circ.getCircularEx(),
                                            circ.getCircularEy(),
                                            circ.getI(),
                                            continuousRAAN,
                                            continuousAlphaM
                                        });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new CircularOrbit(interpolated[0], interpolated[1], interpolated[2],
                                 interpolated[3], interpolated[4], interpolated[5],
                                 PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        final PVCoordinates pvc = getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();

        final double x          = position.getX();
        final double y          = position.getY();
        final double z          = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double pv         = Vector3D.dotProduct(position, velocity);
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double v2         = velocity.getNormSq();

        final double mu         = getMu();
        final double oOsqrtMuA  = 1 / FastMath.sqrt(mu * a);
        final double rOa        = r / a;
        final double aOr        = a / r;
        final double aOr2       = a / r2;
        final double a2         = a * a;

        final double ex2        = ex * ex;
        final double ey2        = ey * ey;
        final double e2         = ex2 + ey2;
        final double epsilon    = FastMath.sqrt(1 - e2);
        final double beta       = 1 / (1 + epsilon);

        final double eCosE      = 1 - rOa;
        final double eSinE      = pv * oOsqrtMuA;

        final double cosI       = FastMath.cos(i);
        final double sinI       = FastMath.sin(i);
        final double cosRaan    = FastMath.cos(raan);
        final double sinRaan    = FastMath.sin(raan);

        // da
        fillHalfRow(2 * aOr * aOr2, position, jacobian[0], 0);
        fillHalfRow(2 * a2 / mu, velocity, jacobian[0], 3);

        // differentials of the normalized momentum
        final Vector3D danP = new Vector3D(v2, position, -pv, velocity);
        final Vector3D danV = new Vector3D(r2, velocity, -pv, position);
        final double recip  = 1 / pvc.getMomentum().getNorm();
        final double recip2 = recip * recip;
        final Vector3D dwXP = new Vector3D(recip, new Vector3D(  0,  vz, -vy), -recip2 * sinRaan * sinI, danP);
        final Vector3D dwYP = new Vector3D(recip, new Vector3D(-vz,   0,  vx),  recip2 * cosRaan * sinI, danP);
        final Vector3D dwZP = new Vector3D(recip, new Vector3D( vy, -vx,   0), -recip2 * cosI,           danP);
        final Vector3D dwXV = new Vector3D(recip, new Vector3D(  0,  -z,   y), -recip2 * sinRaan * sinI, danV);
        final Vector3D dwYV = new Vector3D(recip, new Vector3D(  z,   0,  -x),  recip2 * cosRaan * sinI, danV);
        final Vector3D dwZV = new Vector3D(recip, new Vector3D( -y,   x,   0), -recip2 * cosI,           danV);

        // di
        fillHalfRow(sinRaan * cosI, dwXP, -cosRaan * cosI, dwYP, -sinI, dwZP, jacobian[3], 0);
        fillHalfRow(sinRaan * cosI, dwXV, -cosRaan * cosI, dwYV, -sinI, dwZV, jacobian[3], 3);

        // dRaan
        fillHalfRow(sinRaan / sinI, dwYP, cosRaan / sinI, dwXP, jacobian[4], 0);
        fillHalfRow(sinRaan / sinI, dwYV, cosRaan / sinI, dwXV, jacobian[4], 3);

        // orbital frame: (p, q, w) p along ascending node, w along momentum
        // the coordinates of the spacecraft in this frame are: (u, v, 0)
        final double u     =  x * cosRaan + y * sinRaan;
        final double cv    = -x * sinRaan + y * cosRaan;
        final double v     = cv * cosI + z * sinI;

        // du
        final Vector3D duP = new Vector3D(cv * cosRaan / sinI, dwXP,
                                          cv * sinRaan / sinI, dwYP,
                                          1, new Vector3D(cosRaan, sinRaan, 0));
        final Vector3D duV = new Vector3D(cv * cosRaan / sinI, dwXV,
                                          cv * sinRaan / sinI, dwYV);

        // dv
        final Vector3D dvP = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXP,
                                          -u * sinRaan * cosI / sinI - cosRaan * z, dwYP,
                                          cv, dwZP,
                                          1, new Vector3D(-sinRaan * cosI, cosRaan * cosI, sinI));
        final Vector3D dvV = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXV,
                                          -u * sinRaan * cosI / sinI - cosRaan * z, dwYV,
                                          cv, dwZV);

        final Vector3D dc1P = new Vector3D(aOr2 * (2 * eSinE * eSinE + 1 - eCosE) / r2, position,
                                            -2 * aOr2 * eSinE * oOsqrtMuA, velocity);
        final Vector3D dc1V = new Vector3D(-2 * aOr2 * eSinE * oOsqrtMuA, position,
                                            2 / mu, velocity);
        final Vector3D dc2P = new Vector3D(aOr2 * eSinE * (eSinE * eSinE - (1 - e2)) / (r2 * epsilon), position,
                                            aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon, velocity);
        final Vector3D dc2V = new Vector3D(aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon, position,
                                            eSinE / (mu * epsilon), velocity);

        final double cof1   = aOr2 * (eCosE - e2);
        final double cof2   = aOr2 * epsilon * eSinE;
        final Vector3D dexP = new Vector3D(u, dc1P,  v, dc2P, cof1, duP,  cof2, dvP);
        final Vector3D dexV = new Vector3D(u, dc1V,  v, dc2V, cof1, duV,  cof2, dvV);
        final Vector3D deyP = new Vector3D(v, dc1P, -u, dc2P, cof1, dvP, -cof2, duP);
        final Vector3D deyV = new Vector3D(v, dc1V, -u, dc2V, cof1, dvV, -cof2, duV);
        fillHalfRow(1, dexP, jacobian[1], 0);
        fillHalfRow(1, dexV, jacobian[1], 3);
        fillHalfRow(1, deyP, jacobian[2], 0);
        fillHalfRow(1, deyV, jacobian[2], 3);

        final double cle = u / a + ex - eSinE * beta * ey;
        final double sle = v / a + ey + eSinE * beta * ex;
        final double m1  = beta * eCosE;
        final double m2  = 1 - m1 * eCosE;
        final double m3  = (u * ey - v * ex) + eSinE * beta * (u * ex + v * ey);
        final double m4  = -sle + cle * eSinE * beta;
        final double m5  = cle + sle * eSinE * beta;
        fillHalfRow((2 * m3 / r + aOr * eSinE + m1 * eSinE * (1 + m1 - (1 + aOr) * m2) / epsilon) / r2, position,
                    (m1 * m2 / epsilon - 1) * oOsqrtMuA, velocity,
                    m4, dexP, m5, deyP, -sle / a, duP, cle / a, dvP,
                    jacobian[5], 0);
        fillHalfRow((m1 * m2 / epsilon - 1) * oOsqrtMuA, position,
                    (2 * m3 + eSinE * a + m1 * eSinE * r * (eCosE * beta * 2 - aOr * m2) / epsilon) / mu, velocity,
                    m4, dexV, m5, deyV, -sle / a, duV, cle / a, dvV,
                    jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation aM = aE - ex sin aE + ey cos aE leads to:
        // daM = (1 - ex cos aE - ey sin aE) daE - sin aE dex + cos aE dey
        // which is inverted and rewritten as:
        // daE = a/r daM + sin aE a/r dex - cos aE a/r dey
        final double alphaE = getAlphaE();
        final double cosAe  = FastMath.cos(alphaE);
        final double sinAe  = FastMath.sin(alphaE);
        final double aOr    = 1 / (1 - ex * cosAe - ey * sinAe);

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL  = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr * (rowL[j] + sinAe * rowEx[j] - cosAe * rowEy[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric latitude equation
        // tan((aV - aE)/2) = [ex sin aE - ey cos aE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos aE - ey sin aE]
        // leads to
        // cT (daV - daE) = cE daE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin aE - ey cos aE)^2] / 2
        // d  = 1 + sqrt(1-ex^2-ey^2) - ex cos aE - ey sin aE
        // cE = (ex cos aE + ey sin aE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX =  sin aE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin aE - ey cos aE) / sqrt(1-ex^2-ey^2)
        // cY = -cos aE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin aE - ey cos aE) / sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true latitude
        // daV = (cT + cE) / cT daE + cX / cT deX + cY / cT deX
        final double alphaE    = getAlphaE();
        final double cosAe     = FastMath.cos(alphaE);
        final double sinAe     = FastMath.sin(alphaE);
        final double eSinE     = ex * sinAe - ey * cosAe;
        final double ecosE     = ex * cosAe + ey * sinAe;
        final double e2        = ex * ex + ey * ey;
        final double epsilon   = FastMath.sqrt(1 - e2);
        final double onePeps   = 1 + epsilon;
        final double d         = onePeps - ecosE;
        final double cT        = (d * d + eSinE * eSinE) / 2;
        final double cE        = ecosE * onePeps - e2;
        final double cX        = ex * eSinE / epsilon - ey + sinAe * onePeps;
        final double cY        = ey * eSinE / epsilon + ex - cosAe * onePeps;
        final double factorLe  = (cT + cE) / cT;
        final double factorEx  = cX / cT;
        final double factorEy  = cY / cT;

        // update latitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowA = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowA[j] = factorLe * rowA[j] + factorEx * rowEx[j] + factorEy * rowEy[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final double gm,
                                      final double[] pDot) {
        final double oMe2;
        final double ksi;
        final double n = FastMath.sqrt(gm / a) / a;
        switch (type) {
            case MEAN :
                pDot[5] += n;
                break;
            case ECCENTRIC :
                oMe2  = 1 - ex * ex - ey * ey;
                ksi   = 1 + ex * FastMath.cos(alphaV) + ey * FastMath.sin(alphaV);
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE :
                oMe2  = 1 - ex * ex - ey * ey;
                ksi   = 1 + ex * FastMath.cos(alphaV) + ey * FastMath.sin(alphaV);
                pDot[5] += n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("circular parameters: ").append('{').
                                  append("a: ").append(a).
                                  append(", ex: ").append(ex).append(", ey: ").append(ey).
                                  append(", i: ").append(FastMath.toDegrees(i)).
                                  append(", raan: ").append(FastMath.toDegrees(raan)).
                                  append(", alphaV: ").append(FastMath.toDegrees(alphaV)).
                                  append(";}").toString();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140617L;

        /** Double values. */
        private double[] d;

        /** Frame in which are defined the orbital parameters. */
        private final Frame frame;

        /** Simple constructor.
         * @param orbit instance to serialize
         */
        private DTO(final CircularOrbit orbit) {

            final AbsoluteDate date = orbit.getDate();

            // decompose date
            final double epoch  = FastMath.floor(date.durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = date.durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            if (orbit.serializePV) {
                final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    orbit.a, orbit.ex, orbit.ey,
                    orbit.i, orbit.raan, orbit.alphaV,
                    pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                    pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                    pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ(),
                };
            } else {
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    orbit.a, orbit.ex, orbit.ey,
                    orbit.i, orbit.raan, orbit.alphaV
                };
            }

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link CircularOrbit}.
         * @return replacement {@link CircularOrbit}
         */
        private Object readResolve() {
            if (d.length > 10) {
                return new CircularOrbit(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngle.TRUE,
                                         new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                      new Vector3D(d[9],  d[10], d[11]),
                                                                      new Vector3D(d[12], d[13], d[14]),
                                                                      new Vector3D(d[15], d[16], d[17])),
                                         frame,
                                         d[2]);
            } else {
                return new CircularOrbit(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngle.TRUE,
                                         frame, AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                         d[2]);

            }
        }

    }

}
