/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.estimation.measurements.TurnAroundRangeMeasurementCreator;
import org.orekit.frames.LOFType;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

public class OnBoardAntennaTurnAroundRangeModifierTest {

    @Test
    public void testPreliminary() throws OrekitException {

        // this test does not check OnBoardAntennaTurnAroundRangeModifier at all,
        // it just checks TurnAroundRangeMeasurementCreator behaves as necessary for the other test
        // the *real* test is testEffect below
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect turn-around range measurements without antenna offset
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new TurnAroundRangeMeasurementCreator(context, Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect turn-around range measurements with antenna offset
        final double xOffset = -2.5;
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new TurnAroundRangeMeasurementCreator(context, new Vector3D(xOffset, 0, 0)),
                                                               1.0, 3.0, 300.0);

        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            TurnAroundRange sr = (TurnAroundRange) spacecraftCenteredMeasurements.get(i);
            TurnAroundRange ar = (TurnAroundRange) antennaCenteredMeasurements.get(i);
            Assert.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 2.0e-8);
            Assert.assertTrue(ar.getObservedValue()[0] - sr.getObservedValue()[0] >= 2.0 * xOffset);
            Assert.assertTrue(ar.getObservedValue()[0] - sr.getObservedValue()[0] <= 1.8 * xOffset);
        }
    }

    @Test
    public void testEffect() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect turn-around range measurements without antenna offset
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new TurnAroundRangeMeasurementCreator(context, Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect turn-around range measurements with antenna offset
        final Vector3D apc = new Vector3D(-2.5,  0,  0);
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new TurnAroundRangeMeasurementCreator(context, apc),
                                                               1.0, 3.0, 300.0);

        final Propagator p3 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);

        OnBoardAntennaTurnAroundRangeModifier modifier = new OnBoardAntennaTurnAroundRangeModifier(apc);
        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            TurnAroundRange sr = (TurnAroundRange) spacecraftCenteredMeasurements.get(i);
            sr.addModifier(modifier);
            EstimatedMeasurement<TurnAroundRange> estimated = sr.estimate(0, 0, new SpacecraftState[] { p3.propagate(sr.getDate()) });
            TurnAroundRange ar = (TurnAroundRange) antennaCenteredMeasurements.get(i);
            Assert.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 2.0e-8);
            Assert.assertEquals(ar.getObservedValue()[0], estimated.getEstimatedValue()[0], 5.0e-7);
        }

    }

}


