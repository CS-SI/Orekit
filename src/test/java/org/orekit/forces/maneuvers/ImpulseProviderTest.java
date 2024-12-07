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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImpulseProviderTest {

    @Test
    void testOf() {
        // GIVEN
        final Vector3D forwardImpulse = new Vector3D(1, 2, 3);
        final ImpulseProvider provider = ImpulseProvider.of(forwardImpulse);
        // WHEN
        final Vector3D vector3D = provider.getImpulse(null, true, null);
        // THEN
        Assertions.assertEquals(forwardImpulse, vector3D);
        Assertions.assertEquals(forwardImpulse.negate(), provider.getImpulse(null, false, null));
    }
}
