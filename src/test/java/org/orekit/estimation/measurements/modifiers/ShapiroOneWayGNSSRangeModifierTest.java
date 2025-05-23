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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.gnss.OneWayGNSSRange;
import org.orekit.estimation.measurements.gnss.OneWayGNSSRangeCreator;
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

public class ShapiroOneWayGNSSRangeModifierTest {

    @Test
    public void testShapiro() {
        doTestShapiro(0.000047764, 0.000086953, 0.000164659);
    }

    private void doTestShapiro(final double expectedMin, final double expectedMean, final double expectedMax) {

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
        List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new OneWayGNSSRangeCreator(ephemeris,
                                                                                          localClockOffset,
                                                                                          remoteClockOffset,
                                                                                          Vector3D.ZERO,
                                                                                          Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        final ShapiroOneWayGNSSRangeModifier modifier = new ShapiroOneWayGNSSRangeModifier(context.initialOrbit.getMu());
        final Propagator p3 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        DescriptiveStatistics stat = new DescriptiveStatistics();
        for (int i = 0; i < measurements.size(); ++i) {
            OneWayGNSSRange sr = (OneWayGNSSRange) measurements.get(i);
            SpacecraftState[] states = new SpacecraftState[] {
                p3.propagate(sr.getDate()),
                ephemeris.propagate(sr.getDate())
            };
            EstimatedMeasurementBase<OneWayGNSSRange> evalNoMod = sr.estimateWithoutDerivatives(states);

            // add modifier
            sr.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<OneWayGNSSRange> existing : sr.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            EstimatedMeasurementBase<OneWayGNSSRange> eval = sr.estimateWithoutDerivatives(states);

            stat.addValue(eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]);
            Assertions.assertEquals(1,
                                    eval.getAppliedEffects().entrySet().stream().
                                    filter(e -> e.getKey().getEffectName().equals("Shapiro")).count());

        }

        Assertions.assertEquals(expectedMin,  stat.getMin(),  1.0e-9);
        Assertions.assertEquals(expectedMean, stat.getMean(), 1.0e-9);
        Assertions.assertEquals(expectedMax,  stat.getMax(),  1.0e-9);

    }

}
