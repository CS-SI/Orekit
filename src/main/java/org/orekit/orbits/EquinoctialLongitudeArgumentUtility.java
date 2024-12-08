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
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;

/**
 * Utility methods for converting between different longitude arguments used by {@link EquinoctialOrbit}.
 * @author Romain Serra
 * @see EquinoctialOrbit
 * @since 12.1
 */
public class EquinoctialLongitudeArgumentUtility {

    /** Tolerance for stopping criterion in iterative conversion from mean to eccentric angle. */
    private static final double TOLERANCE_CONVERGENCE = 1.0e-11;

    /** Maximum number of iterations in iterative conversion from mean to eccentric angle. */
    private static final int MAXIMUM_ITERATION = 50;

    /** Private constructor for utility class. */
    private EquinoctialLongitudeArgumentUtility() {
        // nothing here (utils class)
    }

    /**
     * Computes the true longitude argument from the eccentric longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lE = E + ω + Ω eccentric longitude argument (rad)
     * @return the true longitude argument.
     */
    public static double eccentricToTrue(final double ex, final double ey, final double lE) {
        final double epsilon = eccentricAndTrueEpsilon(ex, ey);
        final SinCos scLE    = FastMath.sinCos(lE);
        final double num     = ex * scLE.sin() - ey * scLE.cos();
        final double den     = epsilon + 1 - ex * scLE.cos() - ey * scLE.sin();
        return lE + eccentricAndTrueAtan(num, den);
    }

    /**
     * Computes the eccentric longitude argument from the true longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lV = V + ω + Ω true longitude argument (rad)
     * @return the eccentric longitude argument.
     */
    public static double trueToEccentric(final double ex, final double ey, final double lV) {
        final double epsilon = eccentricAndTrueEpsilon(ex, ey);
        final SinCos scLv    = FastMath.sinCos(lV);
        final double num     = ey * scLv.cos() - ex * scLv.sin();
        final double den     = epsilon + 1 + ex * scLv.cos() + ey * scLv.sin();
        return lV + eccentricAndTrueAtan(num, den);
    }

    /**
     * Computes an intermediate quantity for conversions between true and eccentric.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
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
     * Computes the eccentric longitude argument from the mean longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lM = M + ω + Ω  mean longitude argument (rad)
     * @return the eccentric longitude argument.
     */
    public static double meanToEccentric(final double ex, final double ey, final double lM) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        double lE = lM;
        double shift;
        double lEmlM = 0.0;
        boolean hasConverged;
        int iter = 0;
        do {
            final SinCos scLE  = FastMath.sinCos(lE);
            final double f2 = ex * scLE.sin() - ey * scLE.cos();
            final double f1 = 1.0 - ex * scLE.cos() - ey * scLE.sin();
            final double f0 = lEmlM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            lEmlM -= shift;
            lE     = lM + lEmlM;

            hasConverged = FastMath.abs(shift) <= TOLERANCE_CONVERGENCE;
        } while (++iter < MAXIMUM_ITERATION && !hasConverged);

        if (!hasConverged) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LONGITUDE_ARGUMENT, iter);
        }
        return lE;

    }

    /**
     * Computes the mean longitude argument from the eccentric longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lE = E + ω + Ω  mean longitude argument (rad)
     * @return the mean longitude argument.
     */
    public static double eccentricToMean(final double ex, final double ey, final double lE) {
        final SinCos scLE = FastMath.sinCos(lE);
        return lE - ex * scLE.sin() + ey * scLE.cos();
    }

    /**
     * Computes the mean longitude argument from the eccentric longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lV = V + ω + Ω  true longitude argument (rad)
     * @return the mean longitude argument.
     */
    public static double trueToMean(final double ex, final double ey, final double lV) {
        final double lE = trueToEccentric(ex, ey, lV);
        return eccentricToMean(ex, ey, lE);
    }

    /**
     * Computes the true longitude argument from the eccentric longitude argument.
     *
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
     * @param lM = M + ω + Ω  mean longitude argument (rad)
     * @return the true longitude argument.
     */
    public static double meanToTrue(final double ex, final double ey, final double lM) {
        final double lE = meanToEccentric(ex, ey, lM);
        return eccentricToTrue(ex, ey, lE);
    }

    /**
     * Convert argument of longitude.
     * @param oldType old position angle type
     * @param l old value for argument of longitude
     * @param ex ex
     * @param ey ey
     * @param newType new position angle type
     * @return converted argument of longitude
     * @since 12.2
     */
    public static double convertL(final PositionAngleType oldType, final double l, final double ex,
                                  final double ey, final PositionAngleType newType) {
        if (oldType == newType) {
            return l;

        } else {
            switch (newType) {

                case ECCENTRIC:
                    if (oldType == PositionAngleType.MEAN) {
                        return EquinoctialLongitudeArgumentUtility.meanToEccentric(ex, ey, l);
                    } else {
                        return EquinoctialLongitudeArgumentUtility.trueToEccentric(ex, ey, l);
                    }

                case MEAN:
                    if (oldType == PositionAngleType.TRUE) {
                        return EquinoctialLongitudeArgumentUtility.trueToMean(ex, ey, l);
                    } else {
                        return EquinoctialLongitudeArgumentUtility.eccentricToMean(ex, ey, l);
                    }

                case TRUE:
                    if (oldType == PositionAngleType.MEAN) {
                        return EquinoctialLongitudeArgumentUtility.meanToTrue(ex, ey, l);
                    } else {
                        return EquinoctialLongitudeArgumentUtility.eccentricToTrue(ex, ey, l);
                    }

                default:
                    throw new OrekitInternalError(null);
            }
        }
    }
}
