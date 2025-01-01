/* Copyright 2022-2025 Thales Alenia Space
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
import org.hipparchus.util.FastMath;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;

/** Official model CIPM-2007 (identical to CIPM-1981/91) from Comité International des Poids et Mesures.
 * <p>
 * This water vapor model is the one from Giacomo and Davis as indicated in IERS TN 32, chap. 9.
 * </p>
 * @see <a href="https://www.nist.gov/system/files/documents/calibrations/CIPM-2007.pdf">Revised
 * formula for the density of moist air (CIPM-2007), Metrologia 45 (2008) 149–155</a>
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class CIPM2007 implements WaterVaporPressureProvider {

    /** Laurent series coefficient for degree +2. */
    private static final double L_P2 = 1.2378847e-5;

    /** Laurent series coefficient for degree +1. */
    private static final double L_P1 = -1.9121316e-2;

    /** Laurent series coefficient for degree 0. */
    private static final double L_0 = 33.93711047;

    /** Laurent series coefficient for degree -1. */
    private static final double L_M1 = -6343.1645;

    /** Celsius temperature offset. */
    private static final double CELSIUS = 273.15;

    /** Constant enhancement factor. */
    private static final double F_0 = 1.00062;

    /** Pressure enhancement factor. */
    private static final double F_P = 3.14e-6;

    /** Temperature enhancement factor. */
    private static final double F_T2 = 5.6e-7;

    /** {@inheritDoc} */
    @Override
    public double waterVaporPressure(final double p, final double t, final double rh) {

        // saturation water vapor, equation A1.1 (now in Pa, not hPa)
        final double psv = FastMath.exp(t * (t * L_P2 + L_P1) + L_0 + L_M1 / t);

        // enhancement factor, equation A1.2
        final double tC = t - CELSIUS;
        final double fw = TroposphericModelUtils.HECTO_PASCAL.fromSI(p) * F_P + tC * tC * F_T2 + F_0;

        return rh * fw * psv;

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T waterVaporPressure(final T p, final T t, final T rh) {

        // saturation water vapor, equation A1.1 (now in Pa, not hPa)
        final T psv = FastMath.exp(t.multiply(t.multiply(L_P2).add(L_P1)).add(L_0).add(t.reciprocal().multiply(L_M1)));

        // enhancement factor, equation A1.2
        final T tC = t.subtract(CELSIUS);
        final T fw = TroposphericModelUtils.HECTO_PASCAL.fromSI(p).multiply(F_P).add(tC.multiply(tC).multiply(F_T2)).add(F_0);

        return rh.multiply(fw).multiply(psv);

    }

}
