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

package org.orekit.forces.maneuvers.propulsion;

import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectorsProvider;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriversProvider;

/** Generic interface for a propulsion model used in a {@link Maneuver}.
 * @author Maxime Journot
 * @since 10.2
 */
public interface PropulsionModel extends ParameterDriversProvider, EventDetectorsProvider {

    /** Initialization method.
     *  Called in when Maneuver.init(...) is called (from ForceModel.init(...))
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
    }

    /** Initialization method.
     *  Called in when Maneuver.init(...) is called (from ForceModel.init(...))
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     * @param <T> type of the elements
     * @since 11.1
     */
    default <T extends CalculusFieldElement<T>> void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
        init(initialState.toSpacecraftState(), target.toAbsoluteDate());
    }

    /** {@inheritDoc}.*/
    @Override
    default Stream<EventDetector> getEventDetectors() {
        return getEventDetectors(getParametersDrivers());
    }

    /** {@inheritDoc}.*/
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return getFieldEventDetectors(field, getParametersDrivers());
    }

    /** Get the acceleration of the spacecraft during maneuver and in maneuver frame.
     * @param s current spacecraft state
     * @param maneuverAttitude current attitude in maneuver
     * @param parameters propulsion model parameters
     * @return acceleration
     */
    Vector3D getAcceleration(SpacecraftState s, Attitude maneuverAttitude, double[] parameters);

    /** Get the acceleration of the spacecraft during maneuver and in maneuver frame.
     * @param s current spacecraft state
     * @param maneuverAttitude current attitude in maneuver
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return acceleration
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getAcceleration(FieldSpacecraftState<T> s,
                                                                         FieldAttitude<T> maneuverAttitude,
                                                                         T[] parameters);

    /** Get the mass derivative (i.e. flow rate in kg/s) during maneuver.
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @return mass derivative in kg/s
     */
    double getMassDerivatives(SpacecraftState s, double[] parameters);

    /** Get the mass derivative (i.e. flow rate in kg/s) during maneuver.
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return mass derivative in kg/s
     */
    <T extends CalculusFieldElement<T>> T getMassDerivatives(FieldSpacecraftState<T> s,
                                                             T[] parameters);
    /** Get the maneuver name.
     * @return the maneuver name
     */
    default String getName() {
        return "";
    }

    /** Get the control vector's cost type.
     * @return control cost type
     * @since 12.0
     */
    Control3DVectorCostType getControl3DVectorCostType();

}
