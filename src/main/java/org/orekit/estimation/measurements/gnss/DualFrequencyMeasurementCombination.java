/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.ObservationData;

/**
 * Interface for dual frequency combination of measurements.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public interface DualFrequencyMeasurementCombination {

    /**
     * Combines observation data using a dual frequency combination of measurements.
     * @param od1 first observation data to combined
     * @param od2 second observation data to combined
     * @return a combined observation data
     */
    CombinedObservationData combine(ObservationData od1, ObservationData od2);

    /**
     * Get the name of the combination of measurements.
     * @return name of the combination of measurements
     */
    String getName();

}
