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
    public static final TimeComponents H00   = new TimeComponents(0, 0, 0);

    /** Constant for commonly used hour 12:00:00. */
    public static final TimeComponents H12 = new TimeComponents(12, 0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = 20160331L;

    /** Formatting symbols used in {@link #toString()}. */
    private static final DecimalFormatSymbols US_SYMBOLS =
            new DecimalFormatSymbols(Locale.US);

    /** Basic and extends formats for local time, with optional timezone. */
    private static final Pattern ISO8601_FORMATS = Pattern.compile("^(\\d\\d):?(\\d\\d):?(\\d\\d(?:[.,]\\d+)?)?(?:Z|([-+]\\d\\d(?::?\\d\\d)?))?$");

    /** Hour number. */
    private final int hour;

    /** Minute number. */
    private final int minute;

    /** Second number. */
    private final double second;

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
    public TimeComponents(final int hour, final int minute, final double second,
                          final int minutesFromUTC)
        throws IllegalArgumentException {

        // range check
        if (hour < 0 || hour > 23 ||
            minute < 0 || minute > 59 ||
            second < 0 || second >= 61.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_HMS_TIME,
                                                     hour, minute, second);
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
     * will be less than {@code 60.0}, otherwise it will be less than {@code 61.0}. This constructor
     * may produce an invalid value of {@link #getSecond()} during a negative leap second,
     * through there has never been one. For more control over the number of seconds in
     * the final minute use {@link #fromSeconds(int, double, double, int)}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * @param secondInDay second number from 0.0 to {@link Constants#JULIAN_DAY} {@code +
     *                    1} (excluded)
     * @throws OrekitIllegalArgumentException if seconds number is out of range
     * @see #fromSeconds(int, double, double, int)
     * @see #TimeComponents(int, double)
     */
    public TimeComponents(final double secondInDay)
            throws OrekitIllegalArgumentException {
        this(0, secondInDay);
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
     * the final minute use {@link #fromSeconds(int, double, double, int)}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC()} will
     * return 0).
     *
     * @param secondInDayA first part of the second number
     * @param secondInDayB last part of the second number
     * @throws OrekitIllegalArgumentException if seconds number is out of range
     * @see #fromSeconds(int, double, double, int)
     */
    public TimeComponents(final int secondInDayA, final double secondInDayB)
            throws OrekitIllegalArgumentException {
        // if the total is at least 86400 then assume there is a leap second
        this(
                (Constants.JULIAN_DAY - secondInDayA) - secondInDayB > 0 ? secondInDayA : secondInDayA - 1,
                secondInDayB,
                (Constants.JULIAN_DAY - secondInDayA) - secondInDayB > 0 ? 0 : 1,
                (Constants.JULIAN_DAY - secondInDayA) - secondInDayB > 0 ? 60 : 61);
    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>The seconds past midnight is the sum {@code secondInDayA + secondInDayB +
     * leap}. The two parameters are used for increased accuracy. Only the first part of
     * the sum ({@code secondInDayA + secondInDayB}) is used to compute the hours and
     * minutes. The third parameter ({@code leap}) is added directly to the second value
     * ({@link #getSecond()}) to implement leap seconds. These three quantities must
     * satisfy the following constraints. This first guarantees the hour and minute are
     * valid, the second guarantees the second is valid.
     *
     * <pre>
     *     {@code 0 <= secondInDayA + secondInDayB < 86400}
     *     {@code 0 <= (secondInDayA + secondInDayB) % 60 + leap < minuteDuration}
     *     {@code 0 <= leap <= minuteDuration - 60                        if minuteDuration >= 60}
     *     {@code 0 >= leap >= minuteDuration - 60                        if minuteDuration <  60}
     * </pre>
     *
     * <p>If the seconds of minute ({@link #getSecond()}) computed from {@code
     * secondInDayA + secondInDayB + leap} is greater than or equal to {@code
     * minuteDuration} then the second of minute will be set to {@code
     * FastMath.nextDown(minuteDuration)}. This prevents rounding to an invalid seconds of
     * minute number when the input values have greater precision than a {@code double}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * <p>If {@code secondsInDayB} or {@code leap} is NaN then the hour and minute will
     * be determined from {@code secondInDayA} and the second of minute will be NaN.
     *
     * <p>This constructor is private to avoid confusion with the other constructors that
     * would be caused by overloading. Use {@link #fromSeconds(int, double, double,
     * int)}.
     *
     * @param secondInDayA   first part of the second number.
     * @param secondInDayB   last part of the second number.
     * @param leap           magnitude of the leap second if this point in time is during
     *                       a leap second, otherwise {@code 0.0}. This value is not used
     *                       to compute hours and minutes, but it is added to the computed
     *                       second of minute.
     * @param minuteDuration number of seconds in the current minute, normally {@code 60}.
     * @throws OrekitIllegalArgumentException if the inequalities above do not hold.
     * @see #fromSeconds(int, double, double, int)
     * @since 10.2
     */
    private TimeComponents(final int secondInDayA,
                           final double secondInDayB,
                           final double leap,
                           final int minuteDuration) throws OrekitIllegalArgumentException {

        // split the numbers as a whole number of seconds
        // and a fractional part between 0.0 (included) and 1.0 (excluded)
        final int carry         = (int) FastMath.floor(secondInDayB);
        int wholeSeconds        = secondInDayA + carry;
        final double fractional = secondInDayB - carry;

        // range check
        if (wholeSeconds < 0 || wholeSeconds >= Constants.JULIAN_DAY) {
            throw new OrekitIllegalArgumentException(
                    OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                    // this can produce some strange messages due to rounding
                    secondInDayA + secondInDayB,
                    0,
                    Constants.JULIAN_DAY);
        }
        final int maxExtraSeconds = minuteDuration - 60;
        if (leap * maxExtraSeconds < 0 ||
                FastMath.abs(leap) > FastMath.abs(maxExtraSeconds)) {
            throw new OrekitIllegalArgumentException(
                    OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                    leap, 0, maxExtraSeconds);
        }

        // extract the time components
        hour           = wholeSeconds / 3600;
        wholeSeconds  -= 3600 * hour;
        minute         = wholeSeconds / 60;
        wholeSeconds  -= 60 * minute;
        // at this point ((minuteDuration - wholeSeconds) - leap) - fractional > 0
        // or else one of the preconditions was violated. Even if there is not violation,
        // naiveSecond may round to minuteDuration, creating an invalid time.
        // In that case round down to preserve a valid time at the cost of up to 1 ULP of error.
        // See #676 and #681.
        final double naiveSecond = wholeSeconds + (leap + fractional);
        if (naiveSecond < 0) {
            throw new OrekitIllegalArgumentException(
                    OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL,
                    naiveSecond, 0, minuteDuration);
        }
        if (naiveSecond < minuteDuration || Double.isNaN(naiveSecond)) {
            second = naiveSecond;
        } else {
            second = FastMath.nextDown((double) minuteDuration);
        }
        minutesFromUTC = 0;

    }

    /**
     * Build a time from the second number within the day.
     *
     * <p>The seconds past midnight is the sum {@code secondInDayA + secondInDayB +
     * leap}. The two parameters are used for increased accuracy. Only the first part of
     * the sum ({@code secondInDayA + secondInDayB}) is used to compute the hours and
     * minutes. The third parameter ({@code leap}) is added directly to the second value
     * ({@link #getSecond()}) to implement leap seconds. These three quantities must
     * satisfy the following constraints. This first guarantees the hour and minute are
     * valid, the second guarantees the second is valid.
     *
     * <pre>
     *     {@code 0 <= secondInDayA + secondInDayB < 86400}
     *     {@code 0 <= (secondInDayA + secondInDayB) % 60 + leap <= minuteDuration}
     *     {@code 0 <= leap <= minuteDuration - 60                        if minuteDuration >= 60}
     *     {@code 0 >= leap >= minuteDuration - 60                        if minuteDuration <  60}
     * </pre>
     *
     * <p>If the seconds of minute ({@link #getSecond()}) computed from {@code
     * secondInDayA + secondInDayB + leap} is greater than or equal to {@code 60 + leap}
     * then the second of minute will be set to {@code FastMath.nextDown(60 + leap)}. This
     * prevents rounding to an invalid seconds of minute number when the input values have
     * greater precision than a {@code double}.
     *
     * <p>This constructor is always in UTC (i.e. {@link #getMinutesFromUTC() will return
     * 0}).
     *
     * <p>If {@code secondsInDayB} or {@code leap} is NaN then the hour and minute will
     * be determined from {@code secondInDayA} and the second of minute will be NaN.
     *
     * @param secondInDayA   first part of the second number.
     * @param secondInDayB   last part of the second number.
     * @param leap           magnitude of the leap second if this point in time is during
     *                       a leap second, otherwise {@code 0.0}. This value is not used
     *                       to compute hours and minutes, but it is added to the computed
     *                       second of minute.
     * @param minuteDuration number of seconds in the current minute, normally {@code 60}.
     * @return new time components for the specified time.
     * @throws OrekitIllegalArgumentException if the inequalities above do not hold.
     * @since 10.2
     */
    public static TimeComponents fromSeconds(final int secondInDayA,
                                             final double secondInDayB,
                                             final double leap,
                                             final int minuteDuration) {
        return new TimeComponents(secondInDayA, secondInDayB, leap, minuteDuration);
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
            final int    hour      = Integer.parseInt(timeMatcher.group(1));
            final int    minute    = Integer.parseInt(timeMatcher.group(2));
            final double second    = timeMatcher.group(3) == null ? 0.0 : Double.parseDouble(timeMatcher.group(3).replace(',', '.'));
            final String offset    = timeMatcher.group(4);
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
                minutesFromUTC          = sign * (minutesOffset + 60 * hourOffset);
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
     * @see #getSecondsInUTCDay()
     * @since 7.2
     */
    public double getSecondsInLocalDay() {
        return second + 60 * minute + 3600 * hour;
    }

    /** Get the second number within the UTC day, applying the {@link #getMinutesFromUTC() offset from UTC}.
     * @return second number from {@link #getMinutesFromUTC() -getMinutesFromUTC()}
     * to Constants.JULIAN_DAY {@link #getMinutesFromUTC() + getMinutesFromUTC()}
     * @see #getSecondsInLocalDay()
     * @since 7.2
     */
    public double getSecondsInUTCDay() {
        return second + 60 * (minute - minutesFromUTC) + 3600 * hour;
    }

    /**
     * Package private method that allows specification of seconds format. Allows access
     * from {@link DateTimeComponents#toString(int, int)}. Access from outside of rounding
     * methods would result in invalid times, see #590, #591.
     *
     * @param secondsFormat for the seconds.
     * @return string without UTC offset.
     */
    String toStringWithoutUtcOffset(final DecimalFormat secondsFormat) {
        return String.format("%02d:%02d:%s", hour, minute, secondsFormat.format(second));
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
        final DecimalFormat secondsFormat =
                new DecimalFormat("00.000###########", US_SYMBOLS);
        return toStringWithoutUtcOffset(secondsFormat);
    }

    /**
     * Get the UTC offset as a string in ISO8601 format. For example, {@code +00:00}.
     *
     * @return the UTC offset as a string.
     * @see #toStringWithoutUtcOffset()
     * @see #toString()
     */
    public String formatUtcOffset() {
        final int hourOffset = FastMath.abs(minutesFromUTC) / 60;
        final int minuteOffset = FastMath.abs(minutesFromUTC) % 60;
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
        return Double.compare(getSecondsInUTCDay(), other.getSecondsInUTCDay());
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final TimeComponents otherTime = (TimeComponents) other;
            return otherTime != null &&
                   hour           == otherTime.hour   &&
                   minute         == otherTime.minute &&
                   second         == otherTime.second &&
                   minutesFromUTC == otherTime.minutesFromUTC;
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final long bits = Double.doubleToLongBits(second);
        return ((hour << 16) ^ ((minute - minutesFromUTC) << 8)) ^ (int) (bits ^ (bits >>> 32));
    }

}
