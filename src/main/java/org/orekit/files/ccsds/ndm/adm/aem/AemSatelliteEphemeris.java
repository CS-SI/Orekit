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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.Collections;
import java.util.List;

import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** AEM ephemeris blocks for a single satellite.
 * @author Bryan Cazabonne
 */
public class AemSatelliteEphemeris
    implements AttitudeEphemerisFile.SatelliteAttitudeEphemeris<TimeStampedAngularCoordinates, AemSegment> {

    /** ID of the satellite. */
    private final String id;

    /** The attitude ephemeris data for the satellite. */
    private final List<AemSegment> blocks;

    /**
     * Create a container for the set of ephemeris blocks in the file that pertain to
     * a single satellite.
     * @param id     of the satellite.
     * @param blocks containing ephemeris data for the satellite.
     * @since 10.3
     */
    public AemSatelliteEphemeris(final String id, final List<AemSegment> blocks) {
        this.id = id;
        this.blocks = blocks;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return this.id;
    }

    /** {@inheritDoc} */
    @Override
    public List<AemSegment> getSegments() {
        return Collections.unmodifiableList(blocks);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return blocks.get(0).getStart();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return blocks.get(blocks.size() - 1).getStop();
    }

}
