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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Boilerplate computations for GNSS attitude.
 *
 * <p>
 * This class is intended to hold throw-away data pertaining to <em>one</em> call
 * to {@link GNSSAttitudeProvider#getAttitude(org.orekit.utils.PVCoordinatesProvider,
 * org.orekit.time.AbsoluteDate, org.orekit.frames.Frame) getAttitude}.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
class GNSSAttitudeContext implements TimeStamped {

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

    /** Spacecraft position-velocity in inertial frame. */
    private final TimeStampedPVCoordinates svPV;

    /** Spacecraft position-velocity in inertial frame. */
    private final FieldPVCoordinates<DerivativeStructure> svPVDS;

    /** Angle between Sun and orbital plane. */
    private final DerivativeStructure beta;

    /** Cosine of the angle between spacecraft and Sun direction. */
    private final DerivativeStructure svbCos;

    /** Nominal yaw. */
    private final TimeStampedAngularCoordinates nominalYaw;

    /** Nominal yaw. */
    private final FieldRotation<DerivativeStructure> nominalYawDS;

    /** Spacecraft angular velocity. */
    private double muRate;

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Relative orbit angle to turn center. */
    private DerivativeStructure delta;

    /** Turn time data. */
    private TurnSpan turnSpan;

    /** Simple constructor.
     * @param sunPV Sun position-velocity in inertial frame
     * @param svPV spacecraft position-velocity in inertial frame
     * @param turnSpan turn time data, if a turn has already been identified in the date neighborhood,
     * null otherwise
     * @exception OrekitException if yaw cannot be corrected
     */
    GNSSAttitudeContext(final TimeStampedPVCoordinates sunPV, final TimeStampedPVCoordinates svPV,
                        final TurnSpan turnSpan)
        throws OrekitException {

        final FieldPVCoordinates<DerivativeStructure> sunPVDS = sunPV.toDerivativeStructurePV(ORDER);
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
                        new TimeStampedAngularCoordinates(svPV.getDate(),
                                                          svPV.normalize(),
                                                          PVCoordinates.crossProduct(sunPV, svPV).normalize(),
                                                          MINUS_Z,
                                                          PLUS_Y,
                                                          1.0e-9);
        this.nominalYawDS = nominalYaw.toDerivativeStructureRotation(ORDER);

        // TODO: the Kouba model assumes perfectly circular orbit, it should really be:
        // this.muRate = svPV.getAngularVelocity();
        this.muRate = svPV.getVelocity().getNorm() / svPV.getPosition().getNorm();

        this.turnSpan = turnSpan;

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return svPV.getDate();
    }

    /** Get the turn span.
     * @return turn span, may be null if context is outside of turn
     */
    public TurnSpan getTurnSpan() {
        return turnSpan;
    }

    /** Get the cosine of the angle between spacecraft and Sun direction.
     * @return cosine of the angle between spacecraft and Sun direction.
     */
    public double getSVBcos() {
        return svbCos.getValue();
    }

    /** Get the angle between Sun and orbital plane.
     * @return angle between Sun and orbital plane
     * @see #getBetaDS()
     * @see #getSecuredBeta(TurnTimeRange)
     */
    public double getBeta() {
        return beta.getValue();
    }

