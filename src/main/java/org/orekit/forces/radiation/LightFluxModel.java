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
package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.util.List;

/**
 * Interface describing flux models from a light source, including shadowing effects from occulting bodies.
 * Defines the flux vector itself as well as detectors for entry and exit of the different eclipse zones, if any.
 *
 * @author Romain Serra
 * @since 12.1
 */
public interface LightFluxModel {

    /**
     * Perform initialization steps before starting propagation.
     * @param initialState initial state
     * @param targetDate target date for propagation
     * @since 12.2
     */
    default void init(final SpacecraftState initialState, final AbsoluteDate targetDate) {
        // nothing by default
    }

    /**
     * Perform initialization steps before starting propagation.
     * @param initialState initial state
     * @param targetDate target date for propagation
     * @param <T> field type
     * @since 12.2
     */
    default <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState,
                                                          final FieldAbsoluteDate<T> targetDate) {
        // nothing by default
    }

    /**
     * Get the light flux vector in the state's frame.
     * @param state state
     * @return light flux
     */
    Vector3D getLightFluxVector(SpacecraftState state);

    /**
     * Get the light flux vector in the state's frame. Field version.
     * @param state state
     * @return light flux
     * @param <T> field type
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getLightFluxVector(FieldSpacecraftState<T> state);

    /**
     * Retrieve detectors finding entries and exits in different eclipse zones.
     * @return list of event detectors
     */
    List<EventDetector> getEclipseConditionsDetector();

    /**
     * Retrieve Field detectors finding entries and exits in different eclipse zones.
     * @param field calculus field
     * @return list of event detectors
     * @param <T> field type
     */
    <T extends CalculusFieldElement<T>> List<FieldEventDetector<T>> getFieldEclipseConditionsDetector(Field<T> field);
}
