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

import org.orekit.errors.OrekitException;
import org.orekit.iers.Leap;
import org.orekit.iers.UTCTAIHistoryFilesLoader;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. These
 * adjustments require introduction of leap seconds.</p>
 * <p>The handling of time <em>during</em> the leap seconds insertion has
 * been adapted from the standard in order to compensate for the lack of
 * support for leap seconds in the standard java {@link java.util.Date}
 * class. We consider the leap is introduced as one clock reset at the
 * end of the leap. For example when a one second leap was introduced
 * between 2005-12-31T23:59:59 UTC and 2006-01-01T00:00:00 UTC, we
 * consider time flowed continuously for one second in UTC time scale
 * from 23:59:59 to 00:00:00 and <em>then</em> a -1 second leap reset
 * the clock to 23:59:59 again, leading to have to wait one second more
 * before 00:00:00 was reached. The standard would have required to have
 * introduced a second corresponding to location 23:59:60, i.e. the
 * last minute of 2005 was 61 seconds long instead of 60 seconds.</p>
 * <p>The OREKIT library retrieves time steps data thanks to the {@link
 * org.orekit.iers.IERSDirectoryCrawler IERSDirectoryCrawler} class.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class UTCScale extends TimeScale {

    /** Time steps. */
    private Leap[] leaps;

    /** Date of the first available known UTC steps. */
    private AbsoluteDate UTCStartDate;

    /** Private constructor for the singleton.
     * @exception OrekitException if the time steps cannot be read
     */
    private UTCScale() throws OrekitException {
        super("UTC");

        // get the time steps from the history file
        // found in the IERS directories hierarchy
        leaps = new UTCTAIHistoryFilesLoader().getTimeSteps();

    }

    /** Get the unique instance of this class.
     * @return the unique instance
     * @exception OrekitException if the time steps cannot be read
     */
    public static TimeScale getInstance() throws OrekitException {
        if (LazyHolder.INSTANCE == null) {
            throw LazyHolder.OREKIT_EXCEPTION;
        }
        return LazyHolder.INSTANCE;
    }

    /** Get the offset to convert locations from {@link TAIScale}  to instance.
     * @param taiTime location of an event in the {@link TAIScale}  time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to taiTime to get a location
     * in instance time scale
     */
    public double offsetFromTAI(final double taiTime) {
        for (int i = 0; i < leaps.length; ++i) {
            final Leap leap = leaps[i];
            if ((taiTime  + (leap.getOffsetAfter() - leap.getStep())) >= leap.getUtcTime()) {
                return leap.getOffsetAfter();
            }
        }
        return 0;
    }

    /** Get the offset to convert locations from instance to {@link TAIScale} .
     * @param instanceTime location of an event in the instance time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to instanceTime to get a location
     * in {@link TAIScale}  time scale
     */
    public double offsetToTAI(final double instanceTime) {
        for (int i = 0; i < leaps.length; ++i) {
            final Leap leap = leaps[i];
            if (instanceTime >= leap.getUtcTime()) {
                return -leap.getOffsetAfter();
            }
        }
        return 0;
    }

    /** Get the date of the first available known UTC steps.
     * @return the start date of the available data
     */
    public AbsoluteDate getStartDate() {
        if (UTCStartDate == null) {
            final AbsoluteDate ref =
                new AbsoluteDate(new ChunkedDate(1970, 1, 1),
                                 new ChunkedTime(0, 0, 0),
                                 this);
            final Leap firstLeap = leaps[leaps.length - 1];
            UTCStartDate = new AbsoluteDate(ref, firstLeap.getUtcTime() - firstLeap.getStep());
        }
        return UTCStartDate;
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all version of java.</p>
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
