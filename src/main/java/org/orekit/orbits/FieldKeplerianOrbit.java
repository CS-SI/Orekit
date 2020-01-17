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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
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
 */
public class FieldKeplerianOrbit<T extends RealFieldElement<T>> extends FieldOrbit<T> {

    /** Factory for first time derivatives. */
    private static final Map<Field<? extends RealFieldElement<?>>, FDSFactory<? extends RealFieldElement<?>>> FACTORIES =
                    new HashMap<>();

    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A  = 3 * k2 * k2 / k1;
        B  = k3 * k3 / (6 * k1);
    }

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

    /** True anomaly (rad). */
    private final T v;

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

    /** True anomaly derivative (rad/s). */
    private final T vDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private FieldPVCoordinates<T> partialPV;

    /** Identity element. */
    private final T one;

    /** Zero element. */
    private final T zero;

    /** Third Canonical Vector. */
    private final FieldVector3D<T> PLUS_K;

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity
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
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                               final T pa, final T raan,
                               final T anomaly, final PositionAngle type,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(a, e, i, pa, raan, anomaly,
             null, null, null, null, null, null,
             type, frame, date, mu);
    }

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity
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
                               final PositionAngle type,
                               final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (a.multiply(e.negate().add(1)).getReal() < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a.getReal(), e.getReal());
        }

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

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

        /** Identity element. */
        this.one = a.getField().getOne();

        /** Zero element. */
        this.zero = a.getField().getZero();

        /**Third canonical vector. */
        this.PLUS_K = FieldVector3D.getPlusK(a.getField());

        if (hasDerivatives()) {
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> eDS = factory.build(e, eDot);
            final FieldDerivativeStructure<T> anomalyDS  = factory.build(anomaly,  anomalyDot);
            final FieldDerivativeStructure<T> vDS;
            switch (type) {
                case MEAN :
                    vDS = (a.getReal() < 0) ?
                          hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomalyDS, eDS), eDS) :
                          ellipticEccentricToTrue(meanToEllipticEccentric(anomalyDS, eDS), eDS);
                    break;
                case ECCENTRIC :
                    vDS = (a.getReal() < 0) ?
                          hyperbolicEccentricToTrue(anomalyDS, eDS) :
                          ellipticEccentricToTrue(anomalyDS, eDS);
                    break;
                case TRUE :
                    vDS = anomalyDS;
                    break;
                default : // this should never happen
                    throw new OrekitInternalError(null);
            }
            this.v    = vDS.getValue();
            this.vDot = vDS.getPartialDerivative(1);
        } else {
            switch (type) {
                case MEAN :

                    this.v = (a.getReal() < 0) ?
                             hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomaly, e), e) :
                             ellipticEccentricToTrue(meanToEllipticEccentric(anomaly, e), e);

                    break;
                case ECCENTRIC :
                    this.v = (a.getReal() < 0) ?
                             hyperbolicEccentricToTrue(anomaly, e) :
                             ellipticEccentricToTrue(anomaly, e);

                    break;
                case TRUE :
                    this.v = anomaly;
                    break;
                default : // this should never happen
                    throw new OrekitInternalError(null);
            }
            this.vDot = null;
        }

        // check true anomaly range
        if (e.multiply(v.cos()).add(1).getReal() <= 0) {
            final double vMax = e.reciprocal().negate().acos().getReal();
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE,
                                                     v.getReal(), e.getReal(), -vMax, vMax);
        }

        this.partialPV = null;

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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

        // identity element
        this.one = pvCoordinates.getPosition().getX().getField().getOne();

        // zero element
        this.zero = one.getField().getZero();

        // third canonical vector
        this.PLUS_K = FieldVector3D.getPlusK(one.getField());

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
        if (a.getReal() > 0) {
            // elliptic or circular orbit
            final T eSE = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE = rV2OnMu.subtract(1);
            e = (eSE.multiply(eSE).add(eCE.multiply(eCE))).sqrt();
            v = ellipticEccentricToTrue(eSE.atan2(eCE), e); //(atan2(eSE, eCE));
        } else {
            // hyperbolic orbit
            final T eSH = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
            final T eCH = rV2OnMu.subtract(1);
            e = (m2.negate().divide(muA).add(1)).sqrt();
            v = hyperbolicEccentricToTrue((eCH.add(eSH)).divide(eCH.subtract(eSH)).log().divide(2), e);
        }

        // compute perigee argument
        final FieldVector3D<T> node = new FieldVector3D<>(raan, zero);
        final T px = FieldVector3D.dotProduct(pvP, node);
        final T py = FieldVector3D.dotProduct(pvP, FieldVector3D.crossProduct(momentum, node)).divide(m2.sqrt());
        pa = py.atan2(px).subtract(v);

        partialPV = pvCoordinates;

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

        if (reliableAcceleration) {
            // we have a relevant acceleration, we can compute derivatives

            final T[][] jacobian = MathArrays.buildArray(a.getField(), 6, 6);
            getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

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
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> eDS = factory.build(e, eDot);
            final FieldDerivativeStructure<T> MDS = factory.build(getMeanAnomaly(), MDot);
            final FieldDerivativeStructure<T> vDS = (a.getReal() < 0) ?
                                            FieldKeplerianOrbit.hyperbolicEccentricToTrue(FieldKeplerianOrbit.meanToHyperbolicEccentric(MDS, eDS), eDS) :
                                            FieldKeplerianOrbit.ellipticEccentricToTrue(FieldKeplerianOrbit.meanToEllipticEccentric(MDS, eDS), eDS);
            vDot = vDS.getPartialDerivative(1);

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot    = null;
            eDot    = null;
            iDot    = null;
            paDot   = null;
            raanDot = null;
            vDot    = null;
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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
        this(op.getPVCoordinates(), op.getFrame(), op.getMu(), op.hasDerivatives());
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    public T getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    public T getE() {
        return e;
    }

    /** {@inheritDoc} */
    public T getEDot() {
        return eDot;
    }

    /** {@inheritDoc} */
    public T getI() {
        return i;
    }

    /** {@inheritDoc} */
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
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
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
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return right ascension of the ascending node derivative (rad/s)
     */
    public T getRightAscensionOfAscendingNodeDot() {
        return raanDot;
    }

    /** Get the true anomaly.
     * @return true anomaly (rad)
     */
    public T getTrueAnomaly() {
        return v;
    }

    /** Get the true anomaly derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return true anomaly derivative (rad/s)
     */
    public T getTrueAnomalyDot() {
        return vDot;
    }

    /** Get the eccentric anomaly.
     * @return eccentric anomaly (rad)
     */
    public T getEccentricAnomaly() {
        return (a.getReal() < 0) ? trueToHyperbolicEccentric(v, e) : trueToEllipticEccentric(v, e);
    }

    /** Get the eccentric anomaly derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return eccentric anomaly derivative (rad/s)
     */
    public T getEccentricAnomalyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> eDS = factory.build(e, eDot);
        final FieldDerivativeStructure<T> vDS = factory.build(v, vDot);
        final FieldDerivativeStructure<T> EDS = (a.getReal() < 0) ?
                                                trueToHyperbolicEccentric(vDS, eDS) :
                                                trueToEllipticEccentric(vDS, eDS);
        return EDS.getPartialDerivative(1);

    }

    /** Get the mean anomaly.
     * @return mean anomaly (rad)
     */
    public T getMeanAnomaly() {
        return (a.getReal() < 0) ?
               hyperbolicEccentricToMean(trueToHyperbolicEccentric(v, e), e) :
               ellipticEccentricToMean(trueToEllipticEccentric(v, e), e);
    }

    /** Get the mean anomaly derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return mean anomaly derivative (rad/s)
     */
    public T getMeanAnomalyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> eDS = factory.build(e, eDot);
        final FieldDerivativeStructure<T> vDS = factory.build(v, vDot);
        final FieldDerivativeStructure<T> MDS = (a.getReal() < 0) ?
                                                hyperbolicEccentricToMean(trueToHyperbolicEccentric(vDS, eDS), eDS) :
                                                ellipticEccentricToMean(trueToEllipticEccentric(vDS, eDS), eDS);
        return MDS.getPartialDerivative(1);

    }

    /** Get the anomaly.
     * @param type type of the angle
     * @return anomaly (rad)
     */
    public T getAnomaly(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getMeanAnomaly() :
                                              ((type == PositionAngle.ECCENTRIC) ? getEccentricAnomaly() :
                                                                                   getTrueAnomaly());
    }

    /** Get the anomaly derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @param type type of the angle
     * @return anomaly derivative (rad/s)
     */
    public T getAnomalyDot(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getMeanAnomalyDot() :
                                              ((type == PositionAngle.ECCENTRIC) ? getEccentricAnomalyDot() :
                                                                                   getTrueAnomalyDot());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDerivatives() {
        return aDot != null;
    }

    /** Computes the true anomaly from the elliptic eccentric anomaly.
     * @param E eccentric anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return v the true anomaly
     */
    public static <T extends RealFieldElement<T>> T ellipticEccentricToTrue(final T E, final T e) {
        final T beta = e.divide(e.multiply(e).negate().add(1).sqrt().add(1));
        return E.add(beta.multiply(E.sin()).divide(beta.multiply(E.cos()).subtract(1).negate()).atan().multiply(2));
    }

    /** Computes the elliptic eccentric anomaly from the true anomaly.
     * @param v true anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return E the elliptic eccentric anomaly
     */
    public static <T extends RealFieldElement<T>> T trueToEllipticEccentric(final T v, final T e) {
        final T beta = e.divide(e.multiply(e).negate().add(1).sqrt().add(1));
        return v.subtract((beta.multiply(v.sin()).divide(beta.multiply(v.cos()).add(1))).atan().multiply(2));
    }

    /** Computes the true anomaly from the hyperbolic eccentric anomaly.
     * @param H hyperbolic eccentric anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return v the true anomaly
     */
    public static <T extends RealFieldElement<T>> T hyperbolicEccentricToTrue(final T H, final T e) {
        final T s    = e.add(1).divide(e.subtract(1)).sqrt();
        final T tanH = H.multiply(0.5).tanh();
        return s.multiply(tanH).atan().multiply(2);
    }

    /** Computes the hyperbolic eccentric anomaly from the true anomaly.
     * @param v true anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return H the hyperbolic eccentric anomaly
     */
    public static <T extends RealFieldElement<T>> T trueToHyperbolicEccentric(final T v, final T e) {
        final T sinhH = e.multiply(e).subtract(1).sqrt().multiply(v.sin()).divide(e.multiply(v.cos()).add(1));
        return sinhH.asinh();
    }

    /** Computes the mean anomaly from the hyperbolic eccentric anomaly.
     * @param H hyperbolic eccentric anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return M the mean anomaly
     */
    public static <T extends RealFieldElement<T>> T hyperbolicEccentricToMean(final T H, final T e) {
        return e.multiply(H.sinh()).subtract(H);
    }

    /** Computes the elliptic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving Kepler equation has been published
     * in: "Procedures for  solving Kepler's Equation", A. W. Odell and
     * R. H. Gooding, Celestial Mechanics 38 (1986) 307-334
     * </p>
     * @param M mean anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return E the eccentric anomaly
     */
    public static <T extends RealFieldElement<T>> T meanToEllipticEccentric(final T M, final T e) {
        // reduce M to [-PI PI) interval
        final T reducedM = MathUtils.normalizeAngle(M, M.getField().getZero());

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        T E;
        if (reducedM.abs().getReal() < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getReal()) < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter, mainly used when T is some kind of derivative structure.
                // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                // the computation. As in this case E and M are almost equal, we initialize E with reducedM
                E = reducedM;
            } else {
                // this is the standard S12 starter
                E = reducedM.add(e.multiply( (reducedM.multiply(6).cbrt()).subtract(reducedM)));
            }
        } else {
            if (reducedM.getReal() < 0) {
                final T w = reducedM.add(FastMath.PI);
                E = reducedM.add(e.multiply(w.multiply(A).divide(w.negate().add(B)).subtract(FastMath.PI).subtract(reducedM)));
            } else {
                final T w = reducedM.negate().add(FastMath.PI);
                E = reducedM.add(e.multiply(w.multiply(A).divide(w.negate().add(B)).negate().subtract(reducedM).add(FastMath.PI)));
            }
        }

        final T e1 = e.negate().add(1);
        final boolean noCancellationRisk = (e1.getReal() + E.getReal() * E.getReal() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {

            final T f;
            T fd;
            final T fdd  = e.multiply(E.sin());
            final T fddd = e.multiply(E.cos());

            if (noCancellationRisk) {

                f  = (E.subtract(fdd)).subtract(reducedM);
                fd = fddd.negate().add(1);
            } else {


                f  = eMeSinE(E, e).subtract(reducedM);
                final T s = E.multiply(0.5).sin();
                fd = e1.add(e.multiply(s).multiply(s).multiply(2));
            }
            final T dee = f.multiply(fd).divide(f.multiply(fdd).multiply(0.5).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final T w = fd.add(dee.multiply(fdd.add(dee.multiply(fddd.divide(3)))).multiply(0.5));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(fddd).multiply(0.5))));
            E = E.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));

        }

        // expand the result back to original range
        E = E.add(M).subtract(reducedM);
        return E;
    }

    /** Accurate computation of E - e sin(E).
     * <p>
     * This method is used when E is close to 0 and e close to 1,
     * i.e. near the perigee of almost parabolic orbits
     * </p>
     * @param E eccentric anomaly
     * @param e eccentricity
     * @param <T> Type of the field elements
     * @return E - e sin(E)
     */
    private static <T extends RealFieldElement<T>> T eMeSinE(final T E, final T e) {

        T x = (e.negate().add(1)).multiply(E.sin());
        final T mE2 = E.negate().multiply(E);
        T term = E;
        double d    = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (T x0 = E.getField().getZero().add(Double.NaN); !Double.valueOf(x.getReal()).equals(Double.valueOf(x0.getReal()));) {
            d += 2;
            term = term.multiply(mE2.divide(d * (d + 1)));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /** Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is
     * Danby's iterative method (3rd order) with Vallado's initial guess.
     * </p>
     * @param M mean anomaly (rad)
     * @param e eccentricity
     * @param <T> Type of the field elements
     * @return H the hyperbolic eccentric anomaly
     */
    public static <T extends RealFieldElement<T>> T meanToHyperbolicEccentric(final T M, final T e) {

        // Resolution of hyperbolic Kepler equation for Keplerian parameters

        // Initial guess
        T H;
        if (e.getReal() < 1.6) {
            if ((-FastMath.PI < M.getReal() && M.getReal() < 0.) || M.getReal() > FastMath.PI) {
                H = M.subtract(e);
            } else {
                H = M.add(e);
            }
        } else {
            if (e.getReal() < 3.6 && M.abs().getReal() > FastMath.PI) {
                H = M.subtract(e.copySign(M));
            } else {
                H = M.divide(e.subtract(1));
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final T f3  = e.multiply(H.cosh());
            final T f2  = e.multiply(H.sinh());
            final T f1  = f3.subtract(1);
            final T f0  = f2.subtract(H).subtract(M);
            final T f12 = f1.multiply(2);
            final T d   = f0.divide(f12);
            final T fdf = f1.subtract(d.multiply(f2));
            final T ds  = f0.divide(fdf);

            final T shift = f0.divide(fdf.add(ds.multiply(ds.multiply(f3.divide(6)))));

            H = H.subtract(shift);

            if ((shift.abs().getReal()) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalArgumentException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                               iter);
    }

    /** Computes the mean anomaly from the elliptic eccentric anomaly.
     * @param E eccentric anomaly (rad)
     * @param e eccentricity
     * @param <T> type of the field elements
     * @return M the mean anomaly
     */
    public static <T extends RealFieldElement<T>> T ellipticEccentricToMean(final T E, final T e) {
        return E.subtract(e.multiply(E.sin()));
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        return e.multiply(pa.add(raan).cos());
    }

    /** {@inheritDoc} */
    public T getEquinoctialExDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> eDS    = factory.build(e,    eDot);
        final FieldDerivativeStructure<T> paDS   = factory.build(pa,   paDot);
        final FieldDerivativeStructure<T> raanDS = factory.build(raan, raanDot);
        return eDS.multiply(paDS.add(raanDS).cos()).getPartialDerivative(1);

    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        return  e.multiply((pa.add(raan)).sin());
    }

    /** {@inheritDoc} */
    public T getEquinoctialEyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> eDS    = factory.build(e,    eDot);
        final FieldDerivativeStructure<T> paDS   = factory.build(pa,   paDot);
        final FieldDerivativeStructure<T> raanDS = factory.build(raan, raanDot);
        return eDS.multiply(paDS.add(raanDS).sin()).getPartialDerivative(1);

    }

    /** {@inheritDoc} */
    public T getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }
        return  raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHxDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> iDS    = factory.build(i,    iDot);
        final FieldDerivativeStructure<T> raanDS = factory.build(raan, raanDot);
        return raanDS.cos().multiply(iDS.multiply(0.5).tan()).getPartialDerivative(1);

    }

    /** {@inheritDoc} */
    public T getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }
        return  raan.sin().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> iDS    = factory.build(i,    iDot);
        final FieldDerivativeStructure<T> raanDS = factory.build(raan, raanDot);
        return raanDS.sin().multiply(iDS.multiply(0.5).tan()).getPartialDerivative(1);

    }

    /** {@inheritDoc} */
    public T getLv() {
        return pa.add(raan).add(v);
    }

    /** {@inheritDoc} */
    public T getLvDot() {
        return hasDerivatives() ?
               paDot.add(raanDot).add(vDot) :
               null;
    }

    /** {@inheritDoc} */
    public T getLE() {
        return pa.add(raan).add(getEccentricAnomaly());
    }

    /** {@inheritDoc} */
    public T getLEDot() {
        return hasDerivatives() ?
               paDot.add(raanDot).add(getEccentricAnomalyDot()) :
               null;
    }

    /** {@inheritDoc} */
    public T getLM() {
        return pa.add(raan).add(getMeanAnomaly());
    }

    /** {@inheritDoc} */
    public T getLMDot() {
        return hasDerivatives() ?
               paDot.add(raanDot).add(getMeanAnomalyDot()) :
               null;
    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        // preliminary variables
        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T cosPa   = pa.cos();
        final T sinPa   = pa.sin();
        final T cosI    = i.cos();
        final T sinI    = i.sin();
        final T crcp    = cosRaan.multiply(cosPa);
        final T crsp    = cosRaan.multiply(sinPa);
        final T srcp    = sinRaan.multiply(cosPa);
        final T srsp    = sinRaan.multiply(sinPa);

        // reference axes defining the orbital plane
        final FieldVector3D<T> p = new FieldVector3D<>(crcp.subtract(cosI.multiply(srsp)),  srcp.add(cosI.multiply(crsp)), sinI.multiply(sinPa));
        final FieldVector3D<T> q = new FieldVector3D<>(crsp.add(cosI.multiply(srcp)).negate(), cosI.multiply(crcp).subtract(srsp), sinI.multiply(cosPa));

        if (a.getReal() > 0) {

            // elliptical case

            // elliptic eccentric anomaly
            final T uME2   = e.negate().add(1).multiply(e.add(1));
            final T s1Me2  = uME2.sqrt();
            final T E      = getEccentricAnomaly();
            final T cosE   = E.cos();
            final T sinE   = E.sin();

            // coordinates of position and velocity in the orbital plane
            final T x      = a.multiply(cosE.subtract(e));
            final T y      = a.multiply(sinE).multiply(s1Me2);
            final T factor = FastMath.sqrt(getMu().divide(a)).divide(e.negate().multiply(cosE).add(1));
            final T xDot   = sinE.negate().multiply(factor);
            final T yDot   = cosE.multiply(s1Me2).multiply(factor);

            final FieldVector3D<T> position = new FieldVector3D<>(x, p, y, q);
            final FieldVector3D<T> velocity = new FieldVector3D<>(xDot, p, yDot, q);
            partialPV = new FieldPVCoordinates<>(position, velocity);

        } else {

            // hyperbolic case

            // compute position and velocity factors
            final T sinV      = v.sin();
            final T cosV      = v.cos();
            final T f         = a.multiply(e.multiply(e).negate().add(1));
            final T posFactor = f.divide(e.multiply(cosV).add(1));
            final T velFactor = FastMath.sqrt(getMu().divide(f));

            final FieldVector3D<T> position     = new FieldVector3D<>(posFactor.multiply(cosV), p, posFactor.multiply(sinV), q);
            final FieldVector3D<T> velocity     = new FieldVector3D<>(velFactor.multiply(sinV).negate(), p, velFactor.multiply(e.add(cosV)), q);
            partialPV = new FieldPVCoordinates<>(position, velocity);

        }

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private FieldVector3D<T> nonKeplerianAcceleration() {

        final T[][] dCdP = MathArrays.buildArray(a.getField(), 6, 6);
        getJacobianWrtParameters(PositionAngle.MEAN, dCdP);

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
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final T r2 = partialPV.getPosition().getNormSq();
        final FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r2.multiply(FastMath.sqrt(r2)).reciprocal().multiply(getMu().negate()),
                                                                           partialPV.getPosition());
        final FieldVector3D<T> acceleration = hasDerivatives() ?
                                              keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                              keplerianAcceleration;

        return new TimeStampedFieldPVCoordinates<>(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    public FieldKeplerianOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getDate().getField().getZero().add(dt));
    }

    /** {@inheritDoc} */
    public FieldKeplerianOrbit<T> shiftedBy(final T dt) {

        // use Keplerian-only motion
        final FieldKeplerianOrbit<T> keplerianShifted = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                                  getKeplerianMeanMotion().multiply(dt).add(getMeanAnomaly()),
                                                                                  PositionAngle.MEAN, getFrame(), getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final FieldVector3D<T> nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(one, keplerianShifted.partialPV.getPosition(),
                                                                  dt.multiply(dt).multiply(0.5), nonKeplerianAcceleration);
            final T   fixedR2 = fixedP.getNormSq();
            final T   fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(one, keplerianShifted.partialPV.getVelocity(),
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR2.multiply(fixedR).reciprocal().multiply(getMu().negate()),
                                                                 keplerianShifted.partialPV.getPosition(),
                                                                 one, nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new FieldKeplerianOrbit<>(new TimeStampedFieldPVCoordinates<>(keplerianShifted.getDate(),
                                                                                 fixedP, fixedV, fixedA),
                                             keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Keplerian elements, without derivatives (which means the interpolation
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
    public FieldKeplerianOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Stream<FieldOrbit<T>> sample) {

        // first pass to check if derivatives are available throughout the sample
        final List<FieldOrbit<T>> list = sample.collect(Collectors.toList());
        boolean useDerivatives = true;
        for (final FieldOrbit<T> orbit : list) {
            useDerivatives = useDerivatives && orbit.hasDerivatives();
        }

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();

        // second pass to feed interpolator
        FieldAbsoluteDate<T> previousDate = null;
        T                    previousPA   = zero.add(Double.NaN);
        T                    previousRAAN = zero.add(Double.NaN);
        T                    previousM    = zero.add(Double.NaN);
        for (final FieldOrbit<T> orbit : list) {
            final FieldKeplerianOrbit<T> kep = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(orbit);
            final T continuousPA;
            final T continuousRAAN;
            final T continuousM;
            if (previousDate == null) {
                continuousPA   = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM    = kep.getMeanAnomaly();
            } else {
                final T dt      = kep.getDate().durationFrom(previousDate);
                final T keplerM = previousM.add(kep.getKeplerianMeanMotion().multiply(dt));
                continuousPA   = MathUtils.normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousM    = MathUtils.normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA   = continuousPA;
            previousRAAN = continuousRAAN;
            previousM    = continuousM;
            final T[] toAdd = MathArrays.buildArray(getA().getField(), 6);
            toAdd[0] = kep.getA();
            toAdd[1] = kep.getE();
            toAdd[2] = kep.getI();
            toAdd[3] = continuousPA;
            toAdd[4] = continuousRAAN;
            toAdd[5] = continuousM;
            if (useDerivatives) {
                final T[] toAddDot = MathArrays.buildArray(one.getField(), 6);
                toAddDot[0] = kep.getADot();
                toAddDot[1] = kep.getEDot();
                toAddDot[2] = kep.getIDot();
                toAddDot[3] = kep.getPerigeeArgumentDot();
                toAddDot[4] = kep.getRightAscensionOfAscendingNodeDot();
                toAddDot[5] = kep.getMeanAnomalyDot();
                interpolator.addSamplePoint(kep.getDate().durationFrom(date),
                                            toAdd, toAddDot);
            } else {
                interpolator.addSamplePoint(this.zero.add(kep.getDate().durationFrom(date)),
                                            toAdd);
            }
        }

        // interpolate
        final T[][] interpolated = interpolator.derivatives(zero, 1);

        // build a new interpolated instance
        return new FieldKeplerianOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                         interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                         interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                         interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                         PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianMeanWrtCartesian() {
        if (a.getReal() > 0) {
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
        final T a2         = a.multiply(a);
        final T twoA       = a.multiply(2);
        final T rOnA       = r.divide(a);

        final T oMe2       = e.multiply(e).negate().add(1);
        final T epsilon    = oMe2.sqrt();
        final T sqrtRec    = epsilon.reciprocal();

        final T cosI       = i.cos();
        final T sinI       = i.sin();
        final T cosPA      = pa.cos();
        final T sinPA      = pa.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);
        final T cosE       = a.subtract(r).divide(a.multiply(e));
        final T sinE       = pv.divide(e.multiply(sqrtMuA));

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<>(a2.multiply(2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(mu.divide(2.).reciprocal()));
        fillHalfRow(this.one, vectorAR,    jacobian[0], 0);
        fillHalfRow(this.one, vectorARDot, jacobian[0], 3);

        // de
        final T factorER3 = pv.divide(twoA);
        final FieldVector3D<T> vectorER   = new FieldVector3D<>(cosE.multiply(v2).divide(r.multiply(mu)), position,
                                                                sinE.divide(sqrtMuA), velocity,
                                                                factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> vectorERDot = new FieldVector3D<>(sinE.divide(sqrtMuA), position,
                                                                 cosE.multiply(mu.divide(2.).reciprocal()).multiply(r), velocity,
                                                                 factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorARDot);
        fillHalfRow(this.one, vectorER,    jacobian[1], 0);
        fillHalfRow(this.one, vectorERDot, jacobian[1], 3);

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
        fillHalfRow(i1, new FieldVector3D<>(vy, vx.negate(), this.zero), i2, vectorAR, i3, vectorER, i4, s, i5, t,
                    jacobian[2], 0);
        fillHalfRow(i1, new FieldVector3D<>(py.negate(), px, this.zero), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5, tDot,
                    jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA.divide(sinI), s,    sinPA.negate().divide(sinI), t,    jacobian[3], 0);
        fillHalfRow(cosPA.divide(sinI), sDot, sinPA.negate().divide(sinI), tDot, jacobian[3], 3);

        // dRaan
        final T factorRaanR = (a.multiply(mu).multiply(oMe2).multiply(sinI).multiply(sinI)).reciprocal();
        fillHalfRow( factorRaanR.negate().multiply(my), new FieldVector3D<>(zero, vz, vy.negate()),
                     factorRaanR.multiply(mx), new FieldVector3D<>(vz.negate(), zero,  vx),
                     jacobian[4], 0);
        fillHalfRow(factorRaanR.negate().multiply(my), new FieldVector3D<>(zero, pz.negate(),  py),
                     factorRaanR.multiply(mx), new FieldVector3D<>(pz, zero, px.negate()),
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
        final T a2         = a.multiply(a);
        final T rOa        = r.divide(absA);

        final T cosI       = i.cos();
        final T sinI       = i.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<>(a2.multiply(-2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(-2).divide(mu));
        fillHalfRow(this.one.negate(), vectorAR,    jacobian[0], 0);
        fillHalfRow(this.one.negate(), vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final T m      = momentum.getNorm();
        final T oOm    = m.reciprocal();
        final FieldVector3D<T> dcXP = new FieldVector3D<>(this.zero, vz, vy.negate());
        final FieldVector3D<T> dcYP = new FieldVector3D<>(vz.negate(),   this.zero,  vx);
        final FieldVector3D<T> dcZP = new FieldVector3D<>( vy, vx.negate(),   this.zero);
        final FieldVector3D<T> dcXV = new FieldVector3D<>(  this.zero,  z.negate(),   y);
        final FieldVector3D<T> dcYV = new FieldVector3D<>(  z,   this.zero,  x.negate());
        final FieldVector3D<T> dcZV = new FieldVector3D<>( y.negate(),   x,   this.zero);
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
        final FieldVector3D<T> dacP  = new FieldVector3D<>(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new FieldVector3D<>(my.negate(), mx, this.zero));
        final FieldVector3D<T> dacV  = new FieldVector3D<>(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final FieldVector3D<T> dpoP  = new FieldVector3D<>(cP6, dacP, cP7, this.PLUS_K);
        final FieldVector3D<T> dpoV  = new FieldVector3D<>(cP6, dacV);

        final T re2     = r2.multiply(e).multiply(e);
        final T recOre2 = p.subtract(r).divide(re2);
        final T resOre2 = pv.multiply(mOMu).divide(re2);
        final FieldVector3D<T> dreP  = new FieldVector3D<>(mOMu, velocity, pv.divide(mu), dCP);
        final FieldVector3D<T> dreV  = new FieldVector3D<>(mOMu, position, pv.divide(mu), dCV);
        final FieldVector3D<T> davP  = new FieldVector3D<>(resOre2.negate(), dpP, recOre2, dreP, resOre2.divide(r), position);
        final FieldVector3D<T> davV  = new FieldVector3D<>(resOre2.negate(), dpV, recOre2, dreV);
        fillHalfRow(this.one, dpoP, this.one.negate(), davP, jacobian[3], 0);
        fillHalfRow(this.one, dpoV, this.one.negate(), davV, jacobian[3], 3);

        // dRAAN
        final T cO0 = cI1.multiply(cI1);
        final T cO1 =  mx.multiply(cO0);
        final T cO2 =  my.negate().multiply(cO0);
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final T s2a    = pv.divide(absA.multiply(2));
        final T oObux  = m.multiply(m).add(absA.multiply(mu)).sqrt().reciprocal();
        final T scasbu = pv.multiply(oObux);
        final FieldVector3D<T> dauP = new FieldVector3D<>(sqrtMuA.reciprocal(), velocity, s2a.negate().divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> dauV = new FieldVector3D<>(sqrtMuA.reciprocal(), position, s2a.negate().divide(sqrtMuA), vectorARDot);
        final FieldVector3D<T> dbuP = new FieldVector3D<>(oObux.multiply(mu.divide(2.)), vectorAR,    m.multiply(oObux), dCP);
        final FieldVector3D<T> dbuV = new FieldVector3D<>(oObux.multiply(mu.divide(2.)), vectorARDot, m.multiply(oObux), dCV);
        final FieldVector3D<T> dcuP = new FieldVector3D<>(oObux, velocity, scasbu.negate().multiply(oObux), dbuP);
        final FieldVector3D<T> dcuV = new FieldVector3D<>(oObux, position, scasbu.negate().multiply(oObux), dbuV);
        fillHalfRow(this.one, dauP, e.negate().divide(rOa.add(1)), dcuP, jacobian[5], 0);
        fillHalfRow(this.one, dauV, e.negate().divide(rOa.add(1)), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianEccentricWrtCartesian() {
        if (a.getReal() > 0) {
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
        final T eccentricAnomaly = getEccentricAnomaly();
        final T cosE             = eccentricAnomaly.cos();
        final T sinE             = eccentricAnomaly.sin();
        final T aOr              = e.negate().multiply(cosE).add(1).reciprocal();

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr.multiply(anomalyRow[j].add(sinE.multiply(eRow[j])));
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
    protected T[][] computeJacobianTrueWrtCartesian() {
        if (a.getReal() > 0) {
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
        final T e2               = e.multiply(e);
        final T oMe2             = e2.negate().add(1);
        final T epsilon          = oMe2.sqrt();
        final T eccentricAnomaly = getEccentricAnomaly();
        final T cosE             = eccentricAnomaly.cos();
        final T sinE             = eccentricAnomaly.sin();
        final T aOr              = e.multiply(cosE).negate().add(1).reciprocal();
        final T aFactor          = epsilon.multiply(aOr);
        final T eFactor          = sinE.multiply(aOr).divide(epsilon);

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
        final T e2       = e.multiply(e);
        final T e2Mo     = e2.subtract(1);
        final T epsilon  = e2Mo.sqrt();
        final T H        = getEccentricAnomaly();
        final T coshH    = H.cosh();
        final T sinhH    = H.sinh();
        final T aOr      = e.multiply(coshH).subtract(1).reciprocal();
        final T aFactor  = epsilon.multiply(aOr);
        final T eFactor  = sinhH .multiply(aOr).divide(epsilon);

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor.multiply(anomalyRow[j]).subtract(eFactor.multiply(eRow[j]));
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final T gm,
                                      final T[] pDot) {
        final T oMe2;
        final T ksi;
        final T absA = a.abs();
        final T n    = absA.reciprocal().multiply(gm).sqrt().divide(absA);
        switch (type) {
            case MEAN :
                pDot[5] = pDot[5].add(n);
                break;
            case ECCENTRIC :
                oMe2 = e.multiply(e).negate().add(1).abs();
                ksi  = e.multiply(v.cos()).add(1);
                pDot[5] = pDot[5].add( n.multiply(ksi).divide(oMe2));
                break;
            case TRUE :
                oMe2 = e.multiply(e).negate().add(1).abs();
                ksi  = e.multiply(v.cos()).add(1);
                pDot[5] = pDot[5].add(n.multiply(ksi).multiply(ksi).divide(oMe2.multiply(oMe2.sqrt())));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this Keplerian parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("Keplerian parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append("; e: ").append(e.getReal()).
                                  append("; i: ").append(FastMath.toDegrees(i.getReal())).
                                  append("; pa: ").append(FastMath.toDegrees(pa.getReal())).
                                  append("; raan: ").append(FastMath.toDegrees(raan.getReal())).
                                  append("; v: ").append(FastMath.toDegrees(v.getReal())).
                                  append(";}").toString();
    }

    /**
     * Normalize an angle in a 2&pi; wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br>
     *       {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     * @param a angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     * @param <T> the type of the field elements
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @deprecated replaced by {@link MathUtils#normalizeAngle(RealFieldElement, RealFieldElement)}
     */
    @Deprecated
    public static <T extends RealFieldElement<T>> T normalizeAngle(final T a, final T center) {
        return a.subtract(2 * FastMath.PI * FastMath.floor((a.getReal() + FastMath.PI - center.getReal()) / (2 * FastMath.PI)));
    }

    @Override
    public KeplerianOrbit toOrbit() {
        if (hasDerivatives()) {
            return new KeplerianOrbit(a.getReal(), e.getReal(), i.getReal(),
                                      pa.getReal(), raan.getReal(), v.getReal(),
                                      aDot.getReal(), eDot.getReal(), iDot.getReal(),
                                      paDot.getReal(), raanDot.getReal(), vDot.getReal(),
                                      PositionAngle.TRUE,
                                      getFrame(), getDate().toAbsoluteDate(), getMu().getReal());
        } else {
            return new KeplerianOrbit(a.getReal(), e.getReal(), i.getReal(),
                                      pa.getReal(), raan.getReal(), v.getReal(),
                                      PositionAngle.TRUE,
                                      getFrame(), getDate().toAbsoluteDate(), getMu().getReal());
        }
    }


}
