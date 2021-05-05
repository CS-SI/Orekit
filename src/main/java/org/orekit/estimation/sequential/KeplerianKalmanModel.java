/* Copyright 2002-2021 CS GROUP
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
package org.orekit.estimation.sequential;

import java.util.List;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianJacobiansMapper;
import org.orekit.propagation.analytical.KeplerianPartialDerivativesEquations;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * <p>
 * This class is an adaption of the {@link KalmanModel} class
 * but for the {@link KeplerianPropagator kep propagator}.
 * </p>
 * @author Nicolas Fialton
 */
public class KeplerianKalmanModel extends AbstractKalmanModel {

    /** Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    public KeplerianKalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                                final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                final ParameterDriversList estimatedMeasurementParameters,
                                final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        // call super constructor
        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters,
              measurementProcessNoiseMatrix, new KeplerianJacobiansMapper[propagatorBuilders.size()]);
    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    protected void updateReferenceTrajectories(final Propagator[] propagators,
                                               final PropagationType pType,
                                               final PropagationType sType) {

        // Update the reference trajectory propagator
        setReferenceTrajectories(propagators);

        // Jacobian mappers
        final AbstractJacobiansMapper[] mappers = getMappers();

        for (int k = 0; k < propagators.length; ++k) {
            // Link the partial derivatives to this new propagator
            final String equationName = KalmanEstimator.class.getName() + "-derivatives-" + k;
            final KeplerianPartialDerivativesEquations pde = new KeplerianPartialDerivativesEquations(equationName, (KeplerianPropagator) getReferenceTrajectories()[k]);

            // Reset the Jacobians
            final SpacecraftState rawState = getReferenceTrajectories()[k].getInitialState();
            final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
            ((KeplerianPropagator) getReferenceTrajectories()[k]).resetInitialState(stateWithDerivatives);
            mappers[k] = pde.getMapper();
        }

        // Update Jacobian mappers
        setMappers(mappers);

    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    protected void analyticalDerivativeComputations(final AbstractJacobiansMapper mapper, final SpacecraftState state) {
        ((KeplerianJacobiansMapper) mapper).analyticalDerivatives(state);
    }

}
