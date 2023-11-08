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
package org.orekit.estimation.measurements.gnss;

import org.orekit.files.rinex.observation.ObservationDataSet;

/**
 * Interface for combination of measurements.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public interface MeasurementCombination {

    /**
     * Combines observation data using a combination of measurements.
     * @param observations observation data set
     * @return a combined observation data set
     */
    CombinedObservationDataSet combine(ObservationDataSet observations);

    /**
     * Get the name of the combination of measurements.
     * @return name of the combination of measurements
     */
    String getName();

}
