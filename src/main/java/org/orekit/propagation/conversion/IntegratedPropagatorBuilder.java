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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.estimation.leastsquares.BatchLSODModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.KalmanODModel;
import org.orekit.orbits.Orbit;
import org.orekit.utils.ParameterDriversList;

/** Base class for orbit determination model builders.
 * @author Bryan Cazabonne
 * @since 10.0
 */
public interface IntegratedPropagatorBuilder extends PropagatorBuilder {

    /** Build a new {@link BatchLSODModel}.
     * @param builders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @return a new model for the Batch Least Squares orbit determination
     */
    BatchLSODModel buildLSModel(IntegratedPropagatorBuilder[] builders,
                       List<ObservedMeasurement<?>> measurements,
                       ParameterDriversList estimatedMeasurementsParameters,
                       ModelObserver observer);

    /** Build a new {@link KalmanODModel}.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementsParameters measurement parameters to estimate
     * @return a new model for Kalman Filter orbit determination
     */
    KalmanODModel buildKalmanModel(List<IntegratedPropagatorBuilder> propagatorBuilders,
                                   List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                   ParameterDriversList estimatedMeasurementsParameters);

    /** Reset the orbit in the propagator builder.
     * @param newOrbit New orbit to set in the propagator builder
     */
    void resetOrbit(Orbit newOrbit);

}
