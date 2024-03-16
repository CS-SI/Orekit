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

/**
 * Interface used to provide un-normalized spherical harmonics coefficients.
 * <p>
 * Un-normalized spherical harmonics coefficients are fine for small degrees. At high
 * degree and order the un-normalized coefficients are not representable in a {@code
 * double}. {@link NormalizedSphericalHarmonicsProvider} is recommended for high precision
 * applications.
 *
 * @author Luc Maisonobe
 * @see GravityFields
 * @since 6.0
 */
public interface UnnormalizedSphericalHarmonicsProvider extends SphericalHarmonicsProvider {

    /**
     * Un-normalized spherical harmonics coefficients evaluated at a specific instant.
     * @see #onDate(AbsoluteDate)
     * @since 6.1
     */
    interface UnnormalizedSphericalHarmonics extends TimeStamped {

        /** Get a spherical harmonic cosine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return un-normalized coefficient Cnm
         */
        double getUnnormalizedCnm(int n, int m);

        /** Get a spherical harmonic sine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return un-normalized coefficient Snm
         */
        double getUnnormalizedSnm(int n, int m);

    }


    /**
     * Get the un-normalized spherical harmonic coefficients at a specific instance in time.
     *
     * @param date of evaluation (may be null if model is not time-dependent)
     * @return un-normalized coefficients on {@code date}.
     * @since 6.1
     */
    UnnormalizedSphericalHarmonics onDate(AbsoluteDate date);

}
