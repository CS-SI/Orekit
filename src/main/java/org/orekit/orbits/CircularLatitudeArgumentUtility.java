/* Copyright 2022-2024 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Utility methods for converting between different latitude arguments used by {@link org.orekit.orbits.CircularOrbit}.
 * @author Romain Serra
 * @see org.orekit.orbits.CircularOrbit
 * @since 12.1
 */
public class CircularLatitudeArgumentUtility {

    /** Tolerance for stopping criterion in iterative conversion from mean to eccentric angle. */
    private static final double TOLERANCE_CONVERGENCE = 1.0e-12;

    /** Maximum number of iterations in iterative conversion from mean to eccentric angle. */
    private static final int MAXIMUM_ITERATION = 50;

    /** Private constructor for utility class. */
    private CircularLatitudeArgumentUtility() {
        // nothing here (utils class)
    }

    /**
     * Computes the true latitude argument from the eccentric latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @return the true latitude argument.
     */
    public static double eccentricToTrue(final double ex, final double ey, final double alphaE) {
        final double epsilon   = eccentricAndTrueEpsilon(ex, ey);
        final SinCos scAlphaE  = FastMath.sinCos(alphaE);
        final double num       = ex * scAlphaE.sin() - ey * scAlphaE.cos();
        final double den       = epsilon + 1 - ex * scAlphaE.cos() - ey * scAlphaE.sin();
        return alphaE + eccentricAndTrueAtan(num, den);
    }

    /**
     * Computes the eccentric latitude argument from the true latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaV = V + ω true latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    public static double trueToEccentric(final double ex, final double ey, final double alphaV) {
        final double epsilon   = eccentricAndTrueEpsilon(ex, ey);
        final SinCos scAlphaV  = FastMath.sinCos(alphaV);
        final double num       = ey * scAlphaV.cos() - ex * scAlphaV.sin();
        final double den       = epsilon + 1 + ex * scAlphaV.cos() + ey * scAlphaV.sin();
        return alphaV + eccentricAndTrueAtan(num, den);
    }

    /**
     * Computes an intermediate quantity for conversions between true and eccentric.
     *
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return intermediate variable referred to as epsilon.
     */
    private static double eccentricAndTrueEpsilon(final double ex, final double ey) {
        return FastMath.sqrt(1 - ex * ex - ey * ey);
    }

    /**
     * Computes another intermediate quantity for conversions between true and eccentric.
     *
     * @param num numerator for angular conversion
     * @param den denominator for angular conversion
     * @return arc-tangent of ratio of inputs times two.
     */
    private static double eccentricAndTrueAtan(final double num, final double den) {
        return 2. * FastMath.atan(num / den);
    }

    /**
     * Computes the eccentric latitude argument from the mean latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    public static double meanToEccentric(final double ex, final double ey, final double alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE         = alphaM;
        double shift;
        double alphaEMalphaM  = 0.0;
        boolean hasConverged;
        int    iter           = 0;
        do {
            final SinCos scAlphaE = FastMath.sinCos(alphaE);
            final double f2 = ex * scAlphaE.sin() - ey * scAlphaE.cos();
            final double f1 = 1.0 - ex * scAlphaE.cos() - ey * scAlphaE.sin();
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE         = alphaM + alphaEMalphaM;

            hasConverged = FastMath.abs(shift) <= TOLERANCE_CONVERGENCE;
        } while (++iter < MAXIMUM_ITERATION && !hasConverged);

        if (!hasConverged) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LATITUDE_ARGUMENT, iter);
        }
        return alphaE;

    }

    /**
     * Computes the mean latitude argument from the eccentric latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaE = E + ω  mean latitude argument (rad)
     * @return the mean latitude argument.
     */
    public static double eccentricToMean(final double ex, final double ey, final double alphaE) {
        final SinCos scAlphaE = FastMath.sinCos(alphaE);
        return alphaE + (ey * scAlphaE.cos() - ex * scAlphaE.sin());
    }

    /**
     * Computes the mean latitude argument from the eccentric latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaV = V + ω  true latitude argument (rad)
     * @return the mean latitude argument.
     */
    public static double trueToMean(final double ex, final double ey, final double alphaV) {
        final double alphaE = trueToEccentric(ex, ey, alphaV);
        return eccentricToMean(ex, ey, alphaE);
    }

    /**
     * Computes the true latitude argument from the eccentric latitude argument.
     *
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the true latitude argument.
     */
    public static double meanToTrue(final double ex, final double ey, final double alphaM) {
        final double alphaE = meanToEccentric(ex, ey, alphaM);
        return eccentricToTrue(ex, ey, alphaE);
    }

}
