package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;

import static org.mockito.Mockito.mock;

class AbstractSwitchingAttitudeProviderTest {

    @Test
    void testResetActiveProvider() {
        // GIVEN
        final TestAttitudesSequence attitudesSequence = new TestAttitudesSequence();
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        // WHEN
        attitudesSequence.resetActiveProvider(attitudeProvider);
        // THEN
        final TimeSpanMap<AttitudeProvider> timeSpanMap = attitudesSequence.getActivated();
        Assertions.assertEquals(1, timeSpanMap.getSpansNumber());
    }


    @Test
    void testGetAttitudeRotation() {
        // GIVEN
        final AttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final TestAttitudesSequence attitudesSequence = new TestAttitudesSequence();
        attitudesSequence.resetActiveProvider(attitudeProvider);
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinatesProvider mockPvCoordinatesProvider = mock(PVCoordinatesProvider.class);
        // WHEN
        final Rotation actualRotation = attitudesSequence.getAttitudeRotation(mockPvCoordinatesProvider, date, frame);
        // THEN
        final Attitude attitude = attitudesSequence.getAttitude(mockPvCoordinatesProvider, date, frame);
        final Rotation expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAttitudeRotationFieldTest() {
        // GIVEN
        final GradientField field = GradientField.getField(1);
        final AttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final TestAttitudesSequence attitudesSequence = new TestAttitudesSequence();
        attitudesSequence.resetActiveProvider(attitudeProvider);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldPVCoordinatesProvider<Gradient> pvCoordinatesProvider = mock(FieldPVCoordinatesProvider.class);
        final Frame mockFrame = mock(Frame.class);
        // WHEN
        final FieldRotation<Gradient> actualRotation = attitudesSequence.getAttitudeRotation(pvCoordinatesProvider, fieldDate, mockFrame);
        // THEN
        final FieldAttitude<Gradient> attitude = attitudesSequence.getAttitude(pvCoordinatesProvider, fieldDate, mockFrame);
        final FieldRotation<Gradient> expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    private static class TestAttitudesSequence extends AbstractSwitchingAttitudeProvider {}

    private static class TestAttitudeProvider implements AttitudeProvider {

        TestAttitudeProvider() {
            // nothing to do
        }

        @Override
        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new Attitude(date, frame, new AngularCoordinates());
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                                                                                FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldAttitude<>(date.getField(), new Attitude(date.toAbsoluteDate(), frame,
                    new AngularCoordinates()));
        }

    }
}
