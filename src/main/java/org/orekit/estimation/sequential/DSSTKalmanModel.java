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

import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.utils.ParameterDriversList;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * <p>
 * This class is an adaption of the {@link KalmanModel} class
 * but for the {@link DSSTPropagator DSST propagator}.
 * </p>
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class DSSTKalmanModel extends AbstractKalmanModel {

    /** Kalman process model constructor.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     */
    public DSSTKalmanModel(final List<OrbitDeterminationPropagatorBuilder> propagatorBuilders,
                           final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                           final ParameterDriversList estimatedMeasurementParameters,
                           final CovarianceMatrixProvider measurementProcessNoiseMatrix,
                           final PropagationType propagationType,
                           final PropagationType stateType) {
        // call super constructor
        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters,
              measurementProcessNoiseMatrix, new DSSTJacobiansMapper[propagatorBuilders.size()],
              propagationType, stateType);
    }

    /** {@inheritDoc} */
    @Override
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
            final DSSTPartialDerivativesEquations pde = new DSSTPartialDerivativesEquations(equationName, (DSSTPropagator) getReferenceTrajectories()[k], pType);

            // Reset the Jacobians
            final SpacecraftState rawState = getReferenceTrajectories()[k].getInitialState();
            final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
            ((DSSTPropagator) getReferenceTrajectories()[k]).setInitialState(stateWithDerivatives, sType);
            mappers[k] = pde.getMapper();
        }

        // Update Jacobian mappers
        setMappers(mappers);

    }

    /** {@inheritDoc} */
    @Override
    protected void analyticalDerivativeComputations(final AbstractJacobiansMapper mapper, final SpacecraftState state) {
        ((DSSTJacobiansMapper) mapper).setShortPeriodJacobians(state);
    }

}
