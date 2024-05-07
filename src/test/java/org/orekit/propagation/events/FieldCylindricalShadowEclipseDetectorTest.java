package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;

class FieldCylindricalShadowEclipseDetectorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testCreate() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final FieldCylindricalShadowEclipseDetector<Complex> eclipseDetector = new FieldCylindricalShadowEclipseDetector<>(sun,
                getComplexEarthRadius(), new FieldContinueOnEvent<>());
        final FieldAdaptableInterval<Complex> adaptableInterval = state -> 1;
        final Complex expectedThreshold = new Complex(0.1);
        final int expectedMaxIter = 10;
        // WHEN
        final FieldCylindricalShadowEclipseDetector<Complex> detector = eclipseDetector.create(adaptableInterval, expectedThreshold,
                expectedMaxIter, eclipseDetector.getHandler());
        // THEN
        Assertions.assertEquals(expectedMaxIter, detector.getMaxIterationCount());
        Assertions.assertEquals(expectedThreshold, detector.getThreshold());
        Assertions.assertEquals(adaptableInterval, detector.getMaxCheckInterval());
    }

    @Test
    void testG0Eclipse() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = new TestDirectionProvider();
        final FieldCylindricalShadowEclipseDetector<Complex> eclipseDetector = new FieldCylindricalShadowEclipseDetector<>(sun,
                getComplexEarthRadius(), new FieldContinueOnEvent<>());
        final FieldVector3D<Complex> position = new FieldVector3D<>(ComplexField.getInstance(),
                new Vector3D(1., eclipseDetector.getOccultingBodyRadius().getReal(), 0.));
        final FieldSpacecraftState<Complex> mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState).getReal();
        // THEN
        Assertions.assertEquals(0., g);
    }

    @Test
    void testGEclipse() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = new TestDirectionProvider();
        final FieldCylindricalShadowEclipseDetector<Complex> eclipseDetector = new FieldCylindricalShadowEclipseDetector<>(sun,
                getComplexEarthRadius(), new FieldContinueOnEvent<>());
        final FieldVector3D<Complex> position = new FieldVector3D<>(ComplexField.getInstance(), new Vector3D(1e7, 0, -1e2));
        final FieldSpacecraftState<Complex> mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState).getReal();
        // THEN
        Assertions.assertTrue(g < 0.);
    }

    @Test
    void testGNoEclipse() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = new TestDirectionProvider();
        final FieldCylindricalShadowEclipseDetector<Complex> eclipseDetector = new FieldCylindricalShadowEclipseDetector<>(sun,
                getComplexEarthRadius(), new FieldContinueOnEvent<>());
        final FieldVector3D<Complex> position = new FieldVector3D<>(ComplexField.getInstance(), new Vector3D(0., 1e4, 0.));
        final FieldSpacecraftState<Complex> mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState).getReal();
        // THEN
        Assertions.assertTrue(g > 0.);
    }

    @Test
    void testGNoEclipse2() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = new TestDirectionProvider();
        final FieldCylindricalShadowEclipseDetector<Complex> eclipseDetector = new FieldCylindricalShadowEclipseDetector<>(sun,
                getComplexEarthRadius(), new FieldContinueOnEvent<>());
        final FieldVector3D<Complex> position = new FieldVector3D<>(ComplexField.getInstance(),
                new Vector3D(-1e6, 1e3, 0.));
        final FieldSpacecraftState<Complex> mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState).getReal();
        // THEN
        Assertions.assertTrue(g > 0.);
    }

    @SuppressWarnings("unchecked")
    private FieldSpacecraftState<Complex> mockState(final FieldVector3D<Complex> position) {
        final FieldSpacecraftState<Complex> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(position.getX().getField()));
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        return mockedState;
    }

    private static Complex getComplexEarthRadius() {
        return new Complex(Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
    }

    private static class TestDirectionProvider implements ExtendedPVCoordinatesProvider {

        @Override
        public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(FieldAbsoluteDate<T> date, Frame frame) {
            return new TimeStampedFieldPVCoordinates<>(date.getField(), getPVCoordinates(date.toAbsoluteDate(), frame));
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
            return new TimeStampedPVCoordinates(date, new PVCoordinates(Vector3D.MINUS_I));
        }
    }

}
