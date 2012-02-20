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

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Generator to use for creating entries in {@link TimeStampedCache time stamped caches}.
 * <p>
 * As long as a generator is referenced by one {@link TimeStampedCache cache} only, it is
 * guaranteed to be called in a thread-safe way, even if the cache is used in a multi-threaded
 * environment. The cache takes care of scheduling the calls to all the methods defined in
 * this interface so only one thread uses them at any time. There is no need for the
 * implementing classes to handle synchronization or locks by themselves. 
 * </p>
 * <p>
 * The generator is provided by the user of the {@link TimeStampedCache cache} and should
 * be consistent with the way he will use the cached data.
 * </p>
 * <p>
 * If entries must have regular time gaps (for example one entry every 3600 seconds), then
 * the generator must ensure by itself all generated entries are exactly located on the
 * expected regular grid, even if they are generated in random order. The reason for that
 * is that the cache may ask for entries in different ranges and merge these ranges afterwards.
 * A typical example would be a cache first calling the generator for 6 points around
 * 2012-02-19T17:48:00 and when these points are exhausted calling the generator again for 6
 * new points around 2012-02-19T23:20:00. If all points must be exactly 3600 seconds apart,
 * the generator should generate the first 6 points at 2012-02-19T15:00:00, 2012-02-19T16:00:00,
 * 2012-02-19T17:00:00, 2012-02-19T18:00:00, 2012-02-19T19:00:00 and 2012-02-19T20:00:00, and
 * the next 6 points at 2012-02-19T21:00:00, 2012-02-19T22:00:00, 2012-02-19T23:00:00,
 * 2012-02-20T00:00:00, 2012-02-20T01:00:00 and 2012-02-20T02:00:00. If the separation between
 * the points is irrelevant, the first points could be generated at 17:48:00 instead of
 * 17:00:00 or 18:00:00. The cache <em>will</em> merge arrays returned from different calls in
 * the same global time slot.
 * </p>
 * <p>
 * Note that the {@link #getEarliest() earliest} and {@link #getLatest() latest} dates are
 * used to set up an interpolation mapping, so even if a generator is able to handle dates
 * infinitely far in the past or in the future directions, {@link #getEarliest()} and {@link
 * #getLatest()} must both return finite dates instead of the theoretical infinite boundaries.
 * </p>
 * @param <T> Type of the cached data.
 * @author Luc Maisonobe
 */
public interface TimeStampedGenerator<T extends TimeStamped> {

    /** Get the date of the earliest entry that can be generated.
     * @return date of the earliest entry that can be generated (must be a finite
     * date, date at infinity in the past direction is <em>not</em> allowed)
     * @exception OrekitException if earliest date cannot be estimated
     * @see #getLatest()
     */
    AbsoluteDate getEarliest() throws OrekitException;

    /** Get the date of the latest entry that can be generated.
     * @return date of the latest entry that can be generated (must be a finite
     * date, date at infinity in the future direction is <em>not</em> allowed)
     * @exception OrekitException if latest date cannot be estimated
     * @see #getEarliest()
     */
    AbsoluteDate getLatest() throws OrekitException;

    /** Generate an array of entries to be cached.
     * @param date central date around which entries should be generated (guaranteed to lie
     * between {@link #getEarliest()} and {@link #getLatest()})
     * @param n minimum number of entries to generate
     * @return generated entry (it should have at least n entries surrounding the specified date;
     * it may have more than n entries)
     * @exception OrekitException if entry generation fails
     */
    T[] generate(AbsoluteDate date, int n) throws OrekitException;

}
