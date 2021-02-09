/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.general.EphemerisFile;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class gathers the informations present in the Orbit Comprehensive Message (OCM).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OCMFile extends NDMFile<ODMHeader, Segment<OCMMetadata, OCMData>>
    implements EphemerisFile<TimeStampedPVCoordinates, OrbitStateHistory> {

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OCM_VERS";

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param header file header
     * @param segments ile segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     * @param mu Gravitational coefficient to use for building Cartesian/Keplerian orbits.
     */
    public OCMFile(final ODMHeader header, final List<Segment<OCMMetadata, OCMData>> segments,
                   final IERSConventions conventions, final DataContext dataContext,
                   final double mu) {
        super(header, segments, conventions, dataContext);
        this.mu = mu;
    }

    /** Get the metadata from the single {@link #getSegments() segment}.
     * @return metadata from the single {@link #getSegments() segment}
     */
    public OCMMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the data from the single {@link #getSegments() segment}.
     * @return data from the single {@link #getSegments() segment}
     */
    public OCMData getData() {
        return getSegments().get(0).getData();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, OCMSatelliteEphemeris> getSatellites() {
        // the OCM file has only one segment and a deep structure
        // the real ephemeris is buried within the orbit histories logical block
        final String                  id        = getMetadata().getObjectDesignator();
        final List<OrbitStateHistory> histories = getSegments().get(0).getData().getOrbitBlocks();
        final OCMSatelliteEphemeris   ose       = new OCMSatelliteEphemeris(id, mu, histories);
        return Collections.singletonMap(getMetadata().getObjectDesignator(), ose);
    }

}
