/* Copyright 2023 Thales Alenia Space
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
package org.orekit.models.earth.weather;

import org.hipparchus.CalculusFieldElement;

/** Container for pressure, temperature, and humidity.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldPressureTemperatureHumidity<T extends CalculusFieldElement<T>> {

    /** Pressure (Pa). */
    private final T pressure;

    /** Temperature (Kelvin). */
    private final T temperature;

    /** Humidity as relative humididy (50% → 0.5). */
    private final T relativeHumidity;

    /** Humidity as water vapor pressure (Pa). */
    private final T waterVaporPressure;

    /** Simple constructor.
     * @param pressure pressure (Pa)
     * @param temperature temperature (Kelvin)
     * @param relativeHumidity humidity as relative humididy (50% → 0.5)
     * @param waterVaporPressure humidity as water vapor pressure (Pa)
     */
    public FieldPressureTemperatureHumidity(final T pressure,
                                            final T temperature,
                                            final T relativeHumidity,
                                            final T waterVaporPressure) {
        this.pressure           = pressure;
        this.temperature        = temperature;
        this.relativeHumidity   = relativeHumidity;
        this.waterVaporPressure = waterVaporPressure;
    }

    /** Get pressure.
     * @return pressure (Pa)
     */
    public T getPressure() {
        return pressure;
    }

    /** Get temperature.
     * @return temperature (Kelvin)
     */
    public T getTemperature() {
        return temperature;
    }

    /** Get humidity as relative humididy (50% → 0.5).
     * @return humidity as relative humididy (50% → 0.5)
     */
    public T getRelativeHumidity() {
        return relativeHumidity;
    }

    /** Get humidity as water vapor pressure.
     * @return humidity as water vapor pressure (Pa)
     */
    public T getWaterVaporPressure() {
        return waterVaporPressure;
    }

}
