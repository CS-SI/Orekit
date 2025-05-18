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
package org.orekit.propagation;

import org.hipparchus.util.Pair;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class implementing step handlers to propagate orbital covariance using linearized Keplerian motion, no matter the propagation model.
 * Although less precise than using the same perturbations than the propagator, it is more computationally performant.
 *
 * @author Romain Serra
 * @since 13.1
 * @see StateCovarianceMatrixProvider
 */
public class LinearKeplerianCovarianceHandler implements OrekitFixedStepHandler {

    /** Initial orbital covariance. */
    private final StateCovariance initialCovariance;

    /** Logged states and covariances. */
    private final List<Pair<SpacecraftState, StateCovariance>> statesWithCovariances = new ArrayList<>();

    /**
     * Constructor.
     * @param initialCovariance initial orbital covariance
     */
    public LinearKeplerianCovarianceHandler(final StateCovariance initialCovariance) {
        this.initialCovariance = initialCovariance;
    }

    /**
     * Gets a copy of the covariances.
     * @return state covariances
     */
    public List<StateCovariance> getStatesCovariances() {
        return statesWithCovariances.stream().map(Pair::getValue).collect(Collectors.toList());
    }

    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t, final double dt) {
        OrekitFixedStepHandler.super.init(s0, t, dt);
        statesWithCovariances.clear();
        statesWithCovariances.add(new Pair<>(s0, initialCovariance));
    }

    @Override
    public void handleStep(final SpacecraftState currentState) {
        final Pair<SpacecraftState, StateCovariance> lastPair = statesWithCovariances.get(statesWithCovariances.size() - 1);
        final Orbit lastOrbit = lastPair.getKey().getOrbit();
        final LinearKeplerianCovarianceMapper covarianceHandler = new LinearKeplerianCovarianceMapper(lastOrbit,
                lastPair.getValue());
        final StateCovariance currentCovariance = covarianceHandler.map(currentState.getOrbit());
        statesWithCovariances.add(new Pair<>(currentState, currentCovariance));
    }

    /**
     * Convert into a non-fixed step handler, based on the instance (so do not use it elsewhere for something else).
     * @return fixed-step handler
     * @see OrekitStepHandler
     */
    public OrekitStepHandler toOrekitStepHandler() {
        final LinearKeplerianCovarianceHandler handler = new LinearKeplerianCovarianceHandler(initialCovariance);

        return new OrekitStepHandler() {
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                OrekitStepHandler.super.init(s0, t);
                handler.init(s0, t, 0.);
                statesWithCovariances.clear();
            }

            @Override
            public void handleStep(final OrekitStepInterpolator interpolator) {
                handler.handleStep(interpolator.getCurrentState());
            }

            @Override
            public void finish(final SpacecraftState finalState) {
                OrekitStepHandler.super.finish(finalState);
                statesWithCovariances.addAll(handler.statesWithCovariances);
            }
        };
    }
}
