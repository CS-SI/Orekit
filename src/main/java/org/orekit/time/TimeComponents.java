/* Copyright 2002-2024 CS GROUP
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;


/** Class representing a time within the day broken up as hour,
 * minute and second components.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see DateComponents
 * @see DateTimeComponents
 * @author Luc Maisonobe
 */
public class TimeComponents implements Serializable, Comparable<TimeComponents> {

    /** Constant for commonly used hour 00:00:00. */
    public static final TimeComponents H00   = new TimeComponents(0, 0, TimeOffset.ZERO);

    /** Constant for commonly used hour 12:00:00. */
    public static final TimeComponents H12 = new TimeComponents(12, 0, TimeOffset.ZERO);

    // CHECKSTYLE: stop ConstantName
    /** Constant for NaN time.
     * @since 13.0
     */
    public static final TimeComponents NaN   = new TimeComponents(0, 0, TimeOffset.NaN);
    // CHECKSTYLE: resume ConstantName

    /** Wrapping limits for rounding to next minute.
     * @since 13.0
     */
    private static final TimeOffset[] WRAPPING = new TimeOffset[] {
        new TimeOffset(59L, 500000000000000000L), // round to second
        new TimeOffset(59L, 950000000000000000L), // round to 10⁻¹ second
        new TimeOffset(59L, 995000000000000000L), // round to 10⁻² second
        new TimeOffset(59L, 999500000000000000L), // round to 10⁻³ second
        new TimeOffset(59L, 999950000000000000L), // round to 10⁻⁴ second
        new TimeOffset(59L, 999995000000000000L), // round to 10⁻⁵ second
        new TimeOffset(59L, 999999500000000000L), // round to 10⁻⁶ second
        new TimeOffset(59L, 999999950000000000L), // round to 10⁻⁷ second
        new TimeOffset(59L, 999999995000000000L), // round to 10⁻⁸ second
        new TimeOffset(59L, 999999999500000000L), // round to 10⁻⁹ second
        new TimeOffset(59L, 999999999950000000L), // round to 10⁻¹⁰ second
        new TimeOffset(59L, 999999999995000000L), // round to 10⁻¹¹ second
        new TimeOffset(59L, 999999999999500000L), // round to 10⁻¹² second
        new TimeOffset(59L, 999999999999950000L), // round to 10⁻¹³ second
        new TimeOffset(59L, 999999999999995000L), // round to 10⁻¹⁴ second
        new TimeOffset(59L, 999999999999999500L), // round to 10⁻¹⁵ second
        new TimeOffset(59L, 999999999999999950L), // round to 10⁻¹⁶ second
        new TimeOffset(59L, 999999999999999995L)  // round to 10⁻¹⁷ second
    };

    /** Offset values for rounding attoseconds.
     * @since 13.0
     */
    // CHECKSTYLE: stop Indentation check */
    private static final long[] ROUNDING = new long[] {
        500000000000000000L, // round to second
         50000000000000000L, // round to 10⁻¹ second
          5000000000000000L, // round to 10⁻² second
           500000000000000L, // round to 10⁻³ second
            50000000000000L, // round to 10⁻⁴ second
             5000000000000L, // round to 10⁻⁵ second
              500000000000L, // round to 10⁻⁶ second
               50000000000L, // round to 10⁻⁷ second
                5000000000L, // round to 10⁻⁸ second
                 500000000L, // round to 10⁻⁹ second
                  50000000L, // round to 10⁻¹⁰ second
                   5000000L, // round to 10⁻¹¹ second
                    500000L, // round to 10⁻¹² second
                     50000L, // round to 10⁻¹³ second
                      5000L, // round to 10⁻¹⁴ second
                       500L, // round to 10⁻¹⁵ second
                        50L, // round to 10⁻¹⁶ second
                         5L, // round to 10⁻¹⁷ second
                         0L, // round to 10⁻¹⁸ second
    };
    // CHECKSTYLE: resume Indentation check */

    /** Serializable UID. */
    private static final long serialVersionUID = 20240712L;

    /** Basic and extends formats for local time, with optional timezone. */
    private static final Pattern ISO8601_FORMATS = Pattern.compile("^(\\d\\d):?(\\d\\d):?(\\d\\d(?:[.,]\\d+)?)?(?:Z|([-+]\\d\\d(?::?\\d\\d)?))?$");

