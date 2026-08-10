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
package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class ShiftablePVCoordinatesHolderTest {

    private static final TimeStampedPVCoordinates PV = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
            Vector3D.MINUS_I, Vector3D.MINUS_K, Vector3D.MINUS_J);

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @ParameterizedTest
    @EnumSource(Predefined.class)
    void testGetPositionOtherFrame(final Predefined predefined) {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final Vector3D position = testShiftablePVCoordinatesHolder.getPosition(frame);
        // THEN
        final PVCoordinates expected = testShiftablePVCoordinatesHolder.getPVCoordinates(frame);
        assertEquals(expected.getPosition(), position);
    }

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetPositionOtherDate(final Predefined predefined) {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        final AbsoluteDate shiftedDate = PV.getDate().shiftedBy(1);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final Vector3D position = testShiftablePVCoordinatesHolder.getPosition(shiftedDate, frame);
        // THEN
        final PVCoordinates expected = testShiftablePVCoordinatesHolder.getPVCoordinates(shiftedDate, frame);
        assertEquals(expected.getPosition(), position);
    }

    @ParameterizedTest
    @EnumSource(Predefined.class)
    void testGetVelocityOtherFrame(final Predefined predefined) {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final Vector3D velocity = testShiftablePVCoordinatesHolder.getVelocity(frame);
        // THEN
        final PVCoordinates expected = testShiftablePVCoordinatesHolder.getPVCoordinates(frame);
        assertEquals(expected.getVelocity(), velocity);
    }

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetVelocityOtherDate(final Predefined predefined) {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        final AbsoluteDate shiftedDate = PV.getDate().shiftedBy(1);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final Vector3D velocity = testShiftablePVCoordinatesHolder.getVelocity(shiftedDate, frame);
        // THEN
        final PVCoordinates expected = testShiftablePVCoordinatesHolder.getPVCoordinates(shiftedDate, frame);
        assertEquals(expected.getVelocity(), velocity);
    }

    @Test
    void testGetterVelocity() {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        // WHEN
        final Vector3D velocity = testShiftablePVCoordinatesHolder.getVelocity();
        // THEN
        assertEquals(PV.getVelocity(), velocity);
    }

    private static class TestShiftablePVCoordinatesHolder implements ShiftablePVCoordinatesHolder<TestShiftablePVCoordinatesHolder> {

        private final TimeStampedPVCoordinates timeStampedPVCoordinates;
        private final Frame frame;

        TestShiftablePVCoordinatesHolder(final TimeStampedPVCoordinates timeStampedPVCoordinates,
                                         final Frame frame) {
            this.timeStampedPVCoordinates = timeStampedPVCoordinates;
            this.frame = frame;
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates() {
            return timeStampedPVCoordinates;
        }

        @Override
        public Frame getFrame() {
            return frame;
        }

        @Override
        public TestShiftablePVCoordinatesHolder shiftedBy(double dt) {
            return new TestShiftablePVCoordinatesHolder(new TimeStampedPVCoordinates(getDate().shiftedBy(dt),
                    timeStampedPVCoordinates.getPosition().add(getVelocity().scalarMultiply(dt)),
                    timeStampedPVCoordinates.getVelocity(),
                    timeStampedPVCoordinates.getAcceleration()), getFrame());
        }

        @Override
        public AbsoluteDate getDate() {
            return timeStampedPVCoordinates.getDate();
        }
    }
}
