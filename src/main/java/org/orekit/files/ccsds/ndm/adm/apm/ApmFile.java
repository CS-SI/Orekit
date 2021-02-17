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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.List;

import org.orekit.attitudes.Attitude;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmFile;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.utils.IERSConventions;

/**
 * This class stores all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmFile extends NdmFile<Header, Segment<AdmMetadata, ApmData>> {

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_APM_VERS";

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used for creating frames, time scales, etc.
     */
    public ApmFile(final Header header, final List<Segment<AdmMetadata, ApmData>> segments,
                   final IERSConventions conventions, final boolean simpleEOP,
                   final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
        this.simpleEOP = simpleEOP;
    }

    /** Get the attitude.
     * <p>
     * The attitude is extracted from the file mandatory
     * {@link ApmQuaternion quaternion logical block}.
     * </p>
     * @return attitude
     */
    public Attitude getAttitude() {
        return getSegments().
               get(0).
               getData().
               getQuaternionBlock().
               getAttitude(getConventions(), simpleEOP, getDataContext());
    }

}
