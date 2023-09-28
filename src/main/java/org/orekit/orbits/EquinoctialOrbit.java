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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
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
 * related to Keplerian elements as follows:
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
 * The conversion equations from and to Keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is either equatorial or circular, the equinoctial
 * parameters are still unambiguously defined whereas some Keplerian elements
 * (more precisely ω and Ω) become ambiguous. For this reason, equinoctial
 * parameters are the recommended way to represent orbits. Note however than
 *  * the present implementation does not handle non-elliptical cases.
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
public class EquinoctialOrbit extends Orbit implements PositionAngleBased {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170414L;

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

    /** Semi-major axis derivative (m/s). */
    private final double aDot;

    /** First component of the eccentricity vector derivative. */
    private final double exDot;

    /** Second component of the eccentricity vector derivative. */
    private final double eyDot;

    /** First component of the inclination vector derivative. */
    private final double hxDot;

    /** Second component of the inclination vector derivative. */
    private final double hyDot;

    /** True longitude argument derivative (rad/s). */
    private final double lvDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private transient PVCoordinates partialPV;

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
                            final double hx, final double hy, final double l,
                            final PositionAngleType type,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(a, ex, ey, hx, hy, l,
             Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
             type, frame, date, mu);
    }

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω + Ω), first component of eccentricity vector
     * @param ey e sin(ω + Ω), second component of eccentricity vector
     * @param hx tan(i/2) cos(Ω), first component of inclination vector
     * @param hy tan(i/2) sin(Ω), second component of inclination vector
     * @param l  (M or E or v) + ω + Ω, mean, eccentric or true longitude argument (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param exDot d(e cos(ω + Ω))/dt, first component of eccentricity vector derivative
     * @param eyDot d(e sin(ω + Ω))/dt, second component of eccentricity vector derivative
     * @param hxDot d(tan(i/2) cos(Ω))/dt, first component of inclination vector derivative
     * @param hyDot d(tan(i/2) sin(Ω))/dt, second component of inclination vector derivative
     * @param lDot  d(M or E or v) + ω + Ω)/dr, mean, eccentric or true longitude argument  derivative (rad/s)
     * @param type type of longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public EquinoctialOrbit(final double a, final double ex, final double ey,
                            final double hx, final double hy, final double l,
                            final double aDot, final double exDot, final double eyDot,
                            final double hxDot, final double hyDot, final double lDot,
                            final PositionAngleType type,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a     = a;
        this.aDot  = aDot;
        this.ex    = ex;
        this.exDot = exDot;
        this.ey    = ey;
        this.eyDot = eyDot;
        this.hx    = hx;
        this.hxDot = hxDot;
        this.hy    = hy;
        this.hyDot = hyDot;

        if (hasDerivatives()) {
            final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
            final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
            final UnivariateDerivative1 lUD  = new UnivariateDerivative1(l,  lDot);
            final UnivariateDerivative1 lvUD;
            switch (type) {
                case MEAN :
                    lvUD = FieldEquinoctialOrbit.eccentricToTrue(FieldEquinoctialOrbit.meanToEccentric(lUD, exUD, eyUD), exUD, eyUD);
                    break;
                case ECCENTRIC :
                    lvUD = FieldEquinoctialOrbit.eccentricToTrue(lUD, exUD, eyUD);
                    break;
                case TRUE :
                    lvUD = lUD;
                    break;
                default : // this should never happen
                    throw new OrekitInternalError(null);
            }
            this.lv    = lvUD.getValue();
            this.lvDot = lvUD.getDerivative(1);
        } else {
            switch (type) {
                case MEAN :
                    this.lv = eccentricToTrue(meanToEccentric(l, ex, ey), ex, ey);
                    break;
                case ECCENTRIC :
                    this.lv = eccentricToTrue(l, ex, ey);
                    break;
                case TRUE :
                    this.lv = l;
                    break;
                default : // this should never happen
                    throw new OrekitInternalError(null);
            }
            this.lvDot = Double.NaN;
        }

        this.partialPV = null;

    }

    /** Constructor from Cartesian parameters.
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
        final Vector3D pvP   = pvCoordinates.getPosition();
        final Vector3D pvV   = pvCoordinates.getVelocity();
        final Vector3D pvA   = pvCoordinates.getAcceleration();
        final double r2      = pvP.getNormSq();
        final double r       = FastMath.sqrt(r2);
        final double V2      = pvV.getNormSq();
        final double rV2OnMu = r * V2 / mu;

        // compute semi-major axis
        a = r / (2 - rV2OnMu);

        if (!isElliptical()) {
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

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2  = eCE * eCE + eSE * eSE;
        final double f   = eCE - e2;
        final double g   = FastMath.sqrt(1 - e2) * eSE;
        ex = a * (f * cLv + g * sLv) / r;
        ey = a * (f * sLv - g * cLv) / r;

        partialPV = pvCoordinates;

        if (hasNonKeplerianAcceleration(pvCoordinates, mu)) {
            // we have a relevant acceleration, we can compute derivatives

            final double[][] jacobian = new double[6][6];
            getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

            final Vector3D keplerianAcceleration    = new Vector3D(-mu / (r * r2), pvP);
            final Vector3D nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final double   aX                       = nonKeplerianAcceleration.getX();
            final double   aY                       = nonKeplerianAcceleration.getY();
            final double   aZ                       = nonKeplerianAcceleration.getZ();
            aDot  = jacobian[0][3] * aX + jacobian[0][4] * aY + jacobian[0][5] * aZ;
            exDot = jacobian[1][3] * aX + jacobian[1][4] * aY + jacobian[1][5] * aZ;
            eyDot = jacobian[2][3] * aX + jacobian[2][4] * aY + jacobian[2][5] * aZ;
            hxDot = jacobian[3][3] * aX + jacobian[3][4] * aY + jacobian[3][5] * aZ;
            hyDot = jacobian[4][3] * aX + jacobian[4][4] * aY + jacobian[4][5] * aZ;

            // in order to compute true anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to true anomaly
            final double lMDot = getKeplerianMeanMotion() +
                                 jacobian[5][3] * aX + jacobian[5][4] * aY + jacobian[5][5] * aZ;
            final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
            final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
            final UnivariateDerivative1 lMUD = new UnivariateDerivative1(getLM(), lMDot);
            final UnivariateDerivative1 lvUD = FieldEquinoctialOrbit.eccentricToTrue(FieldEquinoctialOrbit.meanToEccentric(lMUD, exUD, eyUD), exUD, eyUD);
            lvDot = lvUD.getDerivative(1);

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot  = Double.NaN;
            exDot = Double.NaN;
            eyDot = Double.NaN;
            hxDot = Double.NaN;
            hyDot = Double.NaN;
            lvDot = Double.NaN;
        }

    }

    /** Constructor from Cartesian parameters.
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
        a         = op.getA();
        aDot      = op.getADot();
        ex        = op.getEquinoctialEx();
        exDot     = op.getEquinoctialExDot();
        ey        = op.getEquinoctialEy();
        eyDot     = op.getEquinoctialEyDot();
        hx        = op.getHx();
        hxDot     = op.getHxDot();
        hy        = op.getHy();
        hyDot     = op.getHyDot();
        lv        = op.getLv();
        lvDot     = op.getLvDot();
        partialPV = null;
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
    public double getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        return ex;
    }

    /** {@inheritDoc} */
    public double getEquinoctialExDot() {
        return exDot;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        return ey;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEyDot() {
        return eyDot;
    }

    /** {@inheritDoc} */
    public double getHx() {
        return hx;
    }

    /** {@inheritDoc} */
    public double getHxDot() {
        return hxDot;
    }

    /** {@inheritDoc} */
    public double getHy() {
        return hy;
    }

    /** {@inheritDoc} */
    public double getHyDot() {
        return hyDot;
    }

    /** {@inheritDoc} */
    public double getLv() {
        return lv;
    }

    /** {@inheritDoc} */
    public double getLvDot() {
        return lvDot;
    }

    /** {@inheritDoc} */
    public double getLE() {
        return trueToEccentric(lv, ex, ey);
    }

    /** {@inheritDoc} */
    public double getLEDot() {
        final UnivariateDerivative1 lVUD = new UnivariateDerivative1(lv, lvDot);
        final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
        final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
        final UnivariateDerivative1 lEUD = FieldEquinoctialOrbit.trueToEccentric(lVUD, exUD, eyUD);
        return lEUD.getDerivative(1);
    }

    /** {@inheritDoc} */
    public double getLM() {
        return eccentricToMean(trueToEccentric(lv, ex, ey), ex, ey);
    }

    /** {@inheritDoc} */
    public double getLMDot() {
        final UnivariateDerivative1 lVUD = new UnivariateDerivative1(lv, lvDot);
        final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
        final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
        final UnivariateDerivative1 lMUD = FieldEquinoctialOrbit.eccentricToMean(FieldEquinoctialOrbit.trueToEccentric(lVUD, exUD, eyUD), exUD, eyUD);
        return lMUD.getDerivative(1);
    }

    /** Get the longitude argument.
     * @param type type of the angle
     * @return longitude argument (rad)
     */
    public double getL(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getLM() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getLE() :
                                                                                   getLv());
    }

    /** Get the longitude argument derivative.
     * @param type type of the angle
     * @return longitude argument derivative (rad/s)
     */
    public double getLDot(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getLMDot() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getLEDot() :
                                                                                   getLvDot());
    }

    /** Computes the true longitude argument from the eccentric longitude argument.
     * @param lE = E + ω + Ω eccentric longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @return the true longitude argument
     */
    public static double eccentricToTrue(final double lE, final double ex, final double ey) {
        final double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);
        final SinCos scLE    = FastMath.sinCos(lE);
        final double num     = ex * scLE.sin() - ey * scLE.cos();
        final double den     = epsilon + 1 - ex * scLE.cos() - ey * scLE.sin();
        return lE + 2 * FastMath.atan(num / den);
    }

    /** Computes the eccentric longitude argument from the true longitude argument.
     * @param lv = v + ω + Ω true longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @return the eccentric longitude argument
     */
    public static double trueToEccentric(final double lv, final double ex, final double ey) {
        final double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);
        final SinCos scLv    = FastMath.sinCos(lv);
        final double num     = ey * scLv.cos() - ex * scLv.sin();
        final double den     = epsilon + 1 + ex * scLv.cos() + ey * scLv.sin();
        return lv + 2 * FastMath.atan(num / den);
    }

    /** Computes the eccentric longitude argument from the mean longitude argument.
     * @param lM = M + ω + Ω mean longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @return the eccentric longitude argument
     */
    public static double meanToEccentric(final double lM, final double ex, final double ey) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        double lE = lM;
        double shift = 0.0;
        double lEmlM = 0.0;
        SinCos scLE  = FastMath.sinCos(lE);
        int iter = 0;
        do {
            final double f2 = ex * scLE.sin() - ey * scLE.cos();
            final double f1 = 1.0 - ex * scLE.cos() - ey * scLE.sin();
            final double f0 = lEmlM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            lEmlM -= shift;
            lE     = lM + lEmlM;
            scLE   = FastMath.sinCos(lE);

        } while (++iter < 50 && FastMath.abs(shift) > 1.0e-12);

        return lE;

    }

    /** Computes the mean longitude argument from the eccentric longitude argument.
     * @param lE = E + ω + Ω mean longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @return the mean longitude argument
     */
    public static double eccentricToMean(final double lE, final double ex, final double ey) {
        final SinCos scLE = FastMath.sinCos(lE);
        return lE - ex * scLE.sin() + ey * scLE.cos();
    }

    /** {@inheritDoc} */
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    public double getEDot() {
        return (ex * exDot + ey * eyDot) / FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    public double getI() {
        return 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
    }

    /** {@inheritDoc} */
    public double getIDot() {
        final double h2 = hx * hx + hy * hy;
        final double h  = FastMath.sqrt(h2);
        return 2 * (hx * hxDot + hy * hyDot) / (h * (1 + h2));
    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

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
        final SinCos scLe   = FastMath.sinCos(lE);
        final double cLe    = scLe.cos();
        final double sLe    = scLe.sin();
        final double exCeyS = ex * cLe + ey * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * ey * exCeyS);
        final double ydot   = factor * ( cLe - beta * ex * exCeyS);

        final Vector3D position =
                        new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final Vector3D velocity =
                        new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);
        partialPV = new PVCoordinates(position, velocity);

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private Vector3D nonKeplerianAcceleration() {

        final double[][] dCdP = new double[6][6];
        getJacobianWrtParameters(PositionAngleType.MEAN, dCdP);

        final double nonKeplerianMeanMotion = getLMDot() - getKeplerianMeanMotion();
        final double nonKeplerianAx = dCdP[3][0] * aDot  + dCdP[3][1] * exDot + dCdP[3][2] * eyDot +
                                      dCdP[3][3] * hxDot + dCdP[3][4] * hyDot + dCdP[3][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAy = dCdP[4][0] * aDot  + dCdP[4][1] * exDot + dCdP[4][2] * eyDot +
                                      dCdP[4][3] * hxDot + dCdP[4][4] * hyDot + dCdP[4][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAz = dCdP[5][0] * aDot  + dCdP[5][1] * exDot + dCdP[5][2] * eyDot +
                                      dCdP[5][3] * hxDot + dCdP[5][4] * hyDot + dCdP[5][5] * nonKeplerianMeanMotion;

        return new Vector3D(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    protected Vector3D initPosition() {

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
        final SinCos scLe   = FastMath.sinCos(lE);
        final double cLe    = scLe.cos();
        final double sLe    = scLe.sin();

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

        return new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);

    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final double r2 = partialPV.getPosition().getNormSq();
        final Vector3D keplerianAcceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), partialPV.getPosition());
        final Vector3D acceleration = hasDerivatives() ?
                                      keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                      keplerianAcceleration;

        return new TimeStampedPVCoordinates(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    public EquinoctialOrbit shiftedBy(final double dt) {

        // use Keplerian-only motion
        final EquinoctialOrbit keplerianShifted = new EquinoctialOrbit(a, ex, ey, hx, hy,
                                                                       getLM() + getKeplerianMeanMotion() * dt,
                                                                       PositionAngleType.MEAN, getFrame(),
                                                                       getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final Vector3D nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final Vector3D fixedP   = new Vector3D(1, keplerianShifted.partialPV.getPosition(),
                                                   0.5 * dt * dt, nonKeplerianAcceleration);
            final double   fixedR2 = fixedP.getNormSq();
            final double   fixedR  = FastMath.sqrt(fixedR2);
            final Vector3D fixedV  = new Vector3D(1, keplerianShifted.partialPV.getVelocity(),
                                                  dt, nonKeplerianAcceleration);
            final Vector3D fixedA  = new Vector3D(-getMu() / (fixedR2 * fixedR), keplerianShifted.partialPV.getPosition(),
                                                  1, nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new EquinoctialOrbit(new TimeStampedPVCoordinates(keplerianShifted.getDate(),
                                                                     fixedP, fixedV, fixedA),
                                        keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        computePVWithoutA();
        final Vector3D position = partialPV.getPosition();
        final Vector3D velocity = partialPV.getVelocity();
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

        // precomputing equinoctial frame unit vectors (f, g, w)
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
        final SinCos scLe  = FastMath.sinCos(getLE());
        final double cosLe = scLe.cos();
        final double sinLe = scLe.sin();
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
        final SinCos scLe      = FastMath.sinCos(getLE());
        final double cosLe     = scLe.cos();
        final double sinLe     = scLe.sin();
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
    public void addKeplerContribution(final PositionAngleType type, final double gm,
                                      final double[] pDot) {
        final double oMe2;
        final double ksi;
        final double n  = FastMath.sqrt(gm / a) / a;
        final SinCos sc = FastMath.sinCos(lv);
        switch (type) {
            case MEAN :
                pDot[5] += n;
                break;
            case ECCENTRIC :
                oMe2 = 1 - ex * ex - ey * ey;
                ksi  = 1 + ex * sc.cos() + ey * sc.sin();
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE :
                oMe2 = 1 - ex * ex - ey * ey;
                ksi  = 1 + ex * sc.cos() + ey * sc.sin();
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
        return new StringBuilder().append("equinoctial parameters: ").append('{').
                                  append("a: ").append(a).
                                  append("; ex: ").append(ex).append("; ey: ").append(ey).
                                  append("; hx: ").append(hx).append("; hy: ").append(hy).
                                  append("; lv: ").append(FastMath.toDegrees(lv)).
                                  append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getCachedPositionAngleType() {
        return PositionAngleType.TRUE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRates() {
        return hasDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit removeRates() {
        final PositionAngleType positionAngleType = getCachedPositionAngleType();
        return new EquinoctialOrbit(getA(), getEquinoctialEx(), getEquinoctialEy(), getHx(), getHy(),
                getL(positionAngleType), positionAngleType, getFrame(), getDate(), getMu());
    }

    /** Replace the instance with a data transfer object for serialization.
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
        private DTO(final EquinoctialOrbit orbit) {

            final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();

            // decompose date
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(j2000Epoch));
            final double offset = pv.getDate().durationFrom(j2000Epoch.shiftedBy(epoch));

            if (orbit.hasDerivatives()) {
                // we have derivatives
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    orbit.a, orbit.ex, orbit.ey,
                    orbit.hx, orbit.hy, orbit.lv,
                    orbit.aDot, orbit.exDot, orbit.eyDot,
                    orbit.hxDot, orbit.hyDot, orbit.lvDot
                };
            } else {
                // we don't have derivatives
                this.d = new double[] {
                    epoch, offset, orbit.getMu(),
                    orbit.a, orbit.ex, orbit.ey,
                    orbit.hx, orbit.hy, orbit.lv
                };
            }

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link EquinoctialOrbit}.
         * @return replacement {@link EquinoctialOrbit}
         */
        private Object readResolve() {
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            if (d.length >= 15) {
                // we have derivatives
                return new EquinoctialOrbit(d[ 3], d[ 4], d[ 5], d[ 6], d[ 7], d[ 8],
                                            d[ 9], d[10], d[11], d[12], d[13], d[14],
                                            PositionAngleType.TRUE,
                                            frame, j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                            d[2]);
            } else {
                // we don't have derivatives
                return new EquinoctialOrbit(d[ 3], d[ 4], d[ 5], d[ 6], d[ 7], d[ 8], PositionAngleType.TRUE,
                                            frame, j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                            d[2]);
            }
        }

    }

}
