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

package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.EphemerisFile;
import org.orekit.utils.IERSConventions;

/** This class stores all the information of the OEM File parsed by OEMParser.
 * <p>
 * It contains the header and a list of Ephemerides Blocks each containing
 * metadata, a list of ephemerides data lines and optional covariance matrices
 * (and their metadata).
 * </p>
 * @author sports
 * @author Evan Ward
 * @since 6.1
 */
public class OEMFile extends NDMFile<ODMHeader, OEMSegment> implements EphemerisFile {

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OEM_VERS";

    /** Key for covariance section in KVN files. */
    public static final String COVARIANCE_KVN = "COVARIANCE";

    /** Key for covariance section in XML files. */
    public static final String COVARIANCE_XML = "covarianceMatrix";

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     * @param mu gravitational coefficient
     */
    public OEMFile(final ODMHeader header, final List<OEMSegment> segments,
                   final IERSConventions conventions, final DataContext dataContext,
                   final double mu) {
        super(header, segments, conventions, dataContext);
        this.mu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, OEMSatelliteEphemeris> getSatellites() {
        final Map<String, List<OEMSegment>> byId = new HashMap<>();
        for (final OEMSegment segment : getSegments()) {
            final String id = segment.getMetadata().getObjectID();
            byId.putIfAbsent(id, new ArrayList<>());
            byId.get(id).add(segment);
        }
        final Map<String, OEMSatelliteEphemeris> ret = new HashMap<>();
        for (final Map.Entry<String, List<OEMSegment>> entry : byId.entrySet()) {
            ret.put(entry.getKey(), new OEMSatelliteEphemeris(entry.getKey(), mu, entry.getValue()));
        }
        return ret;
    }

    /** Check that, according to the CCSDS standard, every OEMBlock has the same time system.
     */
    public void checkTimeSystems() {
        CcsdsTimeScale referenceTimeSystem = null;
        for (final OEMSegment segment : getSegments()) {
            final CcsdsTimeScale timeSystem = segment.getMetadata().getTimeSystem();
            if (referenceTimeSystem == null) {
                referenceTimeSystem = timeSystem;
            } else if (!referenceTimeSystem.equals(timeSystem)) {
                throw new OrekitException(OrekitMessages.CCSDS_INCONSISTENT_TIME_SYSTEMS,
                                          referenceTimeSystem, timeSystem);
            }
        }
    }

}
