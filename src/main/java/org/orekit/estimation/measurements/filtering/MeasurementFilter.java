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
package org.orekit.estimation.measurements.filtering;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

/** Interface for measurement pre-processing filter.
 * <p>
 * Pre-processing filters are used to disabled measurements
 * before they are used during an orbit determination process.
 * Example of pre-processing filters are:</p>
 * <ul>
 *     <li>Minimum satellite elevation</li>
 *     <li>Minimum value of the signal-to-noise ratio</li>
 *     <li>Measurement residual</li>
 * </ul>
 * @param <T> the type of the measurement
 * @author Bryan Cazabonne
 * @author David Soulard
 * @since 10.2
 */
public interface MeasurementFilter<T extends ObservedMeasurement<T>> {

    /** Apply a filter to an observed measurement.
     * <p>
     * If the observed measurement is rejected by the filter,
     * the method {@link ObservedMeasurement#isEnabled()} will
     * return <code>false</code>.
     * </p>
     * @param measurement observed measurement
     * @param state current spacecraft state.
     * */
    void filter(ObservedMeasurement<T> measurement, SpacecraftState state);

}
