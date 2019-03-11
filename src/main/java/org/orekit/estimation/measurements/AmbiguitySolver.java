/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Base class for integer ambiguity solving algorithms.
 * @author Luc Maisonobe
 * @since 10.0
 */
public abstract class AmbiguitySolver {

    /** Drivers for ambiguity drivers. */
    private final List<ParameterDriver> ambiguityDrivers;

    /** Simple constructor.
     * @param ambiguityDrivers drivers for ambiguity parameters
     */
    protected AmbiguitySolver(final List<ParameterDriver> ambiguityDrivers) {
        this.ambiguityDrivers = ambiguityDrivers;
    }

    /** Get all the ambiguity parameters drivers.
     * @return all ambiguity parameters drivers
     */
    public List<ParameterDriver> getAllAmbiguityDrivers() {
        return Collections.unmodifiableList(ambiguityDrivers);
    }

    /** Get the fixed ambiguity parameters drivers.
     * @return fixed ambiguity parameters drivers
     */
    public List<ParameterDriver> getFixedAmbiguityDrivers() {
        return ambiguityDrivers.
                        stream().
                        filter(d -> d.getMaxValue() - d.getMinValue() < 1.0e-15).
                        collect(Collectors.toList());
    }

    /** Get ambiguity indirection array.
     * @param startIndex start index for measurements parameters in global covariance matrix
     * @param measurementsParametersDrivers measurements parameters drivers in global covariance matrix order
     * @return indirection array between full covariance matrix and ambiguity covariance matrix
     */
    protected int[] getAmbiguityIndirection(final int startIndex,
                                            final ParameterDriversList measurementsParametersDrivers) {

        // set up indirection array
        final int n = ambiguityDrivers.size();
        final int[] indirection = new int[n];
        for (int i = 0; i < n; ++i) {
            final String name = ambiguityDrivers.get(i).getName();
            for (int k = startIndex; k < measurementsParametersDrivers.getNbParams(); ++k) {
                if (name.equals(measurementsParametersDrivers.getDrivers().get(k).getName())) {
                    indirection[i] = k;
                    break;
                }
            }
            if (indirection[i] == 0) {
                // the parameter was not found
                final StringBuilder builder = new StringBuilder();
                for (final ParameterDriver driver : measurementsParametersDrivers.getDrivers()) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(driver.getName());
                }
                throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                         name, builder.toString());
            }
        }

        return indirection;

    }

    /** Un-fix an integer ambiguity (typically after a phase cycle slip).
     * @param ambiguityDriver driver for the ambiguity to un-fix
     */
    public void unFixAmbiguity(final ParameterDriver ambiguityDriver) {
        ambiguityDriver.setMinValue(Double.NEGATIVE_INFINITY);
        ambiguityDriver.setMaxValue(Double.POSITIVE_INFINITY);
    }

    /** Fix integer ambiguities.
     * @param startIndex start index for measurements parameters in global covariance matrix
     * @param measurementsParametersDrivers measurements parameters drivers in global covariance matrix order
     * @param covariance global covariance matrix
     * @return list of newly fixed abiguities (ambiguities already fixed before the call are not counted)
     */
    public abstract List<ParameterDriver> fixIntegerAmbiguities(int startIndex,
                                                                ParameterDriversList measurementsParametersDrivers,
                                                                RealMatrix covariance);

}
