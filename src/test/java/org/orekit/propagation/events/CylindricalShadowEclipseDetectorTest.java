package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.*;

class CylindricalShadowEclipseDetectorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testConstructor() {
        // GIVEN
        final EventDetectionSettings settings = EventDetectionSettings.getDefaultEventDetectionSettings();
        // WHEN
        final CylindricalShadowEclipseDetector detector = new CylindricalShadowEclipseDetector(Mockito.mock(PVCoordinatesProvider.class),
                1., settings, Mockito.mock(EventHandler.class));
        // THEN
        Assertions.assertEquals(settings.getMaxIterationCount(), detector.getDetectionSettings().getMaxIterationCount());
        Assertions.assertEquals(settings.getThreshold(), detector.getDetectionSettings().getThreshold());
    }

    @Test
    void testCreate() {
        // GIVEN
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();
        final CylindricalShadowEclipseDetector eclipseDetector = new CylindricalShadowEclipseDetector(sun,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, new ContinueOnEvent());
        final AdaptableInterval adaptableInterval = AdaptableInterval.of(1.);
        final double expectedThreshold = 0.1;
        final int expectedMaxIter = 10;
        // WHEN
        final CylindricalShadowEclipseDetector detector = eclipseDetector.create(new EventDetectionSettings(adaptableInterval, expectedThreshold,
                expectedMaxIter), eclipseDetector.getHandler());
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

    @Test
    void testDetections() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final RecordAndContinue recordAndContinue = new RecordAndContinue();
        final CylindricalShadowEclipseDetector cylindricalShadowEclipseDetector = new CylindricalShadowEclipseDetector(positionProvider,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, recordAndContinue);
        final Orbit initialOrbit = getOrbit();
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(cylindricalShadowEclipseDetector);
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(initialOrbit.getKeplerianPeriod() * 200);
        // WHEN
        propagator.propagate(targetDate);
        // THEN
        propagator.clearEventsDetectors();
        propagator.resetInitialState(new SpacecraftState(initialOrbit));
        final RecordAndContinue recordAndContinue2 = new RecordAndContinue();
        final EclipseDetector eclipseDetector = new EclipseDetector(positionProvider,
                Constants.SUN_RADIUS, new OneAxisEllipsoid(cylindricalShadowEclipseDetector.getOccultingBodyRadius(), 0., FramesFactory.getGTOD(false)));
        propagator.addEventDetector(eclipseDetector.withPenumbra().withHandler(recordAndContinue2));
        propagator.propagate(targetDate);
        Assertions.assertEquals(400, recordAndContinue2.getEvents().size());
        Assertions.assertEquals(recordAndContinue.getEvents().size(), recordAndContinue2.getEvents().size());
        for (int i = 0; i < recordAndContinue.getEvents().size(); i++) {
            Assertions.assertEquals(0., recordAndContinue.getEvents().get(i).getState().durationFrom(recordAndContinue2.getEvents().get(i).getState()),
                    10.);
        }
    }

    private static KeplerianOrbit getOrbit() {
        return new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 600e3, 0.001, 0.1, 2, 3, 4, PositionAngleType.ECCENTRIC,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

}
