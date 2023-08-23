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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
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

    /** Target in body frame. */
    private final Vector3D target;

    /** Creates a new instance from body frame and target expressed in Cartesian coordinates.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param bodyFrame body frame.
     * @param target target position in body frame
     * @since 7.1
     */
    public TargetPointing(final Frame inertialFrame, final Frame bodyFrame, final Vector3D target) {
        super(inertialFrame, bodyFrame);
        this.target = target;
    }

    /** Creates a new instance from body shape and target expressed in geodetic coordinates.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param targetGeo target defined as a geodetic point in body shape frame
     * @param shape body shape
     * @since 7.1
     */
    public TargetPointing(final Frame inertialFrame, final GeodeticPoint targetGeo, final BodyShape shape) {
        super(inertialFrame, shape.getBodyFrame());
        // Transform target from geodetic coordinates to Cartesian coordinates
        target = shape.transform(targetGeo);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {
        final Transform t = getBodyFrame().getTransformTo(frame, date);
        final TimeStampedPVCoordinates pv =
                new TimeStampedPVCoordinates(date, target, Vector3D.ZERO, Vector3D.ZERO);
        return t.transformPVCoordinates(pv);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {
        final FieldTransform<T> t = getBodyFrame().getTransformTo(frame, date);
        final FieldVector3D<T> zero = FieldVector3D.getZero(date.getField());
        final TimeStampedFieldPVCoordinates<T> pv =
                new TimeStampedFieldPVCoordinates<>(date, new FieldVector3D<>(date.getField(), target), zero, zero);
        return t.transformPVCoordinates(pv);
    }

}
