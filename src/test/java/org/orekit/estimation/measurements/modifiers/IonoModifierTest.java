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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
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
import org.orekit.estimation.measurements.EstimationModifier;
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
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class IonoModifierTest {

    /** ionospheric model. */
    private KlobucharIonoModel model;

    /** frequency [Hz]. */
    private double frequency;

    @BeforeEach
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06, 0},
                                       new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});
        // GPS L1 in HZ
        frequency = Frequency.G01.getMHzFrequency() * 1.0e6;
    }

    @Test
    public void testPhaseIonoModifier() {

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
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context, Frequency.G01, 0,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();


        final PhaseIonosphericDelayModifier modifier = new PhaseIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // add modifier
            phase.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Phase> existing : phase.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            //
            EstimatedMeasurement<Phase> eval = phase.estimate(0, 0,  new SpacecraftState[] { refstate });
            Assertions.assertEquals(evalNoMod.getStatus(), eval.getStatus());
            eval.setStatus(EstimatedMeasurement.Status.REJECTED);
            Assertions.assertEquals(EstimatedMeasurement.Status.REJECTED, eval.getStatus());
            eval.setStatus(evalNoMod.getStatus());

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1), new AbsoluteDate());
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = (eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]) * phase.getWavelength();
            Assertions.assertTrue(diffMeters < 0);
            Assertions.assertEquals(0.0, diffMeters, 30.0);

        }
    }

    @Test
    public void testPhaseEstimatedIonoModifier() {

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
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context, Frequency.G01, 0,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();


        final IonosphericModel mockModel = new MockIonosphericModel(12.0);
        mockModel.getParametersDrivers().get(0).setSelected(true);
        final PhaseIonosphericDelayModifier modifier = new PhaseIonosphericDelayModifier(mockModel, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(12, 17,new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // add modifier
            phase.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Phase> existing : phase.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            //
            EstimatedMeasurement<Phase> eval = phase.estimate(0, 0,  new SpacecraftState[] { refstate });
            Assertions.assertEquals(evalNoMod.getStatus(), eval.getStatus());
            eval.setStatus(EstimatedMeasurement.Status.REJECTED);
            Assertions.assertEquals(EstimatedMeasurement.Status.REJECTED, eval.getStatus());
            eval.setStatus(evalNoMod.getStatus());

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1), new AbsoluteDate());
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = (eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]) * phase.getWavelength();
            Assertions.assertEquals(-12.0, diffMeters, 0.1);

        }
    }

    @Test
    public void testRangeIonoModifier() {

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


        final RangeIonosphericDelayModifier modifier = new RangeIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurementBase<Range> evalNoMod = range.estimateWithoutDerivatives(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // add modifier
            range.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Range> existing : range.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            //
            EstimatedMeasurement<Range> eval = range.estimate(0, 0,  new SpacecraftState[] { refstate });
            Assertions.assertEquals(evalNoMod.getStatus(), eval.getStatus());
            eval.setStatus(EstimatedMeasurement.Status.REJECTED);
            Assertions.assertEquals(EstimatedMeasurement.Status.REJECTED, eval.getStatus());
            eval.setStatus(evalNoMod.getStatus());

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1), new AbsoluteDate());
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffMeters, 30.0);

        }
    }

    @Test
    public void testRangeRateIonoModifier() {

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

        final RangeRateIonosphericDelayModifier modifier = new RangeRateIonosphericDelayModifier(model, frequency, true);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            RangeRate rangeRate = (RangeRate) measurement;
            EstimatedMeasurementBase<RangeRate> evalNoMod = rangeRate.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refstate });

            // add modifier
            rangeRate.addModifier(modifier);

            //
            EstimatedMeasurementBase<RangeRate> eval = rangeRate.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refstate });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffMetersSec, 0.015);

        }
    }

    @Test
    public void testTurnAroundRangeIonoModifier() {

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


        final TurnAroundRangeIonosphericDelayModifier modifier = new TurnAroundRangeIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            TurnAroundRange turnAroundRange = (TurnAroundRange) measurement;
            EstimatedMeasurementBase<TurnAroundRange> evalNoMod = turnAroundRange.estimateWithoutDerivatives(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // Add modifier
            turnAroundRange.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<TurnAroundRange> existing : turnAroundRange.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            //
            EstimatedMeasurement<TurnAroundRange> eval = turnAroundRange.estimate(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(evalNoMod.getStatus(), eval.getStatus());
            eval.setStatus(EstimatedMeasurement.Status.REJECTED);
            Assertions.assertEquals(EstimatedMeasurement.Status.REJECTED, eval.getStatus());
            eval.setStatus(evalNoMod.getStatus());

            try {
                eval.getParameterDerivatives(new ParameterDriver("extra", 0, 1, -1, +1));
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitIllegalArgumentException oiae) {
                Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            }

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffMeters, 30.0);

        }
    }

    @Test
    public void testBistaticRangeIonoModifier() {

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

        final BistaticRangeIonosphericDelayModifier modifier =
                        new BistaticRangeIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            BistaticRange biRange = (BistaticRange) measurement;
            final SpacecraftState refstate = propagator.propagate(biRange.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<BistaticRange> evalNoMod = biRange.estimateWithoutDerivatives(0, 0,
                                                                                                   new SpacecraftState[] { refstate });

            // add modifier
            biRange.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<BistaticRange> eval = biRange.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refstate });

            final double diffMeters = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            Assertions.assertTrue(diffMeters < 12.0);
            Assertions.assertTrue(diffMeters >  4.0);
        }
    }

    @Test
    public void testBistaticRangeRateIonoModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // create perfect range rate measurements
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

        final BistaticRangeRateIonosphericDelayModifier modifier =
                        new BistaticRangeRateIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            BistaticRangeRate biRangeRate = (BistaticRangeRate) measurement;
            final SpacecraftState refstate = propagator.propagate(biRangeRate.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<BistaticRangeRate> evalNoMod = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                           new SpacecraftState[] { refstate });

            // add modifier
            biRangeRate.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurementBase<BistaticRangeRate> eval = biRangeRate.estimateWithoutDerivatives(0, 0,
                                                                                                      new SpacecraftState[] { refstate });

            final double diffMetersSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];
            // TODO: check threshold
            final double epsilon = 1e-5;
            Assertions.assertTrue(Precision.compareTo(diffMetersSec,  0.002, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffMetersSec, -0.010, epsilon) > 0);

        }
    }

    @Test
    public void testTDOAIonoModifier() {

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

        final TDOAIonosphericDelayModifier modifier =
                        new TDOAIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            TDOA tdoa = (TDOA) measurement;
            final SpacecraftState refState = propagator.propagate(tdoa.getDate());

            // Estimate without modifier
            EstimatedMeasurementBase<TDOA> evalNoMod = tdoa.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refState });

            // add modifier
            tdoa.addModifier(modifier);

            // Estimate with modifier
            EstimatedMeasurement<TDOA> eval = tdoa.estimate(0, 0, new SpacecraftState[] { refState });

            final double diffSec = eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0];

            // TODO: check threshold
            final double epsilon = 1.e-11;
            Assertions.assertTrue(Precision.compareTo(diffSec, 2.90e-9, epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(diffSec, 0.85e-9, epsilon) > 0);
        }
    }

    @Test
    public void testAngularIonoModifier() {

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
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();


        final AngularIonosphericDelayModifier modifier = new AngularIonosphericDelayModifier(model, frequency);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            AngularAzEl angular = (AngularAzEl) measurement;
            EstimatedMeasurementBase<AngularAzEl> evalNoMod = angular.estimateWithoutDerivatives(0, 0,
                                                                                                 new SpacecraftState[] { refstate });

            // add modifier
            angular.addModifier(modifier);
            //
            EstimatedMeasurementBase<AngularAzEl> eval = angular.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { refstate });

            final double diffAz = MathUtils.normalizeAngle(eval.getEstimatedValue()[0], evalNoMod.getEstimatedValue()[0]) - evalNoMod.getEstimatedValue()[0];
            final double diffEl = MathUtils.normalizeAngle(eval.getEstimatedValue()[1], evalNoMod.getEstimatedValue()[1]) - evalNoMod.getEstimatedValue()[1];
            // TODO: check threshold
            Assertions.assertEquals(0.0, diffAz, 5.0e-5);
            Assertions.assertEquals(0.0, diffEl, 5.0e-6);
        }
    }

    @Test
    public void testKlobucharIonoModel() {
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

        for (final ObservedMeasurement<?> measurement : measurements) {
            // parameter corresponding to station position offset
            final GroundStation   station = ((Range) measurement).getStation();
            final AbsoluteDate    date    = ((Range) measurement).getDate();
            final SpacecraftState state   = propagator.propagate(date);

            double delayMeters = model.pathDelay(state, station.getBaseFrame(), frequency, model.getParameters());

            final double epsilon = 1e-6;
            Assertions.assertTrue(Precision.compareTo(delayMeters, 15., epsilon) < 0);
            Assertions.assertTrue(Precision.compareTo(delayMeters, 0., epsilon) > 0);
        }

    }

    private class MockIonosphericModel implements IonosphericModel {

        /** Driver for the ionospheric delay.*/
        private final ParameterDriver ionoDelay;

        /** Constructor.
         * @param delay initial ionospheric delay
         */
        public MockIonosphericModel(final double delay) {
            ionoDelay = new ParameterDriver("ionospheric delay",
                                            delay, FastMath.scalb(1.0, 0), 0.0, Double.POSITIVE_INFINITY);
        }

        @Override
        public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                                final double frequency, double[] parameters) {
            return parameters[0];
        }

        @Override
        public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                           final double frequency, final  T[] parameters) {
            return parameters[0];
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.singletonList(ionoDelay);
        }

    }
}


