/* Copyright 2022-2026 Romain Serra
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

package org.orekit.control.heuristics;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AbstractInPlaneImpulseProviderTest {

    @Test
    void testGetter() {
        // GIVEN
        final double maximumMagnitude = 1.;
        final TestAbstractInPlaneImpulseProvider impulseProvider = new TestAbstractInPlaneImpulseProvider(maximumMagnitude,
                mock(Vector3D.class));
        // WHEN
        final double actual = impulseProvider.getMaximumMagnitude();
        // THEN
        assertEquals(maximumMagnitude, actual);
    }

    @Test
    void testGetImpulseConstraintSatisfied() {
        // GIVEN
        final double maximumMagnitude = 1.;
        final Vector3D impulse = Vector3D.ZERO;
        final TestAbstractInPlaneImpulseProvider impulseProvider = new TestAbstractInPlaneImpulseProvider(maximumMagnitude,
                impulse);
        // WHEN
        final Vector3D actual = impulseProvider.getImpulse(mock(SpacecraftState.class), true);
        // THEN
        assertEquals(Vector3D.ZERO, actual);
    }

    @Test
    void testGetImpulseConstraintUnsatisfied() {
        // GIVEN
        final double maximumMagnitude = 0.5;
        final Vector3D impulse = Vector3D.PLUS_I;
        final TestAbstractInPlaneImpulseProvider impulseProvider = new TestAbstractInPlaneImpulseProvider(maximumMagnitude,
                impulse);
        // WHEN
        final Vector3D actual = impulseProvider.getImpulse(mock(SpacecraftState.class), true);
        // THEN
        assertEquals(impulse.normalize().scalarMultiply(maximumMagnitude), actual);
    }

    private static class TestAbstractInPlaneImpulseProvider extends AbstractInPlaneImpulseProvider {

        private final Vector3D impulse;

        protected TestAbstractInPlaneImpulseProvider(double maximumMagnitude, Vector3D impulse) {
            super(maximumMagnitude);
            this.impulse = impulse;
        }

        @Override
        protected Vector3D getUnconstrainedImpulse(SpacecraftState state, boolean isForward) {
            return impulse;
        }
    }
}
