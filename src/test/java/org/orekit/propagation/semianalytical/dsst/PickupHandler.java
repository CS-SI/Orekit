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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

class PickUpHandler implements OrekitStepHandler, DSSTStateTransitionMatrixGenerator.DSSTPartialsObserver {

    private final DSSTPropagator propagator;
    private final MatricesHarvester harvester;
    private final AbsoluteDate pickUpDate;
    private final String accParamName;
    private final String columnName;
    private DSSTStateTransitionMatrixGenerator stmGenerator;
    private SpacecraftState s0;
    private RealMatrix dYdY0;
    private RealMatrix dYdP;
    private double[] accPartial;

    public PickUpHandler(final DSSTPropagator propagator, final AbsoluteDate pickUpDate,
                         final String accParamName, final String columnName) {
        this.propagator   = propagator;
        this.harvester    = propagator.setupMatricesComputation("stm", null, null);
        initializeShortPeriod();
        this.pickUpDate   = pickUpDate;
        this.accParamName = accParamName;
        this.columnName   = columnName;
        this.s0           = null;
        this.accPartial   = null;
    }

    public SpacecraftState getState() {
        return s0;
    }

    public RealMatrix getStm() {
        return dYdY0;
    }

    public RealMatrix getdYdP() {
        return dYdP;
    }

    public double[] getAccPartial() {
        return accPartial.clone();
    }

    public void init(SpacecraftState s0, AbsoluteDate t) {
        // as the generators are only created on the fly at propagation start
        // we retrieve the STM generator here
        for (final AdditionalDerivativesProvider provider : propagator.getAdditionalDerivativesProviders()) {
            if (provider instanceof DSSTStateTransitionMatrixGenerator) {
                stmGenerator = (DSSTStateTransitionMatrixGenerator) provider;
                stmGenerator.addObserver(accParamName, this);
            }
        }
    }

    public void handleStep(OrekitStepInterpolator interpolator) {
        if (pickUpDate != null) {
            // we want to pick up some intermediate Jacobians
            double dt0 = pickUpDate.durationFrom(interpolator.getPreviousState().getDate());
            double dt1 = pickUpDate.durationFrom(interpolator.getCurrentState().getDate());
            if (dt0 * dt1 > 0) {
                // the current step does not cover the pickup date
                return;
            } else {
                checkState(interpolator.getInterpolatedState(pickUpDate));
            }
        }
    }

    public void finish(SpacecraftState finalState) {
        if (s0 == null) {
            checkState(finalState);
        }
    }

    public void partialsComputed(final SpacecraftState state, final RealMatrix newFactor, final double[] newAccelerationPartials) {
        if (accParamName != null &&
            (pickUpDate == null || FastMath.abs(pickUpDate.durationFrom(state.getDate())) < 1.0e-6)) {
            accPartial = newAccelerationPartials.clone();
        }
    }

    private void checkState(final SpacecraftState state) {
        stmGenerator.combinedDerivatives(state); // just for the side effect of calling partialsComputed
        Assertions.assertEquals(columnName == null ? 1 : 2, state.getAdditionalStatesValues().size());
        dYdY0 = harvester.getStateTransitionMatrix(state);
        dYdP  = harvester.getParametersJacobian(state); // may be null
        s0    = state;
    }


    private void initializeShortPeriod() {
        // Mean orbit
        final SpacecraftState initial = propagator.initialIsOsculating() ?
                       DSSTPropagator.computeMeanState(propagator.getInitialState(), propagator.getAttitudeProvider(), propagator.getAllForceModels()) :
                           propagator.getInitialState();
        ((DSSTHarvester) harvester).initializeFieldShortPeriodTerms(initial); // Initial state is MEAN
    }

}
