/* Copyright 2002-2022 CS GROUP
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

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParametersDriversProvider;

/** Defines a tropospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * <p>
 * Models that implement this interface split the delay into hydrostatic
 * and non-hydrostatic part:
 * <p>
 * δ = δ<sub>h</sub> + δ<sub>nh</sub>
 * <p>
 * With:
 * <ul>
 * <li> δ<sub>h</sub>  =  hydrostatic delay </li>
 * <li> δ<sub>nh</sub> =  non-hydrostatic (or wet) delay </li>
 * </ul>
 * @author Bryan Cazabonne
 */
public interface DiscreteTroposphericModel extends ParametersDriversProvider {

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param elevation the elevation of the satellite, in radians
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return the path delay due to the troposphere in m
     */
    double pathDelay(double elevation, GeodeticPoint point, double[] parameters, AbsoluteDate date);

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite, in radians
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return the path delay due to the troposphere in m
     */
    <T extends CalculusFieldElement<T>> T pathDelay(T elevation, FieldGeodeticPoint<T> point, T[] parameters, FieldAbsoluteDate<T> date);

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
    default <T extends CalculusFieldElement<T>> T[] getParameters(final Field<T> field) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue());
        }
        return parameters;
    }

}
