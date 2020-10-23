/* Copyright 2002-2020 CS GROUP
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
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.estimation.measurements.gnss.PhaseMeasurementCreator;
import org.orekit.frames.LOFType;
import org.orekit.gnss.Frequency;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.Constants;

public class OnBoardAntennaPhaseModifierTest {

    @Test
    public void testPreliminary() {

        // this test does not check OnBoardAntennaRangeModifier at all,
        // it just checks RangeMeasurementCreator behaves as necessary for the other test
        // the *real* test is testEffect below
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect range measurements without antenna offset
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 0;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset,
                                                                                           Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements with antenna offset
        final double xOffset = -2.5;
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset,
                                                                                           new Vector3D(xOffset, 0, 0)),
                                                               1.0, 3.0, 300.0);

        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            Phase sr = (Phase) spacecraftCenteredMeasurements.get(i);
            Phase ar = (Phase) antennaCenteredMeasurements.get(i);
            double alphaMax = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / sr.getObservedValue()[0]);
            Assert.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 1.0e-8);
            Assert.assertTrue((ar.getObservedValue()[0] - sr.getObservedValue()[0]) * sr.getWavelength() >= xOffset);
            Assert.assertTrue((ar.getObservedValue()[0] - sr.getObservedValue()[0]) * sr.getWavelength() <= xOffset * FastMath.cos(alphaMax) * sr.getWavelength());
        }
    }

    @Test
    public void testEffect() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // create perfect range measurements without antenna offset
        final Propagator p1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 0;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> spacecraftCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p1,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset,
                                                                                           Vector3D.ZERO),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements with antenna offset
        final Vector3D apc = new Vector3D(-2.5,  0,  0);
        final Propagator p2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);
        final List<ObservedMeasurement<?>> antennaCenteredMeasurements =
                        EstimationTestUtils.createMeasurements(p2,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset,
                                                                                           apc),
                                                               1.0, 3.0, 300.0);

        final Propagator p3 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                   propagatorBuilder);

        OnBoardAntennaPhaseModifier modifier = new OnBoardAntennaPhaseModifier(apc);
        for (int i = 0; i < spacecraftCenteredMeasurements.size(); ++i) {
            Phase sr = (Phase) spacecraftCenteredMeasurements.get(i);
            sr.addModifier(modifier);
            EstimatedMeasurement<Phase> estimated = sr.estimate(0, 0, new SpacecraftState[] { p3.propagate(sr.getDate()) });
            Phase ar = (Phase) antennaCenteredMeasurements.get(i);
            Assert.assertEquals(0.0, sr.getDate().durationFrom(ar.getDate()), 1.0e-8);
            Assert.assertEquals(ar.getObservedValue()[0], estimated.getEstimatedValue()[0], 1.1e-5);
        }

    }

}
