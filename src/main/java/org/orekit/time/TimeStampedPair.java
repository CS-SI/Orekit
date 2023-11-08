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
package org.orekit.time;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/**
 * Pair of time stamped values being defined at the same date.
 *
 * @param <K> first time stamped value
 * @param <V> second time stamped value
 *
 * @author Vincent Cucchietti
 * @see TimeStamped
 */
public class TimeStampedPair<K extends TimeStamped, V extends TimeStamped> implements TimeStamped {

    /** Default date equality threshold of 1 ns. */
    public static final double DEFAULT_DATE_EQUALITY_THRESHOLD = 1e-9;

    /** First time stamped value. */
    private final K first;

    /** Second time stamped value. */
    private final V second;

    /**
     * Constructor.
     * <p>
     * First and second value must have the same date.
     *
     * @param first first time stamped value
     * @param second second time stamped value
     */
    public TimeStampedPair(final K first, final V second) {
        this(first, second, DEFAULT_DATE_EQUALITY_THRESHOLD);
    }

    /**
     * Constructor.
     * <p>
     * First and second value must have the same date.
     *
     * @param first first time stamped value
     * @param second second time stamped value
     * @param dateEqualityThreshold threshold below which dates are considered equal
     */
    public TimeStampedPair(final K first, final V second, final double dateEqualityThreshold) {
        checkDatesConsistency(first.getDate(), second.getDate(), dateEqualityThreshold);
        this.first  = first;
        this.second = second;
    }

    /**
     * Check date consistency.
     *
     * @param firstDate first date
     * @param secondDate second date
     * @param dateEqualityThreshold threshold below which dates are considered equal
     */
    public static void checkDatesConsistency(final AbsoluteDate firstDate, final AbsoluteDate secondDate,
                                             final double dateEqualityThreshold) {
        if (!firstDate.isCloseTo(secondDate, dateEqualityThreshold)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.DATES_MISMATCH, firstDate, secondDate);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return first.getDate();
    }

    /** Get first time stamped value.
     * @return first time stamped value
     */
    public K getFirst() {
        return first;
    }

    /** Get second time stamped value.
     * @return second time stamped value
     */
    public V getSecond() {
        return second;
    }

}
