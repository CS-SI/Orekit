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
import org.orekit.time.FieldTimeShiftable;

/** Interface for time-shiftable, full Cartesian states.
 * @param <T> type of the object
 * @author Romain Serra
 * @since 14.0
 * @see FieldPVCoordinates
 * @see ShiftablePVCoordinatesHolder
 */
public interface ShiftableFieldPVCoordinatesHolder<T extends CalculusFieldElement<T>>
        extends FieldTimeShiftable<ShiftableFieldPVCoordinatesHolder<T>, T> {

    /**
     * Getter for the intrinsic position-velocity-acceleration vector.
     * @return full vector
     */
    FieldPVCoordinates<T> getPVCoordinates();

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
     * Getter for the acceleration vector.
     * @return acceleration
     */
    default FieldVector3D<T> getAcceleration() {
        return getPVCoordinates().getAcceleration();
    }

}
