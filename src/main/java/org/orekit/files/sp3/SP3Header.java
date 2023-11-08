/* Copyright 2023 Luc Maisonobe
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
import java.util.Collections;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;

/** Header for SP3 files.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Header {

    /** String representation of the center of ephemeris coordinate system. **/
    public static final String SP3_FRAME_CENTER_STRING = "EARTH";

    /** Name for pos/vel accuracy base header entry. */
    private static final String POS_VEL_ACCURACY_BASE = "pos/vel accuracy base";

    /** Name for clock accuracy base header entry. */
    private static final String CLOCK_ACCURACY_BASE = "clock accuracy base";

    /** Name for comments header entry. */
    private static final String COMMENTS = "comments";

    /** File version. */
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
    private int modifiedJulianDay;

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

    /** Key for orbit type. */
    private String orbitTypeKey;

    /** Agency providing the file. */
    private String agency;

    /** Indicates if data contains velocity or not. */
    private CartesianDerivativesFilter filter;

    /** Base for position/velocity accuracy. */
    private double posVelBase;

    /** Base for clock/clock-rate accuracy. */
    private double clockBase;

    /** Satellite identifiers. */
    private List<String> satIds;

    /** Satellite accuracies. */
    private double[] accuracies;

    /** Comments. */
    private final List<String> comments;

    /** Create a new SP3 header.
     */
    public SP3Header() {
        this.version    = '?';
        this.satIds     = new ArrayList<>();
        this.accuracies = null;
        this.comments   = new ArrayList<>();
    }

    /** Check header is valid.
     * @param parsing if true, we are parsing an existing file, and are more lenient
     * in order to accept some common errors (like between 86 and 99 satellites
     * in SP3a, SP3b or SP3c files)
     * @param hasAccuracy if true, there are accuracy data in the file
     * @param fileName file name to generate the error message
     * @exception OrekitException if file is not valid
     */
    void validate(final boolean parsing, final boolean hasAccuracy, final String fileName) throws OrekitException {

        // check version
        if ("abcd".indexOf(getVersion()) < 0) {
            throw new OrekitException(OrekitMessages.SP3_UNSUPPORTED_VERSION, getVersion());
        }

        if (getVersion() == 'a') {
            // in SP3 version a, the base accuracy must be set to 0
            if (getPosVelBase() != 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          POS_VEL_ACCURACY_BASE, getPosVelBase(), fileName, getVersion());
            }
            if (getClockBase() != 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          CLOCK_ACCURACY_BASE, getClockBase(), fileName, getVersion());
            }
        } else if (hasAccuracy) {
            // in SP3 versions after version a, the base accuracy must be set if entries specify accuracy
            if (getPosVelBase() <= 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          POS_VEL_ACCURACY_BASE, getPosVelBase(), fileName, getVersion());
            }
            if (getClockBase() <= 0.0) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          CLOCK_ACCURACY_BASE, getClockBase(), fileName, getVersion());
            }
        }
        if (getVersion() < 'd') {
            // in SP3 versions a, b, and c, there are exactly 4 comments with max length 57
            // (60 minus first three characters)
            if (comments.size() != 4 ||
                comments.get(0).length() > 57 ||
                comments.get(1).length() > 57 ||
                comments.get(2).length() > 57 ||
                comments.get(3).length() > 57) {
                throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                          COMMENTS, "/* …", fileName, getVersion());
            }
        } else {
            // starting with SP3 version d, there is an unspecified number of comments with max length 77
            // (80 minus first three characters)
            for (final String c : comments) {
                if (c.length() > 77) {
                    throw new OrekitException(OrekitMessages.SP3_INVALID_HEADER_ENTRY,
                                              COMMENTS, c, fileName, getVersion());
                }
            }
        }

    }

    /** Set the file version.
     * @param version file version
     */
    public void setVersion(final char version) {
        this.version = version;
    }

    /** Get the file version.
     * @return file version
     */
    public char getVersion() {
        return version;
    }

    /** Set the derivatives filter.
     * @param filter that indicates which derivatives of position are available.
     */
    public void setFilter(final CartesianDerivativesFilter filter) {
        this.filter = filter;
    }

    /** Get the derivatives filter.
     * @return filter with available derivatives
     */
    public CartesianDerivativesFilter getFilter() {
        return filter;
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

    /** Returns the modified julian day for this SP3 file.
     * @return the modified julian day
     */
    public int getModifiedJulianDay() {
        return modifiedJulianDay;
    }

    /** Set the modified julian day for this SP3 file.
     * @param day the modified julian day to be set
     */
    public void setModifiedJulianDay(final int day) {
        this.modifiedJulianDay = day;
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
     */
    public String getOrbitTypeKey() {
        return orbitTypeKey;
    }

    /** Set the orbit type key for this SP3 file.
     * @param oTypeKey the orbit type key to be set
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
     */
    public void setPosVelBase(final double posVelBase) {
        this.posVelBase = posVelBase;
    }

    /** Get the base for position/velocity accuracy.
     * @return base for position/velocity accuracy
     */
    public double getPosVelBase() {
        return posVelBase;
    }

    /** Set the base for clock/clock-rate accuracy.
     * @param clockBase base for clock/clock-rate accuracy
     */
    public void setClockBase(final double clockBase) {
        this.clockBase = clockBase;
    }

    /** Get the base for clock/clock-rate accuracy.
     * @return base for clock/clock-rate accuracy
     */
    public double getClockBase() {
        return clockBase;
    }

    /** Add a satellite identifier.
     * @param satId satellite identifier
     */
    public void addSatId(final String satId) {
        satIds.add(satId);
    }

    /** Get the satellite identifiers.
     * @return satellites identifiers
     */
    public List<String> getSatIds() {
        return Collections.unmodifiableList(satIds);
    }

    /** Set the accuracy.
     * @param index satellite index in {@link #getSatIds()}
     * @param accuracy in m
     */
    public void setAccuracy(final int index, final double accuracy) {
        if (accuracies == null) {
            // lazy allocation of the array
            accuracies = new double[satIds.size()];
        }
        accuracies[index] = accuracy;
    }

    /** Get the formal accuracy.
     * <p>
     * The accuracy is limited by the SP3 standard to be a power of 2 in mm.
     * The value returned here is in meters.
     * </p>
     * @param satId satellite identifier
     * @return magnitude of one standard deviation, in m.
     */
    public double getAccuracy(final String satId) {
        for (int i = 0; i < satIds.size(); ++i) {
            if (satIds.get(i).equals(satId)) {
                return accuracies[i];
            }
        }
        return Double.NaN;
    }

    /** Get the comments.
     * @return an unmodifiable view of comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Add a comment.
     * @param comment comment to add
     */
    public void addComment(final String comment) {
        comments.add(comment);
    }

}
