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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.StepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collection;
import java.util.List;

class PropagatorTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final TestPropagator testPropagator = new TestPropagator();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final Vector3D actualPosition = testPropagator.getPosition(date, frame);
        // THEN
        final PVCoordinates expectedState = testPropagator.propagate(date).getPVCoordinates(frame);
        Assertions.assertEquals(expectedState.getPosition(), actualPosition);
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final TestPropagator testPropagator = new TestPropagator();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final PVCoordinates actualState = testPropagator.getPVCoordinates(date, frame);
        // THEN
        final PVCoordinates expectedState = testPropagator.propagate(date).getPVCoordinates(frame);
        Assertions.assertEquals(expectedState.getPosition(), actualState.getPosition());
        Assertions.assertEquals(expectedState.getVelocity(), actualState.getVelocity());
    }

    private static SpacecraftState mockSpacecraftState(final AbsoluteDate date) {
        final SpacecraftState mockedSpacecraftState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedSpacecraftState.getDate()).thenReturn(date);
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_K);
        final TimeStampedPVCoordinates tspvc = new TimeStampedPVCoordinates(date, pvCoordinates);
        Mockito.when(mockedSpacecraftState.getPVCoordinates()).thenReturn(tspvc);
        Mockito.when(mockedSpacecraftState.getPVCoordinates(Mockito.any(Frame.class))).thenReturn(tspvc);
        Mockito.when(mockedSpacecraftState.getPosition(Mockito.any(Frame.class)))
                .thenReturn(pvCoordinates.getPosition());
        return mockedSpacecraftState;
    }

    private static class TestPropagator implements Propagator {

        @Override
        public StepHandlerMultiplexer getMultiplexer() {
            return null;
        }

        @Override
        public EphemerisGenerator getEphemerisGenerator() {
            return null;
        }

        @Override
        public SpacecraftState getInitialState() {
            return null;
        }

        @Override
        public void resetInitialState(SpacecraftState state) {
            // not used in test
        }

        @Override
        public void addAdditionalStateProvider(AdditionalStateProvider additionalStateProvider) {
            // not used in test
        }

        @Override
        public List<AdditionalStateProvider> getAdditionalStateProviders() {
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
        public <T extends EventDetector> void addEventDetector(T detector) {
            // not used in test
        }

        @Override
        public Collection<EventDetector> getEventsDetectors() {
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
        public SpacecraftState propagate(AbsoluteDate target) {
            return mockSpacecraftState(target);
        }

        @Override
        public SpacecraftState propagate(AbsoluteDate start, AbsoluteDate target) {
            return null;
        }
    }

}