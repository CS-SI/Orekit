/* Copyright 2002-2019 CS Systèmes d'Information
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

/** Container for date in QZSS form.
 * <p> QZSS date has the same form as {@link GPSDate} </p>
 * @author Luc Maisonobe (original code for GPS Date)
 * @author Bryan Cazabonne (transformation to QZSS Date)
 * @see AbsoluteDate
 */
public class QZSSDate implements Serializable, TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20190213L;

    /** Duration of a week in seconds. */
    private static final double WEEK = 7 * Constants.JULIAN_DAY;

    /** conversion factor from seconds to milliseconds. */
    private static final double S_TO_MS = 1000.0;

    /** Week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}. */
    private final int weekNumber;

    /** Number of milliseconds since week start. */
    private final double milliInWeek;

    /** Corresponding date. */
    private final transient AbsoluteDate date;

    /** Build an instance corresponding to a QZSS date.
     * <p>QZSS dates are provided as a week number starting at
     * {@link AbsoluteDate#QZSS_EPOCH QZSS epoch} and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}
     * @param milliInWeek number of milliseconds since week start
     */
    public QZSSDate(final int weekNumber, final double milliInWeek) {

        this.weekNumber  = weekNumber;
        this.milliInWeek = milliInWeek;

        final int day = (int) FastMath.floor(milliInWeek / (Constants.JULIAN_DAY * S_TO_MS));
        final double secondsInDay = milliInWeek / S_TO_MS - day * Constants.JULIAN_DAY;
        date = new AbsoluteDate(new DateComponents(DateComponents.QZSS_EPOCH, weekNumber * 7 + day),
                                new TimeComponents(secondsInDay),
                                TimeScalesFactory.getQZSS());

    }

    /** Build an instance from an absolute date.
     * @param date absolute date to consider
     */
    public QZSSDate(final AbsoluteDate date) {

        this.weekNumber  = (int) FastMath.floor(date.durationFrom(AbsoluteDate.QZSS_EPOCH) / WEEK);
        final AbsoluteDate weekStart = new AbsoluteDate(AbsoluteDate.QZSS_EPOCH, WEEK * weekNumber);
        this.milliInWeek = date.durationFrom(weekStart) * S_TO_MS;
        this.date        = date;

    }

    /** Get the week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}.
     * @return week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}
     */
    public int getWeekNumber() {
        return weekNumber;
    }

    /** Get the number of milliseconds since week start.
     * @return number of milliseconds since week start
     */
    public double getMilliInWeek() {
        return milliInWeek;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(weekNumber, milliInWeek);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20190213L;

        /** Week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}. */
        private final int weekNumber;

        /** Number of milliseconds since week start. */
        private final double milliInWeek;

        /** Simple constructor.
         * @param weekNumber week number since {@link AbsoluteDate#QZSS_EPOCH QZSS epoch}
         * @param milliInWeek number of milliseconds since week start
         */
        DataTransferObject(final int weekNumber, final double milliInWeek) {
            this.weekNumber  = weekNumber;
            this.milliInWeek = milliInWeek;
        }

        /** Replace the deserialized data transfer object with a {@link QZSSDate}.
         * @return replacement {@link QZSSDate}
         */
        private Object readResolve() {
            return new QZSSDate(weekNumber, milliInWeek);
        }

    }

}
