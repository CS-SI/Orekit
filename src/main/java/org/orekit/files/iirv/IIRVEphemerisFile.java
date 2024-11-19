/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.iirv;

import org.orekit.errors.OrekitInternalError;
import org.orekit.files.general.EphemerisFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for associating a the {@link IIRVEphemeris} ephemeris state data (obtained from an {@link IIRVMessage})
 * to a single satellite, identified by its IIRV {@link org.orekit.files.iirv.terms.VehicleIdCodeTerm}.
 */
public class IIRVEphemerisFile implements EphemerisFile<TimeStampedPVCoordinates, IIRVSegment> {

    /** Unmodifiable mapping with a single key-value pair from satellite id to ephemeris. */
    private final Map<String, IIRVEphemerisFile.IIRVEphemeris> satellites;

    /**
     * Constructs a {@link IIRVEphemerisFile} instance.
     *
     * @param ephemeris IIRV ephemeris data.
     */
    public IIRVEphemerisFile(final IIRVEphemerisFile.IIRVEphemeris ephemeris) {
        final Map<String, IIRVEphemerisFile.IIRVEphemeris> tempMap = new HashMap<>();
        tempMap.put(ephemeris.getId(), ephemeris);
        this.satellites = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Constructs a {@link IIRVEphemerisFile} instance from a {@link IIRVMessage}.
     *
     * @param mu                   gravitational parameter (m^3/s^2)
     * @param interpolationSamples number of samples to use in interpolation
     * @param startYear            Year associated with the beginning of the IIRV message
     * @param iirvMessage          IIRV message
     */
    public IIRVEphemerisFile(final double mu,
                             final int interpolationSamples,
                             final int startYear,
                             final IIRVMessage iirvMessage) {
        this(new IIRVEphemerisFile.IIRVEphemeris(new IIRVSegment(mu, interpolationSamples, startYear, iirvMessage)));
    }

    /**
     * Constructs a {@link IIRVEphemerisFile} instance from a {@link IIRVMessage} with default values.
     * <p>
     * See {@link IIRVSegment#IIRVSegment(int, IIRVMessage)} for default value information.
     *
     * @param startYear   Year associated with the beginning of the IIRV message
     * @param iirvMessage IIRV message
     */
    public IIRVEphemerisFile(final int startYear, final IIRVMessage iirvMessage) {
        this(new IIRVEphemerisFile.IIRVEphemeris(new IIRVSegment(startYear, iirvMessage)));
    }


    /**
     * {@inheritDoc}
     * <p>
     * STK ephemeris files define ephemeris for a single satellite, so the returned
     * map will have a single entry.
     * </p>
     */
    @Override
    public Map<String, IIRVEphemerisFile.IIRVEphemeris> getSatellites() {
        return satellites;
    }

    /**
     * Gets the {@link IIRVEphemeris} associated with this file.
     *
     * @return {@link IIRVEphemeris} associated with this file.
     */
    public IIRVEphemeris getIIRVEphemeris() {
        if (satellites.size() != 1) {
            // This should never happen
            throw new OrekitInternalError(null);
        }
        return satellites.values().iterator().next();
    }

    /**
     * Gets the IIRV message containing the ephemeris data.
     *
     * @return IIRVMessage containing the ephemeris data.
     */
    public IIRVMessage getIIRV() {
        return getIIRVEphemeris().getIIRV();
    }

    /**
     * Gets the start year for this file.
     *
     * @return start year for this file.
     */
    public int getStartYear() {
        return getIIRVEphemeris().getStartYear();
    }

    /**
     * Ephemeris from an IIRV file.
     */
    public static class IIRVEphemeris implements SatelliteEphemeris<TimeStampedPVCoordinates, IIRVSegment> {

        /** Ephemeris segment. */
        private final IIRVSegment segment;

        /**
         * Constructs a {@link IIRVSegment} instance.
         * <p>
         * An IIRV file contains ephemeris data for a single satellite; thus each {@link IIRVEphemeris} instance
         * contains only a single {@link IIRVSegment}.
         *
         * @param segment ephemeris segments
         */
        public IIRVEphemeris(final IIRVSegment segment) {
            this.segment = segment;
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return segment.getIIRVMessage().getSatelliteID();
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            return segment.getMu();
        }

        /**
         * Get the {@link IIRVSegment} for this satellite.
         *
         * @return {@link IIRVSegment} for this satellite.
         * @see #getSegment()
         */
        public IIRVSegment getSegment() {
            return segment;
        }

        /** {@inheritDoc} */
        @Override
        public List<IIRVSegment> getSegments() {
            return Collections.singletonList(segment);
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            return segment.getStart();
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            return segment.getStop();
        }

        /**
         * Gets the {@link IIRVMessage} containing the ephemeris data for this object's {@link IIRVSegment}.
         *
         * @return {@link IIRVMessage} containing the ephemeris data for this object's {@link IIRVSegment}.
         */
        public IIRVMessage getIIRV() {
            return segment.getIIRVMessage();
        }

        /**
         * Gets the start year of the ephemeris data.
         *
         * @return start year of the ephemeris data
         */
        public int getStartYear() {
            return segment.getStartYear();
        }

    }

}
