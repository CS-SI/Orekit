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
package org.orekit.files.rinex.observation;

import java.util.Collections;
import java.util.List;

import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;


/** Observation Data set.
 * @since 9.2
 */
public class ObservationDataSet implements TimeStamped {

    /** Observed satellite. */
    private final SatInSystem satellite;

    /** Date of the observation. */
    private final AbsoluteDate tObs;

    /** Event flag.
     * @since 12.0
     */
    private final int eventFlag;

    /** List of Observation data. */
    private final List<ObservationData> observationData;

    /** Receiver clock offset (seconds). */
    private final double rcvrClkOffset;

    /**
     * Simple constructor.
     * @param satellite observed satellite
     * @param tObs Observation date
     * @param eventFlag event flag
     * @param rcvrClkOffset Receiver clock offset (optional, 0 by default)
     * @param observationData List of observation data
     * @since 12.0
     */
    public ObservationDataSet(final SatInSystem satellite, final AbsoluteDate tObs, final int eventFlag,
                              final double rcvrClkOffset, final List<ObservationData> observationData) {
        this.satellite       = satellite;
        this.tObs            = tObs;
        this.eventFlag       = eventFlag;
        this.observationData = observationData;
        this.rcvrClkOffset   = rcvrClkOffset;
    }

    /** Get observed satellite.
     * @return observed satellite
     * @since 12.0
     */
    public SatInSystem getSatellite() {
        return satellite;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return tObs;
    }

    /** Get the event flag.
     * @return event flag
     * @since 12.0
     */
    public int getEventFlag() {
        return eventFlag;
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
