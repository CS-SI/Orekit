/* Copyright 2022-2024 Romain Serra
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


import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

class FieldAbstractAnalyticalPropagatorTest {

    @Test
    void testFinish() {
        // GIVEN
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                eme2000, date, Constants.EIGEN5C_EARTH_MU);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final TestHandler handler = new TestHandler();
        final ComplexField field = ComplexField.getInstance();
        propagator.addEventDetector(new FieldDateDetector<>(field, FieldAbsoluteDate.getArbitraryEpoch(field)).withHandler(handler));
        // WHEN
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(1.));
        // THEN
        Assertions.assertTrue(handler.isFinished);
    }

    private static class TestAnalyticalPropagator extends FieldAbstractAnalyticalPropagator<Complex> {

        protected TestAnalyticalPropagator(Orbit orbit) {
            super(ComplexField.getInstance(), new FrameAlignedProvider(FramesFactory.getGCRF()));
            resetInitialState(new FieldSpacecraftState<>(new FieldCartesianOrbit<>(getField(), orbit)));
        }

        @Override
        protected Complex getMass(FieldAbsoluteDate<Complex> date) {
            return Complex.ONE;
        }

        @Override
        protected void resetIntermediateState(FieldSpacecraftState<Complex> state, boolean forward) {

        }

        @Override
        protected FieldOrbit<Complex> propagateOrbit(FieldAbsoluteDate<Complex> date, Complex[] parameters) {
            return getInitialState().getOrbit().shiftedBy(date.durationFrom(getStartDate()));
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }

    private static class TestHandler extends FieldContinueOnEvent<Complex> {
        boolean isFinished = false;

        @Override
        public void finish(FieldSpacecraftState<Complex> finalState, FieldEventDetector<Complex> detector) {
            isFinished = true;
        }
    }
}
