/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.models.earth;

import java.io.Serializable;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;

/** Defines a ionospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 *
 * @author Joris Olympio
 * @since 7.1
 */
public interface IonosphericModel extends Serializable {

    /** Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param date        current date
     * @param geo         the Geodetic point of receiver/station
     * @param elevation   the elevation of the satellite
     * @param azimuth     the azimuth of the satellite
     *
     * @return the path delay due to the ionosphere in m
     */
    double pathDelay(AbsoluteDate date, GeodeticPoint geo, double elevation, double azimuth);

}
