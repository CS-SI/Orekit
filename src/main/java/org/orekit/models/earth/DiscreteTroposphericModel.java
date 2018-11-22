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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Defines a tropospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * <p>
 * Models that implement this interface split the delay into hydrostatic
 * and non-hydrostatic part.
 * <pre>
 * δ = δ<sub>h</sub> + δ<sub>nh</sub>
 * <li>δ<sub>h</sub>  =  hydrostatic delay
 * <li>δ<sub>nh</sub> =  non-hydrostatic (or wet) delay
 * </pre>
 * </p>
 * @author Bryan Cazabonne
 */
public interface DiscreteTroposphericModel extends MappingFunction {

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param elevation the elevation of the satellite, in radians
     * @param height the height of the station in m above sea level
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return the path delay due to the troposphere in m
     */
    double pathDelay(double elevation, double height, double[] parameters, AbsoluteDate date);

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite, in radians
     * @param height the height of the station in m above sea level
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return the path delay due to the troposphere in m
     */
    <T extends RealFieldElement<T>> T pathDelay(T elevation, T height, T[] parameters, FieldAbsoluteDate<T> date);

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> -&gt zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> -&gt zenith wet delay
     * </ul>
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
     double[] computeZenithDelay(double height, double[] parameters);

     /** This method allows the  computation of the zenith hydrostatic and
      * zenith wet delay. The resulting element is an array having the following form:
      * <ul>
      * <li>T[0] = D<sub>hz</sub> -&gt zenith hydrostatic delay
      * <li>T[1] = D<sub>wz</sub> -&gt zenith wet delay
      * </ul>
      * @param <T> type of the elements
      * @param height the height of the station in m above sea level.
      * @param parameters tropospheric model parameters.
      * @return a two components array containing the zenith hydrostatic and wet delays.
      */
    <T extends RealFieldElement<T>> T[] computeZenithDelay(T height, T[] parameters);

    /** Get the drivers for tropospheric model parameters.
     * @return drivers for tropospheric model parameters
     */
    ParameterDriver[] getParametersDrivers();

    /** Get tropospheric model parameters.
     * @return tropospheric model parameters
     */
    default double[] getParameters() {
        final ParameterDriver[] drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.length];
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].getValue();
        }
        return parameters;
    }

    /** Get force model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return force model parameters
     */
    default <T extends RealFieldElement<T>> T[] getParameters(final Field<T> field) {
        final ParameterDriver[] drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.length);
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = field.getZero().add(drivers[i].getValue());
        }
        return parameters;
    }

}
