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
package org.orekit.models.earth.troposphere;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;

/**
 * Utility class for tropospheric models.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class TroposphericModelUtils {

    /**
     * Private constructor as class is a utility.
     */
    private TroposphericModelUtils() {
        // Nothing to do
    }

    /** Compute the mapping function related to the coefficient values and the elevation.
     * @param a a coefficient
     * @param b b coefficient
     * @param c c coefficient
     * @param elevation the elevation of the satellite, in radians.
     * @return the value of the function at a given elevation
     */
    public static double mappingFunction(final double a, final double b, final double c, final double elevation) {
        final double sinE = FastMath.sin(elevation);
        // Numerator
        final double numMP = 1 + a / (1 + b / (1 + c));
        // Denominator
        final double denMP = sinE + a / (sinE + b / (sinE + c));

        final double fElevation = numMP / denMP;

        return fElevation;
    }

    /** Compute the mapping function related to the coefficient values and the elevation.
     * @param <T> type of the elements
     * @param a a coefficient
     * @param b b coefficient
     * @param c c coefficient
     * @param elevation the elevation of the satellite, in radians.
     * @return the value of the function at a given elevation
     */
    public static <T extends CalculusFieldElement<T>> T mappingFunction(final T a, final T b, final T c, final T elevation) {
        final T sinE = FastMath.sin(elevation);
        // Numerator
        final T numMP = a.divide(b.divide(c.add(1.0)).add(1.0)).add(1.0);
        // Denominator
        final T denMP = a.divide(b.divide(c.add(sinE)).add(sinE)).add(sinE);

        final T fElevation = numMP.divide(denMP);

        return fElevation;
    }

    /** This method computes the height correction for the hydrostatic
     *  component of the mapping function.
     *  The formulas are given by Neill's paper, 1996:
     *<p>
     *      Niell A. E. (1996)
     *      "Global mapping functions for the atmosphere delay of radio wavelengths,”
     *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048.
     *</p>
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @return the height correction, in m
     */
    public static double computeHeightCorrection(final double elevation, final double height) {
        final double fixedHeight = FastMath.max(0.0, height);
        final double sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final double function = TroposphericModelUtils.mappingFunction(2.53e-5, 5.49e-3, 1.14e-3, elevation);
        // Ref: Eq. 6
        final double dmdh = (1 / sinE) - function;
        // Ref: Eq. 7
        final double correction = dmdh * (fixedHeight / 1000.0);
        return correction;
    }

    /** This method computes the height correction for the hydrostatic
     *  component of the mapping function.
     *  The formulas are given by Neill's paper, 1996:
     *<p>
     *      Niell A. E. (1996)
     *      "Global mapping functions for the atmosphere delay of radio wavelengths,”
     *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048.
     *</p>
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @param field field to which the elements belong
     * @return the height correction, in m
     */
    public static <T extends CalculusFieldElement<T>> T computeHeightCorrection(final T elevation, final T height, final Field<T> field) {
        final T zero = field.getZero();
        final T fixedHeight = FastMath.max(zero, height);
        final T sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final T function = TroposphericModelUtils.mappingFunction(zero.add(2.53e-5), zero.add(5.49e-3), zero.add(1.14e-3), elevation);
        // Ref: Eq. 6
        final T dmdh = sinE.reciprocal().subtract(function);
        // Ref: Eq. 7
        final T correction = dmdh.multiply(fixedHeight.divide(1000.0));
        return correction;
    }

}
