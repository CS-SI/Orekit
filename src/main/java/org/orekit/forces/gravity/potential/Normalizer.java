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
     * @exception OrekitException if degree and order are too large
     * and the normalization coefficients underflow
     */
    public Normalizer(final UnnormalizedSphericalHarmonicsProvider unnormalized)
        throws OrekitException {
        this.unnormalized = unnormalized;
        this.factors      = GravityFieldFactory.getUnnormalizationFactors(unnormalized.getMaxDegree(),
                                                                          unnormalized.getMaxOrder());
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return unnormalized.getMaxDegree();
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return unnormalized.getMaxOrder();
    }

    /** {@inheritDoc} */
    public double getMu() {
        return unnormalized.getMu();
    }

    /** {@inheritDoc} */
    public double getAe() {
        return unnormalized.getAe();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getReferenceDate() {
        return unnormalized.getReferenceDate();
    }

    /** {@inheritDoc} */
    public double getOffset(final AbsoluteDate date) {
        return unnormalized.getOffset(date);
    }

    /** {@inheritDoc} */
    public double getNormalizedCnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        return unnormalized.getUnnormalizedCnm(dateOffset, n, m) / factors[n][m];
    }

    /** {@inheritDoc} */
    public double getNormalizedSnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        return unnormalized.getUnnormalizedSnm(dateOffset, n, m) / factors[n][m];
    }

}
