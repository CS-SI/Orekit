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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiftablePVCoordinatesHolderTest {

    private static final TimeStampedPVCoordinates PV = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
            Vector3D.MINUS_I, Vector3D.MINUS_K, Vector3D.MINUS_J);

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @ParameterizedTest
    @EnumSource(Predefined.class)
    void testGetPosition(final Predefined predefined) {
        // GIVEN
        final TestShiftablePVCoordinatesHolder testShiftablePVCoordinatesHolder = new TestShiftablePVCoordinatesHolder(PV,
                FramesFactory.getEME2000());
        final AbsoluteDate shiftedDate = PV.getDate().shiftedBy(1);
        final Frame frame = FramesFactory.getFrame(predefined);
        // WHEN
        final Vector3D position = testShiftablePVCoordinatesHolder.getPosition(shiftedDate, frame);
        // THEN
        final PVCoordinates expected = testShiftablePVCoordinatesHolder.getPVCoordinates(shiftedDate, frame);
        assertArrayEquals(expected.getPosition().toArray(), position.toArray(), 1e-15);
    }

    @ParameterizedTest
    @EnumSource(Predefined.class)
    void testGetVelocity(final Predefined predefined) {
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
                    timeStampedPVCoordinates.getPosition(), timeStampedPVCoordinates.getVelocity(),
                    timeStampedPVCoordinates.getAcceleration()), getFrame());
        }

        @Override
        public AbsoluteDate getDate() {
            return timeStampedPVCoordinates.getDate();
        }
    }
}
