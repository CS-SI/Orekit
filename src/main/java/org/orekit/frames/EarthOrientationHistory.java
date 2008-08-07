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
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.iers.BulletinBFilesLoader;
import org.orekit.iers.EOP05C04FilesLoader;
import org.orekit.iers.EarthOrientationParameters;
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
    private TreeSet<TimeStamped> eop = null;

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
        new EOP05C04FilesLoader(eop).loadEOP();

        // add the final values from bulletin B entries for new dates
        // (if duplicated dates occur, the existing data will be preserved)
        new BulletinBFilesLoader(eop).loadEOP();

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
            if ((preceding != null) && ((current.getDate().minus(preceding.getDate())) > maxGap)) {
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
        if (selectBracketingEntries(date)) {
            final double dtP = date.minus(previous.getDate());
            final double dtN = next.getDate().minus(date);
            return (dtP * next.getUT1MinusUTC() + dtN * previous.getUT1MinusUTC()) / (dtN + dtP);
        }
        return 0;
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the EOP C 04 files. It
     * is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    protected PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        if (selectBracketingEntries(date)) {
            final double dtP    = date.minus(previous.getDate());
            final double dtN    = next.getDate().minus(date);
            final double sum    = dtN + dtP;
            final double coeffP = dtN / sum;
            final double coeffN = dtP / sum;
            return new PoleCorrection(coeffP * previous.getPoleCorrection().getXp() +
                                      coeffN * next.getPoleCorrection().getXp(),
                                      coeffP * previous.getPoleCorrection().getYp() +
                                      coeffN * next.getPoleCorrection().getYp());
        }
        return PoleCorrection.NULL_CORRECTION;
    }

    /** Select the entries bracketing a specified date.
     * @param  date target date
     * @return true if the date was found in the tables
     */
    private boolean selectBracketingEntries(final AbsoluteDate date) {

        // don't search if the cached selection is fine
        if ((previous != null) && (date.minus(previous.getDate()) >= 0) &&
            (next != null) && (date.minus(next.getDate()) < 0)) {
            // the current selection is already good
            return true;
        }

        // select the bracketing elements (may be null)
        final SortedSet<TimeStamped> head = eop.headSet(date);
        previous = (EarthOrientationParameters) (head.isEmpty() ? null : head.last());
        final SortedSet<TimeStamped> tail = eop.tailSet(date);
        next     = (EarthOrientationParameters) (tail.isEmpty() ? null : tail.first());

        return (previous != null) && (next != null);

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
