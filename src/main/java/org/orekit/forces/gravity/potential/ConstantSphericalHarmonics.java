/* Copyright 2002-2015 CS Systèmes d'Information
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

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for constant gravity fields.
 * @author Luc Maisonobe
 * @since 6.0
 */
class ConstantSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tide system. */
    private final TideSystem tideSystem;

    /** Raw tesseral-sectorial coefficients matrix. */
    private final double[][] rawC;

    /** Raw tesseral-sectorial coefficients matrix. */
    private final double[][] rawS;

    /** Simple constructor.
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param tideSystem tide system
     * @param rawC raw tesseral-sectorial coefficients
     * @param rawS raw tesseral-sectorial coefficients
     */
    ConstantSphericalHarmonics(final double ae, final double mu,
                                      final TideSystem tideSystem,
                                      final double[][] rawC, final double[][] rawS) {
        this.ae         = ae;
        this.mu         = mu;
        this.tideSystem = tideSystem;
        this.rawC       = rawC;
        this.rawS       = rawS;
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return rawC.length - 1;
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return rawC[rawC.length - 1].length - 1;
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
    public TideSystem getTideSystem() {
        return tideSystem;
    }

    @Override
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {
        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m)
                throws OrekitException {
                checkLimits(n, m);
                return rawC[n][m];
            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m)
                throws OrekitException {
                checkLimits(n, m);
                return rawS[n][m];
            }

        };
    }

    /** Check limits.
     * @param degree degree
     * @param order order
     * @exception OrekitException if indices are out of bound
     */
    private void checkLimits(final int degree, final int order)
        throws OrekitException {

        if (degree >= rawC.length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      degree, rawC.length - 1);
        }

        if (order >= rawC[degree].length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      order, rawC[degree].length - 1);
        }

    }

}

