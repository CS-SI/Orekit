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

import java.util.List;
import java.util.Map;

import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.estimation.measurements.TurnAroundRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.AngularTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.TurnAroundRangeTroposphericDelayModifier;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class TropoModifierTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testRangeTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
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

            final SpacecraftState refState = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurement<Range> evalNoMod = range.estimate(0, 0, new SpacecraftState[] { refState });


            // add modifier
            range.addModifier(modifier);
            EstimatedMeasurement<Range> eval = range.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testRangeEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurement<Range> evalNoMod = range.estimate(0, 0, new SpacecraftState[] { refState });


            // add modifier
            final GroundStation stationParameter = ((Range) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final GeodeticPoint point = baseFrame.getPoint();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel(point.getLatitude());
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(tropoModel);
            
            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            range.addModifier(modifier);
            EstimatedMeasurement<Range> eval = range.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testTurnAroundRangeTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect turn-around measurements
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            final GroundStation    masterStation = entry.getKey();
            final GroundStation    slaveStation  = entry.getValue();
            masterStation.getClockOffsetDriver().setSelected(true);
            masterStation.getEastOffsetDriver().setSelected(true);
            masterStation.getNorthOffsetDriver().setSelected(true);
            masterStation.getZenithOffsetDriver().setSelected(true);
            slaveStation.getClockOffsetDriver().setSelected(false);
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

        final TurnAroundRangeTroposphericDelayModifier modifier = new TurnAroundRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            TurnAroundRange turnAroundRange = (TurnAroundRange) measurement;
            EstimatedMeasurement<TurnAroundRange> evalNoMod = turnAroundRange.estimate(0, 0, new SpacecraftState[] { refState });

            // add modifier
            turnAroundRange.addModifier(modifier);
            //
            EstimatedMeasurement<TurnAroundRange> eval = turnAroundRange.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testRangeRateTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
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

            final SpacecraftState refState = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurement<RangeRate> evalNoMod = rangeRate.estimate(0, 0, new SpacecraftState[] { refState });

            // add modifier
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurement<RangeRate> eval = rangeRate.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMetersSec, 0.01, epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMetersSec, -0.01, epsilon) > 0);
        }
    }

    @Test
    public void testRangeRateEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurement<RangeRate> evalNoMod = rangeRate.estimate(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final GroundStation stationParameter = ((RangeRate) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final GeodeticPoint point = baseFrame.getPoint();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel(point.getLatitude());
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(tropoModel, false);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurement<RangeRate> eval = rangeRate.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(diffMetersSec, 0.01, epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(diffMetersSec, -0.01, epsilon) > 0);
        }
    }

    @Test
    public void testAngularTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect angular measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
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

        final AngularTroposphericDelayModifier modifier = new AngularTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurement<AngularAzEl> evalNoMod = angular.estimate(0, 0, new SpacecraftState[] { refState });

            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<AngularAzEl> eval = angular.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffAz, 5.0e-5);
            Assert.assertEquals(0.0, diffEl, 5.0e-6);
        }
    }

    @Test
    public void testAngularEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurement<AngularAzEl> evalNoMod = angular.estimate(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final GroundStation stationParameter = ((AngularAzEl) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final GeodeticPoint point = baseFrame.getPoint();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel(point.getLatitude());
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final AngularTroposphericDelayModifier modifier = new AngularTroposphericDelayModifier(tropoModel);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<AngularAzEl> eval = angular.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];

            Assert.assertEquals(0.0, diffAz, 1.9e-5);
            Assert.assertEquals(0.0, diffEl, 2.1e-6);
        }
    }

    @Test
    public void testAngularRadioRefractionModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect angular measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
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



        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurement<AngularAzEl> evalNoMod = angular.estimate(0, 0, new SpacecraftState[] { refState });

            // get the altitude of the station (in kilometers)
            final double altitude = angular.getStation().getBaseFrame().getPoint().getAltitude() / 1000.;

            final AngularRadioRefractionModifier modifier = new AngularRadioRefractionModifier(new EarthITU453AtmosphereRefraction(altitude));
            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurement<AngularAzEl> eval = angular.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffEl, 1.0e-3);
        }
    }
}
