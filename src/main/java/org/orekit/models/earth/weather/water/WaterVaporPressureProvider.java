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

/** Interface for converting between relative humidity and water vapor pressure.
 * @author Luc Maisonobe
 * @since 12.1
 */
public interface WaterVaporPressureProvider {

    /** Compute water vapor pressure.
     * @param p pressure (Pa)
     * @param t temperature (Kelvin)
     * @param rh relative humidity, as a ratio (50% → 0.5)
     * @return water vapor pressure (Pa)
     */
    double waterVaporPressure(double p, double t, double rh);

    /** Compute relative humidity.
     * @param p pressure (Pa)
     * @param t temperature (Kelvin)
     * @param e water vapor pressure (Pa)
     * @return relative humidity, as a ratio (50% → 0.5)
     */
    default double relativeHumidity(final double p, final double t, final double e) {
        final double saturationPressure = waterVaporPressure(p, t, 1.0);
        return e / saturationPressure;
    }

    /** Compute water vapor pressure.
     * @param <T> type of the field elements
     * @param p pressure (Pa)
     * @param t temperature (Kelvin)
     * @param rh relative humidity, as a ratio (50% → 0.5)
     * @return water vapor pressure (Pa)
     */
    <T extends CalculusFieldElement<T>> T waterVaporPressure(T p, T t, T rh);

    /** Compute relative humidity.
     * @param <T> type of the field elements
     * @param p pressure (Pa)
     * @param t temperature (Kelvin)
     * @param e water vapor pressure (Pa)
     * @return relative humidity, as a ratio (50% → 0.5)
     */
    default <T extends CalculusFieldElement<T>> T relativeHumidity(final T p, final T t, final T e) {
        final T saturationPressure = waterVaporPressure(p, t, p.getField().getOne());
        return e.divide(saturationPressure);
    }

}
