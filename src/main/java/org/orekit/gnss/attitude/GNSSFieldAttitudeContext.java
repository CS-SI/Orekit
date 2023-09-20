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
package org.orekit.gnss.attitude;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Boilerplate computations for GNSS attitude.
 *
 * <p>
 * This class is intended to hold throw-away data pertaining to <em>one</em> call
 * to {@link GNSSAttitudeProvider#getAttitude(org.orekit.utils.FieldPVCoordinatesProvider,
 * org.orekit.time.FieldAbsoluteDate, org.orekit.frames.Frame)) getAttitude}. It allows
 * the various {@link GNSSAttitudeProvider} implementations to be immutable as they
 * do not store any state, and hence to be thread-safe and reentrant.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
class GNSSFieldAttitudeContext<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y_PV =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z_PV =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Limit value below which we shoud use replace beta by betaIni. */
    private static final double BETA_SIGN_CHANGE_PROTECTION = FastMath.toRadians(0.07);

    /** Constant Y axis. */
    private final FieldPVCoordinates<T> plusY;

    /** Constant Z axis. */
    private final FieldPVCoordinates<T> minusZ;

    /** Context date. */
    private final AbsoluteDate dateDouble;

    /** Current date. */
    private final FieldAbsoluteDate<T> date;

    /** Provider for Sun position. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Provider for spacecraft position. */
    private final FieldPVCoordinatesProvider<T> pvProv;

    /** Spacecraft position at central date.
     * @since 12.0
     */
    private final TimeStampedFieldPVCoordinates<T> svPV;

    /** Sun position at central date.
     * @since 12.0
     */
    private final TimeStampedFieldPVCoordinates<T> sunPV;

    /** Inertial frame where velocity are computed. */
    private final Frame inertialFrame;

    /** Cosine of the angle between spacecraft and Sun direction. */
    private final T svbCos;

    /** Morning/Evening half orbit indicator. */
    private final boolean morning;

    /** Relative orbit angle to turn center. */
    private final FieldUnivariateDerivative2<T> delta;

    /** Sun elevation at center.
     * @since 12.0
     */
    private final FieldUnivariateDerivative2<T> beta;

    /** Spacecraft angular velocity. */
    private T muRate;

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Turn time data. */
    private FieldTurnSpan<T> turnSpan;

    /** Simple constructor.
     * @param date current date
     * @param sun provider for Sun position
     * @param pvProv provider for spacecraft position
     * @param inertialFrame inertial frame where velocity are computed
     * @param turnSpan turn time data, if a turn has already been identified in the date neighborhood,
     * null otherwise
     */
    GNSSFieldAttitudeContext(final FieldAbsoluteDate<T> date,
                             final ExtendedPVCoordinatesProvider sun, final FieldPVCoordinatesProvider<T> pvProv,
                             final Frame inertialFrame,  final FieldTurnSpan<T> turnSpan) {

        final Field<T> field = date.getField();
        plusY    = new FieldPVCoordinates<>(field, PLUS_Y_PV);
        minusZ   = new FieldPVCoordinates<>(field, MINUS_Z_PV);

        this.dateDouble    = date.toAbsoluteDate();
        this.date          = date;
        this.sun           = sun;
        this.pvProv        = pvProv;
        this.inertialFrame = inertialFrame;
        this.sunPV         = sun.getPVCoordinates(date, inertialFrame);
        this.svPV          = pvProv.getPVCoordinates(date, inertialFrame);
        this.morning       = Vector3D.dotProduct(svPV.getVelocity().toVector3D(), sunPV.getPosition().toVector3D()) >= 0.0;
        this.muRate        = svPV.getAngularVelocity().getNorm();
        this.turnSpan      = turnSpan;

        final FieldPVCoordinates<FieldUnivariateDerivative2<T>> sunPVD2 = sunPV.toUnivariateDerivative2PV();
        final FieldPVCoordinates<FieldUnivariateDerivative2<T>> svPVD2  = svPV.toUnivariateDerivative2PV();
        final FieldUnivariateDerivative2<T> svbCosD2  = FieldVector3D.dotProduct(sunPVD2.getPosition(), svPVD2.getPosition()).
                                                        divide(sunPVD2.getPosition().getNorm().multiply(svPVD2.getPosition().getNorm()));
        svbCos = svbCosD2.getValue();

        beta  = FieldVector3D.angle(sunPVD2.getPosition(), svPVD2.getMomentum()).negate().add(0.5 * FastMath.PI);

        final FieldUnivariateDerivative2<T> absDelta;
        if (svbCos.getReal() <= 0) {
            // night side
            absDelta = FastMath.acos(svbCosD2.negate().divide(FastMath.cos(beta)));
        } else {
            // Sun side
            absDelta = FastMath.acos(svbCosD2.divide(FastMath.cos(beta)));
        }
        delta = absDelta.copySign(absDelta.getPartialDerivative(1).negate());

    }

    /** Compute nominal yaw steering.
     * @param d computation date
     * @return nominal yaw steering
     */
    public TimeStampedFieldAngularCoordinates<T> nominalYaw(final FieldAbsoluteDate<T> d) {
        final TimeStampedFieldPVCoordinates<T> pv = pvProv.getPVCoordinates(d, inertialFrame);
        return new TimeStampedFieldAngularCoordinates<>(d,
                                                        pv.normalize(),
                                                        sun.getPVCoordinates(d, inertialFrame).crossProduct(pv).normalize(),
                                                        minusZ,
                                                        plusY,
                                                        1.0e-9);
    }

    /** Compute Sun elevation.
     * @param d computation date
     * @return Sun elevation
     */
    public T beta(final FieldAbsoluteDate<T> d) {
        final TimeStampedFieldPVCoordinates<T> pv = pvProv.getPVCoordinates(d, inertialFrame);
        return FieldVector3D.angle(sun.getPosition(d, inertialFrame), pv.getMomentum()).
               negate().
               add(svPV.getPosition().getX().getPi().multiply(0.5));
    }

    /** Compute Sun elevation.
     * @return Sun elevation
     */
    public FieldUnivariateDerivative2<T> betaD2() {
        return beta;
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the turn span.
     * @return turn span, may be null if context is outside of turn
     */
    public FieldTurnSpan<T> getTurnSpan() {
        return turnSpan;
    }

    /** Get the cosine of the angle between spacecraft and Sun direction.
     * @return cosine of the angle between spacecraft and Sun direction.
     */
    public T getSVBcos() {
        return svbCos;
    }

    /** Get a Sun elevation angle that does not change sign within the turn.
     * <p>
     * This method either returns the current beta or replaces it with the
     * value at turn start, so the sign remains constant throughout the
     * turn. As of 9.2, it is used for GPS, Glonass and Galileo.
     * </p>
     * @return secured Sun elevation angle
     * @see #beta(FieldAbsoluteDate)
     * @see #betaDS(FieldAbsoluteDate)
     */
    public T getSecuredBeta() {
        return FastMath.abs(beta.getValue().getReal()) < BETA_SIGN_CHANGE_PROTECTION ?
               beta(turnSpan.getTurnStartDate()) :
               beta.getValue();
    }

    /** Check if a linear yaw model is still active or if we already reached target yaw.
     * @param linearPhi value of the linear yaw model
     * @param phiDot slope of the linear yaw model
     * @return true if linear model is still active
     */
    public boolean linearModelStillActive(final T linearPhi, final T phiDot) {
        final AbsoluteDate absDate = date.toAbsoluteDate();
        final double dt0 = turnSpan.getTurnEndDate().durationFrom(date).getReal();
        final UnivariateFunction yawReached = dt -> {
            final AbsoluteDate  t       = absDate.shiftedBy(dt);
            final Vector3D      pSun    = sun.getPosition(t, inertialFrame);
            final PVCoordinates pv      = pvProv.getPVCoordinates(date.shiftedBy(dt), inertialFrame).toPVCoordinates();
            final Vector3D      pSat    = pv.getPosition();
            final Vector3D      targetX = Vector3D.crossProduct(pSat, Vector3D.crossProduct(pSun, pSat)).normalize();

            final double        phi         = linearPhi.getReal() + dt * phiDot.getReal();
            final SinCos        sc          = FastMath.sinCos(phi);
            final Vector3D      pU          = pv.getPosition().normalize();
            final Vector3D      mU          = pv.getMomentum().normalize();
            final Vector3D      omega       = new Vector3D(-phiDot.getReal(), pU);
            final Vector3D      currentX    = new Vector3D(-sc.sin(), mU, -sc.cos(), Vector3D.crossProduct(pU, mU));
            final Vector3D      currentXDot = Vector3D.crossProduct(omega, currentX);

            return Vector3D.dotProduct(targetX, currentXDot);
        };
        final double fullTurn = 2 * FastMath.PI / FastMath.abs(phiDot.getReal());
        final double dtMin    = FastMath.min(turnSpan.getTurnStartDate().durationFrom(date).getReal(), dt0 - 60.0);
        final double dtMax    = FastMath.max(dtMin + fullTurn, dt0 + 60.0);
        double[] bracket = UnivariateSolverUtils.bracket(yawReached, dt0,
                                                         dtMin, dtMax, fullTurn / 100, 1.0, 100);
        if (yawReached.value(bracket[0]) <= 0.0) {
            // we have bracketed the wrong crossing
            bracket = UnivariateSolverUtils.bracket(yawReached, 0.5 * (bracket[0] + bracket[1] + fullTurn),
                                                    bracket[1], bracket[1] + fullTurn, fullTurn / 100, 1.0, 100);
        }
        final double dt = new BracketingNthOrderBrentSolver(1.0e-3, 5).
                          solve(100, yawReached, bracket[0], bracket[1]);
        turnSpan.updateEnd(date.shiftedBy(dt), absDate);

        return dt > 0.0;

    }

    /** Set up the midnight/noon turn region.
     * @param cosNight limit cosine for the midnight turn
     * @param cosNoon limit cosine for the noon turn
     * @return true if spacecraft is in the midnight/noon turn region
     */
    public boolean setUpTurnRegion(final double cosNight, final double cosNoon) {
        this.cNight = cosNight;
        this.cNoon  = cosNoon;

        if (svbCos.getReal() < cNight || svbCos.getReal() > cNoon) {
            // we are within turn triggering zone
            return true;
        } else {
            // we are outside of turn triggering zone,
            // but we may still be trying to recover nominal attitude at the end of a turn
            return inTurnTimeRange();
        }

    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public FieldUnivariateDerivative2<T> getDeltaDS() {
        return delta;
    }

    /** Get the orbit angle since solar midnight.
     * @return orbit angle since solar midnight
     */
    public T getOrbitAngleSinceMidnight() {
        final T absAngle = inOrbitPlaneAbsoluteAngle(FastMath.acos(svbCos).negate().add(svbCos.getPi()));
        return morning ? absAngle : absAngle.negate();
    }

    /** Check if spacecraft is in the half orbit closest to Sun.
     * @return true if spacecraft is in the half orbit closest to Sun
     */
    public boolean inSunSide() {
        return svbCos.getReal() > 0;
    }

    /** Get yaw at turn start.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn start
     */
    public T getYawStart(final T sunBeta) {
        final T halfSpan = turnSpan.getTurnDuration().multiply(muRate).multiply(0.5);
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos));
    }

    /** Get yaw at turn end.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn end
     */
    public T getYawEnd(final T sunBeta) {
        final T halfSpan = turnSpan.getTurnDuration().multiply(muRate).multiply(0.5);
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.negate()));
    }

    /** Compute yaw rate.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw rate
     */
    public T yawRate(final T sunBeta) {
        return getYawEnd(sunBeta).subtract(getYawStart(sunBeta)).divide(turnSpan.getTurnDuration());
    }

    /** Get the spacecraft angular velocity.
     * @return spacecraft angular velocity
     */
    public T getMuRate() {
        return muRate;
    }

    /** Project a spacecraft/Sun angle into orbital plane.
     * <p>
     * This method is intended to find the limits of the noon and midnight
     * turns in orbital plane. The return angle is always positive. The
     * correct sign to apply depends on the spacecraft being before or
     * after turn middle point.
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    public T inOrbitPlaneAbsoluteAngle(final T angle) {
        return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta(getDate()))));
    }

    /** Compute yaw.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @param inOrbitPlaneAngle in orbit angle between spacecraft
     * and Sun (or opposite of Sun) projection
     * @return yaw angle
     */
    public T computePhi(final T sunBeta, final T inOrbitPlaneAngle) {
        return FastMath.atan2(FastMath.tan(sunBeta).negate(), FastMath.sin(inOrbitPlaneAngle));
    }

    /** Set turn half span and compute corresponding turn time range.
     * @param halfSpan half span of the turn region, as an angle in orbit plane
     * @param endMargin margin in seconds after turn end
     */
    public void setHalfSpan(final T halfSpan, final double endMargin) {
        final FieldAbsoluteDate<T> start = date.shiftedBy(delta.getValue().subtract(halfSpan).divide(muRate));
        final FieldAbsoluteDate<T> end   = date.shiftedBy(delta.getValue().add(halfSpan).divide(muRate));
        final AbsoluteDate estimationDate = getDate().toAbsoluteDate();
        if (turnSpan == null) {
            turnSpan = new FieldTurnSpan<>(start, end, estimationDate, endMargin);
        } else {
            turnSpan.updateStart(start, estimationDate);
            turnSpan.updateEnd(end, estimationDate);
        }
    }

    /** Check if context is within turn range.
     * @return true if context is within range extended by end margin
     */
    public boolean inTurnTimeRange() {
        return turnSpan != null && turnSpan.inTurnTimeRange(dateDouble);
    }

    /** Get elapsed time since turn start.
     * @return elapsed time from turn start to current date
     */
    public T timeSinceTurnStart() {
        return getDate().durationFrom(turnSpan.getTurnStartDate());
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @param yawDot yaw first time derivative
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final T yaw, final T yawDot) {
        return turnCorrectedAttitude(new FieldUnivariateDerivative2<>(yaw, yawDot, yaw.getField().getZero()));
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final FieldUnivariateDerivative2<T> yaw) {

        // Earth pointing (Z aligned with position) with linear yaw (momentum with known cos/sin in the X/Y plane)
        final FieldVector3D<T>      p             = svPV.getPosition();
        final FieldVector3D<T>      v             = svPV.getVelocity();
        final FieldVector3D<T>      a             = svPV.getAcceleration();
        final T                     r2            = p.getNormSq();
        final T                     r             = FastMath.sqrt(r2);
        final FieldVector3D<T>      keplerianJerk = new FieldVector3D<>(FieldVector3D.dotProduct(p, v).multiply(-3).divide(r2), a,
                                                                        a.getNorm().negate().divide(r), v);
        final FieldPVCoordinates<T> velocity      = new FieldPVCoordinates<>(v, a, keplerianJerk);
        final FieldPVCoordinates<T> momentum      = svPV.crossProduct(velocity);

        final FieldSinCos<FieldUnivariateDerivative2<T>> sc = FastMath.sinCos(yaw);
        final FieldUnivariateDerivative2<T> c = sc.cos().negate();
        final FieldUnivariateDerivative2<T> s = sc.sin().negate();
        final T                             z = yaw.getValueField().getZero();
        final FieldVector3D<T> m0 = new FieldVector3D<>(s.getValue(),              c.getValue(),              z);
        final FieldVector3D<T> m1 = new FieldVector3D<>(s.getPartialDerivative(1), c.getPartialDerivative(1), z);
        final FieldVector3D<T> m2 = new FieldVector3D<>(s.getPartialDerivative(2), c.getPartialDerivative(2), z);
        return new TimeStampedFieldAngularCoordinates<>(date,
                                                        svPV.normalize(), momentum.normalize(),
                                                        minusZ, new FieldPVCoordinates<>(m0, m1, m2),
                                                        1.0e-9);

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw, using inertial frame as reference
     */
    public TimeStampedFieldAngularCoordinates<T> orbitNormalYaw() {
        final FieldTransform<T> t = LOFType.LVLH_CCSDS.transformFromInertial(date, pvProv.getPVCoordinates(date, inertialFrame));
        return new TimeStampedFieldAngularCoordinates<>(date,
                                                        t.getRotation(),
                                                        t.getRotationRate(),
                                                        t.getRotationAcceleration());
    }

}
