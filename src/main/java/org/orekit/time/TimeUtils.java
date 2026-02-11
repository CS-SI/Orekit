/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;

/**
 * Time-related utilities.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class TimeUtils {

    /** Private constructor for utility class. */
    private TimeUtils() {
        // Nothing to do
    }

    /** Get the earliest object from a pair.
     * @param <S>  type of the time stamped objects
     * @param ts1 first object
     * @param ts2 second object
     * @return the earliest occurring object among #@code ts1} and {@code ts2}
     */
    public static <S extends TimeStamped> S earliest(final S ts1, final S ts2) {
        return ts1.getDate().isBeforeOrEqualTo(ts2) ? ts1 : ts2;
    }

    /** Get the earliest object from a pair.
     * @param <T> type of the field elements
     * @param <S>  type of the time stamped objects
     * @param ts1 first object
     * @param ts2 second object
     * @return the earliest occurring object among #@code ts1} and {@code ts2}
     */
    public static <T extends CalculusFieldElement<T>, S extends FieldTimeStamped<T>> S earliest(final S ts1, final S ts2) {
        return ts1.getDate().isBeforeOrEqualTo(ts2) ? ts1 : ts2;
    }

    /** Get the earliest object from a pair.
     * @param <S>  type of the time stamped objects
     * @param ts1 first object
     * @param ts2 second object
     * @return the earliest occurring object among #@code ts1} and {@code ts2}
     */
    public static <S extends TimeStamped> S latest(final S ts1, final S ts2) {
        return ts1.getDate().isBeforeOrEqualTo(ts2) ? ts2 : ts1;
    }

    /** Get the earliest object from a pair.
     * @param <T> type of the field elements
     * @param <S>  type of the time stamped objects
     * @param ts1 first object
     * @param ts2 second object
     * @return the earliest occurring object among #@code ts1} and {@code ts2}
     */
    public static <T extends CalculusFieldElement<T>, S extends FieldTimeStamped<T>> S latest(final S ts1, final S ts2) {
        return ts1.getDate().isBeforeOrEqualTo(ts2) ? ts2 : ts1;
    }

}
