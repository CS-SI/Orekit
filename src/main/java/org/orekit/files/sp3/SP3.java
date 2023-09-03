/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.gnss.TimeSystem;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Represents a parsed SP3 orbit file.
 * @author Thomas Neidhart
 * @author Evan Ward
 */
public class SP3
    implements EphemerisFile<SP3.SP3Coordinate, SP3.SP3Ephemeris> {

    /** String representation of the center of ephemeris coordinate system. **/
    public static final String SP3_FRAME_CENTER_STRING = "EARTH";

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
        /** SBAS only file. */
        SBAS,
        /** IRNSS only file. */
        IRNSS,
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
        HLM,
        /** other type, defined by SP3 file producing agency.
         * @since 9.3
         */
        OTHER;

        /** Parse a string to get the type.
         * @param s string to parse
         * @return the type corresponding to the string
         */
        public static SP3OrbitType parseType(final String s) {
            final String normalizedString = s.trim().toUpperCase(Locale.US);
            if ("EST".equals(normalizedString)) {
                return FIT;
            } else if ("BHN".equals(normalizedString)) {
                // ESOC navigation team uses BHN for files produced
                // by their main parameter estimation program Bahn
                return FIT;
            } else if ("PRO".equals(normalizedString)) {
                // ESOC navigation team uses PRO for files produced
                // by their orbit propagation program Propag
                return EXT;
            } else {
                try {
                    return valueOf(normalizedString);
                } catch (IllegalArgumentException iae) {
                    return OTHER;
                }
            }
        }

    }

    /** Name for pos/vel accuracy base header entry.
     * @since 12.0
     */
    private static final String POS_VEL_ACCURACY_BASE = "pos/vel accuracy base";

    /** Name for clock accuracy base header entry.
     * @since 12.0
     */
    private static final String CLOCK_ACCURACY_BASE = "clock accuracy base";

    /** File version.
     * @since 12.0
     */
    private char version;

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
    private List<DataUsed> dataUsed;

    /** Orbit type. */
    private SP3OrbitType orbitType;

    /** Key for orbit type.
     * @since 9.3
     */
    private String orbitTypeKey;

    /** Agency providing the file. */
    private String agency;

    /** Indicates if data contains velocity or not. */
    private CartesianDerivativesFilter filter;

    /** Base for position/velocity accuracy.
     * @since 12.0
     */
    private double posVelBase;

    /** Base for clock/clock-rate accuracy.
     * @since 12.0
     */
    private double clockBase;

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
     */
    public SP3(final double mu,
               final int interpolationSamples,
               final Function<? super String, ? extends Frame> frameBuilder) {
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.frameBuilder = frameBuilder;
        this.version      = '?';
        // must be linked hash map to preserve order of satellites in the file.
        satellites = new LinkedHashMap<>();
    }

    /** Check file is valid.
     * @param parsing if true, we are parsing an existing file, and are more lenient
     * in order to accept some common errors (like between 86 and 99 satellites
     * in SP3a, SP3b or SP3c files)
     * @param fileName file name to generate the error message
     * @exception OrekitException if file is not valid
     */
    public void validate(final boolean parsing, final String fileName) throws OrekitException {

        // check available data the number of epochs
        final SortedSet<AbsoluteDate> epochs = new TreeSet<>(new ChronologicalComparator());
        boolean hasAccuracy = false;
        for (final Map.Entry<String, SP3Ephemeris> entry : satellites.entrySet()) {
            for (final SP3Coordinate coordinate : entry.getValue().getCoordinates()) {
                epochs.add(coordinate.getDate());
                hasAccuracy |= coordinate.hasAccuracy();
            }
        }

        // check version
        if ("abcd".indexOf(getVersion()) < 0) {
            throw new OrekitException(OrekitMessages.SP3_UNSUPPORTED_VERSION, version);
        }

        // check versions limitations
        if (getSatelliteCount() > getMaxAllowedSatCount(parsing)) {
            throw new OrekitException(OrekitMessages.SP3_TOO_MANY_SATELLITES_FOR_VERSION,
                                      getVersion(), getMaxAllowedSatCount(parsing), getSatelliteCount(),
                                      fileName);
        }
        if (getVersion() == 'a') {
            if (getPosVelBase() != 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          POS_VEL_ACCURACY_BASE, getPosVelBase(), fileName);
            }
            if (getClockBase() != 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          CLOCK_ACCURACY_BASE, getClockBase(), fileName);
            }
        } else if (hasAccuracy) {
            if (getPosVelBase() <= 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          POS_VEL_ACCURACY_BASE, getPosVelBase(), fileName);
            }
            if (getClockBase() <= 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          CLOCK_ACCURACY_BASE, getClockBase(), fileName);
            }
        }

        // check epochs
        if (epochs.size() != getNumberOfEpochs()) {
            throw new OrekitException(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH,
                                      epochs.size(), fileName, getNumberOfEpochs());
        }

    }

    /** Get maximum number of satellites allowed for format version.
     * @param parsing if true, we are parsing an existing file, and are more lenient
     * in order to accept some common errors (like between 86 and 99 satellites
     * in SP3a, SP3b or SP3c files)
     * @return maximum number of satellites allowed for format version
     * @since 12.0
     */
    private int getMaxAllowedSatCount(final boolean parsing) {
        return "abc".indexOf(getVersion()) >= 0 ? (parsing ? 99 : 85) : 999;
    }

    /** Splice several SP3 files together.
     * <p>
     * Splicing SP3 files is intended to be used when continuous computation
     * covering more than one file is needed. The files should all have the exact same
     * metadata: {@link #getType() type}, {@link #getTimeSystem() time system},
     * {@link #getCoordinateSystem() coordinate system}, except for satellite accuracy
     * which can be different from one file to the next one, and some satellites may
     * be missing in some files… Once sorted (which is done internally), the gap between
     * each file should not exceed the {@link #getEpochInterval() epoch interval}.
     * </p>
     * <p>
     * The spliced file only contains the satellites that were present in all files.
     * Satellites present in some files and absent from other files are silently
     * dropped.
     * </p>
     * <p>
     * Depending on producer, successive SP3 files either have a gap between the last
     * entry of one file and the first entry of the next file (for example files with
     * a 5 minutes epoch interval may end at 23:55 and the next file start at 00:00),
     * or both files have one point exactly at the splicing date (i.e. 24:00 one day
     * and 00:00 next day). In the later case, the last point of the early file is dropped
     * and the first point of the late file takes precedence, hence only one point remains
     * in the spliced file ; this design choice is made to enforce continuity and
     * regular interpolation.
     * </p>
     * @param sp3 SP3 files to merge
     * @return merged SP3
     * @since 12.0
     */
    public static SP3 splice(final Collection<SP3> sp3) {

        // sort the files
        final ChronologicalComparator comparator = new ChronologicalComparator();
        final SortedSet<SP3> sorted = new TreeSet<>((s1, s2) -> comparator.compare(s1.getEpoch(), s2.getEpoch()));
        sorted.addAll(sp3);

        // prepare spliced file
        final SP3 first   = sorted.first();
        final SP3 spliced = new SP3(first.mu, first.interpolationSamples, first.frameBuilder);
        spliced.setFilter(first.filter);
        spliced.setType(first.getType());
        spliced.setTimeSystem(first.getTimeSystem());
        spliced.setDataUsed(first.getDataUsed());
        spliced.setEpoch(first.getEpoch());
        spliced.setGpsWeek(first.getGpsWeek());
        spliced.setSecondsOfWeek(first.getSecondsOfWeek());
        spliced.setJulianDay(first.getJulianDay());
        spliced.setDayFraction(first.getDayFraction());
        spliced.setEpochInterval(first.getEpochInterval());
        spliced.setCoordinateSystem(first.getCoordinateSystem());
        spliced.setOrbitTypeKey(first.getOrbitTypeKey());
        spliced.setAgency(first.getAgency());

        // identify the satellites that are present in all files
        final List<String> firstSats  = new ArrayList<>();
        final List<String> commonSats = new ArrayList<>();
        for (final Map.Entry<String, SP3Ephemeris> entry : first.satellites.entrySet()) {
            firstSats.add(entry.getKey());
            commonSats.add(entry.getKey());
        }
        for (final SP3 current : sorted) {
            for (final String sat : commonSats) {
                if (current.containsSatellite(sat)) {
                    // in order to be conservative, we keep the worst accuracy from all SP3 files for this satellite
                    final SP3Ephemeris ephemeris      = current.getEphemeris(sat);
                    final SP3Ephemeris firstEphemeris = first.getEphemeris(sat);
                    firstEphemeris.setAccuracy(FastMath.max(firstEphemeris.getAccuracy(), ephemeris.getAccuracy()));
                } else {
                    commonSats.remove(sat);
                    break;
                }
            }
        }
        for (int i = 0; i < commonSats.size(); ++i) {
            final String sat = commonSats.get(i);
            spliced.addSatellite(sat);
            spliced.getEphemeris(sat).setAccuracy(first.getEphemeris(sat).getAccuracy());
        }

        // splice files
        SP3 previous = null;
        int epochCount = 0;
        for (final SP3 current : sorted) {

            epochCount += current.getNumberOfEpochs();
            if (previous != null) {

                // check metadata and check if we should drop the last entry of previous file
                final boolean dropLast = current.checkSplice(previous);
                if (dropLast) {
                    --epochCount;
                }

                // append the pending data from previous file
                for (final Map.Entry<String, SP3Ephemeris> entry : previous.satellites.entrySet()) {
                    if (commonSats.contains(entry.getKey())) {
                        final List<SP3Coordinate> coordinates = entry.getValue().getCoordinates();
                        for (int i = 0; i < coordinates.size() - (dropLast ? 1 : 0); ++i) {
                            spliced.addSatelliteCoordinate(entry.getKey(), coordinates.get(i));
                        }
                    }
                }

            }

            previous = current;

        }
        spliced.setNumberOfEpochs(epochCount);

        // append the pending data from last file
        for (final Map.Entry<String, SP3Ephemeris> entry : previous.satellites.entrySet()) {
            if (commonSats.contains(entry.getKey())) {
                for (final SP3Coordinate coordinate : entry.getValue().getCoordinates()) {
                    spliced.addSatelliteCoordinate(entry.getKey(), coordinate);
                }
            }
        }
        return spliced;

    }

    /** Check if instance can be spliced after previous one.
     * @param previous SP3 file (should already be sorted to be before current instance), can be null
     * @return true if last entry of previous file should be dropped as first entry of current file
     * is at very close date and will take precedence
     * @exception OrekitException if metadata are incompatible
     * @since 12.0
     */
    private boolean checkSplice(final SP3 previous) throws OrekitException {

        if (!(previous.getType()             == getType()                  &&
              previous.getTimeSystem()       == getTimeSystem()            &&
              previous.getOrbitType()        == getOrbitType()             &&
              previous.getCoordinateSystem().equals(getCoordinateSystem()) &&
              previous.getDataUsed().equals(getDataUsed())                 &&
              previous.getAgency().equals(getAgency()))) {
            throw new OrekitException(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA);
        }

        boolean dropLast = false;
        for (final Map.Entry<String, SP3Ephemeris> entry : previous.satellites.entrySet()) {
            final SP3Ephemeris previousEphem = entry.getValue();
            final SP3Ephemeris currentEphem  = satellites.get(entry.getKey());
            if (currentEphem != null) {
                if (!(previousEphem.getAvailableDerivatives()    == currentEphem.getAvailableDerivatives() &&
                      previousEphem.getFrame()                   == currentEphem.getFrame()                &&
                      previousEphem.getInterpolationSamples()    == currentEphem.getInterpolationSamples() &&
                      Precision.equals(previousEphem.getMu(),       currentEphem.getMu(), 2))) {
                    throw new OrekitException(OrekitMessages.SP3_INCOMPATIBLE_SATELLITE_MEDATADA,
                                              entry.getKey());
                } else {
                    final double dt = currentEphem.getStart().durationFrom(previousEphem.getStop());
                    if (dt > getEpochInterval()) {
                        throw new OrekitException(OrekitMessages.SP3_TOO_LARGE_GAP_FOR_SPLICING,
                                                  entry.getKey(),
                                                  currentEphem.getStart().durationFrom(previousEphem.getStop()));
                    }
                    dropLast = dt < 0.001 * getEpochInterval();
                }
            }
        }

        return dropLast;

    }

    /**
     * Set the file version.
     *
     * @param version file version
     * @since 12.0
     */
    public void setVersion(final char version) {
        this.version = version;
    }

    /**
     * Get the file version.
     *
     * @return file version
     * @since 12.0
     */
    public char getVersion() {
        return version;
    }

    /**
     * Set the derivatives filter.
     *
     * @param filter that indicates which derivatives of position are available.
     */
    public void setFilter(final CartesianDerivativesFilter filter) {
        this.filter = filter;
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
    public void setType(final SP3FileType fileType) {
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
    public void setTimeSystem(final TimeSystem system) {
        this.timeSystem = system;
    }

    /** Returns the data used indicator from the SP3 file.
     * @return the data used indicator
     */
    public List<DataUsed> getDataUsed() {
        return dataUsed;
    }

    /** Set the data used indicator for this SP3 file.
     * @param dataUsed the data used indicator to be set
     */
    public void setDataUsed(final List<DataUsed> dataUsed) {
        this.dataUsed = dataUsed;
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
    public void setEpoch(final AbsoluteDate time) {
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
    public void setGpsWeek(final int week) {
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
    public void setSecondsOfWeek(final double seconds) {
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
    public void setJulianDay(final int day) {
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
    public void setDayFraction(final double fraction) {
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
    public void setEpochInterval(final double interval) {
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
    public void setNumberOfEpochs(final int epochCount) {
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
    public void setCoordinateSystem(final String system) {
        this.coordinateSystem = system;
    }

    /** Returns the {@link SP3OrbitType} for this SP3 file.
     * @return the orbit type
     */
    public SP3OrbitType getOrbitType() {
        return orbitType;
    }

    /** Returns the orbit type key for this SP3 file.
     * @return the orbit type key
     * @since 9.3
     */
    public String getOrbitTypeKey() {
        return orbitTypeKey;
    }

    /** Set the orbit type key for this SP3 file.
     * @param oTypeKey the orbit type key to be set
     * @since 9.3
     */
    public void setOrbitTypeKey(final String oTypeKey) {
        this.orbitTypeKey = oTypeKey;
        this.orbitType    = SP3OrbitType.parseType(oTypeKey);
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
    public void setAgency(final String agencyStr) {
        this.agency = agencyStr;
    }

    /** Set the base for position/velocity accuracy.
     * @param posVelBase base for position/velocity accuracy
     * @since 12.0
     */
    public void setPosVelBase(final double posVelBase) {
        this.posVelBase = posVelBase;
    }

    /** Get the base for position/velocity accuracy.
     * @return base for position/velocity accuracy
     * @since 12.0
     */
    public double getPosVelBase() {
        return posVelBase;
    }

    /** Set the base for clock/clock-rate accuracy.
     * @param clockBase base for clock/clock-rate accuracy
     * @since 12.0
     */
    public void setClockBase(final double clockBase) {
        this.clockBase = clockBase;
    }

    /** Get the base for clock/clock-rate accuracy.
     * @return base for clock/clock-rate accuracy
     * @since 12.0
     */
    public double getClockBase() {
        return clockBase;
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

    /** Get an ephemeris.
     * @param index index of the satellite
     * @return satellite ephemeris
     * @since 12.0
     */
    public SP3Ephemeris getEphemeris(final int index) {
        int n = index;
        for (final Map.Entry<String, SP3Ephemeris> entry : satellites.entrySet()) {
            if (n == 0) {
                return entry.getValue();
            }
            n--;
        }

        // satellite not found
        throw new OrekitException(OrekitMessages.INVALID_SATELLITE_ID, index);

    }

    /** Get an ephemeris.
     * @param satId satellite identifier
     * @return satellite ephemeris, or null if not found
     * @since 12.0
     */
    public SP3Ephemeris getEphemeris(final String satId) {
        final SP3Ephemeris ephemeris = satellites.get(satId);
        if (ephemeris == null) {
            throw new OrekitException(OrekitMessages.INVALID_SATELLITE_ID, satId);
        } else {
            return ephemeris;
        }
    }

    /** Get the number of satellites contained in this orbit file.
     * @return the number of satellites
     */
    public int getSatelliteCount() {
        return satellites.size();
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
    public void addSatelliteCoordinate(final String satId, final SP3Coordinate coord) {
        satellites.get(satId).coordinates.add(coord);
    }

    /** An ephemeris for a single satellite in a SP3 file. */
    public class SP3Ephemeris
        implements  EphemerisFile.SatelliteEphemeris<SP3Coordinate, SP3Ephemeris>,
                    EphemerisFile.EphemerisSegment<SP3Coordinate> {

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
        public SP3Ephemeris(final String id) {
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
        public Frame getFrame() {
            return frameBuilder.apply(SP3_FRAME_CENTER_STRING);
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
        public BoundedPropagator getPropagator() {
            return EphemerisSegment.super.getPropagator();
        }

        @Override
        public BoundedPropagator getPropagator(final AttitudeProvider attitudeProvider) {
            return EphemerisSegment.super.getPropagator(attitudeProvider);
        }

        /**
         * Set the accuracy for this satellite.
         *
         * @param accuracy in m.
         */
        public void setAccuracy(final double accuracy) {
            this.accuracy = accuracy;
        }

        /**
         * Get the formal accuracy for this satellite.
         *
         * <p>The accuracy is limited by the SP3 standard to be a power of 2 in mm.
         * The value returned here is in meters.</p>
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
        private static final long serialVersionUID = 20230903L;

        /** Clock correction in s. */
        private final double clock;

        /** Clock rate in s / s. */
        private final double clockRate;

        /** Position accuracy.
         * @since 12.0
         */
        private Vector3D positionAccuracy;

        /** Velocity accuracy.
         * @since 12.0
         */
        private Vector3D velocityAccuracy;

        /** Clock accuracy.
         * @since 12.0
         */
        private double clockAccuracy;

        /** Clock rate accuracy.
         * @since 12.0
         */
        private double clockRateAccuracy;

        /**
         * Create a coordinate with position and velocity.
         *
         * @param date      of validity.
         * @param position  of the satellite.
         * @param positionAccuracy  of the satellite (null if not known).
         * @param velocity  of the satellite.
         * @param velocityAccuracy  of the satellite (null if not known).
         * @param clock     correction in s.
         * @param clockAccuracy     correction in s ({@code Double.NaN} if not known).
         * @param clockRate in s / s.
         * @param clockRateAccuracy in s / s ({@code Double.NaN} if not known).
         * @since 12.0
         */
        public SP3Coordinate(final AbsoluteDate date,
                             final Vector3D position, final Vector3D positionAccuracy,
                             final Vector3D velocity, final Vector3D velocityAccuracy,
                             final double clock,      final double clockAccuracy,
                             final double clockRate,  final double clockRateAccuracy) {

            super(date, position, velocity, Vector3D.ZERO);
            this.clock     = clock;
            this.clockRate = clockRate;

            this.positionAccuracy  = positionAccuracy;
            this.velocityAccuracy  = velocityAccuracy;
            this.clockAccuracy     = clockAccuracy;
            this.clockRateAccuracy = clockRateAccuracy;

        }

        /** Get the clock correction value.
         * @return the clock correction in s.
         */
        public double getClockCorrection() {
            return clock;
        }

        /** Get the clock rate.
         * @return the clock rate of change in s/s.
         */
        public double getClockRateChange() {
            return clockRate;
        }

        /** Get the position accuracy.
         * @return position accuracy in m (null if not known).
         * @since 12.0
         */
        public Vector3D getPositionAccuracy() {
            return positionAccuracy;
        }

        /** Get the velocity accuracy.
         * @return velocity accuracy in m/s (null if not known).
         * @since 12.0
         */
        public Vector3D getVelocityAccuracy() {
            return velocityAccuracy;
        }

        /** Get the clock accuracy.
         * @return clock accuracy in s ({@code Double.NaN} if not known).
         * @since 12.0
         */
        public double getClockAccuracy() {
            return clockAccuracy;
        }

        /** Get the clock rate accuracy.
         * @return clock rate accuracy in s/s ({@code Double.NaN} if not known).
         * @since 12.0
         */
        public double getClockRateAccuracy() {
            return clockRateAccuracy;
        }

        /** Check if entry has any accuracy parameter.
         * @return true if entry has any accuracy parameter
         * @since 12.0
         */
        public boolean hasAccuracy() {
            return !(positionAccuracy == null &&
                     velocityAccuracy == null &&
                     Double.isNaN(clockAccuracy) &&
                     Double.isNaN(clockRateAccuracy));
        }

    }

}
