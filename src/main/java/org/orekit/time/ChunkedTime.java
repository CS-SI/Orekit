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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.orekit.errors.OrekitException;


/** Class representing a time within the day as hour, minute and second chunks.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @see ChunkedDate
 * @see ChunksPair
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class ChunkedTime implements Serializable, Comparable<ChunkedTime> {

    /** Constant for commonly used hour 00:00:00. */
    public static final ChunkedTime H00   = new ChunkedTime(0, 0, 0);

    /** Constant for commonly used hour 12:00:00. */
    public static final ChunkedTime H12 = new ChunkedTime(12, 0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = 4337093028103368744L;

    /** Format for hours and minutes. */
    private static final DecimalFormat TWO_DIGITS = new DecimalFormat("00");

    /** Format for seconds. */
    private static final DecimalFormat SECONDS_FORMAT =
        new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US));

    /** Hour number. */
    private final int hour;

    /** Minute number. */
    private final int minute;

    /** Second number. */
    private final double second;

    /** Build a time from its clock elements.
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public ChunkedTime(final int hour, final int minute, final double second)
        throws IllegalArgumentException {

        // range check
        if ((hour   < 0) || (hour   >  23) ||
                (minute < 0) || (minute >  59) ||
                (second < 0) || (second >= 60.0)) {
            throw OrekitException.createIllegalArgumentException("non-existent hour {0}:{1}:{2}",
                                                                 new Object[] {
                                                                     Integer.valueOf(hour),
                                                                     Integer.valueOf(minute),
                                                                     new Double(second)
                                                                 });
        }

        this.hour = hour;
        this.minute = minute;
        this.second = second;

    }

    /** Build a time from the second number within the day.
     * @param secondInDay second number from 0.0 to 86400.0 (excluded)
     * @exception IllegalArgumentException if seconds number is out of range
     */
    public ChunkedTime(final double secondInDay) {
        // range check
        if ((secondInDay < 0) || (secondInDay >= 86400.0)) {
            throw OrekitException.createIllegalArgumentException("out of range seconds number: {0}",
                                                                 new Object[] {
                                                                     new Double(secondInDay)
                                                                 });
        }

        // extract the time chunks
        hour = (int) Math.floor(secondInDay / 3600.0);
        double remains = secondInDay - hour * 3600;
        minute = (int) Math.floor(remains / 60.0);
        remains -= minute * 60;
        second = remains;

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
     * @return second second number from 0.0 to 60.0 (excluded)
     */
    public double getSecond() {
        return second;
    }

    /** Get the second number within the day.
     * @return second number from 0.0 to 86400.0
     */
    public double getSecondsInDay() {
        return second + 60 * minute + 3600 * hour;
    }

    /** Get a string representation of the time.
     * @return string representation of the time
     */
    public String toString() {
        return new StringBuffer().
        append(TWO_DIGITS.format(hour)).append(':').
        append(TWO_DIGITS.format(minute)).append(':').
        append(SECONDS_FORMAT.format(second)).
        toString();
    }

    /** {@inheritDoc} */
    public int compareTo(final ChunkedTime other) {
        final double seconds = getSecondsInDay();
        final double otherSeconds = other.getSecondsInDay();
        if (seconds < otherSeconds) {
            return -1;
        } else if (seconds > otherSeconds) {
            return 1;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
        try {
            final ChunkedTime otherTime = (ChunkedTime) other;
            return (otherTime != null) && (hour == otherTime.hour) &&
                   (minute == otherTime.minute) && (second == otherTime.second);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final long bits = Double.doubleToLongBits(second);
        return ((hour << 8) | (minute << 8)) ^ (int) (bits ^ (bits >>> 32));
    }

}
