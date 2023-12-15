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

/** Container for pressure, temperature, and humidity.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class PressureTemperatureHumidity extends PressureTemperature {

    /** Humidity as water vapor pressure (Pa). */
    private final double waterVaporPressure;

    /** Simple constructor.
     * @param altitude altitude at which weather parameters have been computed (m)
     * @param pressure pressure (Pa)
     * @param temperature temperature (Kelvin)
     * @param waterVaporPressure humidity as water vapor pressure (Pa)
     */
    public PressureTemperatureHumidity(final double altitude,
                                       final double pressure,
                                       final double temperature,
                                       final double waterVaporPressure) {
        super(altitude, pressure, temperature);
        this.waterVaporPressure = waterVaporPressure;
    }

    /** Get humidity as water vapor pressure.
     * @return humidity as water vapor pressure (Pa)
     */
    public double getWaterVaporPressure() {
        return waterVaporPressure;
    }

}