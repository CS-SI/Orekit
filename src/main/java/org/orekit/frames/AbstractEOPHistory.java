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
import org.orekit.utils.TimeStampedEntry;

/** This class holds any kind of Earth Orientation Parameter data throughout a large time range.
 * It is a singleton since it handles voluminous data.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public abstract class AbstractEOPHistory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 9141543606409905199L;

    /** Earth Orientation Parameters (either IAU1980 or IAU2000). */
    private final SortedSet<TimeStamped> eop;

    /** Previous EOP entry. */
    private TimeStampedEntry previous;

    /** Next EOP entry. */
    private TimeStampedEntry next;

   /** Private constructor for the singleton.
     * @param ficEOP05C04  name of the EOP05C04 file to load
     * @param ficBulletinB name of the BulletinB file to load
     * @exception OrekitException if there is a problem while reading IERS data
     */
    protected AbstractEOPHistory(final String ficEOP05C04,
                                 final String ficBulletinB) throws OrekitException {

        // set up a date-ordered set, able to use either
        // TimeStampedEntry or AbsoluteDate instances
        // (beware to use AbsoluteDate ONLY as arguments to
        // headSet or tailSet and NOT to add them in the set)
        eop = new TreeSet<TimeStamped>(new ChronologicalComparator());

        // consider first the more accurate EOP 05 C04 entries
        final boolean eop05c04Loaded = new EOP05C04FilesLoader(ficEOP05C04, eop).loadEOP();

        // add the final values from bulletin B entries for new dates
        // (if duplicated dates occur, the existing data will be preserved)
        final boolean bulletinBLoaded = new BulletinBFilesLoader(ficBulletinB, eop).loadEOP();

        if (!(eop05c04Loaded || bulletinBLoaded)) {
            throw new OrekitException("no Earth Orientation Parameters loaded");
        }

        // check the continuity of the loaded data
        checkEOPContinuity(5 * 86400.0);

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
                                          preceding, current);

            }

            // prepare next iteration
            preceding = current;

        }
    }

    /** Get the interpolated value at some date for an indexed field of the entry.
     * @param  date target date
     * @param  index index of the concerned field
     * @return the interpolated value for the indexed field
     */
    protected synchronized double getInterpolatedField(final AbsoluteDate date,
                                                       final int index) {

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

        // interpolate value
        return (dtP * next.getField(index) + dtN * previous.getField(index)) /
               (dtP + dtN);

    }

    /** Select the entries bracketing a specified date.
     * <p>If the date is either before the first entry or after the last entry,
     * previous and next will be set to null.</p>
     * @param  date target date
     * @return true if the date was found in the tables
     */
    protected boolean selectBracketingEntries(final AbsoluteDate date) {
        try {
            // select the bracketing elements
            next     = (TimeStampedEntry) (eop.tailSet(date).first());
            previous = (TimeStampedEntry) (eop.headSet(next).last());
            return true;
        } catch (NoSuchElementException nsee) {
            previous = null;
            next     = null;
            return false;
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

}
