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
package org.orekit.time;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/** Holder for date and time components.
 * <p>This class is a simple holder with no processing methods.</p>
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @see AbsoluteDate
 * @see DateComponents
 * @see TimeComponents
 * @author Luc Maisonobe
 */
public class DateTimeComponents implements Serializable, Comparable<DateTimeComponents> {

    /**
     * The Julian Epoch.
     *
     * @see TimeScales#getJulianEpoch()
     */
    public static final DateTimeComponents JULIAN_EPOCH =
            new DateTimeComponents(DateComponents.JULIAN_EPOCH, TimeComponents.H12);

    /** Serializable UID. */
    private static final long serialVersionUID = 5061129505488924484L;

    /** Date component. */
    private final DateComponents date;

    /** Time component. */
    private final TimeComponents time;

    /** Build a new instance from its components.
     * @param date date component
     * @param time time component
     */
    public DateTimeComponents(final DateComponents date, final TimeComponents time) {
        this.date = date;
        this.time = time;
    }

    /** Build an instance from raw level components.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final int month, final int day,
                              final int hour, final int minute, final double second)
        throws IllegalArgumentException {
        this.date = new DateComponents(year, month, day);
        this.time = new TimeComponents(hour, minute, second);
    }

    /** Build an instance from raw level components.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final Month month, final int day,
                              final int hour, final int minute, final double second)
        throws IllegalArgumentException {
        this.date = new DateComponents(year, month, day);
        this.time = new TimeComponents(hour, minute, second);
    }

    /** Build an instance from raw level components.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final int month, final int day)
        throws IllegalArgumentException {
        this.date = new DateComponents(year, month, day);
        this.time = TimeComponents.H00;
    }

    /** Build an instance from raw level components.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final Month month, final int day)
        throws IllegalArgumentException {
        this.date = new DateComponents(year, month, day);
        this.time = TimeComponents.H00;
    }

    /** Build an instance from a seconds offset with respect to another one.
     * @param reference reference date/time
     * @param offset offset from the reference in seconds
     * @see #offsetFrom(DateTimeComponents)
     */
    public DateTimeComponents(final DateTimeComponents reference,
                              final double offset) {

        // extract linear data from reference date/time
        int    day     = reference.getDate().getJ2000Day();
        double seconds = reference.getTime().getSecondsInLocalDay();

        // apply offset
        seconds += offset;

        // fix range
        final int dayShift = (int) FastMath.floor(seconds / Constants.JULIAN_DAY);
        seconds -= Constants.JULIAN_DAY * dayShift;
        day     += dayShift;
        final TimeComponents tmpTime = new TimeComponents(seconds);

        // set up components
        this.date = new DateComponents(day);
        this.time = new TimeComponents(tmpTime.getHour(), tmpTime.getMinute(), tmpTime.getSecond(),
                                       reference.getTime().getMinutesFromUTC());

    }

    /** Parse a string in ISO-8601 format to build a date/time.
     * <p>The supported formats are all date formats supported by {@link DateComponents#parseDate(String)}
     * and all time formats supported by {@link TimeComponents#parseTime(String)} separated
     * by the standard time separator 'T', or date components only (in which case a 00:00:00 hour is
     * implied). Typical examples are 2000-01-01T12:00:00Z or 1976W186T210000.
     * </p>
     * @param string string to parse
     * @return a parsed date/time
     * @exception IllegalArgumentException if string cannot be parsed
     */
    public static DateTimeComponents parseDateTime(final String string) {

        // is there a time ?
        final int tIndex = string.indexOf('T');
        if (tIndex > 0) {
            return new DateTimeComponents(DateComponents.parseDate(string.substring(0, tIndex)),
                                          TimeComponents.parseTime(string.substring(tIndex + 1)));
        }

        return new DateTimeComponents(DateComponents.parseDate(string), TimeComponents.H00);

    }

    /** Compute the seconds offset between two instances.
     * @param dateTime dateTime to subtract from the instance
     * @return offset in seconds between the two instants
     * (positive if the instance is posterior to the argument)
     * @see #DateTimeComponents(DateTimeComponents, double)
     */
    public double offsetFrom(final DateTimeComponents dateTime) {
        final int dateOffset = date.getJ2000Day() - dateTime.date.getJ2000Day();
        final double timeOffset = time.getSecondsInUTCDay() - dateTime.time.getSecondsInUTCDay();
        return Constants.JULIAN_DAY * dateOffset + timeOffset;
    }

