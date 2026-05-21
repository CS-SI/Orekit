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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class ShiftableFieldPVCoordinatesHolderTest {

    private static final Binary64Field FIELD = Binary64Field.getInstance();
    private static final FieldPVCoordinates<Binary64> PV = new FieldPVCoordinates<>(FieldVector3D.getMinusI(FIELD),
            FieldVector3D.getMinusJ(FIELD), FieldVector3D.getMinusK(FIELD));

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetPositionDate(final Predefined predefined) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final TestShiftableFieldPVCoordinatesHolder pvHolder = new TestShiftableFieldPVCoordinatesHolder(fieldDate, PV);
        final FieldAbsoluteDate<Binary64> otherDate = fieldDate.shiftedBy(1);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final FieldVector3D<Binary64> position = pvHolder.getPosition(otherDate, frame);
        // THEN
        final FieldVector3D<Binary64> expected = pvHolder.getPVCoordinates(otherDate, frame).getPosition();
        assertEquals(expected, position);
    }

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetPositionFrame(final Predefined predefined) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final TestShiftableFieldPVCoordinatesHolder pvHolder = new TestShiftableFieldPVCoordinatesHolder(fieldDate, PV);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final FieldVector3D<Binary64> position = pvHolder.getPosition(frame);
        // THEN
        final FieldVector3D<Binary64> expected = pvHolder.getPVCoordinates(frame).getPosition();
        assertEquals(expected, position);
    }

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetVelocityDate(final Predefined predefined) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final TestShiftableFieldPVCoordinatesHolder pvHolder = new TestShiftableFieldPVCoordinatesHolder(fieldDate, PV);
        final FieldAbsoluteDate<Binary64> otherDate = fieldDate.shiftedBy(1);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final FieldVector3D<Binary64> velocity = pvHolder.getVelocity(otherDate, frame);
        // THEN
        final FieldVector3D<Binary64> expected = pvHolder.getPVCoordinates(otherDate, frame).getVelocity();
        assertEquals(expected, velocity);
    }

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000"})
    void testGetVelocityFrame(final Predefined predefined) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final TestShiftableFieldPVCoordinatesHolder pvHolder = new TestShiftableFieldPVCoordinatesHolder(fieldDate, PV);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final FieldVector3D<Binary64> velocity = pvHolder.getVelocity(frame);
        // THEN
        final FieldVector3D<Binary64> expected = pvHolder.getPVCoordinates(frame).getVelocity();
        assertEquals(expected, velocity);
    }

    private static class TestShiftableFieldPVCoordinatesHolder implements ShiftableFieldPVCoordinatesHolder<TestShiftableFieldPVCoordinatesHolder, Binary64> {

        private static final Binary64Field FIELD = Binary64Field.getInstance();
        final TimeStampedFieldPVCoordinates<Binary64> timeStampedFieldPVCoordinates;

        TestShiftableFieldPVCoordinatesHolder(final FieldAbsoluteDate<Binary64> date,
                                              final FieldPVCoordinates<Binary64> pv) {
            timeStampedFieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(date, pv);
        }

        @Override
        public TimeStampedFieldPVCoordinates<Binary64> getPVCoordinates() {
            return timeStampedFieldPVCoordinates;
        }

        @Override
        public Frame getFrame() {
            return FramesFactory.getGCRF();
        }

        @Override
        public FieldAbsoluteDate<Binary64> getDate() {
            return timeStampedFieldPVCoordinates.getDate();
        }

        @Override
        public TestShiftableFieldPVCoordinatesHolder shiftedBy(double dt) {
            return new TestShiftableFieldPVCoordinatesHolder(timeStampedFieldPVCoordinates.getDate().shiftedBy(dt),
                    new FieldPVCoordinates<>(getPosition().add(getVelocity().scalarMultiply(dt)), getVelocity(),
                            timeStampedFieldPVCoordinates.getAcceleration()));
        }

        @Override
        public TestShiftableFieldPVCoordinatesHolder shiftedBy(Binary64 dt) {
            return shiftedBy(dt.getReal());
        }
    }
}
