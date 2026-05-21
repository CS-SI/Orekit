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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class TleExtendedPositionProviderTest {

    private static final String LINE1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    private static final String LINE2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

    @BeforeEach
    void setUpBeforeClass() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetPosition() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle, FramesFactory.getFrames());
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D position = positionProvider.getPosition(date, frame);
        // THEN
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        assertArrayEquals(propagator.getPosition(date, frame).toArray(), position.toArray(), 1e-6);
    }

    @Test
    void testGetVelocity() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle, FramesFactory.getFrames());
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D velocity = positionProvider.getVelocity(date, frame);
        // THEN
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        assertArrayEquals(propagator.getVelocity(date, frame).toArray(), velocity.toArray(), 1e-6);
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle);
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final TimeStampedPVCoordinates actualPV = positionProvider.getPVCoordinates(date, frame);
        // THEN
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final TimeStampedPVCoordinates expectedPV = propagator.getPVCoordinates(date, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Test
    void testFieldGetPosition() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final Binary64Field field = Binary64Field.getInstance();
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle);
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> position = positionProvider.getPosition(fieldDate, frame);
        // THEN
        final FieldTLE<Binary64> fieldTLE = new FieldTLE<>(field, tle);
        final FieldTLEPropagator<Binary64> propagator = FieldTLEPropagator.selectExtrapolator(fieldTLE, tle.getParameters(field));
        final FieldVector3D<Binary64> expectedPosition = propagator.getPosition(fieldDate, frame);
        assertEquals(expectedPosition.getX().getReal(), position.getX().getReal(), 1e-6);
        assertEquals(expectedPosition.getY().getReal(), position.getY().getReal(), 1e-6);
        assertEquals(expectedPosition.getZ().getReal(), position.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetVelocity() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final Binary64Field field = Binary64Field.getInstance();
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle);
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> velocity = positionProvider.getVelocity(fieldDate, frame);
        // THEN
        final FieldTLE<Binary64> fieldTLE = new FieldTLE<>(field, tle);
        final FieldTLEPropagator<Binary64> propagator = FieldTLEPropagator.selectExtrapolator(fieldTLE, tle.getParameters(field));
        final FieldVector3D<Binary64> expectedVelocity = propagator.getVelocity(fieldDate, frame);
        assertEquals(expectedVelocity.getX().getReal(), velocity.getX().getReal(), 1e-6);
        assertEquals(expectedVelocity.getY().getReal(), velocity.getY().getReal(), 1e-6);
        assertEquals(expectedVelocity.getZ().getReal(), velocity.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetPVCoordinates() {
        // GIVEN
        final TLE tle = new TLE(LINE1, LINE2);
        final Binary64Field field = Binary64Field.getInstance();
        final TleExtendedPositionProvider positionProvider = new TleExtendedPositionProvider(tle);
        final AbsoluteDate date = tle.getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final TimeStampedFieldPVCoordinates<Binary64> actualPV = positionProvider.getPVCoordinates(fieldDate, frame);
        // THEN
        final FieldTLE<Binary64> fieldTLE = new FieldTLE<>(field, LINE1, LINE2);
        final FieldTLEPropagator<Binary64> propagator = FieldTLEPropagator.selectExtrapolator(fieldTLE, tle.getParameters(field));
        final TimeStampedFieldPVCoordinates<Binary64> expectedPV = propagator.getPVCoordinates(fieldDate, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }
}
