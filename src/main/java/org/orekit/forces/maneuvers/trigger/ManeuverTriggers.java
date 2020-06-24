/* Copyright 2002-2020 CS GROUP
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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Generic interface for the maneuver triggers used in a {@link Maneuver}.
 * @author Maxime Journot
 * @since 10.2
 */
public interface ManeuverTriggers {

    /** Initialization method.
     *  Called in when Maneuver.init(...) is called (from ForceModel.init(...)).
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
    }

    /** Get the event detectors associated with the triggers.
     * @return the event detectors
     */
    Stream<EventDetector> getEventsDetectors();

    /** Get the event detectors associated with the triggers.
     * @param field field to which the state belongs
     * @param <T> extends RealFieldElement&lt;T&gt;
     * @return the event detectors
     */
    <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(Field<T> field);

    /** Find out if the maneuver is firing or not.
     * @param date current date
     * @param parameters maneuver triggers parameters
     * @return true if the maneuver is firing, false otherwise
     */
    boolean isFiring(AbsoluteDate date, double[] parameters);

    /** Find out if the maneuver is firing or not.
     * @param date current date
     * @param parameters maneuver triggers parameters
     * @param <T> extends RealFieldElement&lt;T&gt;
     * @return true if the maneuver is firing, false otherwise
     */
    <T extends RealFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters);

    /** Get the maneuver triggers parameter drivers.
     * @return maneuver triggers parameter drivers
     */
    default ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {};
    }

    /** Get the maneuver name.
     * @return the maneuver name
     */
    default String getName() {
        return "";
    }
}
