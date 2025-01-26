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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriversProvider;

/**
 * Interface for (attitude) rotation models taking as inputs a spacecraft state and model parameters.
 * The rotation is defined between a reference frame and the satellite one.
 *
 * @author Romain Serra
 * @see SpacecraftState
 * @see FieldSpacecraftState
 * @see Rotation
 * @see FieldRotation
 * @see Attitude
 * @see FieldAttitude
 * @see org.orekit.forces.maneuvers.Maneuver
 * @since 13.0
 */
public interface AttitudeRotationModel extends ParameterDriversProvider {

    /**
     * Computed the rotation given the input state and parameters' values.
     * @param state spacecraft state
     * @param parameters values for parameter drivers
     * @return attitude's rotation
     */
    Rotation getAttitudeRotation(SpacecraftState state, double[] parameters);

    /**
     * Computed the rotation given the input state and parameters' values.
     * @param state spacecraft state
     * @param parameters values for parameter drivers
     * @param <T> field type
     * @return attitude's rotation
     */
    <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(FieldSpacecraftState<T> state, T[] parameters);
}
