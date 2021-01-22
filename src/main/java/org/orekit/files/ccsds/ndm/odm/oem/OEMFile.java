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
import org.orekit.files.ccsds.ndm.odm.ODMFile;
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
public class OEMFile extends ODMFile<OEMSegment> implements EphemerisFile {

    /** Simple constructor.
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public OEMFile(final IERSConventions conventions, final DataContext dataContext) {
        super(conventions, dataContext);
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
            ret.put(entry.getKey(), new OEMSatelliteEphemeris(entry.getKey(), entry.getValue()));
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
                throw new OrekitException(OrekitMessages.CCSDS_OEM_INCONSISTENT_TIME_SYSTEMS,
                                          referenceTimeSystem, timeSystem);
            }
        }
    }

}
