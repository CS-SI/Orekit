/* Copyright 2002-2026 CS GROUP
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
package org.orekit.utils;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.UnsupportedParameterException;

/** Provider for {@link ParameterDriver parameters drivers}.
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Maxime Journot
 * @since 11.2
 */
public interface ParameterDriversProvider {

    /** Find if a parameter driver with a given name already exists in a list of parameter drivers.
     * @param driversList the list of parameter drivers
     * @param name the parameter driver's name to filter with
     * @return true if the name was found, false otherwise
     * @since 13.0
     */
    static boolean findByName(final List<ParameterDriver> driversList, final String name) {
        for (final ParameterDriver d : driversList) {
            if (d.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Get the drivers for parameters.
     * @return drivers for parameters
     */
    List<ParameterDriver> getParametersDrivers();

    /** Get model parameters.
     * @return model parameters
     * @since 12.0
     */
    default double[] getParameters() {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return model parameters
     * @since 9.0
     */
    default <T extends CalculusFieldElement<T>> T[] getParameters(final Field<T> field) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().newInstance(drivers.get(i).getValue());
        }
        return parameters;
    }

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @since 8.0
     */
    default ParameterDriver getParameterDriver(final String name) {

        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return driver;
            }
        }
        throw new UnsupportedParameterException(name, getParametersDrivers());
    }

    /** Get parameter that matches the sub-name.
     * @param subString a string containing text unique to a single parameter driver
     * @return parameter value
     * @since 14.0
     */
    default ParameterDriver getParameterDriverWithSubstring(final String subString) {
        ParameterDriver result = null;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.getName().contains(subString) && result == null) {
                // we have found a parameter with that name
                result = driver;
            }
            else if (driver.getName().contains(subString) && result != null) {
                throw new UnsupportedParameterException(subString, getParametersDrivers());
            }
        }
        if (result == null) {
            throw new UnsupportedParameterException(subString, getParametersDrivers());
        }
        return result;
    }


    /** Check if a parameter is supported.
     * <p>Supported parameters are those listed by {@link #getParametersDrivers()}.</p>
     * @param name parameter name to check
     * @return true if the parameter is supported
     * @see #getParametersDrivers()
     * @since 8.0
     */
    default boolean isSupported(final String name) {
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return true;
            }
        }
        // the parameter is not supported
        return false;
    }
}
