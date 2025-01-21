/* Copyright 2002-2025 CS GROUP
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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;

import java.util.Arrays;

/**
 * Temporary container for reading gravity field coefficients.
 * @since 12.2
 * @author Fabien Maussion
 */
class TemporaryCoefficientsContainer {

    /** Converter from triangular to flat form. */
    private final Flattener flattener;

    /** Cosine coefficients. */
    private final double[] c;

    /** Sine coefficients. */
    private final double[] s;

    /** Initial value for coefficients. */
    private final double initialValue;

    /** Build a container with given degree and order.
     * @param degree degree of the container
     * @param order order of the container
     * @param initialValue initial value for coefficients
     */
    TemporaryCoefficientsContainer(final int degree, final int order, final double initialValue) {
        this.flattener    = new Flattener(degree, order);
        this.c            = new double[flattener.arraySize()];
        this.s            = new double[flattener.arraySize()];
        this.initialValue = initialValue;
        Arrays.fill(c, initialValue);
        Arrays.fill(s, initialValue);
    }

    /** Build a resized container.
     * @param degree degree of the resized container
     * @param order order of the resized container
     * @return resized container
     */
    TemporaryCoefficientsContainer resize(final int degree, final int order) {
        final TemporaryCoefficientsContainer resized = new TemporaryCoefficientsContainer(degree, order, initialValue);
        for (int n = 0; n <= degree; ++n) {
            for (int m = 0; m <= FastMath.min(n, order); ++m) {
                if (flattener.withinRange(n, m)) {
                    final int rIndex = resized.flattener.index(n, m);
                    final int index  = flattener.index(n, m);
                    resized.c[rIndex] = c[index];
                    resized.s[rIndex] = s[index];
                }
            }
        }
        return resized;
    }

    /**
     * Get the converter from triangular to flat form.
     * @return the converter from triangular to flat form
     */
    Flattener getFlattener() {
        return flattener;
    }

    /**
     * Get the cosine coefficients.
     * @return the cosine coefficients
     */
    public double[] getC() {
        return c;
    }

    /**
     * Get the sine coefficients.
     * @return the cosine coefficients
     */
    public double[] getS() {
        return s;
    }
}
