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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldAbstractInPlaneImpulseProviderTest {

    @Test
    void testGetter() {
        // GIVEN
        final Binary64 maximumMagnitude = Binary64.ONE;
        final TestInPlaneImpulseProvider impulseProvider = new TestInPlaneImpulseProvider(maximumMagnitude, null);
        // WHEN
        final Binary64 actual = impulseProvider.getMaximumMagnitude();
        // THEN
        assertEquals(maximumMagnitude, actual);
    }

    @Test
    void testGetImpulseConstraintSatisfied() {
        // GIVEN
        final Binary64 maximumMagnitude = Binary64.ONE;
        final Vector3D impulse = Vector3D.ZERO;
        final TestInPlaneImpulseProvider impulseProvider = new TestInPlaneImpulseProvider(maximumMagnitude,
                new FieldVector3D<>(Binary64Field.getInstance(), impulse));
        // WHEN
        final FieldVector3D<Binary64> actual = impulseProvider.getImpulse(null, true);
        // THEN
        assertEquals(Vector3D.ZERO, actual.toVector3D());
    }

    @Test
    void testGetImpulseConstraintUnsatisfied() {
        // GIVEN
        final Binary64 maximumMagnitude = new Binary64(0.5);
        final FieldVector3D<Binary64> impulse = FieldVector3D.getPlusI(Binary64Field.getInstance());
        final TestInPlaneImpulseProvider impulseProvider = new TestInPlaneImpulseProvider(maximumMagnitude,
                impulse);
        // WHEN
        final FieldVector3D<Binary64> actual = impulseProvider.getImpulse(null, true);
        // THEN
        assertEquals(impulse.normalize().scalarMultiply(maximumMagnitude), actual);
    }

    private static class TestInPlaneImpulseProvider extends FieldAbstractInPlaneImpulseProvider<Binary64> {

        private final FieldVector3D<Binary64> impulse;

        protected TestInPlaneImpulseProvider(Binary64 maximumMagnitude, FieldVector3D<Binary64> impulse) {
            super(maximumMagnitude);
            this.impulse = impulse;
        }

        @Override
        protected FieldVector3D<Binary64> getUnconstrainedImpulse(FieldSpacecraftState<Binary64> state, boolean isForward) {
            return impulse;
        }
    }
}
