package org.orekit.forces.radiation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

class RadiationPressureModelTest {

    @Test
    void testAcceleration() {
        // GIVEN
        final SpacecraftState state = createState();
        final ComplexField field = ComplexField.getInstance();
        final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(field, state);
        final IsotropicRadiationSingleCoefficient radiationSingleCoefficient = new IsotropicRadiationSingleCoefficient(1., 2.);
        final LightFluxModel mockedFluxModel = Mockito.mock(LightFluxModel.class);
        final Vector3D fluxVector = new Vector3D(1., 2., 3.);
        Mockito.when(mockedFluxModel.getLightFluxVector(state)).thenReturn(fluxVector);
        Mockito.when(mockedFluxModel.getLightFluxVector(fieldState)).thenReturn(new FieldVector3D<>(field, fluxVector));
        final RadiationPressureModel forceModel = new RadiationPressureModel(mockedFluxModel,
                radiationSingleCoefficient);
        // WHEN
        final FieldVector3D<Complex> fieldAcceleration = forceModel.acceleration(fieldState, forceModel.getParameters(field));
        // THEN
        final Vector3D expectedAcceleration = forceModel.acceleration(state, forceModel.getParameters());
        assertEquals(expectedAcceleration, fieldAcceleration.toVector3D());
    }

    @Test
    void testDependsOnPositionOnlyTrue() {
        // GIVEN
        final IsotropicRadiationSingleCoefficient mockedIsotropicRadiationSingleCoefficient = Mockito.mock(IsotropicRadiationSingleCoefficient.class);
        final LightFluxModel mockedFluxModel = Mockito.mock(LightFluxModel.class);
        final RadiationPressureModel forceModel = new RadiationPressureModel(mockedFluxModel,
                mockedIsotropicRadiationSingleCoefficient);
        // WHEN
        final boolean dependsOnPositionOnly = forceModel.dependsOnPositionOnly();
        // THEN
        assertTrue(dependsOnPositionOnly);
    }

    @Test
    void testDependsOnPositionOnlyFalse() {
        // GIVEN
        final BoxAndSolarArraySpacecraft mockedBoxAndSolarArraySpacecraft = Mockito.mock(BoxAndSolarArraySpacecraft.class);
        final LightFluxModel mockedFluxModel = Mockito.mock(LightFluxModel.class);
        final RadiationPressureModel forceModel = new RadiationPressureModel(mockedFluxModel,
                mockedBoxAndSolarArraySpacecraft);
        // WHEN
        final boolean dependsOnPositionOnly = forceModel.dependsOnPositionOnly();
        // THEN
        assertFalse(dependsOnPositionOnly);
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final RadiationSensitive mockedRadiationSensitive = Mockito.mock(RadiationSensitive.class);
        final LightFluxModel mockedFluxModel = Mockito.mock(LightFluxModel.class);
        final List<EventDetector> eclipseDetectors = new ArrayList<>();
        eclipseDetectors.add(Mockito.mock(EventDetector.class));
        Mockito.when(mockedFluxModel.getEclipseConditionsDetector()).thenReturn(eclipseDetectors);
        final RadiationPressureModel forceModel = new RadiationPressureModel(mockedFluxModel,
                mockedRadiationSensitive);
        // WHEN
        final Stream<?> detectors = forceModel.getEventDetectors();
        // THEN
        assertEquals(eclipseDetectors.size(), detectors.toArray().length);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFieldEventDetectors() {
        // GIVEN
        final RadiationSensitive mockedRadiationSensitive = Mockito.mock(RadiationSensitive.class);
        final LightFluxModel mockedFluxModel = Mockito.mock(LightFluxModel.class);
        final List<FieldEventDetector<Complex>> eclipseDetectors = new ArrayList<>();
        eclipseDetectors.add(Mockito.mock(FieldEventDetector.class));
        final ComplexField field = ComplexField.getInstance();
        Mockito.when(mockedFluxModel.getFieldEclipseConditionsDetector(field)).thenReturn(eclipseDetectors);
        final RadiationPressureModel forceModel = new RadiationPressureModel(mockedFluxModel,
                mockedRadiationSensitive);
        // WHEN
        final Stream<?> detectors = forceModel.getFieldEventDetectors(field);
        // THEN
        assertEquals(eclipseDetectors.size(), detectors.toArray().length);
    }

    @Test
    void testPropagation() {
        // GIVEN
        Utils.setDataRoot("regular-data");
        final IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoefficient = new IsotropicRadiationSingleCoefficient(10., 0.5);
        final CylindricallyShadowedLightFluxModel lightFluxModel = new CylindricallyShadowedLightFluxModel(CelestialBodyFactory.getSun(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final RadiationPressureModel forceModel = new RadiationPressureModel(lightFluxModel,
                isotropicRadiationSingleCoefficient);
        final NumericalPropagator propagator = createPropagator();
        propagator.addForceModel(forceModel);
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        // WHEN
        final AbsoluteDate terminalDate = epoch.shiftedBy(Constants.JULIAN_DAY * 10.);
        final SpacecraftState propagateState = propagator.propagate(terminalDate);
        // THEN
        final SpacecraftState comparableState = computeComparableState(forceModel, terminalDate);
        final Vector3D relativePosition = comparableState.getPosition().subtract(propagateState.getPosition(comparableState.getFrame()));
        assertEquals(0., relativePosition.getNorm(), 1e-6);
    }

    private SpacecraftState computeComparableState(final RadiationPressureModel radiationPressureModel,
                                                   final AbsoluteDate terminalDate) {
        final NumericalPropagator propagator = createPropagator();
        final CylindricallyShadowedLightFluxModel lightFluxModel = (CylindricallyShadowedLightFluxModel) radiationPressureModel.getLightFluxModel();
        final SolarRadiationPressure solarRadiationPressure = new SolarRadiationPressure((ExtendedPVCoordinatesProvider) lightFluxModel.getOccultedBody(),
                new OneAxisEllipsoid(lightFluxModel.getOccultingBodyRadius(), 0., FramesFactory.getGTOD(false)),
                radiationPressureModel.getRadiationSensitive());
        propagator.addForceModel(solarRadiationPressure);
        return propagator.propagate(terminalDate);
    }

    private SpacecraftState createState() {
        return new SpacecraftState(createOrbit());
    }

    private Orbit createOrbit() {
        return new EquinoctialOrbit(42000e3, 0., 0., 0., 0., 0., PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

    private NumericalPropagator createPropagator() {
        final SpacecraftState initialState = createState();
        final Orbit initialOrbit = initialState.getOrbit();
        final DormandPrince54IntegratorBuilder integratorBuilder = new DormandPrince54IntegratorBuilder(1e-3, 1e2, 1e-3);
        final AbstractIntegrator integrator = integratorBuilder.buildIntegrator(initialOrbit, initialOrbit.getType());
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(initialOrbit.getType());
        propagator.setInitialState(initialState);
        return propagator;
    }

}
