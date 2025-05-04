/* Copyright 2002-2025 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.numerical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ProfileThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.propulsion.SphericalConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ThrustVectorProvider;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.IntervalEventTrigger;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.forces.radiation.CylindricallyShadowedLightFluxModel;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.LightFluxModel;
import org.orekit.forces.radiation.RadiationPressureModel;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.ParameterDrivenDateIntervalDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedStateTransitionMatrixGeneratorTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

    @Test
    void testManeuverTriggerDateParameter() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN, null);
        final LofOffset lofOffset = new LofOffset(propagator.getFrame(), LOFType.TNW);
        propagator.setAttitudeProvider(lofOffset);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double duration = 100.;
        final double timeOfFlight = duration * 3;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final AbsoluteDate medianDate = epoch.shiftedBy(duration * 2);
        final BasicConstantThrustPropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(0.1, 10.,
                Vector3D.MINUS_I, "man");
        final AbsoluteDate startDate = medianDate.shiftedBy(-duration/2.);
        final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(startDate, duration, null, propulsionModel);
        maneuver.getParameterDriver("man" + ParameterDrivenDateIntervalDetector.START_SUFFIX).setSelected(true);
        propagator.addForceModel(maneuver);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        final double dP = 1e-1;
        final NumericalPropagator propagatorFiniteDifference1 = setupPropagator(propagator.getOrbitType(), lofOffset,
                startDate, duration, propulsionModel, -dP/2.);
        final SpacecraftState finiteDifferencesState1 = propagatorFiniteDifference1.propagate(targetDate);
        final NumericalPropagator propagatorFiniteDifference2 = setupPropagator(propagator.getOrbitType(), lofOffset,
                startDate, duration, propulsionModel, dP/2.);
        final SpacecraftState finiteDifferencesState2 = propagatorFiniteDifference2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(finiteDifferencesState1.getPVCoordinates(),
                finiteDifferencesState2.getPVCoordinates());
        final RealMatrix parameterJacobian = harvester.getParametersJacobian(state);
        compareWithFiniteDifferences(relativePV, dP, parameterJacobian, 1e-3);
    }

    private static NumericalPropagator setupPropagator(final OrbitType orbitType, final AttitudeProvider attitudeProvider,
                                                         final AbsoluteDate startDate, final double duration,
                                                         final BasicConstantThrustPropulsionModel propulsionModel,
                                                         final double dt) {
        final NumericalPropagator propagator = buildPropagator(orbitType, attitudeProvider);
        final AbsoluteDate shiftedStartDate = startDate.shiftedBy(dt);
        final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(shiftedStartDate, duration - dt, null, propulsionModel);
        propagator.addForceModel(maneuver);
        return propagator;
    }

    @Test
    void testManeuverPropulsionParameter() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double thrustMagnitude = 0.1;
        final Maneuver maneuver = getPermanentManeuver(thrustMagnitude);
        propagator.addForceModel(maneuver);
        final double timeOfFlight = 1e3;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        compareMassGradientWithFiniteDifferences(propagator.getInitialState(), maneuver, targetDate,
                harvester.getStateTransitionMatrix(state));
        final RealMatrix parameterJacobian = harvester.getParametersJacobian(state);
        compareParameterJacobianWithFiniteDifferences(propagator, thrustMagnitude, targetDate, parameterJacobian);
    }

    private static void compareParameterJacobianWithFiniteDifferences(final NumericalPropagator propagator,
                                                                      final double thrustMagnitude,
                                                                      final AbsoluteDate targetDate,
                                                                      final RealMatrix parameterJacobian) {
        final NumericalPropagator propagatorFiniteDifference1 = buildPropagator(propagator.getOrbitType(),
                propagator.getAttitudeProvider());
        final double dP = 1e-5;
        propagatorFiniteDifference1.addForceModel(getPermanentManeuver((thrustMagnitude - dP / 2.)));
        final SpacecraftState finiteDifferencesState1 = propagatorFiniteDifference1.propagate(targetDate);
        final NumericalPropagator propagatorFiniteDifference2 = buildPropagator(propagator.getOrbitType(),
                propagator.getAttitudeProvider());
        propagatorFiniteDifference2.addForceModel(getPermanentManeuver(thrustMagnitude + dP / 2.));
        final SpacecraftState finiteDifferencesState2 = propagatorFiniteDifference2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(finiteDifferencesState1.getPVCoordinates(),
                finiteDifferencesState2.getPVCoordinates());
        compareWithFiniteDifferences(relativePV, dP, parameterJacobian, 1e-3);
    }

    private static void compareWithFiniteDifferences(final PVCoordinates relativePV, final double dP,
                                                     final RealMatrix parameterJacobian, final double delta) {
        assertEquals(relativePV.getPosition().getX() / dP, parameterJacobian.getEntry(0, 0), delta);
        assertEquals(relativePV.getPosition().getY() / dP, parameterJacobian.getEntry(1, 0), delta);
        assertEquals(relativePV.getPosition().getZ() / dP, parameterJacobian.getEntry(2, 0), delta);
        assertEquals(relativePV.getVelocity().getX() / dP, parameterJacobian.getEntry(3, 0), delta * 1e-2);
        assertEquals(relativePV.getVelocity().getY() / dP, parameterJacobian.getEntry(4, 0), delta * 1e-2);
        assertEquals(relativePV.getVelocity().getZ() / dP, parameterJacobian.getEntry(5, 0), delta * 1e-2);
    }

    private static Maneuver getPermanentManeuver(final double thrustMagnitude) {
        final Vector3D thrustVector = new Vector3D(3, 2).scalarMultiply(thrustMagnitude);
        final double isp = 100.;
        final String maneuverName = "man";
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, maneuverName);
        propulsionModel.getParameterDriver(maneuverName + SphericalConstantThrustPropulsionModel.THRUST_MAGNITUDE).setSelected(true);
        return new Maneuver(null, new TestManeuverTrigger(), propulsionModel);
    }

    private static class TestManeuverTrigger implements ManeuverTriggers {

        @Override
        public boolean isFiring(AbsoluteDate date, double[] parameters) {
            return true;
        }

        @Override
        public <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters) {
            return true;
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }

    @Test
    void testStm7x7vs6x6Column() {
        // GIVEN
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final NumericalPropagator propagator = buildPropagator(orbitType);
        final String stmName = "stm";
        final MatricesHarvester harvester7x7 = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double timeOfFlight = 1e5;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final ForceModel force = new ThirdBodyAttraction(new AnalyticalSolarPositionProvider(DataContext.getDefault()),
                "sun", Constants.JPL_SSD_SUN_GM);
        force.getParametersDrivers().get(0).setSelected(true);
        propagator.addForceModel(force);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        final RealMatrix actualJacobian = harvester7x7.getParametersJacobian(state);
        // THEN
        final NumericalPropagator otherPropagator = buildPropagator(orbitType);
        otherPropagator.addForceModel(force);
        final MatricesHarvester harvester6x6 = otherPropagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(6), null);
        final SpacecraftState otherState = otherPropagator.propagate(targetDate);
        final RealMatrix expectedJacobian = harvester6x6.getParametersJacobian(otherState);
        assertArrayEquals(expectedJacobian.getColumn(0), Arrays.copyOfRange(actualJacobian.getColumn(0), 0, 6));
    }

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testStm7x7vs6x6J2(final OrbitType orbitType) {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(orbitType);
        final String stmName = "stm";
        final MatricesHarvester harvester7x7 = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double timeOfFlight = 1e5;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final ForceModel j2Perturbation = new J2OnlyPerturbation(GravityFieldFactory.getUnnormalizedProvider(2, 0),
                FramesFactory.getGTOD(true));
        propagator.addForceModel(j2Perturbation);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        final RealMatrix actualStm = harvester7x7.getStateTransitionMatrix(state);
        // THEN
        final NumericalPropagator otherPropagator = buildPropagator(orbitType);
        otherPropagator.addForceModel(j2Perturbation);
        final MatricesHarvester harvester6x6 = otherPropagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(6), null);
        final SpacecraftState otherState = otherPropagator.propagate(targetDate);
        final RealMatrix expectedStm = harvester6x6.getStateTransitionMatrix(otherState);
        for (int i = 0; i < 6; i++) {
            assertArrayEquals(expectedStm.getRow(i), Arrays.copyOfRange(actualStm.getRow(i), 0, 6));
        }
    }

    @Test
    void testParameterJacobian7x7vs6x6ProfileThrust() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN);
        final String stmName = "stm";
        final MatricesHarvester harvester7x7 = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double timeOfFlight = 1e3;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final double slopeThrust = 2e-2;
        final ThrustVectorProvider thrustVectorProvider = getThrustVector(epoch, slopeThrust);
        final ProfileThrustPropulsionModel propulsionModel = buildProfileModel(thrustVectorProvider);
        final AbsoluteDate startDate = epoch.shiftedBy(5e2);
        final double duration = 3e2;
        final Maneuver maneuver = new Maneuver(null, buildDatedBasedTriggers(startDate, duration, 0.), propulsionModel);
        final String parameterName = "triggers" + ParameterDrivenDateIntervalDetector.DURATION_SUFFIX;
        maneuver.getParameterDriver(parameterName).setSelected(true);
        propagator.addForceModel(maneuver);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        final RealMatrix actualJacobian = harvester7x7.getParametersJacobian(state);
        // THEN
        final NumericalPropagator otherPropagator = buildPropagator(propagator.getOrbitType(), propagator.getAttitudeProvider());
        otherPropagator.addForceModel(new Maneuver(null, buildDatedBasedTriggers(startDate, duration, 0.), propulsionModel));
        otherPropagator.getAllForceModels().get(0).getParameterDriver(parameterName).setSelected(true);
        final MatricesHarvester harvester6x6 = otherPropagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(6), null);
        final SpacecraftState otherState = otherPropagator.propagate(targetDate);
        final RealMatrix expectedJacobian = harvester6x6.getParametersJacobian(otherState);
        assertArrayEquals(expectedJacobian.getColumn(0), Arrays.copyOfRange(actualJacobian.getColumn(0), 0, 6), 0.1);
    }

    @Test
    void testManeuverTriggerDateParameterWithProfileThrust() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN, null);
        final LofOffset lofOffset = new LofOffset(propagator.getFrame(), LOFType.TNW);
        propagator.setAttitudeProvider(lofOffset);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double duration = 100.;
        final double timeOfFlight = duration * 3;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final AbsoluteDate startDate = epoch.shiftedBy(duration/2.);
        final double slopeThrust = 1e-3;
        final ThrustVectorProvider thrustVectorProvider = getThrustVector(epoch, slopeThrust);
        final ProfileThrustPropulsionModel propulsionModel = buildProfileModel(thrustVectorProvider);
        final Maneuver maneuver = new Maneuver(null, buildDatedBasedTriggers(startDate, duration, 0.), propulsionModel);
        maneuver.getParameterDriver("triggers" + ParameterDrivenDateIntervalDetector.STOP_SUFFIX).setSelected(true);
        propagator.addForceModel(maneuver);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        final double dT = 1e-1;
        final NumericalPropagator propagatorFiniteDifference1 = buildPropagator(propagator.getOrbitType(), lofOffset);
        final double shiftBackward = -dT /2.;
        propagatorFiniteDifference1.addForceModel(new Maneuver(null, buildDatedBasedTriggers(startDate,
                duration, shiftBackward), buildProfileModel(thrustVectorProvider)));
        final SpacecraftState finiteDifferencesState1 = propagatorFiniteDifference1.propagate(targetDate);
        final NumericalPropagator propagatorFiniteDifference2 = buildPropagator(propagator.getOrbitType(), lofOffset);
        final double shiftForward = dT /2.;
        propagatorFiniteDifference2.addForceModel(new Maneuver(null, buildDatedBasedTriggers(startDate,
                duration, shiftForward), buildProfileModel(thrustVectorProvider)));
        final SpacecraftState finiteDifferencesState2 = propagatorFiniteDifference2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(finiteDifferencesState1.getPVCoordinates(),
                finiteDifferencesState2.getPVCoordinates());
        final RealMatrix parameterJacobian = harvester.getParametersJacobian(state);
        compareWithFiniteDifferences(relativePV, dT, parameterJacobian, 1e-3);
    }

    private static DateBasedManeuverTriggers buildDatedBasedTriggers(final AbsoluteDate startDate,
                                                                     final double duration, final double shiftEnd) {
        return new DateBasedManeuverTriggers("triggers", startDate, duration + shiftEnd);
    }

    private static ProfileThrustPropulsionModel buildProfileModel(final ThrustVectorProvider thrustVectorProvider) {
        final TimeSpanMap<ThrustVectorProvider> providerTimeSpanMap = new TimeSpanMap<>(null);
        providerTimeSpanMap.addValidBetween(thrustVectorProvider, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        return new ProfileThrustPropulsionModel(providerTimeSpanMap, 10.,"man");
    }

    private static ThrustVectorProvider getThrustVector(final AbsoluteDate referenceDate, final double slope) {
        return new ThrustVectorProvider() {
            @Override
            public Vector3D getThrustVector(AbsoluteDate date, double mass) {
                final double factor = slope * FastMath.abs(date.durationFrom(referenceDate));
                return Vector3D.PLUS_I.scalarMultiply(factor);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(FieldAbsoluteDate<T> date, T mass) {
                return new FieldVector3D<>(mass.getField(), getThrustVector(date.toAbsoluteDate(), mass.getReal()));
            }
        };
    }

    private static NumericalPropagator buildPropagator(final OrbitType orbitType) {
        final NumericalPropagator propagator = buildPropagator(orbitType, null);
        propagator.setAttitudeProvider(new FrameAlignedProvider(propagator.getFrame()));
        return propagator;
    }

    private static NumericalPropagator buildPropagator(final OrbitType orbitType, final AttitudeProvider attitudeProvider) {
        final Orbit orbit = new EquinoctialOrbit(7e6, 0.001, 0.001, 1., 2., 3., PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final ODEIntegrator integrator = new DormandPrince54IntegratorBuilder(1e-3, 50., 1e-4).buildIntegrator(orbit, orbitType);
        final NumericalPropagator propagator = new NumericalPropagator(integrator, attitudeProvider);
        propagator.setOrbitType(orbitType);
        propagator.setResetAtEnd(false);
        propagator.setInitialState(new SpacecraftState(orbit));
        return propagator;
    }

    private static void compareMassGradientWithFiniteDifferences(final SpacecraftState state,
                                                                 final ForceModel forceModel,
                                                                 final AbsoluteDate targetDate,
                                                                 final RealMatrix stateTransitionMatrix) {
        final double dM = 1.;
        final NumericalPropagator propagator1 = buildPropagator(OrbitType.CARTESIAN);
        propagator1.resetInitialState(state.withMass(state.getMass() - dM/2.));
        propagator1.addForceModel(forceModel);
        final SpacecraftState propagated1 = propagator1.propagate(targetDate);
        final NumericalPropagator propagator2 = buildPropagator(OrbitType.CARTESIAN);
        propagator2.resetInitialState(state.withMass(state.getMass() + dM/2.));
        propagator2.addForceModel(forceModel);
        final SpacecraftState propagated2 = propagator2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(propagated1.getPVCoordinates(),
                propagated2.getPVCoordinates());
        assertEquals(relativePV.getPosition().getX() / dM, stateTransitionMatrix.getEntry(0, 6), 1e-3);
        assertEquals(relativePV.getPosition().getY() / dM, stateTransitionMatrix.getEntry(1, 6), 1e-3);
        assertEquals(relativePV.getPosition().getZ() / dM, stateTransitionMatrix.getEntry(2, 6), 1e-3);
        assertEquals(relativePV.getVelocity().getX() / dM, stateTransitionMatrix.getEntry(3, 6), 1e-5);
        assertEquals(relativePV.getVelocity().getY() / dM, stateTransitionMatrix.getEntry(4, 6), 1e-5);
        assertEquals(relativePV.getVelocity().getZ() / dM, stateTransitionMatrix.getEntry(5, 6), 1e-5);
    }

    @Test
    void testRadiationPressure() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final CylindricallyShadowedLightFluxModel shadowModel = new CylindricallyShadowedLightFluxModel(new AnalyticalSolarPositionProvider(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
        final double crossSection = 100.;
        final double coefficientValue = 1.6;
        final IsotropicRadiationSingleCoefficient singleCoefficient = new IsotropicRadiationSingleCoefficient(crossSection, coefficientValue);
        singleCoefficient.getRadiationParametersDrivers()
                .stream().filter(driver -> driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)).findFirst()
                .ifPresent(driver -> driver.setSelected(true));
        final RadiationPressureModel model = new RadiationPressureModel(shadowModel, singleCoefficient);
        propagator.addForceModel(model);
        final double timeOfFlight = 1e4;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
                // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        final RealMatrix jacobianMatrix = harvester.getParametersJacobian(state);
        // THEN
        compareRadiationCoefficientGradientWithFiniteDifferences(shadowModel, crossSection, coefficientValue,
                targetDate, jacobianMatrix);
    }

    private static void compareRadiationCoefficientGradientWithFiniteDifferences(
                                                                 final LightFluxModel fluxModel,
                                                                 final double crossSection, final double singleCoefficient,
                                                                 final AbsoluteDate targetDate,
                                                                 final RealMatrix jacobianMatrix) {
        final double dP = 0.01;
        final NumericalPropagator propagator1 = buildPropagator(OrbitType.CARTESIAN);
                propagator1.addForceModel(new RadiationPressureModel(fluxModel, new IsotropicRadiationSingleCoefficient(crossSection,
                singleCoefficient - dP /2)));
        final SpacecraftState propagated1 = propagator1.propagate(targetDate);
        final NumericalPropagator propagator2 = buildPropagator(propagator1.getOrbitType(), propagator1.getAttitudeProvider());
                propagator2.addForceModel(new RadiationPressureModel(fluxModel, new IsotropicRadiationSingleCoefficient(crossSection,
                singleCoefficient + dP /2)));
        final SpacecraftState propagated2 = propagator2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(propagated1.getPVCoordinates(),
                propagated2.getPVCoordinates());
        assertEquals(relativePV.getPosition().getX() / dP, jacobianMatrix.getEntry(0, 0), 5e-3);
        assertEquals(relativePV.getPosition().getY() / dP, jacobianMatrix.getEntry(1, 0), 5e-3);
        assertEquals(relativePV.getPosition().getZ() / dP, jacobianMatrix.getEntry(2, 0), 5e-3);
        assertEquals(relativePV.getVelocity().getX() / dP, jacobianMatrix.getEntry(3, 0), 1e-5);
        assertEquals(relativePV.getVelocity().getY() / dP, jacobianMatrix.getEntry(4, 0), 1e-5);
        assertEquals(relativePV.getVelocity().getZ() / dP, jacobianMatrix.getEntry(5, 0), 1e-5);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-2e4, 2e4})
    void testManeuverApside(final double timeOfFlight) {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double thrustMagnitude = 5e-3;
        final Vector3D thrustDirection = Vector3D.PLUS_I;
        final double isp = 10;
        final PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(thrustMagnitude, isp,
                thrustDirection, "");
        final EventDetectionSettings detectionSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final ManeuverTriggers triggers = new ApsideTriggers(detectionSettings);
        final Maneuver maneuver = new Maneuver(null, triggers, propulsionModel);
        propagator.addForceModel(maneuver);
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        final SpacecraftState initialState = propagator.getInitialState();
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        final RealMatrix stateTransitionMatrix = harvester.getStateTransitionMatrix(state);
        // THEN
        final double dR = 1.;
        final double dV = 1e-3;
        checkStmColumnWithFiniteDifferences(0, dR, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(0));
        checkStmColumnWithFiniteDifferences(1, dR, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(1));
        checkStmColumnWithFiniteDifferences(2, dR, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(2));
        checkStmColumnWithFiniteDifferences(3, dV, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(3));
        checkStmColumnWithFiniteDifferences(4, dV, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(4));
        checkStmColumnWithFiniteDifferences(5, dV, propagator, initialState, maneuver, targetDate, stateTransitionMatrix.getColumnVector(5));
    }

    private void checkStmColumnWithFiniteDifferences(final int index, final double dX, final NumericalPropagator propagator,
                                                     final SpacecraftState initialState, final Maneuver maneuver,
                                                     final AbsoluteDate targetDate, final RealVector stateTransitionMatrixColumn) {
        final double[] cartesianVector = new double[6];
        cartesianVector[index] = -dX/2.;
        final NumericalPropagator propagator1 = buildPropagator(propagator.getOrbitType(), propagator.getAttitudeProvider());
        propagator1.resetInitialState(modifyState(initialState, cartesianVector));
        propagator1.addForceModel(maneuver);
        final SpacecraftState propagated1 = propagator1.propagate(targetDate);
        final NumericalPropagator propagator2 = buildPropagator(propagator.getOrbitType(), propagator.getAttitudeProvider());
        cartesianVector[index] *= -1;
        propagator2.resetInitialState(modifyState(initialState, cartesianVector));
        propagator2.addForceModel(maneuver);
        final SpacecraftState propagated2 = propagator2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(propagated1.getPVCoordinates(), propagated2.getPVCoordinates());
        final double toleranceP = 1e0;
        final double toleranceV = 1e-3;
        assertEquals(relativePV.getPosition().getX() / dX, stateTransitionMatrixColumn.getEntry(0), toleranceP);
        assertEquals(relativePV.getPosition().getY() / dX, stateTransitionMatrixColumn.getEntry(1), toleranceP);
        assertEquals(relativePV.getPosition().getZ() / dX, stateTransitionMatrixColumn.getEntry(2), toleranceP);
        assertEquals(relativePV.getVelocity().getX() / dX, stateTransitionMatrixColumn.getEntry(3), toleranceV);
        assertEquals(relativePV.getVelocity().getY() / dX, stateTransitionMatrixColumn.getEntry(4), toleranceV);
        assertEquals(relativePV.getVelocity().getZ() / dX, stateTransitionMatrixColumn.getEntry(5), toleranceV);
    }

    private static SpacecraftState modifyState(final SpacecraftState baseState, final double[] cartesianShift) {
        final PVCoordinates pvCoordinates = baseState.getPVCoordinates();
        final PVCoordinates newPV = new PVCoordinates(pvCoordinates.getPosition().add(new Vector3D(Arrays.copyOfRange(cartesianShift, 0, 3))),
                pvCoordinates.getVelocity().add(new Vector3D(Arrays.copyOfRange(cartesianShift, 3, 6))));
        final CartesianOrbit orbit = new CartesianOrbit(newPV, baseState.getFrame(), baseState.getDate(),
                baseState.getOrbit().getMu());
        return new SpacecraftState(orbit, baseState.getAttitude()).withMass(baseState.getMass());
    }

    private static class ApsideTriggers extends IntervalEventTrigger<ApsideDetector> {

        protected ApsideTriggers(EventDetectionSettings detectionSettings) {
            super(new ApsideDetector(detectionSettings, new ContinueOnEvent()));
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

        private FieldApsideDetector buildFieldApsideDetector(Field field, EventDetector detector) {
            return new FieldApsideDetector<>(new FieldEventDetectionSettings<>(field, detector.getDetectionSettings()),
                    new FieldStopOnEvent());
        }

        @Override
        protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> D convertIntervalDetector(Field<S> field, ApsideDetector detector) {
            return (D) buildFieldApsideDetector(field, detector);
        }
    }
}

