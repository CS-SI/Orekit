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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.util.FastMath;
import org.orekit.frames.EOPEntry;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Container for date in GPS form.
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @since 9.3
 */
public class GPSDate implements Serializable, TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180633L;

    /** Duration of a week in days. */
    private static final int WEEK_D = 7;

    /** Duration of a week in seconds. */
    private static final double WEEK_S = WEEK_D * Constants.JULIAN_DAY;

    /** Number of weeks in one rollover cycle. */
    private static final int CYCLE_W = 1024;

    /** Number of days in one rollover cycle. */
    private static final int CYCLE_D = WEEK_D * CYCLE_W;

    /** Conversion factor from seconds to milliseconds. */
    private static final double S_TO_MS = 1000.0;

    /** Reference date for ensuring continuity across GPS week rollover.
     * @since 9.3.1
     */
    private static AtomicReference<DateComponents> rolloverReference = new AtomicReference<DateComponents>(null);

    /** Week number since {@link AbsoluteDate#GPS_EPOCH GPS epoch}. */
    private final int weekNumber;

    /** Number of milliseconds since week start. */
    private final double milliInWeek;

    /** Corresponding date. */
    private final transient AbsoluteDate date;

    /** Build an instance corresponding to a GPS date.
     * <p>
     * GPS dates are provided as a week number starting at {@link AbsoluteDate#GPS_EPOCH GPS epoch}
     * and as a number of milliseconds since week start.
     * </p>
     * <p>
     * Many interfaces provide only the 10 lower bits of the GPS week number, just as it comes from
     * the GPS signal. In other words they use a week number modulo 1024. In order to cope with
     * this, when the week number is smaller than 1024, this constructor assumes a modulo operation
     * has been performed and it will fix the week number according to the reference date set up for
     * handling rollover (see {@link #setRolloverReference(DateComponents) setRolloverReference(reference)}).
     * If the week number is 1024 or larger, it will be used without any correction.
     * </p>
     * @param weekNumber week number, either absolute or modulo 1024
     * @param milliInWeek number of milliseconds since week start
     */
    public GPSDate(final int weekNumber, final double milliInWeek) {

        final int day = (int) FastMath.floor(milliInWeek / (Constants.JULIAN_DAY * S_TO_MS));
        final double secondsInDay = milliInWeek / S_TO_MS - day * Constants.JULIAN_DAY;

        int w = weekNumber;
        DateComponents dc = new DateComponents(DateComponents.GPS_EPOCH, weekNumber * 7 + day);
        if (weekNumber < 1024) {

            DateComponents reference = rolloverReference.get();
            if (reference == null) {
                // lazy setting of a default reference, using end of EOP entries
                final UT1Scale       ut1       = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
                final List<EOPEntry> eop       = ut1.getEOPHistory().getEntries();
                final int            lastMJD   = eop.get(eop.size() - 1).getMjd();
                reference = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, lastMJD);
                rolloverReference.compareAndSet(null, reference);
            }

            // fix GPS week rollover
            while (dc.getJ2000Day() < reference.getJ2000Day() - CYCLE_D / 2) {
                dc = new DateComponents(dc, CYCLE_D);
                w += CYCLE_W;
            }

        }

        this.weekNumber  = w;
        this.milliInWeek = milliInWeek;

        date = new AbsoluteDate(dc, new TimeComponents(secondsInDay), TimeScalesFactory.getGPS());

    }

    /** Build an instance from an absolute date.
     * @param date absolute date to consider
     */
    public GPSDate(final AbsoluteDate date) {

        this.weekNumber  = (int) FastMath.floor(date.durationFrom(AbsoluteDate.GPS_EPOCH) / WEEK_S);
        final AbsoluteDate weekStart = new AbsoluteDate(AbsoluteDate.GPS_EPOCH, WEEK_S * weekNumber);
        this.milliInWeek = date.durationFrom(weekStart) * S_TO_MS;
        this.date        = date;

    }

    /** Set a reference date for ensuring continuity across GPS week rollover.
     * <p>
     * Instance created using the {@link #GPSDate(int, double) GPSDate(weekNumber, milliInWeek)}
     * constructor and with a week number between 0 and 1024 after this method has been called will
     * fix the week number to ensure they correspond to dates between {@code reference - 512 weeks}
     * and {@code reference + 512 weeks}.
     * </p>
     * <p>
     * If this method is never called, a default reference date for rollover will be set using
     * the date of the last known EOP entry retrieved from {@link UT1Scale#getEOPHistory() UT1}
     * time scale.
     * </p>
     * @param reference reference date for GPS week rollover
     * @see #getRolloverReference()
     * @see #GPSDate(int, double)
     * @since 9.3.1
     */
    public static void setRolloverReference(final DateComponents reference) {
        rolloverReference.set(reference);
    }

    /** Get the reference date ensuring continuity across GPS week rollover.
     * @return reference reference date for GPS week rollover
     * @see #setRolloverReference(AbsoluteDate)
     * @see #GPSDate(int, double)
     * @since 9.3.1
     */
    public static DateComponents getRolloverReference() {
        return rolloverReference.get();
    }

    /** Get the week number since {@link AbsoluteDate#GPS_EPOCH GPS epoch}.
     * <p>
     * The week number returned here has been fixed for GPS week rollover, i.e.
     * it may be larger than 1024.
     * </p>
     * @return week number since {@link AbsoluteDate#GPS_EPOCH GPS epoch}
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
        private static final long serialVersionUID = 20180633L;

        /** Week number since {@link AbsoluteDate#GPS_EPOCH GPS epoch}. */
        private final int weekNumber;

        /** Number of milliseconds since week start. */
        private final double milliInWeek;

        /** Simple constructor.
         * @param weekNumber week number since {@link AbsoluteDate#GPS_EPOCH GPS epoch}
         * @param milliInWeek number of milliseconds since week start
         */
        DataTransferObject(final int weekNumber, final double milliInWeek) {
            this.weekNumber  = weekNumber;
            this.milliInWeek = milliInWeek;
        }

        /** Replace the deserialized data transfer object with a {@link GPSDate}.
         * @return replacement {@link GPSDate}
         */
        private Object readResolve() {
            return new GPSDate(weekNumber, milliInWeek);
        }

    }

}
