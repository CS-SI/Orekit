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
package org.orekit.propagation.analytical;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.TestUtils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldAdditionalDataProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

class FieldAbstractAnalyticalPropagatorTest {

    @Test
    void testRemoveAdditionalDataProvider() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = TestUtils.getDefaultOrbit(date);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final String name = "a";
        final FieldAdditionalDataProvider<Boolean, Complex> dataProvider = new FieldAdditionalDataProvider<Boolean, Complex>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Boolean getAdditionalData(FieldSpacecraftState<Complex> state) {
                return Boolean.TRUE;
            }
        };
        // WHEN & THEN
        propagator.addAdditionalDataProvider(dataProvider);
        Assertions.assertFalse(propagator.getAdditionalDataProviders().isEmpty());
        propagator.removeAdditionalDataProvider(dataProvider.getName());
        Assertions.assertTrue(propagator.getAdditionalDataProviders().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {"RESET_STATE", "RESET_DERIVATIVES"})
    void testReset(final Action action) {
        // GIVEN
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
        final Orbit orbit = TestUtils.getDefaultOrbit(date.toAbsoluteDate());
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final FieldEventHandler<Complex> handler = (s, detector, increasing) -> action;
        final TestDetector detector = new TestDetector(date.shiftedBy(0.5), handler);
        propagator.addEventDetector(detector);
        // WHEN
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(1.));
        // THEN
        Assertions.assertTrue(detector.resetted);
    }

    private static class TestDetector extends FieldDateDetector<Complex> {
        boolean resetted = false;

        TestDetector(final FieldAbsoluteDate<Complex> date, final FieldEventHandler<Complex> handler) {
            super(new FieldEventDetectionSettings<>(date.getField(), EventDetectionSettings.getDefaultEventDetectionSettings()),
                    handler, 1., date);
        }

        @Override
        public void reset(FieldSpacecraftState<Complex> state, FieldAbsoluteDate<Complex> target) {
            resetted = true;
        }
    }

    @Test
    void testInternalEventDetectors() {
        // GIVEN
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                eme2000, date, Constants.EIGEN5C_EARTH_MU);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final FieldAbsoluteDate<Complex> interruptingDate = propagator.getInitialState().getDate().shiftedBy(1);
        propagator.setAttitudeProvider(new InterruptingAttitudeProvider(interruptingDate.toAbsoluteDate()));
        // WHEN
        final FieldSpacecraftState<Complex> state = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(10.));
        // THEN
        Assertions.assertEquals(state.getDate(), interruptingDate);
    }

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
        public FieldOrbit<Complex> propagateOrbit(
                        FieldAbsoluteDate<Complex> date, Complex[] parameters) {
            final FieldSpacecraftState<Complex> state = getInitialState();
            return new FieldCartesianOrbit<>(state.getOrbit().getPVCoordinates(), getFrame(), date, state.getOrbit().getMu());
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

    private static class InterruptingAttitudeProvider extends FrameAlignedProvider {

        private final AbsoluteDate interruptingDate;

        public InterruptingAttitudeProvider(final AbsoluteDate interruptingDate) {
            super(Rotation.IDENTITY);
            this.interruptingDate = interruptingDate;
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
            final FieldDateDetector<T> detector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, interruptingDate))
                    .withHandler(new FieldStopOnEvent<>());
            return Stream.of(detector);
        }
    }
}
