/* Copyright 2022-2023 Romain Serra
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class AbstractAnalyticalPropagatorTest {

    @Test
    void testSetResetAtEndTrue() {
        // GIVEN
        final TestAbstractAnalyticalPropagator propagator = createPropagator();
        final double nonZeroTimeOfFlight = -10.;
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(nonZeroTimeOfFlight);
        // WHEN
        propagator.setResetAtEnd(true);
        propagator.propagate(date);
        // THEN
        final SpacecraftState expectedSpacecraftState = createPropagator().propagate(date);
        compareSpacecraftStates(expectedSpacecraftState, propagator.getInitialState());
    }

    @Test
    void testSetResetAtEndFalse() {
        // GIVEN
        final TestAbstractAnalyticalPropagator propagator = createPropagator();
        final double nonZeroTimeOfFlight = 10.;
        final SpacecraftState initialState = propagator.getInitialState();
        // WHEN
        propagator.setResetAtEnd(false);
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(nonZeroTimeOfFlight));
        // THEN
        compareSpacecraftStates(initialState, propagator.getInitialState());
    }

    private void compareSpacecraftStates(final SpacecraftState referenceState,
                                         final SpacecraftState otherState) {
        Assertions.assertEquals(referenceState.getFrame(), otherState.getFrame());
        Assertions.assertEquals(referenceState.getDate(), otherState.getDate());
        Assertions.assertEquals(referenceState.getPosition(), referenceState.getPosition());
        Assertions.assertEquals(referenceState.getMass(), otherState.getMass());
    }

    private TestAbstractAnalyticalPropagator createPropagator() {
        final AbsoluteDate initialDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final AttitudeProvider attitudeProvider = new FrameAlignedProvider(frame);
        final PVCoordinates initialPV = new PVCoordinates();
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(initialPV, frame, initialDate,
                Constants.EGM96_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(cartesianOrbit);
        final TestAbstractAnalyticalPropagator propagator = new TestAbstractAnalyticalPropagator(attitudeProvider);
        propagator.resetInitialState(initialState);
        return propagator;
    }

    private static class TestAbstractAnalyticalPropagator extends AbstractAnalyticalPropagator {

        protected TestAbstractAnalyticalPropagator(AttitudeProvider attitudeProvider) {
            super(attitudeProvider);
        }

        @Override
        protected double getMass(AbsoluteDate date) {
            return getInitialState().getMass();
        }

        @Override
        protected void resetIntermediateState(SpacecraftState state, boolean forward) {
            // do nothing
        }

        @Override
        protected Orbit propagateOrbit(AbsoluteDate date) {
            final SpacecraftState initialState = getInitialState();
            final Frame propagationFrame = getFrame();
            final PVCoordinates pvCoordinates = initialState.getPVCoordinates(propagationFrame);
            return new CartesianOrbit(pvCoordinates, propagationFrame, date, initialState.getMu());
        }
    }

}