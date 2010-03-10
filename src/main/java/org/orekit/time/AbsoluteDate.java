/* Copyright 2002-2010 CS Communication & Systèmes
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
 * other words the different locations of an event with respect to two different
 * time scale (say {@link TAIScale TAI} and {@link UTCScale UTC} for example) are
 * simply different perspective related to a single object. Only one
 * <code>AbsoluteDate</code> instance is needed, both representations being available
 * from this single instance by specifying the time scales as parameter when calling
 * the ad-hoc methods.</p>
 *
 * <p>Since an instance is not bound to a specific time-scale, all methods related
 * to the location of the date within some time scale require to provide the time
 * scale as an argument. It is therefore possible to define a date in one time scale
 * and to use it in another one. An example of such use is to read a date from a file
 * in UTC and write it in another file in TAI. This can be done as follows:</p>
 * <pre>
 *   DateTimeComponents utcComponents = readNextDate();
 *   AbsoluteDate date = new AbsoluteDate(utcComponents, TimeScalesFactory.getUTC());
 *   writeNextDate(date.getComponents(TimeScalesFactory.getTAI()));
 * </pre>
 *
 * <p>Two complementary views are available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #AbsoluteDate(DateComponents, TimeComponents, TimeScale)}, {@link
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
 *   {@link #durationFrom(AbsoluteDate)}, {@link #compareTo(AbsoluteDate)}, {@link #equals(Object)}
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
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     */
    public static final AbsoluteDate JULIAN_EPOCH =
        new AbsoluteDate(DateComponents.JULIAN_EPOCH, TimeComponents.H12, TimeScalesFactory.getTT());

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00. */
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH =
        new AbsoluteDate(DateComponents.MODIFIED_JULIAN_EPOCH, TimeComponents.H00, TimeScalesFactory.getTT());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00. */
    public static final AbsoluteDate FIFTIES_EPOCH =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeComponents.H00, TimeScalesFactory.getTT());

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time. */
    public static final AbsoluteDate GPS_EPOCH =
        new AbsoluteDate(DateComponents.GPS_EPOCH, TimeComponents.H00, TimeScalesFactory.getGPS());

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC). */
    public static final AbsoluteDate J2000_EPOCH =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTT());

    /** Java Reference epoch: 1970-01-01T00:00:00 TT. */
    public static final AbsoluteDate JAVA_EPOCH =
        new AbsoluteDate(DateComponents.JAVA_EPOCH, TimeComponents.H00, TimeScalesFactory.getTT());

    /** Dummy date at infinity in the past direction. */
    public static final AbsoluteDate PAST_INFINITY = JAVA_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction. */
    public static final AbsoluteDate FUTURE_INFINITY = JAVA_EPOCH.shiftedBy(Double.POSITIVE_INFINITY);

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

    /** Build an instance from a location (parsed from a string) in a {@link TimeScale time scale}.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard,
     * the exact subset is explained in {@link DateTimeComponents#parseDateTime(String)},
     * {@link DateComponents#parseDate(String)} and {@link TimeComponents#parseTime(String)}.
     * </p>
     * @param location location in the time scale, must be in a supported format
     * @param timeScale time scale
     * @exception IllegalArgumentException if location string is not in a supported format
     */
    public AbsoluteDate(final String location, final TimeScale timeScale) {
        this(DateTimeComponents.parseDateTime(location), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final DateTimeComponents location, final TimeScale timeScale) {
        this(location.getDate(), location.getTime(), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final DateComponents date, final TimeComponents time,
                        final TimeScale timeScale) {
        // set the epoch at the start of the current minute
        final int j2000Day = date.getJ2000Day();
        epoch  = 60l * ((j2000Day * 24l + time.getHour()) * 60l + time.getMinute() - 720l);
        offset = time.getSecond() + timeScale.offsetToTAI(date, time);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
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
        this(new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param date date location in the time scale
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final DateComponents date, final TimeScale timeScale)
        throws IllegalArgumentException {
        this(date, TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
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
        this(new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final Date location, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH,
                             (int) (location.getTime() / 86400000l)),
             new TimeComponents(0.001 * (location.getTime() % 86400000l)),
             timeScale);
    }

    /** Build an instance from an elapsed duration since to another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the duration between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but a stop watch would have measured
     * an elapsed duration of 2 seconds between these two instances because a leap
     * second was introduced at the end of 2005 in this time scale.</p>
     * <p>This constructor is the reverse of the {@link #durationFrom(AbsoluteDate)}
     * method.</p>
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @see #durationFrom(AbsoluteDate)
     */
    public AbsoluteDate(final AbsoluteDate since, final double elapsedDuration) {
        epoch = since.epoch;
        this.offset = since.offset + elapsedDuration;
    }

    /** Build an instance from an apparent clock offset with respect to another
     * instant <em>in the perspective of a specific {@link TimeScale time scale}</em>.
     * <p>It is important to note that the apparent clock offset <em>is</em> the
     * difference between two readings on a time scale and <em>not</em> an elapsed
     * duration. As an example, the apparent clock offset between the two instants
     * leading to the readings 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the
     * {@link UTCScale UTC} time scale is 1 second, but the elapsed duration is 2
     * seconds because a leap second has been introduced at the end of 2005 in this
     * time scale.</p>
     * <p>This constructor is the reverse of the {@link #offsetFrom(AbsoluteDate,
     * TimeScale)} method.</p>
     * @param reference reference instant
     * @param apparentOffset apparent clock offset from the reference instant
     * (difference between two readings in the specified time scale)
     * @param timeScale time scale with respect to which the offset is defined
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     */
    public AbsoluteDate(final AbsoluteDate reference, final double apparentOffset,
                        final TimeScale timeScale) {
        this(new DateTimeComponents(reference.getComponents(timeScale), apparentOffset),
             timeScale);
    }

    /** Build an instance corresponding to a GPS date.
     * <p>GPS dates are provided as a week number starting at
     * {@link #GPS_EPOCH GPS epoch} and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since {@link #GPS_EPOCH GPS epoch}
     * @param milliInWeek number of milliseconds since week start
     * @return a new instant
     */
    public static AbsoluteDate createGPSDate(final int weekNumber,
                                             final double milliInWeek) {
        return GPS_EPOCH.shiftedBy(weekNumber * 604800.0 + milliInWeek / 1000.0);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new AbsoluteDate(this, dt)</code>.
     * </p>
     * @param dt time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.PVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.Attitude#shiftedBy(double)
     * @see org.orekit.orbits.Orbit#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */
    public AbsoluteDate shiftedBy(final double dt) {
        return new AbsoluteDate(this, dt);
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(AbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #AbsoluteDate(AbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, double)
     */
    public double durationFrom(final AbsoluteDate instant) {
        return (epoch - instant.epoch) + (offset - instant.offset);
    }

    /** Compute the apparent clock offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>The offset is the number of seconds counted in the given
     * time scale between the locations of the two instants, with
     * all time scale irregularities removed (i.e. considering all
     * days are exactly 86400 seconds long). This method will give
     * a result that may not have a physical meaning if the time scale
     * is irregular. For example since a leap second was introduced at
     * the end of 2005, the apparent offset between 2005-12-31T23:59:59
     * and 2006-01-01T00:00:00 is 1 second, but the physical duration
     * of the corresponding time interval as returned by the {@link
     * #durationFrom(AbsoluteDate)} method is 2 seconds.</p>
     * <p>This method is the reverse of the {@link #AbsoluteDate(AbsoluteDate,
     * double, TimeScale)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeScale time scale with respect to which the offset should
     * be computed
     * @return apparent clock offset in seconds between the two instants
     * (positive if the instance is posterior to the argument)
     * @see #durationFrom(AbsoluteDate)
     * @see #AbsoluteDate(AbsoluteDate, double, TimeScale)
     */
    public double offsetFrom(final AbsoluteDate instant, final TimeScale timeScale) {
        final double elapsedDuration = (epoch - instant.epoch) +
                                       (offset - instant.offset);
        final double startOffset     = timeScale.offsetFromTAI(instant);
        final double endOffset       = timeScale.offsetFromTAI(this);
        return  elapsedDuration - startOffset + endOffset;
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
    public double timeScalesOffset(final TimeScale scale1, final TimeScale scale2) {
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

    /** Split the instance into date/time components.
     * @param timeScale time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {

        // compute offset from 2000-01-01T00:00:00 in specified time scale
        final double offset2000 = epoch + offset + 43200 + timeScale.offsetFromTAI(this);
        final int    day        = (int) Math.floor(offset2000 / 86400.0);

        // extract calendar elements
        final DateComponents date = new DateComponents(DateComponents.J2000_EPOCH, day);
        TimeComponents time = new TimeComponents(offset2000 - 86400.0 * day);
        try {
            final UTCScale utc = (UTCScale) timeScale;
            if (utc.insideLeap(this)) {
                // fix the seconds number to take the leap into account
                time = new TimeComponents(time.getHour(), time.getMinute(),
                                       time.getSecond() + utc.getLeap(this));
            }
        } catch (ClassCastException cce) {
            // ignored
        }

        // build the components
        return new DateTimeComponents(date, time);

    }

    /** Compare the instance with another date.
     * @param date other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     * is before, simultaneous, or after the specified date.
     */
    public int compareTo(final AbsoluteDate date) {
        final double delta = durationFrom(date);
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

        if (date == this) {
            // first fast check
            return true;
        }

        if ((date != null) && (date instanceof AbsoluteDate)) {
            return durationFrom((AbsoluteDate) date) == 0;
        }

        return false;

    }

    /** Get a hashcode for this date.
     * @return hashcode
     */
    public int hashCode() {
        final long l = Double.doubleToLongBits(durationFrom(J2000_EPOCH));
        return (int) (l ^ (l >>> 32));
    }

    /** Get a String representation of the instant location in UTC time scale.
     * @return a string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     */
    public String toString() {
        try {
            return toString(TimeScalesFactory.getUTC());
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
        return getComponents(timeScale).toString();
    }

}
