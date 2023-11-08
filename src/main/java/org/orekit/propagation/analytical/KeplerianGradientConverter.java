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
 * Converter for Keplerian propagator.
 *
 * @author Bryan Cazabonne
 * @since 11.1
 */
class KeplerianGradientConverter extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final KeplerianPropagator propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    KeplerianGradientConverter(final KeplerianPropagator propagator) {
        super(propagator, propagator.getInitialState().getMu(), FREE_STATE_PARAMETERS);
        // Initialize fields
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldKeplerianPropagator<Gradient> getPropagator(final FieldSpacecraftState<Gradient> state,
                                                            final Gradient[] parameters) {

        // Zero
        final Gradient zero = state.getA().getField().getZero();

        // Model parameters
        final AttitudeProvider provider = propagator.getAttitudeProvider();

        // Central attraction coefficient
        final Gradient mu = zero.add(propagator.getInitialState().getMu());

        // Return the "Field" propagator
        return new FieldKeplerianPropagator<>(state.getOrbit(), provider, mu);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
