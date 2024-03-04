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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TrackingCoordinates;

/** Defines a tropospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * @author Luc Maisonobe
 * @since 12.1
 */
public interface TroposphericModel extends ParameterDriversProvider {

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param trackingCoordinates tracking coordinates of the satellite
     * @param point station location
     * @param weather weather parameters
     * for constant default values)
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return the path delay due to the troposphere
     */
    TroposphericDelay pathDelay(TrackingCoordinates trackingCoordinates, GeodeticPoint point,
                                PressureTemperatureHumidity weather,
                                double[] parameters, AbsoluteDate date);

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param <T> type of the elements
     * @param trackingCoordinates tracking coordinates of the satellite
     * @param point station location
     * @param weather weather parameters
     * for constant default values)
     * @param parameters tropospheric model parameters at current date
     * @param date current date
     * @return the path delay due to the troposphere
     */
    <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(FieldTrackingCoordinates<T> trackingCoordinates,
                                                                            FieldGeodeticPoint<T> point,
                                                                            FieldPressureTemperatureHumidity<T> weather,
                                                                            T[] parameters, FieldAbsoluteDate<T> date);
}
