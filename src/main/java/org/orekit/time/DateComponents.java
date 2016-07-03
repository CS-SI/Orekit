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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/** Class representing a date broken up as year, month and day components.
 * <p>This class uses the astronomical convention for calendars,
 * which is also the convention used by <code>java.util.Date</code>:
 * a year zero is present between years -1 and +1, and 10 days are
 * missing in 1582. The calendar used around these special dates are:</p>
 * <ul>
 *   <li>up to 0000-12-31 : proleptic julian calendar</li>
 *   <li>from 0001-01-01 to 1582-10-04: julian calendar</li>
 *   <li>from 1582-10-15: gregorian calendar</li>
 * </ul>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see TimeComponents
 * @see DateTimeComponents
 * @author Luc Maisonobe
 */
public class DateComponents implements Serializable, Comparable<DateComponents> {

    /** Reference epoch for julian dates: -4712-01-01.
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     */
    public static final DateComponents JULIAN_EPOCH;

    /** Reference epoch for modified julian dates: 1858-11-17. */
    public static final DateComponents MODIFIED_JULIAN_EPOCH;

    /** Reference epoch for 1950 dates: 1950-01-01. */
    public static final DateComponents FIFTIES_EPOCH;

    /** Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4): 1958-01-01. */
    public static final DateComponents CCSDS_EPOCH;

    /** Reference epoch for Galileo System Time: 1999-08-22. */
    public static final DateComponents GALILEO_EPOCH;

    /** Reference epoch for GPS weeks: 1980-01-06. */
    public static final DateComponents GPS_EPOCH;

    /** J2000.0 Reference epoch: 2000-01-01. */
    public static final DateComponents J2000_EPOCH;

    /** Java Reference epoch: 1970-01-01. */
    public static final DateComponents JAVA_EPOCH;

    /** Serializable UID. */
    private static final long serialVersionUID = -2462694707837970938L;

    /** Factory for proleptic julian calendar (up to 0000-12-31). */
    private static final YearFactory PROLEPTIC_JULIAN_FACTORY = new ProlepticJulianFactory();

    /** Factory for julian calendar (from 0001-01-01 to 1582-10-04). */
    private static final YearFactory JULIAN_FACTORY           = new JulianFactory();

    /** Factory for gregorian calendar (from 1582-10-15). */
    private static final YearFactory GREGORIAN_FACTORY        = new GregorianFactory();

    /** Factory for leap years. */
    private static final MonthDayFactory LEAP_YEAR_FACTORY    = new LeapYearFactory();

    /** Factory for non-leap years. */
    private static final MonthDayFactory COMMON_YEAR_FACTORY  = new CommonYearFactory();

    /** Format for years. */
    private static final DecimalFormat FOUR_DIGITS = new DecimalFormat("0000");

    /** Format for months and days. */
    private static final DecimalFormat TWO_DIGITS  = new DecimalFormat("00");

    /** Offset between J2000 epoch and modified julian day epoch. */
    private static final int MJD_TO_J2000 = 51544;

    /** Basic and extended format calendar date. */
    private static Pattern CALENDAR_FORMAT = Pattern.compile("^(-?\\d\\d\\d\\d)-?(\\d\\d)-?(\\d\\d)$");

    /** Basic and extended format ordinal date. */
    private static Pattern ORDINAL_FORMAT = Pattern.compile("^(-?\\d\\d\\d\\d)-?(\\d\\d\\d)$");

    /** Basic and extended format week date. */
    private static Pattern WEEK_FORMAT = Pattern.compile("^(-?\\d\\d\\d\\d)-?W(\\d\\d)-?(\\d)$");

    static {
        // this static statement makes sure the reference epoch are initialized
        // once AFTER the various factories have been set up
        JULIAN_EPOCH          = new DateComponents(-4712,  1,  1);
        MODIFIED_JULIAN_EPOCH = new DateComponents(1858, 11, 17);
        FIFTIES_EPOCH         = new DateComponents(1950, 1, 1);
        CCSDS_EPOCH           = new DateComponents(1958, 1, 1);
        GALILEO_EPOCH         = new DateComponents(1999, 8, 22);
        GPS_EPOCH             = new DateComponents(1980, 1, 6);
        J2000_EPOCH           = new DateComponents(2000, 1, 1);
        JAVA_EPOCH            = new DateComponents(1970, 1, 1);
    }

