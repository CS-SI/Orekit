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
package org.orekit.models.earth.weather;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Converter for weather parameters that change with height.
 * <p>
 * Height variations correspond to equations 5.98, 5.99 and 5.100 from
 * Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class HeightDependentPressureTemperatureHumidityConverter {

    /** Water pressure provider for water vapor pressure. */
    private final WaterVaporPressureProvider provider;

    /** Simple constructor.
     * <p>
     * Points outside of altitude range will be silently clipped back to range.
     * </p>
     * @param provider provider for water vapor pressure
     */
    public HeightDependentPressureTemperatureHumidityConverter(final WaterVaporPressureProvider provider) {
        this.provider = provider;
    }

    /** Convert weather parameters.
     * @param pth0 weather at reference altitude
     * @param h altitude at which weather is requested
     * @return converted weather
     */
    public PressureTemperatureHumidity convert(final PressureTemperatureHumidity pth0,
                                               final double h) {

        // retrieve parameters at reference altitude
        final double rh0 = provider.relativeHumidity(pth0.getPressure(), pth0.getTemperature(), pth0.getWaterVaporPressure());

        // compute changes due to altitude change
        final double dh = h - pth0.getAltitude();
        final double p  = pth0.getPressure() * FastMath.pow(1.0 - 2.26e-5 * dh, 5.225);
        final double t  = pth0.getTemperature() - 6.5e-3 * dh;
        final double rh = rh0 * FastMath.exp(-6.396e-4 * dh);

        return new PressureTemperatureHumidity(h, p, t, provider.waterVaporPressure(p, t, rh),
                                               pth0.getTm(), pth0.getLambda());

    }

    /** Convert weather parameters.
     * @param <T> type of the elements
     * @param pth0 weather at reference altitude
     * @param h altitude at which weather is requested
     * @return converted weather
     */
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> convert(final FieldPressureTemperatureHumidity<T> pth0,
                                                                                           final T h) {
        // retrieve parameters at reference altitude
        final T rh0 = provider.relativeHumidity(pth0.getPressure(), pth0.getTemperature(), pth0.getWaterVaporPressure());

        // compute changes due to altitude change
        final T dh = h.subtract(pth0.getAltitude());
        final T t  = pth0.getTemperature().subtract(dh.multiply(6.5e-3));
        final T p  = pth0.getPressure().multiply(dh.multiply(2.26e-5).negate().add(1.0).pow(5.225));
        final T rh = rh0.multiply(FastMath.exp(dh.multiply(-6.396e-4)));
        return new FieldPressureTemperatureHumidity<>(h, p, t, provider.waterVaporPressure(p, t, rh),
                                                      pth0.getTm(), pth0.getLambda());
    }

    /** Generate a provider applying altitude dependency to fixed weather parameters.
     * @param basePTH base weather parameters
     * @return a provider that applies altitude dependency
     * @since 13.0
     */
    public PressureTemperatureHumidityProvider getProvider(final PressureTemperatureHumidity basePTH) {
        return new PressureTemperatureHumidityProvider() {

            /** {@inheritDoc} */
            @Override
            public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location,
                                                                    final AbsoluteDate date) {
                return convert(basePTH, location.getAltitude());
            }

            /** {@inheritDoc} */
            @Override
            public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T>
            getWeatherParameters(final FieldGeodeticPoint<T> location, final FieldAbsoluteDate<T> date) {
                return convert(new FieldPressureTemperatureHumidity<>(date.getField(), basePTH),
                               location.getAltitude());
            }
        };
    }

}
