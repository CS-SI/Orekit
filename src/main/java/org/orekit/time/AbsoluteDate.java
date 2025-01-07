/* Copyright 2002-2025 CS GROUP
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import java.util.concurrent.TimeUnit;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.utils.Constants;

/** This class represents a specific instant in time.

 * <p>Instances of this class are considered to be absolute in the sense
 * that each one represent the occurrence of some event and can be compared
 * to other instances or located in <em>any</em> {@link TimeScale time scale}. In
 * other words the different locations of an event with respect to two different
 * time scales (say {@link TAIScale TAI} and {@link UTCScale UTC} for example) are
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
 *   TimeScale)}, {@link #parseCCSDSCalendarSegmentedTimeCode(byte, byte[])},
 *   {@link #toDate(TimeScale)}, {@link #toString(TimeScale) toString(timeScale)},
 *   {@link #toString()}, and {@link #timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtually perfect stopwatch.
 *   The related methods are {@link #AbsoluteDate(AbsoluteDate, double)},
 *   {@link #parseCCSDSUnsegmentedTimeCode(byte, byte, byte[], AbsoluteDate)},
 *   {@link #parseCCSDSDaySegmentedTimeCode(byte, byte[], DateComponents)},
 *   {@link #durationFrom(AbsoluteDate)}, {@link #compareTo(TimeOffset)}, {@link #equals(Object)}
 *   and {@link #hashCode()}.</p>
 *   </li>
 * </ul>
 * <p>
 * A few reference epochs which are commonly used in space systems have been defined. These
 * epochs can be used as the basis for offset computation. The supported epochs are:
 * {@link #JULIAN_EPOCH}, {@link #MODIFIED_JULIAN_EPOCH}, {@link #FIFTIES_EPOCH},
 * {@link #CCSDS_EPOCH}, {@link #GALILEO_EPOCH}, {@link #GPS_EPOCH}, {@link #QZSS_EPOCH}
 * {@link #J2000_EPOCH}, {@link #JAVA_EPOCH}.
 * There are also two factory methods {@link #createJulianEpoch(double)}
 * and {@link #createBesselianEpoch(double)} that can be used to compute other reference
 * epochs like J1900.0 or B1950.0.
 * In addition to these reference epochs, two other constants are defined for convenience:
 * {@link #PAST_INFINITY} and {@link #FUTURE_INFINITY}, which can be used either as dummy
 * dates when a date is not yet initialized, or for initialization of loops searching for
 * a min or max date.
 * </p>
 * <p>
 * Instances of the <code>AbsoluteDate</code> class are guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see TimeScale
 * @see TimeStamped
 * @see ChronologicalComparator
 */
