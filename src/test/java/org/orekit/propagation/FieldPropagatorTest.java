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
package org.orekit.propagation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.sampling.FieldStepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collection;
import java.util.List;

class FieldPropagatorTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final TestFieldPropagator testPropagator = new TestFieldPropagator();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(ComplexField.getInstance(), date);
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final FieldVector3D<Complex> actualPosition = testPropagator.getPosition(fieldDate, frame);
        // THEN
        final FieldPVCoordinates<Complex> expectedState = testPropagator.propagate(fieldDate).getPVCoordinates(frame);
        Assertions.assertEquals(expectedState.getPosition().toVector3D(), actualPosition.toVector3D());
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final TestFieldPropagator testPropagator = new TestFieldPropagator();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(ComplexField.getInstance(), date);
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final FieldPVCoordinates<Complex> actualState = testPropagator.getPVCoordinates(fieldDate, frame);
        // THEN
        final FieldPVCoordinates<Complex> expectedState = testPropagator.propagate(fieldDate).getPVCoordinates(frame);
        Assertions.assertEquals(expectedState.getPosition().toVector3D(), actualState.getPosition().toVector3D());
        Assertions.assertEquals(expectedState.getVelocity().toVector3D(), actualState.getVelocity().toVector3D());
    }

    @SuppressWarnings("unchecked")
    private static FieldSpacecraftState<Complex> mockFieldSpacecraftState(final FieldAbsoluteDate<Complex> date) {
        final FieldSpacecraftState<Complex> mockedFieldSpacecraftState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedFieldSpacecraftState.getDate()).thenReturn(date);
        final ComplexField complexField = ComplexField.getInstance();
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_K);
        final TimeStampedPVCoordinates tspvc = new TimeStampedPVCoordinates(date.toAbsoluteDate(), pvCoordinates);
        final TimeStampedFieldPVCoordinates<Complex> fieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(
                complexField, tspvc);
        Mockito.when(mockedFieldSpacecraftState.getPVCoordinates()).thenReturn(fieldPVCoordinates);
        Mockito.when(mockedFieldSpacecraftState.getPVCoordinates(Mockito.any(Frame.class)))
                .thenReturn(fieldPVCoordinates);
        Mockito.when(mockedFieldSpacecraftState.getPosition(Mockito.any(Frame.class)))
                .thenReturn(fieldPVCoordinates.getPosition());
        return mockedFieldSpacecraftState;
    }

    private static class TestFieldPropagator implements FieldPropagator<Complex> {

        @Override
        public FieldStepHandlerMultiplexer<Complex> getMultiplexer() {
            return null;
        }

        @Override
        public FieldEphemerisGenerator<Complex> getEphemerisGenerator() {
            return null;
        }

        @Override
        public FieldSpacecraftState<Complex> getInitialState() {
            return null;
        }

        @Override
        public void resetInitialState(FieldSpacecraftState<Complex> state) {
            // not used in test
        }

        @Override
        public void addAdditionalStateProvider(FieldAdditionalStateProvider<Complex> additionalStateProvider) {
            // not used in test
        }

        @Override
        public List<FieldAdditionalStateProvider<Complex>> getAdditionalStateProviders() {
            return null;
        }

        @Override
        public boolean isAdditionalStateManaged(String name) {
            return false;
        }

        @Override
        public String[] getManagedAdditionalStates() {
            return new String[0];
        }

        @Override
        public <D extends FieldEventDetector<Complex>> void addEventDetector(D detector) {
            // not used in test
        }

        @Override
        public Collection<FieldEventDetector<Complex>> getEventsDetectors() {
            return null;
        }

        @Override
        public void clearEventsDetectors() {
            // not used in test
        }

        @Override
        public AttitudeProvider getAttitudeProvider() {
            return null;
        }

        @Override
        public void setAttitudeProvider(AttitudeProvider attitudeProvider) {
            // not used in test
        }

        @Override
        public Frame getFrame() {
            return null;
        }

        @Override
        public FieldSpacecraftState<Complex> propagate(FieldAbsoluteDate<Complex> target) {
            return mockFieldSpacecraftState(target);
        }

        @Override
        public FieldSpacecraftState<Complex> propagate(FieldAbsoluteDate<Complex> start, FieldAbsoluteDate<Complex> target) {
            return null;
        }
    }

}