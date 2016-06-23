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
            return (otherDateTime != null) &&
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
     * <p>The format used is ISO8601.</p>
     * @return string representation of this pair
     */
    public String toString() {
        return toString(60);
    }

    /** Return a string representation of this pair.
     * <p>The format used is ISO8601.</p>
     * @param minuteDuration 60 or 61 depending on the date being
     * close to a leap second introduction
     * @return string representation of this pair
     */
    public String toString(final int minuteDuration) {
        double second = time.getSecond();
        final double wrap = minuteDuration - 0.0005;
        if (second >= wrap) {
            // we should wrap around next millisecond
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
            return new DateComponents(j2000).toString() + 'T' + new TimeComponents(hour, minute, second).toString();
        }
        return date.toString() + 'T' + time.toString();
    }

}

