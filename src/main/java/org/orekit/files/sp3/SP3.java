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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;

/**
 * Represents a parsed SP3 orbit file.
 * @author Thomas Neidhart
 * @author Evan Ward
 */
public class SP3 implements EphemerisFile<SP3Coordinate, SP3Segment> {

    /** Header.
     * @since 12.0
     */
    private final SP3Header header;

    /** Standard gravitational parameter in m³ / s². */
    private final double mu;

    /** Number of samples to use when interpolating. */
    private final int interpolationSamples;

    /** Reference frame. */
    private final Frame frame;

    /** A map containing satellite information. */
    private Map<String, SP3Ephemeris> satellites;

    /**
     * Create a new SP3 file object.
     *
     * @param mu                   is the standard gravitational parameter in m³ / s².
     * @param interpolationSamples number of samples to use in interpolation.
     * @param frame                reference frame
     */
    public SP3(final double mu, final int interpolationSamples, final Frame frame) {
        this.header               = new SP3Header();
        this.mu                   = mu;
        this.interpolationSamples = interpolationSamples;
        this.frame                = frame;
        this.satellites           = new LinkedHashMap<>(); // must be linked hash map to preserve order of satellites in the file
    }

    /** Check file is valid.
     * @param parsing if true, we are parsing an existing file, and are more lenient
     * in order to accept some common errors (like between 86 and 99 satellites
     * in SP3a, SP3b or SP3c files)
     * @param fileName file name to generate the error message
     * @exception OrekitException if file is not valid
     */
    public void validate(final boolean parsing, final String fileName) throws OrekitException {

        // check available data
        final SortedSet<AbsoluteDate> epochs = new TreeSet<>(new ChronologicalComparator());
        boolean hasAccuracy = false;
        for (final Map.Entry<String, SP3Ephemeris> entry : satellites.entrySet()) {
            SP3Coordinate previous = null;
            for (final SP3Segment segment : entry.getValue().getSegments()) {
                for (final SP3Coordinate coordinate : segment.getCoordinates()) {
                    final AbsoluteDate previousDate = previous == null ? header.getEpoch() : previous.getDate();
                    final double       nbSteps      = coordinate.getDate().durationFrom(previousDate) / header.getEpochInterval();
                    if (FastMath.abs(nbSteps - FastMath.rint(nbSteps)) > 0.001) {
                        // not an integral number of steps
                        throw new OrekitException(OrekitMessages.INCONSISTENT_SAMPLING_DATE,
                                                  previousDate.shiftedBy(FastMath.rint(nbSteps) * header.getEpochInterval()),
                                                  coordinate.getDate());
                    }
                    epochs.add(coordinate.getDate());
                    previous = coordinate;
                    hasAccuracy |= !(coordinate.getPositionAccuracy() == null &&
                                    coordinate.getVelocityAccuracy() == null &&
                                    Double.isNaN(coordinate.getClockAccuracy()) &&
                                    Double.isNaN(coordinate.getClockRateAccuracy()));
                }
            }
        }

        // check versions limitations
        if (getSatelliteCount() > getMaxAllowedSatCount(parsing)) {
            throw new OrekitException(OrekitMessages.SP3_TOO_MANY_SATELLITES_FOR_VERSION,
                                      header.getVersion(), getMaxAllowedSatCount(parsing), getSatelliteCount(),
                                      fileName);
        }

        header.validate(parsing, hasAccuracy, fileName);

        // check epochs
        if (epochs.size() != header.getNumberOfEpochs()) {
            throw new OrekitException(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH,
                                      epochs.size(), fileName, header.getNumberOfEpochs());
        }

    }

    /** Get the header.
     * @return header
     * @since 12.0
     */
    public SP3Header getHeader() {
        return header;
    }

    /** Get maximum number of satellites allowed for format version.
     * @param parsing if true, we are parsing an existing file, and are more lenient
     * in order to accept some common errors (like between 86 and 99 satellites
     * in SP3a, SP3b or SP3c files)
     * @return maximum number of satellites allowed for format version
     * @since 12.0
     */
    private int getMaxAllowedSatCount(final boolean parsing) {
        return header.getVersion() < 'd' ? (parsing ? 99 : 85) : 999;
    }

