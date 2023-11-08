/* Contributed in the public domain.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

/**
 * A cache of {@link TimeStamped} data that provides concurrency through
 * immutability. This strategy is suitable when all of the cached data is stored
 * in memory. (For example, {@link org.orekit.time.UTCScale UTCScale}) This
 * class then provides convenient methods for accessing the data.
 *
 * @author Evan Ward
 * @param <T>  the type of data
 */
public class ImmutableTimeStampedCache<T extends TimeStamped>
    implements TimeStampedCache<T> {

    /**
     * An empty immutable cache that always throws an exception on attempted
     * access.
     */
    @SuppressWarnings("rawtypes")
    private static final ImmutableTimeStampedCache EMPTY_CACHE =
        new EmptyTimeStampedCache<TimeStamped>();

    /**
     * the cached data. Be careful not to modify it after the constructor, or
     * return a reference that allows mutating this list.
     */
    private final List<T> data;

    /**
     * the maximum size list to return from {@link #getNeighbors(AbsoluteDate, int)}.
     * @since 12.0
     */
    private final int maxNeighborsSize;

    /** Earliest date.
     * @since 12.0
     */
    private final AbsoluteDate earliestDate;

    /** Latest date.
     * @since 12.0
     */
    private final AbsoluteDate latestDate;

    /**
     * Create a new cache with the given neighbors size and data.
     *
     * @param maxNeighborsSize the maximum size of the list returned from
     *        {@link #getNeighbors(AbsoluteDate, int)}. Must be less than or equal to
     *        {@code data.size()}.
     * @param data the backing data for this cache. The list will be copied to
     *        ensure immutability. To guarantee immutability the entries in
     *        {@code data} must be immutable themselves. There must be more data
     *        than {@code maxNeighborsSize}.
     * @throws IllegalArgumentException if {@code neightborsSize > data.size()}
     *         or if {@code neighborsSize} is negative
     */
    public ImmutableTimeStampedCache(final int maxNeighborsSize,
                                     final Collection<? extends T> data) {
        // parameter check
        if (maxNeighborsSize > data.size()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                                     data.size(), maxNeighborsSize);
        }
        if (maxNeighborsSize < 1) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL,
                                                     maxNeighborsSize, 0);
        }

        // assign instance variables
        this.maxNeighborsSize = maxNeighborsSize;
        // sort and copy data first
        this.data = new ArrayList<>(data);
        Collections.sort(this.data, new ChronologicalComparator());

        this.earliestDate = this.data.get(0).getDate();
        this.latestDate   = this.data.get(this.data.size() - 1).getDate();

    }

    /**
     * private constructor for {@link #EMPTY_CACHE}.
     */
    private ImmutableTimeStampedCache() {
        this.data             = null;
        this.maxNeighborsSize = 0;
        this.earliestDate     = AbsoluteDate.ARBITRARY_EPOCH;
        this.latestDate       = AbsoluteDate.ARBITRARY_EPOCH;
    }

    /** {@inheritDoc} */
    public Stream<T> getNeighbors(final AbsoluteDate central, final int n) {

        if (n > maxNeighborsSize) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA, maxNeighborsSize);
        }

        // find central index
        final int i = findIndex(central);

        // check index in in the range of the data
        if (i < 0) {
            final AbsoluteDate earliest = this.getEarliest().getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                    earliest, central, earliest.durationFrom(central));
        } else if (i >= this.data.size()) {
            final AbsoluteDate latest = this.getLatest().getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                    latest, central, central.durationFrom(latest));
        }

        // force unbalanced range if necessary
        int start = FastMath.max(0, i - (n - 1) / 2);
        final int end = FastMath.min(this.data.size(), start + n);
        start = end - n;

        // return list without copying
        return this.data.subList(start, end).stream();
    }

    /**
     * Find the index, i, to {@link #data} such that {@code data[i] <= t} and
     * {@code data[i+1] > t} if {@code data[i+1]} exists.
     *
     * @param t the time
     * @return the index of the data at or just before {@code t}, {@code -1} if
     *         {@code t} is before the first entry, or {@code data.size()} if
     *         {@code t} is after the last entry.
     */
    private int findIndex(final AbsoluteDate t) {

        // left bracket of search algorithm
        int    iInf  = 0;
        double dtInf = t.durationFrom(earliestDate);
        if (dtInf < 0) {
            // before first entry
            return -1;
        }

        // right bracket of search algorithm
        int    iSup  = data.size() - 1;
        double dtSup = t.durationFrom(latestDate);
        if (dtSup > 0) {
            // after last entry
            return data.size();
        }

        // search entries, using linear interpolation
        // this should take only 2 iterations for near linear entries (most frequent use case)
        // regardless of the number of entries
        // this is much faster than binary search for large number of entries
        while (iSup - iInf > 1) {
            final int    iInterp = (int) FastMath.rint((iInf * dtSup - iSup * dtInf) / (dtSup - dtInf));
            final int    iMed    = FastMath.max(iInf + 1, FastMath.min(iInterp, iSup - 1));
            final double dtMed   = t.durationFrom(data.get(iMed).getDate());
            if (dtMed < 0) {
                iSup  = iMed;
                dtSup = dtMed;
            } else {
                iInf  = iMed;
                dtInf = dtMed;
            }
        }

        return iInf;
    }

    /** {@inheritDoc} */
    public int getMaxNeighborsSize() {
        return maxNeighborsSize;
    }

    /** {@inheritDoc} */
    public T getEarliest() {
        return this.data.get(0);
    }

    /** {@inheritDoc} */
    public T getLatest() {
        return this.data.get(this.data.size() - 1);
    }

    /**
     * Get all of the data in this cache.
     *
     * @return a sorted collection of all data passed in the
     *         {@link #ImmutableTimeStampedCache(int, Collection) constructor}.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(this.data);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Immutable cache with " + this.data.size() + " entries";
    }

    /**
     * An empty immutable cache that always throws an exception on attempted
     * access.
     */
    private static class EmptyTimeStampedCache<T extends TimeStamped> extends ImmutableTimeStampedCache<T> {

        /** {@inheritDoc} */
        @Override
        public Stream<T> getNeighbors(final AbsoluteDate central) {
            throw new TimeStampedCacheException(OrekitMessages.NO_CACHED_ENTRIES);
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxNeighborsSize() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public T getEarliest() {
            throw new OrekitIllegalStateException(OrekitMessages.NO_CACHED_ENTRIES);
        }

        /** {@inheritDoc} */
        @Override
        public T getLatest() {
            throw new OrekitIllegalStateException(OrekitMessages.NO_CACHED_ENTRIES);
        }

        /** {@inheritDoc} */
        @Override
        public List<T> getAll() {
            return Collections.emptyList();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Empty immutable cache";
        }

    }

    /**
     * Get an empty immutable cache, cast to the correct type.
     * @param <TS>  the type of data
     * @return an empty {@link ImmutableTimeStampedCache}.
     */
    @SuppressWarnings("unchecked")
    public static final <TS extends TimeStamped> ImmutableTimeStampedCache<TS> emptyCache() {
        return (ImmutableTimeStampedCache<TS>) EMPTY_CACHE;
    }

}
