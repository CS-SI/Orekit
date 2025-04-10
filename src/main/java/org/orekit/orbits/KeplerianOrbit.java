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
import org.orekit.errors.OrekitException;
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
 * This class handles traditional Keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical Keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     ω
 *     Ω
 *     v
 *   </pre>
 * where ω stands for the Perigee Argument, Ω stands for the
 * Right Ascension of the Ascending Node and v stands for the true anomaly.
 *
 * <p>
 * This class supports hyperbolic orbits, using the convention that semi major
 * axis is negative for such orbits (and of course eccentricity is greater than 1).
 * </p>
 * <p>
 * When orbit is either equatorial or circular, some Keplerian elements
 * (more precisely ω and Ω) become ambiguous so this class should not
 * be used for such orbits. For this reason, {@link EquinoctialOrbit equinoctial
 * orbits} is the recommended way to represent orbits.
 * </p>

 * <p>
 * The instance <code>KeplerianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see     Orbit
 * @see    CircularOrbit
 * @see    CartesianOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class KeplerianOrbit extends Orbit implements PositionAngleBased<KeplerianOrbit> {

    /** Name of the eccentricity parameter. */
    private static final String ECCENTRICITY = "eccentricity";

    /** Semi-major axis (m). */
    private final double a;

    /** Eccentricity. */
    private final double e;

    /** Inclination (rad). */
    private final double i;

    /** Perigee Argument (rad). */
    private final double pa;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** Cached anomaly (rad). */
    private final double cachedAnomaly;

    /** Semi-major axis derivative (m/s). */
    private final double aDot;

    /** Eccentricity derivative. */
    private final double eDot;

    /** Inclination derivative (rad/s). */
    private final double iDot;

    /** Perigee Argument derivative (rad/s). */
    private final double paDot;

    /** Right Ascension of Ascending Node derivative (rad/s). */
    private final double raanDot;

    /** Derivative of cached anomaly (rad/s). */
    private final double cachedAnomalyDot;

    /** Cached type of position angle. */
    private final PositionAngleType cachedPositionAngleType;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private transient PVCoordinates partialPV;

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity (positive or equal to 0)
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly
     * @param cachedPositionAngleType type of cached anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     * @since 12.1
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan, final double anomaly,
                          final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                          final Frame frame, final AbsoluteDate date, final double mu)
            throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly,
                0., 0., 0., 0., 0., computeKeplerianAnomalyDot(type, a, e, mu, anomaly, type),
                type, cachedPositionAngleType, frame, date, mu);
    }

    /** Creates a new instance without derivatives and with cached position angle same as value inputted.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity (positive or equal to 0)
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan, final double anomaly,
                          final PositionAngleType type,
                          final Frame frame, final AbsoluteDate date, final double mu)
            throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly, type, type, frame, date, mu);
    }

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity (positive or equal to 0)
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param eDot eccentricity derivative
     * @param iDot inclination derivative (rad/s)
     * @param paDot perigee argument derivative (rad/s)
     * @param raanDot right ascension of ascending node derivative (rad/s)
     * @param anomalyDot mean, eccentric or true anomaly derivative (rad/s)
     * @param type type of input anomaly
     * @param cachedPositionAngleType type of cached anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     * @since 12.1
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan, final double anomaly,
                          final double aDot, final double eDot, final double iDot,
                          final double paDot, final double raanDot, final double anomalyDot,
                          final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                          final Frame frame, final AbsoluteDate date, final double mu)
            throws IllegalArgumentException {
        super(frame, date, mu);
        this.cachedPositionAngleType = cachedPositionAngleType;

        if (a * (1 - e) < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a, e);
        }

        // Checking eccentricity range
        checkParameterRangeInclusive(ECCENTRICITY, e, 0.0, Double.POSITIVE_INFINITY);

        this.a       = a;
        this.aDot    = aDot;
        this.e       = e;
        this.eDot    = eDot;
        this.i       = i;
        this.iDot    = iDot;
        this.pa      = pa;
        this.paDot   = paDot;
        this.raan    = raan;
        this.raanDot = raanDot;

        final UnivariateDerivative1 cachedAnomalyUD = initializeCachedAnomaly(anomaly, anomalyDot, type);
        this.cachedAnomaly = cachedAnomalyUD.getValue();
        this.cachedAnomalyDot = cachedAnomalyUD.getFirstDerivative();

        // check true anomaly range
        if (!isElliptical()) {
            final double trueAnomaly = getTrueAnomaly();
            if (1 + e * FastMath.cos(trueAnomaly) <= 0) {
                final double vMax = FastMath.acos(-1 / e);
                throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE,
                        trueAnomaly, e, -vMax, vMax);
            }
        }

        this.partialPV = null;

    }

    /** Creates a new instance with cached position angle same as value inputted.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity (positive or equal to 0)
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param eDot eccentricity derivative
     * @param iDot inclination derivative (rad/s)
     * @param paDot perigee argument derivative (rad/s)
     * @param raanDot right ascension of ascending node derivative (rad/s)
     * @param anomalyDot mean, eccentric or true anomaly derivative (rad/s)
     * @param type type of anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     * @since 9.0
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan, final double anomaly,
                          final double aDot, final double eDot, final double iDot,
                          final double paDot, final double raanDot, final double anomalyDot,
                          final PositionAngleType type,
                          final Frame frame, final AbsoluteDate date, final double mu)
            throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly, aDot, eDot, iDot, paDot, raanDot, anomalyDot, type, type,
                frame, date, mu);
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public KeplerianOrbit(final TimeStampedPVCoordinates pvCoordinates,
                          final Frame frame, final double mu)
            throws IllegalArgumentException {
        this(pvCoordinates, frame, mu, hasNonKeplerianAcceleration(pvCoordinates, mu));
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @param reliableAcceleration if true, the acceleration is considered to be reliable
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    private KeplerianOrbit(final TimeStampedPVCoordinates pvCoordinates,
                           final Frame frame, final double mu,
                           final boolean reliableAcceleration)
            throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        final double m2 = momentum.getNormSq();
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        raan = Vector3D.crossProduct(Vector3D.PLUS_K, momentum).getAlpha();

        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final Vector3D pvP     = pvCoordinates.getPosition();
        final Vector3D pvV     = pvCoordinates.getVelocity();
        final Vector3D pvA     = pvCoordinates.getAcceleration();
        final double   r2      = pvP.getNormSq();
        final double   r       = FastMath.sqrt(r2);
        final double   V2      = pvV.getNormSq();
        final double   rV2OnMu = r * V2 / mu;

        // compute semi-major axis (will be negative for hyperbolic orbits)
        a = r / (2 - rV2OnMu);
        final double muA = mu * a;

        // compute cached anomaly
        if (isElliptical()) {
            // elliptic or circular orbit
            final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
            final double eCE = rV2OnMu - 1;
            e = FastMath.sqrt(eSE * eSE + eCE * eCE);
            this.cachedPositionAngleType = PositionAngleType.ECCENTRIC;
            cachedAnomaly = FastMath.atan2(eSE, eCE);
        } else {
            // hyperbolic orbit
            final double eSH = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            e = FastMath.sqrt(1 - m2 / muA);
            this.cachedPositionAngleType = PositionAngleType.TRUE;
            cachedAnomaly = KeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, FastMath.log((eCH + eSH) / (eCH - eSH)) / 2);
        }

        // Checking eccentricity range
        checkParameterRangeInclusive(ECCENTRICITY, e, 0.0, Double.POSITIVE_INFINITY);

        // compute perigee argument
        final Vector3D node = new Vector3D(raan, 0.0);
        final double px = Vector3D.dotProduct(pvP, node);
        final double py = Vector3D.dotProduct(pvP, Vector3D.crossProduct(momentum, node)) / FastMath.sqrt(m2);
        pa = FastMath.atan2(py, px) - getTrueAnomaly();

        partialPV = pvCoordinates;

        if (reliableAcceleration) {
            // we have a relevant acceleration, we can compute derivatives

            final double[][] jacobian = new double[6][6];
            getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

            final Vector3D keplerianAcceleration    = new Vector3D(-mu / (r * r2), pvP);
            final Vector3D nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final double   aX                       = nonKeplerianAcceleration.getX();
            final double   aY                       = nonKeplerianAcceleration.getY();
            final double   aZ                       = nonKeplerianAcceleration.getZ();
            aDot    = jacobian[0][3] * aX + jacobian[0][4] * aY + jacobian[0][5] * aZ;
            eDot    = jacobian[1][3] * aX + jacobian[1][4] * aY + jacobian[1][5] * aZ;
            iDot    = jacobian[2][3] * aX + jacobian[2][4] * aY + jacobian[2][5] * aZ;
            paDot   = jacobian[3][3] * aX + jacobian[3][4] * aY + jacobian[3][5] * aZ;
            raanDot = jacobian[4][3] * aX + jacobian[4][4] * aY + jacobian[4][5] * aZ;

            // in order to compute cached anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to required anomaly
            final double MDot = getKeplerianMeanMotion() +
                    jacobian[5][3] * aX + jacobian[5][4] * aY + jacobian[5][5] * aZ;
            final UnivariateDerivative1 eUD = new UnivariateDerivative1(e, eDot);
            final UnivariateDerivative1 MUD = new UnivariateDerivative1(getMeanAnomaly(), MDot);
            if (cachedPositionAngleType == PositionAngleType.ECCENTRIC) {
                final UnivariateDerivative1 EUD = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(eUD, MUD) :
                        FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(eUD, MUD);
                cachedAnomalyDot = EUD.getFirstDerivative();
            } else { // TRUE
                final UnivariateDerivative1 vUD = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(eUD, MUD) :
                        FieldKeplerianAnomalyUtility.ellipticMeanToTrue(eUD, MUD);
                cachedAnomalyDot = vUD.getFirstDerivative();
            }

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            aDot    = 0.;
            eDot    = 0.;
            iDot    = 0.;
            paDot   = 0.;
            raanDot = 0.;
            cachedAnomalyDot = computeKeplerianAnomalyDot(cachedPositionAngleType, a, e, mu, cachedAnomaly, cachedPositionAngleType);
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public KeplerianOrbit(final PVCoordinates pvCoordinates,
                          final Frame frame, final AbsoluteDate date, final double mu)
            throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public KeplerianOrbit(final Orbit op) {
        this(op.getPVCoordinates(), op.getFrame(), op.getMu(), op.hasNonKeplerianAcceleration());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNonKeplerianAcceleration() {
        return aDot != 0. || eDot != 0. || paDot != 0. || iDot != 0. || raanDot != 0. ||
                FastMath.abs(cachedAnomalyDot - computeKeplerianAnomalyDot(cachedPositionAngleType, a, e, getMu(), cachedAnomaly, cachedPositionAngleType)) > TOLERANCE_POSITION_ANGLE_RATE;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
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
    public double getE() {
        return e;
    }

    /** {@inheritDoc} */
    @Override
    public double getEDot() {
        return eDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return i;
    }

    /** {@inheritDoc} */
    @Override
    public double getIDot() {
        return iDot;
    }

    /** Get the perigee argument.
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return pa;
    }

    /** Get the perigee argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return perigee argument derivative (rad/s)
     * @since 9.0
     */
    public double getPerigeeArgumentDot() {
        return paDot;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the right ascension of the ascending node derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return right ascension of the ascending node derivative (rad/s)
     * @since 9.0
     */
    public double getRightAscensionOfAscendingNodeDot() {
        return raanDot;
    }

    /** Get the true anomaly.
     * @return true anomaly (rad)
     */
    public double getTrueAnomaly() {
        switch (cachedPositionAngleType) {
            case MEAN: return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicMeanToTrue(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticMeanToTrue(e, cachedAnomaly);

            case TRUE: return cachedAnomaly;

            case ECCENTRIC: return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticEccentricToTrue(e, cachedAnomaly);

            default: throw new OrekitInternalError(null);
        }
    }

    /** Get the true anomaly derivative.
     * @return true anomaly derivative (rad/s)
     */
    public double getTrueAnomalyDot() {
        switch (cachedPositionAngleType) {
            case MEAN:
                final UnivariateDerivative1 eUD = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 MUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 vUD = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(eUD, MUD) :
                        FieldKeplerianAnomalyUtility.ellipticMeanToTrue(eUD, MUD);
                return vUD.getFirstDerivative();

            case TRUE:
                return cachedAnomalyDot;

            case ECCENTRIC:
                final UnivariateDerivative1 eUD2 = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 EUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 vUD2 = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicEccentricToTrue(eUD2, EUD) :
                        FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(eUD2, EUD);
                return vUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the eccentric anomaly.
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        switch (cachedPositionAngleType) {
            case MEAN:
                return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticMeanToEccentric(e, cachedAnomaly);

            case ECCENTRIC:
                return cachedAnomaly;

            case TRUE:
                return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicTrueToEccentric(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticTrueToEccentric(e, cachedAnomaly);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the eccentric anomaly derivative.
     * @return eccentric anomaly derivative (rad/s)
     * @since 9.0
     */
    public double getEccentricAnomalyDot() {
        switch (cachedPositionAngleType) {
            case ECCENTRIC:
                return cachedAnomalyDot;

            case TRUE:
                final UnivariateDerivative1 eUD = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 vUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 EUD = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicTrueToEccentric(eUD, vUD) :
                        FieldKeplerianAnomalyUtility.ellipticTrueToEccentric(eUD, vUD);
                return EUD.getFirstDerivative();

            case MEAN:
                final UnivariateDerivative1 eUD2 = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 MUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 EUD2 = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(eUD2, MUD) :
                        FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(eUD2, MUD);
                return EUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the mean anomaly.
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        switch (cachedPositionAngleType) {
            case ECCENTRIC: return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicEccentricToMean(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticEccentricToMean(e, cachedAnomaly);

            case MEAN: return cachedAnomaly;

            case TRUE: return (a < 0.) ? KeplerianAnomalyUtility.hyperbolicTrueToMean(e, cachedAnomaly) :
                    KeplerianAnomalyUtility.ellipticTrueToMean(e, cachedAnomaly);

            default: throw new OrekitInternalError(null);
        }
    }

    /** Get the mean anomaly derivative.
     * @return mean anomaly derivative (rad/s)
     * @since 9.0
     */
    public double getMeanAnomalyDot() {
        switch (cachedPositionAngleType) {
            case MEAN:
                return cachedAnomalyDot;

            case ECCENTRIC:
                final UnivariateDerivative1 eUD = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 EUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 MUD = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicEccentricToMean(eUD, EUD) :
                        FieldKeplerianAnomalyUtility.ellipticEccentricToMean(eUD, EUD);
                return MUD.getFirstDerivative();

            case TRUE:
                final UnivariateDerivative1 eUD2 = new UnivariateDerivative1(e, eDot);
                final UnivariateDerivative1 vUD = new UnivariateDerivative1(cachedAnomaly, cachedAnomalyDot);
                final UnivariateDerivative1 MUD2 = (a < 0) ?
                        FieldKeplerianAnomalyUtility.hyperbolicTrueToMean(eUD2, vUD) :
                        FieldKeplerianAnomalyUtility.ellipticTrueToMean(eUD2, vUD);
                return MUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the anomaly.
     * @param type type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getMeanAnomaly() :
                ((type == PositionAngleType.ECCENTRIC) ? getEccentricAnomaly() :
                        getTrueAnomaly());
    }

    /** Get the anomaly derivative.
     * @param type type of the angle
     * @return anomaly derivative (rad/s)
     * @since 9.0
     */
    public double getAnomalyDot(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getMeanAnomalyDot() :
                ((type == PositionAngleType.ECCENTRIC) ? getEccentricAnomalyDot() :
                        getTrueAnomalyDot());
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEx() {
        return e * FastMath.cos(pa + raan);
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialExDot() {
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
        final double paPraan = pa + raan;
        final SinCos sc      = FastMath.sinCos(paPraan);
        return eDot * sc.cos() - e * sc.sin() * (paDot + raanDot);
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEy() {
        return e * FastMath.sin(pa + raan);
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEyDot() {
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
        final double paPraan = pa + raan;
        final SinCos sc      = FastMath.sinCos(paPraan);
        return eDot * sc.sin() + e * sc.cos() * (paDot + raanDot);
    }

    /** {@inheritDoc} */
    @Override
    public double getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return FastMath.cos(raan) * FastMath.tan(0.5 * i);
    }

    /** {@inheritDoc} */
    @Override
    public double getHxDot() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
        final SinCos sc      = FastMath.sinCos(raan);
        final double tan     = FastMath.tan(0.5 * i);
        return 0.5 * (1 + tan * tan) * sc.cos() * iDot - tan * sc.sin() * raanDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return FastMath.sin(raan) * FastMath.tan(0.5 * i);
    }

    /** {@inheritDoc} */
    @Override
    public double getHyDot() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        if (!hasNonKeplerianAcceleration()) {
            return 0.;
        }
        final SinCos sc      = FastMath.sinCos(raan);
        final double tan     = FastMath.tan(0.5 * i);
        return 0.5 * (1 + tan * tan) * sc.sin() * iDot + tan * sc.cos() * raanDot;
    }

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        return pa + raan + getTrueAnomaly();
    }

    /** {@inheritDoc} */
    @Override
    public double getLvDot() {
        return paDot + raanDot + getTrueAnomalyDot();
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        return pa + raan + getEccentricAnomaly();
    }

    /** {@inheritDoc} */
    @Override
    public double getLEDot() {
        return paDot + raanDot + getEccentricAnomalyDot();
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        return pa + raan + getMeanAnomaly();
    }

    /** {@inheritDoc} */
    @Override
    public double getLMDot() {
        return paDot + raanDot + getMeanAnomalyDot();
    }

    /** Initialize cached anomaly with rate.
     * @param anomaly input anomaly
     * @param anomalyDot rate of input anomaly
     * @param inputType position angle type passed as input
     * @return anomaly to cache with rate
     * @since 12.1
     */
    private UnivariateDerivative1 initializeCachedAnomaly(final double anomaly, final double anomalyDot,
                                                          final PositionAngleType inputType) {
        if (cachedPositionAngleType == inputType) {
            return new UnivariateDerivative1(anomaly, anomalyDot);

        } else {
            final UnivariateDerivative1 eUD = new UnivariateDerivative1(e, eDot);
            final UnivariateDerivative1 anomalyUD = new UnivariateDerivative1(anomaly, anomalyDot);

            if (a < 0) {
                switch (cachedPositionAngleType) {
                    case MEAN:
                        if (inputType == PositionAngleType.ECCENTRIC) {
                            return FieldKeplerianAnomalyUtility.hyperbolicEccentricToMean(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.hyperbolicTrueToMean(eUD, anomalyUD);
                        }

                    case ECCENTRIC:
                        if (inputType == PositionAngleType.MEAN) {
                            return FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.hyperbolicTrueToEccentric(eUD, anomalyUD);
                        }

                    case TRUE:
                        if (inputType == PositionAngleType.MEAN) {
                            return FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.hyperbolicEccentricToTrue(eUD, anomalyUD);
                        }

                    default:
                        break;
                }

            } else {
                switch (cachedPositionAngleType) {
                    case MEAN:
                        if (inputType == PositionAngleType.ECCENTRIC) {
                            return FieldKeplerianAnomalyUtility.ellipticEccentricToMean(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.ellipticTrueToMean(eUD, anomalyUD);
                        }

                    case ECCENTRIC:
                        if (inputType == PositionAngleType.MEAN) {
                            return FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.ellipticTrueToEccentric(eUD, anomalyUD);
                        }

                    case TRUE:
                        if (inputType == PositionAngleType.MEAN) {
                            return FieldKeplerianAnomalyUtility.ellipticMeanToTrue(eUD, anomalyUD);
                        } else {
                            return FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(eUD, anomalyUD);
                        }

                    default:
                        break;
                }

            }
            throw new OrekitInternalError(null);
        }

    }

    /** Compute reference axes.
     * @return reference axes
     * @since 12.0
     */
    private Vector3D[] referenceAxes() {
        // preliminary variables
        final SinCos scRaan  = FastMath.sinCos(raan);
        final SinCos scPa    = FastMath.sinCos(pa);
        final SinCos scI     = FastMath.sinCos(i);
        final double cosRaan = scRaan.cos();
        final double sinRaan = scRaan.sin();
        final double cosPa   = scPa.cos();
        final double sinPa   = scPa.sin();
        final double cosI    = scI.cos();
        final double sinI    = scI.sin();

        final double crcp    = cosRaan * cosPa;
        final double crsp    = cosRaan * sinPa;
        final double srcp    = sinRaan * cosPa;
        final double srsp    = sinRaan * sinPa;

        // reference axes defining the orbital plane
        return new Vector3D[] {
            new Vector3D( crcp - cosI * srsp,  srcp + cosI * crsp, sinI * sinPa),
            new Vector3D(-crsp - cosI * srcp, -srsp + cosI * crcp, sinI * cosPa)
        };

    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        final Vector3D[] axes = referenceAxes();

        if (isElliptical()) {

            // elliptical case

            // elliptic eccentric anomaly
            final double uME2   = (1 - e) * (1 + e);
            final double s1Me2  = FastMath.sqrt(uME2);
            final SinCos scE    = FastMath.sinCos(getEccentricAnomaly());
            final double cosE   = scE.cos();
            final double sinE   = scE.sin();

            // coordinates of position and velocity in the orbital plane
            final double x      = a * (cosE - e);
            final double y      = a * sinE * s1Me2;
            final double factor = FastMath.sqrt(getMu() / a) / (1 - e * cosE);
            final double xDot   = -sinE * factor;
            final double yDot   =  cosE * s1Me2 * factor;

            final Vector3D position = new Vector3D(x, axes[0], y, axes[1]);
            final Vector3D velocity = new Vector3D(xDot, axes[0], yDot, axes[1]);
            partialPV = new PVCoordinates(position, velocity);

        } else {

            // hyperbolic case

            // compute position and velocity factors
            final SinCos scV       = FastMath.sinCos(getTrueAnomaly());
            final double sinV      = scV.sin();
            final double cosV      = scV.cos();
            final double f         = a * (1 - e * e);
            final double posFactor = f / (1 + e * cosV);
            final double velFactor = FastMath.sqrt(getMu() / f);

            final double   x            =  posFactor * cosV;
            final double   y            =  posFactor * sinV;
            final double   xDot         = -velFactor * sinV;
            final double   yDot         =  velFactor * (e + cosV);

            final Vector3D position = new Vector3D(x, axes[0], y, axes[1]);
            final Vector3D velocity = new Vector3D(xDot, axes[0], yDot, axes[1]);
            partialPV = new PVCoordinates(position, velocity);

        }

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * @return non-Keplerian part of the acceleration
     */
    private Vector3D nonKeplerianAcceleration() {

        final double[][] dCdP = new double[6][6];
        getJacobianWrtParameters(PositionAngleType.MEAN, dCdP);

        final double nonKeplerianMeanMotion = getMeanAnomalyDot() - getKeplerianMeanMotion();
        final double nonKeplerianAx = dCdP[3][0] * aDot    + dCdP[3][1] * eDot    + dCdP[3][2] * iDot    +
                dCdP[3][3] * paDot   + dCdP[3][4] * raanDot + dCdP[3][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAy = dCdP[4][0] * aDot    + dCdP[4][1] * eDot    + dCdP[4][2] * iDot    +
                dCdP[4][3] * paDot   + dCdP[4][4] * raanDot + dCdP[4][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAz = dCdP[5][0] * aDot    + dCdP[5][1] * eDot    + dCdP[5][2] * iDot    +
                dCdP[5][3] * paDot   + dCdP[5][4] * raanDot + dCdP[5][5] * nonKeplerianMeanMotion;

        return new Vector3D(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D initPosition() {

        final Vector3D[] axes = referenceAxes();

        if (isElliptical()) {

            // elliptical case

            // elliptic eccentric anomaly
            final double uME2   = (1 - e) * (1 + e);
            final double s1Me2  = FastMath.sqrt(uME2);
            final SinCos scE    = FastMath.sinCos(getEccentricAnomaly());
            final double cosE   = scE.cos();
            final double sinE   = scE.sin();

            return new Vector3D(a * (cosE - e), axes[0], a * sinE * s1Me2, axes[1]);

        } else {

            // hyperbolic case

            // compute position and velocity factors
            final SinCos scV       = FastMath.sinCos(getTrueAnomaly());
            final double sinV      = scV.sin();
            final double cosV      = scV.cos();
            final double f         = a * (1 - e * e);
            final double posFactor = f / (1 + e * cosV);

            return new Vector3D(posFactor * cosV, axes[0], posFactor * sinV, axes[1]);

        }

    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final double r2 = partialPV.getPosition().getNormSq();
        final Vector3D keplerianAcceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), partialPV.getPosition());
        final Vector3D acceleration = hasNonKeplerianAcceleration() ?
                keplerianAcceleration.add(nonKeplerianAcceleration()) :
                keplerianAcceleration;

        return new TimeStampedPVCoordinates(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit inFrame(final Frame inertialFrame) {
        final PVCoordinates pvCoordinates;
        if (hasNonKeplerianAcceleration()) {
            pvCoordinates = getPVCoordinates(inertialFrame);
        } else {
            final KinematicTransform transform = getFrame().getKinematicTransformTo(inertialFrame, getDate());
            pvCoordinates = transform.transformOnlyPV(getPVCoordinates());
        }
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(pvCoordinates, inertialFrame, getDate(), getMu());
        if (keplerianOrbit.getCachedPositionAngleType() == getCachedPositionAngleType()) {
            return keplerianOrbit;
        } else {
            return keplerianOrbit.withCachedPositionAngleType(getCachedPositionAngleType());
        }
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit withCachedPositionAngleType(final PositionAngleType positionAngleType) {
        return new KeplerianOrbit(a, e, i, pa, raan, getAnomaly(positionAngleType), aDot, eDot, iDot, paDot, raanDot,
                getAnomalyDot(positionAngleType), positionAngleType, getFrame(), getDate(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit shiftedBy(final double dt) {
        return shiftedBy(new TimeOffset(dt));
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit shiftedBy(final TimeOffset dt) {

        final double dtS = dt.toDouble();

        // use Keplerian-only motion
        final KeplerianOrbit keplerianShifted = new KeplerianOrbit(a, e, i, pa, raan,
                getMeanAnomaly() + getKeplerianMeanMotion() * dtS, PositionAngleType.MEAN,
                cachedPositionAngleType, getFrame(), getDate().shiftedBy(dt), getMu());

        if (hasNonKeplerianAcceleration()) {

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
            return new KeplerianOrbit(new TimeStampedPVCoordinates(keplerianShifted.getDate(),
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
        if (isElliptical()) {
            return computeJacobianMeanWrtCartesianElliptical();
        } else {
            return computeJacobianMeanWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianMeanWrtCartesianElliptical() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        computePVWithoutA();
        final Vector3D position = partialPV.getPosition();
        final Vector3D velocity = partialPV.getVelocity();
        final Vector3D momentum = partialPV.getMomentum();
        final double v2         = velocity.getNormSq();
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double r3         = r * r2;

        final double px         = position.getX();
        final double py         = position.getY();
        final double pz         = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double mx         = momentum.getX();
        final double my         = momentum.getY();
        final double mz         = momentum.getZ();

        final double mu         = getMu();
        final double sqrtMuA    = FastMath.sqrt(a * mu);
        final double sqrtAoMu   = FastMath.sqrt(a / mu);
        final double a2         = a * a;
        final double twoA       = 2 * a;
        final double rOnA       = r / a;

        final double oMe2       = 1 - e * e;
        final double epsilon    = FastMath.sqrt(oMe2);
        final double sqrtRec    = 1 / epsilon;

        final SinCos scI        = FastMath.sinCos(i);
        final SinCos scPA       = FastMath.sinCos(pa);
        final double cosI       = scI.cos();
        final double sinI       = scI.sin();
        final double cosPA      = scPA.cos();
        final double sinPA      = scPA.sin();

        final double pv         = Vector3D.dotProduct(position, velocity);
        final double cosE       = (a - r) / (a * e);
        final double sinE       = pv / (e * sqrtMuA);

        // da
        final Vector3D vectorAR = new Vector3D(2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(2 * a2 / mu);
        fillHalfRow(1, vectorAR,    jacobian[0], 0);
        fillHalfRow(1, vectorARDot, jacobian[0], 3);

        // de
        final double factorER3 = pv / twoA;
        final Vector3D vectorER   = new Vector3D(cosE * v2 / (r * mu), position,
                sinE / sqrtMuA, velocity,
                -factorER3 * sinE / sqrtMuA, vectorAR);
        final Vector3D vectorERDot = new Vector3D(sinE / sqrtMuA, position,
                cosE * 2 * r / mu, velocity,
                -factorER3 * sinE / sqrtMuA, vectorARDot);
        fillHalfRow(1, vectorER,    jacobian[1], 0);
        fillHalfRow(1, vectorERDot, jacobian[1], 3);

        // dE / dr (Eccentric anomaly)
        final double coefE = cosE / (e * sqrtMuA);
        final Vector3D  vectorEAnR =
                new Vector3D(-sinE * v2 / (e * r * mu), position, coefE, velocity,
                        -factorER3 * coefE, vectorAR);

        // dE / drDot
        final Vector3D  vectorEAnRDot =
                new Vector3D(-sinE * 2 * r / (e * mu), velocity, coefE, position,
                        -factorER3 * coefE, vectorARDot);

        // precomputing some more factors
        final double s1 = -sinE * pz / r - cosE * vz * sqrtAoMu;
        final double s2 = -cosE * pz / r3;
        final double s3 = -sinE * vz / (2 * sqrtMuA);
        final double t1 = sqrtRec * (cosE * pz / r - sinE * vz * sqrtAoMu);
        final double t2 = sqrtRec * (-sinE * pz / r3);
        final double t3 = sqrtRec * (cosE - e) * vz / (2 * sqrtMuA);
        final double t4 = sqrtRec * (e * sinI * cosPA * sqrtRec - vz * sqrtAoMu);
        final Vector3D s = new Vector3D(cosE / r, Vector3D.PLUS_K,
                s1,       vectorEAnR,
                s2,       position,
                s3,       vectorAR);
        final Vector3D sDot = new Vector3D(-sinE * sqrtAoMu, Vector3D.PLUS_K,
                s1,               vectorEAnRDot,
                s3,               vectorARDot);
        final Vector3D t =
                new Vector3D(sqrtRec * sinE / r, Vector3D.PLUS_K).add(new Vector3D(t1, vectorEAnR,
                        t2, position,
                        t3, vectorAR,
                        t4, vectorER));
        final Vector3D tDot = new Vector3D(sqrtRec * (cosE - e) * sqrtAoMu, Vector3D.PLUS_K,
                t1,                              vectorEAnRDot,
                t3,                              vectorARDot,
                t4,                              vectorERDot);

        // di
        final double factorI1 = -sinI * sqrtRec / sqrtMuA;
        final double i1 =  factorI1;
        final double i2 = -factorI1 * mz / twoA;
        final double i3 =  factorI1 * mz * e / oMe2;
        final double i4 = cosI * sinPA;
        final double i5 = cosI * cosPA;
        fillHalfRow(i1, new Vector3D(vy, -vx, 0), i2, vectorAR, i3, vectorER, i4, s, i5, t,
                jacobian[2], 0);
        fillHalfRow(i1, new Vector3D(-py, px, 0), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5, tDot,
                jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA / sinI, s,    -sinPA / sinI, t,    jacobian[3], 0);
        fillHalfRow(cosPA / sinI, sDot, -sinPA / sinI, tDot, jacobian[3], 3);

        // dRaan
        final double factorRaanR = 1 / (mu * a * oMe2 * sinI * sinI);
        fillHalfRow(-factorRaanR * my, new Vector3D(  0, vz, -vy),
                factorRaanR * mx, new Vector3D(-vz,  0,  vx),
                jacobian[4], 0);
        fillHalfRow(-factorRaanR * my, new Vector3D( 0, -pz,  py),
                factorRaanR * mx, new Vector3D(pz,   0, -px),
                jacobian[4], 3);

        // dM
        fillHalfRow(rOnA, vectorEAnR,    -sinE, vectorER,    jacobian[5], 0);
        fillHalfRow(rOnA, vectorEAnRDot, -sinE, vectorERDot, jacobian[5], 3);

        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianMeanWrtCartesianHyperbolic() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        computePVWithoutA();
        final Vector3D position = partialPV.getPosition();
        final Vector3D velocity = partialPV.getVelocity();
        final Vector3D momentum = partialPV.getMomentum();
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double r3         = r * r2;

        final double x          = position.getX();
        final double y          = position.getY();
        final double z          = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double mx         = momentum.getX();
        final double my         = momentum.getY();
        final double mz         = momentum.getZ();

        final double mu         = getMu();
        final double absA       = -a;
        final double sqrtMuA    = FastMath.sqrt(absA * mu);
        final double a2         = a * a;
        final double rOa        = r / absA;

        final SinCos scI        = FastMath.sinCos(i);
        final double cosI       = scI.cos();
        final double sinI       = scI.sin();

        final double pv         = Vector3D.dotProduct(position, velocity);

        // da
        final Vector3D vectorAR = new Vector3D(-2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(-2 * a2 / mu);
        fillHalfRow(-1, vectorAR,    jacobian[0], 0);
        fillHalfRow(-1, vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final double m      = momentum.getNorm();
        final double oOm    = 1 / m;
        final Vector3D dcXP = new Vector3D(  0,  vz, -vy);
        final Vector3D dcYP = new Vector3D(-vz,   0,  vx);
        final Vector3D dcZP = new Vector3D( vy, -vx,   0);
        final Vector3D dcXV = new Vector3D(  0,  -z,   y);
        final Vector3D dcYV = new Vector3D(  z,   0,  -x);
        final Vector3D dcZV = new Vector3D( -y,   x,   0);
        final Vector3D dCP  = new Vector3D(mx * oOm, dcXP, my * oOm, dcYP, mz * oOm, dcZP);
        final Vector3D dCV  = new Vector3D(mx * oOm, dcXV, my * oOm, dcYV, mz * oOm, dcZV);

        // dp
        final double mOMu   = m / mu;
        final Vector3D dpP  = new Vector3D(2 * mOMu, dCP);
        final Vector3D dpV  = new Vector3D(2 * mOMu, dCV);

        // de
        final double p      = m * mOMu;
        final double moO2ae = 1 / (2 * absA * e);
        final double m2OaMu = -p / absA;
        fillHalfRow(moO2ae, dpP, m2OaMu * moO2ae, vectorAR,    jacobian[1], 0);
        fillHalfRow(moO2ae, dpV, m2OaMu * moO2ae, vectorARDot, jacobian[1], 3);

        // di
        final double cI1 = 1 / (m * sinI);
        final double cI2 = cosI * cI1;
        fillHalfRow(cI2, dCP, -cI1, dcZP, jacobian[2], 0);
        fillHalfRow(cI2, dCV, -cI1, dcZV, jacobian[2], 3);

        // dPA
        final double cP1     =  y * oOm;
        final double cP2     = -x * oOm;
        final double cP3     = -(mx * cP1 + my * cP2);
        final double cP4     = cP3 * oOm;
        final double cP5     = -1 / (r2 * sinI * sinI);
        final double cP6     = z  * cP5;
        final double cP7     = cP3 * cP5;
        final Vector3D dacP  = new Vector3D(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new Vector3D(-my, mx, 0));
        final Vector3D dacV  = new Vector3D(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final Vector3D dpoP  = new Vector3D(cP6, dacP, cP7, Vector3D.PLUS_K);
        final Vector3D dpoV  = new Vector3D(cP6, dacV);

        final double re2     = r2 * e * e;
        final double recOre2 = (p - r) / re2;
        final double resOre2 = (pv * mOMu) / re2;
        final Vector3D dreP  = new Vector3D(mOMu, velocity, pv / mu, dCP);
        final Vector3D dreV  = new Vector3D(mOMu, position, pv / mu, dCV);
        final Vector3D davP  = new Vector3D(-resOre2, dpP, recOre2, dreP, resOre2 / r, position);
        final Vector3D davV  = new Vector3D(-resOre2, dpV, recOre2, dreV);
        fillHalfRow(1, dpoP, -1, davP, jacobian[3], 0);
        fillHalfRow(1, dpoV, -1, davV, jacobian[3], 3);

        // dRAAN
        final double cO0 = cI1 * cI1;
        final double cO1 =  mx * cO0;
        final double cO2 = -my * cO0;
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final double s2a    = pv / (2 * absA);
        final double oObux  = 1 / FastMath.sqrt(m * m + mu * absA);
        final double scasbu = pv * oObux;
        final Vector3D dauP = new Vector3D(1 / sqrtMuA, velocity, -s2a / sqrtMuA, vectorAR);
        final Vector3D dauV = new Vector3D(1 / sqrtMuA, position, -s2a / sqrtMuA, vectorARDot);
        final Vector3D dbuP = new Vector3D(oObux * mu / 2, vectorAR,    m * oObux, dCP);
        final Vector3D dbuV = new Vector3D(oObux * mu / 2, vectorARDot, m * oObux, dCV);
        final Vector3D dcuP = new Vector3D(oObux, velocity, -scasbu * oObux, dbuP);
        final Vector3D dcuV = new Vector3D(oObux, position, -scasbu * oObux, dbuV);
        fillHalfRow(1, dauP, -e / (1 + rOa), dcuP, jacobian[5], 0);
        fillHalfRow(1, dauV, -e / (1 + rOa), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        if (isElliptical()) {
            return computeJacobianEccentricWrtCartesianElliptical();
        } else {
            return computeJacobianEccentricWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianEccentricWrtCartesianElliptical() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesianElliptical();

        // Differentiating the Kepler equation M = E - e sin E leads to:
        // dM = (1 - e cos E) dE - sin E de
        // which is inverted and rewritten as:
        // dE = a/r dM + sin E a/r de
        final SinCos scE              = FastMath.sinCos(getEccentricAnomaly());
        final double aOr              = 1 / (1 - e * scE.cos());

        // update anomaly row
        final double[] eRow           = jacobian[1];
        final double[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr * (anomalyRow[j] + scE.sin() * eRow[j]);
        }

        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianEccentricWrtCartesianHyperbolic() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesianHyperbolic();

        // Differentiating the Kepler equation M = e sinh H - H leads to:
        // dM = (e cosh H - 1) dH + sinh H de
        // which is inverted and rewritten as:
        // dH = 1 / (e cosh H - 1) dM - sinh H / (e cosh H - 1) de
        final double H      = getEccentricAnomaly();
        final double coshH  = FastMath.cosh(H);
        final double sinhH  = FastMath.sinh(H);
        final double absaOr = 1 / (e * coshH - 1);

        // update anomaly row
        final double[] eRow       = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = absaOr * (anomalyRow[j] - sinhH * eRow[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        if (isElliptical()) {
            return computeJacobianTrueWrtCartesianElliptical();
        } else {
            return computeJacobianTrueWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianTrueWrtCartesianElliptical() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesianElliptical();

        // Differentiating the eccentric anomaly equation sin E = sqrt(1-e^2) sin v / (1 + e cos v)
        // and using cos E = (e + cos v) / (1 + e cos v) to get rid of cos E leads to:
        // dE = [sqrt (1 - e^2) / (1 + e cos v)] dv - [sin E / (1 - e^2)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dE + [sin E / sqrt (1 - e^2)] a/r de
        final double e2           = e * e;
        final double oMe2         = 1 - e2;
        final double epsilon      = FastMath.sqrt(oMe2);
        final SinCos scE          = FastMath.sinCos(getEccentricAnomaly());
        final double aOr          = 1 / (1 - e * scE.cos());
        final double aFactor      = epsilon * aOr;
        final double eFactor      = scE.sin() * aOr / epsilon;

        // update anomaly row
        final double[] eRow       = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] + eFactor * eRow[j];
        }

        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianTrueWrtCartesianHyperbolic() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesianHyperbolic();

        // Differentiating the eccentric anomaly equation sinh H = sqrt(e^2-1) sin v / (1 + e cos v)
        // and using cosh H = (e + cos v) / (1 + e cos v) to get rid of cosh H leads to:
        // dH = [sqrt (e^2 - 1) / (1 + e cos v)] dv + [sinh H / (e^2 - 1)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dH - [sinh H / sqrt (e^2 - 1)] a/r de
        final double e2       = e * e;
        final double e2Mo     = e2 - 1;
        final double epsilon  = FastMath.sqrt(e2Mo);
        final double H        = getEccentricAnomaly();
        final double coshH    = FastMath.cosh(H);
        final double sinhH    = FastMath.sinh(H);
        final double aOr      = 1 / (e * coshH - 1);
        final double aFactor  = epsilon * aOr;
        final double eFactor  = sinhH * aOr / epsilon;

        // update anomaly row
        final double[] eRow           = jacobian[1];
        final double[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] - eFactor * eRow[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public void addKeplerContribution(final PositionAngleType type, final double gm,
                                      final double[] pDot) {
        pDot[5] += computeKeplerianAnomalyDot(type, a, e, gm, cachedAnomaly, cachedPositionAngleType);
    }

    /**
     * Compute rate of argument of latitude.
     * @param type position angle type of rate
     * @param a semi major axis
     * @param e eccentricity
     * @param mu mu
     * @param anomaly anomaly
     * @param cachedType position angle type of passed anomaly
     * @return first-order time derivative for anomaly
     * @since 12.2
     */
    private static double computeKeplerianAnomalyDot(final PositionAngleType type, final double a, final double e,
                                                     final double mu, final double anomaly, final PositionAngleType cachedType) {
        final double absA = FastMath.abs(a);
        final double n    = FastMath.sqrt(mu / absA) / absA;
        if (type == PositionAngleType.MEAN) {
            return n;
        }
        final double oMe2 = FastMath.abs(1 - e * e);
        final double ksi = 1 + e * FastMath.cos(KeplerianAnomalyUtility.convertAnomaly(cachedType, anomaly, e, PositionAngleType.TRUE));
        if (type == PositionAngleType.ECCENTRIC) {
            return n * ksi / oMe2;
        } else { // TRUE
            return n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
        }
    }

    /**  Returns a string representation of this Keplerian parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuilder().append("Keplerian parameters: ").append('{').
                append("a: ").append(a).
                append("; e: ").append(e).
                append("; i: ").append(FastMath.toDegrees(i)).
                append("; pa: ").append(FastMath.toDegrees(pa)).
                append("; raan: ").append(FastMath.toDegrees(raan)).
                append("; v: ").append(FastMath.toDegrees(getTrueAnomaly())).
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
    public KeplerianOrbit withKeplerianRates() {
        final PositionAngleType positionAngleType = getCachedPositionAngleType();
        return new KeplerianOrbit(a, e, i, pa, raan, cachedAnomaly, positionAngleType, positionAngleType,
                getFrame(), getDate(), getMu());
    }

    /** Check if the given parameter is within an acceptable range.
     * The bounds are inclusive: an exception is raised when either of those conditions are met:
     * <ul>
     *     <li>The parameter is strictly greater than upperBound</li>
     *     <li>The parameter is strictly lower than lowerBound</li>
     * </ul>
     * <p>
     * In either of these cases, an OrekitException is raised.
     * </p>
     * @param parameterName name of the parameter
     * @param parameter value of the parameter
     * @param lowerBound lower bound of the acceptable range (inclusive)
     * @param upperBound upper bound of the acceptable range (inclusive)
     */
    private void checkParameterRangeInclusive(final String parameterName, final double parameter,
                                              final double lowerBound, final double upperBound) {
        if (parameter < lowerBound || parameter > upperBound) {
            throw new OrekitException(OrekitMessages.INVALID_PARAMETER_RANGE, parameterName,
                    parameter, lowerBound, upperBound);
        }
    }

}
