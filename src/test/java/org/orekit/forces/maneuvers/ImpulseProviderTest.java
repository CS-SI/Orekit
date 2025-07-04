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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

class ImpulseProviderTest {

    @Test
    void testOf() {
        // GIVEN
        final Vector3D forwardImpulse = new Vector3D(1, 2, 3);
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit).withMass(100);
        // WHEN
        final ImpulseProvider provider = ImpulseProvider.of(forwardImpulse);
        final Vector3D vector3D = provider.getImpulse(state, true);
        // THEN
        Assertions.assertEquals(forwardImpulse, vector3D);
        Assertions.assertEquals(forwardImpulse.negate(), provider.getImpulse(state, false));
    }
}
