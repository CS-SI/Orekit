/* Copyright 2002-2024 CS GROUP
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

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Interface used to provide normalized spherical harmonics coefficients.
 * @see GravityFields
 * @author Luc Maisonobe
 * @since 6.0
 */
public interface NormalizedSphericalHarmonicsProvider extends SphericalHarmonicsProvider {

    /**
     * The normalized geopotential coefficients at a specific instance in time.
     * @see NormalizedSphericalHarmonicsProvider
     * @see NormalizedSphericalHarmonicsProvider#onDate(AbsoluteDate)
     * @since 6.1
     */
    interface NormalizedSphericalHarmonics extends TimeStamped {

        /** Get a spherical harmonic cosine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return normalized coefficient Cnm
         */
        double getNormalizedCnm(int n, int m);

        /** Get a spherical harmonic sine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return normalized coefficient Snm
         */
        double getNormalizedSnm(int n, int m);

    }

    /**
     * Get the normalized spherical harmonic coefficients at a specific instance in time.
     *
     * @param date of evaluation
     * @return normalized coefficients on {@code date}.
     * @since 6.1
     */
    NormalizedSphericalHarmonics onDate(AbsoluteDate date);

    /**
     * Get the normalized coefficient of degree 2 and order 0 at a specific instance in time.
     *
     * @param date of evaluation (may be null if model is not time-dependent)
     * @return normalized C20 on {@code date}.
     * @since 12.1
     */
    default double getNormalizedC20(final AbsoluteDate date) {
        return onDate(date).getNormalizedCnm(2, 0);
    }

}
