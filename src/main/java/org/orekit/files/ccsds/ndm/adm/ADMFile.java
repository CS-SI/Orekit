/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.ndm.NDMHeader;
import org.orekit.time.AbsoluteDate;

/**
 * This class stores all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @param <S> type of the segment
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMFile<S extends ADMSegment<?, ?>> extends NDMFile<NDMHeader, S> {

    /** Initial Date for MET or MRT time systems. */
    private AbsoluteDate missionReferenceDate;

    /** Simple constructor. */
    public ADMFile() {
        super(new NDMHeader());
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /**
     * Set reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @param missionReferenceDate reference date for Mission Elapsed Time and Mission Relative Time time systems.
     */
    public void setMissionReferenceDate(final AbsoluteDate missionReferenceDate) {
        this.missionReferenceDate = missionReferenceDate;
    }

}
