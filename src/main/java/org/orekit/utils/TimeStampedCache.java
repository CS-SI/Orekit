/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Generic thread-safe cache for {@link TimeStamped time-stamped} data.

 * @param <T> Type of the cached data.

 * @author Luc Maisonobe
 */
public class TimeStampedCache<T extends TimeStamped> {

    /** Default number of independent cached time slots. */
    public static final int DEFAULT_CACHED_SLOTS_NUMBER = 10;

    /** Threshold factor for creating new slot or extending existing ones. */
    private static final long NEW_RANGE_FACTOR = 10;

    /** Quantum step. */
    private static final double QUANTUM_STEP = 1.0e-6;

    /** Earliest supported date. */
    private final AbsoluteDate earliest;

    /** Latest supported date. */
    private final AbsoluteDate latest;

    /** Reference date for indexing. */
    private final AbsoluteDate reference;

    /** Maximum number of independent cached time slots. */
    private final int maxSlots;

    /** Maximum duration span in seconds of one slot. */
    private final double maxSpan;

    /** Class of the cached entries. */
    private final Class<T> entriesClass;

    /** Generator to use for yet non-cached data. */
    private final TimeStampedGenerator<T> generator;

    /** Number of entries in a neighbors array. */
    private final int neighborsSize;

    /** Independent time slots cached. */
    private final List<Slot> slots;

    /** Number of calls to the generate method. */
    private final AtomicInteger calls;

    /** Number of evictions. */
    private final AtomicInteger evictions;

    /** Global lock. */
    private final ReadWriteLock lock;

