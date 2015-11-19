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

        // use entry mid-date to locate where to put it
        final double       span    = validityEnd.durationFrom(validityStart);
        final AbsoluteDate midDate = validityStart.shiftedBy(0.5 * span);

        // insert the entry at its chronological location
        cache.add(findIndex(midDate), entry);

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
            if (date.compareTo(entry.getValidityStart()) >= 0 &&
                date.compareTo(entry.getValidityEnd())   <= 0) {
                return entry.getShortPeriodTerms();
            }
        }

        throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                                  date,
                                  cache.get(0).getValidityStart(),
                                  cache.get(cache.size() - 1).getValidityEnd());

    }

    /** Get the index corresponding to a date.
     * @param date test date
     * @return index at which the entry covering the date should be
     */
    private int findIndex(final AbsoluteDate date) {

        if (cache.size() > 0) {

            final AbsoluteDate reference = cache.get(0).getValidityStart();
            final double t = date.durationFrom(reference);

            int iInf = 0;
            final double tInf = 0.0;
            int  iSup = cache.size() - 1;
            final double tSup = cache.get(iSup).getValidityStart().durationFrom(reference);
            while (iSup - iInf > 0) {
                final int iInterp = (int) ((iInf * (tSup - t) + iSup * (t - tInf)) / (tSup - tInf));
                final int iMed    = FastMath.max(iInf, FastMath.min(iInterp, iSup));
                final Entry entry = cache.get(iMed);
                if (date.compareTo(entry.getValidityStart()) < 0) {
                    iSup = iMed - 1;
                } else if (date.compareTo(entry.getValidityEnd()) > 0) {
                    iInf = FastMath.min(iSup, iMed + 1);
                } else {
                    return iMed;
                }
            }

        }

        return 0;

    }

    /** Local class for cached entries. */
    private static class Entry implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151106L;

        /** Start date of the validity period for the instance. */
        private final AbsoluteDate validityStart;

        /** End date of the validity period for the instance. */
        private final AbsoluteDate validityEnd;

        /** Short periods terms. */
        private final List<ShortPeriodTerms> shortPeriodsTerm;

        /** Simple constructor.
         * @param validityStart start date of the validity period for the instance
         * @param validityEnd end date of the validity period for the instance
         * @param shortPeriodTerms short periods terms
         */
        Entry(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
              final List<ShortPeriodTerms> shortPeriodTerms) {
            this.validityStart    = validityStart;
            this.validityEnd      = validityEnd;
            this.shortPeriodsTerm = shortPeriodTerms;
        }

        /** Get the start date of the validity period for the instance.
         * @return start date of the validity period for the instance
         */
        public AbsoluteDate getValidityStart() {
            return validityStart;
        }

        /** Get the end date of the validity period for the instance.
         * @return end date of the validity period for the instance
         */
        public AbsoluteDate getValidityEnd() {
            return validityEnd;
        }

        /** Get the short periods terms.
         * @return short periods terms
         */
        public List<ShortPeriodTerms> getShortPeriodTerms() {
            return shortPeriodsTerm;
        }

    }

}
