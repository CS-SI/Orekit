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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldOsculatingSmaChangeImpulseProviderTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetImpulseAlready(final boolean isForward) {
        // GIVEN
        final Binary64 semiMajorAxis = new Binary64(1e7);
        final FieldSmaChangingImpulseProvider<Binary64> impulseProvider = new FieldSmaChangingImpulseProvider<>(semiMajorAxis);
        final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(Binary64Field.getInstance(),
                new EquinoctialOrbit(semiMajorAxis.getReal(), 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU));
        // WHEN
        final FieldVector3D<Binary64> impulse = impulseProvider.getImpulse(new FieldSpacecraftState<>(orbit), isForward);
        // THEN
        assertEquals(FieldVector3D.getZero(semiMajorAxis.getField()), impulse);
    }

    @Test
    void testGetImpulseDifferentConstrained() {
        // GIVEN
        final Binary64 semiMajorAxis = new Binary64(1.0e7);
        final double maximumMagnitude = 10.;
        final FieldSmaChangingImpulseProvider<Binary64> impulseProvider = new FieldSmaChangingImpulseProvider<>(new Binary64(maximumMagnitude),
                semiMajorAxis);
        final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(Binary64Field.getInstance(),
                new EquinoctialOrbit(semiMajorAxis.getReal() + 1e6, 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                        FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU));
        // WHEN
        final FieldVector3D<Binary64> impulse = impulseProvider.getImpulse(new FieldSpacecraftState<>(orbit), true);
        // THEN
        assertEquals(maximumMagnitude, impulse.getNorm2().getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0., 1., 2., 3., 4., 5., 6})
    void testGetImpulseNonField(final double anomaly) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetSemiMajorAxis = new Binary64(1e7);
        final FieldSmaChangingImpulseProvider<Binary64> impulseProvider = new FieldSmaChangingImpulseProvider<>(targetSemiMajorAxis);
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(targetSemiMajorAxis.getReal() + 1e6, 0.5, 0., 0., 0., anomaly, PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final FieldOrbit<Binary64> fieldOrbit = new FieldKeplerianOrbit<>(field, keplerianOrbit);
        final boolean isForward = true;
        // WHEN
        final FieldVector3D<Binary64> impulse = impulseProvider.getImpulse(new FieldSpacecraftState<>(fieldOrbit), isForward);
        // THEN
        final Vector3D expected = new OsculatingSmaChangeImpulseProvider(targetSemiMajorAxis.getReal())
                .getImpulse(new SpacecraftState(keplerianOrbit), isForward);
        assertEquals(expected, impulse.toVector3D());
    }
}
