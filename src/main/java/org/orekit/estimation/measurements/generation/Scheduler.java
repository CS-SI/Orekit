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
package org.orekit.estimation.measurements.generation;

import java.util.Map;
import java.util.SortedSet;

import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** Interface for generating {@link ObservedMeasurement measurements} sequences.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public interface Scheduler<T extends ObservedMeasurement<T>> {

    /** Get the builder associated with this scheduler.
     * @return builder associated with this scheduler
     * @since 12.0
     */
    MeasurementBuilder<T> getBuilder();

    /** Initialize scheduler at the start of a measurements generation.
     * <p>
     * This method is called once at the start of the measurements generation. It
     * may be used by the scheduler to initialize some internal data
     * if needed, typically {@link MeasurementBuilder#init(AbsoluteDate, AbsoluteDate)
     * initializing builders}.
     * </p>
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     */
    void init(AbsoluteDate start, AbsoluteDate end);

    /** Generate a sequence of measurements.
     * @param interpolators interpolators for spacecraft states
     * @return generated measurements
     * @since 12.0
     */
    SortedSet<T> generate(Map<ObservableSatellite, OrekitStepInterpolator> interpolators);

}
