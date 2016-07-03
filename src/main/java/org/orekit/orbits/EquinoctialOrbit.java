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
 * This class handles equinoctial orbital parameters, which can support both
 * circular and equatorial orbits.
 * <p>
 * The parameters used internally are the equinoctial elements which can be
 * related to keplerian elements as follows:
 *   <pre>
 *     a
 *     ex = e cos(ω + Ω)
 *     ey = e sin(ω + Ω)
 *     hx = tan(i/2) cos(Ω)
 *     hy = tan(i/2) sin(Ω)
 *     lv = v + ω + Ω
 *   </pre>
 * where ω stands for the Perigee Argument and Ω stands for the
 * Right Ascension of the Ascending Node.
 *
 * <p>
 * The conversion equations from and to keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is either equatorial or circular, the equinoctial
 * parameters are still unambiguously defined whereas some keplerian elements
 * (more precisely ω and Ω) become ambiguous. For this reason, equinoctial
 * parameters are the recommended way to represent orbits.
 * </p>
 * <p>
 * The instance <code>EquinoctialOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    CartesianOrbit
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class EquinoctialOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20141228L;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the eccentricity vector. */
    private final double ex;

    /** Second component of the eccentricity vector. */
    private final double ey;

    /** First component of the inclination vector. */
    private final double hx;

    /** Second component of the inclination vector. */
    private final double hy;

    /** True longitude argument (rad). */
    private final double lv;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω + Ω), first component of eccentricity vector
     * @param ey e sin(ω + Ω), second component of eccentricity vector
     * @param hx tan(i/2) cos(Ω), first component of inclination vector
     * @param hy tan(i/2) sin(Ω), second component of inclination vector
     * @param l  (M or E or v) + ω + Ω, mean, eccentric or true longitude argument (rad)
     * @param type type of longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public EquinoctialOrbit(final double a, final double ex, final double ey,
                            final double hx, final double hy,
                            final double l, final PositionAngle type,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a  =  a;
        this.ex = ex;
        this.ey = ey;
        this.hx = hx;
        this.hy = hy;

        switch (type) {
            case MEAN :
                this.lv = eccentricToTrue(meanToEccentric(l));
                break;
            case ECCENTRIC :
                this.lv = eccentricToTrue(l);
                break;
            case TRUE :
                this.lv = l;
                break;
            default :
                throw new OrekitInternalError(null);
        }

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the position, velocity and acceleration
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public EquinoctialOrbit(final TimeStampedPVCoordinates pvCoordinates,
                            final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        //  compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double V2 = pvV.getNormSq();
        final double rV2OnMu = r * V2 / mu;

        if (rV2OnMu > 2) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        // compute inclination vector
        final Vector3D w = pvCoordinates.getMomentum().normalize();
        final double d = 1.0 / (1 + w.getZ());
        hx = -d * w.getY();
        hy =  d * w.getX();

        // compute true longitude argument
        final double cLv = (pvP.getX() - d * pvP.getZ() * w.getX()) / r;
        final double sLv = (pvP.getY() - d * pvP.getZ() * w.getY()) / r;
        lv = FastMath.atan2(sLv, cLv);


        // compute semi-major axis
        a = r / (2 - rV2OnMu);

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2  = eCE * eCE + eSE * eSE;
        final double f   = eCE - e2;
        final double g   = FastMath.sqrt(1 - e2) * eSE;
        ex = a * (f * cLv + g * sLv) / r;
        ey = a * (f * sLv - g * cLv) / r;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the position end velocity
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public EquinoctialOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                            final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public EquinoctialOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a  = op.getA();
        ex = op.getEquinoctialEx();
        ey = op.getEquinoctialEy();
        hx = op.getHx();
        hy = op.getHy();
        lv = op.getLv();
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.EQUINOCTIAL;
    }

    /** {@inheritDoc} */
    public double getA() {
        return a;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        return ex;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        return ey;
    }

    /** {@inheritDoc} */
    public double getHx() {
        return hx;
    }

    /** {@inheritDoc} */
    public double getHy() {
        return hy;
    }

    /** Get the longitude argument.
     * @param type type of the angle
     * @return longitude argument (rad)
     */
    public double getL(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getLM() :
                                              ((type == PositionAngle.ECCENTRIC) ? getLE() :
                                                                                   getLv());
    }

    /** {@inheritDoc} */
    public double getLv() {
        return lv;
    }

    /** {@inheritDoc} */
    public double getLE() {
        final double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosLv   = FastMath.cos(lv);
        final double sinLv   = FastMath.sin(lv);
        final double num     = ey * cosLv - ex * sinLv;
        final double den     = epsilon + 1 + ex * cosLv + ey * sinLv;
        return lv + 2 * FastMath.atan(num / den);
    }

    /** Computes the true longitude argument from the eccentric longitude argument.
     * @param lE = E + ω + Ω eccentric longitude argument (rad)
     * @return the true longitude argument
     */
    private double eccentricToTrue(final double lE) {
        final double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosLE   = FastMath.cos(lE);
        final double sinLE   = FastMath.sin(lE);
        final double num     = ex * sinLE - ey * cosLE;
        final double den     = epsilon + 1 - ex * cosLE - ey * sinLE;
        return lE + 2 * FastMath.atan(num / den);
    }

    /** {@inheritDoc} */
    public double getLM() {
        final double lE = getLE();
        return lE - ex * FastMath.sin(lE) + ey * FastMath.cos(lE);
    }

    /** Computes the eccentric longitude argument from the mean longitude argument.
     * @param lM = M + ω + Ω mean longitude argument (rad)
     * @return the eccentric longitude argument
     */
    private double meanToEccentric(final double lM) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        double lE = lM;
        double shift = 0.0;
        double lEmlM = 0.0;
        double cosLE = FastMath.cos(lE);
        double sinLE = FastMath.sin(lE);
        int iter = 0;
        do {
            final double f2 = ex * sinLE - ey * cosLE;
            final double f1 = 1.0 - ex * cosLE - ey * sinLE;
            final double f0 = lEmlM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            lEmlM -= shift;
            lE     = lM + lEmlM;
            cosLE  = FastMath.cos(lE);
            sinLE  = FastMath.sin(lE);

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return lE;

    }

    /** {@inheritDoc} */
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    public double getI() {
        return 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // get equinoctial parameters
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
        final double exey = ex * ey;
        final double ex2  = ex * ex;
        final double ey2  = ey * ey;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + FastMath.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric longitude argument
        final double cLe    = FastMath.cos(lE);
        final double sLe    = FastMath.sin(lE);
        final double exCeyS = ex * cLe + ey * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * ey * exCeyS);
        final double ydot   = factor * ( cLe - beta * ex * exCeyS);

        final Vector3D position =
            new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final double r2 = position.getNormSq();
        final Vector3D velocity =
            new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);

        final Vector3D acceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), position);

        return new TimeStampedPVCoordinates(getDate(), position, velocity, acceleration);

    }

    /** {@inheritDoc} */
    public EquinoctialOrbit shiftedBy(final double dt) {
        return new EquinoctialOrbit(a, ex, ey, hx, hy,
                                    getLM() + getKeplerianMeanMotion() * dt,
                                    PositionAngle.MEAN, getFrame(),
                                    getDate().shiftedBy(dt), getMu());
    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on equinoctial elements, without derivatives (which means the interpolation
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
    public EquinoctialOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousLm = Double.NaN;
        for (final Orbit orbit : sample) {
            final EquinoctialOrbit equi = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);
            final double continuousLm;
            if (previousDate == null) {
                continuousLm = equi.getLM();
            } else {
                final double dt       = equi.getDate().durationFrom(previousDate);
                final double keplerLm = previousLm + equi.getKeplerianMeanMotion() * dt;
                continuousLm = MathUtils.normalizeAngle(equi.getLM(), keplerLm);
            }
            previousDate = equi.getDate();
            previousLm   = continuousLm;
            interpolator.addSamplePoint(equi.getDate().durationFrom(date),
                                        new double[] {
                                            equi.getA(),
                                            equi.getEquinoctialEx(),
                                            equi.getEquinoctialEy(),
                                            equi.getHx(),
                                            equi.getHy(),
                                            continuousLm
                                        });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new EquinoctialOrbit(interpolated[0], interpolated[1], interpolated[2],
                                    interpolated[3], interpolated[4], interpolated[5],
                                    PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        final Vector3D position = getPVCoordinates().getPosition();
        final Vector3D velocity = getPVCoordinates().getVelocity();
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double r3         = r * r2;

        final double mu         = getMu();
        final double sqrtMuA    = FastMath.sqrt(a * mu);
        final double a2         = a * a;

        final double e2         = ex * ex + ey * ey;
        final double oMe2       = 1 - e2;
        final double epsilon    = FastMath.sqrt(oMe2);
        final double beta       = 1 / (1 + epsilon);
        final double ratio      = epsilon * beta;

        final double hx2        = hx * hx;
        final double hy2        = hy * hy;
        final double hxhy       = hx * hy;

        // precomputing equinoctial frame unit vectors (f,g,w)
        final Vector3D f  = new Vector3D(1 - hy2 + hx2, 2 * hxhy, -2 * hy).normalize();
        final Vector3D g  = new Vector3D(2 * hxhy, 1 + hy2 - hx2, 2 * hx).normalize();
        final Vector3D w  = Vector3D.crossProduct(position, velocity).normalize();

        // coordinates of the spacecraft in the equinoctial frame
        final double x    = Vector3D.dotProduct(position, f);
        final double y    = Vector3D.dotProduct(position, g);
        final double xDot = Vector3D.dotProduct(velocity, f);
        final double yDot = Vector3D.dotProduct(velocity, g);

        // drDot / dEx = dXDot / dEx * f + dYDot / dEx * g
        final double c1 = a / (sqrtMuA * epsilon);
        final double c2 = a * sqrtMuA * beta / r3;
        final double c3 = sqrtMuA / (r3 * epsilon);
        final Vector3D drDotSdEx = new Vector3D( c1 * xDot * yDot - c2 * ey * x - c3 * x * y, f,
                                                -c1 * xDot * xDot - c2 * ey * y + c3 * x * x, g);

        // drDot / dEy = dXDot / dEy * f + dYDot / dEy * g
        final Vector3D drDotSdEy = new Vector3D( c1 * yDot * yDot + c2 * ex * x - c3 * y * y, f,
                                                -c1 * xDot * yDot + c2 * ex * y + c3 * x * y, g);

        // da
        final Vector3D vectorAR = new Vector3D(2 * a2 / r3, position);
        final Vector3D vectorARDot = new Vector3D(2 * a2 / mu, velocity);
        fillHalfRow(1, vectorAR,    jacobian[0], 0);
        fillHalfRow(1, vectorARDot, jacobian[0], 3);

        // dEx
        final double d1 = -a * ratio / r3;
        final double d2 = (hy * xDot - hx * yDot) / (sqrtMuA * epsilon);
        final double d3 = (hx * y - hy * x) / sqrtMuA;
        final Vector3D vectorExRDot =
            new Vector3D((2 * x * yDot - xDot * y) / mu, g, -y * yDot / mu, f, -ey * d3 / epsilon, w);
        fillHalfRow(ex * d1, position, -ey * d2, w, epsilon / sqrtMuA, drDotSdEy, jacobian[1], 0);
        fillHalfRow(1, vectorExRDot, jacobian[1], 3);

        // dEy
        final Vector3D vectorEyRDot =
            new Vector3D((2 * xDot * y - x * yDot) / mu, f, -x * xDot / mu, g, ex * d3 / epsilon, w);
        fillHalfRow(ey * d1, position, ex * d2, w, -epsilon / sqrtMuA, drDotSdEx, jacobian[2], 0);
        fillHalfRow(1, vectorEyRDot, jacobian[2], 3);

        // dHx
        final double h = (1 + hx2 + hy2) / (2 * sqrtMuA * epsilon);
        fillHalfRow(-h * xDot, w, jacobian[3], 0);
        fillHalfRow( h * x,    w, jacobian[3], 3);

       // dHy
        fillHalfRow(-h * yDot, w, jacobian[4], 0);
        fillHalfRow( h * y,    w, jacobian[4], 3);

        // dLambdaM
        final double l = -ratio / sqrtMuA;
        fillHalfRow(-1 / sqrtMuA, velocity, d2, w, l * ex, drDotSdEx, l * ey, drDotSdEy, jacobian[5], 0);
        fillHalfRow(-2 / sqrtMuA, position, ex * beta, vectorEyRDot, -ey * beta, vectorExRDot, d3, w, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation lM = lE - ex sin lE + ey cos lE leads to:
        // dlM = (1 - ex cos lE - ey sin lE) dE - sin lE dex + cos lE dey
        // which is inverted and rewritten as:
        // dlE = a/r dlM + sin lE a/r dex - cos lE a/r dey
        final double le    = getLE();
        final double cosLe = FastMath.cos(le);
        final double sinLe = FastMath.sin(le);
        final double aOr   = 1 / (1 - ex * cosLe - ey * sinLe);

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL  = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr * (rowL[j] + sinLe * rowEx[j] - cosLe * rowEy[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric longitude equation
        // tan((lV - lE)/2) = [ex sin lE - ey cos lE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos lE - ey sin lE]
        // leads to
        // cT (dlV - dlE) = cE dlE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin lE - ey cos lE)^2] / 2
        // d  = 1 + sqrt(1-ex^2-ey^2) - ex cos lE - ey sin lE
        // cE = (ex cos lE + ey sin lE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX =  sin lE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // cY = -cos lE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true longitude
        // dlV = (cT + cE) / cT dlE + cX / cT deX + cY / cT deX
        final double le        = getLE();
        final double cosLe     = FastMath.cos(le);
        final double sinLe     = FastMath.sin(le);
        final double eSinE     = ex * sinLe - ey * cosLe;
        final double ecosE     = ex * cosLe + ey * sinLe;
        final double e2        = ex * ex + ey * ey;
        final double epsilon   = FastMath.sqrt(1 - e2);
        final double onePeps   = 1 + epsilon;
        final double d         = onePeps - ecosE;
        final double cT        = (d * d + eSinE * eSinE) / 2;
        final double cE        = ecosE * onePeps - e2;
        final double cX        = ex * eSinE / epsilon - ey + sinLe * onePeps;
        final double cY        = ey * eSinE / epsilon + ex - cosLe * onePeps;
        final double factorLe  = (cT + cE) / cT;
        final double factorEx  = cX / cT;
        final double factorEy  = cY / cT;

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = factorLe * rowL[j] + factorEx * rowEx[j] + factorEy * rowEy[j];
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
                oMe2 = 1 - ex * ex - ey * ey;
                ksi  = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE :
                oMe2 = 1 - ex * ex - ey * ey;
                ksi  = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
                pDot[5] += n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this equinoctial parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("equinoctial parameters: ").append('{').
                                  append("a: ").append(a).
                                  append("; ex: ").append(ex).append("; ey: ").append(ey).
                                  append("; hx: ").append(hx).append("; hy: ").append(hy).
                                  append("; lv: ").append(FastMath.toDegrees(lv)).
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
        private DTO(final EquinoctialOrbit orbit) {

            final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();

            // decompose date
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            this.d = new double[] {
                epoch, offset, orbit.getMu(),
                orbit.a, orbit.ex, orbit.ey,
                orbit.hx, orbit.hy, orbit.lv
            };

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link EquinoctialOrbit}.
         * @return replacement {@link EquinoctialOrbit}
         */
        private Object readResolve() {
            return new EquinoctialOrbit(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngle.TRUE,
                                        frame, AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                        d[2]);
        }

    }

}
