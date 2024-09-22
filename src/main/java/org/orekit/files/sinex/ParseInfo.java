/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.files.sinex;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

import java.util.regex.Pattern;

/** Transient data used for parsing a SINEX file.
 * @param <T> type of the SINEX files
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class ParseInfo<T extends AbstractSinex> {

    /** 00:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_TWO_DIGITS = "00:000:00000";

    /** 0000:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_FOUR_DIGITS = "0000:000:00000";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Time scales. */
    private final TimeScales timeScales;

    /** Name of the data source. */
    private String name;

    /** Current line. */
    private String line;

    /** Current line number of the navigation message. */
    private int lineNumber;

    /** SINEX file creation date as extracted for the first line. */
    private AbsoluteDate creationDate;

    /** SINEX file creation date as extracted for the first line. */
    private String creationDateString;

    /** Start time of the data used in the Sinex solution. */
    private AbsoluteDate startDate;

    /** Start time of the data used in the Sinex solution. */
    private String startDateString;

    /** End time of the data used in the Sinex solution. */
    private AbsoluteDate endDate;

    /** End time of the data used in the Sinex solution. */
    private String endDateString;

    /** Time scale. */
    private TimeScale timeScale;

    /** Simple constructor.
     * @param timeScales time scales
     */
    protected ParseInfo(final TimeScales timeScales) {
        this.timeScales = timeScales;
    }

    /** Start parsing of a new data source.
     * @param name name of the new data source
     */
    void newSource(final String name) {
        // initialize parsing
        this.name = name;
        this.line = null;
        this.lineNumber = 0;
    }

    /** Get name of data source.
     * @return name of data source
     */
    protected String getName() {
        return name;
    }

    /** Set current line.
     * @param line current line
     */
    void setLine(final String line)
    {
        this.line = line;
    }

    /** Get current line.
     * @return current line
     */
    String getLine() {
        return line;
    }

    /** Increment line number.
     */
    void incrementLineNumber() {
        ++lineNumber;
    }

    /** Get current line number.
     * @return current line number
     */
    int getLineNumber() {
        return lineNumber;
    }

    /** Set creation date.
     * @param creationDateString creation date
     */
    protected void setCreationDate(final String creationDateString) {
        this.creationDateString = creationDateString;
        this.creationDate = stringEpochToAbsoluteDate(creationDateString, false);
    }

    /** Get creation date.
     * @return creation date
     */
    protected AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /** Set start date if earlier than previous setting.
     * @param candidateStartDateString candidate start date
     */
    protected void setStartDateIfEarlier(final String candidateStartDateString) {
        final AbsoluteDate candidateStart = stringEpochToAbsoluteDate(candidateStartDateString, true);
        if (startDate == null || candidateStart.isBefore(startDate)) {
            // this is either the first setting
            // or we are parsing a new data source referring to an earlier date than previous ones
            this.startDateString = candidateStartDateString;
            this.startDate = candidateStart;
        }
    }

    /** Get start date.
     * @return start date
     */
    protected AbsoluteDate getStartDate() {
        return startDate;
    }

    /** Set end date if later than previous setting.
     * @param candidateEndDateString end date
     */
    protected void setEndDateIfLater(final String candidateEndDateString) {
        final AbsoluteDate candidateEnd = stringEpochToAbsoluteDate(candidateEndDateString, true);
        if (endDate == null || candidateEnd.isAfter(endDate)) {
            // this is either the first setting
            // or we are parsing a new data source referring to a later date than previous ones
            this.endDateString = candidateEndDateString;
            this.endDate = candidateEnd;
        }
    }

    /** Get end date.
     * @return end date
     */
    protected AbsoluteDate getEndDate() {
        return endDate;
    }

    /** Set time scale.
     * @param timeScale time scale
     */
    protected void setTimeScale(final TimeScale timeScale) {

        this.timeScale = timeScale;

        // A time scale has been parsed, update start, end, and creation dates
        // to take into account the time scale
        if (startDateString != null) {
            startDate = stringEpochToAbsoluteDate(startDateString, true);
        }
        if (endDateString != null) {
            endDate = stringEpochToAbsoluteDate(endDateString, false);
        }
        if (creationDateString != null) {
            creationDate = stringEpochToAbsoluteDate(creationDateString, false);
        }

    }

    /** Get time scales.
     * @return time scales
     */
    TimeScales getTimeScales() {
        return timeScales;
    }

    /** Build the parsed file.
     * @return built parsed file
     */
    protected abstract T build();

    /** Extract a string from current line.
     * @param start  start index of the string
     * @param length length of the string
     * @return parsed string
     */
    protected String parseString(final int start, final int length) {
        return line.substring(start, FastMath.min(line.length(), start + length)).trim();
    }

    /** Extract a double from current line.
     * @param start  start index of the real
     * @param length length of the real
     * @return parsed real
     */
    protected double parseDouble(final int start, final int length) {
        return Double.parseDouble(parseString(start, length));
    }

    /** Extract an integer from current line.
     * @param start  start index of the real
     * @param length length of the real
     * @return parsed integer
     */
    protected int parseInt(final int start, final int length) {
        return Integer.parseInt(parseString(start, length));
    }

    /** Extract a double from current line and convert in SI unit.
     * @param startUnit    start index of the unit
     * @param lengthUnit   length of the unit
     * @param startDouble  start index of the real
     * @param lengthDouble length of the real
     * @return parsed double in SI unit
     */
    protected double parseDoubleWithUnit(final int startUnit, final int lengthUnit,
                                         final int startDouble, final int lengthDouble) {
        final Unit unit = Unit.parse(parseString(startUnit, lengthUnit));
        return unit.toSI(parseDouble(startDouble, lengthDouble));
    }

    /** Transform a String epoch to an AbsoluteDate.
     * @param stringDate string epoch
     * @param isStart    true if epoch is a start validity epoch
     * @return the corresponding AbsoluteDate
     */
    protected AbsoluteDate stringEpochToAbsoluteDate(final String stringDate, final boolean isStart) {

        // Deal with 00:000:00000 epochs
        if (DEFAULT_EPOCH_TWO_DIGITS.equals(stringDate) || DEFAULT_EPOCH_FOUR_DIGITS.equals(stringDate)) {
            // If it's a start validity epoch, the file start date shall be used.
            // For end validity epoch, future infinity is acceptable.
            return isStart ? startDate : AbsoluteDate.FUTURE_INFINITY;
        }

        // Date components
        final String[] fields = SEPARATOR.split(stringDate);

        // Read fields
        final int digitsYear = Integer.parseInt(fields[0]);
        final int day = Integer.parseInt(fields[1]);
        final int secInDay = Integer.parseInt(fields[2]);

        // Data year
        final int year;
        if (digitsYear > 50 && digitsYear < 100) {
            year = 1900 + digitsYear;
        } else if (digitsYear < 100) {
            year = 2000 + digitsYear;
        } else {
            year = digitsYear;
        }

        // Return an absolute date.
        // Initialize to 1st January of the given year because
        // sometimes day is equal to 0 in the file.
        return new AbsoluteDate(new DateComponents(year, 1, 1), timeScale).
               shiftedBy(Constants.JULIAN_DAY * (day - 1)).
               shiftedBy(secInDay);

    }

}
