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
package org.orekit.frames;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

/** This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public abstract class AbstractEOPHistory implements Serializable, EOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = -4066489179973636623L;

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** EOP history entries. */
    protected final SortedSet<TimeStamped> entries;

    /** Previous EOP entry. */
    protected EOPEntry previous;

    /** Next EOP entry. */
    protected EOPEntry next;

    /** Offset from previous date. */
    protected double dtP;

    /** Offset to next date. */
    protected double dtN;

    // CHECKSTYLE: resume VisibilityModifierCheck

    /** Simple constructor.
     */
    protected AbstractEOPHistory() {
        entries = new TreeSet<TimeStamped>(new ChronologicalComparator());
    }

    /** {@inheritDoc} */
    public Iterator<TimeStamped> iterator() {
        return entries.iterator();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getStartDate() {
        return entries.first().getDate();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getEndDate() {
        return entries.last().getDate();
    }

    /** {@inheritDoc} */
    public synchronized double getUT1MinusUTC(final AbsoluteDate date) {
        if (prepareInterpolation(date)) {
            synchronized (this) {
                return (dtP * next.getUT1MinusUTC() + dtN * previous.getUT1MinusUTC()) /
                    (dtP + dtN);
            }
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    public double getLOD(final AbsoluteDate date) {
        if (prepareInterpolation(date)) {
            synchronized (this) {
                return (dtP * next.getLOD() + dtN * previous.getLOD()) /
                    (dtP + dtN);
            }
        } else {
            return 0;
        }
    }

    /** Prepare interpolation between two entries.
     * @param  date target date
     * @return true if there are entries bracketing the target date
     */
    protected synchronized boolean prepareInterpolation(final AbsoluteDate date) {

        // compute offsets assuming the current selection brackets the date
        dtP = (previous == null) ? -1.0 : date.durationFrom(previous.getDate());
        dtN = (next == null) ? -1.0 : next.getDate().durationFrom(date);

        // check if bracketing was correct
        if ((dtP < 0) || (dtN < 0)) {

            // bad luck, we need to recompute brackets
            if (!selectBracketingEntries(date)) {
                // the specified date is outside of supported range
                return false;
            }

            // recompute offsets
            dtP = date.durationFrom(previous.getDate());
            dtN = next.getDate().durationFrom(date);

        }

        return true;

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
            next     = (EOPEntry) (entries.tailSet(date).first());
            previous = (EOPEntry) (entries.headSet(next).last());
            return true;
        } catch (NoSuchElementException nsee) {
            previous = null;
            next     = null;
            return false;
        }
    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in seconds)
     * @exception OrekitException if there are holes in the data sequence
     */
    public void checkEOPContinuity(final double maxGap) throws OrekitException {
        TimeStamped preceding = null;
        for (final TimeStamped current : entries) {

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getDate().durationFrom(preceding.getDate())) > maxGap)) {
                throw new OrekitException("missing Earth Orientation Parameters between {0} and {1}",
                                          preceding.getDate(), current.getDate());

            }

            // prepare next iteration
            preceding = current;

        }
    }

}
