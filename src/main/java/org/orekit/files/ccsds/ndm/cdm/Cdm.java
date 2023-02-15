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
package org.orekit.files.ccsds.ndm.cdm;

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.utils.IERSConventions;

/**
 * This class stores all the information of the Conjunction Data Message (CDM) File parsed
 * by CdmParser. It contains the header and a list of segments each
 * containing metadata and a list of data lines.
 * @author Melina Vanel
 * @since 11.2
 */
public class Cdm extends NdmConstituent<CdmHeader, CdmSegment> {

    /** Root element for XML files. */
    public static final String ROOT = "cdm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_CDM_VERS";

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public Cdm(final CdmHeader header, final List<CdmSegment> segments,
                   final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public CdmRelativeMetadata getRelativeMetadata() {
        return getSegments().get(0).getMetadata().getRelativeMetadata();
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public CdmMetadata getMetadataObject1() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public CdmMetadata getMetadataObject2() {
        return getSegments().get(1).getMetadata();
    }

    /** Get the file data.
     * @return file data
     */
    public CdmData getDataObject1() {
        return getSegments().get(0).getData();
    }

    /** Get the file data.
     * @return file data
     */
    public CdmData getDataObject2() {
        return getSegments().get(1).getData();
    }

    /** Get user defined parameters.
     * <p> This method will return null if the user defined block is not present in the CDM</p>
     * @return file data
     */
    public UserDefined getUserDefinedParameters() {
        return getSegments().get(0).getData().getUserDefinedBlock();
    }

}
