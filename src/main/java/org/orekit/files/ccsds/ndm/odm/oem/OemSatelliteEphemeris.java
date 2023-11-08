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

package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.Collections;
import java.util.List;

import org.orekit.files.general.EphemerisFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** OEM ephemeris blocks for a single satellite.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OemSatelliteEphemeris
    implements EphemerisFile.SatelliteEphemeris<TimeStampedPVCoordinates, OemSegment> {

    /** ID of the satellite. */
    private final String id;

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** The ephemeris data for the satellite. */
    private final List<OemSegment> blocks;

    /**
     * Create a container for the set of ephemeris blocks in the file that pertain to
     * a single satellite.
     *
     * @param id id of the satellite.
     * @param mu gravitational coefficient to use for building Cartesian/Keplerian orbits
     * @param blocks containing ephemeris data for the satellite.
     */
    public OemSatelliteEphemeris(final String id, final double mu, final List<OemSegment> blocks) {
        this.id     = id;
        this.mu     = mu;
        this.blocks = blocks;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public List<OemSegment> getSegments() {
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
