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
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/**
 * Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.1
 */
public class AnalyticalBatchLSModel extends AbstractBatchLSModel {

    /** Name of the State Transition Matrix state. */
    private static final String STM_NAME = AnalyticalBatchLSModel.class.getName() + "-derivatives";

    /** Simple constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     */
    public AnalyticalBatchLSModel(final OrbitDeterminationPropagatorBuilder[] propagatorBuilders,
                                  final List<ObservedMeasurement<?>> measurements,
                                  final ParameterDriversList estimatedMeasurementsParameters,
                                  final ModelObserver observer) {
        // call super constructor
        super(propagatorBuilders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** {@inheritDoc} */
    @Override
    protected MatricesHarvester configureHarvester(final Propagator propagator) {
        return propagator.setupMatricesComputation(STM_NAME, null, null);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    protected AbstractJacobiansMapper configureDerivatives(final Propagator propagators) {
        // This deprecated method is replaced by configureHarvester() method
        // Therefore there is no risk to return a null value here
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit configureOrbits(final MatricesHarvester harvester,
                                    final Propagator propagator) {
        return propagator.getInitialState().getOrbit();
    }

}
