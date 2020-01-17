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
package org.orekit.forces;


import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.ParameterDriver;

/** Base class for force models.
 * @author Luc Maisonobe
 * @since 8.0
 */
public abstract class AbstractForceModel implements ForceModel {

    /** {@inheritDoc} */
    public ParameterDriver getParameterDriver(final String name) {

        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return driver;
            }
        }

        throw notSupportedException(name);

    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final String name) {
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return true;
            }
        }
        // the parameter is not supported
        return false;
    }

    /** Complain if a parameter is not supported.
     * @param name name of the parameter
     */
    protected void complainIfNotSupported(final String name) {
        if (!isSupported(name)) {
            throw notSupportedException(name);
        }
    }

    /** Generate an exception for unsupported parameter.
     * @param name unsupported parameter name
     * @return exception with appropriate message
     */
    private OrekitException notSupportedException(final String name) {

        final StringBuilder builder = new StringBuilder();
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(driver.getName());
        }
        if (builder.length() == 0) {
            builder.append("<none>");
        }

        return new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                   name, builder.toString());

    }

}
