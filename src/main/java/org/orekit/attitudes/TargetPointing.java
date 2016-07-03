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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class handles target pointing attitude provider.

 * <p>
 * This class represents the attitude provider where the satellite z axis is
 * pointing to a ground point target.</p>
 * <p>
 * The target position is defined in a body frame specified by the user.
 * It is important to make sure this frame is consistent.
 * </p>
 * <p>
 * The object <code>TargetPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class TargetPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

    /** Target in body frame. */
    private final Vector3D target;

    /** Creates a new instance from body frame and target expressed in cartesian coordinates.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param bodyFrame body frame.
     * @param target target position in body frame
     * @exception OrekitException if the first frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public TargetPointing(final Frame inertialFrame, final Frame bodyFrame, final Vector3D target)
        throws OrekitException {
        super(inertialFrame, bodyFrame);
        this.target = target;
    }

    /** Creates a new instance from body shape and target expressed in geodetic coordinates.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param targetGeo target defined as a geodetic point in body shape frame
     * @param shape body shape
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public TargetPointing(final Frame inertialFrame, final GeodeticPoint targetGeo, final BodyShape shape)
        throws OrekitException {
        super(inertialFrame, shape.getBodyFrame());
        // Transform target from geodetic coordinates to Cartesian coordinates
        target = shape.transform(targetGeo);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        final Transform t = getBodyFrame().getTransformTo(frame, date);
        final TimeStampedPVCoordinates pv =
                new TimeStampedPVCoordinates(date, target, Vector3D.ZERO, Vector3D.ZERO);
        return t.transformPVCoordinates(pv);
    }

}
