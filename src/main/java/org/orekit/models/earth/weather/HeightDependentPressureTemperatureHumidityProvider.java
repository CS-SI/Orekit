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
package org.orekit.models.earth.weather;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Provider for weather parameters that change with height.
 * <p>
 * Height variations correspond to equations 5.98, 5.99 and 5.100 from
 * Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class HeightDependentPressureTemperatureHumidityProvider implements PressureTemperatureHumidityProvider {

    /** Minimum altitude (m). */
    private final double hMin;

    /** Maximum altitude (m). */
    private final double hMax;

    /** Reference altitude (m). */
    private final double h0;

    /** PTH provider at reference height. */
    private final PressureTemperatureHumidityProvider pth0Provider;

    /** Water pressure provider for water vapor pressure. */
    private final WaterVaporPressureProvider provider;

    /** Simple constructor.
     * <p>
     * Points outside of altitude range will be silently clipped back to range.
     * </p>
     * @param hMin minimum altitude
     * @param hMax maximum altitude
     * @param h0 reference altitude
     * @param pth0Provider PTH provider at reference height
     * @param provider provider for water vapor pressure
     */
    public HeightDependentPressureTemperatureHumidityProvider(final double hMin, final double hMax,
                                                              final double h0,
                                                              final PressureTemperatureHumidityProvider pth0Provider,
                                                              final WaterVaporPressureProvider provider) {
        this.hMin         = hMin;
        this.hMax         = hMax;
        this.h0           = h0;
        this.pth0Provider = pth0Provider;
        this.provider     = provider;
    }

    /** Get minimum altitude.
     * @return minimum altitude
     */
    public double getHMin() {
        return hMin;
    }

    /** Get maximum altitude.
     * @return maximum altitude
     */
    public double getHMax() {
        return hMax;
    }

    /** Get reference altitude.
     * @return reference altitude
     */
    public double getH0() {
        return h0;
    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParamerers(final GeodeticPoint location,
                                                            final AbsoluteDate date) {

        // retrieve parameters at reference altitude
        final PressureTemperatureHumidity pth0 = pth0Provider.getWeatherParamerers(new GeodeticPoint(location.getLatitude(),
                                                                                                     location.getLongitude(),
                                                                                                     h0),
                                                                                   date);
        final double rh0 = provider.relativeHumidity(pth0.getPressure(), pth0.getTemperature(), pth0.getWaterVaporPressure());

        // compute changes due to altitude change
        final double dh = FastMath.min(FastMath.max(location.getAltitude(), hMin), hMax) - h0;
        final double p  = pth0.getPressure() * FastMath.pow(1.0 - 2.26e-5 * dh, 5.225);
        final double t  = pth0.getTemperature() - 6.5e-3 * dh;
        final double rh = rh0 * FastMath.exp(-6.396e-4 * dh);

        return new PressureTemperatureHumidity(p, t, provider.waterVaporPressure(p, t, rh));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> getWeatherParamerers(final FieldGeodeticPoint<T> location,
                                                                                                        final FieldAbsoluteDate<T> date) {
        // retrieve parameters at reference altitude
        final FieldPressureTemperatureHumidity<T> pth0 =
                        pth0Provider.getWeatherParamerers(new FieldGeodeticPoint<>(location.getLatitude(),
                                                                                   location.getLongitude(),
                                                                                   location.getAltitude().newInstance(h0)),
                                                          date);
        final T rh0 = provider.relativeHumidity(pth0.getPressure(), pth0.getTemperature(), pth0.getWaterVaporPressure());

        // compute changes due to altitude change
        final T dh = FastMath.min(FastMath.max(location.getAltitude(), hMin), hMax).subtract(h0);
        final T t  = pth0.getTemperature().subtract(dh.multiply(6.5e-3));
        final T p  = pth0.getPressure().multiply(dh.multiply(2.26e-5).negate().add(1.0).pow(5.225));
        final T rh = rh0.multiply(FastMath.exp(dh.multiply(-6.396e-4)));
        return new FieldPressureTemperatureHumidity<>(p, t, provider.waterVaporPressure(p, t, rh));
    }

}
