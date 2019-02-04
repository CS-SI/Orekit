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

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Defines a tropospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * <p>
 * Models that implement this interface don't split the delay into hydrostatic
 * and non-hydrostatic part.
 * </p>
 * @author Thomas Neidhart
 * @since 7.1
 */
public interface TroposphericModel extends DiscreteTroposphericModel {

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param elevation the elevation of the satellite, in radians
     * @param height the height of the station in m above sea level
     * @return the path delay due to the troposphere in m
     */
    double pathDelay(double elevation, double height);

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * It is discourage to use this method. It has been developed to respect the
     * current architecture of the tropospheric models.
     * </p>
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite, in radians
     * @param height the height of the station in m above sea level
     * @return the path delay due to the troposphere in m
     */
    default <T extends RealFieldElement<T>> T pathDelay(T elevation, T height) {
        final T zero = height.getField().getZero();
        return zero.add(pathDelay(elevation.getReal(), height.getReal()));
    }

    /** Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     *
     * @param elevation the elevation of the satellite, in radians
     * @param height the height of the station in m above sea level
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return the path delay due to the troposphere in m
     */
    default double pathDelay(double elevation, double height, double[] parameters, AbsoluteDate date) {
        return pathDelay(elevation, height);
    }

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
    default <T extends RealFieldElement<T>> T pathDelay(T elevation, T height, T[] parameters, FieldAbsoluteDate<T> date) {
        return pathDelay(elevation, height);
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    default double[] computeZenithDelay(double height, double[] parameters, AbsoluteDate date) {
        return new double[] {
            pathDelay(0.5 * FastMath.PI, height),
            0
        };
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param <T> type of the elements
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    default <T extends RealFieldElement<T>> T[] computeZenithDelay(T height, T[] parameters, FieldAbsoluteDate<T> date) {
        final Field<T> field = height.getField();
        final T zero = field.getZero();
        final T[] delay = MathArrays.buildArray(field, 2);
        delay[0] = pathDelay(zero.add(0.5 * FastMath.PI), height);
        delay[1] = zero;
        return delay;
    }

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
    default double[] mappingFactors(double elevation, double height, double[] parameters, AbsoluteDate date) {
        return new double[] {
            1.0,
            1.0
        };
    }

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
     * @param <T> type of the elements
     * @return a two components array containing the hydrostatic and wet mapping functions.
     */
    default <T extends RealFieldElement<T>> T[] mappingFactors(T elevation, T height,
                                                               T[] parameters, FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T one = field.getOne();
        final T[] factors = MathArrays.buildArray(field, 2);
        factors[0] = one;
        factors[1] = one;
        return factors;
    }

    /** Get the drivers for tropospheric model parameters.
     * @return drivers for tropospheric model parameters
     */
    default List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
