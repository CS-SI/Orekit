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
 * one instance is needed, the representations are made available by
 * calling the appropriate methods on this instance several times with
 * several different time scales as parameters. Two complementary views are
 * available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #AbsoluteDate(ChunkedDate, ChunkedTime, TimeScale)}, {@link
 *   #AbsoluteDate(Date, TimeScale)}, {@link #createGPSDate(int, double)},
 *   toString(){@link #toDate(TimeScale)}, {@link #toString(TimeScale)
 *   toString(timeScale)}, {@link #toString()}, and {@link #timeScalesOffset}.</p>
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
 * In addition to reference epochs, two other constants are defined for convenience:
 * {@link #PAST_INFINITY} and {@link #FUTURE_INFINITY}, which can be used either as
 * dummy dates when a date is not yet initialized, or for initialization of loops
 * searching for a min or max date.
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
        new AbsoluteDate(new ChunkedDate(-4712,  1,  1), ChunkedTime.H12, TTScale.getInstance());

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00. */
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH =
        new AbsoluteDate(new ChunkedDate(1858, 11, 17), ChunkedTime.H00, TTScale.getInstance());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00. */
    public static final AbsoluteDate FIFTIES_EPOCH =
        new AbsoluteDate(new ChunkedDate(1950,  1,  1), ChunkedTime.H00, TTScale.getInstance());

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 UTC. */
    // GPS epoch is 1980-01-06T00:00:00Z (i.e. UTC), TAI - UTC = +19s at this time,
    // we use a date in TAI here for safety reasons, to avoid calling
    // UTCScale.getInstance() which may throw an exception; such an exception would induce
    // many problems in this part of code which runs very early
    public static final AbsoluteDate GPS_EPOCH =
        new AbsoluteDate(new ChunkedDate(1980, 1, 6), new ChunkedTime(0, 0, 19), TAIScale.getInstance());

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC). */
    public static final AbsoluteDate J2000_EPOCH =
        new AbsoluteDate(new ChunkedDate(2000,  1,  1), ChunkedTime.H12, TTScale.getInstance());

    /** Java Reference epoch: 1970-01-01T00:00:00 TT. */
    public static final AbsoluteDate JAVA_EPOCH =
        new AbsoluteDate(new ChunkedDate(1970,  1,  1), ChunkedTime.H00, TTScale.getInstance());

    /** Dummy date at infinity in the past direction. */
    public static final AbsoluteDate PAST_INFINITY =
        new AbsoluteDate(AbsoluteDate.JAVA_EPOCH, Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction. */
    public static final AbsoluteDate FUTURE_INFINITY =
        new AbsoluteDate(AbsoluteDate.JAVA_EPOCH, Double.POSITIVE_INFINITY);

    /** Epoch slightly shifted from J2000: 2000-01-01T00:00:00 TAI. */
    private static final AbsoluteDate SHIFTED_J2000_EPOCH =
        new AbsoluteDate(new ChunkedDate(2000,  1,  1), ChunkedTime.H00, TAIScale.getInstance());

    /** Serializable UID. */
    private static final long serialVersionUID = -897823396684528566L;

    /** Reference epoch in milliseconds from 1970-01-01T00:00:00 TAI. */
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
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final ChunkedDate date, final ChunkedTime time,
                        final TimeScale timeScale) {
        // set the epoch at the start of the current minute
        final int j1970Day = date.getJ2000Day() + 10957;
        epoch  = 60000l * ((j1970Day * 24l + time.getHour()) * 60l + time.getMinute());
        offset = time.getSecond() + timeScale.offsetToTAI(epoch * 0.001 + time.getSecond());
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final Date location, final TimeScale timeScale) {
        epoch  = location.getTime();
        offset = timeScale.offsetToTAI(epoch * 0.001);
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
        return new AbsoluteDate(GPS_EPOCH,
                                weekNumber * 604800 + milliInWeek / 1000);
    }

    /** Compute the offset between two instant.
     * <p>The offset is the number of seconds physically elapsed
     * between the two instants.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instant (positive
     * if the instance is posterior to the argument)
     */
    public double minus(final AbsoluteDate instant) {
        return 0.001 * (epoch - instant.epoch) + (offset - instant.offset);
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
        final double taiTime = 0.001 * epoch + offset;
        return scale1.offsetFromTAI(taiTime) - scale2.offsetFromTAI(taiTime);
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
        double time = 0.001 * epoch + offset;
        time += timeScale.offsetFromTAI(time);
        return new Date(Math.round(time * 1000));
    }

    /** Split the instance into date/time chunks.
     * @param timeScale time scale to use
     * @return date/time chunks
     */
    public ChunksPair getChunks(final TimeScale timeScale) {

        // compute offset from 2000-01-01T00:00:00 in specified time scale
        final double offest = minus(SHIFTED_J2000_EPOCH) +
                              timeScale.offsetFromTAI(0.001 * epoch + offset);
        final int    day    = (int) Math.floor(offest / 86400.0);

        // build the chunks
        return new ChunksPair(new ChunkedDate(day),
                              new ChunkedTime(offest - 86400 * day));

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
            try {
                return minus((AbsoluteDate) date) == 0;
            } catch (ClassCastException cce) {
                // ignored
            }
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
