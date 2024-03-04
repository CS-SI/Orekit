/* Copyright 2002-2024 Thales Alenia Space
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
import org.hipparchus.analysis.interpolation.SplineInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialSplineFunction;
import org.hipparchus.util.FastMath;

/** Steam table from US National Bureau of Standards (NBS) and National Research Council (NRC) of Canada.
 * <p>
 * The table is an extract from table 1 in <a href="https://www.thermopedia.com/content/1150/">Thermopedia</a>,
 * using only the pressure column and truncated to 99°C (the original table goes up to 373.976°C). According to
 * <a href="https://www.thermopedia.com/access/">the access page</a>, this data is available for free.
 * </p>
 * @see <a href="https://dx.doi.org/10.1615/AtoZ.s.steam_tables">Thermopedia Steam Tables</a>
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class NbsNrcSteamTable implements WaterVaporPressureProvider {

    /** Celsius temperature offset. */
    private static final double CELSIUS = 273.15;

    /** Minimum temperature of the model, at the triple point of water, i.e. 273.16K (which is 0.01°C). */
    private static final double MIN_T = CELSIUS + 0.01;

    /** Saturation pressure model. */
    private static final PolynomialSplineFunction MODEL;

    static {

        // saturation pressure in SI units (Pa)
        final double[] pressure = {
            00611.73,    657.16,    706.05,    758.13,    813.59,    872.60,    935.37,   1002.09,   1072.97,   1148.25,
            01228.10,   1312.90,   1402.70,   1497.90,   1598.80,   1705.60,   1818.50,   1938.00,   2064.40,   2197.90,
            02338.80,   2487.70,   2644.70,   2810.40,   2985.00,   3169.10,   3362.90,   3567.00,   3781.80,   4007.80,
            04245.50,   4495.30,   4757.80,   5033.50,   5322.90,   5626.70,   5945.40,   6279.50,   6629.80,   6996.90,
            07381.40,   7784.00,   8205.40,   8646.40,   9107.60,   9589.80,  10093.80,  10620.50,  11170.60,  11744.90,
            12344.00,  12970.00,  13623.00,  14303.00,  15012.00,  15752.00,  16522.00,  17324.00,  18159.00,  19028.00,
            19932.00,  20873.00,  21851.00,  22868.00,  23925.00,  25022.00,  26163.00,  27347.00,  28576.00,  29852.00,
            31176.00,  32549.00,  33972.00,  35448.00,  36978.00,  38563.00,  40205.00,  41905.00,  43665.00,  45487.00,
            47373.00,  49324.00,  51342.00,  53428.00,  55585.00,  57815.00,  60119.00,  62499.00,  64958.00,  67496.00,
            70117.00,  72823.00,  75614.00,  78495.00,  81465.00,  84529.00,  87688.00,  90945.00,  94301.00,  97759.00
        };

        // the table first entry is at 0.01°C, not 0.00°C, but remaining entries are 1°C, 2°C, … 99°C
        final double[] temperature = new double[pressure.length];
        for (int i = 0; i < temperature.length; ++i) {
            temperature[i] = (i == 0) ? MIN_T : (CELSIUS + i);
        }

        MODEL = new SplineInterpolator().interpolate(temperature, pressure);

    }

    /** {@inheritDoc} */
    @Override
    public double waterVaporPressure(final double p, final double t, final double rh) {
        return MODEL.value(FastMath.max(t, MIN_T)) * rh;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T waterVaporPressure(final T p, final T t, final T rh) {
        return MODEL.value(FastMath.max(t, MIN_T)).multiply(rh);
    }

}
