/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.SortedSet;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.sampling.OrekitStepInterpolator;


/** Interface for generating {@link ObservedMeasurements measurements} sequences.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public interface Scheduler<T extends ObservedMeasurement<T>> {

    /** Generate a sequence of measurements.
     * @param interpolators interpolators for spacecraft states
     * @return generated measurements
     */
    SortedSet<T> generate(OrekitStepInterpolator... interpolators);

}
