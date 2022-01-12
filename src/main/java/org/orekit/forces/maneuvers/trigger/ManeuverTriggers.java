/* Copyright 2002-2022 CS GROUP
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

package org.orekit.forces.maneuvers.trigger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Generic interface for the maneuver triggers used in a {@link org.orekit.forces.maneuvers.Maneuver}.
 * @author Maxime Journot
 * @since 10.2
 */
public interface ManeuverTriggers {

    /** Initialization method called at propagation start.
     * <p>
     * The default implementation does nothing.
     * </p>
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
        // nothing by default
    }

    /** Initialization method called at propagation start.
     * <p>
     * The default implementation does nothing.
     * </p>
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     * @param <T> type of the elements
     * @since 11.1
     */
    default <T extends CalculusFieldElement<T>> void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
        init(initialState.toSpacecraftState(), target.toAbsoluteDate());
    }

    /** Get the event detectors associated with the triggers.
     * @return the event detectors
     */
    Stream<EventDetector> getEventsDetectors();

    /** Get the event detectors associated with the triggers.
     * @param field field to which the state belongs
     * @param <T> type of the field elements
     * @return the event detectors
     */
    <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(Field<T> field);

    /** Find out if the maneuver is firing or not.
     * @param date current date
     * @param parameters maneuver triggers parameters
     * @return true if the maneuver is firing, false otherwise
     */
    boolean isFiring(AbsoluteDate date, double[] parameters);

    /** Find out if the maneuver is firing or not.
     * @param date current date
     * @param parameters maneuver triggers parameters
     * @param <T> type of the field elements
     * @return true if the maneuver is firing, false otherwise
     */
    <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters);

    /** Get the maneuver triggers parameter drivers.
     * @return maneuver triggers parameter drivers
     */
    default List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get the maneuver name.
     * @return the maneuver name
     */
    default String getName() {
        return "";
    }
}
