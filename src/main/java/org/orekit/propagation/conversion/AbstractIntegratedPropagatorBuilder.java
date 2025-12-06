/* Copyright 2022-2025 Romain Serra
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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.orbits.OrbitalParameterFactory;
import org.orekit.orbits.OrbitalParameters;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;

/**
 * Abstract class for builders for integrator-based propagators.
 * @param <T> field type
 * @param <O> type of the orbital parameters
 * @param <F> type of the orbital parameters factory
 * @since 13.0
 * @author Romain Serra
 */
public abstract class AbstractIntegratedPropagatorBuilder<T extends AbstractIntegratedPropagator,
                                                          O extends OrbitalParameters,
                                                          F extends OrbitalParameterFactory<O>>
    extends AbstractPropagatorBuilder<T, O, F> {

    /** First order integrator builder for propagation. */
    private final ODEIntegratorBuilder builder;

    /** Type of the orbit used for the propagation.*/
    private final PropagationType propagationType;

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param builder integrator builder
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param attitudeProvider attitude law.
     * @param mass initial mass
     */
    protected AbstractIntegratedPropagatorBuilder(final F factory,
                                                  final ODEIntegratorBuilder builder,
                                                  final PropagationType propagationType,
                                                  final AttitudeProvider attitudeProvider, final double mass) {
        super(factory, true, attitudeProvider, mass);
        this.builder = builder;
        this.propagationType = propagationType;
    }

    /**
     * Getter for integrator builder.
     * @return builder
     */
    public ODEIntegratorBuilder getIntegratorBuilder() {
        return builder;
    }

    /**
     * Getter for the propagation type.
     * @return propagation type
     */
    public PropagationType getPropagationType() {
        return propagationType;
    }

    /** {@inheritDoc} */
    @Override
    public abstract T buildPropagator(double[] normalizedParameters);

    /** {@inheritDoc} */
    @Override
    public T buildPropagator() {
        return buildPropagator(getSelectedNormalizedParameters());
    }

}