    /** Splice several SP3 files together.
     * <p>
     * Splicing SP3 files is intended to be used when continuous computation
     * covering more than one file is needed. The files should all have the exact same
     * metadata: {@link SP3Header#getType() type}, {@link SP3Header#getTimeSystem() time system},
     * {@link SP3Header#getCoordinateSystem() coordinate system}, except for satellite accuracy
     * which can be different from one file to the next one, and some satellites may
     * be missing in some files… Once sorted (which is done internally), if the gap between
     * segments from two file is at most {@link SP3Header#getEpochInterval() epoch interval},
     * then the segments are merged as one segment, otherwise the segments are kept separated.
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
        final SortedSet<SP3> sorted = new TreeSet<>((s1, s2) -> comparator.compare(s1.header.getEpoch(), s2.header.getEpoch()));
        sorted.addAll(sp3);

        // prepare spliced file
        final SP3 first   = sorted.first();
        final SP3 spliced = new SP3(first.mu, first.interpolationSamples, first.frame);
        spliced.header.setFilter(first.header.getFilter());
        spliced.header.setType(first.header.getType());
        spliced.header.setTimeSystem(first.header.getTimeSystem());
        spliced.header.setDataUsed(first.header.getDataUsed());
        spliced.header.setEpoch(first.header.getEpoch());
        spliced.header.setGpsWeek(first.header.getGpsWeek());
        spliced.header.setSecondsOfWeek(first.header.getSecondsOfWeek());
        spliced.header.setModifiedJulianDay(first.header.getModifiedJulianDay());
        spliced.header.setDayFraction(first.header.getDayFraction());
        spliced.header.setEpochInterval(first.header.getEpochInterval());
        spliced.header.setCoordinateSystem(first.header.getCoordinateSystem());
        spliced.header.setOrbitTypeKey(first.header.getOrbitTypeKey());
        spliced.header.setAgency(first.header.getAgency());
        spliced.header.setPosVelBase(first.header.getPosVelBase());
        spliced.header.setClockBase(first.header.getClockBase());

        // identify the satellites that are present in all files
        final List<String> commonSats = new ArrayList<>(first.header.getSatIds());
        for (final SP3 current : sorted) {
            for (final Iterator<String> iter = commonSats.iterator(); iter.hasNext();) {
                final String sat = iter.next();
                if (!current.containsSatellite(sat)) {
                    iter.remove();
                    break;
                }
            }
        }

        // create the spliced list
        for (final String sat : commonSats) {
            spliced.addSatellite(sat);
        }

        // in order to be conservative, we keep the worst accuracy from all SP3 files for this satellite
        for (int i = 0; i < commonSats.size(); ++i) {
            final String sat = commonSats.get(i);
            double accuracy = Double.POSITIVE_INFINITY;
            for (final SP3 current : sorted) {
                accuracy = FastMath.max(accuracy, current.header.getAccuracy(sat));
            }
            spliced.header.setAccuracy(i, accuracy);
        }

        // splice files
        SP3 previous = null;
        int epochCount = 0;
        for (final SP3 current : sorted) {

            epochCount += current.header.getNumberOfEpochs();
            if (previous != null) {

                // check metadata and check if we should drop the last entry of previous file
                final boolean dropLast = current.checkSplice(previous);
                if (dropLast) {
                    --epochCount;
                }

                // append the pending data from previous file
                for (final Map.Entry<String, SP3Ephemeris> entry : previous.satellites.entrySet()) {
                    if (commonSats.contains(entry.getKey())) {
                        final SP3Ephemeris splicedEphemeris = spliced.getEphemeris(entry.getKey());
                        for (final SP3Segment segment : entry.getValue().getSegments()) {
                            final List<SP3Coordinate> coordinates = segment.getCoordinates();
                            for (int i = 0; i < coordinates.size() - (dropLast ? 1 : 0); ++i) {
                                splicedEphemeris.addCoordinate(coordinates.get(i), spliced.header.getEpochInterval());
                            }
                        }
                    }
                }

            }

            previous = current;

        }
        spliced.header.setNumberOfEpochs(epochCount);

        // append the pending data from last file
        for (final Map.Entry<String, SP3Ephemeris> entry : previous.satellites.entrySet()) {
            if (commonSats.contains(entry.getKey())) {
                final SP3Ephemeris splicedEphemeris = spliced.getEphemeris(entry.getKey());
                for (final SP3Segment segment : entry.getValue().getSegments()) {
                    for (final SP3Coordinate coordinate : segment.getCoordinates()) {
                        splicedEphemeris.addCoordinate(coordinate, spliced.header.getEpochInterval());
                    }
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

        if (!(previous.header.getType()             == header.getType()                  &&
              previous.header.getTimeSystem()       == header.getTimeSystem()            &&
              previous.header.getOrbitType()        == header.getOrbitType()             &&
              previous.header.getCoordinateSystem().equals(header.getCoordinateSystem()) &&
              previous.header.getDataUsed().equals(header.getDataUsed())                 &&
              previous.header.getAgency().equals(header.getAgency()))) {
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
                    dropLast = dt < 0.001 * header.getEpochInterval();
                }
            }
        }

        return dropLast;

    }

    /** Add a new satellite with a given identifier to the list of
     * stored satellites.
     * @param satId the satellite identifier
     */
    public void addSatellite(final String satId) {
        header.addSatId(satId);
        satellites.putIfAbsent(satId, new SP3Ephemeris(satId, mu, frame, interpolationSamples, header.getFilter()));
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
        return header.getSatIds().contains(satId);
    }

}
