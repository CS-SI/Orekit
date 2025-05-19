package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundedPVCoordinatesProviderTest {

    @Test
    void testOf() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final AbsoluteDate minDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(1);
        final TimeInterval interval = TimeInterval.of(minDate, maxDate);
        // WHEN
        final BoundedPVCoordinatesProvider boundedProvider = BoundedPVCoordinatesProvider.of(provider, interval);
        // THEN
        assertEquals(maxDate, boundedProvider.getMaxDate());
        assertEquals(minDate, boundedProvider.getMinDate());
        final Frame frame = FramesFactory.getGCRF();
        assertEquals(provider.getPosition(minDate, frame), boundedProvider.getPosition(minDate, frame));
        assertEquals(provider.getPVCoordinates(minDate, frame).getVelocity(),
                boundedProvider.getPVCoordinates(minDate, frame).getVelocity());
    }

    private static class TestProvider implements PVCoordinatesProvider {
        @Override
        public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
            return new TimeStampedPVCoordinates(date, new PVCoordinates(Vector3D.MINUS_I, Vector3D.MINUS_K));
        }
    }
}
