/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
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
 * the present implementation does not handle non-elliptical cases.
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
public class EquinoctialOrbit extends Orbit implements PositionAngleBased<EquinoctialOrbit> {

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

    /** Cached longitude argument (rad). */
    private final double cachedL;

    /** Cache type of position angle (longitude argument). */
    private final PositionAngleType cachedPositionAngleType;

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

    /** Derivative of cached longitude argument (rad/s). */
    private final double cachedLDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private PVCoordinates partialPV;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω + Ω), first component of eccentricity vector
     * @param ey e sin(ω + Ω), second component of eccentricity vector
     * @param hx tan(i/2) cos(Ω), first component of inclination vector
     * @param hy tan(i/2) sin(Ω), second component of inclination vector
     * @param l  (M or E or v) + ω + Ω, mean, eccentric or true longitude argument (rad)
     * @param type type of longitude argument
     * @param cachedPositionAngleType type of cached longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     * @since 12.1
     */
    public EquinoctialOrbit(final double a, final double ex, final double ey,
                            final double hx, final double hy, final double l,
                            final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(a, ex, ey, hx, hy, l,
             0., 0., 0., 0., 0., computeKeplerianLDot(type, a, ex, ey, mu, l, type),
             type, cachedPositionAngleType, frame, date, mu);
    }

    /** Creates a new instance without derivatives and with cached position angle same as value inputted.
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
        this(a, ex, ey, hx, hy, l, type, type, frame, date, mu);
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
     * @param cachedPositionAngleType of cached longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     * @since 12.1
     */
    public EquinoctialOrbit(final double a, final double ex, final double ey,
                            final double hx, final double hy, final double l,
                            final double aDot, final double exDot, final double eyDot,
                            final double hxDot, final double hyDot, final double lDot,
                            final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.cachedPositionAngleType = cachedPositionAngleType;
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

        final UnivariateDerivative1 lUD = initializeCachedL(l, lDot, type);
        this.cachedL = lUD.getValue();
        this.cachedLDot = lUD.getFirstDerivative();

        this.partialPV = null;

    }

    /** Creates a new instance with derivatives and with cached position angle same as value inputted.
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
        this(a, ex, ey, hx, hy, l, aDot, exDot, eyDot, hxDot, hyDot, lDot, type, type, frame, date, mu);
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
        cachedPositionAngleType = PositionAngleType.TRUE;
        final double cLv = (pvP.getX() - d * pvP.getZ() * w.getX()) / r;
        final double sLv = (pvP.getY() - d * pvP.getZ() * w.getY()) / r;
        cachedL = FastMath.atan2(sLv, cLv);

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

            // in order to compute true longitude argument derivative, we must compute
            // mean longitude argument derivative including Keplerian motion and convert to true longitude argument
            final double lMDot = getKeplerianMeanMotion() +
                                 jacobian[5][3] * aX + jacobian[5][4] * aY + jacobian[5][5] * aZ;
            final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
            final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
            final UnivariateDerivative1 lMUD = new UnivariateDerivative1(getLM(), lMDot);
            final UnivariateDerivative1 lvUD = FieldEquinoctialLongitudeArgumentUtility.meanToTrue(exUD, eyUD, lMUD);
            cachedLDot = lvUD.getFirstDerivative();

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot  = 0.;
            exDot = 0.;
            eyDot = 0.;
            hxDot = 0.;
            hyDot = 0.;
            cachedLDot = computeKeplerianLDot(cachedPositionAngleType, a, ex, ey, mu, cachedL, cachedPositionAngleType);
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
        cachedPositionAngleType = PositionAngleType.TRUE;
        cachedL   = op.getLv();
        cachedLDot = op.hasNonKeplerianAcceleration() ? op.getLvDot() :
                computeKeplerianLDot(cachedPositionAngleType, a, ex, ey, op.getMu(), cachedL, cachedPositionAngleType);
        partialPV = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNonKeplerianAcceleration() {
        return aDot != 0. || exDot != 0. || eyDot != 0. || hxDot != 0. || hyDot != 0. ||
                FastMath.abs(cachedLDot - computeKeplerianLDot(cachedPositionAngleType, a, ex, ey, getMu(), cachedL, cachedPositionAngleType)) > TOLERANCE_POSITION_ANGLE_RATE;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.EQUINOCTIAL;
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public double getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEx() {
        return ex;
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialExDot() {
        return exDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEy() {
        return ey;
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEyDot() {
        return eyDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getHx() {
        return hx;
    }

    /** {@inheritDoc} */
    @Override
    public double getHxDot() {
        return hxDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getHy() {
        return hy;
    }

    /** {@inheritDoc} */
    @Override
    public double getHyDot() {
        return hyDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return cachedL;

            case ECCENTRIC:
                return EquinoctialLongitudeArgumentUtility.eccentricToTrue(ex, ey, cachedL);

            case MEAN:
                return EquinoctialLongitudeArgumentUtility.meanToTrue(ex, ey, cachedL);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLvDot() {
        switch (cachedPositionAngleType) {
            case ECCENTRIC:
                final UnivariateDerivative1 lEUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lvUD = FieldEquinoctialLongitudeArgumentUtility.eccentricToTrue(exUD, eyUD,
                        lEUD);
                return lvUD.getFirstDerivative();

            case TRUE:
                return cachedLDot;

            case MEAN:
                final UnivariateDerivative1 lMUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD2    = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD2    = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lvUD2 = FieldEquinoctialLongitudeArgumentUtility.meanToTrue(exUD2,
                        eyUD2, lMUD);
                return lvUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return EquinoctialLongitudeArgumentUtility.trueToEccentric(ex, ey, cachedL);

            case ECCENTRIC:
                return cachedL;

            case MEAN:
                return EquinoctialLongitudeArgumentUtility.meanToEccentric(ex, ey, cachedL);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLEDot() {
        switch (cachedPositionAngleType) {
            case TRUE:
                final UnivariateDerivative1 lvUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lEUD = FieldEquinoctialLongitudeArgumentUtility.trueToEccentric(exUD, eyUD,
                        lvUD);
                return lEUD.getFirstDerivative();

            case ECCENTRIC:
                return cachedLDot;

            case MEAN:
                final UnivariateDerivative1 lMUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD2    = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD2    = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lEUD2 = FieldEquinoctialLongitudeArgumentUtility.meanToEccentric(exUD2,
                        eyUD2, lMUD);
                return lEUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return EquinoctialLongitudeArgumentUtility.trueToMean(ex, ey, cachedL);

            case MEAN:
                return cachedL;

            case ECCENTRIC:
                return EquinoctialLongitudeArgumentUtility.eccentricToMean(ex, ey, cachedL);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLMDot() {
        switch (cachedPositionAngleType) {
            case TRUE:
                final UnivariateDerivative1 lvUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lMUD = FieldEquinoctialLongitudeArgumentUtility.trueToMean(exUD, eyUD, lvUD);
                return lMUD.getFirstDerivative();

            case MEAN:
                return cachedLDot;

            case ECCENTRIC:
                final UnivariateDerivative1 lEUD = new UnivariateDerivative1(cachedL, cachedLDot);
                final UnivariateDerivative1 exUD2    = new UnivariateDerivative1(ex,     exDot);
                final UnivariateDerivative1 eyUD2    = new UnivariateDerivative1(ey,     eyDot);
                final UnivariateDerivative1 lMUD2 = FieldEquinoctialLongitudeArgumentUtility.eccentricToMean(exUD2,
                        eyUD2, lEUD);
                return lMUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
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

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    @Override
    public double getEDot() {
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
        return (ex * exDot + ey * eyDot) / FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
    }

    /** {@inheritDoc} */
    @Override
    public double getIDot() {
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
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

    /** Initialize cached argument of longitude with rate.
     * @param l input argument of longitude
     * @param lDot rate of input argument of longitude
     * @param inputType position angle type passed as input
     * @return argument of longitude to cache with rate
     * @since 12.1
     */
    private UnivariateDerivative1 initializeCachedL(final double l, final double lDot,
                                                    final PositionAngleType inputType) {
        if (cachedPositionAngleType == inputType) {
            return new UnivariateDerivative1(l, lDot);

        } else {
            final UnivariateDerivative1 exUD = new UnivariateDerivative1(ex, exDot);
            final UnivariateDerivative1 eyUD = new UnivariateDerivative1(ey, eyDot);
            final UnivariateDerivative1 lUD = new UnivariateDerivative1(l, lDot);

            switch (cachedPositionAngleType) {

                case ECCENTRIC:
                    if (inputType == PositionAngleType.MEAN) {
                        return FieldEquinoctialLongitudeArgumentUtility.meanToEccentric(exUD, eyUD, lUD);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.trueToEccentric(exUD, eyUD, lUD);
                    }

                case TRUE:
                    if (inputType == PositionAngleType.MEAN) {
                        return FieldEquinoctialLongitudeArgumentUtility.meanToTrue(exUD, eyUD, lUD);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.eccentricToTrue(exUD, eyUD, lUD);
                    }

                case MEAN:
                    if (inputType == PositionAngleType.TRUE) {
                        return FieldEquinoctialLongitudeArgumentUtility.trueToMean(exUD, eyUD, lUD);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.eccentricToMean(exUD, eyUD, lUD);
                    }

                default:
                    throw new OrekitInternalError(null);

            }

        }

    }

    /** {@inheritDoc} */
    @Override
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
    @Override
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final double r2 = partialPV.getPosition().getNormSq();
        final Vector3D keplerianAcceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), partialPV.getPosition());
        final Vector3D acceleration = hasNonKeplerianRates() ?
                                      keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                      keplerianAcceleration;

        return new TimeStampedPVCoordinates(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit inFrame(final Frame inertialFrame) {
        final PVCoordinates pvCoordinates;
        if (hasNonKeplerianAcceleration()) {
            pvCoordinates = getPVCoordinates(inertialFrame);
        } else {
            final KinematicTransform transform = getFrame().getKinematicTransformTo(inertialFrame, getDate());
            pvCoordinates = transform.transformOnlyPV(getPVCoordinates());
        }
        final EquinoctialOrbit equinoctialOrbit = new EquinoctialOrbit(pvCoordinates, inertialFrame, getDate(), getMu());
        if (equinoctialOrbit.getCachedPositionAngleType() == getCachedPositionAngleType()) {
            return equinoctialOrbit;
        } else {
            return equinoctialOrbit.withCachedPositionAngleType(getCachedPositionAngleType());
        }
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit withCachedPositionAngleType(final PositionAngleType positionAngleType) {
        return new EquinoctialOrbit(a, ex, ey, hx, hy, getL(positionAngleType), aDot, exDot, eyDot, hxDot, hyDot,
                getLDot(positionAngleType), positionAngleType, getFrame(), getDate(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit shiftedBy(final double dt) {
        return shiftedBy(new TimeOffset(dt));
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit shiftedBy(final TimeOffset dt) {

        final double dtS = dt.toDouble();

        // use Keplerian-only motion
        final EquinoctialOrbit keplerianShifted = new EquinoctialOrbit(a, ex, ey, hx, hy,
                                                                       getLM() + getKeplerianMeanMotion() * dtS,
                                                                       PositionAngleType.MEAN, cachedPositionAngleType,
                                                                       getFrame(),
                                                                       getDate().shiftedBy(dt), getMu());

        if (dtS != 0. && hasNonKeplerianRates()) {

            // extract non-Keplerian acceleration from first time derivatives
            final Vector3D nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final Vector3D fixedP   = new Vector3D(1, keplerianShifted.partialPV.getPosition(),
                                                   0.5 * dtS * dtS, nonKeplerianAcceleration);
            final double   fixedR2 = fixedP.getNormSq();
            final double   fixedR  = FastMath.sqrt(fixedR2);
            final Vector3D fixedV  = new Vector3D(1, keplerianShifted.partialPV.getVelocity(),
                                                  dtS, nonKeplerianAcceleration);
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
    @Override
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
    @Override
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
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric longitude equation
        // tan((lv - lE)/2) = [ex sin lE - ey cos lE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos lE - ey sin lE]
        // leads to
        // cT (dlv - dlE) = cE dlE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin lE - ey cos lE)^2] / 2
        // d  = 1 + sqrt(1-ex^2-ey^2) - ex cos lE - ey sin lE
        // cE = (ex cos lE + ey sin lE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX =  sin lE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // cY = -cos lE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true longitude
        // dlv = (cT + cE) / cT dlE + cX / cT deX + cY / cT deX
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
    @Override
    public void addKeplerContribution(final PositionAngleType type, final double gm,
                                      final double[] pDot) {
        pDot[5] += computeKeplerianLDot(type, a, ex, ey, gm, cachedL, cachedPositionAngleType);
    }

    /**
     * Compute rate of argument of longitude.
     * @param type position angle type of rate
     * @param a semi major axis
     * @param ex ex
     * @param ey ey
     * @param mu mu
     * @param l argument of longitude
     * @param cachedType position angle type of passed l
     * @return first-order time derivative for l
     * @since 12.2
     */
    private static double computeKeplerianLDot(final PositionAngleType type, final double a, final double ex,
                                               final double ey, final double mu,
                                               final double l, final PositionAngleType cachedType) {
        final double n  = FastMath.sqrt(mu / a) / a;
        if (type == PositionAngleType.MEAN) {
            return n;
        }
        final double oMe2;
        final double ksi;
        final SinCos sc;
        if (type == PositionAngleType.ECCENTRIC) {
            sc = FastMath.sinCos(EquinoctialLongitudeArgumentUtility.convertL(cachedType, l, ex, ey, type));
            ksi  = 1. / (1 - ex * sc.cos() - ey * sc.sin());
            return n * ksi;
        } else { // TRUE
            sc = FastMath.sinCos(EquinoctialLongitudeArgumentUtility.convertL(cachedType, l, ex, ey, type));
            oMe2 = 1 - ex * ex - ey * ey;
            ksi  = 1 + ex * sc.cos() + ey * sc.sin();
            return n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
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
                                  append("; lv: ").append(FastMath.toDegrees(getLv())).
                                  append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getCachedPositionAngleType() {
        return cachedPositionAngleType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNonKeplerianRates() {
        return hasNonKeplerianAcceleration();
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialOrbit withKeplerianRates() {
        final PositionAngleType positionAngleType = getCachedPositionAngleType();
        return new EquinoctialOrbit(getA(), getEquinoctialEx(), getEquinoctialEy(), getHx(), getHy(),
                getL(positionAngleType), positionAngleType, getFrame(), getDate(), getMu());
    }

}
