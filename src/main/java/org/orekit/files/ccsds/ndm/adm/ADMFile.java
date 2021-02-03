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

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * This class stores all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @param <S> type of the segment
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMFile<S extends Segment<?, ?>> extends NDMFile<Header, S> {

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Initial Date for MET or MRT time systems. */
    private AbsoluteDate missionReferenceDate;

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used for creating frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time and Mission Relative Time time systems.
     */
    public ADMFile(final Header header, final List<S> segments,
                   final IERSConventions conventions, final boolean simpleEOP,
                   final DataContext dataContext, final AbsoluteDate missionReferenceDate) {
        super(header, segments, conventions, dataContext);
        this.simpleEOP            = simpleEOP;
        this.missionReferenceDate = missionReferenceDate;
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

}
