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


import java.lang.reflect.Array;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldKinematicTransform;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


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
 * @author Andrea Antolino
 * @since 9.0
 * @param <T> type of the field elements
 */
public class FieldKeplerianOrbit<T extends CalculusFieldElement<T>> extends FieldOrbit<T>
        implements PositionAngleBased {

    /** Name of the eccentricity parameter. */
    private static final String ECCENTRICITY = "eccentricity";

    /** Semi-major axis (m). */
    private final T a;

    /** Eccentricity. */
    private final T e;

    /** Inclination (rad). */
    private final T i;

    /** Perigee Argument (rad). */
    private final T pa;

    /** Right Ascension of Ascending Node (rad). */
    private final T raan;

    /** Cached anomaly (rad). */
    private final T cachedAnomaly;

    /** Semi-major axis derivative (m/s). */
    private final T aDot;

    /** Eccentricity derivative. */
    private final T eDot;

    /** Inclination derivative (rad/s). */
    private final T iDot;

    /** Perigee Argument derivative (rad/s). */
    private final T paDot;

    /** Right Ascension of Ascending Node derivative (rad/s). */
    private final T raanDot;

    /** Derivative of cached anomaly (rad/s). */
    private final T cachedAnomalyDot;

    /** Cached type of position angle. */
    private final PositionAngleType cachedPositionAngleType;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private FieldPVCoordinates<T> partialPV;

    /** PThird Canonical Vector. */
    private final FieldVector3D<T> PLUS_K;

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
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                               final T pa, final T raan,
                               final T anomaly, final PositionAngleType type,
                               final PositionAngleType cachedPositionAngleType,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly,
             a.getField().getZero(), a.getField().getZero(), a.getField().getZero(), a.getField().getZero(), a.getField().getZero(),
             computeKeplerianAnomalyDot(type, a, e, mu, anomaly, type), type, cachedPositionAngleType, frame, date, mu);
    }

    /** Creates a new instance.
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
     * @since 12.1
     */
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                               final T pa, final T raan,
                               final T anomaly, final PositionAngleType type,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
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
     * @param aDot  semi-major axis derivative, null if unknown (m/s)
     * @param eDot eccentricity derivative, null if unknown
     * @param iDot inclination derivative, null if unknown (rad/s)
     * @param paDot perigee argument derivative, null if unknown (rad/s)
     * @param raanDot right ascension of ascending node derivative, null if unknown (rad/s)
     * @param anomalyDot mean, eccentric or true anomaly derivative, null if unknown (rad/s)
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
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                               final T pa, final T raan, final T anomaly,
                               final T aDot, final T eDot, final T iDot,
                               final T paDot, final T raanDot, final T anomalyDot,
                               final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        this.cachedPositionAngleType = cachedPositionAngleType;

        if (a.multiply(e.negate().add(1)).getReal() < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a.getReal(), e.getReal());
        }

        // Checking eccentricity range
        checkParameterRangeInclusive(ECCENTRICITY, e.getReal(), 0.0, Double.POSITIVE_INFINITY);

        this.a       =    a;
        this.aDot    =    aDot;
        this.e       =    e;
        this.eDot    =    eDot;
        this.i       =    i;
        this.iDot    =    iDot;
        this.pa      =    pa;
        this.paDot   =    paDot;
        this.raan    =    raan;
        this.raanDot =    raanDot;

        this.PLUS_K = FieldVector3D.getPlusK(a.getField());  // third canonical vector

        final FieldUnivariateDerivative1<T> cachedAnomalyUD = initializeCachedAnomaly(anomaly, anomalyDot, type);
        this.cachedAnomaly = cachedAnomalyUD.getValue();
        this.cachedAnomalyDot = cachedAnomalyUD.getFirstDerivative();

        // check true anomaly range
        if (!isElliptical()) {
            final T trueAnomaly = getTrueAnomaly();
            if (e.multiply(trueAnomaly.cos()).add(1).getReal() <= 0) {
                final double vMax = e.reciprocal().negate().acos().getReal();
                throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE,
                        trueAnomaly.getReal(), e.getReal(), -vMax, vMax);
            }
        }

        this.partialPV = null;

    }

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity (positive or equal to 0)
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param aDot  semi-major axis derivative, null if unknown (m/s)
     * @param eDot eccentricity derivative, null if unknown
     * @param iDot inclination derivative, null if unknown (rad/s)
     * @param paDot perigee argument derivative, null if unknown (rad/s)
     * @param raanDot right ascension of ascending node derivative, null if unknown (rad/s)
     * @param anomalyDot mean, eccentric or true anomaly derivative, null if unknown (rad/s)
     * @param type type of anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     */
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                               final T pa, final T raan, final T anomaly,
                               final T aDot, final T eDot, final T iDot,
                               final T paDot, final T raanDot, final T anomalyDot,
                               final PositionAngleType type,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
            throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly, aDot, eDot, iDot, paDot, raanDot, anomalyDot,
                type, type, frame, date, mu);
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldKeplerianOrbit(final TimeStampedFieldPVCoordinates<T> pvCoordinates,
                               final Frame frame, final T mu)
        throws IllegalArgumentException {
        this(pvCoordinates, frame, mu, hasNonKeplerianAcceleration(pvCoordinates, mu));
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @param reliableAcceleration if true, the acceleration is considered to be reliable
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    private FieldKeplerianOrbit(final TimeStampedFieldPVCoordinates<T> pvCoordinates,
                                final Frame frame, final T mu,
                                final boolean reliableAcceleration)
        throws IllegalArgumentException {

        super(pvCoordinates, frame, mu);

        // third canonical vector
        this.PLUS_K = FieldVector3D.getPlusK(getOne().getField());

        // compute inclination
        final FieldVector3D<T> momentum = pvCoordinates.getMomentum();
        final T m2 = momentum.getNormSq();

        i = FieldVector3D.angle(momentum, PLUS_K);
        // compute right ascension of ascending node
        raan = FieldVector3D.crossProduct(PLUS_K, momentum).getAlpha();
        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final FieldVector3D<T> pvP     = pvCoordinates.getPosition();
        final FieldVector3D<T> pvV     = pvCoordinates.getVelocity();
        final FieldVector3D<T> pvA     = pvCoordinates.getAcceleration();

        final T   r2      = pvP.getNormSq();
        final T   r       = r2.sqrt();
        final T   V2      = pvV.getNormSq();
        final T   rV2OnMu = r.multiply(V2).divide(mu);

        // compute semi-major axis (will be negative for hyperbolic orbits)
        a = r.divide(rV2OnMu.negate().add(2.0));
        final T muA = a.multiply(mu);

        // compute true anomaly
        if (isElliptical()) {
            // elliptic or circular orbit
            final T eSE = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE = rV2OnMu.subtract(1);
            e = (eSE.multiply(eSE).add(eCE.multiply(eCE))).sqrt();
            this.cachedPositionAngleType = PositionAngleType.ECCENTRIC;
            cachedAnomaly = eSE.atan2(eCE);
        } else {
            // hyperbolic orbit
            final T eSH = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
            final T eCH = rV2OnMu.subtract(1);
            e = (m2.negate().divide(muA).add(1)).sqrt();
            this.cachedPositionAngleType = PositionAngleType.TRUE;
            cachedAnomaly = FieldKeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, (eCH.add(eSH)).divide(eCH.subtract(eSH)).log().divide(2));
        }

        // Checking eccentricity range
        checkParameterRangeInclusive(ECCENTRICITY, e.getReal(), 0.0, Double.POSITIVE_INFINITY);

        // compute perigee argument
        final FieldVector3D<T> node = new FieldVector3D<>(raan, getZero());
        final T px = FieldVector3D.dotProduct(pvP, node);
        final T py = FieldVector3D.dotProduct(pvP, FieldVector3D.crossProduct(momentum, node)).divide(m2.sqrt());
        pa = py.atan2(px).subtract(getTrueAnomaly());

        partialPV = pvCoordinates;

        if (reliableAcceleration) {
            // we have a relevant acceleration, we can compute derivatives

            final T[][] jacobian = MathArrays.buildArray(a.getField(), 6, 6);
            getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

            final FieldVector3D<T> keplerianAcceleration    = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(mu.negate()), pvP);
            final FieldVector3D<T> nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final T   aX                       = nonKeplerianAcceleration.getX();
            final T   aY                       = nonKeplerianAcceleration.getY();
            final T   aZ                       = nonKeplerianAcceleration.getZ();
            aDot    = jacobian[0][3].multiply(aX).add(jacobian[0][4].multiply(aY)).add(jacobian[0][5].multiply(aZ));
            eDot    = jacobian[1][3].multiply(aX).add(jacobian[1][4].multiply(aY)).add(jacobian[1][5].multiply(aZ));
            iDot    = jacobian[2][3].multiply(aX).add(jacobian[2][4].multiply(aY)).add(jacobian[2][5].multiply(aZ));
            paDot   = jacobian[3][3].multiply(aX).add(jacobian[3][4].multiply(aY)).add(jacobian[3][5].multiply(aZ));
            raanDot = jacobian[4][3].multiply(aX).add(jacobian[4][4].multiply(aY)).add(jacobian[4][5].multiply(aZ));

            // in order to compute true anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to true anomaly
            final T MDot = getKeplerianMeanMotion().
                           add(jacobian[5][3].multiply(aX)).add(jacobian[5][4].multiply(aY)).add(jacobian[5][5].multiply(aZ));
            final FieldUnivariateDerivative1<T> eUD = new FieldUnivariateDerivative1<>(e, eDot);
            final FieldUnivariateDerivative1<T> MUD = new FieldUnivariateDerivative1<>(getMeanAnomaly(), MDot);
            if (cachedPositionAngleType == PositionAngleType.ECCENTRIC) {
                final FieldUnivariateDerivative1<T> EUD = (a.getReal() < 0) ?
                         FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(eUD, MUD) :
                         FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(eUD, MUD);
                cachedAnomalyDot = EUD.getFirstDerivative();
            } else { // TRUE
                final FieldUnivariateDerivative1<T> vUD = (a.getReal() < 0) ?
                         FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(eUD, MUD) :
                         FieldKeplerianAnomalyUtility.ellipticMeanToTrue(eUD, MUD);
                cachedAnomalyDot = vUD.getFirstDerivative();
            }

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot    = getZero();
            eDot    = getZero();
            iDot    = getZero();
            paDot   = getZero();
            raanDot = getZero();
            cachedAnomalyDot    = computeKeplerianAnomalyDot(cachedPositionAngleType, a, e, mu, cachedAnomaly, cachedPositionAngleType);
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param FieldPVCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldKeplerianOrbit(final FieldPVCoordinates<T> FieldPVCoordinates,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<>(date, FieldPVCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldKeplerianOrbit(final FieldOrbit<T> op) {
        this(op.getPVCoordinates(), op.getFrame(), op.getMu());
    }

    /** Constructor from Field and KeplerianOrbit.
     * <p>Build a FieldKeplerianOrbit from non-Field KeplerianOrbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldKeplerianOrbit(final Field<T> field, final KeplerianOrbit op) {
        this(field.getZero().newInstance(op.getA()), field.getZero().newInstance(op.getE()), field.getZero().newInstance(op.getI()),
                field.getZero().newInstance(op.getPerigeeArgument()), field.getZero().newInstance(op.getRightAscensionOfAscendingNode()),
                field.getZero().newInstance(op.getAnomaly(op.getCachedPositionAngleType())),
                field.getZero().newInstance(op.getADot()),
                field.getZero().newInstance(op.getEDot()),
                field.getZero().newInstance(op.getIDot()),
                field.getZero().newInstance(op.getPerigeeArgumentDot()),
                field.getZero().newInstance(op.getRightAscensionOfAscendingNodeDot()),
                field.getZero().newInstance(op.getAnomalyDot(op.getCachedPositionAngleType())),
                op.getCachedPositionAngleType(), op.getFrame(),
                new FieldAbsoluteDate<>(field, op.getDate()), field.getZero().newInstance(op.getMu()));
    }

    /** Constructor from Field and Orbit.
     * <p>Build a FieldKeplerianOrbit from any non-Field Orbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldKeplerianOrbit(final Field<T> field, final Orbit op) {
        this(field, (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(op));
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    @Override
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public T getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    @Override
    public T getE() {
        return e;
    }

    /** {@inheritDoc} */
    @Override
    public T getEDot() {
        return eDot;
    }

    /** {@inheritDoc} */
    @Override
    public T getI() {
        return i;
    }

    /** {@inheritDoc} */
    @Override
    public T getIDot() {
        return iDot;
    }

    /** Get the perigee argument.
     * @return perigee argument (rad)
     */
    public T getPerigeeArgument() {
        return pa;
    }

    /** Get the perigee argument derivative.
     * @return perigee argument derivative (rad/s)
     */
    public T getPerigeeArgumentDot() {
        return paDot;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public T getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the right ascension of the ascending node derivative.
     * @return right ascension of the ascending node derivative (rad/s)
     */
    public T getRightAscensionOfAscendingNodeDot() {
        return raanDot;
    }

    /** Get the true anomaly.
     * @return true anomaly (rad)
     */
    public T getTrueAnomaly() {
        switch (cachedPositionAngleType) {
            case MEAN: return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticMeanToTrue(e, cachedAnomaly);

            case TRUE: return cachedAnomaly;

            case ECCENTRIC: return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, cachedAnomaly);

            default: throw new OrekitInternalError(null);
        }
    }

    /** Get the true anomaly derivative.
     * @return true anomaly derivative (rad/s)
     */
    public T getTrueAnomalyDot() {
        switch (cachedPositionAngleType) {
            case MEAN:
                final FieldUnivariateDerivative1<T> eUD = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> MUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> vUD = (a.getReal() < 0) ?
                         FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(eUD, MUD) :
                         FieldKeplerianAnomalyUtility.ellipticMeanToTrue(eUD, MUD);
                return vUD.getFirstDerivative();

            case TRUE:
                return cachedAnomalyDot;

            case ECCENTRIC:
                final FieldUnivariateDerivative1<T> eUD2 = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> EUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> vUD2 = (a.getReal() < 0) ?
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
    public T getEccentricAnomaly() {
        switch (cachedPositionAngleType) {
            case MEAN:
                return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, cachedAnomaly);

            case ECCENTRIC:
                return cachedAnomaly;

            case TRUE:
                return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicTrueToEccentric(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticTrueToEccentric(e, cachedAnomaly);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the eccentric anomaly derivative.
     * @return eccentric anomaly derivative (rad/s)
     */
    public T getEccentricAnomalyDot() {
        switch (cachedPositionAngleType) {
            case ECCENTRIC:
                return cachedAnomalyDot;

            case TRUE:
                final FieldUnivariateDerivative1<T> eUD = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> vUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> EUD = (a.getReal() < 0) ?
                         FieldKeplerianAnomalyUtility.hyperbolicTrueToEccentric(eUD, vUD) :
                         FieldKeplerianAnomalyUtility.ellipticTrueToEccentric(eUD, vUD);
                return EUD.getFirstDerivative();

            case MEAN:
                final FieldUnivariateDerivative1<T> eUD2 = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> MUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> EUD2 = (a.getReal() < 0) ?
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
    public T getMeanAnomaly() {
        switch (cachedPositionAngleType) {
            case ECCENTRIC: return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicEccentricToMean(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticEccentricToMean(e, cachedAnomaly);

            case MEAN: return cachedAnomaly;

            case TRUE: return (a.getReal() < 0) ? FieldKeplerianAnomalyUtility.hyperbolicTrueToMean(e, cachedAnomaly) :
                    FieldKeplerianAnomalyUtility.ellipticTrueToMean(e, cachedAnomaly);

            default: throw new OrekitInternalError(null);
        }
    }

    /** Get the mean anomaly derivative.

     * @return mean anomaly derivative (rad/s)
     */
    public T getMeanAnomalyDot() {
        switch (cachedPositionAngleType) {
            case MEAN:
                return cachedAnomalyDot;

            case ECCENTRIC:
                final FieldUnivariateDerivative1<T> eUD = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> EUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> MUD = (a.getReal() < 0) ?
                         FieldKeplerianAnomalyUtility.hyperbolicEccentricToMean(eUD, EUD) :
                         FieldKeplerianAnomalyUtility.ellipticEccentricToMean(eUD, EUD);
                return MUD.getFirstDerivative();

            case TRUE:
                final FieldUnivariateDerivative1<T> eUD2 = new FieldUnivariateDerivative1<>(e, eDot);
                final FieldUnivariateDerivative1<T> vUD = new FieldUnivariateDerivative1<>(cachedAnomaly, cachedAnomalyDot);
                final FieldUnivariateDerivative1<T> MUD2 = (a.getReal() < 0) ?
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
    public T getAnomaly(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getMeanAnomaly() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getEccentricAnomaly() :
                                                                                   getTrueAnomaly());
    }

    /** Get the anomaly derivative.
     * @param type type of the angle
     * @return anomaly derivative (rad/s)
     */
    public T getAnomalyDot(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getMeanAnomalyDot() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getEccentricAnomalyDot() :
                                                                                   getTrueAnomalyDot());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNonKeplerianAcceleration() {
        return aDot.getReal() != 0. || eDot.getReal() != 0 || iDot.getReal() != 0. || raanDot.getReal() != 0. || paDot.getReal() != 0. ||
                FastMath.abs(cachedAnomalyDot.subtract(computeKeplerianAnomalyDot(cachedPositionAngleType, a, e, getMu(), cachedAnomaly, cachedPositionAngleType)).getReal()) > TOLERANCE_POSITION_ANGLE_RATE;
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEx() {
        return e.multiply(pa.add(raan).cos());
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialExDot() {
        if (!hasNonKeplerianRates()) {
            return getZero();
        }
        final FieldUnivariateDerivative1<T> eUD    = new FieldUnivariateDerivative1<>(e,    eDot);
        final FieldUnivariateDerivative1<T> paUD   = new FieldUnivariateDerivative1<>(pa,   paDot);
        final FieldUnivariateDerivative1<T> raanUD = new FieldUnivariateDerivative1<>(raan, raanDot);
        return eUD.multiply(paUD.add(raanUD).cos()).getFirstDerivative();

    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEy() {
        return e.multiply((pa.add(raan)).sin());
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEyDot() {
        if (!hasNonKeplerianRates()) {
            return getZero();
        }
        final FieldUnivariateDerivative1<T> eUD    = new FieldUnivariateDerivative1<>(e,    eDot);
        final FieldUnivariateDerivative1<T> paUD   = new FieldUnivariateDerivative1<>(pa,   paDot);
        final FieldUnivariateDerivative1<T> raanUD = new FieldUnivariateDerivative1<>(raan, raanDot);
        return eUD.multiply(paUD.add(raanUD).sin()).getFirstDerivative();

    }

    /** {@inheritDoc} */
    @Override
    public T getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        return raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    @Override
    public T getHxDot() {

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        if (!hasNonKeplerianRates()) {
            return getZero();
        }

        final FieldUnivariateDerivative1<T> iUD    = new FieldUnivariateDerivative1<>(i,    iDot);
        final FieldUnivariateDerivative1<T> raanUD = new FieldUnivariateDerivative1<>(raan, raanDot);
        return raanUD.cos().multiply(iUD.multiply(0.5).tan()).getFirstDerivative();

    }

    /** {@inheritDoc} */
    @Override
    public T getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        return raan.sin().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    @Override
    public T getHyDot() {

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        if (!hasNonKeplerianRates()) {
            return getZero();
        }

        final FieldUnivariateDerivative1<T> iUD    = new FieldUnivariateDerivative1<>(i,    iDot);
        final FieldUnivariateDerivative1<T> raanUD = new FieldUnivariateDerivative1<>(raan, raanDot);
        return raanUD.sin().multiply(iUD.multiply(0.5).tan()).getFirstDerivative();

    }

    /** {@inheritDoc} */
    @Override
    public T getLv() {
        return pa.add(raan).add(getTrueAnomaly());
    }

    /** {@inheritDoc} */
    @Override
    public T getLvDot() {
        return paDot.add(raanDot).add(getTrueAnomalyDot());
    }

    /** {@inheritDoc} */
    @Override
    public T getLE() {
        return pa.add(raan).add(getEccentricAnomaly());
    }

    /** {@inheritDoc} */
    @Override
    public T getLEDot() {
        return paDot.add(raanDot).add(getEccentricAnomalyDot());
    }

    /** {@inheritDoc} */
    @Override
    public T getLM() {
        return pa.add(raan).add(getMeanAnomaly());
    }

    /** {@inheritDoc} */
    @Override
    public T getLMDot() {
        return paDot.add(raanDot).add(getMeanAnomalyDot());
    }

    /** Initialize cached anomaly with rate.
     * @param anomaly input anomaly
     * @param anomalyDot rate of input anomaly
     * @param inputType position angle type passed as input
     * @return anomaly to cache with rate
     * @since 12.1
     */
    private FieldUnivariateDerivative1<T> initializeCachedAnomaly(final T anomaly, final T anomalyDot,
                                                                  final PositionAngleType inputType) {
        if (cachedPositionAngleType == inputType) {
            return new FieldUnivariateDerivative1<>(anomaly, anomalyDot);

        } else {
            final FieldUnivariateDerivative1<T> eUD = new FieldUnivariateDerivative1<>(e, eDot);
            final FieldUnivariateDerivative1<T> anomalyUD = new FieldUnivariateDerivative1<>(anomaly, anomalyDot);

            if (a.getReal() < 0) {
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

    /** Initialize cached anomaly.
     * @param anomaly input anomaly
     * @param inputType position angle type passed as input
     * @return anomaly to cache
     * @since 12.1
     */
    private T initializeCachedAnomaly(final T anomaly, final PositionAngleType inputType) {
        return FieldKeplerianAnomalyUtility.convertAnomaly(inputType, anomaly, e, cachedPositionAngleType);
    }

    /** Compute reference axes.
     * @return reference axes
     * @since 12.0
     */
    private FieldVector3D<T>[] referenceAxes() {

        // preliminary variables
        final FieldSinCos<T> scRaan = FastMath.sinCos(raan);
        final FieldSinCos<T> scPa   = FastMath.sinCos(pa);
        final FieldSinCos<T> scI    = FastMath.sinCos(i);
        final T cosRaan = scRaan.cos();
        final T sinRaan = scRaan.sin();
        final T cosPa   = scPa.cos();
        final T sinPa   = scPa.sin();
        final T cosI    = scI.cos();
        final T sinI    = scI.sin();
        final T crcp    = cosRaan.multiply(cosPa);
        final T crsp    = cosRaan.multiply(sinPa);
        final T srcp    = sinRaan.multiply(cosPa);
        final T srsp    = sinRaan.multiply(sinPa);

        // reference axes defining the orbital plane
        @SuppressWarnings("unchecked")
        final FieldVector3D<T>[] axes = (FieldVector3D<T>[]) Array.newInstance(FieldVector3D.class, 2);
        axes[0] = new FieldVector3D<>(crcp.subtract(cosI.multiply(srsp)),  srcp.add(cosI.multiply(crsp)), sinI.multiply(sinPa));
        axes[1] = new FieldVector3D<>(crsp.add(cosI.multiply(srcp)).negate(), cosI.multiply(crcp).subtract(srsp), sinI.multiply(cosPa));

        return axes;

    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        final FieldVector3D<T>[] axes = referenceAxes();

        if (isElliptical()) {

            // elliptical case

            // elliptic eccentric anomaly
            final T uME2             = e.negate().add(1).multiply(e.add(1));
            final T s1Me2            = uME2.sqrt();
            final FieldSinCos<T> scE = FastMath.sinCos(getEccentricAnomaly());
            final T cosE             = scE.cos();
            final T sinE             = scE.sin();

            // coordinates of position and velocity in the orbital plane
            final T x      = a.multiply(cosE.subtract(e));
            final T y      = a.multiply(sinE).multiply(s1Me2);
            final T factor = FastMath.sqrt(getMu().divide(a)).divide(e.negate().multiply(cosE).add(1));
            final T xDot   = sinE.negate().multiply(factor);
            final T yDot   = cosE.multiply(s1Me2).multiply(factor);

            final FieldVector3D<T> position = new FieldVector3D<>(x, axes[0], y, axes[1]);
            final FieldVector3D<T> velocity = new FieldVector3D<>(xDot, axes[0], yDot, axes[1]);
            partialPV = new FieldPVCoordinates<>(position, velocity);

        } else {

            // hyperbolic case

            // compute position and velocity factors
            final FieldSinCos<T> scV = FastMath.sinCos(getTrueAnomaly());
            final T sinV             = scV.sin();
            final T cosV             = scV.cos();
            final T f                = a.multiply(e.square().negate().add(1));
            final T posFactor        = f.divide(e.multiply(cosV).add(1));
            final T velFactor        = FastMath.sqrt(getMu().divide(f));

            final FieldVector3D<T> position     = new FieldVector3D<>(posFactor.multiply(cosV), axes[0], posFactor.multiply(sinV), axes[1]);
            final FieldVector3D<T> velocity     = new FieldVector3D<>(velFactor.multiply(sinV).negate(), axes[0], velFactor.multiply(e.add(cosV)), axes[1]);
            partialPV = new FieldPVCoordinates<>(position, velocity);

        }

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * @return non-Keplerian part of the acceleration
     */
    private FieldVector3D<T> nonKeplerianAcceleration() {

        final T[][] dCdP = MathArrays.buildArray(a.getField(), 6, 6);
        getJacobianWrtParameters(PositionAngleType.MEAN, dCdP);

        final T nonKeplerianMeanMotion = getMeanAnomalyDot().subtract(getKeplerianMeanMotion());
        final T nonKeplerianAx =     dCdP[3][0].multiply(aDot).
                                 add(dCdP[3][1].multiply(eDot)).
                                 add(dCdP[3][2].multiply(iDot)).
                                 add(dCdP[3][3].multiply(paDot)).
                                 add(dCdP[3][4].multiply(raanDot)).
                                 add(dCdP[3][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAy =     dCdP[4][0].multiply(aDot).
                                 add(dCdP[4][1].multiply(eDot)).
                                 add(dCdP[4][2].multiply(iDot)).
                                 add(dCdP[4][3].multiply(paDot)).
                                 add(dCdP[4][4].multiply(raanDot)).
                                 add(dCdP[4][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAz =     dCdP[5][0].multiply(aDot).
                                 add(dCdP[5][1].multiply(eDot)).
                                 add(dCdP[5][2].multiply(iDot)).
                                 add(dCdP[5][3].multiply(paDot)).
                                 add(dCdP[5][4].multiply(raanDot)).
                                 add(dCdP[5][5].multiply(nonKeplerianMeanMotion));

        return new FieldVector3D<>(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    @Override
    protected FieldVector3D<T> initPosition() {
        final FieldVector3D<T>[] axes = referenceAxes();

        if (isElliptical()) {

            // elliptical case

            // elliptic eccentric anomaly
            final T uME2             = e.negate().add(1).multiply(e.add(1));
            final T s1Me2            = uME2.sqrt();
            final FieldSinCos<T> scE = FastMath.sinCos(getEccentricAnomaly());
            final T cosE             = scE.cos();
            final T sinE             = scE.sin();

            return new FieldVector3D<>(a.multiply(cosE.subtract(e)), axes[0], a.multiply(sinE).multiply(s1Me2), axes[1]);

        } else {

            // hyperbolic case

            // compute position and velocity factors
            final FieldSinCos<T> scV = FastMath.sinCos(getTrueAnomaly());
            final T sinV             = scV.sin();
            final T cosV             = scV.cos();
            final T f                = a.multiply(e.square().negate().add(1));
            final T posFactor        = f.divide(e.multiply(cosV).add(1));

            return new FieldVector3D<>(posFactor.multiply(cosV), axes[0], posFactor.multiply(sinV), axes[1]);

        }

    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final T r2 = partialPV.getPosition().getNormSq();
        final FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r2.multiply(FastMath.sqrt(r2)).reciprocal().multiply(getMu().negate()),
                                                                           partialPV.getPosition());
        final FieldVector3D<T> acceleration = hasNonKeplerianRates() ?
                                              keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                              keplerianAcceleration;

        return new TimeStampedFieldPVCoordinates<>(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public FieldKeplerianOrbit<T> withFrame(final Frame inertialFrame) {
        if (hasNonKeplerianAcceleration()) {
            return new FieldKeplerianOrbit<>(getPVCoordinates(inertialFrame), inertialFrame, getMu());
        } else {
            final FieldKinematicTransform<T> transform = getFrame().getKinematicTransformTo(inertialFrame, getDate());
            return new FieldKeplerianOrbit<>(transform.transformOnlyPV(getPVCoordinates()), inertialFrame, getDate(), getMu());
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldKeplerianOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getZero().newInstance(dt));
    }

    /** {@inheritDoc} */
    @Override
    public FieldKeplerianOrbit<T> shiftedBy(final T dt) {

        // use Keplerian-only motion
        final FieldKeplerianOrbit<T> keplerianShifted = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                                  getKeplerianMeanMotion().multiply(dt).add(getMeanAnomaly()),
                                                                                  PositionAngleType.MEAN, cachedPositionAngleType, getFrame(), getDate().shiftedBy(dt), getMu());

        if (hasNonKeplerianRates()) {

            // extract non-Keplerian acceleration from first time derivatives
            final FieldVector3D<T> nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(getOne(), keplerianShifted.partialPV.getPosition(),
                                                                  dt.square().multiply(0.5), nonKeplerianAcceleration);
            final T   fixedR2 = fixedP.getNormSq();
            final T   fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(getOne(), keplerianShifted.partialPV.getVelocity(),
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR2.multiply(fixedR).reciprocal().multiply(getMu().negate()),
                                                                 keplerianShifted.partialPV.getPosition(),
                                                                 getOne(), nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new FieldKeplerianOrbit<>(new TimeStampedFieldPVCoordinates<>(keplerianShifted.getDate(),
                                                                                 fixedP, fixedV, fixedA),
                                             keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianMeanWrtCartesian() {
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
    private T[][] computeJacobianMeanWrtCartesianElliptical() {

        final T[][] jacobian = MathArrays.buildArray(getA().getField(), 6, 6);

        // compute various intermediate parameters
        computePVWithoutA();
        final FieldVector3D<T> position = partialPV.getPosition();
        final FieldVector3D<T> velocity = partialPV.getVelocity();
        final FieldVector3D<T> momentum = partialPV.getMomentum();
        final T v2         = velocity.getNormSq();
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T r3         = r.multiply(r2);

        final T px         = position.getX();
        final T py         = position.getY();
        final T pz         = position.getZ();
        final T vx         = velocity.getX();
        final T vy         = velocity.getY();
        final T vz         = velocity.getZ();
        final T mx         = momentum.getX();
        final T my         = momentum.getY();
        final T mz         = momentum.getZ();

        final T mu         = getMu();
        final T sqrtMuA    = FastMath.sqrt(a.multiply(mu));
        final T sqrtAoMu   = FastMath.sqrt(a.divide(mu));
        final T a2         = a.square();
        final T twoA       = a.multiply(2);
        final T rOnA       = r.divide(a);

        final T oMe2       = e.square().negate().add(1);
        final T epsilon    = oMe2.sqrt();
        final T sqrtRec    = epsilon.reciprocal();

        final FieldSinCos<T> scI  = FastMath.sinCos(i);
        final FieldSinCos<T> scPA = FastMath.sinCos(pa);
        final T cosI       = scI.cos();
        final T sinI       = scI.sin();
        final T cosPA      = scPA.cos();
        final T sinPA      = scPA.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);
        final T cosE       = a.subtract(r).divide(a.multiply(e));
        final T sinE       = pv.divide(e.multiply(sqrtMuA));

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<>(a2.multiply(2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(mu.divide(2.).reciprocal()));
        fillHalfRow(getOne(), vectorAR,    jacobian[0], 0);
        fillHalfRow(getOne(), vectorARDot, jacobian[0], 3);

        // de
        final T factorER3 = pv.divide(twoA);
        final FieldVector3D<T> vectorER   = new FieldVector3D<>(cosE.multiply(v2).divide(r.multiply(mu)), position,
                                                                sinE.divide(sqrtMuA), velocity,
                                                                factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> vectorERDot = new FieldVector3D<>(sinE.divide(sqrtMuA), position,
                                                                 cosE.multiply(mu.divide(2.).reciprocal()).multiply(r), velocity,
                                                                 factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorARDot);
        fillHalfRow(getOne(), vectorER,    jacobian[1], 0);
        fillHalfRow(getOne(), vectorERDot, jacobian[1], 3);

        // dE / dr (Eccentric anomaly)
        final T coefE = cosE.divide(e.multiply(sqrtMuA));
        final FieldVector3D<T>  vectorEAnR =
            new FieldVector3D<>(sinE.negate().multiply(v2).divide(e.multiply(r).multiply(mu)), position, coefE, velocity,
                                factorER3.negate().multiply(coefE), vectorAR);

        // dE / drDot
        final FieldVector3D<T>  vectorEAnRDot =
            new FieldVector3D<>(sinE.multiply(-2).multiply(r).divide(e.multiply(mu)), velocity, coefE, position,
                                factorER3.negate().multiply(coefE), vectorARDot);

        // precomputing some more factors
        final T s1 = sinE.negate().multiply(pz).divide(r).subtract(cosE.multiply(vz).multiply(sqrtAoMu));
        final T s2 = cosE.negate().multiply(pz).divide(r3);
        final T s3 = sinE.multiply(vz).divide(sqrtMuA.multiply(-2));
        final T t1 = sqrtRec.multiply(cosE.multiply(pz).divide(r).subtract(sinE.multiply(vz).multiply(sqrtAoMu)));
        final T t2 = sqrtRec.multiply(sinE.negate().multiply(pz).divide(r3));
        final T t3 = sqrtRec.multiply(cosE.subtract(e)).multiply(vz).divide(sqrtMuA.multiply(2));
        final T t4 = sqrtRec.multiply(e.multiply(sinI).multiply(cosPA).multiply(sqrtRec).subtract(vz.multiply(sqrtAoMu)));
        final FieldVector3D<T> s = new FieldVector3D<>(cosE.divide(r), this.PLUS_K,
                                                       s1,       vectorEAnR,
                                                       s2,       position,
                                                       s3,       vectorAR);
        final FieldVector3D<T> sDot = new FieldVector3D<>(sinE.negate().multiply(sqrtAoMu), this.PLUS_K,
                                                          s1,               vectorEAnRDot,
                                                          s3,               vectorARDot);
        final FieldVector3D<T> t =
            new FieldVector3D<>(sqrtRec.multiply(sinE).divide(r), this.PLUS_K).add(new FieldVector3D<>(t1, vectorEAnR,
                                                                                                       t2, position,
                                                                                                       t3, vectorAR,
                                                                                                       t4, vectorER));
        final FieldVector3D<T> tDot = new FieldVector3D<>(sqrtRec.multiply(cosE.subtract(e)).multiply(sqrtAoMu), this.PLUS_K,
                                                          t1,                                                    vectorEAnRDot,
                                                          t3,                                                    vectorARDot,
                                                          t4,                                                    vectorERDot);

        // di
        final T factorI1 = sinI.negate().multiply(sqrtRec).divide(sqrtMuA);
        final T i1 =  factorI1;
        final T i2 =  factorI1.negate().multiply(mz).divide(twoA);
        final T i3 =  factorI1.multiply(mz).multiply(e).divide(oMe2);
        final T i4 = cosI.multiply(sinPA);
        final T i5 = cosI.multiply(cosPA);
        fillHalfRow(i1, new FieldVector3D<>(vy, vx.negate(), getZero()), i2, vectorAR, i3, vectorER, i4, s, i5, t,
                    jacobian[2], 0);
        fillHalfRow(i1, new FieldVector3D<>(py.negate(), px, getZero()), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5, tDot,
                    jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA.divide(sinI), s,    sinPA.negate().divide(sinI), t,    jacobian[3], 0);
        fillHalfRow(cosPA.divide(sinI), sDot, sinPA.negate().divide(sinI), tDot, jacobian[3], 3);

        // dRaan
        final T factorRaanR = (a.multiply(mu).multiply(oMe2).multiply(sinI).multiply(sinI)).reciprocal();
        fillHalfRow( factorRaanR.negate().multiply(my), new FieldVector3D<>(getZero(), vz, vy.negate()),
                     factorRaanR.multiply(mx), new FieldVector3D<>(vz.negate(), getZero(),  vx),
                     jacobian[4], 0);
        fillHalfRow(factorRaanR.negate().multiply(my), new FieldVector3D<>(getZero(), pz.negate(),  py),
                     factorRaanR.multiply(mx), new FieldVector3D<>(pz, getZero(), px.negate()),
                     jacobian[4], 3);

        // dM
        fillHalfRow(rOnA, vectorEAnR,    sinE.negate(), vectorER,    jacobian[5], 0);
        fillHalfRow(rOnA, vectorEAnRDot, sinE.negate(), vectorERDot, jacobian[5], 3);

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
    private T[][] computeJacobianMeanWrtCartesianHyperbolic() {

        final T[][] jacobian = MathArrays.buildArray(getA().getField(), 6, 6);

        // compute various intermediate parameters
        computePVWithoutA();
        final FieldVector3D<T> position = partialPV.getPosition();
        final FieldVector3D<T> velocity = partialPV.getVelocity();
        final FieldVector3D<T> momentum = partialPV.getMomentum();
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T r3         = r.multiply(r2);

        final T x          = position.getX();
        final T y          = position.getY();
        final T z          = position.getZ();
        final T vx         = velocity.getX();
        final T vy         = velocity.getY();
        final T vz         = velocity.getZ();
        final T mx         = momentum.getX();
        final T my         = momentum.getY();
        final T mz         = momentum.getZ();

        final T mu         = getMu();
        final T absA       = a.negate();
        final T sqrtMuA    = absA.multiply(mu).sqrt();
        final T a2         = a.square();
        final T rOa        = r.divide(absA);

        final FieldSinCos<T> scI = FastMath.sinCos(i);
        final T cosI       = scI.cos();
        final T sinI       = scI.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<>(a2.multiply(-2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(-2).divide(mu));
        fillHalfRow(getOne().negate(), vectorAR,    jacobian[0], 0);
        fillHalfRow(getOne().negate(), vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final T m      = momentum.getNorm();
        final T oOm    = m.reciprocal();
        final FieldVector3D<T> dcXP = new FieldVector3D<>(getZero(), vz, vy.negate());
        final FieldVector3D<T> dcYP = new FieldVector3D<>(vz.negate(),   getZero(),  vx);
        final FieldVector3D<T> dcZP = new FieldVector3D<>( vy, vx.negate(),   getZero());
        final FieldVector3D<T> dcXV = new FieldVector3D<>(  getZero(),  z.negate(),   y);
        final FieldVector3D<T> dcYV = new FieldVector3D<>(  z,   getZero(),  x.negate());
        final FieldVector3D<T> dcZV = new FieldVector3D<>( y.negate(),   x,   getZero());
        final FieldVector3D<T> dCP  = new FieldVector3D<>(mx.multiply(oOm), dcXP, my.multiply(oOm), dcYP, mz.multiply(oOm), dcZP);
        final FieldVector3D<T> dCV  = new FieldVector3D<>(mx.multiply(oOm), dcXV, my.multiply(oOm), dcYV, mz.multiply(oOm), dcZV);

        // dp
        final T mOMu   = m.divide(mu);
        final FieldVector3D<T> dpP  = new FieldVector3D<>(mOMu.multiply(2), dCP);
        final FieldVector3D<T> dpV  = new FieldVector3D<>(mOMu.multiply(2), dCV);

        // de
        final T p      = m.multiply(mOMu);
        final T moO2ae = absA.multiply(2).multiply(e).reciprocal();
        final T m2OaMu = p.negate().divide(absA);
        fillHalfRow(moO2ae, dpP, m2OaMu.multiply(moO2ae), vectorAR,    jacobian[1], 0);
        fillHalfRow(moO2ae, dpV, m2OaMu.multiply(moO2ae), vectorARDot, jacobian[1], 3);

        // di
        final T cI1 = m.multiply(sinI).reciprocal();
        final T cI2 = cosI.multiply(cI1);
        fillHalfRow(cI2, dCP, cI1.negate(), dcZP, jacobian[2], 0);
        fillHalfRow(cI2, dCV, cI1.negate(), dcZV, jacobian[2], 3);


        // dPA
        final T cP1     =  y.multiply(oOm);
        final T cP2     =  x.negate().multiply(oOm);
        final T cP3     =  mx.multiply(cP1).add(my.multiply(cP2)).negate();
        final T cP4     =  cP3.multiply(oOm);
        final T cP5     =  r2.multiply(sinI).multiply(sinI).negate().reciprocal();
        final T cP6     = z.multiply(cP5);
        final T cP7     = cP3.multiply(cP5);
        final FieldVector3D<T> dacP  = new FieldVector3D<>(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new FieldVector3D<>(my.negate(), mx, getZero()));
        final FieldVector3D<T> dacV  = new FieldVector3D<>(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final FieldVector3D<T> dpoP  = new FieldVector3D<>(cP6, dacP, cP7, this.PLUS_K);
        final FieldVector3D<T> dpoV  = new FieldVector3D<>(cP6, dacV);

        final T re2     = r2.multiply(e.square());
        final T recOre2 = p.subtract(r).divide(re2);
        final T resOre2 = pv.multiply(mOMu).divide(re2);
        final FieldVector3D<T> dreP  = new FieldVector3D<>(mOMu, velocity, pv.divide(mu), dCP);
        final FieldVector3D<T> dreV  = new FieldVector3D<>(mOMu, position, pv.divide(mu), dCV);
        final FieldVector3D<T> davP  = new FieldVector3D<>(resOre2.negate(), dpP, recOre2, dreP, resOre2.divide(r), position);
        final FieldVector3D<T> davV  = new FieldVector3D<>(resOre2.negate(), dpV, recOre2, dreV);
        fillHalfRow(getOne(), dpoP, getOne().negate(), davP, jacobian[3], 0);
        fillHalfRow(getOne(), dpoV, getOne().negate(), davV, jacobian[3], 3);

        // dRAAN
        final T cO0 = cI1.square();
        final T cO1 =  mx.multiply(cO0);
        final T cO2 =  my.negate().multiply(cO0);
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final T s2a    = pv.divide(absA.multiply(2));
        final T oObux  = m.square().add(absA.multiply(mu)).sqrt().reciprocal();
        final T scasbu = pv.multiply(oObux);
        final FieldVector3D<T> dauP = new FieldVector3D<>(sqrtMuA.reciprocal(), velocity, s2a.negate().divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> dauV = new FieldVector3D<>(sqrtMuA.reciprocal(), position, s2a.negate().divide(sqrtMuA), vectorARDot);
        final FieldVector3D<T> dbuP = new FieldVector3D<>(oObux.multiply(mu.divide(2.)), vectorAR,    m.multiply(oObux), dCP);
        final FieldVector3D<T> dbuV = new FieldVector3D<>(oObux.multiply(mu.divide(2.)), vectorARDot, m.multiply(oObux), dCV);
        final FieldVector3D<T> dcuP = new FieldVector3D<>(oObux, velocity, scasbu.negate().multiply(oObux), dbuP);
        final FieldVector3D<T> dcuV = new FieldVector3D<>(oObux, position, scasbu.negate().multiply(oObux), dbuV);
        fillHalfRow(getOne(), dauP, e.negate().divide(rOa.add(1)), dcuP, jacobian[5], 0);
        fillHalfRow(getOne(), dauV, e.negate().divide(rOa.add(1)), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianEccentricWrtCartesian() {
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
    private T[][] computeJacobianEccentricWrtCartesianElliptical() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesianElliptical();

        // Differentiating the Kepler equation M = E - e sin E leads to:
        // dM = (1 - e cos E) dE - sin E de
        // which is inverted and rewritten as:
        // dE = a/r dM + sin E a/r de
        final FieldSinCos<T> scE = FastMath.sinCos(getEccentricAnomaly());
        final T aOr              = e.negate().multiply(scE.cos()).add(1).reciprocal();

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr.multiply(anomalyRow[j].add(scE.sin().multiply(eRow[j])));
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
    private T[][] computeJacobianEccentricWrtCartesianHyperbolic() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesianHyperbolic();

        // Differentiating the Kepler equation M = e sinh H - H leads to:
        // dM = (e cosh H - 1) dH + sinh H de
        // which is inverted and rewritten as:
        // dH = 1 / (e cosh H - 1) dM - sinh H / (e cosh H - 1) de
        final T H      = getEccentricAnomaly();
        final T coshH  = H.cosh();
        final T sinhH  = H.sinh();
        final T absaOr = e.multiply(coshH).subtract(1).reciprocal();
        // update anomaly row
        final T[] eRow       = jacobian[1];
        final T[] anomalyRow = jacobian[5];

        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = absaOr.multiply(anomalyRow[j].subtract(sinhH.multiply(eRow[j])));

        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianTrueWrtCartesian() {
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
    private T[][] computeJacobianTrueWrtCartesianElliptical() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesianElliptical();
        // Differentiating the eccentric anomaly equation sin E = sqrt(1-e^2) sin v / (1 + e cos v)
        // and using cos E = (e + cos v) / (1 + e cos v) to get rid of cos E leads to:
        // dE = [sqrt (1 - e^2) / (1 + e cos v)] dv - [sin E / (1 - e^2)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dE + [sin E / sqrt (1 - e^2)] a/r de
        final T e2               = e.square();
        final T oMe2             = e2.negate().add(1);
        final T epsilon          = oMe2.sqrt();
        final FieldSinCos<T> scE = FastMath.sinCos(getEccentricAnomaly());
        final T aOr              = e.multiply(scE.cos()).negate().add(1).reciprocal();
        final T aFactor          = epsilon.multiply(aOr);
        final T eFactor          = scE.sin().multiply(aOr).divide(epsilon);

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor.multiply(anomalyRow[j]).add(eFactor.multiply(eRow[j]));
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
    private T[][] computeJacobianTrueWrtCartesianHyperbolic() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesianHyperbolic();

        // Differentiating the eccentric anomaly equation sinh H = sqrt(e^2-1) sin v / (1 + e cos v)
        // and using cosh H = (e + cos v) / (1 + e cos v) to get rid of cosh H leads to:
        // dH = [sqrt (e^2 - 1) / (1 + e cos v)] dv + [sinh H / (e^2 - 1)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dH - [sinh H / sqrt (e^2 - 1)] a/r de
        final T e2       = e.square();
        final T e2Mo     = e2.subtract(1);
        final T epsilon  = e2Mo.sqrt();
        final T H        = getEccentricAnomaly();
        final T coshH    = H.cosh();
        final T sinhH    = H.sinh();
        final T aOr      = e.multiply(coshH).subtract(1).reciprocal();
        final T aFactor  = epsilon.multiply(aOr);
        final T eFactor  = sinhH.multiply(aOr).divide(epsilon);

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor.multiply(anomalyRow[j]).subtract(eFactor.multiply(eRow[j]));
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public void addKeplerContribution(final PositionAngleType type, final T gm,
                                      final T[] pDot) {
        pDot[5] = pDot[5].add(computeKeplerianAnomalyDot(type, a, e, gm, cachedAnomaly, cachedPositionAngleType));
    }

    /**
     * Compute rate of argument of latitude.
     * @param type position angle type of rate
     * @param a semi major axis
     * @param e eccentricity
     * @param mu mu
     * @param anomaly anomaly
     * @param cachedType position angle type of passed anomaly
     * @param <T> field type
     * @return first-order time derivative for anomaly
     * @since 12.2
     */
    private static <T extends CalculusFieldElement<T>> T computeKeplerianAnomalyDot(final PositionAngleType type, final T a, final T e,
                                                                                    final T mu, final T anomaly, final PositionAngleType cachedType) {
        final T absA = a.abs();
        final T n    = absA.reciprocal().multiply(mu).sqrt().divide(absA);
        if (type == PositionAngleType.MEAN) {
            return n;
        }
        final T ksi;
        final T oMe2;
        final T trueAnomaly = FieldKeplerianAnomalyUtility.convertAnomaly(cachedType, anomaly, e, PositionAngleType.TRUE);
        if (type == PositionAngleType.ECCENTRIC) {
            oMe2 = e.square().negate().add(1).abs();
            ksi = e.multiply(trueAnomaly.cos()).add(1);
            return n.multiply(ksi).divide(oMe2);
        } else {
            oMe2 = e.square().negate().add(1).abs();
            ksi  = e.multiply(trueAnomaly.cos()).add(1);
            return n.multiply(ksi).multiply(ksi).divide(oMe2.multiply(oMe2.sqrt()));
        }
    }

    /**  Returns a string representation of this Keplerian parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuilder().append("Keplerian parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append("; e: ").append(e.getReal()).
                                  append("; i: ").append(FastMath.toDegrees(i.getReal())).
                                  append("; pa: ").append(FastMath.toDegrees(pa.getReal())).
                                  append("; raan: ").append(FastMath.toDegrees(raan.getReal())).
                                  append("; v: ").append(FastMath.toDegrees(getTrueAnomaly().getReal())).
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
    public FieldKeplerianOrbit<T> withKeplerianRates() {
        return new FieldKeplerianOrbit<>(getA(), getE(), getI(), getPerigeeArgument(), getRightAscensionOfAscendingNode(),
                cachedAnomaly, cachedPositionAngleType, getFrame(), getDate(), getMu());
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

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit toOrbit() {
        final double cachedPositionAngle = cachedAnomaly.getReal();
        return new KeplerianOrbit(a.getReal(), e.getReal(), i.getReal(),
                                  pa.getReal(), raan.getReal(), cachedPositionAngle,
                                  aDot.getReal(), eDot.getReal(), iDot.getReal(),
                                  paDot.getReal(), raanDot.getReal(),
                                  cachedAnomalyDot.getReal(),
                                  cachedPositionAngleType, cachedPositionAngleType,
                                  getFrame(), getDate().toAbsoluteDate(), getMu().getReal());
    }


}
