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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

class AbstractBodyAttractionTest {

    @Test
    void testDependsOnPositionOnly() {
        // GIVEN
        final AbstractBodyAttraction mockedAttraction = Mockito.mock(AbstractBodyAttraction.class);
        Mockito.when(mockedAttraction.dependsOnPositionOnly()).thenCallRealMethod();
        // WHEN
        final boolean actualDependsOnPositionOnly = mockedAttraction.dependsOnPositionOnly();
        // THEN
        Assertions.assertTrue(actualDependsOnPositionOnly);
    }

    @Test
    void TestGetBodyName() {
        // GIVEN
        final String expectedName = "Moon";
        final CelestialBody mockedBody = Mockito.mock(CelestialBody.class);
        Mockito.when(mockedBody.getName()).thenReturn(expectedName);
        final TestBodyAttraction testBodyAttraction = new TestBodyAttraction(mockedBody);
        // WHEN
        final String actualName = testBodyAttraction.getBodyName();
        // THEN
        Assertions.assertEquals(expectedName, actualName);
    }

    private static class TestBodyAttraction extends AbstractBodyAttraction {

        protected TestBodyAttraction(CelestialBody body) {
            super(body);
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return null;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return null;
        }
    }

}
