/* Copyright 2022-2025 Romain Serra
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

    @Override
    default Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.accurateDurationFrom(getDate()));
        final Vector3D position = shifted.getPosition();
        if (frame ==  getFrame()) {
            return position;
        }
        final StaticTransform staticTransform = shifted.getFrame().getStaticTransformTo(frame, date);
        return staticTransform.transformPosition(position);
    }

    @Override
    default Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.accurateDurationFrom(getDate()));
        final TimeStampedPVCoordinates pv = shifted.getPVCoordinates();
        if (frame ==  getFrame()) {
            return pv.getVelocity();
        }
        final KinematicTransform kinematicTransform = shifted.getFrame().getKinematicTransformTo(frame, date);
        final PVCoordinates transformedPV = kinematicTransform.transformOnlyPV(pv);
        return transformedPV.getVelocity();
    }

    @Override
    default TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final ShiftablePVCoordinatesHolder<T> shifted = shiftedBy(date.accurateDurationFrom(getDate()));
        final TimeStampedPVCoordinates pv = shifted.getPVCoordinates();
        if (frame ==  getFrame()) {
            return pv;
        }
        final Transform transform = getFrame().getTransformTo(frame, date);
        return transform.transformPVCoordinates(pv);
    }
}
