/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.frames;

import org.orekit.time.AbsoluteDate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** Thread-safe cached provider for frame transforms.
 * <p>
 * This provider is based on a thread-safe Least Recently Used cache
 * using date as it access key, hence saving computation time on
 * transform building.
 * </p>
 * <p>
 * This class is thread-safe.
 * </p>
 * @author Luc Maisonobe
 * @since 13.0.3
 */
public class CachedTransformProvider {

    /** Origin frame. */
    private final Frame origin;

    /** Destination frame. */
    private final Frame destination;

    /** Number of transforms kept in the date-based cache. */
    private final int cacheSize;

    /** Generator for transforms. */
    private final Function<AbsoluteDate, Transform> generator;

    /** Lock for concurrent access. */
    private final ReentrantLock lock;

    /** Transforms LRU cache. */
    private final Map<AbsoluteDate, Transform> cache;

    /** Simple constructor.
     * @param origin      origin frame
     * @param destination destination frame
     * @param generator   generator for transforms
     * @param cacheSize   number of transforms kept in the date-based cache
     */
    public CachedTransformProvider(final Frame origin, final Frame destination,
                                   final Function<AbsoluteDate, Transform> generator,
                                   final int cacheSize) {

        this.origin      = origin;
        this.destination = destination;
        this.cacheSize   = cacheSize;
        this.generator   = generator;
        this.lock        = new ReentrantLock();

        // cache for regular dates
        this.cache       = new LinkedHashMap<AbsoluteDate, Transform>(cacheSize, 0.75f, true) {
            /** {@inheritDoc} */
            @Override
            protected boolean removeEldestEntry(final Map.Entry<AbsoluteDate, Transform> eldest) {
                return size() > cacheSize;
            }
        };

    }

    /** Get origin frame.
     * @return origin frame
     */
    public Frame getOrigin() {
        return origin;
    }

    /** Get destination frame.
     * @return destination frame
     */
    public Frame getDestination() {
        return destination;
    }

    /** Get the nmber of transforms kept in the date-based cache.
     * @return nmber of transforms kept in the date-based cache
     */
    public int getCacheSize() {
        return cacheSize;
    }

    /** Get the {@link Transform} corresponding to specified date.
     * @param date current date
     * @return transform at specified date
     */
    public Transform getTransform(final AbsoluteDate date) {
        lock.lock();
        try {
            return cache.computeIfAbsent(date, generator);
        } finally {
            lock.unlock();
        }
    }

}
