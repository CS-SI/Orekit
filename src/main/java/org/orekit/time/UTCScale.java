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

import java.util.Map;
import java.util.SortedMap;

import org.orekit.errors.OrekitException;
import org.orekit.utils.Constants;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. These
 * adjustments require introduction of leap seconds.</p>
 * <p>Leap seconds are always inserted as additional seconds at the last minute
 * of the day, pushing the next day forward. Such minutes are therefore more
 * than 60 seconds long. As an example, when a one second leap was introduced
 * at the end of 2005, the UTC time sequence was 2005-12-31T23:59:59 UTC,
 * followed by 2005-12-31T23:59:60 UTC, followed by 2006-01-01T00:00:00 UTC.</p>
 * <p>The OREKIT library retrieves time steps data thanks to the {@link
 * org.orekit.data.DataProvidersManager DataProvidersManager} class.</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class UTCScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 1096634108833538374L;

    /** Reference TAI date. */
    private static final AbsoluteDate TAI_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** Time steps. */
    private UTCTAIOffset[] offsets;

    /** Current position. */
    private int current;

    /** Package private constructor for the factory.
     * @param entries user supplied entries
     */
    UTCScale(final SortedMap<DateComponents, Integer> entries) {

        offsets = new UTCTAIOffset[entries.size() + 1];
        current = 0;

        // set up a first entry covering the far past before first leap second
        UTCTAIOffset last = new UTCTAIOffset(AbsoluteDate.PAST_INFINITY, 0, 0);
        offsets[current++] = last;

        // add leap second entries in chronological order
        for (Map.Entry<DateComponents, Integer> entry : entries.entrySet()) {
            final double offset            = entry.getValue().doubleValue();
            final double leap              = offset - last.getOffset();
            final AbsoluteDate taiDayStart = new AbsoluteDate(entry.getKey(), TimeScalesFactory.getTAI());
            final AbsoluteDate leapDate    = taiDayStart.shiftedBy(last.getOffset());
            last.setValidityEnd(leapDate);
            last = new UTCTAIOffset(leapDate, leap, offset);
            offsets[current++] = last;
        }

        // set the current index on the last known leap
        --current;

    }

    /** Get the unique instance of this class.
     * @return the unique instance
     * @exception OrekitException if the leap seconds cannot be read
     * @deprecated since 4.1 replaced by {@link TimeScalesFactory#getUTC()}
     */
    @Deprecated
    public static UTCScale getInstance() throws OrekitException {
        return TimeScalesFactory.getUTC();
    }

    /** {@inheritDoc} */
    public synchronized double offsetFromTAI(final AbsoluteDate date) {
        setCurrent(date);
        return -offsets[current].getOffset();
    }

    /** {@inheritDoc} */
    public synchronized double offsetToTAI(final DateComponents date,
                                           final TimeComponents time) {
        setCurrent(date.getJ2000Day() * Constants.JULIAN_DAY + time.getSecondsInDay() - Constants.JULIAN_DAY / 2);
        return offsets[current].getOffset();
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
    public synchronized AbsoluteDate getFirstKnownLeapSecond() {
        return offsets[0].getValidityEnd();
    }

    /** Get the date of the last known leap second.
     * @return date of the last known leap second
     */
    public synchronized AbsoluteDate getLastKnownLeapSecond() {
        return offsets[offsets.length - 1].getDate();
    }

    /** Check if date is within a leap second introduction.
     * @param date date to check
     * @return true if time is within a leap second introduction
     */
    public synchronized boolean insideLeap(final AbsoluteDate date) {
        setCurrent(date);
        return date.compareTo(offsets[current].getValidityStart()) < 0;
    }

    /** Get the value of the previous leap.
     * @param date date to check
     * @return value of the previous leap
     */
    public synchronized double getLeap(final AbsoluteDate date) {
        setCurrent(date);
        return offsets[current].getLeap();
    }

    /** Set the current index.
     * @param date current date
     */
    private synchronized void setCurrent(final AbsoluteDate date) {
        while (date.compareTo(offsets[current].getValidityStart()) < 0) {
            --current;
        }
        while (date.compareTo(offsets[current].getValidityEnd()) >= 0) {
            ++current;
        }
    }

    /** Set the current index.
     * @param utcTime location of an event in the utc time scale
     * as a seconds index starting at 2000-01-01T12:00:00
     */
    private synchronized void setCurrent(final double utcTime) {
        while (offsets[current].getValidityStart().durationFrom(TAI_REFERENCE) >
               (utcTime + offsets[current].getOffset())) {
            --current;
        }
        while (offsets[current].getValidityEnd().durationFrom(TAI_REFERENCE) <=
               (utcTime + offsets[current].getOffset())) {
            ++current;
        }
    }

}
