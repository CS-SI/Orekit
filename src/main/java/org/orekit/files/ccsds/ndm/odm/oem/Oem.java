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

package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.general.EphemerisFile;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

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
public class Oem extends NdmConstituent<OdmHeader, OemSegment>
    implements EphemerisFile<TimeStampedPVCoordinates, OemSegment> {

    /** Root element for XML files. */
    public static final String ROOT = "oem";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OEM_VERS";

    /** Default name for unknown object. */
    public static final String UNKNOWN_OBJECT = "UNKNOWN";

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     * @param mu gravitational coefficient
     */
    public Oem(final OdmHeader header, final List<OemSegment> segments,
               final IERSConventions conventions, final DataContext dataContext,
               final double mu) {
        super(header, segments, conventions, dataContext);
        this.mu = mu;
    }

    /** {@inheritDoc}
     * <p>
     * The metadata entries checked for use as the key are the following ones,
     * the first non-null being used. The map from OEM files always contains only
     * one object.
     * <ul>
     *   <li>{@link org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata#getObjectID()}  OBJECT_ID}
     *   if non-null and not equal to {@link #UNKNOWN_OBJECT}</li>
     *   <li>{@link org.orekit.files.ccsds.ndm.odm.OdmMetadata#getObjectName() OBJECT_NAME}</li>
     *   <li>the default name {@link #UNKNOWN_OBJECT} for unknown objects</li>
     * </ul>
     */
    @Override
    public Map<String, OemSatelliteEphemeris> getSatellites() {
        final Map<String, List<OemSegment>> byName = new HashMap<>();
        for (final OemSegment segment : getSegments()) {
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
        final Map<String, OemSatelliteEphemeris> ret = new HashMap<>();
        for (final Map.Entry<String, List<OemSegment>> entry : byName.entrySet()) {
            ret.put(entry.getKey(), new OemSatelliteEphemeris(entry.getKey(), mu, entry.getValue()));
        }
        return ret;
    }

    /** Check that, according to the CCSDS standard, every OEMBlock has the same time system.
     */
    public void checkTimeSystems() {
        TimeSystem referenceTimeSystem = null;
        for (final OemSegment segment : getSegments()) {
            final TimeSystem timeSystem = segment.getMetadata().getTimeSystem();
            if (referenceTimeSystem == null) {
                referenceTimeSystem = timeSystem;
            } else if (!referenceTimeSystem.equals(timeSystem)) {
                throw new OrekitException(OrekitMessages.CCSDS_INCONSISTENT_TIME_SYSTEMS,
                                          referenceTimeSystem.name(), timeSystem.name());
            }
        }
    }

}