    /** Get the angle between Sun and orbital plane.
     * @return angle between Sun and orbital plane
     * @see #getBeta()
     * @see #getSecuredBeta(TurnTimeRange)
     */
    public DerivativeStructure getBetaDS() {
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
    public double getSecuredBeta() {
        return FastMath.abs(beta.getReal()) < BETA_SIGN_CHANGE_PROTECTION ?
               beta.taylor(-turnSpan.timeSinceTurnStart(getDate())) :
               getBeta();
    }

    /** Get the nominal yaw.
     * @return nominal yaw
     */
    public TimeStampedAngularCoordinates getNominalYaw() {
        return nominalYaw;
    }

    /** Compute nominal yaw angle.
     * @return nominal yaw angle
     */
    public double yawAngle() {
        final Vector3D xSat = nominalYaw.getRotation().revert().applyTo(Vector3D.PLUS_I);
        return FastMath.copySign(Vector3D.angle(svPV.getVelocity(), xSat), -beta.getReal());
    }

    /** Compute nominal yaw angle.
     * @return nominal yaw angle
     */
    public DerivativeStructure yawAngleDS() {
        final FieldVector3D<DerivativeStructure> xSat = nominalYawDS.revert().applyTo(Vector3D.PLUS_I);
        return FastMath.copySign(FieldVector3D.angle(svPV.getVelocity(), xSat), -beta.getReal());
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
        final DerivativeStructure absDelta;
        if (svbCos.getValue() <= 0) {
            // night side
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos().negate().add(FastMath.PI));
        } else {
            // Sun side
            absDelta = inOrbitPlaneAbsoluteAngle(svbCos.acos());
        }
        delta = FastMath.copySign(absDelta, -absDelta.getPartialDerivative(1));

        if (svbCos.getValue() < cNight || svbCos.getValue() > cNoon) {
            // we are within turn triggering zone
            return true;
        } else {
            // we are outside of turn triggering zone,
            // but we may still be trying to recover nominal attitude at the end of a turn
            return turnSpan != null && turnSpan.inTurnTimeRange(getDate());
        }

    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public double getDelta() {
        return delta.getValue();
    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public DerivativeStructure getDeltaDS() {
        return delta;
    }

    /** Check if spacecraft is in the half orbit closest to Sun.
     * @return true if spacecraft is in the half orbit closest to Sun
     */
    public boolean inSunSide() {
        return svbCos.getValue() > 0;
    }

    /** Get yaw at turn start.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn start
     */
    public double getYawStart(final double sunBeta) {
        final double halfSpan = 0.5 * turnSpan.getTurnDuration() * muRate;
        return computePhi(sunBeta, FastMath.copySign(halfSpan, svbCos.getValue()));
    }

    /** Get yaw at turn end.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw at turn end
     */
    public double getYawEnd(final double sunBeta) {
        final double halfSpan = 0.5 * turnSpan.getTurnDuration() * muRate;
        return computePhi(sunBeta, FastMath.copySign(halfSpan, -svbCos.getValue()));
    }

    /** Compute yaw rate.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @return yaw rate
     */
    public double yawRate(final double sunBeta) {
        return (getYawEnd(sunBeta) - getYawStart(sunBeta)) / turnSpan.getTurnDuration();
    }

    /** Get the spacecraft angular velocity.
     * @return spacecraft angular velocity
     */
    public double getMuRate() {
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
    private DerivativeStructure inOrbitPlaneAbsoluteAngle(final DerivativeStructure angle) {
        // TODO: the Kouba model assumes planar right-angle triangle resolution, it should really be:
        //  return FastMath.acos(FastMath.cos(angle).divide(FastMath.cos(beta)));
        return angle.multiply(angle).subtract(beta.multiply(beta)).sqrt();
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
    public double inOrbitPlaneAbsoluteAngle(final double angle) {
        // TODO: the Kouba model assumes planar right-angle triangle resolution, it should really be:
        // return FastMath.acos(FastMath.cos(angle) / FastMath.cos(beta.getReal()));
        return FastMath.sqrt(angle * angle - beta.getReal() * beta.getReal());
    }

    /** Compute yaw.
     * @param sunBeta Sun elevation above orbital plane
     * (it <em>may</em> be different from {@link #getBeta()} in
     * some special cases)
     * @param inOrbitPlaneAngle in orbit angle between spacecraft
     * and Sun (or opposite of Sun) projection
     * @return yaw angle
     */
    public double computePhi(final double sunBeta, final double inOrbitPlaneAngle) {
        return FastMath.atan2(-FastMath.tan(sunBeta), FastMath.sin(inOrbitPlaneAngle));
    }

    /** Set turn half span and compute corresponding turn time range.
     * @param halfSpan half span of the turn region, as an angle in orbit plane
     * @param endMargin margin in seconds after turn end
     */
    public void setHalfSpan(final double halfSpan, final double endMargin) {
        final AbsoluteDate start = svPV.getDate().shiftedBy((delta.getValue() - halfSpan) / muRate);
        final AbsoluteDate end   = svPV.getDate().shiftedBy((delta.getValue() + halfSpan) / muRate);
        if (turnSpan == null) {
            turnSpan = new TurnSpan(start, end, getDate(), endMargin);
        } else {
            turnSpan.update(start, end, getDate());
        }
    }

    /** Check if a date is within range.
     * @param date date to check
     * @return true if date is within range extended by end margin
     */
    public boolean inTurnTimeRange(final AbsoluteDate date) {
        return turnSpan.inTurnTimeRange(date);
    }

    /** Get turn duration.
     * @return turn duration
     */
    public double getTurnDuration() {
        return turnSpan.getTurnDuration();
    }

    /** Get elapsed time since turn start.
     * @param date date to check
     * @return elapsed time from turn start to specified date
     */
    public double timeSinceTurnStart(final AbsoluteDate date) {
        return turnSpan.timeSinceTurnStart(date);
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @param yawDot yaw first time derivative
     * @return attitude with specified yaw
     */
    public TimeStampedAngularCoordinates turnCorrectedAttitude(final double yaw, final double yawDot) {
        return turnCorrectedAttitude(beta.getFactory().build(yaw, yawDot, 0.0));
    }

    /** Generate an attitude with turn-corrected yaw.
     * @param yaw yaw value to apply
     * @return attitude with specified yaw
     */
    public TimeStampedAngularCoordinates turnCorrectedAttitude(final DerivativeStructure yaw) {

        // compute a linear yaw correction model
        final DerivativeStructure nominalAngle   = yawAngleDS();
        final TimeStampedAngularCoordinates correction =
                        new TimeStampedAngularCoordinates(nominalYaw.getDate(),
                                                          new FieldRotation<>(FieldVector3D.getPlusK(nominalAngle.getField()),
                                                                              yaw.subtract(nominalAngle),
                                                                              RotationConvention.VECTOR_OPERATOR));

        // combine the two parts of the attitude
        return correction.addOffset(getNominalYaw());

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw, using inertial frame as reference
     */
    public TimeStampedAngularCoordinates orbitNormalYaw() {
        final Transform t = LOFType.VVLH.transformFromInertial(svPV.getDate(), svPV);
        return new TimeStampedAngularCoordinates(svPV.getDate(),
                                                 t.getRotation(),
                                                 t.getRotationRate(),
                                                 t.getRotationAcceleration());
    }

}
