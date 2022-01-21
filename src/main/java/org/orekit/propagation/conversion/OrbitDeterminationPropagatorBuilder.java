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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.AbstractKalmanModel;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.orbits.Orbit;
import org.orekit.utils.ParameterDriversList;

/** Base class for orbit determination model builders.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public interface OrbitDeterminationPropagatorBuilder extends PropagatorBuilder {

    /** Build a new batch least squares model.
     * @param builders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @return a new model for the Batch Least Squares orbit determination
     */
    AbstractBatchLSModel buildLSModel(OrbitDeterminationPropagatorBuilder[] builders,
                                      List<ObservedMeasurement<?>> measurements,
                                      ParameterDriversList estimatedMeasurementsParameters,
                                      ModelObserver observer);

    /** Build a new Kalman model.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementsParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @return a new model for Kalman Filter orbit determination
     */
    AbstractKalmanModel buildKalmanModel(List<OrbitDeterminationPropagatorBuilder> propagatorBuilders,
                                         List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                         ParameterDriversList estimatedMeasurementsParameters,
                                         CovarianceMatrixProvider measurementProcessNoiseMatrix);

    /** Reset the orbit in the propagator builder.
     * @param newOrbit New orbit to set in the propagator builder
     */
    void resetOrbit(Orbit newOrbit);

}
