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
    private static final int NEW_RANGE_FACTOR = 10;

    /** Earliest supported date. */
    private final AbsoluteDate earliest;

    /** Latest supported date. */
    private final AbsoluteDate latest;

    /** Quantum step. */
    private final double quantumStep;

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

    /** Lock for slots. */
    private final ReadWriteLock globalLock;

    /** Simple constructor.
     * @param maxSlots maximum number of independent cached time slots
     * @param maxSpan maximum duration span in seconds of one slot
     * @param entriesClass class of the cached entries
     * @param generator generator to use for yet non-existent data
     * @param neighborsSize fixed size of the arrays to be returned by {@link
     * #getNeighbors(AbsoluteDate)}, must be at least 2
     * @exception OrekitException if generator cannot provide supported range, or
     * the number of neighbors is too small
     */
    public TimeStampedCache(final int maxSlots, final double maxSpan, final Class<T> entriesClass,
                            final TimeStampedGenerator<T> generator, final int neighborsSize)
        throws OrekitException {

        // safety check
        if (neighborsSize < 2) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                      neighborsSize, 2);
        }

        // compute boundaries
        this.earliest         = generator.getEarliest();
        this.latest           = generator.getLatest();
        final double halfSpan = 0.5 * latest.durationFrom(earliest);
        this.reference        = earliest.shiftedBy(halfSpan);
        this.quantumStep      = halfSpan / Integer.MAX_VALUE;

        this.maxSlots       = maxSlots;
        this.maxSpan        = maxSpan;
        this.entriesClass   = entriesClass;
        this.generator      = generator;
        this.neighborsSize  = neighborsSize;
        this.slots          = new ArrayList<Slot>(maxSlots);
        this.calls          = new AtomicInteger(0);
        this.evictions      = new AtomicInteger(0);
        this.globalLock     = new ReentrantReadWriteLock();

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
     * @return a new array containing date neighbors
     * @exception OrekitException if the underlying {@link TimeStampedGenerator
     * generator} triggers one
     * @see #getBefore(AbsoluteDate)
     * @see #getAfter(AbsoluteDate)
     */
    public T[] getNeighbors(final AbsoluteDate central) throws OrekitException {

        globalLock.readLock().lock();
        try {
            final int dateQuantum = quantum(central);
            return selectSlot(central, dateQuantum).getNeighbors(central, dateQuantum);
        } finally {
            globalLock.readLock().unlock();
        }

    }

    /** Convert a date to a rough global quantum.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param date date to convert
     * @return quantum corresponding to the date
     */
    private int quantum(final AbsoluteDate date) {
        return (int) FastMath.rint(date.durationFrom(reference) / quantumStep);
    }

    /** Select a slot containing a date.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param date target date
     * @param dateQuantum global quantum of the date
     * @return slot covering the date
     * @exception OrekitException if the generator triggers one
     */
    private Slot selectSlot(final AbsoluteDate date, final int dateQuantum)
        throws OrekitException {

        Slot selected = null;

        int index = slots.isEmpty() ? 0 : slotIndex(dateQuantum);
        if (slots.isEmpty() ||
            slots.get(index).getEarliestQuantum() > dateQuantum + NEW_RANGE_FACTOR * neighborsSize ||
            slots.get(index).getLatestQuantum()   < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {
            // no existing slot is suitable

            // upgrade the read lock to a write lock so we can change the list of available slots
            globalLock.readLock().unlock();
            globalLock.writeLock().lock();

            try {
                // check slots again as another thread may have changed
                // the list while we were waiting for the write lock
                index = slots.isEmpty() ? 0 : slotIndex(dateQuantum);
                if (slots.isEmpty() ||
                    slots.get(index).getEarliestQuantum() > dateQuantum + NEW_RANGE_FACTOR * neighborsSize ||
                    slots.get(index).getLatestQuantum()   < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {

                    // we really need to create a new slot in the current thread
                    // (no other threads have created it while we were waiting for the lock)
                    if ((! slots.isEmpty()) &&
                        slots.get(index).getLatestQuantum() < dateQuantum - NEW_RANGE_FACTOR * neighborsSize) {
                        ++index;
                    }
                    slots.add(index, new Slot(date));

                    if (slots.size() > maxSlots) {
                        // we have exceeded the allowed maximum

                        // select the oldest accessed slot for eviction
                        int evict = index;
                        for (int i = 0; i < slots.size(); ++i) {
                            if (slots.get(i).getLastAccess() < slots.get(evict).getLastAccess()) {
                                evict = i;
                            }
                        }

                        if (evict == index) {
                            // all slots have been accessed earlier than the one just created!
                            // select arbitrarily the preceding one to preserve the newly created slot
                            evict = (index + slots.size() - 1) % slots.size();
                        }

                        // evict the selected slot
                        evictions.incrementAndGet();
                        slots.remove(evict);

                        if (evict < index) {
                            // adjust index of created slot as it was shifted by the eviction
                            index--;
                        }

                    }

                }

            } finally {
                // downgrade back to a read lock
                globalLock.readLock().lock();
                globalLock.writeLock().unlock();
            }
        }

        selected = slots.get(index);


        return selected;

    }

    /** Get the index of the slot in which a date should be cached.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * @param dateQuantum quantum of the date to search for
     * @param index of the slot in which the date should be cached (may not exist yet)
     */
    private int slotIndex(final int dateQuantum) {

        int iInf = 0;
        int qInf = slots.get(iInf).getEarliestQuantum();
        int iSup = slots.size() - 1;
        int qSup = slots.get(iSup).getLatestQuantum();
        while (iSup - iInf > 0) {
            final int iInterp = (iInf * (qSup - dateQuantum) + iSup * (dateQuantum - qInf)) / (qSup - qInf);
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

        /** Lock for the slot. */
        private final ReadWriteLock localLock;

        /** Earliest quantum. */
        private AtomicInteger earliestQuantum;

        /** Latest quantum. */
        private AtomicInteger latestQuantum;

        /** Index from a previous recent call. */
        private AtomicInteger guessedIndex;

        /** Last access time. */
        private AtomicLong lastAccess;

        /** Simple constructor.
         * @param initial initial entries to insert in the slot
         * @exception OrekitException if the first entries cannot be generated
         */
        public Slot(final AbsoluteDate date) throws OrekitException {

            // allocate cache
            this.cache     = new ArrayList<Entry>();
            this.localLock = new ReentrantReadWriteLock();

            // set up first entries
            final AbsoluteDate generationDate;
            if (date.compareTo(earliest) < 0) {
                generationDate = earliest;
            } else if (date.compareTo(latest) > 0) {
                generationDate = latest;
            } else {
                generationDate = date;
            }
            calls.incrementAndGet();
            for (final T entry : generator.generate(generationDate, neighborsSize)) {
                cache.add(new Entry(entry, quantum(entry.getDate())));
            }

            earliestQuantum = new AtomicInteger(cache.get(0).getQuantum());
            latestQuantum   = new AtomicInteger(cache.get(cache.size() - 1).getQuantum() + 1);
            guessedIndex    = new AtomicInteger(cache.size() / 2);
            lastAccess      = new AtomicLong(System.currentTimeMillis());

        }

        /** Get the quantum of the earliest date contained in the slot.
         * @return quantum of the earliest date contained in the slot
         */
        public int getEarliestQuantum() {
            // there is no need to get a lock here, atomic read access is sufficient
            return earliestQuantum.get();
        }

        /** Get the quantum of the latest date contained in the slot.
         * @return quantum of the latest date contained in the slot
         */
        public int getLatestQuantum() {
            // there is no need to get a lock here, atomic read access is sufficient
            return latestQuantum.get();
        }

        /** Get last access time of slot.
         * @return last known access time
         */
        public long getLastAccess() {
            // there is no need to get a lock here, atomic read access is sufficient
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
         * @exception OrekitException if the underlying {@link TimeStampedGenerator
         * generator} triggers one
         * @see #getBefore(AbsoluteDate)
         * @see #getAfter(AbsoluteDate)
         */
        public T[] getNeighbors(final AbsoluteDate central, final int dateQuantum) throws OrekitException {

            // get a read lock on the cache
            localLock.readLock().lock();

            try {

                int index         = entryIndex(central, dateQuantum);
                int firstNeighbor = index - (neighborsSize - 1) / 2;
                if (firstNeighbor < 0 || firstNeighbor + neighborsSize > cache.size()) {
                    // the cache is not balanced around the desired date, we can try to generate new data

                    // upgrade the read lock to a write lock so we can change the list of available slots
                    localLock.readLock().unlock();
                    localLock.writeLock().lock();

                    try {
                        // check entries again as another thread may have changed
                        // the list while we were waiting for the write lock
                        boolean loop = true;
                        while (loop) {
                            index         = entryIndex(central, dateQuantum);
                            firstNeighbor = index - (neighborsSize - 1) / 2;
                            if (firstNeighbor < 0 || firstNeighbor + neighborsSize > cache.size()) {

                                // generate data at the appropriate slot end
                                AbsoluteDate generationDate;
                                if (firstNeighbor < 0) {
                                    final AbsoluteDate t0 = cache.get(0).getData().getDate();
                                    final AbsoluteDate t1 = cache.get(1).getData().getDate();
                                    final double deltaT = t1.durationFrom(t0);
                                    generationDate = t0.shiftedBy(- neighborsSize * deltaT / 2);
                                } else {
                                    final AbsoluteDate t0 = cache.get(cache.size() - 2).getData().getDate();
                                    final AbsoluteDate t1 = cache.get(cache.size() - 1).getData().getDate();
                                    final double deltaT = t1.durationFrom(t0);
                                    generationDate = t1.shiftedBy(neighborsSize * deltaT / 2);
                                }
                                if (generationDate.compareTo(earliest) < 0) {
                                    generationDate = earliest;
                                    loop = false;
                                } else if (generationDate.compareTo(latest) > 0) {
                                    generationDate = latest;
                                    loop = false;
                                }
                                calls.incrementAndGet();
                                final T[] generated = generator.generate(generationDate, neighborsSize);

                                // add generated data to the slot
                                if (firstNeighbor < 0) {
                                    insertAtStart(generated);
                                } else {
                                    appendAtEnd(generated);
                                }

                                // update boundaries
                                earliestQuantum.set(cache.get(0).getQuantum());
                                latestQuantum.set(cache.get(cache.size() - 1).getQuantum());

                            } else {
                                loop = false;
                            }
                        }
                    } finally {
                        // downgrade back to a read lock
                        localLock.readLock().lock();
                        localLock.writeLock().unlock();
                    }

                }

                @SuppressWarnings("unchecked")
                final T[] array = (T[]) Array.newInstance(entriesClass, neighborsSize);
                for (int i = 0; i < neighborsSize; ++i) {
                    array[i] = cache.get(firstNeighbor + i).getData();
                }

                return array;

            } finally {
                // release the lock
                localLock.readLock().unlock();
            }

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
        private int entryIndex(final AbsoluteDate date, final int dateQuantum) {

            // first quick guesses, assuming a recent search was close enough
            final int guess = guessedIndex.get();
            if (cache.get(guess).getQuantum() <= dateQuantum) {
                if (cache.get(guess + 1).getQuantum() > dateQuantum) {
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

            // quick guesses have failed, we need to perform a full blown search
            if (dateQuantum < getEarliestQuantum()) {
                // date if before the first entry
                return -1;
            } else if (dateQuantum >= getLatestQuantum()) {
                // date is after the last entry
                return cache.size();
            } else {

                // try to get an existing entry
                int iInf = 0;
                int qInf = cache.get(iInf).getQuantum();
                int iSup = cache.size() - 1;
                int qSup = cache.get(iSup).getQuantum();
                while (iSup - iInf > 0) {
                    // within a continuous slot, entries are expected to be roughly linear
                    final int iInterp = (iInf * (qSup - dateQuantum) + iSup * (dateQuantum - qInf)) / (qSup - qInf);
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
        private void insertAtStart(final T[] data) {

            // insert data at start
            final int q0 = earliestQuantum.get();
            for (int i = 0; i < data.length; ++i) {
                final int quantum = quantum(data[i].getDate());
                if (quantum < q0) {
                    cache.add(i, new Entry(data[i], quantum));
                } else {
                    break;
                }
            }

            // evict excess data at end
            final AbsoluteDate t0 = cache.get(0).getData().getDate();
            while (cache.get(cache.size() - 1).getData().getDate().durationFrom(t0) > maxSpan) {
                cache.remove(cache.size() - 1);
            }

        }

        /** Append data at slot end.
         * @param data data to append
         */
        private void appendAtEnd(final T[] data) {

            // append data at end
            final int qn = latestQuantum.get();
            final int n  = cache.size();
            for (int i = data.length - 1; i >= 0; --i) {
                final int quantum = quantum(data[i].getDate());
                if (quantum > qn) {
                    cache.add(n, new Entry(data[i], quantum));
                } else {
                    break;
                }
            }

            // evict excess data at start
            final AbsoluteDate tn = cache.get(cache.size() - 1).getData().getDate();
            while (tn.durationFrom(cache.get(0).getData().getDate()) > maxSpan) {
                cache.remove(0);
            }

        }

        /** Container for entries. */
        private class Entry {

            /** Entry data. */
            private final T data;

            /** Global quantum of the entry. */
            private final int quantum;

            /** Simple constructor.
             * @param data entry data
             * @param quantum entry quantum
             */
            public Entry(final T data, final int quantum) {
                this.quantum = quantum;
                this.data  = data;
            }

            /** Get the quantum.
             * @return quantum
             */
            public int getQuantum() {
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
