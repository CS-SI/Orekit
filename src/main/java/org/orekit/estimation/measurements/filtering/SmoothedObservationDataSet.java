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

import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;

/**
 * Container used to store smoothed observation data along with the original data set it originates from.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class SmoothedObservationDataSet {

    /** Smoothed observation data. */
    private ObservationData smoothedObsData;

    /** Original observation data set used to compute the smoothed observation data. */
    private ObservationDataSet obsDataSet;

    /**
     * Simple constructor.
     * @param smoothedObsData smoothed observation data
     * @param obsDataSet original observation data set used to compute the smoothed observation data
     */
    public SmoothedObservationDataSet(final ObservationData smoothedObsData, final ObservationDataSet obsDataSet) {
        this.smoothedObsData = smoothedObsData;
        this.obsDataSet = obsDataSet;
    }

    /**
     * Get the smoothed observation data.
     * @return the smoothed observation data
     */
    public ObservationData getSmoothedData() {
        return smoothedObsData;
    }

    /**
     * Get the original observation data set used to compute the smoothed observation data.
     * @return the original observation data set used to compute the smoothed observation data
     */
    public ObservationDataSet getDataSet() {
        return obsDataSet;
    }

}
