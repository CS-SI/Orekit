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
package org.orekit.errors;

import java.util.List;

import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;

/** Exception for unsupported {@link ParameterDriver} in a model implementing {@link ParameterDriversProvider}.
 *
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 12.0
 */
public class UnsupportedParameterException extends OrekitException {

    /** String for empty parameter drivers' list. */
    public static final String NO_PARAMETER = "<none>";

    /** Comma separator for printing list of supported parameter drivers. */
    public static final String COMMA_SEP = ", ";

    /** Serializable UID. */
    private static final long serialVersionUID =  -1363569710782876135L;

    /** Constructor.
     *
     * @param parameterName name of the parameter driver that is not supported by the model
     * @param parameterDrivers list of the model's parameter drivers
     */
    public UnsupportedParameterException(final String parameterName, final List<ParameterDriver> parameterDrivers) {
        super(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, parameterName, getSupportedNames(parameterDrivers));
    }

    /** Builder for the supported parameters' names.
     *
     * @param parameterDrivers list of model parameter drivers
     * @return supported parameter names as a String
     */
    private static String getSupportedNames(final List<ParameterDriver> parameterDrivers) {
        final StringBuilder builder = new StringBuilder();
        for (final ParameterDriver driver : parameterDrivers) {
            if (builder.length() > 0) {
                builder.append(COMMA_SEP);
            }
            builder.append(driver.getName());
        }
        if (builder.length() == 0) {
            builder.append(NO_PARAMETER);
        }
        return builder.toString();
    }
}
