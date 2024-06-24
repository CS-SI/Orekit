/* Copyright 2002-2024 Thales Alenia Space
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

/** Container for pressure and temperature.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldPressureTemperature<T extends CalculusFieldElement<T>> {

    /** Altitude at which weather parameters have been computed. */
    private final T altitude;

    /** Pressure (Pa). */
    private final T pressure;

    /** Temperature (Kelvin). */
    private final T temperature;

    /** Simple constructor.
     * @param altitude altitude at which weather parameters have been computed (m)
     * @param pressure pressure (Pa)
     * @param temperature temperature (Kelvin)
     */
    public FieldPressureTemperature(final T altitude, final T pressure, final T temperature) {
        this.altitude    = altitude;
        this.pressure    = pressure;
        this.temperature = temperature;
    }

    /** Simple constructor.
     * @param field field to which elements belong
     * @param weather regular weather parameters
     */
    public FieldPressureTemperature(final Field<T> field, final PressureTemperatureHumidity weather) {
        this.altitude    = field.getZero().newInstance(weather.getAltitude());
        this.pressure    = field.getZero().newInstance(weather.getPressure());
        this.temperature = field.getZero().newInstance(weather.getTemperature());
    }

    /** Get altitude at which weather parameters have been computed.
     * @return altitude at which weather parameters have been computed (m)
     */
    public T getAltitude() {
        return altitude;
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

}
