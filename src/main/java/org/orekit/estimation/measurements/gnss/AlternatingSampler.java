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

import org.hipparchus.util.FastMath;

/** Sampler for generating long integers between two limits in an alternating pattern.
 * <p>
 * Given a center a and a radius r, this class will generate integers kᵢ such
 * that a - r ≤ kᵢ ≤ a + r. The generation order will start from the middle
 * (i.e. k₀ is the long integer closest to a) and go towards the boundaries,
 * alternating between values lesser than a and values greater than a.
 * For example, with a = 17.3 and r = 5.2, it will generate: k₀ = 17, k₁ = 18,
 * k₂ = 16, k₃ = 19, k₄ = 15, k₅ = 20, k₆ = 14, k₇ = 21, k₈ = 13, k₉ = 22.
 * </p>
 * <p>
 * There are no hard limits to the generation, i.e. in the example above, the
 * generator will happily generate k₁₀ = 12, k₁₁ = 23, k₁₂ = 11... In fact, if
 * there are no integers at all between {@code a - r} and {@code a + r}, even
 * the initial k₀ that is implicitly generated at construction will be out of
 * range. The {@link #inRange()} method can be used to check if the last generator
 * is still producing numbers within the initial range or if it has already
 * started generating out of range numbers.
 * </p>
 * <p>
 * If there are integers between {@code a - r} and {@code a + r}, it is guaranteed
 * that they will all be generated once before {@link #inRange()} starts returning
 * {@code false}.
 * </p>
 * <p>
 * This allows to explore the range for one integer ambiguity starting
 * with the most probable values (closest to a) and continuing with
 * values less probable.
 * </p>
 * @see <a href="https://www.researchgate.net/publication/2790708_The_LAMBDA_method_for_integer_ambiguity_estimation_implementation_aspects">
 * The LAMBDA method for integer ambiguity estimation: implementation aspects</a>
 * @see <a href="https://oeis.org/A001057">
 * A001057: Canonical enumeration of integers: interleaved positive and negative integers with zero prepended.</a>
 * @author Luc Maisonobe
 * @since 10.0
 */
class AlternatingSampler {

    /** Range midpoint. */
    private final double a;

    /** Offset with respect to A001057. */
    private final long offset;

    /** Sign with respect to A001057. */
    private final long sign;

    /** Minimum number to generate. */
    private long min;

    /** Maximum number to generate. */
    private long max;

    /** Previous generated number in A001057. */
    private long k1;

    /** Current generated number in A001057. */
    private long k0;

    /** Current generated number. */
    private long current;

    /** Simple constructor.
     * <p>
     * A first initial integer is already generated as a side effect of
     * construction, so {@link #getCurrent()} can be called even before
     * calling {@link #generateNext()}. If there are no integers at
     * all between {@code a - r} and {@code a + r}, then this initial
     * integer will already be out of range.
     * </p>
     * @param a range midpoint
     * @param r range radius
     */
    AlternatingSampler(final double a, final double r) {

        this.a      = a;
        this.offset = (long) FastMath.rint(a);
        this.sign   = offset <= a ? +1 : -1;
        setRadius(r);

        this.k1      = 0;
        this.k0      = 0;
        this.current = offset;
    }

    /** Reset the range radius.
     * <p>
     * Resetting radius is allowed during sampling, it simply changes
     * the boundaries used when calling {@link #inRange()}. Resetting
     * the radius does not change the sampling itself, neither the
     * {@link #getCurrent() current} value nor the {@link #generateNext()
     * next generated} ones.
     * </p>
     * <p>
     * A typical use case for calling {@link #setRadius(double)} during
     * sampling is to reduce sampling interval. It is used to shrink
     * the search ellipsoid on the fly in LAMBDA-based methods in order
     * to speed-up search.
     * </p>
     * @param r range radius
     */
    public void setRadius(final double r) {
        this.min = (long) FastMath.ceil(a - r);
        this.max = (long) FastMath.floor(a + r);
    }

    /** Get the range midpoint.
     * @return range midpoint
     */
    public double getMidPoint() {
        return a;
    }

    /** Get current value.
     * @return current value
     */
    public long getCurrent() {
        return current;
    }

    /** Check if the current value is within range.
     * @return true if current value is within range
     */
    public boolean inRange() {
        return min <= current && current <= max;
    }

    /** Generate next value.
     */
    public void generateNext() {

        // apply A001057 recursion
        final long k2 = k1;
        k1 = k0;
        k0 = 1 - (k1 << 1) - k2;

        // take offset and sign into account
        current = offset + sign * k0;

    }

}
