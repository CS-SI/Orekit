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
package org.orekit.estimation.measurements.gnss;

/** Ambiguity acceptance test based on a ratio of the two best candidates.
 * @see AmbiguitySolver
 * @author Luc Maisonobe
 * @since 10.0
 */
public class SimpleRatioAmbiguityAcceptance implements AmbiguityAcceptance {

    /** Number of candidate solutions. */
    private static final int NB_CANDIDATES = 2;

    /** Acceptance ratio for {@code candidate[0]/candidate[1]}. */
    private final double ratio;

    /** Simple constructor.
     * @param ratio acceptance ratio for {@code candidate[0]/candidate[1]},
     * typically {@code 1.0/2.0} or {@code 1.0/3.0}
     */
    public SimpleRatioAmbiguityAcceptance(final double ratio) {
        this.ratio = ratio;
    }

    /** {@inheritDoc} */
    @Override
    public int numberOfCandidates() {
        return NB_CANDIDATES;
    }

    /** {@inheritDoc}
     * <p>
     * If the ratio {@code candidate[0]/candidate[1]} is smaller or
     * equal to the ratio given at construction, then {@code candidate[0]}
     * will be accepted
     * </p>
     */
    @Override
    public IntegerLeastSquareSolution accept(final IntegerLeastSquareSolution[] candidates) {
        if (candidates[0].getSquaredDistance() / candidates[1].getSquaredDistance() <= ratio) {
            return candidates[0];
        } else {
            return null;
        }
    }

}
