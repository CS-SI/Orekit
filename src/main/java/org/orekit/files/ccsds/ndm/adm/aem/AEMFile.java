/* Copyright 2002-2020 CS GROUP
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
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.adm.ADMFile;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.files.general.OrekitAttitudeEphemerisFile;

/**
 * This class stocks all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of attitude ephemerides data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMFile extends ADMFile<AEMMetadata, AEMData> implements AttitudeEphemerisFile {

    /** {@inheritDoc} */
    @Override
    public Map<String, AEMSatelliteEphemeris> getSatellites() {
        final Map<String, AEMSatelliteEphemeris> ret = new HashMap<>();
        for (final NDMSegment<AEMMetadata, AEMData> segment : getSegments()) {
            final String id = segment.getMetadata().getObjectID();
            if (!ret.containsKey(id)) {
                ret.put(id, new AEMSatelliteEphemeris(id, new ArrayList<>()));
            }
            ret.get(id).getSegments().add(new OrekitAttitudeEphemerisFile.OrekitAttitudeEphemerisSegment(segment.getData().getAngularCoordinates(),
                                                                                                         segment.getMetadata().getCenterName(),
                                                                                                         segment.getMetadata().getRefFrameAString(),
                                                                                                         segment.getMetadata().getRefFrameBString(),
                                                                                                         segment.getMetadata().getAttitudeDirection(),
                                                                                                         segment.getMetadata().getAttitudeType(),
                                                                                                         segment.getMetadata().isFirst(),
                                                                                                         segment.getMetadata().getRotationOrder(),
                                                                                                         segment.getMetadata().getTimeScale(),
                                                                                                         segment.getMetadata().getInterpolationMethod(),
                                                                                                         segment.getMetadata().getInterpolationSamples(),
                                                                                                         segment.getMetadata().getReferenceFrame()));
        }
        return ret;
    }

    /**
     * Check that, according to the CCSDS standard, every AEMBlock has the same time system.
     */
    public void checkTimeSystems() {
        CcsdsTimeScale referenceTimeSystem = null;
        for (final NDMSegment<AEMMetadata, AEMData> segment : getSegments()) {
            final CcsdsTimeScale timeSystem = segment.getMetadata().getTimeSystem();
            if (referenceTimeSystem == null) {
                referenceTimeSystem = timeSystem;
            } else if (!referenceTimeSystem.equals(timeSystem)) {
                throw new OrekitException(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS,
                                          referenceTimeSystem, timeSystem);
            }
        }
    }

}
