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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

class FieldImpulseProviderTest {

    @Test
    void testOfVector3D() {
        // GIVEN
        final Vector3D forwardImpulse = new Vector3D(1, 2, 3);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldImpulseProvider<Binary64> provider = FieldImpulseProvider.of(field, forwardImpulse);
        // WHEN
        final Vector3D vector3D = provider.getImpulse(null, true, null).toVector3D();
        // THEN
        Assertions.assertEquals(forwardImpulse, vector3D);
        Assertions.assertEquals(forwardImpulse.negate(), provider.getImpulse(null, false, null).toVector3D());
    }

    @Test
    void testOfFieldVector3D() {
        // GIVEN
        final Vector3D forwardImpulse = new Vector3D(1, 2, 3);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldImpulseProvider<Binary64> provider = FieldImpulseProvider.of(new FieldVector3D<>(field, forwardImpulse));
        // WHEN
        final Vector3D vector3D = provider.getImpulse(null, true, null).toVector3D();
        // THEN
        Assertions.assertEquals(forwardImpulse, vector3D);
        Assertions.assertEquals(forwardImpulse.negate(), provider.getImpulse(null, false, null).toVector3D());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOfImpulseProvider(final boolean isForward) {
        // GIVEN
        final Vector3D forwardImpulse = new Vector3D(1, 2, 3);
        final ImpulseProvider impulseProvider = ImpulseProvider.of(forwardImpulse);
        final FieldImpulseProvider<Binary64> fieldImpulseProvider = FieldImpulseProvider.of(impulseProvider);
        final FieldSpacecraftState<Binary64> fieldSpacecraftState = buildFieldState();
        // WHEN
        final Vector3D vector3D = fieldImpulseProvider.getImpulse(fieldSpacecraftState, isForward, null).toVector3D();
        // THEN
        Assertions.assertEquals(impulseProvider.getImpulse(fieldSpacecraftState.toSpacecraftState(), isForward, null), vector3D);
    }

    private static FieldSpacecraftState<Binary64> buildFieldState() {
        return new FieldSpacecraftState<>(new FieldCartesianOrbit<>(Binary64Field.getInstance(),
                new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K), FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1)));
    }
}

