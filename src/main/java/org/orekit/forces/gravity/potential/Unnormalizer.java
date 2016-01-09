/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;

/** Wrapper providing un-normalized coefficients from normalized ones.
 * @author Luc Maisonobe
 * @since 6.0
 */
class Unnormalizer implements UnnormalizedSphericalHarmonicsProvider {

    /** Normalized provider to which everything is delegated. */
    private final NormalizedSphericalHarmonicsProvider normalized;

    /** Factors for un-normalization. */
    private final double[][] factors;

    /** Simple constructor.
     * @param normalized provider to un-normalize
     * @exception OrekitException if degree and order are too large
     * and the un-normalization coefficients underflow
     */
    Unnormalizer(final NormalizedSphericalHarmonicsProvider normalized)
        throws OrekitException {
        this.normalized = normalized;
        this.factors    = GravityFieldFactory.getUnnormalizationFactors(normalized.getMaxDegree(),
                                                                        normalized.getMaxOrder());
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return normalized.getMaxDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return normalized.getMaxOrder();
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return normalized.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return normalized.getAe();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return normalized.getReferenceDate();
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final AbsoluteDate date) {
        return normalized.getOffset(date);
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        return normalized.getTideSystem();
    }

    /** {@inheritDoc} */
    @Override
    public UnnormalizedSphericalHarmonics onDate(final AbsoluteDate date) throws OrekitException {
        final NormalizedSphericalHarmonics harmonics = normalized.onDate(date);
        return new UnnormalizedSphericalHarmonics() {

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            @Override
            public double getUnnormalizedCnm(final int n, final int m) throws OrekitException {
                return harmonics.getNormalizedCnm(n, m) * factors[n][m];
            }

            /** {@inheritDoc} */
            @Override
            public double getUnnormalizedSnm(final int n, final int m) throws OrekitException {
                return harmonics.getNormalizedSnm(n, m) * factors[n][m];
            }

        };
    }

}
