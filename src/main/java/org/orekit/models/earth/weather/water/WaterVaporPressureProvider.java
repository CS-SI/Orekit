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
package org.orekit.models.earth.weather.water;

import org.hipparchus.CalculusFieldElement;

/** Interface for converting relative humidity into water vapor pressure.
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

    /** Compute water vapor pressure.
     * @param p pressure (Pa)
     * @param t temperature (Kelvin)
     * @param rh relative humidity, as a ratio (50% → 0.5)
     * @param <T> type of the field elements
     * @return water vapor pressure (Pa)
     */
    <T extends CalculusFieldElement<T>> T waterVaporPressure(T p, T t, T rh);

}

