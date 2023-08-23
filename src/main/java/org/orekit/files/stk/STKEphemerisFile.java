/* Copyright 2002-2023 Andrew Goetz
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
package org.orekit.files.stk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.stk.STKEphemerisFile.STKEphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * STK ephemeris file.
 *
 * @author Andrew Goetz
 * @since 12.0
 */
public class STKEphemerisFile implements EphemerisFile<TimeStampedPVCoordinates, STKEphemerisSegment> {

    /** STK version. */
    private final String stkVersion;

    /** Unmodifiable mapping with a single key-value pair from satellite id to ephemeris. */
    private final Map<String, STKEphemeris> satellites;

    /**
     * Constructs a {@link STKEphemerisFile} instance.
     * @param stkVersion STK version string (example: "stk.v.11.0")
     * @param satelliteId satellite id
     * @param ephemeris ephemeris
     */
    public STKEphemerisFile(final String stkVersion, final String satelliteId, final STKEphemeris ephemeris) {
        this.stkVersion = Objects.requireNonNull(stkVersion);
        final Map<String, STKEphemeris> tempMap = new HashMap<>();
        tempMap.put(satelliteId, ephemeris);
        this.satellites = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Returns the STK version string.
     * @return STK version string
     */
    public String getSTKVersion() {
        return stkVersion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * STK ephemeris files define ephemeris for a single satellite, so the returned
     * map will have a single entry.
     * </p>
     */
    @Override
    public Map<String, STKEphemeris> getSatellites() {
        return satellites;
    }

    /**
     * Ephemeris segment from an STK ephemeris file.
     */
    public static class STKEphemerisSegment implements EphemerisFile.EphemerisSegment<TimeStampedPVCoordinates> {

        /** Gravitational parameter (m^3/s^2). */
        private final double mu;

        /** Reference frame. */
        private final Frame frame;

        /** Number of samples to use in interpolation. */
        private final int interpolationSamples;

        /** Cartesian derivatives filter. */
        private final CartesianDerivativesFilter cartesianDerivativesFilter;

        /** Time-sorted time/position/velocity data. */
        private final List<TimeStampedPVCoordinates> timeStampedPVCoordinates;

        /**
         * Constructs a {@link STKEphemerisSegment} instance.
         * @param mu gravitational parameter (m^3/s^2)
         * @param frame frame
         * @param interpolationSamples number of samples to use in interpolation
         * @param cartesianDerivativesFilter Cartesian derivatives filter
         * @param timeStampedPVCoordinates time-sorted time/position/velocity data
         */
        public STKEphemerisSegment(final double mu, final Frame frame, final int interpolationSamples,
                final CartesianDerivativesFilter cartesianDerivativesFilter,
                final List<TimeStampedPVCoordinates> timeStampedPVCoordinates) {
            this.mu = mu;
            this.frame = Objects.requireNonNull(frame);
            this.interpolationSamples = interpolationSamples;
            this.cartesianDerivativesFilter = Objects.requireNonNull(cartesianDerivativesFilter);
            this.timeStampedPVCoordinates = Collections.unmodifiableList(new ArrayList<>(timeStampedPVCoordinates));
        }

        @Override
        public double getMu() {
            return mu;
        }

        @Override
        public Frame getFrame() {
            return frame;
        }

        @Override
        public int getInterpolationSamples() {
            return interpolationSamples;
        }

        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return cartesianDerivativesFilter;
        }

        @Override
        public List<TimeStampedPVCoordinates> getCoordinates() {
            return timeStampedPVCoordinates;
        }

        @Override
        public AbsoluteDate getStart() {
            return timeStampedPVCoordinates.get(0).getDate();
        }

        @Override
        public AbsoluteDate getStop() {
            return timeStampedPVCoordinates.get(timeStampedPVCoordinates.size() - 1).getDate();
        }

    }

    /**
     * Ephemeris from an STK ephemeris file.
     */
    public static class STKEphemeris implements SatelliteEphemeris<TimeStampedPVCoordinates, STKEphemerisSegment> {

        /** Satellite id.*/
        private final String satelliteId;

        /** Gravitational parameter (m^3/s^2). */
        private final double mu;

        /** Unmodifiable list of ephemeris segments. */
        private final List<STKEphemerisSegment> segments;

        /**
         * Constructs a {@link STKEphemeris} instance. This constructor shallowly copies the list of segments provided.
         * @param satelliteId satellite id
         * @param mu gravitational parameter (m^3/s^2)
         * @param segments ephemeris segments
         */
        public STKEphemeris(final String satelliteId, final double mu, final List<STKEphemerisSegment> segments) {
            this.satelliteId = Objects.requireNonNull(satelliteId);
            this.mu = mu;
            this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        }

        @Override
        public String getId() {
            return satelliteId;
        }

        @Override
        public double getMu() {
            return mu;
        }

        @Override
        public List<STKEphemerisSegment> getSegments() {
            return segments;
        }

        @Override
        public AbsoluteDate getStart() {
            return segments.get(0).getStart();
        }

        @Override
        public AbsoluteDate getStop() {
            return segments.get(segments.size() - 1).getStop();
        }

    }

    /**
     * STK coordinate system.
     * <p>
     * Currently, only Earth-centered coordinate systems are supported.
     * </p>
     */
    public enum STKCoordinateSystem {

        /** International Celestial Reference Frame. */
        ICRF,

        /** Mean equator and mean equinox of the J2000 epoch. */
        J2000,

        /** Central-body-dependent inertial frame, equivalent to ICRF for Earth. */
        INERTIAL,

        /** Fixed frame. */
        FIXED,

        /** True equator and true equinox of date. */
        TRUE_OF_DATE,

        /** Mean equator and mean equinox of date. */
        MEAN_OF_DATE,

        /** True equator and mean equinox of date. */
        TEME_OF_DATE;

        /**
         * Parses a coordinate system from a string.
         * @param s string
         * @return coordinate system
         */
        public static STKCoordinateSystem parse(final String s) {
            final String sUpper = s.toUpperCase(Locale.US);
            switch (sUpper) {
                case "ICRF":
                    return ICRF;
                case "J2000":
                    return J2000;
                case "INERTIAL":
                    return INERTIAL;
                case "FIXED":
                    return FIXED;
                case "TRUEOFDATE":
                    return TRUE_OF_DATE;
                case "MEANOFDATE":
                    return MEAN_OF_DATE;
                case "TEMEOFDATE":
                    return TEME_OF_DATE;
                default:
                    throw new OrekitException(OrekitMessages.STK_INVALID_OR_UNSUPPORTED_COORDINATE_SYSTEM, s);
            }
        }

    }

}
