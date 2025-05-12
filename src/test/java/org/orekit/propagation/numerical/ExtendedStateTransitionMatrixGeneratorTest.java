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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.SphericalConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

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
    void testManeuverParameter() {
        // GIVEN
        final NumericalPropagator propagator = buildPropagator(OrbitType.CARTESIAN);
        final String stmName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, MatrixUtils.createRealIdentityMatrix(7), null);
        final double thrustMagnitude = 0.1;
        addManeuver(thrustMagnitude, propagator);
        final double timeOfFlight = 1e3;
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate targetDate = epoch.shiftedBy(timeOfFlight);
        // WHEN
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        final RealMatrix parameterJacobian = harvester.getParametersJacobian(state);
        final NumericalPropagator propagatorFiniteDifference1 = buildPropagator(propagator.getOrbitType());
        final double dP = 1e-5;
        addManeuver(thrustMagnitude - dP / 2., propagatorFiniteDifference1);
        final SpacecraftState finiteDifferencesState1 = propagatorFiniteDifference1.propagate(targetDate);
        final NumericalPropagator propagatorFiniteDifference2 = buildPropagator(propagator.getOrbitType());
        addManeuver(thrustMagnitude + dP / 2., propagatorFiniteDifference2);
        final SpacecraftState finiteDifferencesState2 = propagatorFiniteDifference2.propagate(targetDate);
        final PVCoordinates relativePV = new PVCoordinates(finiteDifferencesState1.getPVCoordinates(),
                finiteDifferencesState2.getPVCoordinates());
        assertEquals(relativePV.getPosition().getX() / dP, parameterJacobian.getEntry(0, 0), 1e-5);
        assertEquals(relativePV.getPosition().getY() / dP, parameterJacobian.getEntry(1, 0), 1e-3);
        assertEquals(relativePV.getPosition().getZ() / dP, parameterJacobian.getEntry(2, 0), 1e-3);
        assertEquals(relativePV.getVelocity().getX() / dP, parameterJacobian.getEntry(3, 0), 1e-5);
        assertEquals(relativePV.getVelocity().getY() / dP, parameterJacobian.getEntry(4, 0), 1e-5);
        assertEquals(relativePV.getVelocity().getZ() / dP, parameterJacobian.getEntry(5, 0), 1e-5);
    }

    private void addManeuver(final double thrustMagnitude, final NumericalPropagator propagator) {
        final Vector3D thrustVector = new Vector3D(3, 2).scalarMultiply(thrustMagnitude);
        final double isp = 100.;
        final String maneuverName = "man";
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, maneuverName);
        propulsionModel.getParameterDriver(maneuverName + SphericalConstantThrustPropulsionModel.THRUST_MAGNITUDE).setSelected(true);
        final Maneuver maneuver = new Maneuver(null, new TestManeuverTrigger(), propulsionModel);
        propagator.addForceModel(maneuver);
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

    private NumericalPropagator buildPropagator(final OrbitType orbitType) {
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(100);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(orbitType);
        propagator.setResetAtEnd(false);
        final Orbit orbit = new EquinoctialOrbit(7e6, 0.001, 0.001, 1., 2., 3., PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        propagator.setInitialState(new SpacecraftState(orbit));
        return propagator;
    }
}

