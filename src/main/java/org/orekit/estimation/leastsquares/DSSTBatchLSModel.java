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
package org.orekit.estimation.leastsquares;

import java.util.List;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTHarvester;
import org.orekit.propagation.semianalytical.dsst.DSSTJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.utils.ParameterDriversList;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * <p>
 * This class is an adaption of the {@link BatchLSModel} class
 * but for the {@link DSSTPropagator DSST propagator}.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class DSSTBatchLSModel extends AbstractBatchLSModel {

    /** Name of the State Transition Matrix state. */
    private static final String STM_NAME = DSSTBatchLSModel.class.getName() + "-derivatives";

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType;

    /** Type of the elements used to define the orbital state.*/
    private PropagationType stateType;

    /** Simple constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     */
    public DSSTBatchLSModel(final OrbitDeterminationPropagatorBuilder[] propagatorBuilders,
                            final List<ObservedMeasurement<?>> measurements,
                            final ParameterDriversList estimatedMeasurementsParameters,
                            final ModelObserver observer,
                            final PropagationType propagationType,
                            final PropagationType stateType) {
        // call super constructor
        super(propagatorBuilders, measurements, estimatedMeasurementsParameters, observer);
        this.propagationType = propagationType;
        this.stateType       = stateType;
    }

    /** {@inheritDoc} */
    @Override
    protected MatricesHarvester configureHarvester(final Propagator propagator) {
        return propagator.setupMatricesComputation(STM_NAME, null, null);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    protected DSSTJacobiansMapper configureDerivatives(final Propagator propagator) {

        final org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations partials =
                        new org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations(STM_NAME, (DSSTPropagator) propagator, propagationType);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagator.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        ((DSSTPropagator) propagator).setInitialState(stateWithDerivatives, stateType);

        return partials.getMapper();

    }

    /** {@inheritDoc} */
    @Override
    protected Orbit configureOrbits(final MatricesHarvester harvester, final Propagator propagator) {
        // Cast
        final DSSTPropagator dsstPropagator = (DSSTPropagator) propagator;
        final DSSTHarvester  dsstHarvester  = (DSSTHarvester) harvester;
        // Mean orbit
        final SpacecraftState initial = dsstPropagator.initialIsOsculating() ?
                       DSSTPropagator.computeMeanState(dsstPropagator.getInitialState(), dsstPropagator.getAttitudeProvider(), dsstPropagator.getAllForceModels()) :
                       dsstPropagator.getInitialState();
        dsstHarvester.initializeFieldShortPeriodTerms(initial);
        // Compute short period derivatives at the beginning of the iteration
        if (propagationType == PropagationType.OSCULATING) {
            dsstHarvester.updateFieldShortPeriodTerms(initial);
            dsstHarvester.setReferenceState(initial);
        }
        // Compute short period derivatives at the beginning of the iteration
        harvester.setReferenceState(initial);
        return initial.getOrbit();
    }

}