    /** Simple constructor.
     * @param maxSlots maximum number of independent cached time slots
     * @param maxSpan maximum duration span in seconds of one slot
     * (can be set to {@code Double.POSITIVE_INFINITY} if desired)
     * @param entriesClass class of the cached entries
     * @param generator generator to use for yet non-existent data
     * @param neighborsSize fixed size of the arrays to be returned by {@link
     * #getNeighbors(AbsoluteDate)}, must be at least 2
     */
    public TimeStampedCache(final int maxSlots, final double maxSpan, final Class<T> entriesClass,
                            final TimeStampedGenerator<T> generator, final int neighborsSize) {

        // safety check
        if (maxSlots < 1) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.NUMBER_TOO_SMALL, maxSlots, 1);
        }
        if (neighborsSize < 2) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                                                 neighborsSize, 2);
        }

        // compute boundaries
        this.earliest         = generator.getEarliest();
        this.latest           = generator.getLatest();
        final double halfSpan = 0.5 * latest.durationFrom(earliest);
        this.reference        = earliest.shiftedBy(halfSpan);

        this.maxSlots       = maxSlots;
        this.maxSpan        = maxSpan;
        this.entriesClass   = entriesClass;
        this.generator      = generator;
        this.neighborsSize  = neighborsSize;
        this.slots          = new ArrayList<Slot>(maxSlots);
        this.calls          = new AtomicInteger(0);
        this.evictions      = new AtomicInteger(0);
        this.lock           = new ReentrantReadWriteLock();

    }

    /** Get the generator.
     * @return generator
     */
    public TimeStampedGenerator<T> getGenerator() {
        return generator;
    }

    /** Get the number of calls to the generate method.
     * <p>
     * This number of calls is related to the number of cache misses and may
     * be used to tune the cache configuration. Each cache miss implies at
     * least one call is performed, but may require several calls if the new
     * date is far offset from the existing cache, depending on the number of
     * elements and step between elements in the arrays returned by the generator.
     * </p>
     * @return number of calls to the generate method
     */
    public int getGenerateCalls() {
        return calls.get();
    }

    /** Get the number of slots evictions.
     * <p>
     * This number should remain small when the max number of slots is sufficient
     * with respect to the number of concurrent requests to the cache. If it
     * increases too much, then the cache configuration is probably bad and cache
     * does not really improve things (in this case, the {@link #getGenerateCalls()
     * number of calls to the generate method} will probably increase too.
     * </p>
     * @return number of slots evictions
     */
    public int getSlotsEvictions() {
        return evictions.get();
    }

    /** Get the number of slots in use.
     * @return number of slots in use
     */
    public int getSlots() {

        lock.readLock().lock();
        try {
            return slots.size();
        } finally {
            lock.readLock().unlock();
        }

    }

    /** Get the total number of entries cached.
     * @return total number of entries cached
     */
    public int getEntries() {

        lock.readLock().lock();
        try {
            int entries = 0;
            for (final Slot slot : slots) {
                entries += slot.getEntries();
            }
            return entries;
        } finally {
            lock.readLock().unlock();
        }

    }

    /** Get the earliest cached entry.
     * @return earliest cached entry
     * @exception IllegalStateException if the cache has no slots at all
     * @see #getSlots()
     */
    public T getEarliest() throws IllegalStateException {

        lock.readLock().lock();
        try {
            if (slots.isEmpty()) {
                throw OrekitException.createIllegalStateException(OrekitMessages.NO_CACHED_ENTRIES);
            }
            return slots.get(0).getEarliest();
        } finally {
            lock.readLock().unlock();
        }

    }

    /** Get the latest cached entry.
     * @return latest cached entry
     * @exception IllegalStateException if the cache has no slots at all
     * @see #getSlots()
     */
    public T getLatest() throws IllegalStateException {

        lock.readLock().lock();
        try {
            if (slots.isEmpty()) {
                throw OrekitException.createIllegalStateException(OrekitMessages.NO_CACHED_ENTRIES);
            }
            return slots.get(slots.size() - 1).getLatest();
        } finally {
            lock.readLock().unlock();
        }

    }

    /** Get the fixed size of the arrays to be returned by {@link #getNeighbors(AbsoluteDate)}.
     * @return size of the array
     */
    public int getNeighborsSize() {
        return neighborsSize;
    }

    /** Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array
     * will be balanced with half the points before central date and half the
     * points after it (depending on n parity, of course). If the central date
     * is near the generator range boundary, then the returned array will be
     * unbalanced and will contain only the n earliest (or latest) generated
     * (and cached) entries. A typical example of the later case is leap seconds
     * cache, since the number of leap seconds cannot be arbitrarily increased.
     * </p>
     * @param central central date
     * @return array of cached entries surrounding specified date (the size
     * of the array is fixed to the one specified in the {@link #TimeStampedCache(int,
     * double, Class, TimeStampedGenerator, int) constructor})
     * @exception IllegalArgumentException if the requested date is outside the supported range
     * @see #getBefore(AbsoluteDate)
     * @see #getAfter(AbsoluteDate)
     * @see #getEarliest()
     * @see #getLatest()
     */
    public T[] getNeighbors(final AbsoluteDate central) {

        if (central.compareTo(earliest) < 0 || central.compareTo(latest) > 0) {
            OrekitException.createIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_CACHE,
                                                           central, earliest, latest);
        }

        lock.readLock().lock();
        try {
            final long dateQuantum = quantum(central);
            return selectSlot(central, dateQuantum).getNeighbors(central, dateQuantum);
        } finally {
            lock.readLock().unlock();
        }

    }

    /** Convert a date to a rough global quantum.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param date date to convert
     * @return quantum corresponding to the date
     */
    private long quantum(final AbsoluteDate date) {
        return FastMath.round(date.durationFrom(reference) / QUANTUM_STEP);
    }

    /** Select a slot containing a date.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param date target date
     * @param dateQuantum global quantum of the date
     * @return slot covering the date
     */
    private Slot selectSlot(final AbsoluteDate date, final long dateQuantum) {

        Slot selected = null;

        int index = slots.isEmpty() ? 0 : slotIndex(dateQuantum);
        if (slots.isEmpty() ||
            slots.get(index).getEarliestQuantum() > dateQuantum + NEW_RANGE_FACTOR * neighborsSize ||
            slots.get(index).getLatestQuantum()   < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {
            // no existing slot is suitable

            // upgrade the read lock to a write lock so we can change the list of available slots
            lock.readLock().unlock();
            lock.writeLock().lock();

            try {
                // check slots again as another thread may have changed
                // the list while we were waiting for the write lock
                index = slots.isEmpty() ? 0 : slotIndex(dateQuantum);
                if (slots.isEmpty() ||
                    slots.get(index).getEarliestQuantum() > dateQuantum + NEW_RANGE_FACTOR * neighborsSize ||
                    slots.get(index).getLatestQuantum()   < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {

                    // we really need to create a new slot in the current thread
                    // (no other threads have created it while we were waiting for the lock)
                    if ((!slots.isEmpty()) &&
                        slots.get(index).getLatestQuantum() < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {
                        ++index;
                    }

                    if (slots.size() >= maxSlots) {
                        // we must prevent exceeding allowed max

                        // select the oldest accessed slot for eviction
                        int evict = 0;
                        for (int i = 0; i < slots.size(); ++i) {
                            if (slots.get(i).getLastAccess() < slots.get(evict).getLastAccess()) {
                                evict = i;
                            }
                        }

                        // evict the selected slot
                        evictions.incrementAndGet();
                        slots.remove(evict);

                        if (evict < index) {
                            // adjust index of created slot as it was shifted by the eviction
                            index--;
                        }
                    }

                    slots.add(index, new Slot(date));

                }

            } finally {
                // downgrade back to a read lock
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        }

        selected = slots.get(index);


        return selected;

    }

    /** Get the index of the slot in which a date could be cached.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param dateQuantum quantum of the date to search for
     * @return the slot in which the date could be cached
     */
    private int slotIndex(final long dateQuantum) {

        int  iInf = 0;
        final long qInf = slots.get(iInf).getEarliestQuantum();
        int  iSup = slots.size() - 1;
        final long qSup = slots.get(iSup).getLatestQuantum();
        while (iSup - iInf > 0) {
            final int iInterp = (int) ((iInf * (qSup - dateQuantum) + iSup * (dateQuantum - qInf)) / (qSup - qInf));
            final int iMed    = FastMath.max(iInf, FastMath.min(iInterp, iSup));
            final Slot slot   = slots.get(iMed);
            if (dateQuantum < slot.getEarliestQuantum()) {
                iSup = iMed - 1;
            } else if (dateQuantum > slot.getLatestQuantum()) {
                iInf = FastMath.min(iSup, iMed + 1);
            } else {
                return iMed;
            }
        }

        return iInf;

    }

    /** Time slot. */
    private final class Slot {

        /** Cached time-stamped entries. */
        private final List<Entry> cache;

        /** Earliest quantum. */
        private AtomicLong earliestQuantum;

        /** Latest quantum. */
        private AtomicLong latestQuantum;

        /** Index from a previous recent call. */
        private AtomicInteger guessedIndex;

        /** Last access time. */
        private AtomicLong lastAccess;

        /** Simple constructor.
         * @param date central date for initial entries to insert in the slot
         * @exception IllegalStateException if entries are not chronologically sorted
         */
        public Slot(final AbsoluteDate date) throws IllegalStateException {

            // allocate cache
            this.cache = new ArrayList<Entry>();

            // set up first entries
            AbsoluteDate generationDate;
            if (date.compareTo(earliest) < 0) {
                generationDate = earliest;
            } else if (date.compareTo(latest) > 0) {
                generationDate = latest;
            } else {
                generationDate = date;
            }

            calls.incrementAndGet();
            for (final T entry : generateAndCheck(null, generationDate)) {
                cache.add(new Entry(entry, quantum(entry.getDate())));
            }
            earliestQuantum = new AtomicLong(cache.get(0).getQuantum());
            latestQuantum   = new AtomicLong(cache.get(cache.size() - 1).getQuantum() + 1);

            while (cache.size() < neighborsSize) {
                // we need to generate more entries

                final T entry0 = cache.get(0).getData();
                final T entryN = cache.get(cache.size() - 1).getData();
                calls.incrementAndGet();

                final T existing;
                if (latest.durationFrom(entryN.getDate()) >= entry0.getDate().durationFrom(earliest)) {
                    // generate additional point at the end of the slot
                    existing = entryN;
                    generationDate = entryN.getDate().shiftedBy(getMeanStep() * (neighborsSize - cache.size()));
                    if (generationDate.compareTo(latest) > 0) {
                        generationDate = latest;
                    }
                    appendAtEnd(generateAndCheck(existing, generationDate));
                } else {
                    // generate additional point at the start of the slot
                    existing = entry0;
                    generationDate = entry0.getDate().shiftedBy(-getMeanStep() * (neighborsSize - cache.size()));
                    if (generationDate.compareTo(earliest) < 0) {
                        generationDate = earliest;
                    }
                    insertAtStart(generateAndCheck(existing, generationDate));
                }

            }

            guessedIndex    = new AtomicInteger(cache.size() / 2);
            lastAccess      = new AtomicLong(System.currentTimeMillis());

        }

        /** Get the earliest entry contained in the slot.
         * @return earliest entry contained in the slot
         */
        public T getEarliest() {
            return cache.get(0).getData();
        }

        /** Get the quantum of the earliest date contained in the slot.
         * @return quantum of the earliest date contained in the slot
         */
        public long getEarliestQuantum() {
            return earliestQuantum.get();
        }

        /** Get the latest entry contained in the slot.
         * @return latest entry contained in the slot
         */
        public T getLatest() {
            return cache.get(cache.size() - 1).getData();
        }

        /** Get the quantum of the latest date contained in the slot.
         * @return quantum of the latest date contained in the slot
         */
        public long getLatestQuantum() {
            return latestQuantum.get();
        }

        /** Get the number of entries contained din the slot.
         * @return number of entries contained din the slot
         */
        public int getEntries() {
            return cache.size();
        }

        /** Get the mean step between entries.
         * @return mean step between entries (or an arbitrary non-null value
         * if there are fewer than 2 entries)
         */
        private double getMeanStep() {
            if (cache.size() < 2) {
                return 1.0;
            } else {
                final AbsoluteDate t0 = cache.get(0).getData().getDate();
                final AbsoluteDate tn = cache.get(cache.size() - 1).getData().getDate();
                return tn.durationFrom(t0) / (cache.size() - 1);
            }
        }

        /** Get last access time of slot.
         * @return last known access time
         */
        public long getLastAccess() {
            return lastAccess.get();
        }

        /** Get the entries surrounding a central date.
         * <p>
         * If the central date is well within covered slot, the returned array
         * will be balanced with half the points before central date and half the
         * points after it (depending on n parity, of course). If the central date
         * is near slot boundary and the underlying {@link TimeStampedGenerator
         * generator} cannot extend it (i.e. it returns null), then the returned
         * array will be unbalanced and will contain only the n earliest (or latest)
         * cached entries. A typical example of the later case is leap seconds cache,
         * since the number of leap seconds cannot be arbitrarily increased.
         * </p>
         * @param central central date
         * @param dateQuantum global quantum of the date
         * @return a new array containing date neighbors
         * @exception IllegalStateException if entries are not chronologically sorted
         * @see #getBefore(AbsoluteDate)
         * @see #getAfter(AbsoluteDate)
         */
        public T[] getNeighbors(final AbsoluteDate central, final long dateQuantum)
            throws IllegalStateException {

            int index         = entryIndex(central, dateQuantum);
            int firstNeighbor = index - (neighborsSize - 1) / 2;

            // if the request for neighbors is outside the supported generator limits,
            // adjust the firstNeighbor index to match the current available cache entries
            // without requesting more data from the generator
            // as the slot will always contain entries after initialization, there is no
            // need to do a null check for the data returned from getLatest() or getEarliest()
            if (firstNeighbor + neighborsSize > cache.size() &&
                getLatest().getDate().compareTo(generator.getLatest()) >= 0) {
                firstNeighbor = cache.size() - neighborsSize;
            } else if (firstNeighbor < 0 &&
                       getEarliest().getDate().compareTo(generator.getEarliest()) <= 0) {
                firstNeighbor = 0;
            }

            if (firstNeighbor < 0 || firstNeighbor + neighborsSize > cache.size()) {
                // the cache is not balanced around the desired date, we can try to generate new data

                // upgrade the read lock to a write lock so we can change the list of available slots
                lock.readLock().unlock();
                lock.writeLock().lock();

                try {
                    // check entries again as another thread may have changed
                    // the list while we were waiting for the write lock
                    boolean loop = true;
                    while (loop) {
                        index         = entryIndex(central, dateQuantum);
                        firstNeighbor = index - (neighborsSize - 1) / 2;
                        if (firstNeighbor < 0 || firstNeighbor + neighborsSize > cache.size()) {

                            // generate data at the appropriate slot end
                            final double step = getMeanStep();
                            final T existing;
                            AbsoluteDate generationDate;
                            if (firstNeighbor < 0) {
                                existing       = cache.get(0).getData();
                                generationDate = central.shiftedBy(-step * neighborsSize / 2);
                                if (generationDate.compareTo(existing.getDate()) >= 0) {
                                    generationDate = existing.getDate().shiftedBy(-step);
                                }
                            } else {
                                existing       = cache.get(cache.size() - 1).getData();
                                generationDate = central.shiftedBy(-step * neighborsSize / 2);
                                if (generationDate.compareTo(existing.getDate()) <= 0) {
                                    generationDate = existing.getDate().shiftedBy(step);
                                }
                            }
                            if (generationDate.compareTo(earliest) < 0) {
                                generationDate = earliest;
                                loop = false;
                            } else if (generationDate.compareTo(latest) > 0) {
                                generationDate = latest;
                                loop = false;
                            }
                            calls.incrementAndGet();

                            // add generated data to the slot
                            if (firstNeighbor < 0) {
                                insertAtStart(generateAndCheck(existing, generationDate));
                            } else {
                                appendAtEnd(generateAndCheck(existing, generationDate));
                            }

                        } else {
                            loop = false;
                        }
                    }
                } finally {
                    // downgrade back to a read lock
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            }

            @SuppressWarnings("unchecked")
            final T[] array = (T[]) Array.newInstance(entriesClass, neighborsSize);
            if (firstNeighbor + neighborsSize > cache.size()) {
                // we end up with a non-balanced neighborhood,
                // adjust the start point to fit within the cache
                firstNeighbor = cache.size() - neighborsSize;
            }
            if (firstNeighbor < 0) {
                firstNeighbor = 0;
            }
            for (int i = 0; i < neighborsSize; ++i) {
                array[i] = cache.get(firstNeighbor + i).getData();
            }

            return array;

        }

        /** Get the index of the entry corresponding to a date.
         * <p>
         * We own a local read lock while calling this method.
         * </p>
         * @param date date
         * @param dateQuantum global quantum of the date
         * @return index in the array such that entry[index] is before
         * date and entry[index + 1] is after date (or they are at array boundaries)
         */
        private int entryIndex(final AbsoluteDate date, final long dateQuantum) {

            // first quick guesses, assuming a recent search was close enough
            final int guess = guessedIndex.get();
            if (guess > 0 && guess < cache.size()) {
                if (cache.get(guess).getQuantum() <= dateQuantum) {
                    if (guess + 1 < cache.size() && cache.get(guess + 1).getQuantum() > dateQuantum) {
                        // good guess!
                        return guess;
                    } else {
                        // perhaps we have simply shifted just one point forward ?
                        if (guess + 2 < cache.size() && cache.get(guess + 2).getQuantum() > dateQuantum) {
                            guessedIndex.set(guess + 1);
                            return guess + 1;
                        }
                    }
                } else {
                    // perhaps we have simply shifted just one point backward ?
                    if (guess > 1 && cache.get(guess - 1).getQuantum() <= dateQuantum) {
                        guessedIndex.set(guess - 1);
                        return guess - 1;
                    }
                }
            }

            // quick guesses have failed, we need to perform a full blown search
            if (dateQuantum < getEarliestQuantum()) {
                // date if before the first entry
                return -1;
            } else if (dateQuantum >= getLatestQuantum()) {
                // date is after the last entry
                return cache.size();
            } else {

                // try to get an existing entry
                int  iInf = 0;
                final long qInf = cache.get(iInf).getQuantum();
                int  iSup = cache.size() - 1;
                final long qSup = cache.get(iSup).getQuantum();
                while (iSup - iInf > 0) {
                    // within a continuous slot, entries are expected to be roughly linear
                    final int iInterp = (int) ((iInf * (qSup - dateQuantum) + iSup * (dateQuantum - qInf)) / (qSup - qInf));
                    final int iMed    = FastMath.max(iInf + 1, FastMath.min(iInterp, iSup));
                    final Entry entry = cache.get(iMed);
                    if (dateQuantum < entry.getQuantum()) {
                        iSup = iMed - 1;
                    } else if (dateQuantum > entry.getQuantum()) {
                        iInf = iMed;
                    } else {
                        guessedIndex.set(iMed);
                        return iMed;
                    }
                }

                guessedIndex.set(iInf);
                return iInf;

            }

        }

        /** Insert data at slot start.
         * @param data data to insert
         */
        private void insertAtStart(final List<T> data) {

            // insert data at start
            final long q0 = earliestQuantum.get();
            for (int i = 0; i < data.size(); ++i) {
                final long quantum = quantum(data.get(i).getDate());
                if (quantum < q0) {
                    cache.add(i, new Entry(data.get(i), quantum));
                } else {
                    break;
                }
            }

            // evict excess data at end
            final AbsoluteDate t0 = cache.get(0).getData().getDate();
            while (cache.size() > neighborsSize &&
                   cache.get(cache.size() - 1).getData().getDate().durationFrom(t0) > maxSpan) {
                cache.remove(cache.size() - 1);
            }

            // update boundaries
            earliestQuantum.set(cache.get(0).getQuantum());
            latestQuantum.set(cache.get(cache.size() - 1).getQuantum());

        }

        /** Append data at slot end.
         * @param data data to append
         */
        private void appendAtEnd(final List<T> data) {

            // append data at end
            final long qn = latestQuantum.get();
            final int  n  = cache.size();
            for (int i = data.size() - 1; i >= 0; --i) {
                final long quantum = quantum(data.get(i).getDate());
                if (quantum > qn) {
                    cache.add(n, new Entry(data.get(i), quantum));
                } else {
                    break;
                }
            }

            // evict excess data at start
            final AbsoluteDate tn = cache.get(cache.size() - 1).getData().getDate();
            while (cache.size() > neighborsSize &&
                   tn.durationFrom(cache.get(0).getData().getDate()) > maxSpan) {
                cache.remove(0);
            }

            // update boundaries
            earliestQuantum.set(cache.get(0).getQuantum());
            latestQuantum.set(cache.get(cache.size() - 1).getQuantum());

        }

        /** Generate entries and check ordering.
         * @param existing closest already existing entry (may be null)
         * @param date date that must be covered by the range of the generated array
         * (guaranteed to lie between {@link #getEarliest()} and {@link #getLatest()})
         * @return chronologically sorted list of generated entries
         * @exception IllegalStateException if entries are not chronologically sorted
         */
        private List<T> generateAndCheck(final T existing, final AbsoluteDate date)
            throws IllegalStateException {
            final List<T> entries = generator.generate(existing, date);
            for (int i = 1; i < entries.size(); ++i) {
                if (entries.get(i).getDate().compareTo(entries.get(i - 1).getDate()) < 0) {
                    throw OrekitException.createIllegalStateException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES,
                                                                      entries.get(i - 1).getDate(),
                                                                      entries.get(i).getDate());
                }
            }
            return entries;
        }

        /** Container for entries. */
        private class Entry {

            /** Entry data. */
            private final T data;

            /** Global quantum of the entry. */
            private final long quantum;

            /** Simple constructor.
             * @param data entry data
             * @param quantum entry quantum
             */
            public Entry(final T data, final long quantum) {
                this.quantum = quantum;
                this.data  = data;
            }

            /** Get the quantum.
             * @return quantum
             */
            public long getQuantum() {
                return quantum;
            }

            /** Get the data.
             * @return data
             */
            public T getData() {
                return data;
            }

        }
    }

}
