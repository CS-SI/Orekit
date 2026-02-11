/* Copyright 2022-2026 Romain Serra
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

package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/** Interface for time-shiftable PV provider holding themselves PV coordinates.
 * @author Romain Serra
 * @since 13.1.2
 */
public interface ShiftablePVCoordinatesHolder<T extends PVCoordinatesProvider>
        extends PVCoordinatesProvider, TimeStamped, TimeShiftable<ShiftablePVCoordinatesHolder<T>> {

    /**
     * Getter for the intrinsic position-velocity vector.
     * @return position-velocity
     */
    TimeStampedPVCoordinates getPVCoordinates();

    /**
     * Getter for the position vector.
     * @return position
     */
    default Vector3D getPosition() {
        return getPVCoordinates().getPosition();
    }

    /**
     * Getter for the velocity vector.
     * @return velocity
     */
    default Vector3D getVelocity() {
        return getPVCoordinates().getVelocity();
    }

    /**
     * Getter for the intrinsic frame.
     * @return frame
     */
    Frame getFrame();

    /** Get the position in a specified frame.
     * @param outputFrame frame in which the position coordinates shall be computed
     * @return position
     */
    default Vector3D getPosition(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // Position vector is returned directly
        if (outputFrame == getFrame()) {
            return getPosition();
        }

        // Else, position vector is transformed to output frame
        final StaticTransform t = getFrame().getStaticTransformTo(outputFrame, getDate());
        return t.transformPosition(getPosition());
    }

    @Override
    default Vector3D getPosition(final AbsoluteDate date, final Frame outputFrame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.durationFrom(getDate()));
        final Vector3D position = shifted.getPosition();
        if (outputFrame == getFrame()) {
            return position;
        }
        final StaticTransform staticTransform = shifted.getFrame().getStaticTransformTo(outputFrame, date);
        return staticTransform.transformPosition(position);
    }

    /** Get the velocity in a specified frame.
     * @param outputFrame frame in which the velocity coordinates shall be computed
     * @return velocity
     */
    default Vector3D getVelocity(final Frame outputFrame) {
        if (outputFrame ==  getFrame()) {
            return getVelocity();
        }
        final KinematicTransform kinematicTransform = getFrame().getKinematicTransformTo(outputFrame, getDate());
        return kinematicTransform.transformOnlyPV(getPVCoordinates()).getVelocity();
    }

    @Override
    default Vector3D getVelocity(final AbsoluteDate date, final Frame outputFrame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.durationFrom(getDate()));
        final TimeStampedPVCoordinates pv = shifted.getPVCoordinates();
        if (outputFrame ==  getFrame()) {
            return pv.getVelocity();
        }
        final KinematicTransform kinematicTransform = shifted.getFrame().getKinematicTransformTo(outputFrame, date);
        final PVCoordinates transformedPV = kinematicTransform.transformOnlyPV(pv);
        return transformedPV.getVelocity();
    }

    /** Get the TimeStampedPVCoordinates in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return TimeStampedPVCoordinates
     * @exception OrekitException if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    default TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == getFrame()) {
            return getPVCoordinates();
        }

        // Else, PV coordinates are transformed to output frame
        final Transform t = getFrame().getTransformTo(outputFrame, getDate());
        return t.transformPVCoordinates(getPVCoordinates());
    }

    @Override
    default TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame outputFrame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.durationFrom(getDate()));
        final TimeStampedPVCoordinates pv = shifted.getPVCoordinates();
        if (outputFrame == getFrame()) {
            return pv;
        }
        final Transform transform = getFrame().getTransformTo(outputFrame, date);
        return transform.transformPVCoordinates(pv);
    }
}
