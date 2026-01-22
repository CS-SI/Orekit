package org.orekit.forces.radiation;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.dfp.DfpField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.BigReal;
import org.hipparchus.util.BigRealField;
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
        final SolarRadiationPressure radiationPressure = new SolarRadiationPressure(positionProvider,
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

    /**
     * Check for NaN in case discovered when running RadiationPressureModelTest.
     * This case causes numerical issues because the Sun is almost entirely
     * occulted. The remaining area is computed as 1 - (occulted area) which
     * causes catastrophic cancellation in this case. Computing the occulted
     * area is further problematic because it sums two quantities that are
     * almost the same magnitude, but with opposite signs. Even in extended
     * precision, changing nominally equivalent expressions can produce a
     * negative (infeasible) result. For example, computing an angle with acos
     * instead of atan2.
     */
    @Test
    public void testGetLightingRatioNan() {
        // setup
        final ConicallyShadowedLightFluxModel model = new ConicallyShadowedLightFluxModel(
                1.0205062355092827E17,
                6.957E8,
                null,
                6378136.3);
        final Vector3D position = new Vector3D(
                4634793.393351071,
                6979388.527107593,
                0.1946021760969457);
        final Vector3D occultedBodyPosition = new Vector3D(
                2.6535624002170452E10,
                -1.3275125037266681E11,
                -5.755404493076553E10);
        // value was computed with extended precision below.
        // Though I don't trust it, making some mathematically allowable
        // transformations gives different, even negative results.
        final double expected = 7.0542775362244865347040611195042e-21;

        // action
        double actual = model.getLightingRatio(position, occultedBodyPosition);

        // verify
        MatcherAssert.assertThat(actual, Matchers.greaterThan(0.0));
        MatcherAssert.assertThat(actual, Matchers.closeTo(expected, 1e-5));

        // now for the field version
        Field<Dfp> field = new DfpField(50);
        FieldVector3D<Dfp> fieldPosition = new FieldVector3D<>(field, position);
        FieldVector3D<Dfp> fieldOccultedBodyPosition =
                new FieldVector3D<>(field, occultedBodyPosition);
        Dfp fieldActual = model.getLightingRatio(
                fieldPosition,
                fieldOccultedBodyPosition);
        MatcherAssert.assertThat(
                fieldActual.toString(),
                fieldActual.greaterThan(field.getZero()),
                Matchers.is(true));
        MatcherAssert.assertThat(
                fieldActual.toString(),
                fieldActual.toDouble(),
                Matchers.closeTo(expected, 1e-25));
    }

}