    /** Get the date component.
     * @return date component
     */
    public DateComponents getDate() {
        return date;
    }

    /** Get the time component.
     * @return time component
     */
    public TimeComponents getTime() {
        return time;
    }

    /** {@inheritDoc} */
    public int compareTo(final DateTimeComponents other) {
        final int dateComparison = date.compareTo(other.date);
        if (dateComparison < 0) {
            return -1;
        } else if (dateComparison > 0) {
            return 1;
        }
        return time.compareTo(other.time);
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final DateTimeComponents otherDateTime = (DateTimeComponents) other;
            return otherDateTime != null &&
                   date.equals(otherDateTime.date) && time.equals(otherDateTime.time);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return (date.hashCode() << 16) ^ time.hashCode();
    }

    /** Return a string representation of this pair.
     * <p>The format used is ISO8601 including the UTC offset.</p>
     * @return string representation of this pair
     */
    public String toString() {
        return date.toString() + 'T' + time.toString();
    }

    /**
     * Get a string representation of the date-time without the offset from UTC. The
     * format used is ISO6801, except without the offset from UTC.
     *
     * @return a string representation of the date-time.
     * @see #toStringWithoutUtcOffset(int, int)
     * @see #toString(int, int)
     * @see #toStringRfc3339()
     */
    public String toStringWithoutUtcOffset() {
        return date.toString() + 'T' + time.toStringWithoutUtcOffset();
    }


    /**
     * Return a string representation of this date-time, rounded to millisecond
     * precision.
     *
     * <p>The format used is ISO8601 including the UTC offset.</p>
     *
     * @param minuteDuration 60, 61, or 62 seconds depending on the date being close to a
     *                       leap second introduction and the magnitude of the leap
     *                       second.
     * @return string representation of this date, time, and UTC offset
     * @see #toString(int, int)
     */
    public String toString(final int minuteDuration) {
        return toString(minuteDuration, 3);
    }

    /**
     * Return a string representation of this date-time, rounded to the given precision.
     *
     * <p>The format used is ISO8601 including the UTC offset.</p>
     *
     * @param minuteDuration 59, 60, 61, or 62 seconds depending on the date being close
     *                       to a leap second introduction and the magnitude of the leap
     *                       second.
     * @param fractionDigits the number of digits to include after the decimal point in
     *                       the string representation of the seconds. The date and time
     *                       is first rounded as necessary. {@code fractionDigits} must
     *                       be greater than or equal to {@code 0}.
     * @return string representation of this date, time, and UTC offset
     * @see #toStringRfc3339()
     * @see #toStringWithoutUtcOffset()
     * @see #toStringWithoutUtcOffset(int, int)
     * @since 11.0
     */
    public String toString(final int minuteDuration, final int fractionDigits) {
        return toStringWithoutUtcOffset(minuteDuration, fractionDigits) +
                time.formatUtcOffset();
    }

    /**
     * Return a string representation of this date-time, rounded to the given precision.
     *
     * <p>The format used is ISO8601 without the UTC offset.</p>
     *
     * @param minuteDuration 59, 60, 61, or 62 seconds depending on the date being close
     *                       to a leap second introduction and the magnitude of the leap
     *                       second.
     * @param fractionDigits the number of digits to include after the decimal point in
     *                       the string representation of the seconds. The date and time
     *                       is first rounded as necessary. {@code fractionDigits} must
     *                       be greater than or equal to {@code 0}.
     * @return string representation of this date, time, and UTC offset
     * @see #toStringRfc3339()
     * @see #toStringWithoutUtcOffset()
     * @see #toString(int, int)
     * @since 11.1
     */
    public String toStringWithoutUtcOffset(final int minuteDuration,
                                           final int fractionDigits) {
        final DecimalFormat secondsFormat =
                new DecimalFormat("00", new DecimalFormatSymbols(Locale.US));
        secondsFormat.setMaximumFractionDigits(fractionDigits);
        secondsFormat.setMinimumFractionDigits(fractionDigits);
        final DateTimeComponents rounded = roundIfNeeded(minuteDuration, fractionDigits);
        return rounded.getDate().toString() + 'T' +
                rounded.getTime().toStringWithoutUtcOffset(secondsFormat);
    }

