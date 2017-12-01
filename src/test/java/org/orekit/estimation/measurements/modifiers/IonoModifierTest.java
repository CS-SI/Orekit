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
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.estimation.measurements.TurnAroundRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.AngularIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.TurnAroundRangeIonosphericDelayModifier;
import org.orekit.models.earth.KlobucharIonoModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class IonoModifierTest {

    /** ionospheric model. */
    private KlobucharIonoModel model;

    @Before
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06, 0},
                                       new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testRangeIonoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();


        final RangeIonosphericDelayModifier modifier = new RangeIonosphericDelayModifier(model);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurement<Range> evalNoMod = range.estimate(12, 17, new SpacecraftState[] { refstate });
            Assert.assertEquals(12, evalNoMod.getIteration());
            Assert.assertEquals(17, evalNoMod.getCount());

            // add modifier
            range.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Range> existing : range.getModifiers()) {
                found = found || existing == modifier;
            }
            Assert.assertTrue(found);
            //
            EstimatedMeasurement<Range> eval = range.estimate(0, 0,  new SpacecraftState[] { refstate });
            final double w = evalNoMod.getCurrentWeight()[0];
            Assert.assertEquals(w, eval.getCurrentWeight()[0], 1.0e-10);
            eval.setCurrentWeight(new double[] { w + 2 });
            Assert.assertEquals(w + 2, eval.getCurrentWeight()[0], 1.0e-10);

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1));
                Assert.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffMeters, 30.0);

        }
    }

    @Test
    public void testTurnAroundRangeIonoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect turn-around measurements
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            final GroundStation    masterStation = entry.getKey();
            final GroundStation    slaveStation  = entry.getValue();
            masterStation.getEastOffsetDriver().setSelected(true);
            masterStation.getNorthOffsetDriver().setSelected(true);
            masterStation.getZenithOffsetDriver().setSelected(true);
            slaveStation.getEastOffsetDriver().setSelected(true);
            slaveStation.getNorthOffsetDriver().setSelected(true);
            slaveStation.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TurnAroundRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();


        final TurnAroundRangeIonosphericDelayModifier modifier = new TurnAroundRangeIonosphericDelayModifier(model);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            TurnAroundRange turnAroundRange = (TurnAroundRange) measurement;
            EstimatedMeasurement<TurnAroundRange> evalNoMod = turnAroundRange.estimate(12, 17, new SpacecraftState[] { refstate });
            Assert.assertEquals(12, evalNoMod.getIteration());
            Assert.assertEquals(17, evalNoMod.getCount());

            // Add modifier
            turnAroundRange.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<TurnAroundRange> existing : turnAroundRange.getModifiers()) {
                found = found || existing == modifier;
            }
            Assert.assertTrue(found);
            //
            EstimatedMeasurement<TurnAroundRange> eval = turnAroundRange.estimate(12, 17, new SpacecraftState[] { refstate });
            final double w = evalNoMod.getCurrentWeight()[0];
            Assert.assertEquals(w, eval.getCurrentWeight()[0], 1.0e-10);
            eval.setCurrentWeight(new double[] { w + 2 });
            Assert.assertEquals(w + 2, eval.getCurrentWeight()[0], 1.0e-10);

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1));
                Assert.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffMeters, 30.0);

        }
    }

    @Test
    public void testRangeRateIonoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        final RangeRateIonosphericDelayModifier modifier = new RangeRateIonosphericDelayModifier(model, true);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurement<RangeRate> evalNoMod = rangeRate.estimate(0, 0, new SpacecraftState[] { refstate });

            // add modifier
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurement<RangeRate> eval = rangeRate.estimate(0, 0, new SpacecraftState[] { refstate });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffMetersSec, 0.016);

        }
    }

    @Test
    public void testAngularIonoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();


        final AngularIonosphericDelayModifier modifier = new AngularIonosphericDelayModifier(model);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurement<AngularAzEl> evalNoMod = angular.estimate(0, 0, new SpacecraftState[] { refstate });

            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<AngularAzEl> eval = angular.estimate(0, 0, new SpacecraftState[] { refstate });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffAz, 5.0e-5);
            Assert.assertEquals(0.0, diffEl, 5.0e-6);
        }
    }

    @Test
    public void testKlobucharIonoModel() throws OrekitException {
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final ObservedMeasurement<?> measurement : measurements) {
            // parameter corresponding to station position offset
            final GroundStation   station = ((Range) measurement).getStation();
            final AbsoluteDate    date    = ((Range) measurement).getDate();
            final SpacecraftState state   = propagator.propagate(date);

            final Vector3D position = state.getPVCoordinates().getPosition();

            //
            final GeodeticPoint geo = station.getBaseFrame().getPoint();

            // elevation
            final double elevation = station.getBaseFrame().getElevation(position,
                                                                         state.getFrame(),
                                                                         state.getDate());

            // elevation
            final double azimuth = station.getBaseFrame().getAzimuth(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

            double delayMeters = model.pathDelay(date, geo, elevation, azimuth);

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(delayMeters, 15., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(delayMeters, 0., epsilon) > 0);
        }

    }
}


