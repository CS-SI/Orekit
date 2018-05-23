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
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.LOFType;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.FieldPVCoordinates;
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

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Limit value below which we shoud use replace beta by betaIni. */
    private static final double BETA_SIGN_CHANGE_PROTECTION = FastMath.toRadians(0.07);

    /** Derivation order. */
    private static final int ORDER = 2;

    /** Constant Y axis. */
    private final FieldPVCoordinates<T> plusY;

    /** Constant Z axis. */
    private final FieldPVCoordinates<T> minusZ;

    /** Spacecraft position-velocity in inertial frame. */
    private final TimeStampedFieldPVCoordinates<T> svPV;

    /** Spacecraft position-velocity in inertial frame. */
    private final FieldPVCoordinates<FieldDerivativeStructure<T>> svPVDS;

    /** Angle between Sun and orbital plane. */
    private final FieldDerivativeStructure<T> beta;

    /** Cosine of the angle between spacecraft and Sun direction. */
    private final FieldDerivativeStructure<T> svbCos;

    /** Nominal yaw. */
    private final TimeStampedFieldAngularCoordinates<T> nominalYaw;

    /** Nominal yaw. */
    private final FieldRotation<FieldDerivativeStructure<T>> nominalYawDS;

    /** Spacecraft angular velocity. */
    private T muRate;

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Relative orbit angle to turn center. */
    private FieldDerivativeStructure<T> delta;

    /** Half span of the turn region, as an angle in orbit plane. */
    private T halfSpan;

    /** Turn start date. */
    private FieldAbsoluteDate<T> turnStart;

    /** Turn end date. */
    private FieldAbsoluteDate<T> turnEnd;

    /** Simple constructor.
     * @param sunPV Sun position-velocity in inertial frame
     * @param svPV spacecraft position-velocity in inertial frame
     * @exception OrekitException if yaw cannot be corrected
     */
    GNSSFieldAttitudeContext(final TimeStampedFieldPVCoordinates<T> sunPV, final TimeStampedFieldPVCoordinates<T> svPV)
        throws OrekitException {

        final Field<T> field = sunPV.getDate().getField();
        plusY  = new FieldPVCoordinates<>(field, PLUS_Y);
        minusZ = new FieldPVCoordinates<>(field, MINUS_Z);

        final FieldPVCoordinates<FieldDerivativeStructure<T>> sunPVDS = sunPV.toDerivativeStructurePV(ORDER);
        this.svPV    = svPV;
        this.svPVDS  = svPV.toDerivativeStructurePV(ORDER);
        this.svbCos  = FieldVector3D.dotProduct(sunPVDS.getPosition(), svPVDS.getPosition()).
                       divide(sunPVDS.getPosition().getNorm().
                       multiply(svPVDS.getPosition().getNorm()));
        this.beta    = FieldVector3D.angle(sunPVDS.getPosition(), svPVDS.getMomentum()).
                       negate().
                       add(0.5 * FastMath.PI);

        // nominal yaw steering
        this.nominalYaw =
                        new TimeStampedFieldAngularCoordinates<>(svPV.getDate(),
                                                                 svPV.normalize(),
                                                                 sunPV.crossProduct(svPV).normalize(),
                                                                 minusZ,
                                                                 plusY,
                                                                 1.0e-9);
        this.nominalYawDS = nominalYaw.toDerivativeStructureRotation(ORDER);

        this.muRate = svPV.getAngularVelocity().getNorm();

    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return svPV.getDate();
    }

    /** Get the cosine of the angle between spacecraft and Sun direction.
     * @return cosine of the angle between spacecraft and Sun direction.
     */
    public T getSVBcos() {
        return svbCos.getValue();
    }

    /** Get the angle between Sun and orbital plane.
     * @return angle between Sun and orbital plane
     * @see #getBetaDS()
     * @see #getSecuredBeta(TurnTimeRange)
     */
    public T getBeta() {
        return beta.getValue();
    }

    /** Get the angle between Sun and orbital plane.
     * @return angle between Sun and orbital plane
     * @see #getBeta()
     * @see #getSecuredBeta(TurnTimeRange)
     */
    public FieldDerivativeStructure<T> getBetaDS() {
        return beta;
    }

    /** Get a Sun elevation angle that does not change sign within the turn.
     * <p>
     * This method either returns the current beta or replaces it with the
     * value at turn start, so the sign remains constant throughout the
     * turn. As of 9.2, it is only useful for GPS and Glonass.
     * </p>
     * @return secured Sun elevation angle
     * @see #getBeta()
     * @see #getBetaDS()
     */
    public T getSecuredBeta() {
        return FastMath.abs(beta.getReal()) < BETA_SIGN_CHANGE_PROTECTION ?
               beta.taylor(timeSinceTurnStart(getDate()).negate()) :
               getBeta();
    }

    /** Get the nominal yaw.
     * @return nominal yaw
     */
    public TimeStampedFieldAngularCoordinates<T> getNominalYaw() {
        return nominalYaw;
    }

    /** Compute nominal yaw angle.
     * @return nominal yaw angle
     */
    public T yawAngle() {
        final FieldVector3D<T> xSat = nominalYaw.getRotation().revert().applyTo(Vector3D.PLUS_I);
        return FastMath.copySign(FieldVector3D.angle(svPV.getVelocity(), xSat), -beta.getReal());
    }

    /** Compute nominal yaw angle.
     * @return nominal yaw angle
     */
    public FieldDerivativeStructure<T> yawAngleDS() {
        final FieldVector3D<FieldDerivativeStructure<T>> xSat    = nominalYawDS.revert().applyTo(Vector3D.PLUS_I);
        final FDSFactory<T>                              factory = xSat.getX().getFactory();
        final FieldVector3D<T>                           v       = svPV.getVelocity();
        final FieldVector3D<FieldDerivativeStructure<T>> vDS     = new FieldVector3D<>(factory.constant(v.getX()),
                                                                                       factory.constant(v.getY()),
                                                                                       factory.constant(v.getZ()));
        return FieldVector3D.angle(vDS, xSat).copySign(beta.getValue().negate());
    }

    /** Set up the midnight/noon turn region.
     * @param cosNight limit cosine for the midnight turn
     * @param cosNoon limit cosine for the noon turn
     * @return true if spacecraft is in the midnight/noon turn region
     */
    public boolean setUpTurnRegion(final double cosNight, final double cosNoon) {
        this.cNight = cosNight;
        this.cNoon  = cosNoon;
        if (svbCos.getValue().getReal() < cNight) {
            // in eclipse turn mode
            final FieldDerivativeStructure<T> absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos().negate().add(FastMath.PI));
            delta = absDelta.copySign(absDelta.getPartialDerivative(1).negate());
            return true;
        } else if (svbCos.getValue().getReal() > cNoon) {
            // in noon turn mode
            final FieldDerivativeStructure<T> absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos());
            delta = absDelta.copySign(absDelta.getPartialDerivative(1).negate());
            return true;
        } else {
            return false;
        }
    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public T getDelta() {
        return delta.getValue();
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
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.getValue()));
    }

    /** Get yaw at turn end.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn end
     */
    public T getYawEnd(final T sunBeta) {
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
        return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta)));
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
        return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta.getReal())));
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
     */
    public void setHalfSpan(final T halfSpan) {
        this.halfSpan  = halfSpan;
        this.turnStart = svPV.getDate().shiftedBy(delta.getValue().subtract(halfSpan).divide(muRate));
        this.turnEnd   = svPV.getDate().shiftedBy(delta.getValue().add(halfSpan).divide(muRate));
    }

    /** Check if a date is within range.
     * @param date date to check
     * @param endMargin margin in seconds after turn end
     * @return true if date is within range extended by end margin
     */
    public boolean inTurnTimeRange(final FieldAbsoluteDate<T> date, final double endMargin) {
        return date.durationFrom(turnStart).getReal() > 0 &&
               date.durationFrom(turnEnd).getReal()   < endMargin;
    }

    /** Get turn duration.
     * @return turn duration
     */
    public T getTurnDuration() {
        return halfSpan.multiply(2).divide(muRate);
    }

    /** Get elapsed time since turn start.
     * @param date date to check
     * @return elapsed time from turn start to specified date
     */
    public T timeSinceTurnStart(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(turnStart);
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @param yawDot yaw first time derivative
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final T yaw, final T yawDot) {
        return turnCorrectedAttitude(beta.getFactory().build(yaw, yawDot, yaw.getField().getZero()));
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @return attitude with specified yaw
     */
    public TimeStampedFieldAngularCoordinates<T> turnCorrectedAttitude(final FieldDerivativeStructure<T> yaw) {

        // compute a linear yaw correction model
        final FieldDerivativeStructure<T> nominalAngle   = yawAngleDS();
        final TimeStampedFieldAngularCoordinates<T> correction =
                        new TimeStampedFieldAngularCoordinates<>(nominalYaw.getDate(),
                                                                 new FieldRotation<>(FieldVector3D.getPlusK(nominalAngle.getField()),
                                                                                     yaw.subtract(nominalAngle),
                                                                                     RotationConvention.VECTOR_OPERATOR));

        // combine the two parts of the attitude
        return correction.addOffset(getNominalYaw());

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw, using inertial frame as reference
     */
    public TimeStampedFieldAngularCoordinates<T> orbitNormalYaw() {
        final FieldTransform<T> t = LOFType.VVLH.transformFromInertial(svPV.getDate(), svPV);
        return new TimeStampedFieldAngularCoordinates<>(svPV.getDate(),
                                                        t.getRotation(),
                                                        t.getRotationRate(),
                                                        t.getRotationAcceleration());
    }

}
