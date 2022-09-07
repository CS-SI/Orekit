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
package org.orekit.forces;


import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

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

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #acceleration(SpacecraftState, double[]) acceleration} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @return the parameters given the date
     */
    public double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final double[] outParameters = new double[getNbParametersDriversValue()];
        int index = 0;
        int paramIndex = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final ParameterDriver driver = allParameters.get(i);
            final String driverNameforDate = driver.getNameSpan(date);
            // Loop on the spans
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                // Add all the parameter drivers of the span
                if (span.getData().equals(driverNameforDate)) {
                    outParameters[index++] = parameters[paramIndex];
                }
                paramIndex++;
            }
        }
        return outParameters;
    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #acceleration(FieldSpacecraftState, CalculusFieldElement[]) acceleration} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @param <T> extends CalculusFieldElement
     * @return the parameters given the date
     */
    public <T extends CalculusFieldElement<T>> T[] extractParameters(final T[] parameters,
                                                                 final FieldAbsoluteDate<T> date) {

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), allParameters.size());
        int index = 0;
        int paramIndex = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final ParameterDriver driver = allParameters.get(i);
            final String driverNameforDate = driver.getNameSpan(date.toAbsoluteDate());
            // Loop on the spans
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                // Add all the parameter drivers of the span
                if (span.getData().equals(driverNameforDate)) {
                    outParameters[index++] = parameters[paramIndex];
                }
                ++paramIndex;
            }
        }
        return outParameters;
    }
}
