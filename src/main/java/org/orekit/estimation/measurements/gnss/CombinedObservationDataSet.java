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

import java.util.Collections;
import java.util.List;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * Combined observation data set.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class CombinedObservationDataSet implements TimeStamped {

    /** Satellite System. */
    private final SatelliteSystem satelliteSystem;

    /** PRN Number of the satellite observed. */
    private final int prnNumber;

    /** Date of the observation. */
    private final AbsoluteDate tObs;

    /** List of Observation data. */
    private final List<CombinedObservationData> observationData;

    /** Receiver clock offset (seconds). */
    private final double rcvrClkOffset;

    /**
     * Simple constructor.
     * @param satelliteSystem Satellite system
     * @param prnNumber PRN number
     * @param tObs Observation date
     * @param rcvrClkOffset Receiver clock offset (optional, 0 by default)
     * @param observationData List of combined observation data
     */
    public CombinedObservationDataSet(final SatelliteSystem satelliteSystem,
                                      final int prnNumber, final AbsoluteDate tObs,
                                      final double rcvrClkOffset, final List<CombinedObservationData> observationData) {
        this.satelliteSystem = satelliteSystem;
        this.prnNumber       = prnNumber;
        this.tObs            = tObs;
        this.observationData = observationData;
        this.rcvrClkOffset   = rcvrClkOffset;
    }

    /** Get Satellite System.
     * @return satellite system of observed satellite
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Get PRN number.
     * @return PRN number of the observed satellite
     */
    public int getPrnNumber() {
        return prnNumber;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return tObs;
    }

    /** Get list of observation data.
     * @return unmodifiable view of of observation data for the observed satellite
     */
    public List<CombinedObservationData> getObservationData() {
        return Collections.unmodifiableList(observationData);
    }

    /** Get receiver clock offset.
     * @return receiver clock offset (it is optional, may be 0)
     */
    public double getRcvrClkOffset() {
        return rcvrClkOffset;
    }

}
