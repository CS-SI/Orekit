/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.TimeStampedPVCoordinates;

public class ShapiroInterSatelliteRangeModifierTest {

    @Test
    public void testShapiroOneWay() {
        doTestShapiro(false, 0.000047764, 0.000086953, 0.000164659);
    }

    @Test
    public void testShapiroTwoWay() {
        doTestShapiro(true, 0.000047764, 0.000086952, 0.000164656);
    }

    private void doTestShapiro(final boolean twoWay,
                               final double expectedMin, final double expectedMean, final double expectedMax) {
 
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
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
        closePropagator.setEphemerisMode();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = closePropagator.getGeneratedEphemeris();
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          Vector3D.ZERO,
                                                                                                          Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);
        if (!twoWay) {
            // convert default two way measurements to one way measurements
            final List<ObservedMeasurement<?>> converted = new ArrayList<>();
            for (final ObservedMeasurement<?> m : measurements) {
                final InterSatellitesRange sr = (InterSatellitesRange) m;
                converted.add(new InterSatellitesRange(sr.getSatellites().get(0), sr.getSatellites().get(1),
                                                       false, sr.getDate(),
                                                       sr.getObservedValue()[0],
                                                       sr.getTheoreticalStandardDeviation()[0],
                                                       sr.getBaseWeight()[0]));
            }
            measurements = converted;
        }

        final ShapiroInterSatelliteRangeModifier modifier = new ShapiroInterSatelliteRangeModifier(context.initialOrbit.getMu());
        final Propagator p3 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        DescriptiveStatistics stat = new DescriptiveStatistics();
        for (int i = 0; i < measurements.size(); ++i) {
            InterSatellitesRange sr = (InterSatellitesRange) measurements.get(i);
            SpacecraftState[] states = new SpacecraftState[] {
                p3.propagate(sr.getDate()),
                ephemeris.propagate(sr.getDate())
            };
            EstimatedMeasurement<InterSatellitesRange> evalNoMod = sr.estimate(0, 0, states);

            // add modifier
            sr.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<InterSatellitesRange> existing : sr.getModifiers()) {
                found = found || existing == modifier;
            }
            Assert.assertTrue(found);
            EstimatedMeasurement<InterSatellitesRange> eval = sr.estimate(0, 0, states);

            stat.addValue(eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]);

        }

        Assert.assertEquals(expectedMin,  stat.getMin(),  1.0e-9);
        Assert.assertEquals(expectedMean, stat.getMean(), 1.0e-9);
        Assert.assertEquals(expectedMax,  stat.getMax(),  1.0e-9);

    }

}


