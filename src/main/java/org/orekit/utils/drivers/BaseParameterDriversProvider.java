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
package org.orekit.utils.drivers;

import org.orekit.errors.UnsupportedParameterException;

import java.util.List;

/** Provider for {@link BaseParameterDriver parameters drivers}.
 * @param <P> type of the parameter driver
 * @param <O> type of the parameter observer
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Maxime Journot
 * @since 14.0
 */
public interface BaseParameterDriversProvider<P extends BaseParameterDriver<P, O>,
                                              O extends BaseParameterObserver<P, O>> {

    /** Find if a parameter driver with a given name already exists in a list of parameter drivers.
     * @param <D> type of the parameter drivers
     * @param driversList the list of parameter drivers
     * @param name the parameter driver's name to filter with
     * @return true if the name was found, false otherwise
     */
    static <D extends BaseParameterDriver<?, ?>> boolean findByName(final List<D> driversList, final String name) {
        for (final D d : driversList) {
            if (d.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Get the drivers for parameters.
     * @return drivers for parameters
     */
    List<P> getParametersDrivers();

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     */
    default P getParameterDriver(final String name) {

        for (final P driver : getParametersDrivers()) {
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
     */
    default P getParameterDriverWithSubstring(final String subString) {
        P result = null;
        for (final P driver : getParametersDrivers()) {
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
     */
    default boolean isSupported(final String name) {
        for (final P driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return true;
            }
        }
        // the parameter is not supported
        return false;
    }

}
