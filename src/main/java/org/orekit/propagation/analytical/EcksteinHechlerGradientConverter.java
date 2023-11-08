/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical;

import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.utils.ParameterDriver;

/**
 * Converter for Eckstein-Hechler propagator.
 *
 * @author Bryan Cazabonne
 * @since 11.1
 */
class EcksteinHechlerGradientConverter extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final EcksteinHechlerPropagator propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    EcksteinHechlerGradientConverter(final EcksteinHechlerPropagator propagator) {
        super(propagator, propagator.getMu(), FREE_STATE_PARAMETERS);
        // Initialize fields
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldEcksteinHechlerPropagator<Gradient> getPropagator(final FieldSpacecraftState<Gradient> state,
                                                                  final Gradient[] parameters) {

        // Zero
        final Gradient zero = state.getA().getField().getZero();

        // Model parameters
        final double[]         ck0      = propagator.getCk0();
        final double           radius   = propagator.getReferenceRadius();
        final AttitudeProvider provider = propagator.getAttitudeProvider();

        // Central attraction coefficient
        final Gradient mu = zero.add(propagator.getMu());

        // Return the "Field" propagator
        return new FieldEcksteinHechlerPropagator<>(state.getOrbit(), provider, radius, mu,
                ck0[2], ck0[3], ck0[4], ck0[5], ck0[6]);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
