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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;

/**
 * Utility methods for converting between different latitude arguments used by {@link FieldCircularOrbit}.
 * @author Romain Serra
 * @see FieldCircularOrbit
 * @since 12.1
 */
public class FieldCircularLatitudeArgumentUtility {

    /** Tolerance for stopping criterion in iterative conversion from mean to eccentric angle. */
    private static final double TOLERANCE_CONVERGENCE = 1.0e-11;

    /** Maximum number of iterations in iterative conversion from mean to eccentric angle. */
    private static final int MAXIMUM_ITERATION = 50;

    /** Private constructor for utility class. */
    private FieldCircularLatitudeArgumentUtility() {
        // nothing here (utils class)
    }

    /**
     * Computes the true latitude argument from the eccentric latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @return the true latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T eccentricToTrue(final T ex, final T ey, final T alphaE) {
        final T epsilon               = eccentricAndTrueEpsilon(ex, ey);
        final FieldSinCos<T> scAlphaE = FastMath.sinCos(alphaE);
        final T cosAlphaE             = scAlphaE.cos();
        final T sinAlphaE             = scAlphaE.sin();
        final T num                   = ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE));
        final T den                   = epsilon.add(1).subtract(ex.multiply(cosAlphaE)).subtract(ey.multiply(sinAlphaE));
        return alphaE.add(eccentricAndTrueAtan(num, den));
    }

    /**
     * Computes the eccentric latitude argument from the true latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaV = v + ω true latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T trueToEccentric(final T ex, final T ey, final T alphaV) {
        final T epsilon               = eccentricAndTrueEpsilon(ex, ey);
        final FieldSinCos<T> scAlphaV = FastMath.sinCos(alphaV);
        final T cosAlphaV             = scAlphaV.cos();
        final T sinAlphaV             = scAlphaV.sin();
        final T num                   = ey.multiply(cosAlphaV).subtract(ex.multiply(sinAlphaV));
        final T den                   = epsilon.add(1).add(ex.multiply(cosAlphaV).add(ey.multiply(sinAlphaV)));
        return alphaV.add(eccentricAndTrueAtan(num, den));
    }

    /**
     * Computes an intermediate quantity for conversions between true and eccentric.
     *
     * @param <T>    Type of the field elements
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return intermediate variable referred to as epsilon.
     */
    private static <T extends CalculusFieldElement<T>> T eccentricAndTrueEpsilon(final T ex, final T ey) {
        return (ex.square().negate().subtract(ey.square()).add(1.)).sqrt();
    }

    /**
     * Computes another intermediate quantity for conversions between true and eccentric.
     *
     * @param <T>    Type of the field elements
     * @param num numerator for angular conversion
     * @param den denominator for angular conversion
     * @return arc-tangent of ratio of inputs times two.
     */
    private static <T extends CalculusFieldElement<T>> T eccentricAndTrueAtan(final T num, final T den) {
        return (num.divide(den)).atan().multiply(2);
    }

    /**
     * Computes the eccentric latitude argument from the mean latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T meanToEccentric(final T ex, final T ey, final T alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)

        T alphaE                = alphaM;
        T shift;
        T alphaEMalphaM         = alphaM.getField().getZero();
        boolean hasConverged;
        int    iter     = 0;
        do {
            final FieldSinCos<T> scAlphaE = FastMath.sinCos(alphaE);
            final T f2 = ex.multiply(scAlphaE.sin()).subtract(ey.multiply(scAlphaE.cos()));
            final T f1 = ex.negate().multiply(scAlphaE.cos()).subtract(ey.multiply(scAlphaE.sin())).add(1);
            final T f0 = alphaEMalphaM.subtract(f2);

            final T f12 = f1.multiply(2);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            alphaEMalphaM  = alphaEMalphaM.subtract(shift);
            alphaE         = alphaM.add(alphaEMalphaM);

            hasConverged = FastMath.abs(shift.getReal()) <= TOLERANCE_CONVERGENCE;
        } while (++iter < MAXIMUM_ITERATION && !hasConverged);

        if (!hasConverged) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LATITUDE_ARGUMENT, iter);
        }
        return alphaE;

    }

    /**
     * Computes the mean latitude argument from the eccentric latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaE = E + ω  eccentric latitude argument (rad)
     * @return the mean latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T eccentricToMean(final T ex, final T ey, final T alphaE) {
        final FieldSinCos<T> scAlphaE = FastMath.sinCos(alphaE);
        return alphaE.subtract(ex.multiply(scAlphaE.sin()).subtract(ey.multiply(scAlphaE.cos())));
    }

    /**
     * Computes the mean latitude argument from the eccentric latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaV = V + ω  true latitude argument (rad)
     * @return the mean latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T trueToMean(final T ex, final T ey, final T alphaV) {
        final T alphaE = trueToEccentric(ex, ey, alphaV);
        return eccentricToMean(ex, ey, alphaE);
    }

    /**
     * Computes the true latitude argument from the eccentric latitude argument.
     *
     * @param <T>    Type of the field elements
     * @param ex     e cos(ω), first component of circular eccentricity vector
     * @param ey     e sin(ω), second component of circular eccentricity vector
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the true latitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T meanToTrue(final T ex, final T ey, final T alphaM) {
        final T alphaE = meanToEccentric(ex, ey, alphaM);
        return eccentricToTrue(ex, ey, alphaE);
    }

    /**
     * Convert argument of latitude.
     * @param oldType old position angle type
     * @param alpha old value for argument of latitude
     * @param ex ex
     * @param ey ey
     * @param newType new position angle type
     * @param <T> field type
     * @return convert argument of latitude
     * @since 12.2
     */
    public static <T extends CalculusFieldElement<T>> T convertAlpha(final PositionAngleType oldType, final T alpha,
                                                                     final T ex, final T ey,
                                                                     final PositionAngleType newType) {
        if (oldType == newType) {
            return alpha;

        } else {
            switch (newType) {

                case ECCENTRIC:
                    if (oldType == PositionAngleType.MEAN) {
                        return FieldCircularLatitudeArgumentUtility.meanToEccentric(ex, ey, alpha);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.trueToEccentric(ex, ey, alpha);
                    }

                case MEAN:
                    if (oldType == PositionAngleType.TRUE) {
                        return FieldCircularLatitudeArgumentUtility.trueToMean(ex, ey, alpha);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.eccentricToMean(ex, ey, alpha);
                    }

                case TRUE:
                    if (oldType == PositionAngleType.MEAN) {
                        return FieldCircularLatitudeArgumentUtility.meanToTrue(ex, ey, alpha);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.eccentricToTrue(ex, ey, alpha);
                    }

                default:
                    throw new OrekitInternalError(null);
            }
        }
    }
}
