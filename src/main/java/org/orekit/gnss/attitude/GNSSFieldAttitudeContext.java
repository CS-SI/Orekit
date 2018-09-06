/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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
 * do not store any state, and hence to be thread-safe, reentrant and naturally
 * serializable (so for example ephemeris built from them are also serializable).
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
class GNSSFieldAttitudeContext<T extends RealFieldElement<T>> implements FieldTimeStamped<T> {

    /** Derivation order. */
    private static final int ORDER = 2;

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y_PV =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z_PV =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Limit value below which we shoud use replace beta by betaIni. */
    private static final double BETA_SIGN_CHANGE_PROTECTION = FastMath.toRadians(0.07);

    /** Time derivation factory. */
    private final FDSFactory<T> factory;

    /** Constant Y axis. */
    private final FieldPVCoordinates<T> plusY;

    /** Constant Z axis. */
    private final FieldPVCoordinates<T> minusZ;

    /** Constant Y axis. */
    private final FieldPVCoordinates<FieldDerivativeStructure<T>> plusYDS;

    /** Constant Z axis. */
    private final FieldPVCoordinates<FieldDerivativeStructure<T>> minusZDS;

    /** Context date. */
    private final AbsoluteDate dateDouble;

    /** Current date. */
    private final FieldAbsoluteDate<T> date;

    /** Current date. */
    private final FieldAbsoluteDate<FieldDerivativeStructure<T>> dateDS;

    /** Provider for Sun position. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Provider for spacecraft position. */
    private final FieldPVCoordinatesProvider<T> pvProv;

    /** Inertial frame where velocity are computed. */
    private final Frame inertialFrame;

    /** Cosine of the angle between spacecraft and Sun direction. */
    private final FieldDerivativeStructure<T> svbCos;

    /** Spacecraft angular velocity. */
    private T muRate;

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Relative orbit angle to turn center. */
    private FieldDerivativeStructure<T> delta;

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
        factory  = new FDSFactory<>(field, 1, ORDER);
        plusY    = new FieldPVCoordinates<>(field, PLUS_Y_PV);
        minusZ   = new FieldPVCoordinates<>(field, MINUS_Z_PV);
        plusYDS  = plusY.toDerivativeStructurePV(factory.getCompiler().getOrder());
        minusZDS = minusZ.toDerivativeStructurePV(factory.getCompiler().getOrder());

        this.dateDouble    = date.toAbsoluteDate();
        this.date          = date;
        this.dateDS        = addDerivatives(date);
        this.sun           = sun;
        this.pvProv        = pvProv;
        this.inertialFrame = inertialFrame;
        final FieldPVCoordinates<FieldDerivativeStructure<T>> sunPVDS = sun.getPVCoordinates(dateDS, inertialFrame);

        final TimeStampedFieldPVCoordinates<T> svPV = pvProv.getPVCoordinates(date, inertialFrame);
        final FieldPVCoordinates<FieldDerivativeStructure<T>> svPVDS  =
                        svPV.toDerivativeStructurePV(factory.getCompiler().getOrder());
        this.svbCos  = FieldVector3D.dotProduct(sunPVDS.getPosition(), svPVDS.getPosition()).
                       divide(sunPVDS.getPosition().getNorm().multiply(svPVDS.getPosition().getNorm()));

        this.muRate = svPV.getAngularVelocity().getNorm();