public class AbsoluteDate
    extends TimeOffset
    implements TimeStamped, TimeShiftable<AbsoluteDate>, Comparable<TimeOffset>, Serializable {

    /** Reference epoch for julian dates: -4712-01-01T12:00:00 Terrestrial Time.
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getJulianEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate JULIAN_EPOCH = DataContext.getDefault().getTimeScales().getJulianEpoch();

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getModifiedJulianEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH = DataContext.getDefault().getTimeScales().getModifiedJulianEpoch();

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getFiftiesEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate FIFTIES_EPOCH = DataContext.getDefault().getTimeScales().getFiftiesEpoch();

    /** Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4):
     * 1958-01-01T00:00:00 International Atomic Time (<em>not</em> UTC).
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getCcsdsEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate CCSDS_EPOCH = DataContext.getDefault().getTimeScales().getCcsdsEpoch();

    /** Reference epoch for Galileo System Time: 1999-08-22T00:00:00 GST.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGalileoEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GALILEO_EPOCH = DataContext.getDefault().getTimeScales().getGalileoEpoch();

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGpsEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GPS_EPOCH = DataContext.getDefault().getTimeScales().getGpsEpoch();

    /** Reference epoch for QZSS weeks: 1980-01-06T00:00:00 QZSS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getQzssEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate QZSS_EPOCH = DataContext.getDefault().getTimeScales().getQzssEpoch();

    /** Reference epoch for IRNSS weeks: 1999-08-22T00:00:00 IRNSS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getIrnssEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate IRNSS_EPOCH = DataContext.getDefault().getTimeScales().getIrnssEpoch();

    /** Reference epoch for BeiDou weeks: 2006-01-01T00:00:00 UTC.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getBeidouEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate BEIDOU_EPOCH = DataContext.getDefault().getTimeScales().getBeidouEpoch();

    /** Reference epoch for GLONASS four-year interval number: 1996-01-01T00:00:00 GLONASS time.
     * <p>By convention, TGLONASS = UTC + 3 hours.</p>
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGlonassEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GLONASS_EPOCH = DataContext.getDefault().getTimeScales().getGlonassEpoch();

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC).
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #createJulianEpoch(double)
     * @see #createBesselianEpoch(double)
     * @see TimeScales#getJ2000Epoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate J2000_EPOCH = DataContext.getDefault().getTimeScales().getJ2000Epoch();

    /** Java Reference epoch: 1970-01-01T00:00:00 Universal Time Coordinate.
     * <p>
     * Between 1968-02-01 and 1972-01-01, UTC-TAI = 4.213 170 0s + (MJD - 39 126) x 0.002 592s.
     * As on 1970-01-01 MJD = 40587, UTC-TAI = 8.000082s
     * </p>
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getJavaEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate JAVA_EPOCH = DataContext.getDefault().getTimeScales().getJavaEpoch();

    /**
     * An arbitrary finite date. Uses when a non-null date is needed but its value doesn't
     * matter.
     */
    public static final AbsoluteDate ARBITRARY_EPOCH = new AbsoluteDate(TimeOffset.ZERO);

    /** Dummy date at infinity in the past direction.
     * @see TimeScales#getPastInfinity()
     */
    public static final AbsoluteDate PAST_INFINITY = ARBITRARY_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction.
     * @see TimeScales#getFutureInfinity()
     */
    public static final AbsoluteDate FUTURE_INFINITY = ARBITRARY_EPOCH.shiftedBy(Double.POSITIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 20240711L;

    /** Create an instance with a default value ({@link #J2000_EPOCH}).
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #AbsoluteDate(DateTimeComponents, TimeScale)
     */
    @DefaultDataContext
    public AbsoluteDate() {
        super(J2000_EPOCH.getSeconds(), J2000_EPOCH.getAttoSeconds());
    }

    /** Build an instance from a location (parsed from a string) in a {@link TimeScale time scale}.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard,
     * the exact subset is explained in {@link DateTimeComponents#parseDateTime(String)},
     * {@link DateComponents#parseDate(String)} and {@link TimeComponents#parseTime(String)}.
     * </p>
     * <p>
     * As CCSDS ASCII calendar segmented time code is a trimmed down version of ISO-8601,
     * it is also supported by this constructor.
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
        // epoch is at 12:00 (close to J2000.0, but in TAI scale), hence the subtraction of 720 minutes
        super(new TimeOffset(60L * ((date.getJ2000Day() * 24L + time.getHour()) * 60L +
                              time.getMinute() - time.getMinutesFromUTC() - 720L),
                             0L),
              time.getSplitSecond(),
              timeScale.offsetToTAI(date, time));
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
        this(year, month, day, hour, minute, new TimeOffset(second), timeScale);
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
     * @since 13.0
     */
    public AbsoluteDate(final int year, final int month, final int day,
                        final int hour, final int minute, final TimeOffset second,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final Month month, final int day,
                        final int hour, final int minute, final double second,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(year, month, day, hour, minute, new TimeOffset(second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 13.0
     */
    public AbsoluteDate(final int year, final Month month, final int day,
                        final int hour, final int minute, final TimeOffset second,
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
     * <p>The hour is set to 00:00:00.000.</p>
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final Month month, final int day,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public AbsoluteDate(final Date location, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH, (int) (location.getTime() / 86400000L)),
             new TimeComponents(new TimeOffset(location.getTime() % 86400000L, TimeOffset.MILLISECOND)),
             timeScale);
    }

    /** Build an instance from an {@link Instant instant} in utc time scale.
     * @param instant instant in the time scale
     * @since 12.1
     */
    @DefaultDataContext
    public AbsoluteDate(final Instant instant) {
        this(instant, TimeScalesFactory.getUTC());
    }

    /** Build an instance from an {@link Instant instant} in the {@link UTCScale time scale}.
     * @param instant instant in the time scale
     * @param utcScale utc time scale
     * @since 12.1
     */
    public AbsoluteDate(final Instant instant, final UTCScale utcScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH, (int) (instant.getEpochSecond() / 86400L)),
             new TimeComponents(TimeOffset.SECOND.multiply(instant.getEpochSecond() % 86400L).
                                add(new TimeOffset(instant.getNano(), TimeUnit.NANOSECONDS))),
             utcScale);
    }

    /** Build an instance from an elapsed duration since another instant.
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
        this(since, new TimeOffset(elapsedDuration));
    }

    /** Build an instance from an elapsed duration since another instant.
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
     * @since 13.0
     */
    public AbsoluteDate(final AbsoluteDate since, final TimeOffset elapsedDuration) {
        super(since, elapsedDuration);
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the duration between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but a stop watch would have measured
     * an elapsed duration of 2 seconds between these two instances because a leap
     * second was introduced at the end of 2005 in this time scale.</p>
     * <p>This constructor is the reverse of the {@link #durationFrom(AbsoluteDate, TimeUnit)}
     * method.</p>
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @param timeUnit {@link TimeUnit} of the elapsedDuration
     * @see #durationFrom(AbsoluteDate, TimeUnit)
     * @since 12.1
     */
    public AbsoluteDate(final AbsoluteDate since, final long elapsedDuration, final TimeUnit timeUnit) {
        this(since, new TimeOffset(elapsedDuration, timeUnit));
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
    public AbsoluteDate(final AbsoluteDate reference, final double apparentOffset, final TimeScale timeScale) {
        this(reference, new TimeOffset(apparentOffset), timeScale);
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
     * @since 13.0
     */
    public AbsoluteDate(final AbsoluteDate reference, final TimeOffset apparentOffset, final TimeScale timeScale) {
        this(new DateTimeComponents(reference.getComponents(timeScale), apparentOffset),
             timeScale);
    }

    /** Build a date from an offset since a reference epoch.
     * @param offset offset since reference epoch 2000-01-01T12:00:00 TAI.
     * (beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT)
     * @since 13.0
     */
    public AbsoluteDate(final TimeOffset offset) {
        super(offset.getSeconds(), offset.getAttoSeconds());
    }

    /** Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard
     * (CCSDS 301.0-B-3 published in 2002) or if the extension of the preamble
     * field introduced in version 4 of the standard is not used, then the
     * {@code preambleField2} parameter can be set to 0.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context} if
     * the CCSDS epoch is used.
     *
     * @param preambleField1 first byte of the field specifying the format, often
     * not transmitted in data interfaces, as it is constant for a given data interface
     * @param preambleField2 second byte of the field specifying the format
     * (added in revision 4 of the CCSDS standard in 2010), often not transmitted in data
     * interfaces, as it is constant for a given data interface (value ignored if presence
     * not signaled in {@code preambleField1})
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #CCSDS_EPOCH CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @see #parseCCSDSUnsegmentedTimeCode(byte, byte, byte[], AbsoluteDate, AbsoluteDate)
     */
    @DefaultDataContext
    public static AbsoluteDate parseCCSDSUnsegmentedTimeCode(final byte preambleField1,
                                                             final byte preambleField2,
                                                             final byte[] timeField,
                                                             final AbsoluteDate agencyDefinedEpoch) {
        return parseCCSDSUnsegmentedTimeCode(preambleField1, preambleField2, timeField,
                                             agencyDefinedEpoch,
                                             DataContext.getDefault().getTimeScales().getCcsdsEpoch());
    }

    /**
     * Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book: CCSDS Time Code Format
     * (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard (CCSDS
     * 301.0-B-3 published in 2002) or if the extension of the preamble field introduced
     * in version 4 of the standard is not used, then the {@code preambleField2} parameter
     * can be set to 0.
     * </p>
     *
     * @param preambleField1     first byte of the field specifying the format, often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface
     * @param preambleField2     second byte of the field specifying the format (added in
     *                           revision 4 of the CCSDS standard in 2010), often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface (value ignored if presence not
     *                           signaled in {@code preambleField1})
     * @param timeField          byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field specifies
     *                           the {@link DateComponents#CCSDS_EPOCH CCSDS reference epoch} is used
     *                           (and hence may be null in this case, but then {@code ccsdsEpoch} must be non-null)
     * @param ccsdsEpoch         reference epoch, ignored if the preamble field specifies
     *                           the agency epoch is used (and hence may be null in this case,
     *                           but then {@code agencyDefinedEpoch} must be non-null).
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static AbsoluteDate parseCCSDSUnsegmentedTimeCode(final byte preambleField1,
                                                             final byte preambleField2,
                                                             final byte[] timeField,
                                                             final AbsoluteDate agencyDefinedEpoch,
                                                             final AbsoluteDate ccsdsEpoch) {
        final CcsdsUnsegmentedTimeCode<AbsoluteDate> timeCode =
            new CcsdsUnsegmentedTimeCode<>(preambleField1, preambleField2, timeField, agencyDefinedEpoch, ccsdsEpoch);
        return timeCode.getEpoch().shiftedBy(timeCode.getTime());

    }

    /** Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #CCSDS_EPOCH CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @see #parseCCSDSDaySegmentedTimeCode(byte, byte[], DateComponents, TimeScale)
     */
    @DefaultDataContext
    public static AbsoluteDate parseCCSDSDaySegmentedTimeCode(final byte preambleField, final byte[] timeField,
                                                              final DateComponents agencyDefinedEpoch) {
        return parseCCSDSDaySegmentedTimeCode(preambleField, timeField,
                                              agencyDefinedEpoch, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #CCSDS_EPOCH CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @param utc      time scale used to compute date and time components.
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static AbsoluteDate parseCCSDSDaySegmentedTimeCode(final byte preambleField,
                                                              final byte[] timeField,
                                                              final DateComponents agencyDefinedEpoch,
                                                              final TimeScale utc) {

        final CcsdsSegmentedTimeCode timeCode = new CcsdsSegmentedTimeCode(preambleField, timeField,
                                                                           agencyDefinedEpoch);
        return new AbsoluteDate(timeCode.getDate(), timeCode.getTime(), utc);
    }

    /** Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @return an instance corresponding to the specified date
     * @see #parseCCSDSCalendarSegmentedTimeCode(byte, byte[], TimeScale)
     */
    @DefaultDataContext
    public static AbsoluteDate parseCCSDSCalendarSegmentedTimeCode(final byte preambleField, final byte[] timeField) {
        return parseCCSDSCalendarSegmentedTimeCode(preambleField, timeField,
                                                   DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param utc      time scale used to compute date and time components.
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static AbsoluteDate parseCCSDSCalendarSegmentedTimeCode(final byte preambleField,
                                                                   final byte[] timeField,
                                                                   final TimeScale utc) {
        final CcsdsSegmentedTimeCode timeCode = new CcsdsSegmentedTimeCode(preambleField, timeField);
        return new AbsoluteDate(timeCode.getDate(), timeCode.getTime(), utc);
    }

    /** Build an instance corresponding to a Julian Day date.
     * @param jd Julian day
     * @param secondsSinceNoon seconds in the Julian day
     * (BEWARE, Julian days start at noon, so 0.0 is noon)
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     */
    public static AbsoluteDate createJDDate(final int jd, final double secondsSinceNoon,
                                            final TimeScale timeScale) {
        return new AbsoluteDate(new DateComponents(DateComponents.JULIAN_EPOCH, jd),
                TimeComponents.H12, timeScale).shiftedBy(secondsSinceNoon);
    }

    /** Build an instance corresponding to a Julian Day date.
     * <p>
     * This function should be preferred to {@link #createMJDDate(int, double, TimeScale)} when the target time scale
     * has a non-constant offset with respect to TAI.
     * </p>
     * <p>
     * The idea is to introduce a pivot time scale that is close to the target time scale but has a constant bias with TAI.
     * </p>
     * <p>
     * For example, to get a date from an MJD in TDB time scale, it's advised to use the TT time scale
     * as a pivot scale. TT is very close to TDB and has constant offset to TAI.
     * </p>
     * @param jd Julian day
     * @param secondsSinceNoon seconds in the Julian day
     * (BEWARE, Julian days start at noon, so 0.0 is noon)
     * @param timeScale timescale in which the seconds in day are defined
     * @param pivotTimeScale pivot timescale used as intermediate timescale
     * @return a new instant
     */
    public static AbsoluteDate createJDDate(final int jd, final double secondsSinceNoon,
                                            final TimeScale timeScale,
                                            final TimeScale pivotTimeScale) {
        // Get the date in pivot timescale
        final AbsoluteDate dateInPivotTimeScale = createJDDate(jd, secondsSinceNoon, pivotTimeScale);

        // Compare offsets to TAI of the two time scales
        final TimeOffset offsetFromTAI = timeScale.
                                        offsetFromTAI(dateInPivotTimeScale).
                                        subtract(pivotTimeScale.offsetFromTAI(dateInPivotTimeScale));

        // Return date in desired timescale
        return new AbsoluteDate(dateInPivotTimeScale, offsetFromTAI.negate());
    }

    /** Build an instance corresponding to a Modified Julian Day date.
     * @param mjd modified Julian day
     * @param secondsInDay seconds in the day
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @exception OrekitIllegalArgumentException if seconds number is out of range
     */
    public static AbsoluteDate createMJDDate(final int mjd, final double secondsInDay,
                                             final TimeScale timeScale)
        throws OrekitIllegalArgumentException {
        return createMJDDate(mjd, new TimeOffset(secondsInDay), timeScale);
    }

    /** Build an instance corresponding to a Modified Julian Day date.
     * @param mjd modified Julian day
     * @param secondsInDay seconds in the day
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @exception OrekitIllegalArgumentException if seconds number is out of range
     * @since 13.0
     */
    public static AbsoluteDate createMJDDate(final int mjd, final TimeOffset secondsInDay,
                                             final TimeScale timeScale)
        throws OrekitIllegalArgumentException {
        final DateComponents dc = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd);
        final TimeComponents tc;
        if (secondsInDay.compareTo(TimeOffset.DAY) >= 0) {
            // check we are really allowed to use this number of seconds
            final TimeOffset secondsA = new TimeOffset(86399); // 23:59:59, i.e. 59s in the last minute of the day
            final TimeOffset secondsB = secondsInDay.subtract(secondsA);
            final TimeComponents safeTC = new TimeComponents(secondsA);
            final AbsoluteDate safeDate = new AbsoluteDate(dc, safeTC, timeScale);
            if (timeScale.minuteDuration(safeDate) > 59 + secondsB.toDouble()) {
                // we are within the last minute of the day, the number of seconds is OK
                return safeDate.shiftedBy(secondsB);
            } else {
                // let TimeComponents trigger an OrekitIllegalArgumentException
                // for the wrong number of seconds
                tc = new TimeComponents(secondsA.add(secondsB));
            }
        } else {
            tc = new TimeComponents(secondsInDay);
        }

        // create the date
        return new AbsoluteDate(dc, tc, timeScale);

    }

    /** Build an instance corresponding to a Julian Epoch (JE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Julian Epoch is related to Julian Ephemeris Date as:</p>
     * <pre>
     * JE = 2000.0 + (JED - 2451545.0) / 365.25
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code AbsoluteDate} from the Julian Epoch.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.</p>
     *
     * @param julianEpoch Julian epoch, like 2000.0 for defining the classical reference J2000.0
     * @return a new instant
     * @see #J2000_EPOCH
     * @see #createBesselianEpoch(double)
     * @see TimeScales#createJulianEpoch(double)
     */
    @DefaultDataContext
    public static AbsoluteDate createJulianEpoch(final double julianEpoch) {
        return DataContext.getDefault().getTimeScales().createJulianEpoch(julianEpoch);
    }

    /** Build an instance corresponding to a Besselian Epoch (BE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Besselian Epoch is related to Julian Ephemeris Date as:</p>
     * <pre>
     * BE = 1900.0 + (JED - 2415020.31352) / 365.242198781
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code AbsoluteDate} from the Besselian Epoch.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.</p>
     *
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical reference B1950.0
     * @return a new instant
     * @see #createJulianEpoch(double)
     * @see TimeScales#createBesselianEpoch(double)
     */
    @DefaultDataContext
    public static AbsoluteDate createBesselianEpoch(final double besselianEpoch) {
        return DataContext.getDefault().getTimeScales().createBesselianEpoch(besselianEpoch);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate shiftedBy(final double dt) {
        return new AbsoluteDate(this, dt);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate shiftedBy(final TimeOffset dt) {
        return new AbsoluteDate(this, dt);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new AbsoluteDate(this, shift, timeUnit)</code>.
     * </p>
     * @param dt time shift in time units
     * @param timeUnit {@link TimeUnit} of the shift
     * @return a new date, shifted with respect to instance (which is immutable)
     * @since 12.1
     */
    public AbsoluteDate shiftedBy(final long dt, final TimeUnit timeUnit) {
        return new AbsoluteDate(this, dt, timeUnit);
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
     * @see #accurateDurationFrom(AbsoluteDate)
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, double)
     */
    public double durationFrom(final AbsoluteDate instant) {
        return accurateDurationFrom(instant).toDouble();
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
     * @see #durationFrom(AbsoluteDate)
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, double)
     * @since 13.0
     */
    public TimeOffset accurateDurationFrom(final AbsoluteDate instant) {
        return this.subtract(instant);
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the duration physically
     * elapsed between the two instants, using the given time unit and rounded to the nearest integer, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method is the reverse of the {@link #AbsoluteDate(AbsoluteDate,
     * long, TimeUnit)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeUnit {@link TimeUnit} precision for the offset
     * @return offset in the given timeunit between the two instants (positive
     * if the instance is posterior to the argument), rounded to the nearest integer {@link TimeUnit}
     * @see #AbsoluteDate(AbsoluteDate, long, TimeUnit)
     * @since 12.1
     */
    public long durationFrom(final AbsoluteDate instant, final TimeUnit timeUnit) {
        return accurateDurationFrom(instant).getRoundedTime(timeUnit);
    }

    /** Compute the apparent <em>clock</em> offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>The offset is the number of seconds counted in the given
     * time scale between the locations of the two instants, with
     * all time scale irregularities removed (i.e. considering all
     * days are exactly 86400 seconds long). This method will give
     * a result that may not have a physical meaning if the time scale
     * is irregular. For example since a leap second was introduced at
     * the end of 2005, the apparent clock offset between 2005-12-31T23:59:59
     * and 2006-01-01T00:00:00 is 1 second and is the value this method
     * will return. On the other hand, the physical duration
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
     * @see #accurateOffsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, TimeOffset, TimeScale)
     */
    public double offsetFrom(final AbsoluteDate instant, final TimeScale timeScale) {
        return accurateOffsetFrom(instant, timeScale).toDouble();
    }

    /** Compute the apparent <em>clock</em> offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>The offset is the number of seconds counted in the given
     * time scale between the locations of the two instants, with
     * all time scale irregularities removed (i.e. considering all
     * days are exactly 86400 seconds long). This method will give
     * a result that may not have a physical meaning if the time scale
     * is irregular. For example since a leap second was introduced at
     * the end of 2005, the apparent clock offset between 2005-12-31T23:59:59
     * and 2006-01-01T00:00:00 is 1 second and is the value this method
     * will return. On the other hand, the physical duration
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
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, TimeOffset, TimeScale)
     * @since 13.0
     */
    public TimeOffset accurateOffsetFrom(final AbsoluteDate instant, final TimeScale timeScale) {
        return new TimeOffset(this,
                              timeScale.offsetFromTAI(this),
                              instant.negate(),
                              timeScale.offsetFromTAI(instant).negate());
    }

    /** Compute the offset between two time scales at the current instant.
     * <p>The offset is defined as <i>l₁-l₂</i>
     * where <i>l₁</i> is the location of the instant in
     * the <code>scale1</code> time scale and <i>l₂</i> is the
     * location of the instant in the <code>scale2</code> time scale.</p>
     * @param scale1 first time scale
     * @param scale2 second time scale
     * @return offset in seconds between the two time scales at the
     * current instant
     */
    public double timeScalesOffset(final TimeScale scale1, final TimeScale scale2) {
        return scale1.offsetFromTAI(this).subtract(scale2.offsetFromTAI(this)).toDouble();
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
        final TimeOffset time = add(timeScale.offsetFromTAI(this));
        return new Date(FastMath.round((time.toDouble() + 10957.5 * Constants.JULIAN_DAY) * 1000));
    }

    /**
     * Convert the instance to a Java {@link java.time.Instant Instant}.
     * Nanosecond precision is preserved during this conversion
     *
     * @return a {@link java.time.Instant Instant} instance representing the location
     * of the instant in the utc time scale
     * @since 12.1
     */
    @DefaultDataContext
    public Instant toInstant() {
        return toInstant(TimeScalesFactory.getTimeScales());
    }

    /**
     * Convert the instance to a Java {@link java.time.Instant Instant}.
     * Nanosecond precision is preserved during this conversion
     *
     * @param timeScales the timescales to use
     * @return a {@link java.time.Instant Instant} instance representing the location
     * of the instant in the utc time scale
     * @since 12.1
     */
    public Instant toInstant(final TimeScales timeScales) {
        final UTCScale utc = timeScales.getUTC();
        final String stringWithoutUtcOffset = toStringWithoutUtcOffset(utc, 9);

        final LocalDateTime localDateTime = LocalDateTime.parse(stringWithoutUtcOffset, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    /** Split the instance into date/time components.
     * @param timeScale time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {

        if (!isFinite()) {
            // special handling for NaN, past and future infinity
            if (isNaN()) {
                return new DateTimeComponents(DateComponents.J2000_EPOCH, TimeComponents.NaN);
            } else if (isNegativeInfinity()) {
                return new DateTimeComponents(DateComponents.MIN_EPOCH, TimeComponents.H00);
            } else {
                // the fact we truncate at 59.999 seconds is for compatibility reasons
                // with pre-13.0 Orekit versions. Indeed, this date is fake and more than
                // 5 millions years in the future, so milliseconds are not really relevant
                // truncating makes cleaner text output
                return new DateTimeComponents(DateComponents.MAX_EPOCH,
                                              new TimeComponents(23, 59,
                                                                 new TimeOffset(59, TimeOffset.SECOND,
                                                                                999, TimeOffset.MILLISECOND)));
            }
        }

        final TimeOffset sum = add(timeScale.offsetFromTAI(this));

        // split date and time
        final long offset2000A = sum.getSeconds() + 43200L;
        long time = offset2000A % 86400L;
        if (time < 0L) {
            time += 86400L;
        }
        final int date = (int) ((offset2000A - time) / 86400L);

        // extract calendar elements
        final DateComponents dateComponents = new DateComponents(DateComponents.J2000_EPOCH, date);

        // extract time element, accounting for leap seconds
        final TimeOffset leap = timeScale.insideLeap(this) ? timeScale.getLeap(this) : TimeOffset.ZERO;
        final int minuteDuration = timeScale.minuteDuration(this);
        final TimeComponents timeComponents = new TimeComponents(new TimeOffset(time, sum.getAttoSeconds()),
                                                                 leap, minuteDuration);

        // build the components
        return new DateTimeComponents(dateComponents, timeComponents);

    }

    /** Split the instance into date/time components for a local time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC)
     * @return date/time components
     * @since 7.2
     * @see #getComponents(int, TimeScale)
     */
    @DefaultDataContext
    public DateTimeComponents getComponents(final int minutesFromUTC) {
        return getComponents(minutesFromUTC,
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Split the instance into date/time components for a local time.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     *                       negative Westward UTC)
     * @param utc            time scale used to compute date and time components.
     * @return date/time components
     * @since 10.1
     */
    public DateTimeComponents getComponents(final int minutesFromUTC,
                                            final TimeScale utc) {

        final DateTimeComponents utcComponents = getComponents(utc);

        // shift the date according to UTC offset, but WITHOUT touching the seconds,
        // as they may exceed 60.0 during a leap seconds introduction,
        // and we want to preserve these special cases
        final TimeOffset seconds = utcComponents.getTime().getSplitSecond();

        int minute = utcComponents.getTime().getMinute() + minutesFromUTC;
        final int hourShift;
        if (minute < 0) {
            hourShift = (minute - 59) / 60;
        } else if (minute > 59) {
            hourShift = minute / 60;
        } else {
            hourShift = 0;
        }
        minute -= 60 * hourShift;

        int hour = utcComponents.getTime().getHour() + hourShift;
        final int dayShift;
        if (hour < 0) {
            dayShift = (hour - 23) / 24;
        } else if (hour > 23) {
            dayShift = hour / 24;
        } else {
            dayShift = 0;
        }
        hour -= 24 * dayShift;

        return new DateTimeComponents(new DateComponents(utcComponents.getDate(), dayShift),
                                      new TimeComponents(hour, minute, seconds, minutesFromUTC));

    }

    /** Split the instance into date/time components for a time zone.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param timeZone time zone
     * @return date/time components
     * @since 7.2
     * @see #getComponents(TimeZone, TimeScale)
     */
    @DefaultDataContext
    public DateTimeComponents getComponents(final TimeZone timeZone) {
        return getComponents(timeZone, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Split the instance into date/time components for a time zone.
     *
     * @param timeZone time zone
     * @param utc      time scale used to computed date and time components.
     * @return date/time components
     * @since 10.1
     */
    public DateTimeComponents getComponents(final TimeZone timeZone,
                                            final TimeScale utc) {
        final AbsoluteDate javaEpoch = new AbsoluteDate(DateComponents.JAVA_EPOCH, utc);
        final long milliseconds = FastMath.round(1000 * offsetFrom(javaEpoch, utc));
        return getComponents(timeZone.getOffset(milliseconds) / 60000, utc);
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return this;
    }

    /** Check if the instance represents the same time as another.
     * @param other the instant to compare this date to
     * @return true if the instance and the argument refer to the same instant
     * @see #isCloseTo(TimeStamped, double)
     * @since 10.1
     */
    public boolean isEqualTo(final TimeStamped other) {
        return this.equals(other.getDate());
    }

    /** Check if the instance time is close to another.
     * @param other the instant to compare this date to
     * @param tolerance the separation, in seconds, under which the two instants will be considered close to each other
     * @return true if the duration between the instance and the argument is strictly below the tolerance
     * @see #isEqualTo(TimeStamped)
     * @since 10.1
     */
    public boolean isCloseTo(final TimeStamped other, final double tolerance) {
        return FastMath.abs(this.durationFrom(other.getDate())) < tolerance;
    }

    /** Check if the instance represents a time that is strictly before another.
     * @param other the instant to compare this date to
     * @return true if the instance is strictly before the argument when ordering chronologically
     * @see #isBeforeOrEqualTo(TimeStamped)
     * @since 10.1
     */
    public boolean isBefore(final TimeStamped other) {
        return this.compareTo(other.getDate()) < 0;
    }

    /** Check if the instance represents a time that is strictly after another.
     * @param other the instant to compare this date to
     * @return true if the instance is strictly after the argument when ordering chronologically
     * @see #isAfterOrEqualTo(TimeStamped)
     * @since 10.1
     */
    public boolean isAfter(final TimeStamped other) {
        return this.compareTo(other.getDate()) > 0;
    }

    /** Check if the instance represents a time that is before or equal to another.
     * @param other the instant to compare this date to
     * @return true if the instance is before (or equal to) the argument when ordering chronologically
     * @see #isBefore(TimeStamped)
     * @since 10.1
     */
    public boolean isBeforeOrEqualTo(final TimeStamped other) {
        return this.isEqualTo(other) || this.isBefore(other);
    }

    /** Check if the instance represents a time that is after or equal to another.
     * @param other the instant to compare this date to
     * @return true if the instance is after (or equal to) the argument when ordering chronologically
     * @see #isAfterOrEqualTo(TimeStamped)
     * @since 10.1
     */
    public boolean isAfterOrEqualTo(final TimeStamped other) {
        return this.isEqualTo(other) || this.isAfter(other);
    }

    /** Check if the instance represents a time that is strictly between two others representing
     * the boundaries of a time span. The two boundaries can be provided in any order: in other words,
     * whether <code>boundary</code> represents a time that is before or after <code>otherBoundary</code> will
     * not change the result of this method.
     * @param boundary one end of the time span
     * @param otherBoundary the other end of the time span
     * @return true if the instance is strictly between the two arguments when ordering chronologically
     * @see #isBetweenOrEqualTo(TimeStamped, TimeStamped)
     * @since 10.1
     */
    public boolean isBetween(final TimeStamped boundary, final TimeStamped otherBoundary) {
        final TimeStamped beginning;
        final TimeStamped end;
        if (boundary.getDate().isBefore(otherBoundary)) {
            beginning = boundary;
            end = otherBoundary;
        } else {
            beginning = otherBoundary;
            end = boundary;
        }
        return this.isAfter(beginning) && this.isBefore(end);
    }

    /** Check if the instance represents a time that is between two others representing
     * the boundaries of a time span, or equal to one of them. The two boundaries can be provided in any order:
     * in other words, whether <code>boundary</code> represents a time that is before or after
     * <code>otherBoundary</code> will not change the result of this method.
     * @param boundary one end of the time span
     * @param otherBoundary the other end of the time span
     * @return true if the instance is between the two arguments (or equal to at least one of them)
     * when ordering chronologically
     * @see #isBetween(TimeStamped, TimeStamped)
     * @since 10.1
     */
    public boolean isBetweenOrEqualTo(final TimeStamped boundary, final TimeStamped otherBoundary) {
        return this.isEqualTo(boundary) || this.isEqualTo(otherBoundary) || this.isBetween(boundary, otherBoundary);
    }

    /**
     * Get a String representation of the instant location with up to 18 digits of
     * precision for the seconds value.
     *
     * <p> Since this method is used in exception messages and error handling every
     * effort is made to return some representation of the instant. If UTC is available
     * from the default data context then it is used to format the string in UTC. If not
     * then TAI is used. Finally if the prior attempts fail this method falls back to
     * converting this class's internal representation to a string.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return a string representation of the instance, in ISO-8601 format if UTC is
     * available from the default data context.
     * @see #toString(TimeScale)
     * @see #toStringRfc3339(TimeScale)
     * @see DateTimeComponents#toString(int, int)
     */
    @DefaultDataContext
    public String toString() {
        // CHECKSTYLE: stop IllegalCatch check
        try {
            // try to use UTC first at that is likely most familiar to the user.
            return toString(DataContext.getDefault().getTimeScales().getUTC()) + "Z";
        } catch (RuntimeException e1) {
            // catch OrekitException, OrekitIllegalStateException, etc.
            try {
                // UTC failed, try to use TAI
                return toString(new TAIScale()) + " TAI";
            } catch (RuntimeException e2) {
                // catch OrekitException, OrekitIllegalStateException, etc.
                // Likely failed to convert to ymdhms.
                // Give user some indication of what time it is.
                return "(" + this.getSeconds() + "s + " + this.getAttoSeconds() + "as) seconds past epoch";
            }
        }
        // CHECKSTYLE: resume IllegalCatch check
    }

    /**
     * Get a String representation of the instant location in ISO-8601 format without the
     * UTC offset and with up to 16 digits of precision for the seconds value.
     *
     * @param timeScale time scale to use
     * @return a string representation of the instance.
     * @see #toStringRfc3339(TimeScale)
     * @see DateTimeComponents#toString(int, int)
     */
    public String toString(final TimeScale timeScale) {
        return getComponents(timeScale).toStringWithoutUtcOffset();
    }

    /** Get a String representation of the instant location for a local time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC).
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @since 7.2
     * @see #toString(int, TimeScale)
     */
    @DefaultDataContext
    public String toString(final int minutesFromUTC) {
        return toString(minutesFromUTC,
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Get a String representation of the instant location for a local time.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     *                       negative Westward UTC).
     * @param utc            time scale used to compute date and time components.
     * @return string representation of the instance, in ISO-8601 format with milliseconds
     * accuracy
     * @since 10.1
     * @see #getComponents(int, TimeScale)
     * @see DateTimeComponents#toString(int, int)
     */
    public String toString(final int minutesFromUTC, final TimeScale utc) {
        final int minuteDuration = utc.minuteDuration(this);
        return getComponents(minutesFromUTC, utc).toString(minuteDuration);
    }

    /** Get a String representation of the instant location for a time zone.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param timeZone time zone
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @since 7.2
     * @see #toString(TimeZone, TimeScale)
     */
    @DefaultDataContext
    public String toString(final TimeZone timeZone) {
        return toString(timeZone, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Get a String representation of the instant location for a time zone.
     *
     * @param timeZone time zone
     * @param utc      time scale used to compute date and time components.
     * @return string representation of the instance, in ISO-8601 format with milliseconds
     * accuracy
     * @since 10.1
     * @see #getComponents(TimeZone, TimeScale)
     * @see DateTimeComponents#toString(int, int)
     */
    public String toString(final TimeZone timeZone, final TimeScale utc) {
        final int minuteDuration = utc.minuteDuration(this);
        return getComponents(timeZone, utc).toString(minuteDuration);
    }

    /**
     * Represent the given date as a string according to the format in RFC 3339. RFC3339
     * is a restricted subset of ISO 8601 with a well defined grammar. Enough digits are
     * included in the seconds value to avoid rounding up to the next minute.
     *
     * <p>This method is different than {@link AbsoluteDate#toString(TimeScale)} in that
     * it includes a {@code "Z"} at the end to indicate the time zone and enough precision
     * to represent the point in time without rounding up to the next minute.
     *
     * <p>RFC3339 is unable to represent BC years, years of 10000 or more, time zone
     * offsets of 100 hours or more, or NaN. In these cases the value returned from this
     * method will not be valid RFC3339 format.
     *
     * @param utc time scale.
     * @return RFC 3339 format string.
     * @see <a href="https://tools.ietf.org/html/rfc3339#page-8">RFC 3339</a>
     * @see DateTimeComponents#toStringRfc3339()
     * @see #toString(TimeScale)
     * @see #getComponents(TimeScale)
     */
    public String toStringRfc3339(final TimeScale utc) {
        return this.getComponents(utc).toStringRfc3339();
    }

    /**
     * Return a string representation of this date-time, rounded to the given precision.
     *
     * <p>The format used is ISO8601 without the UTC offset.</p>
     *
     * <p>Calling {@code toStringWithoutUtcOffset(DataContext.getDefault().getTimeScales().getUTC(),
     * 3)} will emulate the behavior of {@link #toString()} in Orekit 10 and earlier. Note
     * this method is more accurate as it correctly handles rounding during leap seconds.
     *
     * @param timeScale      to use to compute components.
     * @param fractionDigits the number of digits to include after the decimal point in
     *                       the string representation of the seconds. The date and time
     *                       is first rounded as necessary. {@code fractionDigits} must be
     *                       greater than or equal to {@code 0}.
     * @return string representation of this date, time, and UTC offset
     * @see #toString(TimeScale)
     * @see #toStringRfc3339(TimeScale)
     * @see DateTimeComponents#toString(int, int)
     * @see DateTimeComponents#toStringWithoutUtcOffset(int, int)
     * @since 11.1
     */
    public String toStringWithoutUtcOffset(final TimeScale timeScale,
                                           final int fractionDigits) {
        return this.getComponents(timeScale)
                .toStringWithoutUtcOffset(timeScale.minuteDuration(this), fractionDigits);
    }

    /**
     * Return the given date as a Modified Julian Date <b>expressed in UTC</b>.
     *
     * @return double representation of the given date as Modified Julian Date.
     *
     * @since 12.2
     */
    @DefaultDataContext
    public double getMJD() {
        return this.getJD() - DateComponents.JD_TO_MJD;
    }

    /**
     * Return the given date as a Modified Julian Date expressed in given timescale.
     *
     * @param ts time scale
     *
     * @return double representation of the given date as Modified Julian Date.
     *
     * @since 12.2
     */
    public double getMJD(final TimeScale ts) {
        return this.getJD(ts) - DateComponents.JD_TO_MJD;
    }

    /**
     * Return the given date as a Julian Date <b>expressed in UTC</b>.
     *
     * @return double representation of the given date as Julian Date.
     *
     * @since 12.2
     */
    @DefaultDataContext
    public double getJD() {
        return getJD(TimeScalesFactory.getUTC());
    }

    /**
     * Return the given date as a Julian Date expressed in given timescale.
     *
     * @param ts time scale
     *
     * @return double representation of the given date as Julian Date.
     *
     * @since 12.2
     */
    public double getJD(final TimeScale ts) {
        return this.getComponents(ts).offsetFrom(DateTimeComponents.JULIAN_EPOCH) / Constants.JULIAN_DAY;
    }

    /** Get day of year, preserving continuity as much as possible.
     * <p>
     * This is a continuous extension of the integer value returned by
     * {@link #getComponents(TimeZone) getComponents(utc)}{@link DateTimeComponents#getDate() .getDate()}{@link DateComponents#getDayOfYear() .getDayOfYear()}.
     * In order to have it remain as close as possible to its integer counterpart,
     * day 1.0 is considered to occur on January 1st at noon.
     * </p>
     * <p>
     * Continuity is preserved from day to day within a year, but of course
     * there is a discontinuity at year change, where it switches from 365.49999…
     * (or 366.49999… on leap years) to 0.5
     * </p>
     * @param utc time scale to compute date components
     * @return day of year, with day 1.0 occurring on January first at noon
     * @since 13.0
     */
    public double getDayOfYear(final TimeScale utc) {
        final int          year        = getComponents(utc).getDate().getYear();
        final AbsoluteDate newYearsEve = new AbsoluteDate(year - 1, 12, 31, 12, 0, 0.0, utc);
        return durationFrom(newYearsEve) / Constants.JULIAN_DAY;
    }

}
