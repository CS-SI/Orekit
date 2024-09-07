/* Copyright 2002-2024 CS GROUP
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
package org.orekit.forces.drag;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Interface for spacecraft that are sensitive to atmospheric drag forces.
 *
 * @see DragForce
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface DragSensitive {

    /** Parameter name for global multiplicative factor.
     * @since 12.0
     */
    String GLOBAL_DRAG_FACTOR = "global drag factor";

    /** Parameter name for drag coefficient. */
    String DRAG_COEFFICIENT = "drag coefficient";

    /** Parameter name for lift ration enabling Jacobian processing.
     * <p>
     * The lift ratio is the proportion of atmosphere modecules that will
     * experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection. The ratio is between 0 and 1,
     * 0 meaning there are no specular reflection, only diffuse reflection,
     * and hence no lift effect.
     * </p>
     * @since 9.0
     */
    String LIFT_RATIO = "lift ratio";

    /** Check if model depends on attitude's rotation rate or acceleration at a given, fixed date.
     * If false, it essentially means that at most the attitude's rotation is used when computing the acceleration vector.
     * The default implementation returns false as common models for orbital mechanics do not.
     * @return true if force model depends on attitude derivatives
     * @since 12.1
     */
    default boolean dependsOnAttitudeRate() {
        return false;
    }

    /** Get the drivers for supported parameters.
     * @return parameters drivers
     * @since 8.0
     */
    List<ParameterDriver> getDragParametersDrivers();

    /** Compute the acceleration due to drag.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param state current state
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @param parameters values of the force model parameters
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @since 12.0
     */
    Vector3D dragAcceleration(SpacecraftState state,
                              double density, Vector3D relativeVelocity,
                              double[] parameters);

    /** Compute the acceleration due to drag.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param state current state
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @param parameters values of the force model parameters
     * @param <T> instance of a CalculusFieldElement
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @since 12.0
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> dragAcceleration(FieldSpacecraftState<T> state,
                                                                          T density, FieldVector3D<T> relativeVelocity,
                                                                          T[] parameters);
}
