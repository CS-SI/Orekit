/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Represents a parsed SP3 orbit file.
 * @author Thomas Neidhart
 * @author Evan Ward
 */
public class SP3File implements EphemerisFile {

    /** File type indicator. */
    public enum SP3FileType {
        /** GPS only file. */
        GPS,
        /** Mixed file. */
        MIXED,
        /** GLONASS only file. */
        GLONASS,
        /** LEO only file. */
        LEO,
        /** Galileo only file. */
        GALILEO,
        /** COMPASS only file. */
        COMPASS,
        /** QZSS only file. */
        QZSS,
        /** undefined file format. */
        UNDEFINED
    }

    /** Orbit type indicator. */
    public enum SP3OrbitType {
        /** fitted. */
        FIT,
        /** extrapolated or predicted. */
        EXT,
        /** broadcast. */
        BCT,
        /** fitted after applying a Helmert transformation. */
        HLM;

        /** Parse a string to get the type.
         * @param s string to parse
         * @return the type corresponding to the string
         * @exception IllegalArgumentException if the string does not correspond to a type
         */
        public static SP3OrbitType parseType(final String s) {
            final String normalizedString = s.trim().toUpperCase();
            if ("EST".equals(normalizedString)) {
                return FIT;
            } else {
                return valueOf(normalizedString);
            }
        }

    }

    /** Time system used throughout this SP3 file. */
    public enum TimeSystem {
        /** Global Positioning System. */
        GPS,
        /** GLONASS. */
        GLO,
        /** GALILEO. */
        GAL,
        /** International Atomic Time. */
        TAI,
        /** Coordinated Universal Time. */
        UTC,
        /** Quasi-Zenith System. */
        QZS
    }

    /** File type. */
    private SP3FileType type;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Epoch of the file. */
    private AbsoluteDate epoch;

    /** GPS week. */
    private int gpsWeek;

    /** Seconds of the current GPS week. */
    private double secondsOfWeek;

    /** Julian day. */
    private int julianDay;

    /** Day fraction. */
    private double dayFraction;

    /** Time-interval between epochs. */
    private double epochInterval;

    /** Number of epochs. */
    private int numberOfEpochs;

    /** Coordinate system. */
    private String coordinateSystem;

    /** Data used indicator. */
    private String dataUsed;

    /** Orbit type. */
    private SP3OrbitType orbitType;

    /** Agency providing the file. */
    private String agency;

    /** Indicates if data contains velocity or not. */
    private CartesianDerivativesFilter filter;

    /** Time scale of dates in the ephemeris file. */
    private TimeScale timeScale;

    /** Time scale, as specified in the file. */
    private String timeScaleString;

    /** Standard gravitational parameter in m^3 / s^2. */
    private final double mu;

    /** Number of samples to use when interpolating. */
    private final int interpolationSamples;

    /** Maps {@link #coordinateSystem} to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** A map containing satellite information. */
    private Map<String, SP3Ephemeris> satellites;

    /**
     * Create a new SP3 file object.
     *
     * @param mu                   is the standard gravitational parameter in m^3 / s^2.
     * @param interpolationSamples number of samples to use in interpolation.
     * @param frameBuilder         for constructing a reference frame from the identifier
     *                             in the file.
     */
    SP3File(final double mu,
            final int interpolationSamples,
            final Function<? super String, ? extends Frame> frameBuilder) {
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.frameBuilder = frameBuilder;
        // must be linked has map to preserve order of satellites in the file.
        satellites = new LinkedHashMap<>();
    }

    /**
     * Set the derivatives filter.
     *
     * @param filter that indicates which derivatives of position are available.
     */
    void setFilter(final CartesianDerivativesFilter filter) {
        this.filter = filter;
    }

    /**
     * Set the time scale.
     *
     * @param timeScale use to parse dates in this file.
     */
    void setTimeScale(final TimeScale timeScale) {
        this.timeScale = timeScale;
    }

    /**
     * Set the string used to define the time scale.
     *
     * @param timeScaleString the time scale identifier used in the file.
     */
    void setTimeScaleString(final String timeScaleString) {
        this.timeScaleString = timeScaleString;
    }

    /** Returns the {@link SP3FileType} associated with this SP3 file.
     * @return the file type for this SP3 file
     */
    public SP3FileType getType() {
        return type;
    }

    /** Set the file type for this SP3 file.
     * @param fileType the file type to be set
     */
    void setType(final SP3FileType fileType) {
        this.type = fileType;
    }

