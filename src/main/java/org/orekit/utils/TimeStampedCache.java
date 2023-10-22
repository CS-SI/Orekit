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

import java.util.stream.Stream;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * Interface for a data structure that can provide concurrent access to
 * {@link TimeStamped} data surrounding a given date.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @param <T> the type of data
 * @see GenericTimeStampedCache
 * @see ImmutableTimeStampedCache
 */
public interface TimeStampedCache<T extends TimeStamped> {

    /**
     * Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array will
     * be balanced with half the points before central date and half the points
     * after it (depending on n parity, of course). If the central date is near
     * the boundary, then the returned array will be unbalanced and will contain
     * only the n earliest (or latest) entries. A typical example of the later
     * case is leap seconds cache, since the number of leap seconds cannot be
     * arbitrarily increased.
     * <p>
     * This method is safe for multiple threads to execute concurrently.
     *
     * @param central central date
     * @return list of cached entries surrounding the specified date. The size
     *         of the list is guaranteed to be {@link #getMaxNeighborsSize()}.
     * @see #getNeighbors(AbsoluteDate, int)
     */
    default Stream<T> getNeighbors(AbsoluteDate central) {
        return getNeighbors(central, getMaxNeighborsSize());
    }

    /**
     * Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array will
     * be balanced with half the points before central date and half the points
     * after it (depending on n parity, of course). If the central date is near
     * the boundary, then the returned array will be unbalanced and will contain
     * only the n earliest (or latest) entries. A typical example of the later
     * case is leap seconds cache, since the number of leap seconds cannot be
     * arbitrarily increased.
     * <p>
     * This method is safe for multiple threads to execute concurrently.
     *
     * @param central central date
     * @param n number of neighbors (cannot exceed {@link #getMaxNeighborsSize()})
     * @return stream of cached entries surrounding the specified date.
     * @since 12.0
     */
    Stream<T> getNeighbors(AbsoluteDate central, int n);

    /**
     * Get the maximum size of the lists returned by
     * {@link #getNeighbors(AbsoluteDate, int)}.
     *
     * @return size of the list
     */
    int getMaxNeighborsSize();

    /**
     * Get the earliest entry in this cache.
     *
     * @return earliest cached entry
     * @throws IllegalStateException if this cache is empty
     */
    T getEarliest()
        throws IllegalStateException;

    /**
     * Get the latest entry in this cache.
     *
     * @return latest cached entry
     * @throws IllegalStateException if this cache is empty
     */
    T getLatest()
        throws IllegalStateException;

}
