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
package org.orekit.propagation.numerical;

import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.IntegrableGenerator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

class PickUpHandler implements OrekitStepHandler {

    private final NumericalPropagator propagator;
    private final MatricesHarvester harvester;
    private final AbsoluteDate pickUpDate;
    private final String accParamName;
    private final String columnName;
    private SpacecraftState s0;
    private RealMatrix dYdY0;
    private RealMatrix dYdP;
    private double[] accPartial;
    private StateTransitionMatrixGenerator stmGenerator;

    public PickUpHandler(final NumericalPropagator propagator, final AbsoluteDate pickUpDate,
                         final String accParamName, final String columnName) {
        this.propagator   = propagator;
        this.harvester    = propagator.getMatricesHarvester("stm", null, null);
        this.pickUpDate   = pickUpDate;
        this.accParamName = accParamName;
        this.columnName   = columnName;
        this.s0           = null;
        this.accPartial   = null;
        Assert.assertTrue(harvester.getJacobiansColumnsNames().isEmpty());
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
        for (final IntegrableGenerator generator : propagator.getIntegrableGenerators()) {
            if (generator instanceof StateTransitionMatrixGenerator) {
                stmGenerator = (StateTransitionMatrixGenerator) generator;
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

    private void checkState(final SpacecraftState state) {
        Assert.assertEquals(columnName == null ? 1 : 2, state.getAdditionalStatesValues().size());
        dYdY0 = harvester.getStateTransitionMatrix(state);
        if (accParamName != null) {
            accPartial = stmGenerator.getAccelerationPartials(accParamName);
        }
        dYdP = harvester.getParametersJacobian(state); // may be null
        s0 = state;
    }

}