    /** Returns the {@link TimeSystem} used to time-stamp position entries.
     * @return the {@link TimeSystem} of the orbit file
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the time system used in this SP3 file.
     * @param system the time system to be set
     */
    void setTimeSystem(final TimeSystem system) {
        this.timeSystem = system;
    }

    /** Returns the data used indicator from the SP3 file.
     * @return the data used indicator (unparsed)
     */
    public String getDataUsed() {
        return dataUsed;
    }

    /** Set the data used indicator for this SP3 file.
     * @param data the data used indicator to be set
     */
    void setDataUsed(final String data) {
        this.dataUsed = data;
    }

    /** Returns the start epoch of the orbit file.
     * @return the start epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set the epoch of the SP3 file.
     * @param time the epoch to be set
     */
    void setEpoch(final AbsoluteDate time) {
        this.epoch = time;
    }

    /** Returns the GPS week as contained in the SP3 file.
     * @return the GPS week of the SP3 file
     */
    public int getGpsWeek() {
        return gpsWeek;
    }

    /** Set the GPS week of the SP3 file.
     * @param week the GPS week to be set
     */
    void setGpsWeek(final int week) {
        this.gpsWeek = week;
    }

    /** Returns the seconds of the GPS week as contained in the SP3 file.
     * @return the seconds of the GPS week
     */
    public double getSecondsOfWeek() {
        return secondsOfWeek;
    }

    /** Set the seconds of the GPS week for this SP3 file.
     * @param seconds the seconds to be set
     */
    void setSecondsOfWeek(final double seconds) {
        this.secondsOfWeek = seconds;
    }

    /** Returns the julian day for this SP3 file.
     * @return the julian day
     */
    public int getJulianDay() {
        return julianDay;
    }

    /** Set the julian day for this SP3 file.
     * @param day the julian day to be set
     */
    void setJulianDay(final int day) {
        this.julianDay = day;
    }

    /** Returns the day fraction for this SP3 file.
     * @return the day fraction
     */
    public double getDayFraction() {
        return dayFraction;
    }

    /** Set the day fraction for this SP3 file.
     * @param fraction the day fraction to be set
     */
    void setDayFraction(final double fraction) {
        this.dayFraction = fraction;
    }

    /** Returns the time interval between epochs (in seconds).
     * @return the time interval between epochs
     */
    public double getEpochInterval() {
        return epochInterval;
    }

    /** Set the epoch interval for this SP3 file.
     * @param interval the interval between orbit entries
     */
    void setEpochInterval(final double interval) {
        this.epochInterval = interval;
    }

    /** Returns the number of epochs contained in this orbit file.
     * @return the number of epochs
     */
    public int getNumberOfEpochs() {
        return numberOfEpochs;
    }

    /** Set the number of epochs as contained in the SP3 file.
     * @param epochCount the number of epochs to be set
     */
    void setNumberOfEpochs(final int epochCount) {
        this.numberOfEpochs = epochCount;
    }

    /** Returns the coordinate system of the entries in this orbit file.
     * @return the coordinate system
     */
    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    /** Set the coordinate system used for the orbit entries.
     * @param system the coordinate system to be set
     */
    void setCoordinateSystem(final String system) {
        this.coordinateSystem = system;
    }

    /** Returns the {@link SP3OrbitType} for this SP3 file.
     * @return the orbit type
     */
    public SP3OrbitType getOrbitType() {
        return orbitType;
    }

    /** Set the {@link SP3OrbitType} for this SP3 file.
     * @param oType the orbit type to be set
     */
    void setOrbitType(final SP3OrbitType oType) {
        this.orbitType = oType;
    }

    /** Returns the agency that prepared this SP3 file.
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /** Set the agency string for this SP3 file.
     * @param agencyStr the agency string to be set
     */
    void setAgency(final String agencyStr) {
        this.agency = agencyStr;
    }

    /** Add a new satellite with a given identifier to the list of
     * stored satellites.
     * @param satId the satellite identifier
     */
    public void addSatellite(final String satId) {
        // only add satellites which have not been added before
        satellites.putIfAbsent(satId, new SP3Ephemeris(satId));
    }

    @Override
    public Map<String, SP3Ephemeris> getSatellites() {
        return Collections.unmodifiableMap(satellites);
    }

    /** Get the number of satellites contained in this orbit file.
     * @return the number of satellites
     */
    public int getSatelliteCount() {
        return satellites.size();
    }

