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
import java.util.Map;

import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.BistaticRange;
import org.orekit.estimation.measurements.BistaticRangeMeasurementCreator;
import org.orekit.estimation.measurements.BistaticRangeRate;
import org.orekit.estimation.measurements.BistaticRangeRateMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.estimation.measurements.TDOAMeasurementCreator;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.estimation.measurements.TurnAroundRangeMeasurementCreator;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.estimation.measurements.gnss.PhaseMeasurementCreator;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class TropoModifierTest {

    @BeforeEach
    public void setUp() throws Exception {

    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    public void testRangeTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurementBase<Range> evalNoMod = range.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });


            // add modifier
            range.addModifier(modifier);
            EstimatedMeasurement<Range> eval = range.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testRangeEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurementBase<Range> evalNoMod = range.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final GroundStation stationParameter = ((Range) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(tropoModel);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            range.addModifier(modifier);
            EstimatedMeasurementBase<Range> eval = range.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testPhaseTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        final int    ambiguity         = 1234;
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final PhaseTroposphericDelayModifier modifier = new PhaseTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });


            // add modifier
            phase.addModifier(modifier);
            EstimatedMeasurementBase<Phase> eval = phase.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = (eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]) * phase.getWavelength();

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testPhaseEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final int    ambiguity         = 1234;
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context, Frequency.G01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });


            // add modifier
            final GroundStation stationParameter = phase.getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final PhaseTroposphericDelayModifier modifier = new PhaseTroposphericDelayModifier(tropoModel);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            phase.addModifier(modifier);
            EstimatedMeasurementBase<Phase> eval = phase.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = (eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]) * phase.getWavelength();

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testTurnAroundRangeTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect turn-around measurements
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            final GroundStation    primaryStation = entry.getKey();
            final GroundStation    secondaryStation  = entry.getValue();
            primaryStation.getClockOffsetDriver().setSelected(true);
            primaryStation.getEastOffsetDriver().setSelected(true);
            primaryStation.getNorthOffsetDriver().setSelected(true);
            primaryStation.getZenithOffsetDriver().setSelected(true);
            secondaryStation.getClockOffsetDriver().setSelected(false);
            secondaryStation.getEastOffsetDriver().setSelected(true);
            secondaryStation.getNorthOffsetDriver().setSelected(true);
            secondaryStation.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TurnAroundRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final TurnAroundRangeTroposphericDelayModifier modifier = new TurnAroundRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            TurnAroundRange turnAroundRange = (TurnAroundRange) measurement;
            EstimatedMeasurementBase<TurnAroundRange> evalNoMod = turnAroundRange.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            turnAroundRange.addModifier(modifier);
            //
            EstimatedMeasurement<TurnAroundRange> eval = turnAroundRange.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMeters, 12., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMeters, 0., epsilon) > 0);
        }
    }

    @Test
    public void testBistaticRangeTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range measurements
        final GroundStation emitter = context.BRRstations.getKey();
        emitter.getClockOffsetDriver().setSelected(true);
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);
        final GroundStation receiver = context.BRRstations.getValue();
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final BistaticRangeTroposphericDelayModifier modifier =
                        new BistaticRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            BistaticRange biRange = (BistaticRange) measurement;
            final SpacecraftState refState = propagator.propagate(biRange.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<BistaticRange> evalNoMod = biRange.estimateWithoutDerivatives(0, 0,
                                                                                                   new SpacecraftState[] { refState });

            // add modifier
            biRange.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<BistaticRange> eval = biRange.estimateWithoutDerivatives(0, 0,
                                                                                              new SpacecraftState[] { refState });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            Assertions.assertTrue(diffMeters < 9.0);
            Assertions.assertTrue(diffMeters > 5.0);
        }
    }

    @Test
    public void testBistaticRangeRateTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range-rate measurements
        final GroundStation emitter = context.BRRstations.getKey();
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);
        final GroundStation receiver = context.BRRstations.getValue();
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeRateMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final BistaticRangeRateTroposphericDelayModifier modifier =
                        new BistaticRangeRateTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            BistaticRangeRate biRangeRate = (BistaticRangeRate) measurement;
            final SpacecraftState refState = propagator.propagate(biRangeRate.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<BistaticRangeRate> evalNoMod = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                           new SpacecraftState[] { refState });

            // add modifier
            biRangeRate.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<BistaticRangeRate> eval = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                      new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 5e-5;
            Assertions.assertTrue(Precision.compareTo(diffMetersSec,  0.005, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, -0.007, epsilon) > 0);
        }
    }

    @Test
    public void testBistaticRangeRateEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range-rate measurements
        final GroundStation emitter = context.BRRstations.getKey();
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);
        final GroundStation receiver = context.BRRstations.getValue();
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeRateMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            BistaticRangeRate biRangeRate = (BistaticRangeRate) measurement;
            final SpacecraftState refState = propagator.propagate(biRangeRate.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<BistaticRangeRate> evalNoMod = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                           new SpacecraftState[] { refState });

            // add modifier
            final NiellMappingFunctionModel mappingFunc = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel = new EstimatedTroposphericModel(mappingFunc, 5.0);
            final BistaticRangeRateTroposphericDelayModifier modifier =
                            new BistaticRangeRateTroposphericDelayModifier(tropoModel);

            final TopocentricFrame baseFrame = biRangeRate.getReceiverStation().getBaseFrame();
            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);

            biRangeRate.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<BistaticRangeRate> eval = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                      new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-4;
            Assertions.assertTrue(Precision.compareTo(diffMetersSec,  0.010, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, -0.014, epsilon) > 0);
        }
    }

    @Test
    public void testTDOATropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range measurements
        final GroundStation emitter = context.TDOAstations.getKey();
        emitter.getClockOffsetDriver().setSelected(true);
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);
        final GroundStation receiver = context.TDOAstations.getValue();
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TDOAMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final TDOATroposphericDelayModifier modifier =
                        new TDOATroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            TDOA tdoa = (TDOA) measurement;
            final SpacecraftState refState = propagator.propagate(tdoa.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<TDOA> evalNoMod = tdoa.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            tdoa.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<TDOA> eval = tdoa.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1.e-11;
            Assertions.assertTrue(Precision.compareTo(diffSec,  2.35e-9, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffSec, -1.05e-9, epsilon) > 0);
        }
    }

    @Test
    public void testTDOAEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range measurements
        final GroundStation emitter = context.TDOAstations.getKey();
        emitter.getClockOffsetDriver().setSelected(true);
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);
        final GroundStation receiver = context.TDOAstations.getValue();
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TDOAMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            TDOA tdoa = (TDOA) measurement;
            final SpacecraftState refState = propagator.propagate(tdoa.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<TDOA> evalNoMod = tdoa.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final NiellMappingFunctionModel mappingFunct = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel  = new EstimatedTroposphericModel(mappingFunct, 5.0);
            final TDOATroposphericDelayModifier modifier = new TDOATroposphericDelayModifier(tropoModel);

            final TopocentricFrame baseFrame      = tdoa.getPrimeStation().getBaseFrame();
            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);

            tdoa.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurement<TDOA> eval = tdoa.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 5.e-11;
            Assertions.assertTrue(Precision.compareTo(diffSec,  4.90e-9, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffSec, -2.20e-9, epsilon) > 0);
        }
    }

    @Test
    public void testRangeRateTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(SaastamoinenModel.getStandardModel(), false);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurementBase<RangeRate> evalNoMod = rangeRate.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurement<RangeRate> eval = rangeRate.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, 0.01, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, -0.01, epsilon) > 0);
        }
    }

    @Test
    public void testRangeRateEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurementBase<RangeRate> evalNoMod = rangeRate.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final GroundStation stationParameter = ((RangeRate) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(tropoModel, false);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurementBase<RangeRate> eval = rangeRate.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, 0.01, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, -0.01, epsilon) > 0);
        }
    }

    @Test
    public void testAngularTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        propagator.clearStepHandlers();

        final AngularTroposphericDelayModifier modifier = new AngularTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurementBase<AngularAzEl> evalNoMod = angular.estimateWithoutDerivatives(0, 0,
                                                                                                 new SpacecraftState[] { refState });

            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurementBase<AngularAzEl> eval = angular.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffAz, 5.0e-5);
            Assertions.assertEquals(0.0, diffEl, 5.0e-6);
        }
    }

    @Test
    public void testAngularEstimatedTropoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurementBase<AngularAzEl> evalNoMod = angular.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            final GroundStation stationParameter = ((AngularAzEl) measurement).getStation();
            final TopocentricFrame baseFrame = stationParameter.getBaseFrame();
            final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel();
            final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
            final AngularTroposphericDelayModifier modifier = new AngularTroposphericDelayModifier(tropoModel);

            final ParameterDriver parameterDriver = modifier.getParametersDrivers().get(0);
            parameterDriver.setSelected(true);
            parameterDriver.setName(baseFrame.getName() + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
            angular.addModifier(modifier);
            //
            EstimatedMeasurementBase<AngularAzEl> eval = angular.estimateWithoutDerivatives(0, 0,
                                                                                            new SpacecraftState[] { refState });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];

            Assertions.assertEquals(0.0, diffAz, 1.9e-5);
            Assertions.assertEquals(0.0, diffEl, 2.1e-6);
        }
    }

    @Test
    public void testAngularRadioRefractionModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        propagator.clearStepHandlers();



        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurementBase<AngularAzEl> evalNoMod = angular.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // get the altitude of the station (in kilometers)
            final double altitude = angular.getStation().getBaseFrame().getPoint().getAltitude() / 1000.;

            final AngularRadioRefractionModifier modifier = new AngularRadioRefractionModifier(new EarthITU453AtmosphereRefraction(altitude));
            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurementBase<AngularAzEl> eval = angular.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffEl, 1.0e-3);
        }
    }

}
