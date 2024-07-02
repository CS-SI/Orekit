/* Copyright 2002-2024 CS GROUP
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
package org.orekit.files.rinex;

import org.orekit.gnss.SatelliteSystem;

/** Corrections of Differential Code Biases (DCBs) applied.
 * Contains information on the programs used to correct the observations
 * in RINEX or clock files for differential code biases.
 */
public class AppliedDCBS {

    /** Satellite system. */
    private final SatelliteSystem satelliteSystem;

    /** Program name used to apply differential code bias corrections. */
    private final String progDCBS;

    /** Source of corrections (URL). */
    private final String sourceDCBS;

    /** Simple constructor.
     * @param satelliteSystem satellite system
     * @param progDCBS Program name used to apply DCBs
     * @param sourceDCBS Source of corrections (URL)
     */
    public AppliedDCBS(final SatelliteSystem satelliteSystem,
                       final String progDCBS, final String sourceDCBS) {
        this.satelliteSystem = satelliteSystem;
        this.progDCBS        = progDCBS;
        this.sourceDCBS      = sourceDCBS;
    }

    /** Get the satellite system.
     * @return satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Get the program name used to apply DCBs.
     * @return  Program name used to apply DCBs
     */
    public String getProgDCBS() {
        return progDCBS;
    }

    /** Get the source of corrections.
     * @return Source of corrections (URL)
     */
    public String getSourceDCBS() {
        return sourceDCBS;
    }

}
