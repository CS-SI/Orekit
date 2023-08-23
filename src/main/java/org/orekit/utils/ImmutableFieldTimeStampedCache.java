/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldChronologicalComparator;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;

/**
 * A cache of {@link TimeStamped} data that provides concurrency through immutability. This strategy is suitable when all the
 * cached data is stored in memory. (For example, {@link org.orekit.time.UTCScale UTCScale}) This class then provides
 * convenient methods for accessing the data.
 *
 * @param <T> the type of data
 * @param <KK> the type the field element
 *
 * @author Evan Ward
 * @author Vincent Cucchietti
 */
public class ImmutableFieldTimeStampedCache<T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>>
        implements FieldTimeStampedCache<T, KK> {

    /**
     * the cached data. Be careful not to modify it after the constructor, or return a reference that allows mutating this
     * list.
     */
    private final List<T> data;

    /** the size list to return from {@link #getNeighbors(FieldAbsoluteDate)}. */
    private final int neighborsSize;

    /** Earliest date.
     * @since 12.0
     */
    private final FieldAbsoluteDate<KK> earliestDate;

    /** Latest date.
     * @since 12.0
     */
    private final FieldAbsoluteDate<KK> latestDate;

    /**
     * Create a new cache with the given neighbors size and data.
     *
     * @param neighborsSize the size of the list returned from {@link #getNeighbors(FieldAbsoluteDate)}. Must be less than or
     * equal to {@code data.size()}.
     * @param data the backing data for this cache. The list will be copied to ensure immutability. To guarantee immutability
     * the entries in {@code data} must be immutable themselves. There must be more data than {@code neighborsSize}.
     *
     * @throws IllegalArgumentException if {@code neighborsSize > data.size()} or if {@code neighborsSize} is negative
     */
    public ImmutableFieldTimeStampedCache(final int neighborsSize,
                                          final Collection<? extends T> data) {
        // Parameter check
        if (neighborsSize > data.size()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                                     data.size(), neighborsSize);
        }
        if (neighborsSize < 1) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL,
                                                     neighborsSize, 0);
        }

        // Assign instance variables
        this.neighborsSize = neighborsSize;

        // Sort and copy data first
        this.data = new ArrayList<>(data);
        Collections.sort(this.data, new FieldChronologicalComparator<>());

        this.earliestDate = this.data.get(0).getDate();
        this.latestDate   = this.data.get(this.data.size() - 1).getDate();

    }

    /**
     * private constructor for {@link #EMPTY_CACHE}.
     * @param field field to which the elements belong
     */
    private ImmutableFieldTimeStampedCache(final Field<KK> field) {
        this.data          = null;
        this.neighborsSize = 0;
        this.earliestDate  = FieldAbsoluteDate.getArbitraryEpoch(field);
        this.latestDate    = FieldAbsoluteDate.getArbitraryEpoch(field);
    }

    /**
     * Get an empty immutable cache, cast to the correct type.
     *
     * @param <TS> the type of data
     * @param <CFE> the type of the calculus field element
     * @param field field to which the elements belong
     * @return an empty {@link ImmutableTimeStampedCache}.
     */
    public static <TS extends FieldTimeStamped<CFE>, CFE extends CalculusFieldElement<CFE>>
        ImmutableFieldTimeStampedCache<TS, CFE> emptyCache(final Field<CFE> field) {
        return new EmptyFieldTimeStampedCache<>(field);
    }

    /** {@inheritDoc} */
    public Stream<T> getNeighbors(final FieldAbsoluteDate<KK> central) {

        // Find central index
        final int i = findIndex(central);

        // Check index in the range of the data
        if (i < 0) {
            final FieldAbsoluteDate<KK> earliest = this.getEarliest().getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                earliest, central, earliest.durationFrom(central).getReal());
        }
        else if (i >= this.data.size()) {
            final FieldAbsoluteDate<KK> latest = this.getLatest().getDate();
            throw new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                                                latest, central, central.durationFrom(latest).getReal());
        }

        // Force unbalanced range if necessary
        int start = FastMath.max(0, i - (this.neighborsSize - 1) / 2);
        final int end = FastMath.min(this.data.size(), start +
                this.neighborsSize);
        start = end - this.neighborsSize;

        // Return list without copying
        return this.data.subList(start, end).stream();
    }

    /** {@inheritDoc} */
    public int getNeighborsSize() {
        return this.neighborsSize;
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
     * Get all the data in this cache.
     *
     * @return a sorted collection of all data passed in the
     * {@link #ImmutableFieldTimeStampedCache(int, Collection) constructor}.
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
     * Find the index, i, to {@link #data} such that {@code data[i] <= t} and {@code data[i+1] > t} if {@code data[i+1]}
     * exists.
     *
     * @param t the time
     *
     * @return the index of the data at or just before {@code t}, {@code -1} if {@code t} is before the first entry, or
     * {@code data.size()} if {@code t} is after the last entry.
     */
    private int findIndex(final FieldAbsoluteDate<KK> t) {
        // left bracket of search algorithm
        int iInf  = 0;
        KK  dtInf = t.durationFrom(earliestDate);
        if (dtInf.getReal() < 0) {
            // before first entry
            return -1;
        }

        // right bracket of search algorithm
        int iSup  = data.size() - 1;
        KK  dtSup = t.durationFrom(latestDate);
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
            final KK  dtMed   = t.durationFrom(data.get(iMed).getDate());
            if (dtMed.getReal() < 0) {
                iSup  = iMed;
                dtSup = dtMed;
            } else {
                iInf  = iMed;
                dtInf = dtMed;
            }
        }

        return iInf;
    }

    /** An empty immutable cache that always throws an exception on attempted access. */
    private static class EmptyFieldTimeStampedCache<T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>>
            extends ImmutableFieldTimeStampedCache<T, KK> {

        /** Simple constructor.
         * @param field field to which elements belong
         */
        EmptyFieldTimeStampedCache(final Field<KK> field) {
            super(field);
        }

        /** {@inheritDoc} */
        @Override
        public Stream<T> getNeighbors(final FieldAbsoluteDate<KK> central) {
            throw new TimeStampedCacheException(OrekitMessages.NO_CACHED_ENTRIES);
        }

        /** {@inheritDoc} */
        @Override
        public int getNeighborsSize() {
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

}
