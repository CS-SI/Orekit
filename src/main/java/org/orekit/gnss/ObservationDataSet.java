/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.gnss;

import java.util.Collections;
import java.util.List;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;


/** Observation Data set.
 * @since 9.2
 */
public class ObservationDataSet implements TimeStamped {

    /** Rinex header associated with this data set. */
    private final RinexHeader header;

    /** Satellite System. */
    private final SatelliteSystem satelliteSystem;

    /** PRN Number of the satellite observed. */
    private final int prnNumber;

    /** Date of the observation. */
    private final AbsoluteDate tObs;

    /** List of Observation data. */
    private final List<ObservationData> observationData;

    /** Receiver clock offset (seconds). */
    private final double rcvrClkOffset;

    /**
     * Simple constructor.
     * @param header Rinex header associated with this data set
     * @param satelliteSystem Satellite system
     * @param prnNumber PRN number
     * @param tObs Observation date
     * @param rcvrClkOffset Receiver clock offset (optional, 0 by default)
     * @param observationData List of observation data
     */
    public ObservationDataSet(final RinexHeader header, final SatelliteSystem satelliteSystem,
                              final int prnNumber, final AbsoluteDate tObs,
                              final double rcvrClkOffset, final List<ObservationData> observationData) {
        this.header          = header;
        this.satelliteSystem = satelliteSystem;
        this.prnNumber       = prnNumber;
        this.tObs            = tObs;
        this.observationData = observationData;
        this.rcvrClkOffset   = rcvrClkOffset;
    }

    /** Get the Rinex header associated with this data set.
     * @return Rinex header associated with this data set
     * @since 9.3
     */
    public RinexHeader getHeader() {
        return header;
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
    public List<ObservationData> getObservationData() {
        return Collections.unmodifiableList(observationData);
    }

    /** Get receiver clock offset.
     * @return receiver clock offset (it is optional, may be 0)
     */
    public double getRcvrClkOffset() {
        return rcvrClkOffset;
    }

}
