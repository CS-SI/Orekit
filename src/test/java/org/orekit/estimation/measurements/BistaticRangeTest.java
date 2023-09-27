/* Copyright 2002-2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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

import java.util.Arrays;
import java.util.List;

import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.BistaticRangeTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.StateFunction;

public class BistaticRangeTest {

    /**
     * Compare observed values and estimated values.
     * Both are calculated with a different algorithm.
     */
    @Test
    public void testValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        // Prepare statistics for values difference
        final StreamingStatistics diffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);

            // Estimate the measurement value
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            diffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, diffStat.getMean(), 1.59e-7);
        Assertions.assertEquals(0.0, diffStat.getStandardDeviation(), 1.3e-7);

        // Test measurement type
        Assertions.assertEquals(BistaticRange.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final AbsoluteDate    date  = measurement.getDate().shiftedBy(1);
            final SpacecraftState state = propagator.propagate(date);

            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assertions.assertEquals(3, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) {
                    return measurement.
                           estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                           getEstimatedValue();
                }
            }, 1, propagator.getAttitudeProvider(),
               OrbitType.CARTESIAN, PositionAngleType.TRUE, 15.0, 3).value(state);

            Assertions.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assertions.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                 finiteDifferencesJacobian[i][j]));
                }
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 2.7e-5);

    }

    /**
     * Test the values of the state derivatives, using a numerical
     * finite differences calculation as a reference, with modifiers (tropospheric corrections).
     */
    @Test
    public void testStateDerivativesWithModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double clockOffset = 4.8e-9;
        for (final GroundStation station : Arrays.asList(context.BRRstations.getKey(),
                                                         context.BRRstations.getValue())) {
            station.getClockOffsetDriver().setValue(clockOffset);
        }
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new BistaticRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final BistaticRangeTroposphericDelayModifier modifier = new BistaticRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            ((BistaticRange) measurement).addModifier(modifier);

            final AbsoluteDate    date  = measurement.getDate().shiftedBy(1);
            final SpacecraftState state = propagator.propagate(date);

            final double[][] jacobian = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) {
                    return measurement.
                           estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                           getEstimatedValue();
                }
            }, 1, propagator.getAttitudeProvider(),
               OrbitType.CARTESIAN, PositionAngleType.TRUE, 15.0, 3).value(state);

            Assertions.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assertions.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                 finiteDifferencesJacobian[i][j]));
                }
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 2.5e-5);

    }

    /**
     * Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     */
    @Test
    public void testParameterDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final BistaticRangeMeasurementCreator creator = new BistaticRangeMeasurementCreator(context);
        final double clockOffset = 4.8e-9;
        for (final GroundStation station : Arrays.asList(context.BRRstations.getKey(),
                                                         context.BRRstations.getValue())) {
            station.getClockOffsetDriver().setValue(clockOffset);
            station.getClockOffsetDriver().setSelected(true);
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               creator,
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation emitterParameter = ((BistaticRange) measurement).getEmitterStation();
            final GroundStation receiverParameter = ((BistaticRange) measurement).getReceiverStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate    date  = measurement.getDate().shiftedBy(0.05);
            final SpacecraftState state = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                emitterParameter.getEastOffsetDriver(),
                emitterParameter.getNorthOffsetDriver(),
                emitterParameter.getZenithOffsetDriver(),
                receiverParameter.getEastOffsetDriver(),
                receiverParameter.getNorthOffsetDriver(),
                receiverParameter.getZenithOffsetDriver(),
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                                        return measurement.
                                               estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                               getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 1.9e-7);

    }

    /**
     * Test the values of the parameters' derivatives, using a numerical
     * finite differences calculation as a reference, with modifiers (tropospheric corrections).
     */
    @Test
    public void testParameterDerivativesWithModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final GroundStation emitter = context.BRRstations.getKey();
        emitter.getEastOffsetDriver().setSelected(true);
        emitter.getNorthOffsetDriver().setSelected(true);
        emitter.getZenithOffsetDriver().setSelected(true);

        final double clockOffset = 4.8e-9;
        final GroundStation receiver = context.BRRstations.getValue();
        receiver.getClockOffsetDriver().setValue(clockOffset);
        receiver.getClockOffsetDriver().setSelected(true);
        receiver.getEastOffsetDriver().setSelected(true);
        receiver.getNorthOffsetDriver().setSelected(true);
        receiver.getZenithOffsetDriver().setSelected(true);

        final BistaticRangeMeasurementCreator creator = new BistaticRangeMeasurementCreator(context);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               creator,
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final BistaticRangeTroposphericDelayModifier modifier = new BistaticRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            ((BistaticRange) measurement).addModifier(modifier);

            // parameter corresponding to station position offset
            final GroundStation emitterParameter  = ((BistaticRange) measurement).getEmitterStation();
            final GroundStation receiverParameter = ((BistaticRange) measurement).getReceiverStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate    date  = measurement.getDate().shiftedBy(0.05);
            final SpacecraftState state = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                emitterParameter.getEastOffsetDriver(),
                emitterParameter.getNorthOffsetDriver(),
                emitterParameter.getZenithOffsetDriver(),
                receiverParameter.getClockOffsetDriver(),
                receiverParameter.getEastOffsetDriver(),
                receiverParameter.getNorthOffsetDriver(),
                receiverParameter.getZenithOffsetDriver(),
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 2.9e-6);

    }

}
