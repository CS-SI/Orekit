/* Copyright 2002-2011 CS Communication & Systèmes
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

import java.util.Map;
import java.util.SortedMap;

import org.orekit.errors.OrekitException;
import org.orekit.utils.Constants;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. Before 1972,
 * these adjustments were piecewise linear offsets. Since 1972, these adjustments
 * are piecewise constant offsets, which require introduction of leap seconds.</p>
 * <p>Leap seconds are always inserted as additional seconds at the last minute
 * of the day, pushing the next day forward. Such minutes are therefore more
 * than 60 seconds long. In theory, there may be seconds removal instead of seconds
 * insertion, but up to now (2010) it has never been used. As an example, when a
 * one second leap was introduced at the end of 2005, the UTC time sequence was
 * 2005-12-31T23:59:59 UTC, followed by 2005-12-31T23:59:60 UTC, followed by
 * 2006-01-01T00:00:00 UTC.</p>
 * <p>The OREKIT library retrieves the post-1972 constant time steps data thanks
 * to the {@link org.orekit.data.DataProvidersManager DataProvidersManager} class.
 * The linear models used between 1961 and 1972 are built-in in the class itself.</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor. Every call to {@link TimeScalesFactory#getUTC()}
 * will create a new {@link UTCScale} instance, sharing the UTC-TAI offset table between
 * all instances.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class UTCScale implements TimeScale, Cloneable {

    /** Serializable UID. */
    private static final long serialVersionUID = 1096634108833538374L;

    /** Time steps. */
    private UTCTAIOffset[] offsets;

    /** Current position. */
    private int current;

    /** Package private constructor for the factory.
     * Used to create the prototype instance of this class that is used to
     * clone all subsequent instances of {@link UTCScale}. Initializes the offset
     * table that is shared among all instances.
     * @param entries user supplied entries
     */
    UTCScale(final SortedMap<DateComponents, Integer> entries) {

        offsets = new UTCTAIOffset[entries.size() + 14];
        current = 0;

        // set up a first entry covering the far past before first offset
        offsets[current] = new UTCTAIOffset(AbsoluteDate.PAST_INFINITY, Integer.MIN_VALUE, 0, 0);

        // set up the linear offsets used between 1961-01-01 and 1971-12-31
        // excerpt from UTC-TAI.history file:
        //  1961  Jan.  1 - 1961  Aug.  1     1.422 818 0s + (MJD - 37 300) x 0.001 296s
        //        Aug.  1 - 1962  Jan.  1     1.372 818 0s +        ""
        //  1962  Jan.  1 - 1963  Nov.  1     1.845 858 0s + (MJD - 37 665) x 0.001 123 2s
        //  1963  Nov.  1 - 1964  Jan.  1     1.945 858 0s +        ""
        //  1964  Jan.  1 -       April 1     3.240 130 0s + (MJD - 38 761) x 0.001 296s
        //        April 1 -       Sept. 1     3.340 130 0s +        ""
        //        Sept. 1 - 1965  Jan.  1     3.440 130 0s +        ""
        //  1965  Jan.  1 -       March 1     3.540 130 0s +        ""
        //        March 1 -       Jul.  1     3.640 130 0s +        ""
        //        Jul.  1 -       Sept. 1     3.740 130 0s +        ""
        //        Sept. 1 - 1966  Jan.  1     3.840 130 0s +        ""
        //  1966  Jan.  1 - 1968  Feb.  1     4.313 170 0s + (MJD - 39 126) x 0.002 592s
        //  1968  Feb.  1 - 1972  Jan.  1     4.213 170 0s +        ""
        addOffsetModel(new DateComponents(1961,  1, 1), 37300, 1.4228180, 0.0012960);
        addOffsetModel(new DateComponents(1961,  8, 1), 37300, 1.3728180, 0.0012960);
        addOffsetModel(new DateComponents(1962,  1, 1), 37665, 1.8458580, 0.0011232);
        addOffsetModel(new DateComponents(1963, 11, 1), 37665, 1.9458580, 0.0011232);
        addOffsetModel(new DateComponents(1964,  1, 1), 38761, 3.2401300, 0.0012960);
        addOffsetModel(new DateComponents(1964,  4, 1), 38761, 3.3401300, 0.0012960);
        addOffsetModel(new DateComponents(1964,  9, 1), 38761, 3.4401300, 0.0012960);
        addOffsetModel(new DateComponents(1965,  1, 1), 38761, 3.5401300, 0.0012960);
        addOffsetModel(new DateComponents(1965,  3, 1), 38761, 3.6401300, 0.0012960);
        addOffsetModel(new DateComponents(1965,  7, 1), 38761, 3.7401300, 0.0012960);
        addOffsetModel(new DateComponents(1965,  9, 1), 38761, 3.8401300, 0.0012960);
        addOffsetModel(new DateComponents(1966,  1, 1), 39126, 4.3131700, 0.0025920);
        addOffsetModel(new DateComponents(1968,  2, 1), 39126, 4.2131700, 0.0025920);

        // add leap second entries in chronological order
        for (Map.Entry<DateComponents, Integer> entry : entries.entrySet()) {
            addOffsetModel(entry.getKey(), 0, entry.getValue(), 0);
        }

    }

    /** Package private constructor for the {@link #clone()} method.
     * Creates a new {@link UTCScale} instance as clone of the given prototype. The
     * table containing UTC - TAI offsets is shared among all instances.
     * @param prototype the prototype {@link UTCScale} instance to be cloned
     */
    UTCScale(final UTCScale prototype) {
        this.offsets = prototype.offsets;
        this.current = prototype.current;
    }

    /** Add an offset model.
     * <p>
     * This method <em>must</em> be called in chronological order.
     * </p>
     * @param date date of the constant offset model start
     * @param mjdRef reference date of the linear model as a modified julian day
     * @param offset offset at reference date in seconds (TAI minus UTC)
     * @param slope offset slope in seconds per UTC day (TAI minus UTC / dUTC)
     */
    private synchronized void addOffsetModel(final DateComponents date, final int mjdRef,
                                             final double offset, final double slope) {

        final TimeScale tai = TimeScalesFactory.getTAI();

        // start of the leap
        final UTCTAIOffset previous    = offsets[current];
        final double previousOffset    = previous.getOffset(date, TimeComponents.H00);
        final AbsoluteDate leapStart   = new AbsoluteDate(date, tai).shiftedBy(previousOffset);

        // end of the leap
        final double startOffset       = offset + slope * (date.getMJD() - mjdRef);
        final AbsoluteDate leapEnd     = new AbsoluteDate(date, tai).shiftedBy(startOffset);

        // leap computed at leap start and in UTC scale
        final double normalizedSlope   = slope / Constants.JULIAN_DAY;
        final double leap              = leapEnd.durationFrom(leapStart) / (1 + normalizedSlope);

        previous.setValidityEnd(leapStart);
        offsets[++current] = new UTCTAIOffset(leapStart, date.getMJD(), leap, offset, mjdRef, normalizedSlope);

    }

    /** Get a new instance of this class.
     * @return a new {@link UTCScale} instance
     * @exception OrekitException if the leap seconds cannot be read
     * @deprecated since 4.1 replaced by {@link TimeScalesFactory#getUTC()}
     */
    @Deprecated
    public static UTCScale getInstance() throws OrekitException {
        return TimeScalesFactory.getUTC();
    }

    /** {@inheritDoc} */
    public double offsetFromTAI(final AbsoluteDate date) {
        return -getCurrent(date).getOffset(date);
    }

    /** {@inheritDoc} */
    public double offsetToTAI(final DateComponents date,
                              final TimeComponents time) {
        return getCurrent(date).getOffset(date, time);
    }

    /** {@inheritDoc} */
    public String getName() {
        return "UTC";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

    /** Get the date of the first known leap second.
     * @return date of the first known leap second
     */
    public AbsoluteDate getFirstKnownLeapSecond() {
        return offsets[0].getValidityEnd();
    }

    /** Get the date of the last known leap second.
     * @return date of the last known leap second
     */
    public AbsoluteDate getLastKnownLeapSecond() {
        return offsets[offsets.length - 1].getDate();
    }

    /** Check if date is within a leap second introduction.
     * @param date date to check
     * @return true if time is within a leap second introduction
     */
    public boolean insideLeap(final AbsoluteDate date) {
        return date.compareTo(getCurrent(date).getValidityStart()) < 0;
    }

    /** Get the value of the previous leap.
     * @param date date to check
     * @return value of the previous leap
     */
    public double getLeap(final AbsoluteDate date) {
        return getCurrent(date).getLeap();
    }

    /** Get the offset model at some specified date.
     * @param date current date
     * @return offset model valid at the specified date
     */
    private synchronized UTCTAIOffset getCurrent(final AbsoluteDate date) {
        while ((current > 0) && (date.compareTo(offsets[current].getValidityStart()) < 0)) {
            --current;
        }
        while ((current < (offsets.length - 1)) && (date.compareTo(offsets[current].getValidityEnd()) >= 0)) {
            ++current;
        }
        return offsets[current];
    }

    /** Get the offset model at some specified date.
     * @param date current date
     * @return offset model valid at the specified date
     */
    private synchronized UTCTAIOffset getCurrent(final DateComponents date) {
        final int mjd = date.getMJD();
        while ((current > 0) && (mjd < offsets[current].getMJD())) {
            --current;
        }
        while ((current < (offsets.length - 1)) && (mjd >= offsets[current + 1].getMJD())) {
            ++current;
        }
        return offsets[current];
    }

    /** Get a clone of this {@link UTCScale} instance.
     * @return a clone of this instance
     * @exception CloneNotSupportedException required by the {@link Cloneable} interface
     *            but it is not actually thrown
     */
    protected UTCScale clone() throws CloneNotSupportedException {
        return new UTCScale(this);
    }

}
