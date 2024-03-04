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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
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

    /** An empty immutable cache that always throws an exception on attempted access.
     * @since 12.1
     */
    @SuppressWarnings("rawtypes")
    private static final ImmutableFieldTimeStampedCache EMPTY_CACHE =
        new EmptyFieldTimeStampedCache();

    /**
     * the cached data. Be careful not to modify it after the constructor, or return a reference that allows mutating this
     * list.
     */
    private final List<T> data;

    /** the maximum size list to return from {@link #getNeighbors(FieldAbsoluteDate)}. */
    private final int maxNeighborsSize;

    /**
     * Create a new cache with the given neighbors size and data.
     *
     * @param maxNeighborsSize the maximum size of the list returned from {@link #getNeighbors(FieldAbsoluteDate)}. Must be less than or
     * equal to {@code data.size()}.
     * @param data the backing data for this cache. The list will be copied to ensure immutability. To guarantee immutability
     * the entries in {@code data} must be immutable themselves. There must be more data than {@code maxNeighborsSize}.
     *
     * @throws IllegalArgumentException if {@code maxNeighborsSize > data.size()} or if {@code maxNeighborsSize} is negative
     */
    public ImmutableFieldTimeStampedCache(final int maxNeighborsSize,
                                          final Collection<? extends T> data) {
        // Parameter check
        if (maxNeighborsSize > data.size()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                                     data.size(), maxNeighborsSize);
        }
        if (maxNeighborsSize < 1) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL,
                                                     maxNeighborsSize, 1);
        }

        // Assign instance variables
        this.maxNeighborsSize = maxNeighborsSize;

        // Sort and copy data first
        this.data = new ArrayList<>(data);
        this.data.sort(new FieldChronologicalComparator<>());

    }

    /** Private constructor for {@link #EMPTY_CACHE}.
     */
    private ImmutableFieldTimeStampedCache() {
        this.data             = null;
        this.maxNeighborsSize = 0;
    }

    /**
     * Get an empty immutable cache, cast to the correct type.
     *
     * @param <TS> the type of data
     * @param <CFE> the type of the calculus field element
     * @param ignored field to which the elements belong
     * @return an empty {@link ImmutableTimeStampedCache}.
     * @deprecated as of 12.1, replaced by {@link #emptyCache()}
     */
    @Deprecated
    public static <TS extends FieldTimeStamped<CFE>, CFE extends CalculusFieldElement<CFE>>
        ImmutableFieldTimeStampedCache<TS, CFE> emptyCache(final Field<CFE> ignored) {
        return emptyCache();
    }

    /**
     * Get an empty immutable cache.
     *
     * @param <TS> the type of data
     * @param <CFE> the type of the calculus field element
     * @return an empty {@link ImmutableTimeStampedCache}.
     * @since 12.1
     */
    @SuppressWarnings("unchecked")
    public static <TS extends FieldTimeStamped<CFE>, CFE extends CalculusFieldElement<CFE>>
        ImmutableFieldTimeStampedCache<TS, CFE> emptyCache() {
        return (ImmutableFieldTimeStampedCache<TS, CFE>) EMPTY_CACHE;
    }

    /** {@inheritDoc} */
    public Stream<T> getNeighbors(final FieldAbsoluteDate<KK> central, final int n) {
        if (n > maxNeighborsSize) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA, maxNeighborsSize);
        }
        return new FieldSortedListTrimmer(n).getNeighborsSubList(central, data).stream();
    }

    /** {@inheritDoc} */
    public int getMaxNeighborsSize() {
        return this.maxNeighborsSize;
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

    /** An empty immutable cache that always throws an exception on attempted access. */
    private static class EmptyFieldTimeStampedCache<T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>>
            extends ImmutableFieldTimeStampedCache<T, KK> {

        /** {@inheritDoc} */
        @Override
        public Stream<T> getNeighbors(final FieldAbsoluteDate<KK> central) {
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

}