    /** Number of seconds in one hour. */
    private static final int HOUR = 3600;

    /** Number of seconds in one minute. */
    private static final int MINUTE = 60;

    /** Constant for 23 hours. */
    private static final int TWENTY_THREE = 23;

    /** Constant for 59 minutes. */
    private static final int FIFTY_NINE = 59;

    /** Constant for 23:59. */
    private static final TimeOffset TWENTY_THREE_FIFTY_NINE =
        new TimeOffset(TWENTY_THREE * HOUR + FIFTY_NINE * MINUTE, 0L);

    /** Hour number. */
    private final int hour;

    /** Minute number. */
    private final int minute;

    /** Second number. */
    private final TimeOffset second;

    /** Offset between the specified date and UTC.
     * <p>
     * Always an integral number of minutes, as per ISO-8601 standard.
     * </p>
     * @since 7.2
     */
    private final int minutesFromUTC;

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
        this(hour, minute, new TimeOffset(second));
    }

    /** Build a time from its clock elements.
     * <p>Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed
     * in this method, since they do occur during leap seconds introduction
     * in the {@link UTCScale UTC} time scale.</p>
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 61.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 13.0
     */
    public TimeComponents(final int hour, final int minute, final TimeOffset second)
        throws IllegalArgumentException {
        this(hour, minute, second, 0);
    }

    /** Build a time from its clock elements.
     * <p>Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed
     * in this method, since they do occur during leap seconds introduction
     * in the {@link UTCScale UTC} time scale.</p>
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 61.0 (excluded)
     * @param minutesFromUTC offset between the specified date and UTC, as an
     * integral number of minutes, as per ISO-8601 standard
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 7.2
     */
    public TimeComponents(final int hour, final int minute, final double second, final int minutesFromUTC)
        throws IllegalArgumentException {
        this(hour, minute, new TimeOffset(second), minutesFromUTC);
    }

    /** Build a time from its clock elements.
     * <p>Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed
     * in this method, since they do occur during leap seconds introduction
     * in the {@link UTCScale UTC} time scale.</p>
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 62.0 (excluded, more than 61 s occurred on
     *               the 1961 leap second, which was between 1 and 2 seconds in duration)
     * @param minutesFromUTC offset between the specified date and UTC, as an
     * integral number of minutes, as per ISO-8601 standard
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 13.0
     */
    public TimeComponents(final int hour, final int minute, final TimeOffset second,
                          final int minutesFromUTC)
        throws IllegalArgumentException {

        // range check
        if (hour < 0 || hour > 23 ||
            minute < 0 || minute > 59 ||
            second.getSeconds() < 0L || second.getSeconds() >= 62L) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_HMS_TIME,
                                                     hour, minute, second.toDouble());
        }

        this.hour           = hour;
        this.minute         = minute;
        this.second         = second;
        this.minutesFromUTC = minutesFromUTC;

    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>If the {@code secondInDay} is less than {@code 60.0} then {@link #getSecond()}
     * and {@link #getSplitSecond()} will be less than {@code 60.0}, otherwise they will be
     * less than {@code 61.0}. This constructor may produce an invalid value of
     * {@link #getSecond()} and {@link #getSplitSecond()} during a negative leap second,
     * through there has never been one. For more control over the number of seconds in
     * the final minute use {@link #TimeComponents(TimeOffset, TimeOffset, int)}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * @param secondInDay second number from 0.0 to {@link Constants#JULIAN_DAY} {@code +
     *                    1} (excluded)
     * @throws OrekitIllegalArgumentException if seconds number is out of range
     * @see #TimeComponents(TimeOffset, TimeOffset, int)
     * @see #TimeComponents(int, double)
     */
    public TimeComponents(final double secondInDay)
            throws OrekitIllegalArgumentException {
        this(new TimeOffset(secondInDay));
    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>The second number is defined here as the sum
     * {@code secondInDayA + secondInDayB} from 0.0 to {@link Constants#JULIAN_DAY}
     * {@code + 1} (excluded). The two parameters are used for increased accuracy.
     *
     * <p>If the sum is less than {@code 60.0} then {@link #getSecond()} will be less
     * than {@code 60.0}, otherwise it will be less than {@code 61.0}. This constructor
     * may produce an invalid value of {@link #getSecond()} during a negative leap second,
     * through there has never been one. For more control over the number of seconds in
     * the final minute use {@link #TimeComponents(TimeOffset, TimeOffset, int)}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC()} will
     * return 0).
     *
     * @param secondInDayA first part of the second number
     * @param secondInDayB last part of the second number
     * @throws OrekitIllegalArgumentException if seconds number is out of range
     * @see #TimeComponents(TimeOffset, TimeOffset, int)
     */
    public TimeComponents(final int secondInDayA, final double secondInDayB)
            throws OrekitIllegalArgumentException {

        // if the total is at least 86400 then assume there is a leap second
        final TimeOffset aPlusB = new TimeOffset(secondInDayA).add(new TimeOffset(secondInDayB));
        final TimeComponents tc     = aPlusB.compareTo(TimeOffset.DAY) >= 0 ?
                                      new TimeComponents(aPlusB.subtract(TimeOffset.SECOND), TimeOffset.SECOND, 61) :
                                      new TimeComponents(aPlusB, TimeOffset.ZERO, 60);

        this.hour           = tc.hour;
        this.minute         = tc.minute;
        this.second         = tc.second;
        this.minutesFromUTC = tc.minutesFromUTC;

    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>If the {@code secondInDay} is less than {@code 60.0} then {@link #getSecond()}
     * will be less than {@code 60.0}, otherwise it will be less than {@code 61.0}. This constructor
     * may produce an invalid value of {@link #getSecond()} during a negative leap second,
     * through there has never been one. For more control over the number of seconds in
     * the final minute use {@link #TimeComponents(TimeOffset, TimeOffset, int)}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * @param splitSecondInDay second number from 0.0 to {@link Constants#JULIAN_DAY} {@code +
     *                    1} (excluded)
     * @see #TimeComponents(TimeOffset, TimeOffset, int)
     * @see #TimeComponents(int, double)
     * @since 13.0
     */
    public TimeComponents(final TimeOffset splitSecondInDay) {
        if (splitSecondInDay.compareTo(TimeOffset.ZERO) < 0) {
            // negative time
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                                                     splitSecondInDay.toDouble(),
                                                     0, TimeOffset.DAY_WITH_POSITIVE_LEAP.getSeconds());
        } else if (splitSecondInDay.compareTo(TimeOffset.DAY) >= 0) {
            // if the total is at least 86400 then assume there is a leap second
            if (splitSecondInDay.compareTo(TimeOffset.DAY_WITH_POSITIVE_LEAP) >= 0) {
                // more than one leap second is too much
                throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                                                         splitSecondInDay.toDouble(),
                                                         0, TimeOffset.DAY_WITH_POSITIVE_LEAP.getSeconds());
            } else {
                hour   = TWENTY_THREE;
                minute = FIFTY_NINE;
                second = splitSecondInDay.subtract(TWENTY_THREE_FIFTY_NINE);
            }
        } else {
            // regular time within day
            hour   = (int) splitSecondInDay.getSeconds() / HOUR;
            minute = ((int) splitSecondInDay.getSeconds() % HOUR) / MINUTE;
            second = splitSecondInDay.subtract(new TimeOffset(hour * HOUR + minute * MINUTE, 0L));
        }

        minutesFromUTC = 0;

    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>The seconds past midnight is the sum {@code secondInDay + leap}. Only the part
     * {@code secondInDay} is used to compute the hours and minutes. The second parameter
     * ({@code leap}) is added directly to the second value ({@link #getSecond()}) to
     * implement leap seconds. These two quantities must satisfy the following constraints.
     * This first guarantees the hour and minute are valid, the second guarantees the second
     * is valid.
     *
     * <pre>
     *     {@code 0 <= secondInDay < 86400}
     *     {@code 0 <= secondInDay % 60 + leap <= minuteDuration}
     *     {@code 0 <= leap <= minuteDuration - 60 if minuteDuration >= 60}
     *     {@code 0 >= leap >= minuteDuration - 60 if minuteDuration <  60}
     * </pre>
     *
     * <p>If the seconds of minute ({@link #getSecond()}) computed from {@code
     * secondInDay + leap} is greater than or equal to {@code 60 + leap}
     * then the second of minute will be set to {@code FastMath.nextDown(60 + leap)}. This
     * prevents rounding to an invalid seconds of minute number when the input values have
     * greater precision than a {@code double}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * <p>If {@code secondsInDay} or {@code leap} is NaN then the hour and minute will
     * be set arbitrarily and the second of minute will be NaN.
     *
     * @param secondInDay    part of the second number.
     * @param leap           magnitude of the leap second if this point in time is during
     *                       a leap second, otherwise {@code 0.0}. This value is not used
     *                       to compute hours and minutes, but it is added to the computed
     *                       second of minute.
     * @param minuteDuration number of seconds in the current minute, normally {@code 60}.
     * @throws OrekitIllegalArgumentException if the inequalities above do not hold.
     * @since 10.2
     */
    public TimeComponents(final TimeOffset secondInDay, final TimeOffset leap, final int minuteDuration) {

        minutesFromUTC = 0;

        if (secondInDay.isNaN()) {
            // special handling for NaN
            hour   = 0;
            minute = 0;
            second = secondInDay;
            return;
        }

        // range check
        if (secondInDay.compareTo(TimeOffset.ZERO) < 0 || secondInDay.compareTo(TimeOffset.DAY) >= 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                                                     // this can produce some strange messages due to rounding
                                                     secondInDay.toDouble(), 0, Constants.JULIAN_DAY);
        }
        final int maxExtraSeconds = minuteDuration - MINUTE;
        if (leap.getSeconds() * maxExtraSeconds < 0 || FastMath.abs(leap.getSeconds()) > FastMath.abs(maxExtraSeconds)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                                                     leap, 0, maxExtraSeconds);
        }

        // extract the time components
        int wholeSeconds = (int) secondInDay.getSeconds();
        hour           = wholeSeconds / HOUR;
        wholeSeconds  -= HOUR * hour;
        minute         = wholeSeconds / MINUTE;
        wholeSeconds  -= MINUTE * minute;
        // at this point ((minuteDuration - wholeSeconds) - leap) - fractional > 0
        // or else one of the preconditions was violated. Even if there is no violation,
        // naiveSecond may round to minuteDuration, creating an invalid time.
        // In that case round down to preserve a valid time at the cost of up to 1as of error.
        // See #676 and #681.
        final TimeOffset naiveSecond = new TimeOffset(wholeSeconds, secondInDay.getAttoSeconds()).add(leap);
        if (naiveSecond.compareTo(TimeOffset.ZERO) < 0) {
            throw new OrekitIllegalArgumentException(
                    OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                    naiveSecond, 0, minuteDuration);
        }
        if (naiveSecond.getSeconds() < minuteDuration) {
            second = naiveSecond;
        } else {
            second = new TimeOffset(minuteDuration - 1, 999999999999999999L);
        }

    }

    /** Parse a string in ISO-8601 format to build a time.
     * <p>The supported formats are:
     * <ul>
     *   <li>basic and extended format local time: hhmmss, hh:mm:ss (with optional decimals in seconds)</li>
     *   <li>optional UTC time: hhmmssZ, hh:mm:ssZ</li>
     *   <li>optional signed hours UTC offset: hhmmss+HH, hhmmss-HH, hh:mm:ss+HH, hh:mm:ss-HH</li>
     *   <li>optional signed basic hours and minutes UTC offset: hhmmss+HHMM, hhmmss-HHMM, hh:mm:ss+HHMM, hh:mm:ss-HHMM</li>
     *   <li>optional signed extended hours and minutes UTC offset: hhmmss+HH:MM, hhmmss-HH:MM, hh:mm:ss+HH:MM, hh:mm:ss-HH:MM</li>
     * </ul>
     *
     * <p> As shown by the list above, only the complete representations defined in section 4.2
     * of ISO-8601 standard are supported, neither expended representations nor representations
     * with reduced accuracy are supported.
     *
     * @param string string to parse
     * @return a parsed time
     * @exception IllegalArgumentException if string cannot be parsed
     */
    public static TimeComponents parseTime(final String string) {

        // is the date a calendar date ?
        final Matcher timeMatcher = ISO8601_FORMATS.matcher(string);
        if (timeMatcher.matches()) {
            final int        hour    = Integer.parseInt(timeMatcher.group(1));
            final int        minute  = Integer.parseInt(timeMatcher.group(2));
            final TimeOffset second  = timeMatcher.group(3) == null ?
                                       TimeOffset.ZERO :
                                       TimeOffset.parse(timeMatcher.group(3).replace(',', '.'));
            final String     offset  = timeMatcher.group(4);
            final int    minutesFromUTC;
            if (offset == null) {
                // no offset from UTC is given
                minutesFromUTC = 0;
            } else {
                // we need to parse an offset from UTC
                // the sign is mandatory and the ':' separator is optional
                // so we can have offsets given as -06:00 or +0100
                final int sign          = offset.codePointAt(0) == '-' ? -1 : +1;
                final int hourOffset    = Integer.parseInt(offset.substring(1, 3));
                final int minutesOffset = offset.length() <= 3 ? 0 : Integer.parseInt(offset.substring(offset.length() - 2));
                minutesFromUTC          = sign * (minutesOffset + MINUTE * hourOffset);
            }
            return new TimeComponents(hour, minute, second, minutesFromUTC);
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
     * @return second second number from 0.0 to 61.0 (excluded). Note that 60 &le; second
     * &lt; 61 only occurs during a leap second.
     */
    public double getSecond() {
        return second.toDouble();
    }

    /** Get the seconds number.
     * @return second second number from 0.0 to 61.0 (excluded). Note that 60 &le; second
     * &lt; 61 only occurs during a leap second.
     */
    public TimeOffset getSplitSecond() {
        return second;
    }

    /** Get the offset between the specified date and UTC.
     * <p>
     * The offset is always an integral number of minutes, as per ISO-8601 standard.
     * </p>
     * @return offset in minutes between the specified date and UTC
     * @since 7.2
     */
    public int getMinutesFromUTC() {
        return minutesFromUTC;
    }

    /** Get the second number within the local day, <em>without</em> applying the {@link #getMinutesFromUTC() offset from UTC}.
     * @return second number from 0.0 to Constants.JULIAN_DAY
     * @see #getSplitSecondsInLocalDay()
     * @see #getSecondsInUTCDay()
     * @since 7.2
     */
    public double getSecondsInLocalDay() {
        return getSplitSecondsInLocalDay().toDouble();
    }

    /** Get the second number within the local day, <em>without</em> applying the {@link #getMinutesFromUTC() offset from UTC}.
     * @return second number from 0.0 to Constants.JULIAN_DAY
     * @see #getSecondsInLocalDay()
     * @see #getSplitSecondsInUTCDay()
     * @since 13.0
     */
    public TimeOffset getSplitSecondsInLocalDay() {
        return new TimeOffset((long) MINUTE * minute + (long) HOUR * hour, 0L).add(second);
    }

    /** Get the second number within the UTC day, applying the {@link #getMinutesFromUTC() offset from UTC}.
     * @return second number from {@link #getMinutesFromUTC() -getMinutesFromUTC()}
     * to Constants.JULIAN_DAY {@link #getMinutesFromUTC() + getMinutesFromUTC()}
     * @see #getSplitSecondsInUTCDay()
     * @see #getSecondsInLocalDay()
     * @since 7.2
     */
    public double getSecondsInUTCDay() {
        return getSplitSecondsInUTCDay().toDouble();
    }

    /** Get the second number within the UTC day, applying the {@link #getMinutesFromUTC() offset from UTC}.
     * @return second number from {@link #getMinutesFromUTC() -getMinutesFromUTC()}
     * to Constants.JULIAN_DAY {@link #getMinutesFromUTC() + getMinutesFromUTC()}
     * @see #getSecondsInUTCDay()
     * @see #getSplitSecondsInLocalDay()
     * @since 13.0
     */
    public TimeOffset getSplitSecondsInUTCDay() {
        return new TimeOffset((long) MINUTE * (minute - minutesFromUTC) + (long) HOUR * hour, 0L).add(second);
    }

    /**
     * Round this time to the given precision if needed to prevent rounding up to an
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
     * @return the instance itself if no rounding was needed, or a time within
     * {@code 0.5 * 10**-fractionDigits} seconds of this, and with a seconds number that
     * will not round up to {@code minuteDuration} when rounded to {@code fractionDigits}
     * after the decimal point
     * @since 13.0
     */
    public TimeComponents wrapIfNeeded(final int minuteDuration, final int fractionDigits) {
        TimeOffset second = getSplitSecond();

        // adjust limit according to current minute duration
        final TimeOffset limit = WRAPPING[FastMath.min(fractionDigits, WRAPPING.length - 1)].
                                add(new TimeOffset(minuteDuration - 60, 0L));

        if (second.compareTo(limit) >= 0) {
            // we should wrap around to the next minute
            int wrappedMinute = minute;
            int wrappedHour   = hour;
            second = TimeOffset.ZERO;
            ++wrappedMinute;
            if (wrappedMinute > 59) {
                wrappedMinute = 0;
                ++wrappedHour;
                if (wrappedHour > 23) {
                    wrappedHour = 0;
                }
            }
            return new TimeComponents(wrappedHour, wrappedMinute, second);
        }
        return this;
    }

    /**
     * Package private method that allows specification of seconds format. Allows access from
     * {@link DateTimeComponents#toString(int, int)}. Access from outside of rounding methods would result in invalid
     * times, see #590, #591.
     *
     * @param fractionDigits the number of digits to include after the decimal point in the string representation of the
     *                       seconds. The date and time is first rounded as necessary. {@code fractionDigits} must be
     *                       greater than or equal to {@code 0}.
     * @return string without UTC offset.
     * @since 13.0
     */
    String toStringWithoutUtcOffset(final int fractionDigits) {

        if (second.isFinite()) {
            // general case for regular times
            final long      rounding = ROUNDING[FastMath.min(fractionDigits, ROUNDING.length - 1)];
            final TimeComponents rounded  = new TimeComponents(hour, minute,
                                                               new TimeOffset(second.getSeconds(),
                                                                              second.getAttoSeconds() + rounding));
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format("%02d:%02d:%02d",
                                         rounded.hour, rounded.minute, rounded.second.getSeconds()));
            if (fractionDigits > 0) {
                builder.append('.');
                builder.append(String.format("%018d", rounded.second.getAttoSeconds()), 0, fractionDigits);
            }
            return builder.toString();
        } else if (second.isNaN()) {
            // special handling for NaN
            return String.format("%02d:%02d:NaN", hour, minute);
        } else if (second.isNegativeInfinity()) {
            // special handling for -∞
            return String.format("%02d:%02d:-∞", hour, minute);
        } else {
            // special handling for +∞
            return String.format("%02d:%02d:+∞", hour, minute);
        }

    }

    /**
     * Get a string representation of the time without the offset from UTC.
     *
     * @return a string representation of the time in an ISO 8601 like format.
     * @see #formatUtcOffset()
     * @see #toString()
     */
    public String toStringWithoutUtcOffset() {
        // create formats here as they are not thread safe
        // Format for seconds to prevent rounding up to an invalid time. See #591
        final String formatted = toStringWithoutUtcOffset(18);
        int last = formatted.length() - 1;
        while (last > 11 && formatted.charAt(last) == '0') {
            // we want to remove final zeros (but keeping milliseconds for compatibility)
            --last;
        }
        return formatted.substring(0, last + 1);
    }

    /**
     * Get the UTC offset as a string in ISO8601 format. For example, {@code +00:00}.
     *
     * @return the UTC offset as a string.
     * @see #toStringWithoutUtcOffset()
     * @see #toString()
     */
    public String formatUtcOffset() {
        final int hourOffset = FastMath.abs(minutesFromUTC) / MINUTE;
        final int minuteOffset = FastMath.abs(minutesFromUTC) % MINUTE;
        return (minutesFromUTC < 0 ? '-' : '+') +
                String.format("%02d:%02d", hourOffset, minuteOffset);
    }

    /**
     * Get a string representation of the time including the offset from UTC.
     *
     * @return string representation of the time in an ISO 8601 like format including the
     * UTC offset.
     * @see #toStringWithoutUtcOffset()
     * @see #formatUtcOffset()
     */
    public String toString() {
        return toStringWithoutUtcOffset() + formatUtcOffset();
    }

    /** {@inheritDoc} */
    public int compareTo(final TimeComponents other) {
        return getSplitSecondsInUTCDay().compareTo(other.getSplitSecondsInUTCDay());
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final TimeComponents otherTime = (TimeComponents) other;
            return otherTime != null &&
                   hour           == otherTime.hour   &&
                   minute         == otherTime.minute &&
                   second.compareTo(otherTime.second) == 0 &&
                   minutesFromUTC == otherTime.minutesFromUTC;
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return ((hour << 16) ^ ((minute - minutesFromUTC) << 8)) ^ second.hashCode();
    }

}
