/* Copyright 2002-2024 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

import java.util.List;

/** A trimmer for externally stored chronologically sorted lists.
 * @author Evan Ward
 * @author Vincent Cucchietti
 * @since 12.1
 */
public class FieldSortedListTrimmer {

    /** Size list to return from {@link #getNeighborsSubList(FieldAbsoluteDate, List)}. */
    private final int neighborsSize;

    /**
     * Create a new cache with the given neighbors size and data.
     *
     * @param neighborsSize size of the list returned from {@link #getNeighborsSubList(FieldAbsoluteDate, List)}.
     */
    public FieldSortedListTrimmer(final int neighborsSize) {
        if (neighborsSize < 1) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL,
                                                     neighborsSize, 1);
        }
        // assign instance variables
        this.neighborsSize = neighborsSize;
    }

    /** Get size of the list returned from {@link #getNeighborsSubList(FieldAbsoluteDate, List)}.
     * @return size of the list returned from {@link #getNeighborsSubList(FieldAbsoluteDate, List)}
     */
    public int getNeighborsSize() {
        return neighborsSize;
    }

    /** Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array will
     * be balanced with half the points before central date and half the points
     * after it (depending on n parity, of course). If the central date is near
     * the boundary, then the returned array will be unbalanced and will contain
     * only the n earliest (or latest) entries. A typical example of the later
     * case is leap seconds cache, since the number of leap seconds cannot be
     * arbitrarily increased.
     * </p>
     * @param <T>  the type of data
     * @param <K>  the type of the field elements
     * @param central central date
     * @param data complete list of entries (must be chronologically sorted)
     * @return entries surrounding the specified date (sublist of {@code data})
     */
    public <T extends FieldTimeStamped<K>, K extends CalculusFieldElement<K>>
        List<T> getNeighborsSubList(final FieldAbsoluteDate<K> central, final List<T> data) {

        if (neighborsSize > data.size()) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA, data.size());
        }

        // Find central index
        final int i = findIndex(central, data);

        // Check index in the range of the data
        if (i < 0) {
            final FieldAbsoluteDate<K> earliest = data.get(0).getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                earliest, central, earliest.durationFrom(central).getReal());
        }
        else if (i >= data.size()) {
            final FieldAbsoluteDate<K> latest = data.get(data.size() - 1).getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                                                latest, central, central.durationFrom(latest).getReal());
        }

        // Force unbalanced range if necessary
        int start = FastMath.max(0, i - (neighborsSize - 1) / 2);
        final int end = FastMath.min(data.size(), start + neighborsSize);
        start = end - neighborsSize;

        // Return list without copying
        return data.subList(start, end);
    }

    /**
     * Find the index, i, to {@code data} such that {@code data[i] <= t} and
     * {@code data[i+1] > t} if {@code data[i+1]} exists.
     *
     * @param <T>  the type of data
     * @param <K>  the type of the field elements
     * @param t the time
     * @param data complete list of entries (must be chronologically sorted)
     *
     * @return the index of the data at or just before {@code t}, {@code -1} if {@code t} is before the first entry, or
     * {@code data.size()} if {@code t} is after the last entry.
     */
    private <T extends FieldTimeStamped<K>, K extends CalculusFieldElement<K>>
        int findIndex(final FieldAbsoluteDate<K> t, final List<T> data) {
        // left bracket of search algorithm
        int iInf  = 0;
        K   dtInf = t.durationFrom(data.get(0));
        if (dtInf.getReal() < 0) {
            // before first entry
            return -1;
        }

        // right bracket of search algorithm
        int iSup  = data.size() - 1;
        K   dtSup = t.durationFrom(data.get(data.size() - 1));
        if (dtSup.getReal() > 0) {
            // after last entry
            return data.size();
        }

        // search entries, using linear interpolation
        // this should take only 2 iterations for near linear entries (most frequent use case)
        // regardless of the number of entries
        // this is much faster than binary search for large number of entries
        while (iSup - iInf > 1) {
            final int iInterp = (int) FastMath.rint(dtSup.multiply(iInf).subtract(dtInf.multiply(iSup)).divide(dtSup.subtract(dtInf)).getReal());
            final int iMed    = FastMath.max(iInf + 1, FastMath.min(iInterp, iSup - 1));
            final K   dtMed   = t.durationFrom(data.get(iMed).getDate());
            if (dtMed.getReal() < 0) {
                iSup  = iMed;
                dtSup = dtMed;
            } else {
                iInf  = iMed;
                dtInf = dtMed;
            }
        }

        // at this point data[iInf] <= t <= data[iSup], but the javadoc for this method
        // says the upper bound is exclusive, so check for equality to make a half open
        // interval.
        if (dtSup.getReal() == 0.0) {
            return iSup;
        }

        return iInf;

    }

}
