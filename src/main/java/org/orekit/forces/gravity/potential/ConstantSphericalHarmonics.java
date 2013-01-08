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
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

/** Simple implementation of {@link SphericalHarmonicsProvider} for constant gravity fields.
 * @author Luc Maisonobe
 * @since 6.0
 */
public class ConstantSphericalHarmonics implements SphericalHarmonicsProvider {

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Un-normalized tesseral-sectorial coefficients matrix. */
    private final double[][] unNormalizedC;

    /** Un-normalized tesseral-sectorial coefficients matrix. */
    private final double[][] unNormalizedS;

    /** Simple constructor.
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param unNormalizedC un-normalized tesseral-sectorial coefficients
     * @param unNormalizedS un-normalized tesseral-sectorial coefficients
     */
    public ConstantSphericalHarmonics(final double ae, final double mu,
                                      final double[][] unNormalizedC,
                                      final double[][] unNormalizedS) {
        this.ae = ae;
        this.mu = mu;
        this.unNormalizedC = unNormalizedC;
        this.unNormalizedS = unNormalizedS;
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return unNormalizedC.length - 1;
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return unNormalizedC[unNormalizedC.length - 1].length - 1;
    }

    /** {@inheritDoc} */
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    public double getAe() {
        return ae;
    }

    /** {@inheritDoc}
     * <p>
     * For a constant field, null is always returned.
     * </p>
     */
    public AbsoluteDate getReferenceDate() {
        return null;
    }

    /** {@inheritDoc} */
    public double getOffset(final AbsoluteDate date) {
        return 0.0;
    }

    /** {@inheritDoc} */
    public double getUnnormalizedCnm(final double dateOffset, final int n, final int m)
            throws OrekitException {
        checkLimits(n, m);
        return unNormalizedC[n][m];
    }

    /** {@inheritDoc} */
    public double getUnnormalizedSnm(final double dateOffset, final int n, final int m)
            throws OrekitException {
        checkLimits(n, m);
        return unNormalizedS[n][m];
    }

    /** Check limits.
     * @param degree degree
     * @param order order
     * @exception OrekitException if indices are out of bound
     */
    private void checkLimits(final int degree, final int order)
            throws OrekitException {

        if (degree >= unNormalizedC.length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      degree, unNormalizedC.length - 1);
        }

        if (order >= unNormalizedC[degree].length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      order, unNormalizedC[degree].length - 1);
        }

    }

}

