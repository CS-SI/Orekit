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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.orekit.errors.OrekitException;

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
 * org.orekit.data.DataDirectoryCrawler DataDirectoryCrawler} class.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class UTCScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = -920132549431081663L;

    /** Reference TAI date. */
    private static final AbsoluteDate TAI_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TAIScale.getInstance());

    /** Time steps. */
    private UTCTAIOffset[] offsets;

    /** Current position. */
    private transient int current;

    /** Private constructor for the singleton.
     */
    private UTCScale() {
    }

    /** Set the time steps from the predefined entries and the history
     * files in IERS folders.
     * @exception OrekitException if the time steps cannot be read
     */
    private synchronized void setTimeSteps() throws OrekitException {

        // gather all entries, both predefined and user supplied
        final SortedMap<DateComponents, Integer> entries = createPredefinedEntries();
        entries.putAll(new UTCTAIHistoryFilesLoader().loadTimeSteps());
        offsets = new UTCTAIOffset[entries.size() + 1];
        current = 0;

        // set up a first entry covering the far past before first leap second
        UTCTAIOffset last = new UTCTAIOffset(AbsoluteDate.PAST_INFINITY, 0, 0);
        offsets[current++] = last;

        // add leap second entries in chronological order
        for (Map.Entry<DateComponents, Integer> entry : entries.entrySet()) {
            final double offset            = entry.getValue().doubleValue();
            final double leap              = offset - last.getOffset();
            final AbsoluteDate taiDayStart = new AbsoluteDate(entry.getKey(), TAIScale.getInstance());
            final AbsoluteDate leapDate    = new AbsoluteDate(taiDayStart, last.getOffset());
            last.setValidityEnd(leapDate);
            last = new UTCTAIOffset(leapDate, leap, offset);
            offsets[current++] = last;
        }

        // set the current index on the last known leap
        --current;

    }

    /** Create predefined entries for UTC-TAI history.
     * <p>The predefined entries guarantee that at least already known values are
     * supported even if user as not set up a proper configuration.</p>
     * @return predefined entries
     * (contains at least all leaps between 1972-01-01 and 2009-01-01)
     */
    private SortedMap<DateComponents, Integer> createPredefinedEntries() {
        final SortedMap<DateComponents, Integer> entries =
            new TreeMap<DateComponents, Integer>();
        entries.put(new DateComponents(1972, 1, 1), 10);
        entries.put(new DateComponents(1972, 7, 1), 11);
        entries.put(new DateComponents(1973, 1, 1), 12);
        entries.put(new DateComponents(1974, 1, 1), 13);
        entries.put(new DateComponents(1975, 1, 1), 14);
        entries.put(new DateComponents(1976, 1, 1), 15);
        entries.put(new DateComponents(1977, 1, 1), 16);
        entries.put(new DateComponents(1978, 1, 1), 17);
        entries.put(new DateComponents(1979, 1, 1), 18);
        entries.put(new DateComponents(1980, 1, 1), 19);
        entries.put(new DateComponents(1981, 7, 1), 20);
        entries.put(new DateComponents(1982, 7, 1), 21);
        entries.put(new DateComponents(1983, 7, 1), 22);
        entries.put(new DateComponents(1985, 7, 1), 23);
        entries.put(new DateComponents(1988, 1, 1), 24);
        entries.put(new DateComponents(1990, 1, 1), 25);
        entries.put(new DateComponents(1991, 1, 1), 26);
        entries.put(new DateComponents(1992, 7, 1), 27);
        entries.put(new DateComponents(1993, 7, 1), 28);
        entries.put(new DateComponents(1994, 7, 1), 29);
        entries.put(new DateComponents(1996, 1, 1), 30);
        entries.put(new DateComponents(1997, 7, 1), 31);
        entries.put(new DateComponents(1999, 1, 1), 32);
        entries.put(new DateComponents(2006, 1, 1), 33);
        entries.put(new DateComponents(2009, 1, 1), 34);
        return entries;
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     * @exception OrekitException if the time steps cannot be read
     */
    public static UTCScale getInstance() throws OrekitException {
        if (LazyHolder.INSTANCE == null) {
            throw LazyHolder.OREKIT_EXCEPTION;
        }
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    public synchronized double offsetFromTAI(final AbsoluteDate date) {
        setCurrent(date);
        return -offsets[current].getOffset();
    }

    /** {@inheritDoc} */
    public synchronized double offsetToTAI(final DateComponents date,
                                           final TimeComponents time) {
        setCurrent(date.getJ2000Day() * 86400.0 + time.getSecondsInDay() - 43200);
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

    /** Change object upon deserialization.
     * <p>Since {@link TimeScale} classes are serializable, they can
     * be deserialized. This class being a singleton, we always replace the
     * read object by the singleton instance at deserialization time.</p>
     * @return the singleton instance
     */
    private Object readResolve() {
        return LazyHolder.INSTANCE;
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final UTCScale INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            UTCScale tmpInstance = null;
            OrekitException tmpException = null;
            try {
                tmpInstance = new UTCScale();
                tmpInstance.setTimeSteps();
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE         = tmpInstance;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyHolder() {
        }

    }

}
