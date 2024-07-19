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
package org.orekit.models.earth.weather;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface for providing weather parameters.
 * @author Luc Maisonobe
 * @since 12.1
 */
public interface PressureTemperatureHumidityProvider {

    /** Provide weather parameters.
     * @param location location at which parameters are requested
     * @param date date at which parameters are requested
     * @return weather parameters
     */
    PressureTemperatureHumidity getWeatherParameters(GeodeticPoint location, AbsoluteDate date);

    /** Provide weather parameters.
     * @param <T> type of the field elements
     * @param location location at which parameters are requested
     * @param date date at which parameters are requested
     * @return weather parameters
     */
    <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> getWeatherParameters(FieldGeodeticPoint<T> location,
                                                                                                 FieldAbsoluteDate<T> date);

}
