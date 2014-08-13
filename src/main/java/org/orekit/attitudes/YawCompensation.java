/* Copyright 2002-2014 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
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
    private static final long serialVersionUID = 20140811L;

    /** I axis. */
    private static final PVCoordinates PLUS_I =
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Underlying ground pointing attitude provider.  */
    private final GroundPointing groundPointingLaw;

    /** Creates a new instance.
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     */
    public YawCompensation(final GroundPointing groundPointingLaw) {
        super(groundPointingLaw.getBodyFrame());
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

        // attitude from base attitude provider
        final Attitude base = getBaseState(pvProv, date, frame);

        // compute yaw compensation
        final TimeStampedAngularCoordinates compensation =
                getCompensation(pvProv, date, frame, base);

        // add compensation
        return new Attitude(frame, compensation.addOffset(base.getOrientation()));

    }

    /** Compute the compensation rotation at given date.
     * @param pvProv provider for PV coordinates
     * @param date date at which rotation is requested
     * @param frame reference frame from which attitude is computed
     * @param base base satellite attitude in given frame.
     * @return compensation rotation at date, i.e rotation between non compensated
     * attitude state and compensated state.
     * @throws OrekitException if some specific error occurs
     */
    private TimeStampedAngularCoordinates getCompensation(final PVCoordinatesProvider pvProv,
                                                          final AbsoluteDate date, final Frame frame,
                                                          final Attitude base)
        throws OrekitException {

        final Transform bodyToRef = getBodyFrame().getTransformTo(frame, date);

        // compute sliding target ground point
        final PVCoordinates slidingRef  = getTargetPV(pvProv, date, frame);
        final Vector3D      slidingBody = bodyToRef.getInverse().transformPosition(slidingRef.getPosition());

        // the sliding target point is superimposed to a ground point at current date,
        // but this ground point has its own velocity due to central body rotation,
        // which is unrelated to the sliding point velocity
        final PVCoordinates fixedBody = new PVCoordinates(slidingBody, Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinates fixedRef  = bodyToRef.transformPVCoordinates(fixedBody);

        // compute relative velocity of FIXED ground point with respect to satellite
        final PVCoordinates rel = new PVCoordinates(fixedRef, slidingRef);

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . target relative velocity is in (Z,X) plane, in the -X half plane part
        return new TimeStampedAngularCoordinates(date,
                                                 PLUS_K, base.getOrientation().applyTo(new PVCoordinates(rel.getVelocity(), rel.getAcceleration(), Vector3D.ZERO)),
                                                 PLUS_K, PLUS_I);

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
        final TimeStampedAngularCoordinates compensation =
                getCompensation(pvProv, date, frame, getBaseState(pvProv, date, frame));
        return compensation.getRotation().applyTo(Vector3D.PLUS_I).getAlpha();
    }

}
