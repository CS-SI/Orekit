/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Holder for attitude context data.
 *
 * <p>
 * This class is intended to hold data pertaining to <em>one</em> call
 * to {@link GNSSAttitudeProvider#getAttitude(org.orekit.utils.PVCoordinatesProvider,
 * org.orekit.time.AbsoluteDate, org.orekit.frames.Frame) getAttitude}. It allows
 * the various {@link GNSSAttitudeProvider} implementations to be immutable as they
 * do not store any state, and hence thread-safe and reentrant. This is helpful when
 * propagating a full GNSS constellation where several spacecrafts may share the same
 * attitude provider.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
class GNSSAttitudeContext implements TimeStamped {

    /** Constant X axis. */
    private static final PVCoordinates PLUS_X =
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Y axis. */
    private static final PVCoordinates PLUS_Y =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** Constant Z axis. */
    private static final PVCoordinates MINUS_Z =
            new PVCoordinates(Vector3D.MINUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Indicator for half orbit from orbital noon to orbital midnight or the other way round. */
    private final double towardsEclipse;

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

    /** Limit cosine for the midnight turn. */
    private double cNight;

    /** Limit cosine for the noon turn. */
    private double cNoon;

    /** Relative orbit angle to turn center. */
    private DerivativeStructure det;

    /** Yaw. */
    private DerivativeStructure phi;

    /** Simple constructor.
     * @param sunPV Sun position-velocity in inertial frame
     * @param svPV spacecraft position-velocity in inertial frame
     * @exception OrekitException if yaw cannot be corrected
     */
    GNSSAttitudeContext(final TimeStampedPVCoordinates sunPV, final TimeStampedPVCoordinates svPV)
        throws OrekitException {

        this.towardsEclipse = -Vector3D.dotProduct(sunPV.getPosition(), svPV.getVelocity());
        this.svPV    = svPV;
        final FieldPVCoordinates<DerivativeStructure> sunPVDS = sunPV.toDerivativeStructurePV(2);
        this.svPVDS  = svPV.toDerivativeStructurePV(2);
        final DerivativeStructure r      = svPVDS.getPosition().getNorm();
        this.svbCos = FieldVector3D.dotProduct(sunPVDS.getPosition(), svPVDS.getPosition()).
                                           divide(sunPVDS.getPosition().getNorm().multiply(r));
        this.beta   = FieldVector3D.angle(sunPVDS.getPosition(), svPVDS.getMomentum()).
                                           negate().
                                           add(0.5 * FastMath.PI);

        // nominal yaw steering
        this.nominalYaw =
                        new TimeStampedAngularCoordinates(svPV.getDate(),
                                                          svPV.normalize(),
                                                          PVCoordinates.crossProduct(svPV, sunPV).normalize(),
                                                          MINUS_Z,
                                                          PLUS_Y,
                                                          1.0e-9);

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return svPV.getDate();
    }

    /** Get the spacecraft position-velocity in inertial frame.
     * @return spacecraft position-velocity in inertial frame
     */
    public TimeStampedPVCoordinates getPV() {
        return svPV;
    }

    /** Get the spacecraft position-velocity in inertial frame.
     * @return spacecraft position-velocity in inertial frame
     */
    public FieldPVCoordinates<DerivativeStructure> getPVDS() {
        return svPVDS;
    }

    /** Get the angle between Sun and orbital plane.
     * @return angle between Sun and orbital plane
     */
    public DerivativeStructure getBeta() {
        return beta;
    }

    /** Get the cosine of the angle between spacecraft and Sun direction.
     * @return cosine of the angle between spacecraft and Sun direction
     */
    public DerivativeStructure getSvbCos() {
        return svbCos;
    }

    /** Get the nominal yaw.
     * @return nominal yaw
     */
    public TimeStampedAngularCoordinates getNominalYaw() {
        return nominalYaw;
    }

    /** Set up the midnight/noon turn region.
     * @param cosNight limit cosine for the midnight turn
     * @param cosNoon limit cosine for the noon turn
     * @return true if spacecraft is in the midnight/noon turn region
     */
    public boolean inTurnRegion(final double cosNight, final double cosNoon) {
        this.cNight = cosNight;
        this.cNoon  = cosNoon;
        if (svbCos.getValue() < cNight) {
            // in eclipse turn mode
            final DerivativeStructure piMB = svbCos.acos().negate().add(FastMath.PI);
            det = inOrbitPlaneAngle(piMB).copySign(towardsEclipse());
            phi = beta.tan().negate().atan2(det.negate().sin());
            return true;
        } else if (svbCos.getValue() > cNoon) {
            // in noon turn mode
            final DerivativeStructure b   = svbCos.acos();
            det = inOrbitPlaneAngle(b).copySign(-towardsEclipse());
            phi = beta.tan().negate().atan2(det.sin());
            return true;
        } else {
            return false;
        }
    }

    /** Check if spacecraft is in the half orbit closest to Sun.
     * @return true if spacecraft is in the half orbit closest to Sun
     */
    public boolean inSunSide() {
        return svbCos.getValue() > 0;
    }

    /** Get the relative orbit angle to turn center.
     * @return relative orbit angle to turn center
     */
    public DerivativeStructure getDet() {
        return det;
    }

    /** Get the spacecraft angular velocity.
     * @return spacecraft angular velocity
     */
    public double getMuRate() {
        // TODO: the Kouba model assumes perfectly circular orbit, it should really be:
        // return svPV.getAngularVelocity();
        return svPV.getVelocity().getNorm() / svPV.getPosition().getNorm();
    }

    /** Check if spacecraft is going from orbital noon to orbital midnight or the other way round.
     * @return a positive sign if spacecraft is going from orbital noon to orbital midnight, negative otherwise
     */
    public double towardsEclipse() {
        return towardsEclipse;
    }

    /** Project a spacecraft/Sun angle into orbital plane.
     * <p>
     * This method is intended to find the limits of the noon and midnight
     * turns in orbital plane. The return angle is always positive. The
     * {@link #towardsEclipse()} method must be called to find the correct
     * sign to apply
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    public DerivativeStructure inOrbitPlaneAngle(final DerivativeStructure angle) {
        // TODO: the Kouba model assumes planar right-angle triangle resolution, it should really be:
        // return angle.cos().divide(beta.cos().acos();
        return angle.multiply(angle).subtract(beta.multiply(beta)).sqrt();
    }

    /** Project a spacecraft/Sun angle into orbital plane.
     * <p>
     * This method is intended to find the limits of the noon and midnight
     * turns in orbital plane. The return angle is always positive. The
     * {@link #towardsEclipse()} method must be called to find the correct
     * sign to apply
     * </p>
     * @param angle spacecraft/Sun angle (or spacecraft/opposite-of-Sun)
     * @return angle projected into orbital plane, always positive
     */
    public double inOrbitPlaneAngle(final double angle) {
        // TODO: the Kouba model assumes planar right-angle triangle resolution, it should really be:
        // return FastMath.acos(FastMath.cos(angle) / FastMath.cos(beta.getReal()));
        return FastMath.sqrt(angle * angle - beta.getReal() * beta.getReal());
    }

    /** Combine nominal yaw and correction.
     * @param yawCorrection yaw correction angle
     * @param yawCorrectionDot yaw correction angle derivative
     * @return corrected yaw
     */
    public TimeStampedAngularCoordinates applyCorrection(final double yawCorrection,
                                                         final double yawCorrectionDot) {

        // compute a linear correction model
        final AngularCoordinates correction =
                        new AngularCoordinates(new Rotation(Vector3D.PLUS_K, yawCorrection,
                                                            RotationConvention.FRAME_TRANSFORM),
                                               new Vector3D(yawCorrectionDot, Vector3D.PLUS_K));

        // combine the correction with the nominal yaw
        return nominalYaw.addOffset(correction);

    }

    /** Compute Orbit Normal (ON) yaw.
     * @return Orbit Normal yaw
     * @exception OrekitException if derivation order is too large (never happens with hard-coded order)
     */
    public TimeStampedAngularCoordinates orbitNormalYaw()
        throws OrekitException {
        final Vector3D p             = svPV.getPosition();
        final Vector3D v             = svPV.getVelocity();
        final Vector3D a             = svPV.getAcceleration();
        final double   r2            = p.getNormSq();
        final double   r             = FastMath.sqrt(r2);
        final Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity = new PVCoordinates(v, a, keplerianJerk);
        final PVCoordinates normal   =
                        new PVCoordinates(FieldVector3D.crossProduct(svPV.toDerivativeStructureVector(2),
                                                                     velocity.toDerivativeStructureVector(2)).
                                          normalize());

        return new TimeStampedAngularCoordinates(svPV.getDate(), MINUS_Z, svPV, PLUS_Y, normal, 1.0e-9);
    }

    /** Compute nominal yaw angle.
     * @return nominal yaw angle
     * @exception OrekitException if the user specified order is too large
     * (never really thrown)
     */
    public DerivativeStructure yawAngle()
        throws OrekitException {
        final int order = svPVDS.getPosition().getX().getOrder();
        final FieldVector3D<DerivativeStructure> xSat =
                        nominalYaw.revert().applyTo(PLUS_X).toDerivativeStructureVector(order);
        return FieldVector3D.angle(svPVDS.getVelocity(), xSat).copySign(-beta.getReal());
    }

    /** Compute turn time range.
     * @param halfSpan half span of the turn region, as an angle in orbit plane
     * @return turn time range
     */
    public TurnTimeRange turnTimeRange(final double halfSpan) {
        final double       offsetToCenter = det.getReal();
        final double       muRate         = getMuRate();
        final AbsoluteDate startDate      = svPV.getDate().shiftedBy((offsetToCenter - halfSpan) / muRate);
        final AbsoluteDate endDate        = svPV.getDate().shiftedBy((offsetToCenter + halfSpan) / muRate);
        return new TurnTimeRange(startDate, endDate);
    }

}
