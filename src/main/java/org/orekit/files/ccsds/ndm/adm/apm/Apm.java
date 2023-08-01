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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.List;

import org.orekit.attitudes.Attitude;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.frames.Frame;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * This class stores all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Apm extends NdmConstituent<AdmHeader, Segment<AdmMetadata, ApmData>> {

    /** Root element for XML files. */
    public static final String ROOT = "apm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_APM_VERS";

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public Apm(final AdmHeader header, final List<Segment<AdmMetadata, ApmData>> segments,
               final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public AdmMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the file data.
     * @return file data
     */
    public ApmData getData() {
        return getSegments().get(0).getData();
    }

    /** Get the attitude.
     * @param frame reference frame with respect to which attitude must be defined,
     * (may be null if attitude is <em>not</em> orbit-relative and one wants
     * attitude in the same frame as used in the attitude message)
     * @param pvProvider provider for spacecraft position and velocity
     * (may be null if attitude is <em>not</em> orbit-relative)
     * @return attitude
     */
    public Attitude getAttitude(final Frame frame, final PVCoordinatesProvider pvProvider) {
        return getData().getAttitude(frame, pvProvider);
    }

}
