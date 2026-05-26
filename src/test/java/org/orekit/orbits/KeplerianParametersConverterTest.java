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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class KeplerianParametersConverterTest {

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testBackAndForthHyperbolic(final PositionAngleType positionAngleType) {
        // GIVEN
        final PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
        final KeplerianParametersConverter converter = new KeplerianParametersConverter(1);
        // WHEN
        final KeplerianParameters elements = converter.toParameters(pv, positionAngleType);
        final PVCoordinates reconverted = converter.toCartesian(elements);
        // THEN
        assertArrayEquals(pv.getPosition().toArray(), reconverted.getPosition().toArray(), 1e-12);
        assertArrayEquals(pv.getVelocity().toArray(), reconverted.getVelocity().toArray(), 1e-13);
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testBackAndForthElliptic(final PositionAngleType positionAngleType) {
        // GIVEN
        final PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(0.1, -0.2, 0.3));
        final KeplerianParametersConverter converter = new KeplerianParametersConverter(1);
        // WHEN
        final KeplerianParameters elements = converter.toParameters(pv, positionAngleType);
        final PVCoordinates reconverted = converter.toCartesian(elements);
        // THEN
        assertArrayEquals(pv.getPosition().toArray(), reconverted.getPosition().toArray(), 1e-15);
        assertArrayEquals(pv.getVelocity().toArray(), reconverted.getVelocity().toArray(), 1e-14);
    }
}
