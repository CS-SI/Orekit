/* Copyright 2022-2025 Thales Alenia Space
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
import org.hipparchus.Field;

/** Container for pressure, temperature, and humidity.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldPressureTemperatureHumidity<T extends CalculusFieldElement<T>> extends FieldPressureTemperature<T> {

    /** Humidity as water vapor pressure (Pa). */
    private final T waterVaporPressure;

    /** Mean temperature weighted with water vapor pressure. */
    private final T tm;

    /** Water vapor decrease factor. */
    private final T lambda;

    /** Simple constructor.
     * @param altitude altitude at which weather parameters have been computed (m)
     * @param pressure pressure (Pa)
     * @param temperature temperature (Kelvin)
     * @param waterVaporPressure humidity as water vapor pressure (Pa)
     * @param tm mean temperature weighted with water vapor pressure
     * @param lambda water vapor decrease factor
     */
    public FieldPressureTemperatureHumidity(final T altitude,
                                            final T pressure,
                                            final T temperature,
                                            final T waterVaporPressure,
                                            final T tm,
                                            final T lambda) {
        super(altitude, pressure, temperature);
        this.waterVaporPressure = waterVaporPressure;
        this.tm                 = tm;
        this.lambda             = lambda;
    }

    /** Simple constructor.
     * @param field field to which elements belong
     * @param weather regular weather parameters
     */
    public FieldPressureTemperatureHumidity(final Field<T> field, final PressureTemperatureHumidity weather) {
        super(field, weather);
        this.waterVaporPressure = field.getZero().newInstance(weather.getWaterVaporPressure());
        this.tm                 = field.getZero().newInstance(weather.getTm());
        this.lambda             = field.getZero().newInstance(weather.getLambda());
    }

    /** Get humidity as water vapor pressure.
     * @return humidity as water vapor pressure (Pa)
     */
    public T getWaterVaporPressure() {
        return waterVaporPressure;
    }

    /** Get mean temperature weighted with water vapor pressure.
     * @return mean temperature weighted with water vapor pressure
     */
    public T getTm() {
        return tm;
    }

    /** Get water vapor decrease factor.
     * @return water vapor decrease factor
     */
    public T getLambda() {
        return lambda;
    }

}
