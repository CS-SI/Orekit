/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Cache for short period terms.
 * @see DSSTForceModel
 * @author Luc Maisonobe
 * @since 7.1
 */
public class CachedShortPeriodTerms implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20151106L;

    /** Cached entries. */
    private final List<Entry> cache;

    /** Simple constructor.
     */
    public CachedShortPeriodTerms() {
        cache = new ArrayList<Entry>();
    }

    /** Add an entry.
     * @param validityStart start date of the validity period for the instance
     * @param validityEnd end date of the validity period for the instance
     * @param shortPeriodTerms short periods terms
     */
    public void addEntry(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                         final List<ShortPeriodTerms> shortPeriodTerms) {

        // create the entry
        final Entry entry = new Entry(validityStart, validityEnd, shortPeriodTerms);

        // insert the entry at its chronological location
        final int index = findIndex(entry.getDate());
        if (index >= cache.size()) {
            cache.add(entry);
        } else {
            final Entry existing = cache.get(index);
            if (existing.getEarliest().compareTo(entry.getDate()) > 0) {
                cache.add(index, entry);
            } else {
                cache.add(index + 1, entry);
            }
        }

    }

    /** Get the entry covering a date.
     * @param date test date
     * @return short period terms covering the date
     * @exception OrekitException if no cached entries cover the date
     */
    public List<ShortPeriodTerms> getShortPeriodTerms(final AbsoluteDate date)
        throws OrekitException {

        if (cache.isEmpty()) {
            throw new OrekitException(OrekitMessages.NO_CACHED_ENTRIES);
        }

        // tentative index
        final int index = findIndex(date);

        // check if we have found an entry
        if (index < cache.size()) {
            final Entry entry = cache.get(index);
            if (date.compareTo(entry.getEarliest()) >= 0 &&
                date.compareTo(entry.getLatest())   <= 0) {
                return entry.getShortPeriodTerms();
            }
        }

        throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                                  date,
                                  cache.get(0).getEarliest(),
                                  cache.get(cache.size() - 1).getLatest());

    }

    /** Get the index corresponding to a date.
     * @param date test date
     * @return index at which the entry covering the date should be
     */
    private int findIndex(final AbsoluteDate date) {

        if (cache.size() > 0) {

            final AbsoluteDate reference = cache.get(0).getDate();
            final double t = date.durationFrom(reference);

            int iInf = 0;
            final double tInf = 0.0;
            int  iSup = cache.size() - 1;
            final double tSup = cache.get(iSup).getDate().durationFrom(reference);
            while (iSup - iInf > 0) {
                final int iInterp = (int) ((iInf * (tSup - t) + iSup * (t - tInf)) / (tSup - tInf));
                final int iMed    = FastMath.max(iInf, FastMath.min(iInterp, iSup));
                final Entry entry = cache.get(iMed);
                if (date.compareTo(entry.getEarliest()) < 0) {
                    iSup = iMed - 1;
                } else if (date.compareTo(entry.getLatest()) > 0) {
                    iInf = FastMath.min(iSup, iMed + 1);
                } else {
                    return iMed;
                }
            }

            return iInf;

        }

        return 0;

    }

    /** Local class for cached entries. */
    private static class Entry implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151106L;

        /** Mid date of the validity period for the instance. */
        private final AbsoluteDate midDate;

        /** Earliest date covered by the entry. */
        private final AbsoluteDate earliest;

        /** Latest date covered by the entry. */
        private final AbsoluteDate latest;

        /** Short periods terms. */
        private final List<ShortPeriodTerms> shortPeriodsTerm;

        /** Simple constructor.
         * @param validityStart start date of the validity period for the instance
         * @param validityEnd end date of the validity period for the instance
         * @param shortPeriodTerms short periods terms
         */
        Entry(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
              final List<ShortPeriodTerms> shortPeriodTerms) {
            final double deltaT = validityEnd.durationFrom(validityStart);
            this.midDate        = validityStart.shiftedBy(0.5 * deltaT);
            if (deltaT >= 0) {
                earliest = validityStart;
                latest   = validityEnd;
            } else {
                earliest = validityEnd;
                latest   = validityStart;
            }
            this.shortPeriodsTerm = shortPeriodTerms;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getDate() {
            return midDate;
        }

        /** Get the earliest date covered by the entry.
         * @return earliest date covered by the entry
         */
        public AbsoluteDate getEarliest() {
            return earliest;
        }

        /** Get the latest date covered by the entry.
         * @return latest date covered by the entry
         */
        public AbsoluteDate getLatest() {
            return latest;
        }

        /** Get the short periods terms.
         * @return short periods terms
         */
        public List<ShortPeriodTerms> getShortPeriodTerms() {
            return shortPeriodsTerm;
        }

    }

}
