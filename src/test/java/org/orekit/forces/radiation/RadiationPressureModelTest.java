package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DormandPrince54FieldIntegratorBuilder;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class RadiationPressureModelTest {

    @Test
    void testAcceleration() {
        // GIVEN
        final SpacecraftState state = createState(42000e3);
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
        Assertions.assertEquals(expectedAcceleration, fieldAcceleration.toVector3D());
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
        Assertions.assertTrue(dependsOnPositionOnly);
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
        Assertions.assertFalse(dependsOnPositionOnly);
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
        Assertions.assertEquals(eclipseDetectors.size(), detectors.toArray().length);
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
        Assertions.assertEquals(eclipseDetectors.size(), detectors.toArray().length);
    }

    @ParameterizedTest
    @ValueSource(doubles = {400e3, 500e3, 800e3, 1000e3, 2000e3})
    void testLeoPropagation(final double altitude) {
        // GIVEN
        Utils.setDataRoot("regular-data");
        final IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoefficient = new IsotropicRadiationSingleCoefficient(10., 1.6);
        final ConicallyShadowedLightFluxModel lightFluxModel = new ConicallyShadowedLightFluxModel(Constants.SUN_RADIUS,
                CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final RadiationPressureModel forceModel = new RadiationPressureModel(lightFluxModel,
                isotropicRadiationSingleCoefficient);
        final double radius = lightFluxModel.getOccultingBodyRadius() + altitude;
        final NumericalPropagator propagator = createPropagator(radius);
        propagator.addForceModel(forceModel);
        final Orbit orbit = propagator.getInitialState().getOrbit();
        final AbsoluteDate epoch = orbit.getDate();
        // WHEN
        final AbsoluteDate terminalDate = epoch.shiftedBy(orbit.getKeplerianPeriod() * 5);
        final SpacecraftState propagateState = propagator.propagate(terminalDate);
        // THEN
        final SpacecraftState comparableState = computeComparableState(forceModel, radius, terminalDate);
        final Vector3D relativePosition = comparableState.getPosition().subtract(propagateState.getPosition(comparableState.getFrame()));
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e0);
    }

    @Test
    void testLeoFieldPropagation() {
        // GIVEN
        Utils.setDataRoot("regular-data");
        final IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoefficient =
              new IsotropicRadiationSingleCoefficient(10., 1.6);
        final ConicallyShadowedLightFluxModel lightFluxModel = new ConicallyShadowedLightFluxModel(Constants.SUN_RADIUS,
                                                                                                   CelestialBodyFactory.getSun(),
                                                                                                   Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final RadiationPressureModel forceModel = new RadiationPressureModel(lightFluxModel,
                                                                             isotropicRadiationSingleCoefficient);
        final double                             radius          = lightFluxModel.getOccultingBodyRadius() + 700e3;
        final Binary64Field                      field           = Binary64Field.getInstance();
        final SpacecraftState                    initialState    = createState(radius);
        final FieldNumericalPropagator<Binary64> fieldPropagator =
              doBuildFieldPropagator(field, forceModel, initialState);
        final AbsoluteDate                       epoch           = initialState.getDate();
        final AbsoluteDate terminalDate =
              epoch.shiftedBy(initialState.getKeplerianPeriod() * 10);
        // WHEN
        final FieldSpacecraftState<Binary64> propagatedState =
              fieldPropagator.propagate(new FieldAbsoluteDate<>(field, terminalDate));
        // THEN
        final NumericalPropagator propagator = createPropagator(radius);
        propagator.setOrbitType(fieldPropagator.getOrbitType());
        propagator.setPositionAngleType(fieldPropagator.getPositionAngleType());
        propagator.addForceModel(forceModel);
        final SpacecraftState comparableState = propagator.propagate(terminalDate);
        final Vector3D relativePosition = comparableState.getPosition()
                                                         .subtract(propagatedState.getPosition(
                                                               comparableState.getFrame()).toVector3D());
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e-3);
    }

    private static <T extends CalculusFieldElement<T>> FieldNumericalPropagator<T>
    doBuildFieldPropagator(final Field<T> field,
                           final ForceModel forceModel,
                           final SpacecraftState initialState) {
        final FieldODEIntegratorBuilder<T> fieldIntegratoBuilder =
              new DormandPrince54FieldIntegratorBuilder<>(1e-3, 1e2, 1e-3);

        final OrbitType propagationType = OrbitType.EQUINOCTIAL;
        final FieldODEIntegrator<T> fieldIntegrator =
              fieldIntegratoBuilder.buildIntegrator(field, initialState.getOrbit(), propagationType);

        final FieldNumericalPropagator<T> fieldPropagator =
              new FieldNumericalPropagator<>(field, fieldIntegrator);

        fieldPropagator.addForceModel(forceModel);
        fieldPropagator.setOrbitType(propagationType);
        fieldPropagator.setInitialState(new FieldSpacecraftState<>(field, initialState));

        return fieldPropagator;
    }

    @Test
    void testGeoPropagation() {
        // GIVEN
        Utils.setDataRoot("regular-data");
        final IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoefficient = new IsotropicRadiationSingleCoefficient(10., 1.6);
        final CylindricallyShadowedLightFluxModel lightFluxModel = new CylindricallyShadowedLightFluxModel(CelestialBodyFactory.getSun(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final RadiationPressureModel forceModel = new RadiationPressureModel(lightFluxModel,
                isotropicRadiationSingleCoefficient);
        final double radius = 42000e3;
        final NumericalPropagator propagator = createPropagator(radius);
        propagator.addForceModel(forceModel);
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        // WHEN
        final AbsoluteDate terminalDate = epoch.shiftedBy(Constants.JULIAN_DAY * 10.);
        final SpacecraftState propagateState = propagator.propagate(terminalDate);
        // THEN
        final SpacecraftState comparableState = computeComparableState(forceModel, radius, terminalDate);
        final Vector3D relativePosition = comparableState.getPosition().subtract(propagateState.getPosition(comparableState.getFrame()));
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e-6);
    }

    private SpacecraftState computeComparableState(final RadiationPressureModel radiationPressureModel,
                                                   final double radius, final AbsoluteDate terminalDate) {
        final NumericalPropagator propagator = createPropagator(radius);
        final AbstractSolarLightFluxModel lightFluxModel = (AbstractSolarLightFluxModel) radiationPressureModel.getLightFluxModel();
        final SolarRadiationPressure solarRadiationPressure = new SolarRadiationPressure((ExtendedPVCoordinatesProvider) lightFluxModel.getOccultedBody(),
                new OneAxisEllipsoid(lightFluxModel.getOccultingBodyRadius(), 0., FramesFactory.getGTOD(false)),
                radiationPressureModel.getRadiationSensitive());
        propagator.addForceModel(solarRadiationPressure);
        return propagator.propagate(terminalDate);
    }

    private SpacecraftState createState(final double radius) {
        return new SpacecraftState(createOrbit(radius), 100);
    }

    private Orbit createOrbit(final double radius) {
        return new EquinoctialOrbit(radius, 0., 0., 0., 0., 0., PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

    private NumericalPropagator createPropagator(final double radius) {
        final SpacecraftState initialState = createState(radius);
        final Orbit initialOrbit = initialState.getOrbit();
        final ODEIntegratorBuilder integratorBuilder = new DormandPrince54IntegratorBuilder(1e-3, 1e2, 1e-3);
        final AbstractIntegrator integrator = integratorBuilder.buildIntegrator(initialOrbit, initialOrbit.getType());
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(initialOrbit.getType());
        propagator.setInitialState(initialState);
        return propagator;
    }

}
