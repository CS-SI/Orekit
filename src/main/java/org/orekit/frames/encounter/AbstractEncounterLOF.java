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
package org.orekit.frames.encounter;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Abstract class for encounter frame between two objects.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractEncounterLOF implements EncounterLOF {

    /**
     * Other position and velocity of the encounter frame. Can be null.
     * <p>
     * <b>BEWARE: This is not the origin of the encounter local orbital frame !</b>
     */
    private PVCoordinates other;

    /**
     * Other position and velocity of the encounter frame. Can be null.
     * <p>
     * <b>BEWARE: This is not the origin of the encounter local orbital frame !</b>
     */
    private FieldPVCoordinates<?> fieldOther;

    /**
     * Constructor with {@link PVCoordinates}.
     *
     * @param other other object to create the encounter local orbital frame with (<b>not</b> the origin of the frame !)
     */
    protected AbstractEncounterLOF(final PVCoordinates other) {
        this.other = other;
    }

    /**
     * Constructor with {@link FieldPVCoordinates}.
     *
     * @param other other object to create the encounter frame with (<b>not</b> the origin of the frame !)
     * @param <T> type of the field elements
     */
    protected <T extends CalculusFieldElement<T>> AbstractEncounterLOF(final FieldPVCoordinates<T> other) {
        this.fieldOther = other;
    }

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param field field to which the elements belong
     * @param origin position-velocity of the origin in the same inertial frame as the one this instance has been expressed
     * in.
     * @param <T> type of the field elements
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> origin) {
        return rotationFromInertial(field, origin, getFieldOther(field));
    }

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param origin position-velocity of the origin in some inertial frame
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    public Rotation rotationFromInertial(final PVCoordinates origin) {
        return rotationFromInertial(origin, getOther());
    }

    /**
     * Get the field version of other's position and velocity coordinates. If the instance has been created with normal
     * {@link PVCoordinates}, then it will build its field equivalent.
     *
     * @param field field of the elements
     * @param <T> type of the field elements
     *
     * @return field version of other's position and velocity coordinates
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> getFieldOther(final Field<T> field) {
        return fieldOther == null ? new FieldPVCoordinates<>(field, other) : (FieldPVCoordinates<T>) fieldOther;
    }

    /**
     * Get the normal version of other's position and velocity coordinates. If the instance has been created with field
     * {@link FieldPVCoordinates}, then it will convert it to its {@link PVCoordinates} equivalent.
     *
     * @return normal version of other's position and velocity coordinates
     */
    @Override
    public PVCoordinates getOther() {
        return other == null ? fieldOther.toPVCoordinates() : other;
    }
}
