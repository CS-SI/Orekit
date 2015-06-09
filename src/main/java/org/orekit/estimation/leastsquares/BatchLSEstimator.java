/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.MeasurementModifier;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.PropagatorBuilder;


/** Least squares estimator for orbit determination.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class BatchLSEstimator {

    /** Measurements. */
    private final List<Measurement> measurements;

    /** Build for propagator. */
    private final PropagatorBuilder builder;

    /** Parameters. */
    private final Map<String, Parameter> parameters;

    /** Simple constructor.
     * @param builder builder to user for propagation
     */
    public BatchLSEstimator(final PropagatorBuilder builder) {
        this.builder      = builder;
        this.measurements = new ArrayList<Measurement>();
        this.parameters   = new HashMap<String, Parameter>();
    }

    /** Get all the parameters.
     * @return all the supported parameters
     */
    public Collection<Parameter> getParameters() {
        return parameters.values();
    }

    /** Add a measurement.
     * @param measurement measurement to add
     * @exception OrekitException if the measurement has a parameter
     * that is already used
     */
    public void addMeasurement(final Measurement measurement)
      throws OrekitException {

        // add the measurement
        measurements.add(measurement);

        // add parameters
        for (final MeasurementModifier modifier : measurement.getModifiers()) {
            for (final Parameter parameter : modifier.getParameters()) {
                final Parameter existing = parameters.get(parameter.getName());
                if (existing == null) {
                    parameters.put(parameter.getName(), parameter);
                } else if (existing != parameter) {
                    // we have two different parameters sharing the same name
                    throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                              parameter.getName());
                }
            }
        }

    }

    /** Estimate the orbit and the parameters.
     * <p>
     * The estimated parameters are available using {@link #getParameters()}
     * </p>
     * @return estimated orbit
     */
    public Orbit estimate() {
        // TODO
        throw OrekitException.createInternalError(null);
    }

}
