/* Copyright 2002-2013 CS Systèmes d'Information
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
    public Unnormalizer(final NormalizedSphericalHarmonicsProvider normalized)
        throws OrekitException {
        this.normalized = normalized;
        this.factors    = GravityFieldFactory.getUnnormalizationFactors(normalized.getMaxDegree(),
                                                                        normalized.getMaxOrder());
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return normalized.getMaxDegree();
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return normalized.getMaxOrder();
    }

    /** {@inheritDoc} */
    public double getMu() {
        return normalized.getMu();
    }

    /** {@inheritDoc} */
    public double getAe() {
        return normalized.getAe();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getReferenceDate() {
        return normalized.getReferenceDate();
    }

    /** {@inheritDoc} */
    public double getOffset(final AbsoluteDate date) {
        return normalized.getOffset(date);
    }

    /** {@inheritDoc} */
    public double getUnnormalizedCnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        return normalized.getNormalizedCnm(dateOffset, n, m) * factors[n][m];
    }

    /** {@inheritDoc} */
    public double getUnnormalizedSnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        return normalized.getNormalizedSnm(dateOffset, n, m) * factors[n][m];
    }

}
