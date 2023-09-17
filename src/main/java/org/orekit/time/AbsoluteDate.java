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
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.MathUtils.SumAndResidual;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
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
 *   {@link #durationFrom(AbsoluteDate)}, {@link #compareTo(AbsoluteDate)}, {@link #equals(Object)}
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
    implements TimeStamped, TimeShiftable<AbsoluteDate>, Comparable<AbsoluteDate>, Serializable {

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
    public static final AbsoluteDate JULIAN_EPOCH =
            DataContext.getDefault().getTimeScales().getJulianEpoch();

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getModifiedJulianEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH =
            DataContext.getDefault().getTimeScales().getModifiedJulianEpoch();

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getFiftiesEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate FIFTIES_EPOCH =
            DataContext.getDefault().getTimeScales().getFiftiesEpoch();

    /** Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4):
     * 1958-01-01T00:00:00 International Atomic Time (<em>not</em> UTC).
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getCcsdsEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate CCSDS_EPOCH =
            DataContext.getDefault().getTimeScales().getCcsdsEpoch();

    /** Reference epoch for Galileo System Time: 1999-08-22T00:00:00 GST.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGalileoEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GALILEO_EPOCH =
            DataContext.getDefault().getTimeScales().getGalileoEpoch();

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGpsEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GPS_EPOCH =
            DataContext.getDefault().getTimeScales().getGpsEpoch();

    /** Reference epoch for QZSS weeks: 1980-01-06T00:00:00 QZSS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getQzssEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate QZSS_EPOCH =
            DataContext.getDefault().getTimeScales().getQzssEpoch();

    /** Reference epoch for IRNSS weeks: 1999-08-22T00:00:00 IRNSS time.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getIrnssEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate IRNSS_EPOCH =
            DataContext.getDefault().getTimeScales().getIrnssEpoch();

    /** Reference epoch for BeiDou weeks: 2006-01-01T00:00:00 UTC.
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getBeidouEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate BEIDOU_EPOCH =
            DataContext.getDefault().getTimeScales().getBeidouEpoch();

    /** Reference epoch for GLONASS four-year interval number: 1996-01-01T00:00:00 GLONASS time.
     * <p>By convention, TGLONASS = UTC + 3 hours.</p>
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see TimeScales#getGlonassEpoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate GLONASS_EPOCH =
            DataContext.getDefault().getTimeScales().getGlonassEpoch();

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC).
     *
     * <p>This constant uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #createJulianEpoch(double)
     * @see #createBesselianEpoch(double)
     * @see TimeScales#getJ2000Epoch()
     */
    @DefaultDataContext
    public static final AbsoluteDate J2000_EPOCH = // TODO
            DataContext.getDefault().getTimeScales().getJ2000Epoch();

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
    public static final AbsoluteDate JAVA_EPOCH =
            DataContext.getDefault().getTimeScales().getJavaEpoch();

    /**
     * An arbitrary finite date. Uses when a non-null date is needed but its value doesn't
     * matter.
     */
    public static final AbsoluteDate ARBITRARY_EPOCH = new AbsoluteDate(0, 0);

    /** Dummy date at infinity in the past direction.
     * @see TimeScales#getPastInfinity()
     */
    public static final AbsoluteDate PAST_INFINITY = ARBITRARY_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction.
     * @see TimeScales#getFutureInfinity()
     */
    public static final AbsoluteDate FUTURE_INFINITY = ARBITRARY_EPOCH.shiftedBy(Double.POSITIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 617061803741806846L;

    /** Reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * <p>Beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT.</p> */
    private final long epoch;

    /** Offset from the reference epoch in seconds. */
    private final double offset;

    /** Create an instance with a default value ({@link #J2000_EPOCH}).
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #AbsoluteDate(DateTimeComponents, TimeScale)
     */
    @DefaultDataContext
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

        final double seconds  = time.getSecond();
        final double tsOffset = timeScale.offsetToTAI(date, time);

        // Use 2Sum for high precision.
        final SumAndResidual sumAndResidual = MathUtils.twoSum(seconds, tsOffset);
        final long dl = (long) FastMath.floor(sumAndResidual.getSum());
        final double regularOffset = (sumAndResidual.getSum() - dl) + sumAndResidual.getResidual();

        if (regularOffset >= 0) {
            // regular case, the offset is between 0.0 and 1.0
            offset = regularOffset;
            epoch  = 60l * ((date.getJ2000Day() * 24l + time.getHour()) * 60l +
                            time.getMinute() - time.getMinutesFromUTC() - 720l) + dl;
        } else {
            // very rare case, the offset is just before a whole second
            // we will loose some bits of accuracy when adding 1 second
            // but this will ensure the offset remains in the [0.0; 1.0] interval
            offset = 1.0 + regularOffset;
            epoch  = 60l * ((date.getJ2000Day() * 24l + time.getHour()) * 60l +
                            time.getMinute() - time.getMinutesFromUTC() - 720l) + dl - 1;
        }

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
        this(new DateComponents(DateComponents.JAVA_EPOCH,
                                (int) (location.getTime() / 86400000l)),
                                 millisToTimeComponents((int) (location.getTime() % 86400000l)),
             timeScale);
    }

    /** Build an instance from an {@link Instant instant} in a {@link TimeScale time scale}.
     * @param instant instant in the time scale
     * @param timeScale time scale
     * @since 12.0
     */
    public AbsoluteDate(final Instant instant, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH,
                                (int) (instant.getEpochSecond() / 86400l)),
             instantToTimeComponents(instant),
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
        // Use 2Sum for high precision.
        final SumAndResidual sumAndResidual = MathUtils.twoSum(since.offset, elapsedDuration);
        if (Double.isInfinite(sumAndResidual.getSum())) {
            offset = sumAndResidual.getSum();
            epoch  = (sumAndResidual.getSum() < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        } else {
            final long dl = (long) FastMath.floor(sumAndResidual.getSum());
            final double regularOffset = (sumAndResidual.getSum() - dl) + sumAndResidual.getResidual();
            if (regularOffset >= 0) {
                // regular case, the offset is between 0.0 and 1.0
                offset = regularOffset;
                epoch  = since.epoch + dl;
            } else {
                // very rare case, the offset is just before a whole second
                // we will loose some bits of accuracy when adding 1 second
                // but this will ensure the offset remains in the [0.0; 1.0] interval
                offset = 1.0 + regularOffset;
                epoch  = since.epoch + dl - 1;
            }
        }
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

    /** Build a date from its internal components.
     * <p>
     * This method is reserved for internal used (for example by {@link FieldAbsoluteDate}).
     * </p>
     * @param epoch reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * (beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT)
     * @param offset offset from the reference epoch in seconds (must be
     * between 0.0 included and 1.0 excluded)
     * @since 9.0
     */
    AbsoluteDate(final long epoch, final double offset) {
        this.epoch  = epoch;
        this.offset = offset;
    }

    /** Extract time components from a number of milliseconds within the day.
     * @param millisInDay number of milliseconds within the day
     * @return time components
     */
    private static TimeComponents millisToTimeComponents(final int millisInDay) {
        return new TimeComponents(millisInDay / 1000, 0.001 * (millisInDay % 1000));
    }

    /** Extract time components from an instant within the day.
     * @param instant instant to extract the number of seconds within the day
     * @return time components
     */
    private static TimeComponents instantToTimeComponents(final Instant instant) {
        final int secInDay = (int) (instant.getEpochSecond() % 86400l);
        return new TimeComponents(secInDay, 1.0e-9 * instant.getNano());
    }

    /** Get the reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * <p>
     * This method is reserved for internal used (for example by {@link FieldAbsoluteDate}).
     * </p>
     * <p>
     * Beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT.
     * </p>
     * @return reference epoch in seconds from 2000-01-01T12:00:00 TAI
     * @since 9.0
     */
    long getEpoch() {
        return epoch;
    }

    /** Get the offset from the reference epoch in seconds.
     * <p>
     * This method is reserved for internal used (for example by {@link FieldAbsoluteDate}).
     * </p>
     * @return offset from the reference epoch in seconds
     * @since 9.0
     */
    double getOffset() {
        return offset;
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
     *                           the {@link #CCSDS_EPOCH CCSDS reference epoch} is used
     *                           (and hence may be null in this case)
     * @param ccsdsEpoch         reference epoch, ignored if the preamble field specifies
     *                           the agency epoch is used.
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static AbsoluteDate parseCCSDSUnsegmentedTimeCode(
            final byte preambleField1,
            final byte preambleField2,
            final byte[] timeField,
            final AbsoluteDate agencyDefinedEpoch,
            final AbsoluteDate ccsdsEpoch) {

        // time code identification and reference epoch
        final AbsoluteDate epoch;
        switch (preambleField1 & 0x70) {
            case 0x10:
                // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
                epoch = ccsdsEpoch;
                break;
            case 0x20:
                // the reference epoch is agency defined
                if (agencyDefinedEpoch == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
                }
                epoch = agencyDefinedEpoch;
                break;
            default :
                throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                          formatByte(preambleField1));
        }

        // time field lengths
        int coarseTimeLength = 1 + ((preambleField1 & 0x0C) >>> 2);
        int fineTimeLength   = preambleField1 & 0x03;

        if ((preambleField1 & 0x80) != 0x0) {
            // there is an additional octet in preamble field
            coarseTimeLength += (preambleField2 & 0x60) >>> 5;
            fineTimeLength   += (preambleField2 & 0x1C) >>> 2;
        }

        if (timeField.length != coarseTimeLength + fineTimeLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, coarseTimeLength + fineTimeLength);
        }

        double seconds = 0;
        for (int i = 0; i < coarseTimeLength; ++i) {
            seconds = seconds * 256 + toUnsigned(timeField[i]);
        }
        double subseconds = 0;
        for (int i = timeField.length - 1; i >= coarseTimeLength; --i) {
            subseconds = (subseconds + toUnsigned(timeField[i])) / 256;
        }

        return new AbsoluteDate(epoch, seconds).shiftedBy(subseconds);

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
    public static AbsoluteDate parseCCSDSDaySegmentedTimeCode(
            final byte preambleField,
            final byte[] timeField,
            final DateComponents agencyDefinedEpoch,
            final TimeScale utc) {

        // time code identification
        if ((preambleField & 0xF0) != 0x40) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // reference epoch
        final DateComponents epoch;
        if ((preambleField & 0x08) == 0x00) {
            // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
            epoch = DateComponents.CCSDS_EPOCH;
        } else {
            // the reference epoch is agency defined
            if (agencyDefinedEpoch == null) {
                throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
            }
            epoch = agencyDefinedEpoch;
        }

        // time field lengths
        final int daySegmentLength = ((preambleField & 0x04) == 0x0) ? 2 : 3;
        final int subMillisecondLength = (preambleField & 0x03) << 1;
        if (subMillisecondLength == 6) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != daySegmentLength + 4 + subMillisecondLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, daySegmentLength + 4 + subMillisecondLength);
        }


        int i   = 0;
        int day = 0;
        while (i < daySegmentLength) {
            day = day * 256 + toUnsigned(timeField[i++]);
        }

        long milliInDay = 0l;
        while (i < daySegmentLength + 4) {
            milliInDay = milliInDay * 256 + toUnsigned(timeField[i++]);
        }
        final int milli   = (int) (milliInDay % 1000l);
        final int seconds = (int) ((milliInDay - milli) / 1000l);

        double subMilli = 0;
        double divisor  = 1;
        while (i < timeField.length) {
            subMilli = subMilli * 256 + toUnsigned(timeField[i++]);
            divisor *= 1000;
        }

        final DateComponents date = new DateComponents(epoch, day);
        final TimeComponents time = new TimeComponents(seconds);
        return new AbsoluteDate(date, time, utc).shiftedBy(milli * 1.0e-3 + subMilli / divisor);

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
    public static AbsoluteDate parseCCSDSCalendarSegmentedTimeCode(
            final byte preambleField,
            final byte[] timeField,
            final TimeScale utc) {

        // time code identification
        if ((preambleField & 0xF0) != 0x50) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // time field length
        final int length = 7 + (preambleField & 0x07);
        if (length == 14) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != length) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, length);
        }

        // date part in the first four bytes
        final DateComponents date;
        if ((preambleField & 0x08) == 0x00) {
            // month of year and day of month variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                      toUnsigned(timeField[2]),
                                      toUnsigned(timeField[3]));
        } else {
            // day of year variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                      toUnsigned(timeField[2]) * 256 + toUnsigned(timeField[3]));
        }

        // time part from bytes 5 to last (between 7 and 13 depending on precision)
        final TimeComponents time = new TimeComponents(toUnsigned(timeField[4]),
                                                       toUnsigned(timeField[5]),
                                                       toUnsigned(timeField[6]));
        double subSecond = 0;
        double divisor   = 1;
        for (int i = 7; i < length; ++i) {
            subSecond = subSecond * 100 + toUnsigned(timeField[i]);
            divisor *= 100;
        }

        return new AbsoluteDate(date, time, utc).shiftedBy(subSecond / divisor);

    }

    /** Decode a signed byte as an unsigned int value.
     * @param b byte to decode
     * @return an unsigned int value
     */
    private static int toUnsigned(final byte b) {
        final int i = (int) b;
        return (i < 0) ? 256 + i : i;
    }

    /** Format a byte as an hex string for error messages.
     * @param data byte to format
     * @return a formatted string
     */
    private static String formatByte(final byte data) {
        return "0x" + Integer.toHexString(data).toUpperCase();
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
        final DateComponents dc = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd);
        final TimeComponents tc;
        if (secondsInDay >= Constants.JULIAN_DAY) {
            // check we are really allowed to use this number of seconds
            final int    secondsA = 86399; // 23:59:59, i.e. 59s in the last minute of the day
            final double secondsB = secondsInDay - secondsA;
            final TimeComponents safeTC = new TimeComponents(secondsA, 0.0);
            final AbsoluteDate safeDate = new AbsoluteDate(dc, safeTC, timeScale);
            if (timeScale.minuteDuration(safeDate) > 59 + secondsB) {
                // we are within the last minute of the day, the number of seconds is OK
                return safeDate.shiftedBy(secondsB);
            } else {
                // let TimeComponents trigger an OrekitIllegalArgumentException
                // for the wrong number of seconds
                tc = new TimeComponents(secondsA, secondsB);
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
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
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
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical reference B1950.0
     * @return a new instant
     * @see #createJulianEpoch(double)
     * @see TimeScales#createBesselianEpoch(double)
     */
    @DefaultDataContext
    public static AbsoluteDate createBesselianEpoch(final double besselianEpoch) {
        return DataContext.getDefault().getTimeScales()
                .createBesselianEpoch(besselianEpoch);
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
        final long   elapsedDurationA = epoch - instant.epoch;
        final double elapsedDurationB = (offset         + timeScale.offsetFromTAI(this)) -
                                        (instant.offset + timeScale.offsetFromTAI(instant));
        return  elapsedDurationA + elapsedDurationB;
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
        final double time = epoch + (offset + timeScale.offsetFromTAI(this));
        return new Date(FastMath.round((time + 10957.5 * 86400.0) * 1000));
    }

    /** Split the instance into date/time components.
     * @param timeScale time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {

        if (Double.isInfinite(offset)) {
            // special handling for past and future infinity
            if (offset < 0) {
                return new DateTimeComponents(DateComponents.MIN_EPOCH, TimeComponents.H00);
            } else {
                return new DateTimeComponents(DateComponents.MAX_EPOCH,
                                              new TimeComponents(23, 59, 59.999));
            }
        }

        // Compute offset from 2000-01-01T00:00:00 in specified time scale.
        // Use 2Sum for high precision.
        final double taiOffset = timeScale.offsetFromTAI(this);
        final SumAndResidual sumAndResidual = MathUtils.twoSum(offset, taiOffset);

        // split date and time
        final long   carry = (long) FastMath.floor(sumAndResidual.getSum());
        double offset2000B = (sumAndResidual.getSum() - carry) + sumAndResidual.getResidual();
        long   offset2000A = epoch + carry + 43200l;
        if (offset2000B < 0) {
            offset2000A -= 1;
            offset2000B += 1;
        }
        long time = offset2000A % 86400l;
        if (time < 0l) {
            time += 86400l;
        }
        final int date = (int) ((offset2000A - time) / 86400l);

        // extract calendar elements
        final DateComponents dateComponents = new DateComponents(DateComponents.J2000_EPOCH, date);

        // extract time element, accounting for leap seconds
        final double leap = timeScale.insideLeap(this) ? timeScale.getLeap(this) : 0;
        final int minuteDuration = timeScale.minuteDuration(this);
        final TimeComponents timeComponents =
                TimeComponents.fromSeconds((int) time, offset2000B, leap, minuteDuration);

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
        final double seconds = utcComponents.getTime().getSecond();

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

    /** Compare the instance with another date.
     * @param date other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     * is before, simultaneous, or after the specified date.
     */
    public int compareTo(final AbsoluteDate date) {
        final double duration = durationFrom(date);
        if (!Double.isNaN(duration)) {
            return Double.compare(duration, 0.0);
        }
        // both dates are infinity or one is NaN or both are NaN
        return Double.compare(offset, date.offset);
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return this;
    }

    /** Check if the instance represents the same time as another instance.
     * @param date other date
     * @return true if the instance and the other date refer to the same instant
     */
    public boolean equals(final Object date) {

        if (date == this) {
            // first fast check
            return true;
        }

        if (date instanceof AbsoluteDate) {

            // Improve robustness against positive/negative infinity dates
            if ( this.offset == Double.NEGATIVE_INFINITY && ((AbsoluteDate) date).offset == Double.NEGATIVE_INFINITY ||
                    this.offset == Double.POSITIVE_INFINITY && ((AbsoluteDate) date).offset == Double.POSITIVE_INFINITY ) {
                return true;
            } else {
                return durationFrom((AbsoluteDate) date) == 0;
            }
        }

        return false;
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

    /** Get a hashcode for this date.
     * @return hashcode
     */
    public int hashCode() {
        final long l = Double.doubleToLongBits(durationFrom(ARBITRARY_EPOCH));
        return (int) (l ^ (l >>> 32));
    }

    /**
     * Get a String representation of the instant location with up to 16 digits of
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
                try {
                    return "(" + this.epoch + " + " + this.offset + ") seconds past epoch";
                } catch (RuntimeException e3) {
                    // give up and throw an exception
                    e2.addSuppressed(e3);
                    e1.addSuppressed(e2);
                    throw e1;
                }
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

}
