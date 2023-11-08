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
package org.orekit.estimation.measurements;

import org.orekit.utils.ParameterDriversProvider;


/** Interface for estimated measurements modifiers used for orbit determination.
 * <p>
 * Modifiers are used to take some physical corrections into account in
 * the theoretical {@link EstimatedMeasurement measurement} model. They can be
 * used to model for example:
 * <ul>
 *   <li>on board delays</li>
 *   <li>ground delays</li>
 *   <li>antennas mount and center of phase offsets</li>
 *   <li>tropospheric effects</li>
 *   <li>clock drifts</li>
 *   <li>ground station displacements due to tidal effects</li>
 *   <li>...</li>
 * </ul>
 *
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface EstimationModifier<T extends ObservedMeasurement<T>> extends ParameterDriversProvider {

    /** Apply a modifier to an estimated measurement without derivatives.
     * @param estimated estimated measurement to modify
     * @since 12.0
     */
    void modifyWithoutDerivatives(EstimatedMeasurementBase<T> estimated);

    /** Apply a modifier to an estimated measurement.
     * @param estimated estimated measurement to modify
     */
    default void modify(EstimatedMeasurement<T> estimated) {
        modifyWithoutDerivatives(estimated);
    }

}
