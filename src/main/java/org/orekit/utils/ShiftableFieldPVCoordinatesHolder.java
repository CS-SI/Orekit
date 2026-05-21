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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.FieldKinematicTransform;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeShiftable;
import org.orekit.time.FieldTimeStamped;

/** Interface for time-shiftable Field PV provider holding themselves PV coordinates.
 * @author Romain Serra
 * @since 14.0
 * @see ShiftablePVCoordinatesHolder
 */
public interface ShiftableFieldPVCoordinatesHolder<S extends FieldPVCoordinatesProvider<T>, T extends CalculusFieldElement<T>>
        extends FieldPVCoordinatesProvider<T>, FieldTimeStamped<T>, FieldTimeShiftable<ShiftableFieldPVCoordinatesHolder<S, T>, T> {

    /**
     * Getter for the intrinsic position-velocity vector.
     * @return position-velocity
     */
    TimeStampedFieldPVCoordinates<T> getPVCoordinates();

    /**
     * Getter for the position vector.
     * @return position
     */
    default FieldVector3D<T> getPosition() {
        return getPVCoordinates().getPosition();
    }

    /**
     * Getter for the velocity vector.
     * @return velocity
     */
    default FieldVector3D<T> getVelocity() {
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
    default FieldVector3D<T> getPosition(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // Position vector is returned directly
        if (outputFrame == getFrame()) {
            return getPosition();
        }

        // Else, position vector is transformed to output frame
        final FieldStaticTransform<T> t = getFrame().getStaticTransformTo(outputFrame, getDate());
        return t.transformPosition(getPosition());

    }

    @Override
    default FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date, final Frame outputFrame) {
        final ShiftableFieldPVCoordinatesHolder<S, T> shifted = shiftedBy(date.durationFrom(getDate()));
        final FieldVector3D<T> position = shifted.getPosition();
        if (outputFrame == getFrame()) {
            return position;
        }
        final FieldStaticTransform<T> staticTransform = shifted.getFrame().getStaticTransformTo(outputFrame, date);
        return staticTransform.transformPosition(position);
    }

    /** Get the velocity in a specified frame.
     * @param outputFrame frame in which the velocity coordinates shall be computed
     * @return velocity
     */
    default FieldVector3D<T> getVelocity(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // velocity is returned directly
        if (outputFrame == getFrame()) {
            return getVelocity();
        }

        // Else, velocity is transformed to output frame
        final FieldKinematicTransform<T> t = getFrame().getKinematicTransformTo(outputFrame, getDate());
        return t.transformOnlyPV(getPVCoordinates()).getVelocity();
    }

    @Override
    default FieldVector3D<T> getVelocity(final FieldAbsoluteDate<T> date, final Frame outputFrame) {
        final ShiftableFieldPVCoordinatesHolder<S, T> shifted = shiftedBy(date.durationFrom(getDate()));
        final TimeStampedFieldPVCoordinates<T> pv = shifted.getPVCoordinates();
        if (outputFrame == getFrame()) {
            return pv.getVelocity();
        }
        final FieldKinematicTransform<T> kinematicTransform = shifted.getFrame().getKinematicTransformTo(outputFrame, date);
        final FieldPVCoordinates<T> transformedPV = kinematicTransform.transformOnlyPV(pv);
        return transformedPV.getVelocity();
    }

    /** Get the position-velocity vector in a specified frame.
     * @param outputFrame frame in which the coordinates shall be computed
     * @return vector
     */
    default TimeStampedFieldPVCoordinates<T> getPVCoordinates(final Frame outputFrame) {
        if (outputFrame == getFrame()) {
            return getPVCoordinates();
        }
        final FieldTransform<T> transform = getFrame().getTransformTo(outputFrame, getDate());
        return transform.transformPVCoordinates(getPVCoordinates());
    }

    @Override
    default TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame outputFrame) {
        final ShiftableFieldPVCoordinatesHolder<S, T> shifted = shiftedBy(date.durationFrom(getDate()));
        final TimeStampedFieldPVCoordinates<T> pv = shifted.getPVCoordinates();
        if (outputFrame == getFrame()) {
            return pv;
        }
        final FieldTransform<T> transform = getFrame().getTransformTo(outputFrame, date);
        return transform.transformPVCoordinates(pv);
    }

}
