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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.general.EphemerisFile;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class gathers the informations present in the Orbit Comprehensive Message (OCM).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Ocm extends NdmConstituent<OdmHeader, Segment<OcmMetadata, OcmData>>
    implements EphemerisFile<TimeStampedPVCoordinates, TrajectoryStateHistory> {

    /** Root element for XML messages. */
    public static final String ROOT = "ocm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OCM_VERS";

    /** Trajectory line element for XML messages. */
    public static final String TRAJ_LINE = "trajLine";

    /** Covariance line element for XML messages. */
    public static final String COV_LINE = "covLine";

    /** Maneuver line element for XML messages. */
    public static final String MAN_LINE = "manLine";

    /** Default name for unknown object. */
    public static final String UNKNOWN_OBJECT = "UNKNOWN";

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param header file header
     * @param segments ile segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     * @param mu Gravitational coefficient to use for building Cartesian/Keplerian orbits.
     */
    public Ocm(final OdmHeader header, final List<Segment<OcmMetadata, OcmData>> segments,
               final IERSConventions conventions, final DataContext dataContext,
               final double mu) {
        super(header, segments, conventions, dataContext);
        this.mu = mu;
    }

    /** Get the metadata from the single {@link #getSegments() segment}.
     * @return metadata from the single {@link #getSegments() segment}
     */
    public OcmMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the data from the single {@link #getSegments() segment}.
     * @return data from the single {@link #getSegments() segment}
     */
    public OcmData getData() {
        return getSegments().get(0).getData();
    }

    /** {@inheritDoc}
     * <p>
     * The metadata entries checked for use as the key are the following ones,
     * the first non-null being used. The map from OCM files always contains only
     * one object.
     * <ul>
     *   <li>{@link org.orekit.files.ccsds.ndm.odm.OdmMetadata#getObjectName() OBJECT_NAME}</li>
     *   <li>{@link OcmMetadata#getInternationalDesignator() INTERNATIONAL_DESIGNATOR}</li>
     *   <li>{@link OcmMetadata#getObjectDesignator() OBJECT_DESIGNATOR}</li>
     *   <li>the default name {@link #UNKNOWN_OBJECT} for unknown objects</li>
     * </ul>
     */
    @Override
    public Map<String, OcmSatelliteEphemeris> getSatellites() {
        // the OCM file has only one segment and a deep structure
        // the real ephemeris is buried within the orbit histories logical block
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
        final List<TrajectoryStateHistory> histories = getSegments().get(0).getData().getTrajectoryBlocks();
        final OcmSatelliteEphemeris        ose       = new OcmSatelliteEphemeris(name, mu, histories);
        return Collections.singletonMap(name, ose);
    }

}
