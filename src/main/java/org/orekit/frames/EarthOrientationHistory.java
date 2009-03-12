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
package org.orekit.frames;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

/** This class holds Earth Orientation data throughout a large time range.
 * It is a singleton since it handles voluminous data.
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @version $Revision$ $Date$
 */
public class EarthOrientationHistory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 9141543606409905199L;

    /** Earth Orientation Parameters. */
    private SortedSet<TimeStamped> eop = null;

    /** Previous EOP entry. */
    private EarthOrientationParameters previous;

    /** Next EOP entry. */
    private EarthOrientationParameters next;

   /** Private constructor for the singleton.
     * @exception OrekitException if there is a problem while reading IERS data
     */
    private EarthOrientationHistory() throws OrekitException {

        // set up a date-ordered set, able to use either
        // EarthOrientationParameters or AbsoluteDate instances
        // (beware to use AbsoluteDate ONLY as arguments to
        // headSet or tailSet and NOT to add them in the set)
        eop = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());

        // consider first the more accurate EOP 05 C04 entries
        final boolean eop05c04Loaded = new EOP05C04FilesLoader(eop).loadEOP();

        // add the final values from bulletin B entries for new dates
        // (if duplicated dates occur, the existing data will be preserved)
        final boolean bulletinBLoaded = new BulletinBFilesLoader(eop).loadEOP();

        if (!(eop05c04Loaded || bulletinBLoaded)) {
            throw new OrekitException("no Earth Orientation Parameters loaded", new Object[0]);
        }

        // check the continuity of the loaded data
        checkEOPContinuity(5 * 86400.0);

    }

    /** Get the singleton instance.
     * @return the unique dated eop reader instance.
     * @exception OrekitException when there is a problem while reading IERS data
     */
    public static EarthOrientationHistory getInstance() throws OrekitException {
        if (LazyHolder.INSTANCE == null) {
            throw LazyHolder.OREKIT_EXCEPTION;
        }
        return LazyHolder.INSTANCE;
    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in seconds)
     * @exception OrekitException if there are holes in the data sequence
     */
    private void checkEOPContinuity(final double maxGap) throws OrekitException {
        TimeStamped preceding = null;
        for (final TimeStamped current : eop) {

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getDate().durationFrom(preceding.getDate())) > maxGap)) {
                throw new OrekitException("missing Earth Orientation Parameters between {0} and {1}",
                                          new Object[] {
                                              preceding, current
                                          });

            }

            // prepare next iteration
            preceding = current;

        }
    }

    /** Get the date of the first available Earth Orientation Parameters.
     * @return the start date of the available data
     */
    public AbsoluteDate getStartDate() {
        return eop.first().getDate();
    }

    /** Get the date of the last available Earth Orientation Parameters.
     * @return the end date of the available data
     */
    public AbsoluteDate getEndDate() {
        return eop.last().getDate();
    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the EOP C 04 files. It
     * is smoothed data.</p>
     * @param date date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    protected double getUT1MinusUTC(final AbsoluteDate date) {

        // compute offsets assuming the current selection brackets the date
        double dtP = (previous == null) ? -1.0 : date.durationFrom(previous.getDate());
        double dtN = (next == null) ? -1.0 : next.getDate().durationFrom(date);

        // check if bracketing was correct
        if ((dtP < 0) || (dtN < 0)) {

            // bad luck, we need to recompute brackets
            if (!selectBracketingEntries(date)) {
                // the specified date is outside of supported range
                return 0;
            }

            // recompute offsets
            dtP = date.durationFrom(previous.getDate());
            dtN = next.getDate().durationFrom(date);

        }

        // interpolate UT1 - UTC
        return (dtP * next.getUT1MinusUTC() + dtN * previous.getUT1MinusUTC()) /
               (dtP + dtN);

    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the EOP C 04 files. It
     * is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    protected PoleCorrection getPoleCorrection(final AbsoluteDate date) {

        // compute offsets assuming the current selection brackets the date
        double dtP = (previous == null) ? -1.0 : date.durationFrom(previous.getDate());
        double dtN = (next == null) ? -1.0 : next.getDate().durationFrom(date);

        // check if bracketing was correct
        if ((dtP < 0) || (dtN < 0)) {

            // bad luck, we need to recompute brackets
            if (!selectBracketingEntries(date)) {
                // the specified date is outside of supported range
                return PoleCorrection.NULL_CORRECTION;
            }

            // recompute offsets
            dtP = date.durationFrom(previous.getDate());
            dtN = next.getDate().durationFrom(date);

        }

        // interpolate pole correction
        final PoleCorrection pCorr = previous.getPoleCorrection();
        final PoleCorrection nCorr = next.getPoleCorrection();
        final double sum = dtP + dtN;
        return new PoleCorrection((dtP * nCorr.getXp() + dtN * pCorr.getXp()) / sum,
                                  (dtP * nCorr.getYp() + dtN * pCorr.getYp()) / sum);

    }

    /** Select the entries bracketing a specified date.
     * <p>If the date is either before the first entry or after the last entry,
     * previous and next will be set to null.</p>
     * @param  date target date
     * @return true if the date was found in the tables
     */
    private boolean selectBracketingEntries(final AbsoluteDate date) {
        try {
            // select the bracketing elements
            next     = (EarthOrientationParameters) (eop.tailSet(date).first());
            previous = (EarthOrientationParameters) (eop.headSet(next).last());
            return true;
        } catch (NoSuchElementException nsee) {
            previous = null;
            next     = null;
            return false;
        }
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final EarthOrientationHistory INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            EarthOrientationHistory tmpInstance = null;
            OrekitException tmpException = null;
            try {
                tmpInstance = new EarthOrientationHistory();
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE        = tmpInstance;
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
