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

package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collection;
import java.util.List;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

public class NumericalPropagatorBuilderTest {

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testClearImpulseManeuvers() {
        // Given
        final ODEIntegratorBuilder integratorBuilder = new ClassicalRungeKuttaIntegratorBuilder(60);
        final Orbit orbit = getOrbit();
        final NumericalPropagatorBuilder builder =
                new NumericalPropagatorBuilder(orbit, integratorBuilder, PositionAngleType.MEAN, 1.0, Utils.defaultLaw());
        final ImpulseManeuver mockedManeuver = Mockito.mock(ImpulseManeuver.class);
        builder.addImpulseManeuver(mockedManeuver);
        // WHEN
        builder.clearImpulseManeuvers();
        // THEN
        final Propagator propagator = builder.buildPropagator();
        final Collection<EventDetector> detectors = propagator.getEventDetectors();
        Assertions.assertTrue(detectors.isEmpty());
    }

    @Test
    void testAddImpulseManeuver() {
        // Given
        final ODEIntegratorBuilder integratorBuilder = new ClassicalRungeKuttaIntegratorBuilder(60);
        final Orbit orbit = getOrbit();
        final NumericalPropagatorBuilder builder =
                new NumericalPropagatorBuilder(orbit, integratorBuilder, PositionAngleType.MEAN, 1.0, Utils.defaultLaw());
        final ImpulseManeuver mockedManeuver = Mockito.mock(ImpulseManeuver.class);
        // WHEN
        builder.addImpulseManeuver(mockedManeuver);
        // THEN
        final Propagator propagator = builder.buildPropagator();
        final Collection<EventDetector> detectors = propagator.getEventDetectors();
        Assertions.assertEquals(1, detectors.size());
        Assertions.assertEquals(mockedManeuver, detectors.toArray()[0]);
    }

    @Test
    void testClone() {

        // Given
        final ODEIntegratorBuilder integratorBuilder = new ClassicalRungeKuttaIntegratorBuilder(60);
        final Orbit orbit = getOrbit();

        final NumericalPropagatorBuilder builder =
                new NumericalPropagatorBuilder(orbit, integratorBuilder, PositionAngleType.MEAN, 1.0, Utils.defaultLaw());

        builder.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                GravityFieldFactory.getNormalizedProvider(2, 0)));

        // Select propagation parameters to test this point
        builder.getPropagationParametersDrivers().getDrivers().forEach(d -> d.setSelected(true));

        // When
        final NumericalPropagatorBuilder copyBuilder = builder.clone();

        // Then
        assertNumericalPropagatorBuilderIsACopy(builder, copyBuilder);
    }

    /** Test for issue #1741.
     * <p>This test checks that orbital drivers in cloned propagator builders
     * ain't at the same physical address, i.e. that they're not linked anymore.</p>
     */
    @Test
    void testIssue1741() {

        // Given
        final ODEIntegratorBuilder integratorBuilder = new ClassicalRungeKuttaIntegratorBuilder(60);
        final Orbit orbit = getOrbit();

        final NumericalPropagatorBuilder builder =
                        new NumericalPropagatorBuilder(orbit, integratorBuilder, PositionAngleType.MEAN, 1.0, Utils.defaultLaw());

        builder.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                    GravityFieldFactory.getNormalizedProvider(2, 0)));

        // When
        final PropagatorBuilder copyBuilder = builder.clone();

        // Change orbit of the copied builder
        final TimeStampedPVCoordinates modifiedPv = orbit.shiftedBy(3600.).getPVCoordinates();
        copyBuilder.resetOrbit(new CartesianOrbit(modifiedPv, orbit.getFrame(), orbit.getDate(), orbit.getMu()));


        // Then
        // Original builder should still have original orbit
        final PVCoordinates originalPv = orbit.getPVCoordinates();
        final PVCoordinates initialPv = builder.createInitialOrbit().getPVCoordinates();
        final double dP = originalPv.getPosition().distance(initialPv.getPosition());
        final double dV = originalPv.getVelocity().distance(initialPv.getVelocity());
        final double dA = originalPv.getAcceleration().distance(initialPv.getAcceleration());
        Assertions.assertEquals(0., dP, 0.);
        Assertions.assertEquals(0., dV, 0.);
        Assertions.assertEquals(0., dA, 0.);
    }

    private static Orbit getOrbit() {
        return new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(),
                new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);
    }

    private void assertNumericalPropagatorBuilderIsACopy(final NumericalPropagatorBuilder expected,
                                                         final NumericalPropagatorBuilder actual) {
        assertPropagatorBuilderIsACopy(expected, actual);

        // Assert force models
        final List<ForceModel> expectedForceModelList = expected.getAllForceModels();
        final List<ForceModel> actualForceModelList   = actual.getAllForceModels();
        Assertions.assertEquals(expectedForceModelList.size(), actualForceModelList.size());
        for (int i = 0; i < expectedForceModelList.size(); i++) {
            Assertions.assertEquals(expectedForceModelList.get(i).getClass(), actualForceModelList.get(i).getClass());
        }

        // Assert integrator builder
        Assertions.assertEquals(expected.getIntegratorBuilder().getClass(), actual.getIntegratorBuilder().getClass());

        // Assert mass
        Assertions.assertEquals(expected.getMass(), actual.getMass());
    }

}
