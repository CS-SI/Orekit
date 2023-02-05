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
package org.orekit.forces.radiation;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Interface for spacecraft that are sensitive to radiation pressure forces.
 *
 * @see SolarRadiationPressure
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface RadiationSensitive {

    /** Parameter name for global multiplicative factor.
     * @since 12.0
     */
    String GLOBAL_RADIATION_FACTOR = "global radiation factor";

    /** Parameter name for absorption coefficient. */
    String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    String REFLECTION_COEFFICIENT = "reflection coefficient";

    /** Get the drivers for supported parameters.
     * @return parameters drivers
     * @since 8.0
     */
    List<ParameterDriver> getRadiationParametersDrivers();

    /** Compute the acceleration due to radiation pressure.
     * @param state current state
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @param parameters values of the force model parameters
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @since 12.0
     */
    Vector3D radiationPressureAcceleration(SpacecraftState state, Vector3D flux, double[] parameters);

    /** Compute the acceleration due to radiation pressure.
     * @param state current state
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @param parameters values of the force model parameters
     * @param <T> extends CalculusFieldElement
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @since 12.0
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> radiationPressureAcceleration(FieldSpacecraftState<T> state,
                                                                                       FieldVector3D<T> flux,
                                                                                       T[] parameters);
}
