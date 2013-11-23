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

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;

/** Converter providing the older {@link PotentialCoefficientsProvider} interface.
 * <p>
 * This converter is a temporary one, used to convert the new {@link UnnormalizedSphericalHarmonicsProvider}
 * interface to the old {@link PotentialCoefficientsProvider} interface for people who still
 * need it.
 * </p>
 * @since 6.0
 * @deprecated this converter is temporary for 6.x series
 */
public class ProviderConverter implements PotentialCoefficientsProvider {

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Un-normalized tesseral-sectorial coefficients matrix. */
    private final double[][] unNormalizedC;

    /** Un-normalized tesseral-sectorial coefficients matrix. */
    private final double[][] unNormalizedS;

    /** Simple constructor.
     * @param provider provider to convert
     * @exception OrekitException if provider cannot be converted
     */
    public ProviderConverter(final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {

        // constant terms
        ae = provider.getAe();
        mu = provider.getMu();

        // prepare triangular arrays
        unNormalizedC = new double[provider.getMaxDegree() + 1][];
        unNormalizedS = new double[provider.getMaxDegree() + 1][];
        for (int i = 0; i <= provider.getMaxDegree(); ++i) {
            final int order = FastMath.min(provider.getMaxOrder(), i);
            unNormalizedC[i] = new double[order + 1];
            unNormalizedS[i] = new double[order + 1];
        }

        // fill-in the arrays
        final UnnormalizedSphericalHarmonics harmonics =
                provider.onDate(provider.getReferenceDate());
        for (int i = 0; i < unNormalizedC.length; ++i) {
            for (int j = 0; j < unNormalizedC[i].length; ++j) {
                unNormalizedC[i][j] = harmonics.getUnnormalizedCnm(i, j);
                unNormalizedS[i][j] = harmonics.getUnnormalizedSnm(i, j);
            }
        }

    }

    /** {@inheritDoc} */
    @Deprecated
    public double[] getJ(final boolean normalized, final int n)
        throws OrekitException {

        // safety check
        if (n >= unNormalizedC.length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      n, unNormalizedC.length - 1);
        }

        final double[] zonal = new double[n + 1];
        if (normalized) {
            final double[][] unnormalization = GravityFieldFactory.getUnnormalizationFactors(n, 0);
            for (int i = 0; i < zonal.length; ++i) {
                zonal[i] = -unNormalizedC[i][0] / unnormalization[i][0];
            }
        } else {
            for (int i = 0; i < zonal.length; ++i) {
                zonal[i] = -unNormalizedC[i][0];
            }
        }

        return zonal;

    }

    /** {@inheritDoc} */
    @Deprecated
    public double[][] getC(final int n, final int m, final boolean normalized)
        throws OrekitException {

        // allocate the array
        final double[][] tesserals = createTesseralsArray(n, m);

        // fill-in the elements
        if (normalized) {
            final double[][] unnormalization = GravityFieldFactory.getUnnormalizationFactors(n, m);
            for (int i = 0; i < tesserals.length; ++i) {
                for (int j = 0; j < tesserals[i].length; ++j) {
                    tesserals[i][j] = unNormalizedC[i][j] / unnormalization[i][j];
                }
            }
        } else {
            for (int i = 0; i < tesserals.length; ++i) {
                for (int j = 0; j < tesserals[i].length; ++j) {
                    tesserals[i][j] = unNormalizedC[i][j];
                }
            }
        }

        return tesserals;

    }

    /** {@inheritDoc} */
    @Deprecated
    public double[][] getS(final int n, final int m, final boolean normalized)
        throws OrekitException {

        // allocate the array
        final double[][] tesserals = createTesseralsArray(n, m);

        // fill-in the elements
        if (normalized) {
            final double[][] unnormalization = GravityFieldFactory.getUnnormalizationFactors(n, m);
            for (int i = 0; i < tesserals.length; ++i) {
                for (int j = 0; j < tesserals[i].length; ++j) {
                    tesserals[i][j] = unNormalizedS[i][j] / unnormalization[i][j];
                }
            }
        } else {
            for (int i = 0; i < tesserals.length; ++i) {
                for (int j = 0; j < tesserals[i].length; ++j) {
                    tesserals[i][j] = unNormalizedS[i][j];
                }
            }
        }

        return tesserals;

    }

    /** {@inheritDoc} */
    @Deprecated
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Deprecated
    public double getAe() {
        return ae;
    }

    /** Allocate a triangular arrays for tesseral coefficients.
     * @param degree maximal degree
     * @param order maximal order
     * @return allocated triangular array
     * @exception OrekitException if degree or order is too large
     */
    private double[][] createTesseralsArray(final int degree, final int order)
        throws OrekitException {

        // safety checks
        if (degree >= unNormalizedC.length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      degree, unNormalizedC.length - 1);
        }
        if (order >= unNormalizedC[degree].length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      degree, unNormalizedC[degree].length - 1);
        }

        // allocate the array
        final double[][] tesserals = new double[degree + 1][];
        for (int i = 0; i < tesserals.length; ++i) {
            tesserals[i] = new double[FastMath.min(i, order) + 1];
        }

        return tesserals;

    }

}
