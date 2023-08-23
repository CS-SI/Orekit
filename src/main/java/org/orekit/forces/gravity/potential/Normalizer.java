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
package org.orekit.forces.gravity.potential;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;

/** Wrapper providing normalized coefficients from un-normalized ones.
 * @author Luc Maisonobe
 * @since 6.0
 */
class Normalizer implements NormalizedSphericalHarmonicsProvider {

    /** Un-normalized provider to which everything is delegated. */
    private final UnnormalizedSphericalHarmonicsProvider unnormalized;

    /** Factors for normalization. */
    private final double[][] factors;

    /** Simple constructor.
     * @param unnormalized provider to normalize
     */
    Normalizer(final UnnormalizedSphericalHarmonicsProvider unnormalized) {
        this.unnormalized = unnormalized;
        this.factors      = GravityFieldFactory.getUnnormalizationFactors(unnormalized.getMaxDegree(),
                                                                          unnormalized.getMaxOrder());
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return unnormalized.getMaxDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return unnormalized.getMaxOrder();
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return unnormalized.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return unnormalized.getAe();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return unnormalized.getReferenceDate();
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        return unnormalized.getTideSystem();
    }

    /** {@inheritDoc} */
    @Override
    public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) {
        final UnnormalizedSphericalHarmonics harmonics = unnormalized.onDate(date);
        return new NormalizedSphericalHarmonics() {

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            @Override
            public double getNormalizedCnm(final int n, final int m) {
                return harmonics.getUnnormalizedCnm(n, m) / factors[n][m];
            }

            /** {@inheritDoc} */
            @Override
            public double getNormalizedSnm(final int n, final int m) {
                return harmonics.getUnnormalizedSnm(n, m) / factors[n][m];
            }

        };
    }

}
