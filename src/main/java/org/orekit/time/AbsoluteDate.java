/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
import java.util.Date;

import org.orekit.errors.OrekitException;


/** This class represents a specific instant in time.

 * <p>Instances of this class are considered to be absolute in the sense
 * that each one represent the occurrence of some event and can be compared
 * to other instances or located in <em>any</em> {@link TimeScale time scale}. In
 * order to represent a specific event instant in two different time scales
 * (say {@link TAIScale TAI} and {@link UTCScale UTC} for example), only
 * one instance is needed, both representations being available by specifying
 * the time scales as parameter when calling the ad-hoc methods.</p>
 * <p>Two complementary views are available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #AbsoluteDate(ChunkedDate, ChunkedTime, TimeScale)}, {@link
 *   #AbsoluteDate(int, int, int, int, int, double, TimeScale)}, {@link
 *   #AbsoluteDate(int, int, int, TimeScale)}, {@link #AbsoluteDate(Date,
 *   TimeScale)}, {@link #createGPSDate(int, double)}, toString(){@link
 *   #toDate(TimeScale)}, {@link #toString(TimeScale) toString(timeScale)},
 *   {@link #toString()}, and {@link #timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtually perfect stopwatch.
 *   The related methods are {@link #AbsoluteDate(AbsoluteDate, double)},
 *   {@link #minus(AbsoluteDate)}, {@link #compareTo(AbsoluteDate)}, {@link #equals(Object)}
 *   and {@link #hashCode()}.</p>
 *   </li>
 * </ul>
 * <p>
 * A few reference epochs which are commonly used in space systems have been defined. These
 * epochs can be used as the basis for offset computation. The supported epochs are:
 * {@link #JULIAN_EPOCH}, {@link #MODIFIED_JULIAN_EPOCH}, {@link #FIFTIES_EPOCH},
 * {@link #GPS_EPOCH}, {@link #J2000_EPOCH}, {@link #JAVA_EPOCH}. In addition to these reference
 * epochs, two other constants are defined for convenience: {@link #PAST_INFINITY} and
 * {@link #FUTURE_INFINITY}, which can be used either as dummy dates when a date is not yet
 * initialized, or for initialization of loops searching for a min or max date.
 * </p>
 * <p>
 * Instances of the <code>AbsoluteDate</code> class are guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see TimeScale
 * @see TimeStamped
 * @see ChronologicalComparator
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class AbsoluteDate implements TimeStamped, Comparable<AbsoluteDate>, Serializable {

    /** Reference epoch for julian dates: -4712-01-01T12:00:00.
     * <p>Both <code>java.util.Date</code> and {@link ChunkedDate} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     */
    public static final AbsoluteDate JULIAN_EPOCH =
        new AbsoluteDate(ChunkedDate.JULIAN_EPOCH, ChunkedTime.H12, TTScale.getInstance());

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00. */
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH =
        new AbsoluteDate(ChunkedDate.MODIFIED_JULIAN_EPOCH, ChunkedTime.H00, TTScale.getInstance());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00. */
    public static final AbsoluteDate FIFTIES_EPOCH =
        new AbsoluteDate(ChunkedDate.FIFTIES_EPOCH, ChunkedTime.H00, TTScale.getInstance());

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time. */
    public static final AbsoluteDate GPS_EPOCH =
        new AbsoluteDate(ChunkedDate.GPS_EPOCH, ChunkedTime.H00, GPSScale.getInstance());

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC). */
    public static final AbsoluteDate J2000_EPOCH =
        new AbsoluteDate(ChunkedDate.J2000_EPOCH, ChunkedTime.H12, TTScale.getInstance());

    /** Java Reference epoch: 1970-01-01T00:00:00 TT. */
    public static final AbsoluteDate JAVA_EPOCH =
        new AbsoluteDate(ChunkedDate.JAVA_EPOCH, ChunkedTime.H00, TTScale.getInstance());

    /** Dummy date at infinity in the past direction. */
    public static final AbsoluteDate PAST_INFINITY =
        new AbsoluteDate(AbsoluteDate.JAVA_EPOCH, Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction. */
    public static final AbsoluteDate FUTURE_INFINITY =
        new AbsoluteDate(AbsoluteDate.JAVA_EPOCH, Double.POSITIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 617061803741806846L;

    /** Reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * <p>Beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT.</p> */
    private final long epoch;

    /** Offset from the reference epoch in seconds. */
    private final double offset;

    /** Create an instance with a default value ({@link #J2000_EPOCH}).
     */
    public AbsoluteDate() {
        epoch  = J2000_EPOCH.epoch;
        offset = J2000_EPOCH.offset;
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param dateTime location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final ChunksPair dateTime, final TimeScale timeScale) {
        this(dateTime.getDate(), dateTime.getTime(), timeScale);
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final ChunkedDate date, final ChunkedTime time,
                        final TimeScale timeScale) {
        // set the epoch at the start of the current minute
        final int j2000Day = date.getJ2000Day();
        epoch  = 60l * ((j2000Day * 24l + time.getHour()) * 60l + time.getMinute() - 720l);
        offset = time.getSecond() + timeScale.offsetToTAI(date, time);
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final int month, final int day,
                        final int hour, final int minute, final double second,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(new ChunkedDate(year, month, day), new ChunkedTime(hour, minute, second), timeScale);
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param date date location in the time scale
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final ChunkedDate date, final TimeScale timeScale)
        throws IllegalArgumentException {
        this(date, ChunkedTime.H00, timeScale);
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final int month, final int day,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(new ChunkedDate(year, month, day), ChunkedTime.H00, timeScale);
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final Date location, final TimeScale timeScale) {
        this(new ChunkedDate(ChunkedDate.JAVA_EPOCH,
                             (int) (location.getTime() / 86400000l)),
             new ChunkedTime(0.001 * (location.getTime() % 86400000l)),
             timeScale);
    }

    /** Build an instant from an offset with respect to another instant.
     * <p>It is important to note that the <code>offset</code> is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the offset between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 on {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but 2 seconds because a leap
     * second has been introduced at the end of 2005 in this time scale.</p>
     * @param instant reference instant
     * @param offset offset from the reference instant (seconds physically
     * separating the two instants)
     */
    public AbsoluteDate(final AbsoluteDate instant, final double offset) {
        epoch = instant.epoch;
        this.offset = instant.offset + offset;
    }

    /** Build an instant corresponding to a GPS date.
     * <p>GPS dates are provided as a week number starting at
     * {@link #GPS_EPOCH GPS epoch} and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since {@link #GPS_EPOCH GPS epoch}
     * @param milliInWeek number of milliseconds since week start
     * @return a new instant
     */
    public static AbsoluteDate createGPSDate(final int weekNumber,
                                             final double milliInWeek) {
        return new AbsoluteDate(GPS_EPOCH, weekNumber * 604800.0 + milliInWeek / 1000.0);
    }

    /** Get the TAI time.
     * <p>The TAI time is the number of seconds since 2000-01-01T12:00:00 in
     * {@link TAIScale TAI}. Note that this reference epoch is <strong>not</strong>
     * {@link #J2000_EPOCH J2000.0 epoch}, which is the same day but at noon in
     * {@link TTScale TT}.</p>
     * @return tai time in seconds
     */
    public double getTAITime() {
        return epoch + offset;
    }

    /** Compute the offset between two instant.
     * <p>The offset is the number of seconds physically elapsed
     * between the two instants.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instant (positive
     * if the instance is posterior to the argument)
     */
    public double minus(final AbsoluteDate instant) {
        return (epoch - instant.epoch) + (offset - instant.offset);
    }

    /** Compute the offset between two time scales at the current instant.
     * <p>The offset is defined as <i>l<sub>1</sub>-l<sub>2</sub></i>
     * where <i>l<sub>1</sub></i> is the location of the instant in
     * the <code>scale1</code> time scale and <i>l<sub>2</sub></i> is the
     * location of the instant in the <code>scale2</code> time scale.</p>
     * @param scale1 first time scale
     * @param scale2 second time scale
     * @return offset in seconds between the two time scales at the
     * current instant
     */
    public double timeScalesOffset(final TimeScale scale1,
                                   final TimeScale scale2) {
        return scale1.offsetFromTAI(this) - scale2.offsetFromTAI(this);
    }

    /** Convert the instance to a Java {@link java.util.Date Date}.
     * <p>Conversion to the Date class induces a loss of precision because
     * the Date class does not provide sub-millisecond information. Java Dates
     * are considered to be locations in some times scales.</p>
     * @param timeScale time scale to use
     * @return a {@link java.util.Date Date} instance representing the location
     * of the instant in the time scale
     */
    public Date toDate(final TimeScale timeScale) {
        final double time = epoch + offset + timeScale.offsetFromTAI(this);
        return new Date(Math.round((time + 10957.5 * 86400.0) * 1000));
    }

    /** Split the instance into date/time chunks.
     * @param timeScale time scale to use
     * @return date/time chunks
     */
    public ChunksPair getChunks(final TimeScale timeScale) {

        // compute offset from 2000-01-01T00:00:00 in specified time scale
        final double offset2000 = epoch + offset + 43200 + timeScale.offsetFromTAI(this);
        final int    day        = (int) Math.floor(offset2000 / 86400.0);

        // extract calendar elements
        ChunkedDate date = new ChunkedDate(ChunkedDate.J2000_EPOCH, day);
        ChunkedTime time = new ChunkedTime(offset2000 - 86400.0 * day);
        try {
            UTCScale utc = (UTCScale) timeScale;
            if (utc.insideLeap(this)) {
                // fix the seconds number to take the leap into account
                time = new ChunkedTime(time.getHour(), time.getMinute(),
                                       time.getSecond() + utc.getLeap(this));
            }
        } catch (ClassCastException cce) {
            // ignored
        }

        // build the chunks
        return new ChunksPair(date, time);

    }

    /** Compare the instance with another date.
     * @param date other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     * is before, simultaneous, or after the specified date.
     */
    public int compareTo(final AbsoluteDate date) {
        final double delta = minus(date);
        if (delta < 0) {
            return -1;
        } else if (delta > 0) {
            return +1;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return this;
    }

    /** Check if the instance represent the same time as another instance.
     * @param date other date
     * @return true if the instance and the other date refer to the same instant
     */
    public boolean equals(final Object date) {
        if ((date != null) && (date instanceof AbsoluteDate)) {
            return minus((AbsoluteDate) date) == 0;
        }
        return false;
    }

    /** Get a hashcode for this date.
     * @return hashcode
     */
    public int hashCode() {
        final long l = Double.doubleToLongBits(minus(J2000_EPOCH));
        return (int) (l ^ (l >>> 32));
    }

    /** Get a String representation of the instant location in UTC time scale.
     * @return a string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     */
    public String toString() {
        try {
            return toString(UTCScale.getInstance());
        } catch (OrekitException oe) {
            throw new RuntimeException(oe);
        }
    }

    /** Get a String representation of the instant location.
     * @param timeScale time scale to use
     * @return a string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     */
    public String toString(final TimeScale timeScale) {
        return getChunks(timeScale).toString();
    }

}
