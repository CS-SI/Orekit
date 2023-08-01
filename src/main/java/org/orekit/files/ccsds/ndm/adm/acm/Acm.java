/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** This class gathers the informations present in the Attitude Comprehensive Message (ACM).
 * @author Luc Maisonobe
 * @since 12.0
 */
public class Acm extends NdmConstituent<AdmHeader, Segment<AcmMetadata, AcmData>>
    implements AttitudeEphemerisFile<TimeStampedAngularCoordinates, AttitudeStateHistory> {

    /** Root element for XML messages. */
    public static final String ROOT = "acm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_ACM_VERS";

    /** Attitude line element for XML messages. */
    public static final String ATT_LINE = "attLine";

    /** Covariance line element for XML messages. */
    public static final String COV_LINE = "covLine";

    /** Default name for unknown object. */
    public static final String UNKNOWN_OBJECT = "UNKNOWN";

    /** Simple constructor.
     * @param header file header
     * @param segments ile segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public Acm(final AdmHeader header, final List<Segment<AcmMetadata, AcmData>> segments,
               final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** Get the metadata from the single {@link #getSegments() segment}.
     * @return metadata from the single {@link #getSegments() segment}
     */
    public AcmMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the data from the single {@link #getSegments() segment}.
     * @return data from the single {@link #getSegments() segment}
     */
    public AcmData getData() {
        return getSegments().get(0).getData();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, AcmSatelliteEphemeris> getSatellites() {
        // the ACM file has only one segment and a deep structure
        // the real ephemeris is buried within the attitude state time history logical block
        final String name;
        if (getMetadata().getObjectName() != null) {
            name = getMetadata().getObjectName();
        } else if (getMetadata().getInternationalDesignator() != null) {
            name = getMetadata().getInternationalDesignator();
        } else if (getMetadata().getObjectDesignator() != null) {
            name = getMetadata().getObjectDesignator();
        } else {
            name = UNKNOWN_OBJECT;
        }
        final List<AttitudeStateHistory> histories = getSegments().get(0).getData().getAttitudeBlocks();
        return (histories == null) ?
               Collections.emptyMap() :
               Collections.singletonMap(name, new AcmSatelliteEphemeris(name, histories));
    }

}
