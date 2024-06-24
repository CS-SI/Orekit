package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.*;

class CylindricalShadowEclipseDetectorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testCreate() {
        // GIVEN
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final AdaptableInterval adaptableInterval = AdaptableInterval.of(1.);
        final double expectedThreshold = 0.1;
        final int expectedMaxIter = 10;
        // WHEN
        final CylindricalShadowEclipseDetector detector = eclipseDetector.create(adaptableInterval, expectedThreshold,
                expectedMaxIter, eclipseDetector.getHandler());
        // THEN
        Assertions.assertEquals(expectedMaxIter, detector.getMaxIterationCount());
        Assertions.assertEquals(expectedThreshold, detector.getThreshold());
        Assertions.assertEquals(adaptableInterval, detector.getMaxCheckInterval());
    }

    @Test
    void testG0Eclipse() {
        // GIVEN
        final PVCoordinatesProvider sun = new TestDirectionProvider();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final Vector3D position = new Vector3D(1., eclipseDetector.getOccultingBodyRadius(), 0.);
        final SpacecraftState mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState);
        // THEN
        Assertions.assertEquals(0., g);
    }

    @Test
    void testGEclipse() {
        // GIVEN
        final PVCoordinatesProvider sun = new TestDirectionProvider();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final Vector3D position = new Vector3D(1e7, 0, -1e2);
        final SpacecraftState mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState);
        // THEN
        Assertions.assertTrue(g < 0.);
    }

    @Test
    void testGNoEclipse() {
        // GIVEN
        final PVCoordinatesProvider sun = new TestDirectionProvider();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final Vector3D position = new Vector3D(0., 1e4, 0.);
        final SpacecraftState mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState);
        // THEN
        Assertions.assertTrue(g > 0.);
    }

    @Test
    void testGNoEclipse2() {
        // GIVEN
        final PVCoordinatesProvider sun = new TestDirectionProvider();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final Vector3D position = new Vector3D(-1e6, 1e3, 0.);
        final SpacecraftState mockedState = mockState(position);
        // WHEN
        final double g = eclipseDetector.g(mockedState);
        // THEN
        Assertions.assertTrue(g > 0.);
    }

    private SpacecraftState mockState(final Vector3D position) {
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        return mockedState;
    }

    private static class TestDirectionProvider implements PVCoordinatesProvider {

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
            return new TimeStampedPVCoordinates(date, new PVCoordinates(Vector3D.MINUS_I));
        }
    }

}
