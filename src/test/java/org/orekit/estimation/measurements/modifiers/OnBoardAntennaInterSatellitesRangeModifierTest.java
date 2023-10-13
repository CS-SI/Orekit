/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OnBoardAntennaInterSatellitesRangeModifierTest {

    @Test
    public void testPreliminary() {

        // this test does not check OnBoardAntennaInterSatellitesRangeModifier at all,
        // it just checks InterSatellitesRangeMeasurementCreator behaves as necessary for the other test
        // the *real* test is testEffect below
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect inter-satellites range measurements without antenna offset
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset,
                                                                                                          Vector3D.ZERO,
                                                                                                          Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect inter-satellites range measurements with antenna offset
        final double xOffset1 = -2.5;
        final double yOffset2 =  0.8;
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset,
                                                                                                          new Vector3D(xOffset1, 0, 0),
                                                                                                          new Vector3D(0, yOffset2, 0)),
                                                               1.0, 3.0, 300.0);

        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            InterSatellitesRange sr = (InterSatellitesRange) spacecraftCenteredMeasurements.get(i);
            InterSatellitesRange ar = (InterSatellitesRange) antennaCenteredMeasurements.get(i);
            Assertions.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 2.0e-8);
            Assertions.assertTrue(ar.getObservedValue()[0] - sr.getObservedValue()[0] >= -1.0);
            Assertions.assertTrue(ar.getObservedValue()[0] - sr.getObservedValue()[0] <= -0.36);
        }
    }

    @Test
    public void testEffect() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect inter-satellites range measurements without antenna offset
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset,
                                                                                                          Vector3D.ZERO,
                                                                                                          Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect inter-satellites range measurements with antenna offset
        final Vector3D apc1 = new Vector3D(-2.5, 0.0, 0);
        final Vector3D apc2 = new Vector3D( 0.0, 0.8, 0);
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset,
                                                                                                          apc1, apc2),
                                                               1.0, 3.0, 300.0);

        final Propagator p3 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);

        OnBoardAntennaInterSatellitesRangeModifier modifier = new OnBoardAntennaInterSatellitesRangeModifier(apc1, apc2);
        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            InterSatellitesRange sr = (InterSatellitesRange) spacecraftCenteredMeasurements.get(i);
            sr.addModifier(modifier);
            EstimatedMeasurementBase<InterSatellitesRange> estimated = sr.estimateWithoutDerivatives(0, 0,
                                                                                                     new SpacecraftState[] {
                                                                                                         p3.propagate(sr.getDate()),
                                                                                                         ephemeris.propagate(sr.getDate())
                                                                                                     });
            InterSatellitesRange ar = (InterSatellitesRange) antennaCenteredMeasurements.get(i);
            Assertions.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 2.0e-8);
            Assertions.assertEquals(ar.getObservedValue()[0], estimated.getEstimatedValue()[0], 2.0e-5);
        }

    }

}


