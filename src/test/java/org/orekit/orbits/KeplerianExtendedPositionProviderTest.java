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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class KeplerianExtendedPositionProviderTest {

    @BeforeEach
    void setUpBeforeClass() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetPosition() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(OrbitType.EQUINOCTIAL.convertType(orbit));
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D position = positionProvider.getPosition(date, frame);
        // THEN
        assertArrayEquals(orbit.getPosition(date, frame).toArray(), position.toArray(), 1e-6);
    }

    @Test
    void testGetVelocity() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(OrbitType.EQUINOCTIAL.convertType(orbit));
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D velocity = positionProvider.getVelocity(date, frame);
        // THEN
        assertArrayEquals(orbit.getVelocity(date, frame).toArray(), velocity.toArray(), 1e-6);
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e5);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final TimeStampedPVCoordinates actualPV = positionProvider.getPVCoordinates(date, frame);
        // THEN
        final TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(date, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Test
    void testFieldGetPosition() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final EquinoctialOrbit equinoctialOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(equinoctialOrbit);
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> position = positionProvider.getPosition(fieldDate, frame);
        // THEN
        final FieldVector3D<Binary64> expectedPosition = new FieldEquinoctialOrbit<>(fieldDate.getField(), equinoctialOrbit).getPosition(fieldDate, frame);
        assertEquals(expectedPosition.getX().getReal(), position.getX().getReal(), 1e-6);
        assertEquals(expectedPosition.getY().getReal(), position.getY().getReal(), 1e-6);
        assertEquals(expectedPosition.getZ().getReal(), position.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetVelocity() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final EquinoctialOrbit equinoctialOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(equinoctialOrbit);
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> velocity = positionProvider.getVelocity(fieldDate, frame);
        // THEN
        final FieldVector3D<Binary64> expectedVelocity = new FieldEquinoctialOrbit<>(fieldDate.getField(), equinoctialOrbit).getVelocity(fieldDate, frame);
        assertEquals(expectedVelocity.getX().getReal(), velocity.getX().getReal(), 1e-6);
        assertEquals(expectedVelocity.getY().getReal(), velocity.getY().getReal(), 1e-6);
        assertEquals(expectedVelocity.getZ().getReal(), velocity.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetPVCoordinates() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final AbsoluteDate date = orbit.getDate().shiftedBy(1e5);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final TimeStampedFieldPVCoordinates<Binary64> actualPV = positionProvider.getPVCoordinates(fieldDate, frame);
        // THEN
        final TimeStampedFieldPVCoordinates<Binary64> expectedPV = new FieldCartesianOrbit<>(fieldDate.getField(), orbit).getPVCoordinates(fieldDate, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }
}
