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

import org.hipparchus.CalculusFieldElement;

/**
 * Pair of time stamped values being defined at the same date.
 *
 * @param <F> first time stamped value
 * @param <S> second time stamped value
 * @param <KK> type of the field element
 *
 * @author Vincent Cucchietti
 * @see FieldTimeStamped
 */
public class FieldTimeStampedPair<F extends FieldTimeStamped<KK>, S extends FieldTimeStamped<KK>,
        KK extends CalculusFieldElement<KK>> implements FieldTimeStamped<KK> {

    /** Default date equality threshold of 1 ns. */
    public static final double DEFAULT_DATE_EQUALITY_THRESHOLD = 1e-9;

    /** First time stamped value. */
    private final F first;

    /** Second time stamped value. */
    private final S second;

    /**
     * Constructor.
     * <p>
     * First and second value must have the same date.
     *
     * @param first first time stamped value
     * @param second second time stamped value
     */
    public FieldTimeStampedPair(final F first, final S second) {
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
    public FieldTimeStampedPair(final F first, final S second, final double dateEqualityThreshold) {
        TimeStampedPair.checkDatesConsistency(first.getDate().toAbsoluteDate(), second.getDate().toAbsoluteDate(),
                                              dateEqualityThreshold);
        this.first  = first;
        this.second = second;
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<KK> getDate() {
        return first.getDate();
    }

    /** Get first time stamped value.
     * @return first time stamped value
     */
    public F getFirst() {
        return first;
    }

    /** Get second time stamped value.
     * @return second time stamped value
     */
    public S getSecond() {
        return second;
    }
}
