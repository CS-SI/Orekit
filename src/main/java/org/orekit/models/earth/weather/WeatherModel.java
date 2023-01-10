/* Copyright 2002-2023 CS GROUP
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

import org.orekit.time.AbsoluteDate;

/** Defines a surface meteorology model that can be used to
 * compute the different weather parameters (pressure, temperature, ...).
 * @author Bryan Cazabonne
 * @since 9.3
 */
public interface WeatherModel {

    /** Calculates the weather parameters of the model.
     * In order to obtain the correct values of the parameters
     * this method has to be call just after the construction of the model.
     * @param stationHeight the height of the station in m
     * @param currentDate current date
     */
    void weatherParameters(double stationHeight, AbsoluteDate currentDate);

}
