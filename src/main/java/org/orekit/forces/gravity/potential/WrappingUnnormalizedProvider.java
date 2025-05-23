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

import org.orekit.forces.gravity.potential.RawSphericalHarmonicsProvider.RawSphericalHarmonics;
import org.orekit.time.AbsoluteDate;

/** Wrapper providing un-normalized coefficients.
 * <p>
 * The caller <em>must</em> ensure by itself that the raw provider already
 * stores (and provides) un-normalized coefficients.
 * </p>
 * @author Luc Maisonobe
 * @since 6.0
 */
class WrappingUnnormalizedProvider implements UnnormalizedSphericalHarmonicsProvider {

    /** Raw provider to which everything is delegated. */
    private final RawSphericalHarmonicsProvider rawProvider;

    /** Simple constructor.
     * @param rawProvider raw provider to which everything is delegated
     */
    WrappingUnnormalizedProvider(final RawSphericalHarmonicsProvider rawProvider) {
        this.rawProvider = rawProvider;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return rawProvider.getMaxDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return rawProvider.getMaxOrder();
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return rawProvider.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return rawProvider.getAe();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return rawProvider.getReferenceDate();
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        return rawProvider.getTideSystem();
    }

    @Override
    public UnnormalizedSphericalHarmonics onDate(final AbsoluteDate date) {
        final RawSphericalHarmonics raw = rawProvider.onDate(date);
        return new UnnormalizedSphericalHarmonics() {

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            @Override
            public double getUnnormalizedCnm(final int n, final int m) {
                // no conversion is done here
                return raw.getRawCnm(n, m);
            }

            /** {@inheritDoc} */
            @Override
            public double getUnnormalizedSnm(final int n, final int m) {
                // no conversion is done here
                return raw.getRawSnm(n, m);
            }

        };
    }
}
