/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

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
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    double[] mappingFactors(double elevation, double height, double[] parameters, AbsoluteDate date);

    /** This method allows the computation of the hydrostatic and
     * wet mapping functions. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = m<sub>h</sub>(e) → hydrostatic mapping function
     * <li>T[1] = m<sub>w</sub>(e) → wet mapping function
     * </ul>
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @param <T> type of the elements
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    <T extends RealFieldElement<T>> T[] mappingFactors(T elevation, T height, T[] parameters, FieldAbsoluteDate<T> date);

    /** Get the drivers for tropospheric model parameters.
     * @return drivers for tropospheric model parameters
     */
    List<ParameterDriver> getParametersDrivers();

    /** Get tropospheric model parameters.
     * @return tropospheric model parameters
     */
    default double[] getParameters() {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get tropospheric model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return tropospheric model parameters
     */
    default <T extends RealFieldElement<T>> T[] getParameters(final Field<T> field) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue());
        }
        return parameters;
    }
}
