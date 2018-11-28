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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;


/** Main generator for {@link ObservedMeasurements observed measurements}.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Generator {

    /** Sequences generators. */
    private final List<Scheduler<?>> schedulers;

    /** Build a generator with no sequences generator.
     */
    public Generator() {
        this.schedulers = new ArrayList<>();
    }

    /** Add a sequences generator for a specific measurement type.
     * @param scheduler sequences generator to add
     * @param <T> the type of the measurement
     */
    public <T extends ObservedMeasurement<T>> void addScheduler(final Scheduler<T> scheduler) {
        schedulers.add(scheduler);
    }

    /** Generate measurements.
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     * @param propagators propagators to use
     * @return generated measurements
     */
    public SortedSet<ObservedMeasurement<?>> generate(final AbsoluteDate start, final AbsoluteDate end,
                                                      final Propagator... propagators) {
        // TODO
        return null;
    }

}
