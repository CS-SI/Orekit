package org.orekit.forces.radiation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;

import java.util.List;

class ConicallyShadowedLightFluxModelTest {

    @BeforeEach
    public void setUp() throws OrekitException {
        Utils.setDataRoot("potential/shm-format:regular-data");
    }

    @Test
    void testConstructor() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final ConicallyShadowedLightFluxModel fluxModel = new ConicallyShadowedLightFluxModel(1., 1.,
                positionProvider, 1.);
        // WHEN
        final List<EventDetector> detectors = fluxModel.getEclipseConditionsDetector();
        // THEN
        final EventDetectionSettings detectionSettings = ConicallyShadowedLightFluxModel.getDefaultEclipseDetectionSettings();
        Assertions.assertEquals(detectionSettings.getMaxIterationCount(), detectors.get(0).getMaxIterationCount());
        Assertions.assertEquals(detectionSettings.getThreshold(), detectors.get(0).getThreshold());
    }

    @Test
    void testGetEclipseConditionsDetector() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final ConicallyShadowedLightFluxModel fluxModel = new ConicallyShadowedLightFluxModel(1., positionProvider,
                1.);
        // WHEN
        final List<EventDetector> detectors = fluxModel.getEclipseConditionsDetector();
        // THEN
        Assertions.assertEquals(2, detectors.size());
        for (EventDetector detector : detectors) {
            Assertions.assertInstanceOf(ResetDerivativesOnEvent.class, detector.getHandler());
        }
    }

    @Test
    void testGetFieldEclipseConditionsDetector() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final ConicallyShadowedLightFluxModel fluxModel = new ConicallyShadowedLightFluxModel(1., positionProvider,
                1.);
        final ComplexField field = ComplexField.getInstance();
        // WHEN
        final List<FieldEventDetector<Complex>> fieldDetectors = fluxModel.getFieldEclipseConditionsDetector(field);
        // THEN
        final List<EventDetector> detectors = fluxModel.getEclipseConditionsDetector();
        Assertions.assertEquals(detectors.size(), fieldDetectors.size());
        for (int i = 0; i < detectors.size(); i++) {
            Assertions.assertInstanceOf(FieldResetDerivativesOnEvent.class, fieldDetectors.get(i).getHandler());
            Assertions.assertEquals(detectors.get(i).getThreshold(), fieldDetectors.get(i).getThreshold().getReal());
            Assertions.assertEquals(detectors.get(i).getMaxIterationCount(), fieldDetectors.get(i).getMaxIterationCount());
        }
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_J),
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1.));
        final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(field, state);
        fluxModel.init(fieldState, null);
        for (int i = 0; i < detectors.size(); i++) {
            Assertions.assertEquals(detectors.get(i).g(state), fieldDetectors.get(i).g(fieldState).getReal(), 1e-8);
            Assertions.assertEquals(detectors.get(i).getMaxCheckInterval().currentInterval(state, true),
                    fieldDetectors.get(i).getMaxCheckInterval().currentInterval(fieldState, true), 1e-14);
        }
    }

    @Test
    void testFieldGetLightingRatio() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final ConicallyShadowedLightFluxModel fluxModel = new ConicallyShadowedLightFluxModel(Constants.SUN_RADIUS, positionProvider,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final ComplexField field = ComplexField.getInstance();
        // WHEN & THEN
        for (int i = 0; i < 100000; i++) {
            final SpacecraftState state = new SpacecraftState(new CartesianOrbit(
                    new PVCoordinates(new Vector3D(fluxModel.getOccultingBodyRadius() + 500e3, 0., 1e3), new Vector3D(1e1, 7e3, 1e2)),
                    FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i * 3600.), Constants.EGM96_EARTH_MU));
            final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(field, state);
            final double expectedRatio = fluxModel.getLightingRatio(state);
            Assertions.assertEquals(expectedRatio, fluxModel.getLightingRatio(fieldState).getReal(), 1e-8);
        }
    }

    @Test
    void testGetLightingRatio() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final ConicallyShadowedLightFluxModel fluxModel = new ConicallyShadowedLightFluxModel(Constants.SUN_RADIUS, positionProvider,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final Frame frame = FramesFactory.getGCRF();
        final SolarRadiationPressure radiationPressure = new SolarRadiationPressure(getExtendedPVCoordinatesProvider(positionProvider),
                new OneAxisEllipsoid(fluxModel.getOccultingBodyRadius(), 0., FramesFactory.getGCRF()), null);
        // WHEN & THEN
        for (int i = 0; i < 10000; i++) {
            final SpacecraftState state = new SpacecraftState(new CartesianOrbit(
                    new PVCoordinates(new Vector3D(fluxModel.getOccultingBodyRadius() + 500e3, 0., 1e3), new Vector3D(1e1, 7e3, 1e2)),
                    frame, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i * 3600.), Constants.EGM96_EARTH_MU));
            final double expectedRatio = radiationPressure.getLightingRatio(state);
            Assertions.assertEquals(expectedRatio, fluxModel.getLightingRatio(state), 1e-6);
        }
    }

    private static ExtendedPVCoordinatesProvider getExtendedPVCoordinatesProvider(final ExtendedPositionProvider positionProvider) {
        return positionProvider::getPVCoordinates;
    }
}
