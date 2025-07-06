/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.section;

import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Base container for both Rinex clock and observations headers.
 * @since 14.0
 */
public abstract class RinexClockObsBaseHeader extends RinexBaseHeader {

    /** Observation types for each satellite systems. */
    private final Map<SatelliteSystem, List<ObservationType>> mapTypeObs;

    /** List of applied differential code bias corrections. */
    private final List<AppliedDCBS> listAppliedDCBS;

    /** List of antenna center variation corrections. */
    private final List<AppliedPCVS> listAppliedPCVS;

    /** Number of leap seconds since 6-Jan-1980. */
    private int leapSeconds;

    /** Simple constructor.
     * @param fileType file type
     */
    protected RinexClockObsBaseHeader(final RinexFileType fileType) {
        super(fileType);
        mapTypeObs      = new HashMap<>();
        listAppliedDCBS = new ArrayList<>();
        listAppliedPCVS = new ArrayList<>();
        leapSeconds     = 0;
    }

    /** Set the number of observations for a satellite.
     * @param system satellite system
     * @param types observation types
     */
    public void setTypeObs(final SatelliteSystem system, final List<ObservationType> types) {
        mapTypeObs.put(system, new ArrayList<>(types));
    }

    /** Get an unmodifiable view of the map of observation types.
     * @return unmodifiable view of the map of observation types
     */
    public Map<SatelliteSystem, List<ObservationType>> getTypeObs() {
        return Collections.unmodifiableMap(mapTypeObs);
    }

    /** Add applied differential code bias corrections.
     * @param appliedDCBS applied differential code bias corrections to add
     */
    public void addAppliedDCBS(final AppliedDCBS appliedDCBS) {
        listAppliedDCBS.add(appliedDCBS);
    }

    /** Get the list of applied differential code bias corrections.
     * @return list of applied differential code bias corrections
     */
    public List<AppliedDCBS> getListAppliedDCBS() {
        return Collections.unmodifiableList(listAppliedDCBS);
    }

    /** Add antenna center variation corrections.
     * @param appliedPCVS antenna center variation corrections
     */
    public void addAppliedPCVS(final AppliedPCVS appliedPCVS) {
        listAppliedPCVS.add(appliedPCVS);
    }

    /** Get the list of antenna center variation corrections.
     * @return List of antenna center variation corrections
     */
    public List<AppliedPCVS> getListAppliedPCVS() {
        return Collections.unmodifiableList(listAppliedPCVS);
    }

    /** Set the Number of leap seconds since 6-Jan-1980.
     * @param leapSeconds Number of leap seconds since 6-Jan-1980
     */
    public void setLeapSeconds(final int leapSeconds) {
        this.leapSeconds = leapSeconds;
    }

    /** Get the Number of leap seconds since 6-Jan-1980.
     * @return Number of leap seconds since 6-Jan-1980
     */
    public int getLeapSeconds() {
        return leapSeconds;
    }

}
