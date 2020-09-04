/* Copyright 2002-2020 CS GROUP
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

import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEJacobiansMapper;
import org.orekit.propagation.analytical.tle.TLEPartialDerivativesEquations;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.ODPropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * <p>
 * This class is an adaption of the {@link KalmanModel} class
 * but for the {@link TLEPropagator TLE propagator}.
 * </p>
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */

public class TLEKalmanModel extends AbstractKalmanModel {

    /** Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     */
    public TLEKalmanModel (final List<ODPropagatorBuilder> propagatorBuilders,
                             final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                             final ParameterDriversList estimatedMeasurementParameters) {

        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractPropagator[] getEstimatedPropagators() {

        // Return propagators built with current instantiation of the propagator builders
        final TLEPropagator[] propagators = new TLEPropagator[getBuilders().size()];
        for (int k = 0; k < getBuilders().size(); ++k) {
            propagators[k] = (TLEPropagator) getBuilders().get(k).buildPropagator(getBuilders().get(k).getSelectedNormalizedParameters());
        }
        return propagators;
    }

    /** {@inheritDoc} */
    @Override
    protected void updateReferenceTrajectories(final AbstractPropagator[] propagators,
                                               final PropagationType pType,
                                               final PropagationType sType) {

        // Update the reference trajectory propagator
        setReferenceTrajectories(propagators);

        for (int k = 0; k < propagators.length; ++k) {
            // Link the partial derivatives to this new propagator
            final String equationName = KalmanEstimator.class.getName() + "-derivatives-" + k;
            final TLEPartialDerivativesEquations pde = new TLEPartialDerivativesEquations(equationName, (TLEPropagator) getReferenceTrajectories()[k]);

            // Reset the Jacobians
            final SpacecraftState rawState = getReferenceTrajectories()[k].getInitialState();
            final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
            ((TLEPropagator) getReferenceTrajectories()[k]).resetInitialState(stateWithDerivatives);
            getMappers()[k] = pde.getMapper();
        }

    }

    /** {@inheritDoc} */
    @Override
    protected AbstractJacobiansMapper[] buildMappers() {
        return new TLEJacobiansMapper[getBuilders().size()];
    }

    /** {@inheritDoc} */
    @Override
    protected void analyticalDerivativeComputations(final AbstractJacobiansMapper mapper, final SpacecraftState state) {
        ((TLEJacobiansMapper) mapper).computeDerivatives(state);
    }

}
