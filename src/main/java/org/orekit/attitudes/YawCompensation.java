/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


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
public class YawCompensation extends GroundPointingWrapper {

    /** Serializable UID. */
    private static final long serialVersionUID = 1145977506851433023L;

    /** Creates a new instance.
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     */
    public YawCompensation(final GroundPointing groundPointingLaw) {
        super(groundPointingLaw);
    }

    /** {@inheritDoc} */
    public Rotation getCompensation(final PVCoordinatesProvider pvProv,
                                    final AbsoluteDate date, final Frame orbitFrame,
                                    final Attitude base)
        throws OrekitException {

        // compute relative velocity of FIXED ground point with respect to satellite
        // beware the point considered is NOT the sliding point on central body surface
        // as returned by getUnderlyingAttitudeProvider().getTargetPV(), but the fixed
        // point that at current time is the target, but before and after is only a body
        // surface point with its own motion and not aligned with satellite Z axis.
        // So the following computation needs to recompute velocity by itself, using
        // the velocity provided by getTargetPV would be wrong!
        final Frame bodyFrame  = getBodyFrame();
        final Vector3D surfacePointLocation = ((GroundPointing) getUnderlyingAttitudeProvider()).getTargetPoint(pvProv, date, orbitFrame);
        final Vector3D bodySpin = bodyFrame.getTransformTo(orbitFrame, date).getRotationRate().negate();
        final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, surfacePointLocation);
        final Vector3D satVelocity = pvProv.getPVCoordinates(date, orbitFrame).getVelocity();
        final Vector3D satPosition = pvProv.getPVCoordinates(date, orbitFrame).getPosition();
        final Plane sspPlane = new Plane(surfacePointLocation);
        final Vector3D satVelocityHorizonal = sspPlane.toSpace(sspPlane.toSubSpace(satVelocity));
        final Vector3D satVelocityAtSurface = satVelocityHorizonal.scalarMultiply(surfacePointLocation.getNorm() / satPosition.getNorm());

        final Vector3D relativeVelocity = surfacePointVelocity.subtract(satVelocityAtSurface);

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . target relative velocity is in (Z,X) plane, in the -X half plane part
        final Rotation compensation =
            new Rotation(Vector3D.PLUS_K, base.getRotation().applyTo(relativeVelocity),
                         Vector3D.PLUS_K, Vector3D.MINUS_I);

        return compensation;

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
        return getCompensation(pvProv, date, frame, getBaseState(pvProv, date, frame)).getAngle();
    }

}
