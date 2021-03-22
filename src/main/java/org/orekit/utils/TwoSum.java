/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.RealFieldElement;

/**
 * Møller-Knuth TwoSum algorithm for adding two floating-point numbers while tracking the residual
 * error.
 */
public final class TwoSum {

    private TwoSum() {
    }

    /**
     * Sums {@code a} and {@code b} using the TwoSum algorithm.
     * @param a first summand
     * @param b second summand
     * @return sum and residual error
     */
    public static SumAndResidual twoSum(final double a, final double b) {
        final double s = a + b;
        final double aPrime = s - b;
        final double bPrime = s - aPrime;
        final double deltaA = a - aPrime;
        final double deltaB = b - bPrime;
        final double t = deltaA + deltaB;
        return new SumAndResidual(s, t);
    }

    /**
     * Sums {@code a} and {@code b} using the TwoSum algorithm.
     * @param <T> field element type
     * @param a first summand
     * @param b second summand
     * @return sum and residual error
     */
    public static <T extends RealFieldElement<T>> FieldSumAndResidual<T> twoSum(final T a, final T b) {
        final T s = a.add(b);
        final T aPrime = s.subtract(b);
        final T bPrime = s.subtract(aPrime);
        final T deltaA = a.subtract(aPrime);
        final T deltaB = b.subtract(bPrime);
        final T t = deltaA.add(deltaB);
        return new FieldSumAndResidual<>(s, t);
    }

    /**
     * Result class containing the sum and the residual error in the sum.
     */
    public static final class SumAndResidual {

        /** Sum. */
        private final double sum;
        /** Residual error. */
        private final double residual;

        /**
         * Constructs a {@link SumAndResidual} instance.
         * @param sum      sum
         * @param residual residual
         */
        private SumAndResidual(final double sum, final double residual) {
            this.sum = sum;
            this.residual = residual;
        }

        /**
         * Returns the sum.
         * @return sum
         */
        public double getSum() {
            return sum;
        }

        /**
         * Returns the residual error.
         * @return residual error
         */
        public double getResidual() {
            return residual;
        }

    }

    /**
     * Result class containing the sum and the residual error in the sum.
     * @param <T> field element type
     */
    public static final class FieldSumAndResidual<T extends RealFieldElement<T>> {

        /** Sum. */
        private final T sum;
        /** Residual error. */
        private final T residual;

        /**
         * Constructs a {@link SumAndResidual} instance.
         * @param sum      sum
         * @param residual residual
         */
        private FieldSumAndResidual(final T sum, final T residual) {
            this.sum = sum;
            this.residual = residual;
        }

        /**
         * Returns the sum.
         * @return sum
         */
        public T getSum() {
            return sum;
        }

        /**
         * Returns the residual error.
         * @return residual error
         */
        public T getResidual() {
            return residual;
        }

    }

}
