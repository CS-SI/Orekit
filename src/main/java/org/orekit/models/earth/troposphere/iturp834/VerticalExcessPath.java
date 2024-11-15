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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;

/** Computation engine for vertical excess path in ITU-R P.834 tropospheric model.
 * <p>
 * This class implements equations 19, 20 and table 2 of the ITU-R P.834 model.
 * </p>
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
public enum VerticalExcessPath {

    /** Model for coastal areas (islands and locations less than 10km from sea shore). */
    COASTAL(5.5e-4, 2.91e-2),

    /** Non-coastal equatorial areas. */
    NON_COASTAL_EQUATORIAL(6.5e-4, 2.73e-2),

    /** All other areas. */
    NON_COASTAL_NON_EQUATORIAL(7.3e-4, 2.35e-2);

    /** Base delay coefficient. */
    private static final double L0 = 2.2768e-5;

    /** Celsius temperature offset. */
    private static final double CELSIUS = 273.15;

    /** Vertical path excess factor (m/% relative humidity). */
    private final double a;

    /** Temperature scaling factor (%C⁻¹). */
    private final double b;

    /** Simple constructor.
     * @param a vertical excess path factor (m/% relative humidity)
     * @param b temperature scaling factor (%C⁻¹)
     */
    VerticalExcessPath(final double a, final double b) {
        this.a = a;
        this.b = b;
    }

    /** Compute vertical excess path.
     * @param weather weather parameters
     * @param waterVaporPressureProvider converter between pressure, temperature and humidity and relative humidity
     * @return temperature contribution to vertical excess path
     */
    public double verticalExcessPath(final PressureTemperatureHumidity weather,
                                     final WaterVaporPressureProvider waterVaporPressureProvider) {

        // contribution of pressure
        final double relativeHumidity =
            waterVaporPressureProvider.relativeHumidity(weather.getPressure(),
                                                        weather.getTemperature(),
                                                        weather.getWaterVaporPressure());
        final double pressureContribution = weather.getPressure() * L0;

        // contribution of temperature (depends on point being close or far to water body).
        final double temperatureFunction = FastMath.pow(10.0, (weather.getTemperature() - CELSIUS) * b) * a;

        // compute vertical excess path
        return pressureContribution + temperatureFunction * relativeHumidity;

    }

    /** Compute vertical excess path.
     * @param weather weather parameters
     * @param waterVaporPressureProvider converter between pressure, temperature and humidity and relative humidity
     * @return temperature contribution to vertical excess path
     */
    public <T extends CalculusFieldElement<T>> T verticalExcessPath(final FieldPressureTemperatureHumidity<T> weather,
                                                                    final WaterVaporPressureProvider waterVaporPressureProvider) {

        // contribution of pressure
        final T relativeHumidity =
            waterVaporPressureProvider.relativeHumidity(weather.getPressure(),
                                                        weather.getTemperature(),
                                                        weather.getWaterVaporPressure());
        final T pressureContribution = weather.getPressure().multiply(L0);

        // contribution of temperature (depends on point being close or far to water body).
        final T temperatureFunction = FastMath.pow(relativeHumidity.newInstance(10.0),
                                                   weather.getTemperature().subtract(CELSIUS).multiply(b)).
                                      multiply(a);

        // compute vertical excess path
        return pressureContribution.add(temperatureFunction.multiply(relativeHumidity));

    }

}
