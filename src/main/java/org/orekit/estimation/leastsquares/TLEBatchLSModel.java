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
package org.orekit.estimation.leastsquares;

import java.util.List;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEJacobiansMapper;
import org.orekit.propagation.analytical.tle.TLEPartialDerivativesEquations;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.ODPropagatorBuilder;
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
    public TLEBatchLSModel(final ODPropagatorBuilder[] propagatorBuilders,
                           final List<ObservedMeasurement<?>> measurements,
                           final ParameterDriversList estimatedMeasurementsParameters,
                           final ModelObserver observer) {
        // call super constructor
        super(propagatorBuilders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** Configure the propagator to compute derivatives.
     *
     *<p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param propagators {@link Propagator} to configure
     * @return mapper for this propagator
     */
    @DefaultDataContext
    protected TLEJacobiansMapper configureDerivatives(final AbstractPropagator propagators) {

        final String equationName = TLEBatchLSModel.class.getName() + "-derivatives";

        final TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations(equationName, (TLEPropagator) propagators);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagators.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        ((TLEPropagator) propagators).setInitialState(stateWithDerivatives);

        return partials.getMapper();

    }

    /** {@inheritDoc} */
    @Override
    protected AbstractJacobiansMapper[] buildMappers() {
        return new TLEJacobiansMapper[getBuilders().length];
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractPropagator[] buildPropagators() {
        return new TLEPropagator[getBuilders().length];
    }

    /** {@inheritDoc} */
    protected void computeDerivatives(final AbstractJacobiansMapper mapper,
                                      final SpacecraftState state) {
        ((TLEJacobiansMapper) mapper).computeDerivatives(state);
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit computeInitialDerivatives(final AbstractJacobiansMapper mapper,
                                              final AbstractPropagator propagator) {
        return propagator.getInitialState().getOrbit();
    }

}
