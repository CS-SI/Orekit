/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.leastsquares.ODModel;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriversList;

/** Base class for model builders.  */
public interface IntegratedPropagatorBuilder extends PropagatorBuilder {

    /** Build a new {@link ODModel}.
     * @param builders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @return a new model for orbit determination
     * @throws OrekitException if some propagator parameter cannot be set properly
     */
    ODModel buildModel(IntegratedPropagatorBuilder[] builders,
                       List<ObservedMeasurement<?>> measurements,
                       ParameterDriversList estimatedMeasurementsParameters,
                       ModelObserver observer)
        throws OrekitException;
}
