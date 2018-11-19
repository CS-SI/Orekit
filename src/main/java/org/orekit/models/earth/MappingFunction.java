/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth;

import java.io.Serializable;

import org.orekit.time.AbsoluteDate;

/** Interface for mapping functions used in the tropospheric delay computation.
 * @author Bryan Cazabonne
 */
public interface MappingFunction extends Serializable {

    /** This method allows the computation of the hydrostatic and
     * wet mapping functions. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = m<sub>h</sub>(e) -&gt hydrostatic mapping function
     * <li>double[1] = m<sub>w</sub>(e) -&gt wet mapping function
     * </ul>
     * @param height the height of the station in m above sea level.
     * @param elevation the elevation of the satellite, in radians.
     * @param date current date
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    double[] mappingFactors(double height, double elevation, AbsoluteDate date);
}
