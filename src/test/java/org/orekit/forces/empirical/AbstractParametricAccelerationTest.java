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
package org.orekit.forces.empirical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

class AbstractParametricAccelerationTest {

    @Test
    void testGetAccelerationDirectionAbsolutePVCoordinates() {
        // GIVEN
        final AbsolutePVCoordinates orbit = new AbsolutePVCoordinates(
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K));
        final TestAcceleration testAcceleration = new TestAcceleration(Vector3D.PLUS_I, true,
                new FrameAlignedProvider(orbit.getFrame()));
        // WHEN
        final Vector3D direction = testAcceleration.getAccelerationDirection(new SpacecraftState(orbit));
        // THEN
        Assertions.assertEquals(Vector3D.PLUS_I, direction);
    }

    @Test
    void testGetAccelerationDirection() {
        // GIVEN
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final TestAcceleration testAcceleration = new TestAcceleration(Vector3D.PLUS_I, true,
                new FrameAlignedProvider(orbit.getFrame()));
        // WHEN
        final Vector3D direction = testAcceleration.getAccelerationDirection(new SpacecraftState(orbit));
        // THEN
        Assertions.assertEquals(Vector3D.PLUS_I, direction);
    }

    @Test
    void testGetAccelerationDirectionNoOverride() {
        // GIVEN
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final TestAcceleration testAcceleration = new TestAcceleration(Vector3D.PLUS_I, true, null);
        // WHEN
        final Vector3D direction = testAcceleration.getAccelerationDirection(new SpacecraftState(orbit));
        // THEN
        Assertions.assertEquals(Vector3D.PLUS_I, direction);
    }

    @Test
    void testGetEventDetectorsEmpty() {
        // GIVEN
        final TestAcceleration testAcceleration = new TestAcceleration(Vector3D.PLUS_I, true, null);
        // WHEN
        final Stream<EventDetector> detectorStream = testAcceleration.getEventDetectors();
        // THEN
        Assertions.assertEquals(0, detectorStream.count());
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final AttitudeProvider mockedAttitudeProvider = Mockito.mock(AttitudeProvider.class);
        Mockito.when(mockedAttitudeProvider.getEventDetectors()).thenReturn(Stream.of(new DateDetector()));
        final TestAcceleration testAcceleration = new TestAcceleration(Vector3D.PLUS_I, true, mockedAttitudeProvider);
        // WHEN
        final Stream<EventDetector> detectorStream = testAcceleration.getEventDetectors();
        // THEN
        Assertions.assertEquals(1, detectorStream.count());
    }

    private static class TestAcceleration extends AbstractParametricAcceleration {

        protected TestAcceleration(Vector3D direction, boolean isInertial, AttitudeProvider attitudeOverride) {
            super(direction, isInertial, attitudeOverride);
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return null;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return null;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }
}