    /** Year number. */
    private final int year;

    /** Month number. */
    private final int month;

    /** Day number. */
    private final int day;

    /** Build a date from its components.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateComponents(final int year, final int month, final int day)
        throws IllegalArgumentException {

        // very rough range check
        // (just to avoid ArrayOutOfboundException in MonthDayFactory later)
        if ((month < 1) || (month > 12)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_MONTH, month);
        }

        // start by trusting the parameters
        this.year  = year;
        this.month = month;
        this.day   = day;

        // build a check date from the J2000 day
        final DateComponents check = new DateComponents(getJ2000Day());

        // check the parameters for mismatch
        // (i.e. invalid date components, like 29 february on non-leap years)
        if ((year != check.year) || (month != check.month) || (day != check.day)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_YEAR_MONTH_DAY,
                                                      year, month, day);
        }

    }

    /** Build a date from its components.
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, february 29 for non-leap years,
     * dates during the gregorian leap in 1582 ...)
     */
    public DateComponents(final int year, final Month month, final int day)
        throws IllegalArgumentException {
        this(year, month.getNumber(), day);
    }

    /** Build a date from a year and day number.
     * @param year year number (may be 0 or negative for BC years)
     * @param dayNumber day number in the year from 1 to 366
     * @exception IllegalArgumentException if dayNumber is out of range
     * with respect to year
     */
    public DateComponents(final int year, final int dayNumber)
        throws IllegalArgumentException {
        this(J2000_EPOCH, new DateComponents(year - 1, 12, 31).getJ2000Day() + dayNumber);
        if (dayNumber != getDayOfYear()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_DAY_NUMBER_IN_YEAR,
                                                     dayNumber, year);
        }
    }

    /** Build a date from its offset with respect to a {@link #J2000_EPOCH}.
     * @param offset offset with respect to a {@link #J2000_EPOCH}
     * @see #getJ2000Day()
     */
    public DateComponents(final int offset) {

        // we follow the astronomical convention for calendars:
        // we consider a year zero and 10 days are missing in 1582
        // from 1582-10-15: gregorian calendar
        // from 0001-01-01 to 1582-10-04: julian calendar
        // up to 0000-12-31 : proleptic julian calendar
        YearFactory yFactory = GREGORIAN_FACTORY;
        if (offset < -152384) {
            if (offset > -730122) {
                yFactory = JULIAN_FACTORY;
            } else {
                yFactory = PROLEPTIC_JULIAN_FACTORY;
            }
        }
        year = yFactory.getYear(offset);
        final int dayInYear = offset - yFactory.getLastJ2000DayOfYear(year - 1);

        // handle month/day according to the year being a common or leap year
        final MonthDayFactory mdFactory =
            yFactory.isLeap(year) ? LEAP_YEAR_FACTORY : COMMON_YEAR_FACTORY;
        month = mdFactory.getMonth(dayInYear);
        day   = mdFactory.getDay(dayInYear, month);

    }

    /** Build a date from its offset with respect to a reference epoch.
     * <p>This constructor is mainly useful to build a date from a modified
     * julian day (using {@link #MODIFIED_JULIAN_EPOCH}) or a GPS week number
     * (using {@link #GPS_EPOCH}).</p>
     * @param epoch reference epoch
     * @param offset offset with respect to a reference epoch
     * @see #DateComponents(int)
     * @see #getMJD()
     */
    public DateComponents(final DateComponents epoch, final int offset) {
        this(epoch.getJ2000Day() + offset);
    }

    /** Build a date from week components.
     * <p>The calendar week number is a number between 1 and 52 or 53 depending
     * on the year. Week 1 is defined by ISO as the one that includes the first
     * Thursday of a year. Week 1 may therefore start the previous year and week
     * 52 or 53 may end in the next year. As an example calendar date 1995-01-01
     * corresponds to week date 1994-W52-7 (i.e. Sunday in the last week of 1994
     * is in fact the first day of year 1995). This date would beAnother example is calendar date
     * 1996-12-31 which corresponds to week date 1997-W01-2 (i.e. Tuesday in the
     * first week of 1997 is in fact the last day of year 1996).</p>
     * @param wYear year associated to week numbering
     * @param week week number in year,from 1 to 52 or 53
     * @param dayOfWeek day of week, from 1 (Monday) to 7 (Sunday)
     * @return a builded date
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range, week 53 on a 52 weeks year ...)
     */
    public static DateComponents createFromWeekComponents(final int wYear, final int week, final int dayOfWeek)
        throws IllegalArgumentException {

        final DateComponents firstWeekMonday = new DateComponents(getFirstWeekMonday(wYear));
        final DateComponents d = new DateComponents(firstWeekMonday, 7 * week + dayOfWeek - 8);

        // check the parameters for invalid date components
        if ((week != d.getCalendarWeek()) || (dayOfWeek != d.getDayOfWeek())) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_WEEK_DATE,
                                                     wYear, week, dayOfWeek);
        }

        return d;

    }

    /** Parse a string in ISO-8601 format to build a date.
     * <p>The supported formats are:
     * <ul>
     *   <li>basic format calendar date: YYYYMMDD</li>
     *   <li>extended format calendar date: YYYY-MM-DD</li>
     *   <li>basic format ordinal date: YYYYDDD</li>
     *   <li>extended format ordinal date: YYYY-DDD</li>
     *   <li>basic format week date: YYYYWwwD</li>
     *   <li>extended format week date: YYYY-Www-D</li>
     * </ul>
     *
     * <p> As shown by the list above, only the complete representations defined in section 4.1
     * of ISO-8601 standard are supported, neither expended representations nor representations
     * with reduced accuracy are supported.
     *
     * <p>
     * Parsing a single integer as a julian day is <em>not</em> supported as it may be ambiguous
     * with either the basic format calendar date or the basic format ordinal date depending
     * on the number of digits.
     * </p>
     * @param string string to parse
     * @return a parsed date
     * @exception IllegalArgumentException if string cannot be parsed
     */
    public static  DateComponents parseDate(final String string) {

        // is the date a calendar date ?
        final Matcher calendarMatcher = CALENDAR_FORMAT.matcher(string);
        if (calendarMatcher.matches()) {
            return new DateComponents(Integer.parseInt(calendarMatcher.group(1)),
                                      Integer.parseInt(calendarMatcher.group(2)),
                                      Integer.parseInt(calendarMatcher.group(3)));
        }

        // is the date an ordinal date ?
        final Matcher ordinalMatcher = ORDINAL_FORMAT.matcher(string);
        if (ordinalMatcher.matches()) {
            return new DateComponents(Integer.parseInt(ordinalMatcher.group(1)),
                                      Integer.parseInt(ordinalMatcher.group(2)));
        }

        // is the date a week date ?
        final Matcher weekMatcher = WEEK_FORMAT.matcher(string);
        if (weekMatcher.matches()) {
            return createFromWeekComponents(Integer.parseInt(weekMatcher.group(1)),
                                            Integer.parseInt(weekMatcher.group(2)),
                                            Integer.parseInt(weekMatcher.group(3)));
        }

        throw new OrekitIllegalArgumentException(OrekitMessages.NON_EXISTENT_DATE, string);

    }

    /** Get the year number.
     * @return year number (may be 0 or negative for BC years)
     */
    public int getYear() {
        return year;
    }

    /** Get the month.
     * @return month number from 1 to 12
     */
    public int getMonth() {
        return month;
    }

    /** Get the month as an enumerate.
     * @return month as an enumerate
     */
    public Month getMonthEnum() {
        return Month.getMonth(month);
    }

    /** Get the day.
     * @return day number from 1 to 31
     */
    public int getDay() {
        return day;
    }

    /** Get the day number with respect to J2000 epoch.
     * @return day number with respect to J2000 epoch
     */
    public int getJ2000Day() {
        YearFactory yFactory = GREGORIAN_FACTORY;
        if (year < 1583) {
            if (year < 1) {
                yFactory = PROLEPTIC_JULIAN_FACTORY;
            } else if ((year < 1582) || (month < 10) || ((month < 11) && (day < 5))) {
                yFactory = JULIAN_FACTORY;
            }
        }
        final MonthDayFactory mdFactory =
            yFactory.isLeap(year) ? LEAP_YEAR_FACTORY : COMMON_YEAR_FACTORY;
        return yFactory.getLastJ2000DayOfYear(year - 1) +
               mdFactory.getDayInYear(month, day);
    }

    /** Get the modified julian day.
     * @return modified julian day
     */
    public int getMJD() {
        return MJD_TO_J2000 + getJ2000Day();
    }

    /** Get the calendar week number.
     * <p>The calendar week number is a number between 1 and 52 or 53 depending
     * on the year. Week 1 is defined by ISO as the one that includes the first
     * Thursday of a year. Week 1 may therefore start the previous year and week
     * 52 or 53 may end in the next year. As an example calendar date 1995-01-01
     * corresponds to week date 1994-W52-7 (i.e. Sunday in the last week of 1994
     * is in fact the first day of year 1995). Another example is calendar date
     * 1996-12-31 which corresponds to week date 1997-W01-2 (i.e. Tuesday in the
     * first week of 1997 is in fact the last day of year 1996).</p>
     * @return calendar week number
     */
    public int getCalendarWeek() {
        final int firstWeekMonday = getFirstWeekMonday(year);
        int daysSincefirstMonday = getJ2000Day() - firstWeekMonday;
        if (daysSincefirstMonday < 0) {
            // we are still in a week from previous year
            daysSincefirstMonday += firstWeekMonday - getFirstWeekMonday(year - 1);
        } else if (daysSincefirstMonday > 363) {
            // up to three days at end of year may belong to first week of next year
            // (by chance, there is no need for a specific check in year 1582 ...)
            final int weekYearLength = getFirstWeekMonday(year + 1) - firstWeekMonday;
            if (daysSincefirstMonday >= weekYearLength) {
                daysSincefirstMonday -= weekYearLength;
            }
        }
        return 1 + daysSincefirstMonday / 7;
    }

    /** Get the monday of a year first week.
     * @param year year to consider
     * @return day of the monday of the first weak of year
     */
    private static int getFirstWeekMonday(final int year) {
        final int yearFirst = new DateComponents(year, 1, 1).getJ2000Day();
        final int offsetToMonday = 4 - (yearFirst + 2) % 7;
        return yearFirst + offsetToMonday + ((offsetToMonday > 3) ? -7 : 0);
    }

    /** Get the day of week.
     * <p>Day of week is a number between 1 (Monday) and 7 (Sunday).</p>
     * @return day of week
     */
    public int getDayOfWeek() {
        final int dow = (getJ2000Day() + 6) % 7; // result is between -6 and +6
        return (dow < 1) ? (dow + 7) : dow;
    }

    /** Get the day number in year.
     * <p>Day number in year is between 1 (January 1st) and either 365 or
     * 366 inclusive depending on year.</p>
     * @return day number in year
     */
    public int getDayOfYear() {
        return getJ2000Day() - new DateComponents(year - 1, 12, 31).getJ2000Day();
    }

    /** Get a string representation (ISO-8601) of the date.
     * @return string representation of the date.
     */
    public String toString() {
        return new StringBuffer().
               append(FOUR_DIGITS.format(year)).append('-').
               append(TWO_DIGITS.format(month)).append('-').
               append(TWO_DIGITS.format(day)).
               toString();
    }

    /** {@inheritDoc} */
    public int compareTo(final DateComponents other) {
        final int j2000Day = getJ2000Day();
        final int otherJ2000Day = other.getJ2000Day();
        if (j2000Day < otherJ2000Day) {
            return -1;
        } else if (j2000Day > otherJ2000Day) {
            return 1;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final DateComponents otherDate = (DateComponents) other;
            return (otherDate != null) && (year == otherDate.year) &&
                   (month == otherDate.month) && (day == otherDate.day);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return (year << 16) ^ (month << 8) ^ day;
    }

    /** Interface for dealing with years sequences according to some calendar. */
    private interface YearFactory {

        /** Get the year number for a given day number with respect to J2000 epoch.
         * @param j2000Day day number with respect to J2000 epoch
         * @return year number
         */
        int getYear(int j2000Day);

        /** Get the day number with respect to J2000 epoch for new year's Eve.
         * @param year year number
         * @return day number with respect to J2000 epoch for new year's Eve
         */
        int getLastJ2000DayOfYear(int year);

        /** Check if a year is a leap or common year.
         * @param year year number
         * @return true if year is a leap year
         */
        boolean isLeap(int year);

    }

    /** Class providing a years sequence compliant with the proleptic julian calendar. */
    private static class ProlepticJulianFactory implements YearFactory {

        /** {@inheritDoc} */
        public int getYear(final int j2000Day) {
            return  (int) -((-4l * j2000Day - 2920488l) / 1461l);
        }

        /** {@inheritDoc} */
        public int getLastJ2000DayOfYear(final int year) {
            return (1461 * year + 1) / 4 - 730123;
        }

        /** {@inheritDoc} */
        public boolean isLeap(final int year) {
            return (year % 4) == 0;
        }

    }

    /** Class providing a years sequence compliant with the julian calendar. */
    private static class JulianFactory implements YearFactory {

        /** {@inheritDoc} */
        public int getYear(final int j2000Day) {
            return  (int) ((4l * j2000Day + 2921948l) / 1461l);
        }

        /** {@inheritDoc} */
        public int getLastJ2000DayOfYear(final int year) {
            return (1461 * year) / 4 - 730122;
        }

        /** {@inheritDoc} */
        public boolean isLeap(final int year) {
            return (year % 4) == 0;
        }

    }

    /** Class providing a years sequence compliant with the gregorian calendar. */
    private static class GregorianFactory implements YearFactory {

        /** {@inheritDoc} */
        public int getYear(final int j2000Day) {

            // year estimate
            int year = (int) ((400l * j2000Day + 292194288l) / 146097l);

            // the previous estimate is one unit too high in some rare cases
            // (240 days in the 400 years gregorian cycle, about 0.16%)
            if (j2000Day <= getLastJ2000DayOfYear(year - 1)) {
                --year;
            }

            // exact year
            return year;

        }

        /** {@inheritDoc} */
        public int getLastJ2000DayOfYear(final int year) {
            return (1461 * year) / 4 - year / 100 + year / 400 - 730120;
        }

        /** {@inheritDoc} */
        public boolean isLeap(final int year) {
            return ((year % 4) == 0) && (((year % 400) == 0) || ((year % 100) != 0));
        }

    }

    /** Interface for dealing with months sequences according to leap/common years. */
    private interface MonthDayFactory {

        /** Get the month number for a given day number within year.
         * @param dayInYear day number within year
         * @return month number
         */
        int getMonth(int dayInYear);

        /** Get the day number for given month and day number within year.
         * @param dayInYear day number within year
         * @param month month number
         * @return day number
         */
        int getDay(int dayInYear, int month);

        /** Get the day number within year for given month and day numbers.
         * @param month month number
         * @param day day number
         * @return day number within year
         */
        int getDayInYear(int month, int day);

    }

    /** Class providing the months sequence for leap years. */
    private static class LeapYearFactory implements MonthDayFactory {

        /** Months succession definition. */
        private static final int[] PREVIOUS_MONTH_END_DAY = {
            0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335
        };

        /** {@inheritDoc} */
        public int getMonth(final int dayInYear) {
            return (dayInYear < 32) ? 1 : (10 * dayInYear + 313) / 306;
        }

        /** {@inheritDoc} */
        public int getDay(final int dayInYear, final int month) {
            return dayInYear - PREVIOUS_MONTH_END_DAY[month];
        }

        /** {@inheritDoc} */
        public int getDayInYear(final int month, final int day) {
            return day + PREVIOUS_MONTH_END_DAY[month];
        }

    }

    /** Class providing the months sequence for common years. */
    private static class CommonYearFactory implements MonthDayFactory {

        /** Months succession definition. */
        private static final int[] PREVIOUS_MONTH_END_DAY = {
            0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
        };

        /** {@inheritDoc} */
        public int getMonth(final int dayInYear) {
            return (dayInYear < 32) ? 1 : (10 * dayInYear + 323) / 306;
        }

        /** {@inheritDoc} */
        public int getDay(final int dayInYear, final int month) {
            return dayInYear - PREVIOUS_MONTH_END_DAY[month];
        }

        /** {@inheritDoc} */
        public int getDayInYear(final int month, final int day) {
            return day + PREVIOUS_MONTH_END_DAY[month];
        }

    }

}
