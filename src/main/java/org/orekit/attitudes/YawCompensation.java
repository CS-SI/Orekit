/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.attitudes;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class handles yaw compensation attitude provider.

 * <p>
 * Yaw compensation is mainly used for Earth observation satellites. As a
 * satellites moves along its track, the image of ground points move on
 * the focal point of the optical sensor. This motion is a combination of
 * the satellite motion, but also on the Earth rotation and on the current
 * attitude (in particular if the pointing includes Roll or Pitch offset).
 * In order to reduce geometrical distortion, the yaw angle is changed a
 * little from the simple ground pointing attitude such that the apparent
 * motion of ground points is along a prescribed axis (orthogonal to the
 * optical sensors rows), taking into account all effects.
 * </p>
 * <p>
 * This attitude is implemented as a wrapper on top of an underlying ground
 * pointing law that defines the roll and pitch angles.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class YawCompensation extends GroundPointing implements AttitudeProviderModifier {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

    /** J axis. */
    private static final PVCoordinates PLUS_J =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Underlying ground pointing attitude provider.  */
    private final GroundPointing groundPointingLaw;

    /** Creates a new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public YawCompensation(final Frame inertialFrame, final GroundPointing groundPointingLaw)
        throws OrekitException {
        super(inertialFrame, groundPointingLaw.getBodyFrame());
        this.groundPointingLaw = groundPointingLaw;
    }

    /** Get the underlying (ground pointing) attitude provider.
     * @return underlying attitude provider, which in this case is a {@link GroundPointing} instance
     */
    public AttitudeProvider getUnderlyingAttitudeProvider() {
        return groundPointingLaw;
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /** Compute the base system state at given date, without compensation.
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @return satellite base attitude state, i.e without compensation.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getBaseState(final PVCoordinatesProvider pvProv,
                                 final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return groundPointingLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        final Transform bodyToRef = getBodyFrame().getTransformTo(frame, date);

        // compute sliding target ground point
        final PVCoordinates slidingRef  = getTargetPV(pvProv, date, frame);
        final PVCoordinates slidingBody = bodyToRef.getInverse().transformPVCoordinates(slidingRef);

        // compute relative position of sliding ground point with respect to satellite
        final PVCoordinates relativePosition =
                new PVCoordinates(pvProv.getPVCoordinates(date, frame), slidingRef);

        // compute relative velocity of fixed ground point with respect to sliding ground point
        // the velocity part of the transformPVCoordinates for the sliding point ps
        // from body frame to reference frame is:
        //     d(ps_ref)/dt = r(d(ps_body)/dt + dq/dt) - Ω ⨯ ps_ref
        // where r is the rotation part of the transform, Ω is the corresponding
        // angular rate, and dq/dt is the derivative of the translation part of the
        // transform (the translation itself, without derivative, is hidden in the
        // ps_ref part in the cross product).
        // The sliding point ps is co-located to a fixed ground point pf (i.e. they have the
        // same position at time of computation), but this fixed point as zero velocity
        // with respect to the ground. So the velocity part of the transformPVCoordinates
        // for this fixed point can be computed using the same formula as above:
        // from body frame to reference frame is:
        //     d(pf_ref)/dt = r(0 + dq/dt) - Ω ⨯ pf_ref
        // so remembering that the two points are at the same location at computation time,
        // i.e. that at t=0 pf_ref=ps_ref, the relative velocity between the fixed point
        // and the sliding point is given by the simple expression:
        //     d(ps_ref)/dt - d(pf_ref)/dt = r(d(ps_body)/dt)
        // the acceleration is computed by differentiating the expression, which gives:
        //    d²(ps_ref)/dt² - d²(pf_ref)/dt² = r(d²(ps_body)/dt²) - Ω ⨯ [d(ps_ref)/dt - d(pf_ref)/dt]
        final Vector3D v = bodyToRef.getRotation().applyTo(slidingBody.getVelocity());
        final Vector3D a = new Vector3D(+1, bodyToRef.getRotation().applyTo(slidingBody.getAcceleration()),
                                        -1, Vector3D.crossProduct(bodyToRef.getRotationRate(), v));
        final PVCoordinates relativeVelocity = new PVCoordinates(v, a, Vector3D.ZERO);

        final PVCoordinates relativeNormal =
                PVCoordinates.crossProduct(relativePosition, relativeVelocity).normalize();

        // attitude definition :
        //  . Z satellite axis points to sliding target
        //  . target relative velocity is in (Z,X) plane, in the -X half plane part
        return new Attitude(frame,
                            new TimeStampedAngularCoordinates(date,
                                                              relativePosition.normalize(),
                                                              relativeNormal.normalize(),
                                                              PLUS_K, PLUS_J,
                                                              1.0e-9));

    }

    /** Compute the yaw compensation angle at date.
     * @param pvProv provider for PV coordinates
     * @param date date at which compensation is requested
     * @param frame reference frame from which attitude is computed
     * @return yaw compensation angle for orbit.
     * @throws OrekitException if some specific error occurs
     */
    public double getYawAngle(final PVCoordinatesProvider pvProv,
                              final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        final Rotation rBase        = getBaseState(pvProv, date, frame).getRotation();
        final Rotation rCompensated = getAttitude(pvProv, date, frame).getRotation();
        final Rotation compensation = rCompensated.compose(rBase.revert(), RotationConvention.VECTOR_OPERATOR);
        return -compensation.applyTo(Vector3D.PLUS_I).getAlpha();
    }

}
