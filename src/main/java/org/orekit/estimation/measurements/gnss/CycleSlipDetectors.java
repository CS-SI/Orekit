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

import java.util.List;

import org.orekit.files.rinex.observation.ObservationDataSet;

/**
 * Interface for phase measurement cycle-slip detection.
 * @author David Soulard
 * @author Bryan Cazabonne
 * @since 10.2
 */
public interface CycleSlipDetectors {

    /**
     * Detects if a cycle-slip occurs for a given list of observation data set.
     * @param observations list of observation data set
     * @return a list of results computed by the cycle-slip detectors
     */
    List<CycleSlipDetectorResults> detect(List<ObservationDataSet> observations);

}
