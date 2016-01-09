/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.time;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;


/** Class representing a time within the day broken up as hour,
 * minute and second components.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see DateComponents
 * @see DateTimeComponents
 * @author Luc Maisonobe
 */
public class TimeComponents implements Serializable, Comparable<TimeComponents> {

    /** Constant for commonly used hour 00:00:00. */
    public static final TimeComponents H00   = new TimeComponents(0, 0, 0);

    /** Constant for commonly used hour 12:00:00. */
    public static final TimeComponents H12 = new TimeComponents(12, 0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = -8566834296299377436L;

    /** Format for hours and minutes. */
    private static final DecimalFormat TWO_DIGITS = new DecimalFormat("00");

    /** Format for seconds. */
    private static final DecimalFormat SECONDS_FORMAT =
        new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US));

    /** Basic and extends formats for local time, UTC time (only 0 difference with UTC is supported). */
    private static Pattern ISO8601_FORMATS = Pattern.compile("^(\\d\\d):?(\\d\\d):?(\\d\\d(?:[.,]\\d+)?)?(?:Z|[-+]00(?::00)?)?$");

    /** Hour number. */
    private final int hour;

    /** Minute number. */
    private final int minute;

    /** Second number. */
    private final double second;

    /** Build a time from its clock elements.
     * <p>Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed
     * in this method, since they do occur during leap seconds introduction
     * in the {@link UTCScale UTC} time scale.</p>
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 61.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public TimeComponents(final int hour, final int minute, final double second)
        throws IllegalArgumentException {

        // range check
        if ((hour   < 0) || (hour   >  23) ||
            (minute < 0) || (minute >  59) ||
            (second < 0) || (second >= 61.0)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_HMS_TIME,
                                                     hour, minute, second);
        }

        this.hour = hour;
        this.minute = minute;
        this.second = second;

    }

    /** Build a time from the second number within the day.
     * @param secondInDay second number from 0.0 to {@link
     * org.orekit.utils.Constants#JULIAN_DAY} (excluded)
     * @exception IllegalArgumentException if seconds number is out of range
     */
    public TimeComponents(final double secondInDay) {
        this(0, secondInDay);
    }

    /** Build a time from the second number within the day.
     * <p>
     * The second number is defined here as the sum
     * {@code secondInDayA + secondInDayB} from 0.0 to {@link
     * org.orekit.utils.Constants#JULIAN_DAY} (excluded). The two parameters
     * are used for increased accuracy.
     * </p>
     * @param secondInDayA first part of the second number
     * @param secondInDayB last part of the second number
     * @exception IllegalArgumentException if seconds number is out of range
     */
    public TimeComponents(final int secondInDayA, final double secondInDayB) {

        // split the numbers as a whole number of seconds
        // and a fractional part between 0.0 (included) and 1.0 (excluded)
        final int carry         = (int) FastMath.floor(secondInDayB);
        int wholeSeconds        = secondInDayA + carry;
        final double fractional = secondInDayB - carry;

        // range check
        if (wholeSeconds < 0 || wholeSeconds > 86400) {
            // beware, 86400 must be allowed to cope with leap seconds introduction days
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER,
                                                     wholeSeconds);
        }

        // extract the time components
        hour          = wholeSeconds / 3600;
        wholeSeconds -= 3600 * hour;
        minute        = wholeSeconds / 60;
        wholeSeconds -= 60 * minute;
        second        = wholeSeconds + fractional;

    }

    /** Parse a string in ISO-8601 format to build a time.
     * <p>The supported formats are:
     * <ul>
     *   <li>basic format local time: hhmmss (with optional decimals in seconds)</li>
     *   <li>extended format local time: hh:mm:ss (with optional decimals in seconds)</li>
     *   <li>basic format UTC time: hhmmssZ (with optional decimals in seconds)</li>
     *   <li>extended format UTC time: hh:mm:ssZ (with optional decimals in seconds)</li>
     *   <li>basic format local time with 00h UTC offset: hhmmss+00 (with optional decimals in seconds)</li>
     *   <li>extended format local time with 00h UTC offset: hhmmss+00 (with optional decimals in seconds)</li>
     *   <li>basic format local time with 00h and 00m UTC offset: hhmmss+00:00 (with optional decimals in seconds)</li>
     *   <li>extended format local time with 00h and 00m UTC offset: hhmmss+00:00 (with optional decimals in seconds)</li>
     * </ul>
     * As shown by the list above, only the complete representations defined in section 4.2
     * of ISO-8601 standard are supported, neither expended representations nor representations
     * with reduced accuracy are supported.
     * </p>
     * <p>As this class does not support time zones (because space flight dynamics uses {@link
     * TimeScale time scales} with offsets from UTC having sub-second accuracy), only UTC is zone is
     * supported (and in fact ignored). It is the responsibility of the {@link AbsoluteDate} class to
     * handle time scales appropriately.</p>
     * @param string string to parse
     * @return a parsed time
     * @exception IllegalArgumentException if string cannot be parsed
     */
    public static  TimeComponents parseTime(final String string) {

        // is the date a calendar date ?
        final Matcher timeMatcher = ISO8601_FORMATS.matcher(string);
        if (timeMatcher.matches()) {
            return new TimeComponents(Integer.parseInt(timeMatcher.group(1)),
                                      Integer.parseInt(timeMatcher.group(2)),
                                      Double.parseDouble(timeMatcher.group(3).replace(',', '.')));
        }

        throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_TIME, string);

    }

    /** Get the hour number.
     * @return hour number from 0 to 23
     */
    public int getHour() {
        return hour;
    }

    /** Get the minute number.
     * @return minute minute number from 0 to 59
     */
    public int getMinute() {
        return minute;
    }

    /** Get the seconds number.
     * @return second second number from 0.0 to 60.0 (excluded)
     */
    public double getSecond() {
        return second;
    }

    /** Get the second number within the day.
     * @return second number from 0.0 to Constants.JULIAN_DAY
     */
    public double getSecondsInDay() {
        return second + 60 * minute + 3600 * hour;
    }

    /** Get a string representation of the time.
     * @return string representation of the time
     */
    public String toString() {
        return new StringBuffer().
        append(TWO_DIGITS.format(hour)).append(':').
        append(TWO_DIGITS.format(minute)).append(':').
        append(SECONDS_FORMAT.format(second)).
        toString();
    }

    /** {@inheritDoc} */
    public int compareTo(final TimeComponents other) {
        return Double.compare(getSecondsInDay(), other.getSecondsInDay());
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final TimeComponents otherTime = (TimeComponents) other;
            return (otherTime != null) && (hour == otherTime.hour) &&
                   (minute == otherTime.minute) && (second == otherTime.second);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final long bits = Double.doubleToLongBits(second);
        return ((hour << 16) ^ (minute << 8)) ^ (int) (bits ^ (bits >>> 32));
    }

}
