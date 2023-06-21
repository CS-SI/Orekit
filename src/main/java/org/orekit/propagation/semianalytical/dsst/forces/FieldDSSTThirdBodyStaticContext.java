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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;

/**
 * This class is a container for the common "Field" parameters used in
 * {@link DSSTThirdBody}.
 * <p>
 * It performs parameters initialization at each integration step for the third
 * body attraction perturbation. These parameters are initialize as soon as
 * possible. In fact, they are initialized once with short period terms and
 * don't evolve during propagation.
 * <p>
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class FieldDSSTThirdBodyStaticContext<T extends CalculusFieldElement<T>> extends FieldForceModelContext<T> {

    /** Max power for a/R3 in the serie expansion. */
    private int maxAR3Pow;

    /** Max power for e in the serie expansion. */
    private int maxEccPow;

    /** Max frequency of F. */
    private int maxFreqF;

    /**
     * Constructor.
     *
     * @param context force model context
     * @param parameters force model parameters
     */
    public FieldDSSTThirdBodyStaticContext(final FieldDSSTThirdBodyDynamicContext<T> context,
                                           final T[] parameters) {
        super(context.getFieldAuxiliaryElements());

        // Auxiliary elements
        final FieldAuxiliaryElements<T> aux = context.getFieldAuxiliaryElements();

        // Factorials computation
        final int dim = 2 * DSSTThirdBody.MAX_POWER;
        final double[] fact = new double[dim];
        fact[0] = 1.;
        for (int i = 1; i < dim; i++) {
            fact[i] = i * fact[i - 1];
        }

        // Truncation tolerance.
        final double aor = aux.getSma().getReal() / context.getR3().getReal();
        final double tol = (aor > .3 || aor > .15 && aux.getEcc().getReal() > .25) ? DSSTThirdBody.BIG_TRUNCATION_TOLERANCE : DSSTThirdBody.SMALL_TRUNCATION_TOLERANCE;

        // Utilities for truncation
        // Set a lower bound for eccentricity
        final double eo2 = FastMath.max(0.0025, 0.5 * aux.getEcc().getReal());
        final double x2o2 = context.getXX().getReal() / 2.;
        final double[] eccPwr = new double[DSSTThirdBody.MAX_POWER];
        final double[] chiPwr = new double[DSSTThirdBody.MAX_POWER];
        eccPwr[0] = 1.;
        chiPwr[0] = context.getX().getReal();
        for (int i = 1; i < DSSTThirdBody.MAX_POWER; i++) {
            eccPwr[i] = eccPwr[i - 1] * eo2;
            chiPwr[i] = chiPwr[i - 1] * x2o2;
        }

        // Auxiliary quantities.
        final double ao2rxx = aor / (2. * context.getXX().getReal());
        double xmuarn =
            ao2rxx *
                        ao2rxx * parameters[0].getReal() /
                        (context.getX().getReal() * context.getR3().getReal());
        double term = 0.;

        // Compute max power for a/R3 and e.
        maxAR3Pow = 2;
        maxEccPow = 0;
        int n = 2;
        int m = 2;
        int nsmd2 = 0;

        do {
            // Upper bound for Tnm.
            term =
                xmuarn *
                   (fact[n + m] / (fact[nsmd2] * fact[nsmd2 + m])) *
                   (fact[n + m + 1] / (fact[m] * fact[n + 1])) *
                   (fact[n - m + 1] / fact[n + 1]) * eccPwr[m] *
                   UpperBounds.getDnl(context.getXX().getReal(), chiPwr[m], n + 2, m);

            if (term < tol) {
                if (m == 0) {
                    break;
                } else if (m < 2) {
                    xmuarn *= ao2rxx;
                    m = 0;
                    n++;
                    nsmd2++;
                } else {
                    m -= 2;
                    nsmd2++;
                }
            } else {
                maxAR3Pow = n;
                maxEccPow = FastMath.max(m, maxEccPow);
                xmuarn *= ao2rxx;
                m++;
                n++;
            }
        } while (n < DSSTThirdBody.MAX_POWER);

        maxEccPow = FastMath.min(maxAR3Pow, maxEccPow);
        maxFreqF = maxAR3Pow + 1;

    }

    /**
     * Get the value of max power for a/R3 in the serie expansion.
     *
     * @return maxAR3Pow
     */
    public int getMaxAR3Pow() {
        return maxAR3Pow;
    }

    /**
     * Get the value of max power for e in the serie expansion.
     *
     * @return maxEccPow
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /**
     * Get the value of max frequency of F.
     *
     * @return maxFreqF
     */
    public int getMaxFreqF() {
        return maxFreqF;
    }

}