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
package org.orekit.attitudes;

import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BoundedAttitudeProviderTest {

    @Test
    void testOfGetters() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(10.);
        final TimeInterval timeInterval = TimeInterval.of(minDate, maxDate);
        final AttitudeProvider provider = mock();
        // WHEN
        final BoundedAttitudeProvider boundedAttitudeProvider = BoundedAttitudeProvider.of(provider, timeInterval);
        // THEN
        assertEquals(minDate, boundedAttitudeProvider.getMinDate());
        assertEquals(maxDate, boundedAttitudeProvider.getMaxDate());
    }

    @Test
    void testOfAttitude() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(10.);
        final TimeInterval timeInterval = TimeInterval.of(minDate, maxDate);
        final FrameAlignedProvider provider = new FrameAlignedProvider(FramesFactory.getEME2000());
        final Orbit orbit = TestUtils.getDefaultOrbit(minDate);
        // WHEN
        final BoundedAttitudeProvider boundedAttitudeProvider = BoundedAttitudeProvider.of(provider, timeInterval);
        // THEN
        final Attitude actualAttitude = boundedAttitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        final Attitude expectedAttitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        assertEquals(0., Rotation.distance(actualAttitude.getRotation(), expectedAttitude.getRotation()), 1e-6);
        assertEquals(actualAttitude.getSpin(), expectedAttitude.getSpin());
        assertEquals(actualAttitude.getRotationAcceleration(), expectedAttitude.getRotationAcceleration());
        assertEquals(actualAttitude.getDate(), expectedAttitude.getDate());
        assertEquals(actualAttitude.getReferenceFrame(), expectedAttitude.getReferenceFrame());
        final Rotation actualRotation = boundedAttitudeProvider.getAttitudeRotation(orbit, orbit.getDate(), orbit.getFrame());
        final Rotation expectedRotation = provider.getAttitudeRotation(orbit, orbit.getDate(), orbit.getFrame());
        assertEquals(0., Rotation.distance(actualRotation, expectedRotation), 1e-6);
    }

    @Test
    void testOfFieldAttitude() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(10.);
        final TimeInterval timeInterval = TimeInterval.of(minDate, maxDate);
        final FrameAlignedProvider provider = new FrameAlignedProvider(FramesFactory.getEME2000());
        final Orbit orbit = TestUtils.getDefaultOrbit(maxDate);
        final FieldOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(Binary64Field.getInstance(), orbit);
        // WHEN
        final BoundedAttitudeProvider boundedAttitudeProvider = BoundedAttitudeProvider.of(provider, timeInterval);
        // THEN
        final FieldAttitude<Binary64> actualAttitude = boundedAttitudeProvider.getAttitude(fieldOrbit, fieldOrbit.getDate(), orbit.getFrame());
        final FieldAttitude<Binary64> expectedAttitude = provider.getAttitude(fieldOrbit, fieldOrbit.getDate(), orbit.getFrame());
        assertEquals(0., Rotation.distance(actualAttitude.getRotation().toRotation(),
                expectedAttitude.getRotation().toRotation()), 1e-6);
        assertEquals(actualAttitude.getSpin(), expectedAttitude.getSpin());
        assertEquals(actualAttitude.getRotationAcceleration(), expectedAttitude.getRotationAcceleration());
        assertEquals(actualAttitude.getDate(), expectedAttitude.getDate());
        assertEquals(actualAttitude.getReferenceFrame(), expectedAttitude.getReferenceFrame());
        final FieldRotation<Binary64> actualRotation = boundedAttitudeProvider.getAttitudeRotation(fieldOrbit, fieldOrbit.getDate(), orbit.getFrame());
        final FieldRotation<Binary64> expectedRotation = provider.getAttitudeRotation(fieldOrbit, fieldOrbit.getDate(), orbit.getFrame());
        assertEquals(0., Rotation.distance(actualRotation.toRotation(), expectedRotation.toRotation()), 1e-6);
    }
}
