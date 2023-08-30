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
package org.orekit.files.rinex;

import org.orekit.gnss.SatelliteSystem;

/** Corrections of antenna phase center variations (PCVs) applied.
 * Contains information on the programs used to correct the observations
 * in RINEX or clock files for antenna phase center variations.
 */
public class AppliedPCVS {

    /** Satellite system. */
    private final SatelliteSystem satelliteSystem;

    /** Program name used to antenna center variation corrections. */
    private final String progPCVS;

    /** Source of corrections (URL). */
    private final String sourcePCVS;

    /** Simple constructor.
     * @param satelliteSystem satellite system
     * @param progPCVS Program name used for PCVs
     * @param sourcePCVS Source of corrections (URL)
     */
    public AppliedPCVS(final SatelliteSystem satelliteSystem,
                        final String progPCVS, final String sourcePCVS) {
        this.satelliteSystem = satelliteSystem;
        this.progPCVS        = progPCVS;
        this.sourcePCVS      = sourcePCVS;
    }

    /** Get the satellite system.
     * @return satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Get the program name used to apply PCVs.
     * @return  Program name used to apply PCVs
     */
    public String getProgPCVS() {
        return progPCVS;
    }

    /** Get the source of corrections.
     * @return Source of corrections (URL)
     */
    public String getSourcePCVS() {
        return sourcePCVS;
    }

}
