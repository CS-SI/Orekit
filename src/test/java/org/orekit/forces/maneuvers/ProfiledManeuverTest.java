/* Copyright 2022-2024 Romain Serra
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
package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.forces.maneuvers.propulsion.ProfileThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ThrustSegment;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.CartesianToleranceProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.Collections;
import java.util.List;

class ProfiledManeuverTest {

    @ParameterizedTest
    @ValueSource(doubles = {1e2, 5e3, 1e3, 2e3})
    void testPropagate(final double mass) {
        // GIVEN
        final double isp = Double.POSITIVE_INFINITY;
        final AbsoluteDate initialDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = buildOrbit(initialDate);
        final LofOffset lofOffset = new LofOffset(orbit.getFrame(), LOFType.TNW);
        final NumericalPropagator propagator = buildPropagator(orbit, mass, lofOffset);
        final TimeSpanMap<ThrustSegment> thrustSegmentTimeSpanMap = new TimeSpanMap<>(null);
        final double singleBurnDuration = 1e2;
        final AbsoluteDate firstFiringStart = initialDate.shiftedBy(10);
        final AbsoluteDate secondFiringStart = firstFiringStart.shiftedBy(1e3);
        final Vector3D thrustVector = Vector3D.PLUS_I.scalarMultiply(1e0);
        thrustSegmentTimeSpanMap.addValidBetween(new ConstantThrustSegment(thrustVector), firstFiringStart, firstFiringStart.shiftedBy(singleBurnDuration));
        thrustSegmentTimeSpanMap.addValidBetween(new ConstantThrustSegment(thrustVector), secondFiringStart, secondFiringStart.shiftedBy(singleBurnDuration));
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(thrustSegmentTimeSpanMap,
                isp, Control3DVectorCostType.TWO_NORM, "");
        propagator.addForceModel(new ProfiledManeuver(null, propulsionModel));
        final AbsoluteDate targetDate = initialDate.shiftedBy(1e4);
        // WHEN
        final SpacecraftState terminalState = propagator.propagate(targetDate);
        // THEN
        final NumericalPropagator otherPropagator = buildPropagator(orbit, mass, lofOffset);
        final double thrustMagnitude = thrustVector.getNorm();
        final Vector3D thrustDirection = thrustVector.normalize();
        otherPropagator.addForceModel(new ConstantThrustManeuver(firstFiringStart, singleBurnDuration, thrustMagnitude, isp, thrustDirection));
        otherPropagator.addForceModel(new ConstantThrustManeuver(secondFiringStart, singleBurnDuration, thrustMagnitude, isp, thrustDirection));
        final SpacecraftState expectedState = otherPropagator.propagate(targetDate);
        Assertions.assertEquals(expectedState.getDate(), terminalState.getDate());
        final Vector3D relativePosition = expectedState.getPosition().subtract(terminalState.getPosition());
        Assertions.assertEquals(0., relativePosition.getNorm(), 2e0);
    }

    private static Orbit buildOrbit(final AbsoluteDate date) {
        return new EquinoctialOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 1000e3, 0.01, 0.001,
                0.1, -0.2, 1., PositionAngleType.TRUE, FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
    }

    private static NumericalPropagator buildPropagator(final Orbit initialOrbit, final double initialMass,
                                                       final AttitudeProvider attitudeProvider) {
        final DormandPrince54IntegratorBuilder integratorBuilder = new DormandPrince54IntegratorBuilder(1e-3, 1e2,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e-4)));
        final NumericalPropagator propagator = new NumericalPropagator(integratorBuilder.buildIntegrator(initialOrbit, OrbitType.EQUINOCTIAL),
                attitudeProvider);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(new SpacecraftState(initialOrbit, initialMass));
        return propagator;
    }

    private static class ConstantThrustSegment implements ThrustSegment {

        private final Vector3D thrustVector;

        ConstantThrustSegment(final Vector3D thrustVector) {
            this.thrustVector = thrustVector;
        }

        @Override
        public Vector3D getThrustVector(AbsoluteDate date, double mass, double[] parameters) {
            return thrustVector;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(FieldAbsoluteDate<T> date, T mass, T[] parameters) {
            return new FieldVector3D<>(mass.getField(), getThrustVector(date.toAbsoluteDate(), mass.getReal(), new double[0]));
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }
}