    /**
     * Round this date-time to the given precision if needed to prevent rounding up to an
     * invalid seconds number. This is useful, for example, when writing custom date-time
     * formatting methods so one does not, e.g., end up with "60.0" seconds during a
     * normal minute when the value of seconds is {@code 59.999}. This method will instead
     * round up the minute, hour, day, month, and year as needed.
     *
     * @param minuteDuration 59, 60, 61, or 62 seconds depending on the date being close
     *                       to a leap second introduction and the magnitude of the leap
     *                       second.
     * @param fractionDigits the number of decimal digits after the decimal point in the
     *                       seconds number that will be printed. This date-time is
     *                       rounded to {@code fractionDigits} after the decimal point if
     *                       necessary to prevent rounding up to {@code minuteDuration}.
     *                       {@code fractionDigits} must be greater than or equal to
     *                       {@code 0}.
     * @return a date-time within {@code 0.5 * 10**-fractionDigits} seconds of this, and
     * with a seconds number that will not round up to {@code minuteDuration} when rounded
     * to {@code fractionDigits} after the decimal point.
     * @since 11.3
     */
    public DateTimeComponents roundIfNeeded(final int minuteDuration,
                                            final int fractionDigits) {
        double second = time.getSecond();
        final double wrap = minuteDuration - 0.5 * FastMath.pow(10, -fractionDigits);
        if (second >= wrap) {
            // we should wrap around to the next minute
            int minute = time.getMinute();
            int hour   = time.getHour();
            int j2000  = date.getJ2000Day();
            second = 0;
            ++minute;
            if (minute > 59) {
                minute = 0;
                ++hour;
                if (hour > 23) {
                    hour = 0;
                    ++j2000;
                }
            }
            return new DateTimeComponents(
                    new DateComponents(j2000),
                    new TimeComponents(hour, minute, second));
        }
        return this;
    }

    /**
     * Represent the given date and time as a string according to the format in RFC 3339.
     * RFC3339 is a restricted subset of ISO 8601 with a well defined grammar. This method
     * includes enough precision to represent the point in time without rounding up to the
     * next minute.
     *
     * <p>RFC3339 is unable to represent BC years, years of 10000 or more, time zone
     * offsets of 100 hours or more, or NaN. In these cases the value returned from this
     * method will not be valid RFC3339 format.
     *
     * @return RFC 3339 format string.
     * @see <a href="https://tools.ietf.org/html/rfc3339#page-8">RFC 3339</a>
     * @see AbsoluteDate#toStringRfc3339(TimeScale)
     * @see #toString(int, int)
     * @see #toStringWithoutUtcOffset()
     */
    public String toStringRfc3339() {
        final DateComponents d = this.getDate();
        final TimeComponents t = this.getTime();
        // date
        final String dateString = String.format("%04d-%02d-%02dT",
                d.getYear(), d.getMonth(), d.getDay());
        // time
        final String timeString;
        if (t.getSecondsInLocalDay() != 0) {
            final DecimalFormat format = new DecimalFormat("00.##############", new DecimalFormatSymbols(Locale.US));
            timeString = String.format("%02d:%02d:", t.getHour(), t.getMinute()) +
                    format.format(t.getSecond());
        } else {
            // shortcut for midnight local time
            timeString = "00:00:00";
        }
        // offset
        final int minutesFromUTC = t.getMinutesFromUTC();
        final String timeZoneString;
        if (minutesFromUTC == 0) {
            timeZoneString = "Z";
        } else {
            // sign must be accounted for separately because there is no -0 in Java.
            final String sign = minutesFromUTC < 0 ? "-" : "+";
            final int utcOffset = FastMath.abs(minutesFromUTC);
            final int hourOffset = utcOffset / 60;
            final int minuteOffset = utcOffset % 60;
            timeZoneString = sign + String.format("%02d:%02d", hourOffset, minuteOffset);
        }
        return dateString + timeString + timeZoneString;
    }

}

