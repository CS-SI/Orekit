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
 * Utility methods for converting between different longitude arguments used by {@link FieldEquinoctialOrbit}.
 * @author Romain Serra
 * @see FieldEquinoctialOrbit
 * @since 12.1
 */
public class FieldEquinoctialLongitudeArgumentUtility {

    /** Tolerance for stopping criterion in iterative conversion from mean to eccentric angle. */
    private static final double TOLERANCE_CONVERGENCE = 1.0e-11;

    /** Maximum number of iterations in iterative conversion from mean to eccentric angle. */
    private static final int MAXIMUM_ITERATION = 50;

    /** Private constructor for utility class. */
    private FieldEquinoctialLongitudeArgumentUtility() {
        // nothing here (utils class)
    }

    /**
     * Computes the true longitude argument from the eccentric longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lE  = E + ω + Ω eccentric longitude argument (rad)
     * @return the true longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T eccentricToTrue(final T ex, final T ey, final T lE) {
        final T epsilon           = eccentricAndTrueEpsilon(ex, ey);
        final FieldSinCos<T> scLE = FastMath.sinCos(lE);
        final T cosLE             = scLE.cos();
        final T sinLE             = scLE.sin();
        final T num               = ex.multiply(sinLE).subtract(ey.multiply(cosLE));
        final T den               = epsilon.add(1).subtract(ex.multiply(cosLE)).subtract(ey.multiply(sinLE));
        return lE.add(eccentricAndTrueAtan(num, den));
    }

    /**
     * Computes the eccentric longitude argument from the true longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lV  = V + ω + Ω true longitude argument (rad)
     * @return the eccentric longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T trueToEccentric(final T ex, final T ey, final T lV) {
        final T epsilon           = eccentricAndTrueEpsilon(ex, ey);
        final FieldSinCos<T> scLv = FastMath.sinCos(lV);
        final T cosLv             = scLv.cos();
        final T sinLv             = scLv.sin();
        final T num               = ey.multiply(cosLv).subtract(ex.multiply(sinLv));
        final T den               = epsilon.add(1).add(ex.multiply(cosLv)).add(ey.multiply(sinLv));
        return lV.add(eccentricAndTrueAtan(num, den));
    }

    /**
     * Computes an intermediate quantity for conversions between true and eccentric.
     *
     * @param <T>    Type of the field elements
     * @param ex e cos(ω), first component of eccentricity vector
     * @param ey e sin(ω), second component of eccentricity vector
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
     * Computes the eccentric longitude argument from the mean longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lM  = M + ω + Ω  mean longitude argument (rad)
     * @return the eccentric longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T meanToEccentric(final T ex, final T ey, final T lM) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        T lE = lM;
        T shift;
        T lEmlM = lM.getField().getZero();
        boolean hasConverged;
        int iter = 0;
        do {
            final FieldSinCos<T> scLE = FastMath.sinCos(lE);
            final T f2 = ex.multiply(scLE.sin()).subtract(ey.multiply(scLE.cos()));
            final T f1 = ex.multiply(scLE.cos()).add(ey.multiply(scLE.sin())).negate().add(1);
            final T f0 = lEmlM.subtract(f2);

            final T f12 = f1.multiply(2.0);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            lEmlM = lEmlM.subtract(shift);
            lE     = lM.add(lEmlM);

            hasConverged = FastMath.abs(shift.getReal()) <= TOLERANCE_CONVERGENCE;
        } while (++iter < MAXIMUM_ITERATION && !hasConverged);

        if (!hasConverged) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LONGITUDE_ARGUMENT, iter);
        }
        return lE;

    }

    /**
     * Computes the mean longitude argument from the eccentric longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lE  = E + ω + Ω  mean longitude argument (rad)
     * @return the mean longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T eccentricToMean(final T ex, final T ey, final T lE) {
        final FieldSinCos<T> scLE = FastMath.sinCos(lE);
        return lE.subtract(ex.multiply(scLE.sin())).add(ey.multiply(scLE.cos()));
    }

    /**
     * Computes the mean longitude argument from the eccentric longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lV  = V + ω + Ω  true longitude argument (rad)
     * @return the mean longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T trueToMean(final T ex, final T ey, final T lV) {
        final T alphaE = trueToEccentric(ex, ey, lV);
        return eccentricToMean(ex, ey, alphaE);
    }

    /**
     * Computes the true longitude argument from the eccentric longitude argument.
     *
     * @param <T> Type of the field elements
     * @param ex  e cos(ω), first component of eccentricity vector
     * @param ey  e sin(ω), second component of eccentricity vector
     * @param lM  = M + ω + Ω  mean longitude argument (rad)
     * @return the true longitude argument.
     */
    public static <T extends CalculusFieldElement<T>> T meanToTrue(final T ex, final T ey, final T lM) {
        final T alphaE = meanToEccentric(ex, ey, lM);
        return eccentricToTrue(ex, ey, alphaE);
    }

    /**
     * Convert argument of longitude.
     * @param oldType old position angle type
     * @param l old value for argument of longitude
     * @param ex ex
     * @param ey ey
     * @param newType new position angle type
     * @param <T> field type
     * @return converted argument of longitude
     * @since 12.2
     */
    public static <T extends CalculusFieldElement<T>> T convertL(final PositionAngleType oldType, final T l,
                                                                 final T ex, final T ey, final PositionAngleType newType) {
        if (oldType == newType) {
            return l;

        } else {
            switch (newType) {

                case ECCENTRIC:
                    if (oldType == PositionAngleType.MEAN) {
                        return FieldEquinoctialLongitudeArgumentUtility.meanToEccentric(ex, ey, l);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.trueToEccentric(ex, ey, l);
                    }

                case MEAN:
                    if (oldType == PositionAngleType.TRUE) {
                        return FieldEquinoctialLongitudeArgumentUtility.trueToMean(ex, ey, l);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.eccentricToMean(ex, ey, l);
                    }

                case TRUE:
                    if (oldType == PositionAngleType.MEAN) {
                        return FieldEquinoctialLongitudeArgumentUtility.meanToTrue(ex, ey, l);
                    } else {
                        return FieldEquinoctialLongitudeArgumentUtility.eccentricToTrue(ex, ey, l);
                    }

                default:
                    throw new OrekitInternalError(null);
            }
        }
    }
}
