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

import java.util.Collections;
import java.util.List;

import org.orekit.files.general.EphemerisFile;
import org.orekit.time.AbsoluteDate;

/** OEM ephemeris blocks for a single satellite.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OEMSatelliteEphemeris implements EphemerisFile.SatelliteEphemeris {

    /** ID of the satellite. */
    private final String id;

    /** The ephemeris data for the satellite. */
    private final List<OEMSegment> blocks;

    /**
     * Create a container for the set of ephemeris blocks in the file that pertain to
     * a single satellite.
     *
     * @param id     of the satellite.
     * @param blocks containing ephemeris data for the satellite.
     */
    public OEMSatelliteEphemeris(final String id, final List<OEMSegment> blocks) {
        this.id     = id;
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
        return blocks.get(0).getMu();
    }

    /** {@inheritDoc} */
    @Override
    public List<OEMSegment> getSegments() {
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