    /**
     * Set the formal accuracy for a satellite.
     *
     * @param index    is the index of the satellite.
     * @param accuracy of the satellite, in m.
     */
    void setAccuracy(final int index, final double accuracy) {
        int n = index;
        for (final SP3Ephemeris ephemeris : satellites.values()) {
            if (n == 0) {
                ephemeris.setAccuracy(accuracy);
                return;
            }
            n--;
        }
    }

    /** Tests whether a satellite with the given id is contained in this orbit
     * file.
     * @param satId the satellite id
     * @return {@code true} if the satellite is contained in the file,
     *         {@code false} otherwise
     */
    public boolean containsSatellite(final String satId) {
        return satellites.containsKey(satId);
    }

    /**
     * Adds a new P/V coordinate for a given satellite.
     *
     * @param satId the satellite identifier
     * @param coord the P/V coordinate of the satellite
     */
    void addSatelliteCoordinate(final String satId, final SP3Coordinate coord) {
        satellites.get(satId).coordinates.add(coord);
    }

    /** An ephemeris for a single satellite in a SP3 file. */
    public class SP3Ephemeris implements SatelliteEphemeris, EphemerisSegment {

        /** Satellite ID. */
        private final String id;
        /** Ephemeris Data. */
        private final List<SP3Coordinate> coordinates;
        /** Accuracy in m. */
        private double accuracy;

        /**
         * Create an ephemeris for a single satellite.
         *
         * @param id of the satellite.
         */
        SP3Ephemeris(final String id) {
            this.id = id;
            this.coordinates = new ArrayList<>();
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public double getMu() {
            return mu;
        }

        @Override
        public String getFrameString() {
            return getCoordinateSystem();
        }

        @Override
        public Frame getFrame() throws OrekitException {
            return frameBuilder.apply(getFrameString());
        }

        @Override
        public String getTimeScaleString() {
            return timeScaleString;
        }

        @Override
        public TimeScale getTimeScale() throws OrekitException {
            return timeScale;
        }

        @Override
        public int getInterpolationSamples() {
            return interpolationSamples;
        }

        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return filter;
        }

        @Override
        public List<SP3Coordinate> getCoordinates() {
            return Collections.unmodifiableList(this.coordinates);
        }

        /** Returns a list containing only {@code this}. */
        @Override
        public List<SP3Ephemeris> getSegments() {
            return Collections.singletonList(this);
        }

        @Override
        public AbsoluteDate getStart() {
            return coordinates.get(0).getDate();
        }

        @Override
        public AbsoluteDate getStop() {
            return coordinates.get(coordinates.size() - 1).getDate();
        }

        @Override
        public BoundedPropagator getPropagator() throws OrekitException {
            return EphemerisSegment.super.getPropagator();
        }

        /**
         * Set the accuracy for this satellite.
         *
         * @param accuracy in m.
         */
        void setAccuracy(final double accuracy) {
            this.accuracy = accuracy;
        }

        /**
         * Get the formal accuracy for this satellite.
         *
         * <p> The accuracy is limited by the SP3 standard to be a power of 2.
         *
         * @return magnitude of one standard deviation, in m.
         */
        public double getAccuracy() {
            return accuracy;
        }

    }

    /** A single record of position clock and possibly derivatives in an SP3 file. */
    public static class SP3Coordinate extends TimeStampedPVCoordinates {

        /** Serializable UID. */
        private static final long serialVersionUID = 20161116L;
        /** Clock correction in s. */
        private final double clock;
        /** Clock rate in s / s. */
        private final double clockRate;

        /**
         * Create a coordinate with only position.
         *
         * @param date     of validity.
         * @param position of the satellite.
         * @param clock    correction in s.
         */
        SP3Coordinate(final AbsoluteDate date,
                      final Vector3D position,
                      final double clock) {
            this(date, position, Vector3D.ZERO, clock, 0);
        }

        /**
         * Create a coordinate with position and velocity.
         *
         * @param date      of validity.
         * @param position  of the satellite.
         * @param velocity  of the satellite.
         * @param clock     correction in s.
         * @param clockRate in s / s.
         */
        SP3Coordinate(final AbsoluteDate date,
                      final Vector3D position,
                      final Vector3D velocity,
                      final double clock,
                      final double clockRate) {
            super(date, position, velocity, Vector3D.ZERO);
            this.clock = clock;
            this.clockRate = clockRate;
        }

        /**
         * Returns the clock correction value.
         *
         * @return the clock correction in s.
         */
        public double getClockCorrection() {
            return clock;
        }

        /**
         * Returns the clock rate.
         *
         * @return the clock rate of change in s/s.
         */
        public double getClockRateChange() {
            return clockRate;
        }

    }

}
