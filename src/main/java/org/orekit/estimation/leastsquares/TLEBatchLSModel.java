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
package org.orekit.estimation.leastsquares;

import java.util.List;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEJacobiansMapper;
import org.orekit.propagation.analytical.tle.TLEPartialDerivativesEquations;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * <p>
 * This class is an adaption of the {@link BatchLSModel} class
 * but for the {@link TLEPropagator TLE propagator}.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 *
 */
public class TLEBatchLSModel extends AbstractBatchLSModel {

    /** Simple constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     */
    public TLEBatchLSModel(final OrbitDeterminationPropagatorBuilder[] propagatorBuilders,
                           final List<ObservedMeasurement<?>> measurements,
                           final ParameterDriversList estimatedMeasurementsParameters,
                           final ModelObserver observer) {
        // call super constructor
        super(propagatorBuilders, measurements, estimatedMeasurementsParameters,
              new TLEJacobiansMapper[propagatorBuilders.length], observer);
    }

    /** {@inheritDoc} */
    @Override
    protected TLEJacobiansMapper configureDerivatives(final Propagator propagator) {

        final String equationName = TLEBatchLSModel.class.getName() + "-derivatives";

        final TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations(equationName, (TLEPropagator) propagator);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagator.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        propagator.resetInitialState(stateWithDerivatives);

        return partials.getMapper();

    }

    /** {@inheritDoc} */
    @Override
    protected Orbit configureOrbits(final AbstractJacobiansMapper mapper,
                                    final Propagator propagator) {
        // Directly return the propagator's initial state
        return propagator.getInitialState().getOrbit();
    }

}
