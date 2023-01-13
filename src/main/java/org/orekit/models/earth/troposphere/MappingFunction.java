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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface for mapping functions used in the tropospheric delay computation.
 * @author Bryan Cazabonne
 */
public interface MappingFunction {

    /** This method allows the computation of the hydrostatic and
     * wet mapping functions. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = m<sub>h</sub>(e) → hydrostatic mapping function
     * <li>double[1] = m<sub>w</sub>(e) → wet mapping function
     * </ul>
     * @param elevation the elevation of the satellite, in radians
     * @param point station location
     * @param date current date
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    double[] mappingFactors(double elevation, GeodeticPoint point, AbsoluteDate date);

    /** This method allows the computation of the hydrostatic and
     * wet mapping functions. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = m<sub>h</sub>(e) → hydrostatic mapping function
     * <li>T[1] = m<sub>w</sub>(e) → wet mapping function
     * </ul>
     * @param elevation the elevation of the satellite, in radians
     * @param point station location
     * @param date current date
     * @param <T> type of the elements
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    <T extends CalculusFieldElement<T>> T[] mappingFactors(T elevation, FieldGeodeticPoint<T> point, FieldAbsoluteDate<T> date);

}
