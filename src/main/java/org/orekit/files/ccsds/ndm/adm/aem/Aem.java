/* Copyright 2002-2026 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stores all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of attitude ephemerides data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Aem extends NdmConstituent<AdmHeader, AemSegment>
    implements AttitudeEphemerisFile<TimeStampedAngularCoordinates, AemSegment> {

    /** Root element for XML files. */
    public static final String ROOT = "aem";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_AEM_VERS";

    /** Default name for unknown object. */
    public static final String UNKNOWN_OBJECT = "UNKNOWN";

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public Aem(final AdmHeader header, final List<AemSegment> segments,
               final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** {@inheritDoc}
     * <p>
     * The metadata entries checked for use as the key are the following ones,
     * the first non-null being used. The map from AEM files always contains only
     * one object.
     * <ul>
     *   <li>{@link org.orekit.files.ccsds.ndm.adm.AdmMetadata#getObjectID()}  OBJECT_ID}
     *   if non-null and not equal to {@link #UNKNOWN_OBJECT}</li>
     *   <li>{@link org.orekit.files.ccsds.ndm.adm.AdmMetadata#getObjectName() OBJECT_NAME}</li>
     *   <li>the default name {@link #UNKNOWN_OBJECT} for unknown objects</li>
     * </ul>
     */
    @Override
    public Map<String, AemSatelliteEphemeris> getSatellites() {
        final Map<String, List<AemSegment>> byName = new HashMap<>();
        for (final AemSegment segment : getSegments()) {
            final String name;
            if (segment.getMetadata().getObjectID() != null &&
                !UNKNOWN_OBJECT.equals(segment.getMetadata().getObjectID())) {
                name = segment.getMetadata().getObjectID();
            } else if (segment.getMetadata().getObjectName() != null) {
                name = segment.getMetadata().getObjectName();
            } else {
                name = UNKNOWN_OBJECT;
            }
            byName.putIfAbsent(name, new ArrayList<>());
            byName.get(name).add(segment);
        }
        final Map<String, AemSatelliteEphemeris> ret = new HashMap<>();
        for (final Map.Entry<String, List<AemSegment>> entry : byName.entrySet()) {
            ret.put(entry.getKey(), new AemSatelliteEphemeris(entry.getKey(), entry.getValue()));
        }
        return ret;
    }

}
