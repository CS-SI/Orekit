/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.AngularTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

public class TropoModifierTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testRangeTropoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

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

        final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurement<Range> evalNoMod = range.estimate(0, 0, refstate);

            // add modifier
            range.addModifier(modifier);
            //
            EstimatedMeasurement<Range> eval = range.estimate(0, 0, refstate);

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testRangeRateTropoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

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

        final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(SaastamoinenModel.getStandardModel(), false);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurement<RangeRate> evalNoMod = rangeRate.estimate(0, 0, refstate);

            // add modifier
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurement<RangeRate> eval = rangeRate.estimate(0, 0, refstate);

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMetersSec, 0.01, epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMetersSec, -0.01, epsilon) > 0);
        }
    }

    @Test
    public void testAngularTropoModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect angular measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        final AngularTroposphericDelayModifier modifier = new AngularTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Angular angular = (Angular) measurement;
            EstimatedMeasurement<Angular> evalNoMod = angular.estimate(0, 0, refstate);

            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<Angular> eval = angular.estimate(0, 0, refstate);

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffAz, 5.0e-5);
            Assert.assertEquals(0.0, diffEl, 5.0e-6);
        }
    }

    @Test
    public void testAngularRadioRefractionModifier() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect angular measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();



        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Angular angular = (Angular) measurement;
            EstimatedMeasurement<Angular> evalNoMod = angular.estimate(0, 0, refstate);

            // get the altitude of the station (in kilometers)
            final double altitude = angular.getStation().getBaseFrame().getPoint().getAltitude() / 1000.;

            final AngularRadioRefractionModifier modifier = new AngularRadioRefractionModifier(new EarthITU453AtmosphereRefraction(altitude));
            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<Angular> eval = angular.estimate(0, 0, refstate);

            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffEl, 1.0e-3);
        }
    }
}