        this.turnSpan = turnSpan;

    }

    /** Convert a date, removing derivatives.
     * @param d date to convert
     * @return date without derivatives
     */
    private FieldAbsoluteDate<T> removeDerivatives(final FieldAbsoluteDate<FieldDerivativeStructure<T>> d) {
        final AbsoluteDate                dd     = d.toAbsoluteDate();
        final FieldDerivativeStructure<T> offset = d.durationFrom(dd);
        return new FieldAbsoluteDate<>(date.getField(), dd).shiftedBy(offset.getValue());
    }

    /** Convert a date, adding derivatives.
     * @param d date to convert
     * @return date without derivatives
     */
    private FieldAbsoluteDate<FieldDerivativeStructure<T>> addDerivatives(final FieldAbsoluteDate<T> d) {
        final AbsoluteDate dd     = d.toAbsoluteDate();
        final T            offset = d.durationFrom(dd);
        return new FieldAbsoluteDate<>(factory.getDerivativeField(), dd).shiftedBy(factory.variable(0, offset));
    }

    /** Compute nominal yaw steering.
     * @param d computation date
     * @return nominal yaw steering
     */
    public TimeStampedFieldAngularCoordinates<T> nominalYaw(final FieldAbsoluteDate<T> d) {
        final TimeStampedFieldPVCoordinates<T> svPV = pvProv.getPVCoordinates(d, inertialFrame);
        return new TimeStampedFieldAngularCoordinates<>(d,
                                                        svPV.normalize(),
                                                        sun.getPVCoordinates(d, inertialFrame).crossProduct(svPV).normalize(),
                                                        minusZ,
                                                        plusY,
                                                        1.0e-9);
    }

    /** Compute nominal yaw steering.
     * @param d computation date
     * @return nominal yaw steering
     */
    private TimeStampedFieldAngularCoordinates<FieldDerivativeStructure<T>> nominalYawDS(final FieldAbsoluteDate<FieldDerivativeStructure<T>> d) {
        final FieldPVCoordinates<FieldDerivativeStructure<T>> svPV = pvProv.getPVCoordinates(removeDerivatives(d), inertialFrame).
                                                                     toDerivativeStructurePV(d.getField().getZero().getOrder());
        return new TimeStampedFieldAngularCoordinates<>(d,
                                                        svPV.normalize(),
                                                        sun.getPVCoordinates(d, inertialFrame).crossProduct(svPV).normalize(),
                                                        minusZDS,
                                                        plusYDS,
                                                        1.0e-9);
    }

    /** Compute Sun elevation.
     * @param d computation date
     * @return Sun elevation
     */
    private T beta(final FieldAbsoluteDate<T> d) {
        final TimeStampedFieldPVCoordinates<T> svPV = pvProv.getPVCoordinates(d, inertialFrame);
        return FieldVector3D.angle(sun.getPVCoordinates(d, inertialFrame).getPosition(), svPV.getMomentum()).
               negate().
               add(0.5 * FastMath.PI);
    }

    /** Compute Sun elevation.
     * @return Sun elevation
     */
    public T beta() {
        return beta(date);
    }

    /** Compute Sun elevation.
     * @param d computation date
     * @return Sun elevation
     */
    private FieldDerivativeStructure<T> betaDS(final FieldAbsoluteDate<FieldDerivativeStructure<T>> d) {
        final FieldPVCoordinates<FieldDerivativeStructure<T>> svPV =
                        pvProv.getPVCoordinates(removeDerivatives(d), inertialFrame).
                        toDerivativeStructurePV(d.getField().getZero().getOrder());
        return FieldVector3D.angle(sun.getPVCoordinates(d, inertialFrame).getPosition(), svPV.getMomentum()).
               negate().
               add(0.5 * FastMath.PI);
    }

    /** Compute Sun elevation.
     * @return Sun elevation
     */
    public FieldDerivativeStructure<T> betaDS() {
        return betaDS(dateDS);
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the current date.
     * @return current date
     */
    public FieldAbsoluteDate<FieldDerivativeStructure<T>> getDateDS() {
        return dateDS;
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
        return svbCos.getValue();
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
        final T beta = beta(getDate());
        return FastMath.abs(beta.getReal()) < BETA_SIGN_CHANGE_PROTECTION ?
               beta(turnSpan.getTurnStartDate()) :
               beta;
    }

    /** Compute nominal yaw angle.
     * @param d computation date
     * @return nominal yaw angle
     */
    public T yawAngle(final FieldAbsoluteDate<T> d) {
        final FieldVector3D<T> xSat     = nominalYaw(d).getRotation().applyInverseTo(Vector3D.PLUS_I);
        final FieldVector3D<T> velocity = pvProv.getPVCoordinates(d, inertialFrame).getVelocity();
        return FastMath.copySign(FieldVector3D.angle(velocity, xSat), beta(d).negate());
    }

    /** Compute nominal yaw angle.
     * @param d computation date
     * @return nominal yaw angle
     */
    public FieldDerivativeStructure<T> yawAngleDS(final FieldAbsoluteDate<FieldDerivativeStructure<T>> d) {
        final FieldVector3D<FieldDerivativeStructure<T>> xSat     = nominalYawDS(d).getRotation().applyInverseTo(Vector3D.PLUS_I);
        final FieldVector3D<FieldDerivativeStructure<T>> velocity =
                        pvProv.getPVCoordinates(removeDerivatives(d), inertialFrame).
                        toDerivativeStructurePV(d.getField().getZero().getOrder()).getVelocity();
        return FastMath.copySign(FieldVector3D.angle(velocity, xSat), betaDS(d).negate());
    }

    /** Check if a linear yaw model has already reached target yaw.
     * @param linearPhi value of the linear yaw model
     * @param phiDot slope of the linear yaw model
     * @return true if linear model has already reached target yaw
     */
    public boolean targetYawReached(final T linearPhi, final T phiDot) {
        final FieldAbsoluteDate<T> targetDate;
        if (afterTurnEnd()) {
            // we are after the end of the turn, probably during the recovery margin
            // the nominal yaw is valid, we compare with the current nominal yaw
            targetDate = date;
        } else {
            // we are within the turn
            // the nominal yaw is not yet valid, we compare to the yaw at turn end
            targetDate = turnSpan.getTurnEndDate();
        }
        final T targetYaw = yawAngle(targetDate);

        // find the delay between the turn end and the closest crossing
        // taking care of 2π wrapping (typically GPS block IIR takes only 1800s to perform a full turn)
        final double nowToCrossing     = (targetYaw.getReal() - linearPhi.getReal()) / phiDot.getReal();
        final double nowToTurnEnd      = turnSpan.timeUntilTurnEnd(date).getReal();
        final double turnEndToCrossing = nowToCrossing - nowToTurnEnd;
        final double halfTurn          = FastMath.PI / FastMath.abs(phiDot.getReal());
        final double fullTurn          = 2 * halfTurn;
        final double delay             = turnEndToCrossing - fullTurn * FastMath.floor((turnEndToCrossing + halfTurn) / fullTurn);

        return nowToTurnEnd + delay <= 0;

    }

    /** Set up the midnight/noon turn region.
     * @param cosNight limit cosine for the midnight turn
     * @param cosNoon limit cosine for the noon turn
     * @return true if spacecraft is in the midnight/noon turn region
     */
    public boolean setUpTurnRegion(final double cosNight, final double cosNoon) {
        this.cNight = cosNight;
        this.cNoon  = cosNoon;

        // update relative orbit angle
        final FieldDerivativeStructure<T> absDelta;
        if (svbCos.getValue().getReal() <= 0) {
            // in eclipse turn mode
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos().negate().add(FastMath.PI));
        } else {
            // in noon turn mode
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos());
        }
        delta = absDelta.copySign(absDelta.getPartialDerivative(1).negate());

        if (svbCos.getValue().getReal() < cNight || svbCos.getValue().getReal() > cNoon) {
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
    public FieldDerivativeStructure<T> getDeltaDS() {
        return delta;
    }

    /** Check if spacecraft is in the half orbit closest to Sun.
     * @return true if spacecraft is in the half orbit closest to Sun
     */
    public boolean inSunSide() {
        return svbCos.getValue().getReal() > 0;
    }

    /** Get yaw at turn start.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn start
     */
    public T getYawStart(final T sunBeta) {
        final T halfSpan = turnSpan.getTurnDuration().multiply(muRate).multiply(0.5);
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.getValue()));
    }

    /** Get yaw at turn end.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn end
     */
    public T getYawEnd(final T sunBeta) {
        final T halfSpan = turnSpan.getTurnDuration().multiply(muRate).multiply(0.5);
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.getValue().negate()));
    }

    /** Compute yaw rate.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw rate
     */
    public T yawRate(final T sunBeta) {
        return getYawEnd(sunBeta).subtract(getYawStart(sunBeta)).divide(getTurnDuration());
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
     * turns in orbital plane. The return angle is signed, depending on the
     * spacecraft being before or after turn middle point.
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    private FieldDerivativeStructure<T> inOrbitPlaneAbsoluteAngle(final FieldDerivativeStructure<T> angle) {
        return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta(getDate()))));
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
        if (turnSpan == null) {
            turnSpan = new FieldTurnSpan<>(start, end, getDate().toAbsoluteDate(), endMargin);
        } else {
            turnSpan.update(start, end, getDate().toAbsoluteDate());
        }
    }

    /** Check if context is within turn range.
     * @return true if context is within range extended by end margin
     */
    public boolean inTurnTimeRange() {
        return turnSpan != null && turnSpan.inTurnTimeRange(dateDouble);
    }

    /** Get turn duration.
     * @return turn duration
     */
    public T getTurnDuration() {
        return turnSpan.getTurnDuration();
    }

    /** Get elapsed time since turn start.
     * @return elapsed time from turn start to current date
     */
    public T timeSinceTurnStart() {
        return turnSpan.timeSinceTurnStart(getDate());
    }

    /** Check if we are after turn end (without margin).
     * @return true if we are after turn end (without margin)
     */
    public boolean afterTurnEnd() {
        return date.durationFrom(turnSpan.getTurnEndDate()).getReal() >= 0.0;
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @param yawDot yaw first time derivative
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final T yaw, final T yawDot) {
        return turnCorrectedAttitude(factory.build(yaw, yawDot, yaw.getField().getZero()));
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final FieldDerivativeStructure<T> yaw) {

        // compute nominal yaw
        final FieldDerivativeStructure<T> nominalAngle = yawAngleDS(dateDS);

        // compute a linear yaw correction model
        final TimeStampedFieldAngularCoordinates<T> correction =
                        new TimeStampedFieldAngularCoordinates<>(date,
                                                                 new FieldRotation<>(FieldVector3D.getPlusK(nominalAngle.getField()),
                                                                                     nominalAngle.subtract(yaw),
                                                                                     RotationConvention.VECTOR_OPERATOR));

        // combine the two parts of the attitude
        return correction.addOffset(nominalYaw(date));

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw, using inertial frame as reference
     */
    public TimeStampedFieldAngularCoordinates<T> orbitNormalYaw() {
        final FieldTransform<T> t = LOFType.VVLH.transformFromInertial(date, pvProv.getPVCoordinates(date, inertialFrame));
        return new TimeStampedFieldAngularCoordinates<>(date,
                                                        t.getRotation(),
                                                        t.getRotationRate(),
                                                        t.getRotationAcceleration());
    }

}
