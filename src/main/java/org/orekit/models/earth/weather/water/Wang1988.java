/* Copyright 2023 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.weather.water;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;

/** Conversion polynomial from "The Principle of the GPS Precise Positioning System", Wang et al, 1988.
 * <p>
 * This corresponds to equation 5.96 in Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class Wang1988 implements WaterVaporPressureProvider {

    /** Coefficients for the partial pressure of water vapor polynomial. */
    private static final double[] E_COEFFICIENTS = {
        -37.2465, 0.213166, -0.000256908
    };

    /** Conversion polynomial. */
    private static final PolynomialFunction E_POLYNOMIAL = new PolynomialFunction(E_COEFFICIENTS);

    /** {@inheritDoc} */
    @Override
    public double waterVaporPressure(final double p, final double t, final double rh) {
        // the 100.0 factor is due to conversion from hPa to Pa
        return rh * 100.0 * FastMath.exp(E_POLYNOMIAL.value(t));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T waterVaporPressure(final T p, final T t, final T rh) {
        // the 100.0 factor is due to conversion from hPa to Pa
        return rh.multiply(100.0).multiply(FastMath.exp(E_POLYNOMIAL.value(t)));
    }

}
