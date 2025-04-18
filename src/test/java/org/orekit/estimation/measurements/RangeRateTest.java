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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.RangeRateTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.EstimatedModel;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.ModifiedSaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

public class RangeRateTest {

    /** Compare observed values and estimated values.
     *  Both are calculated with a different algorithm.
     *  One-way measurements.
     */
    @Test
    public void testValuesOneWay() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);

        propagator.clearStepHandlers();

        // Prepare statistics for values difference
        final StreamingStatistics diffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);

            // Estimate the AZEL value
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            diffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, diffStat.getMean(), 6.5e-8);
        Assertions.assertEquals(0.0, diffStat.getStandardDeviation(), 5.5e-8);

        // Test measurement type
        Assertions.assertEquals(RangeRate.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    /** Compare observed values and estimated values.
     *  Both are calculated with a different algorithm.
     *  Two-ways measurements.
     */
    @Test
    public void testValuesTwoWays() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, true, satClkDrift),
                                                               1.0, 3.0, 300.0);

        propagator.clearStepHandlers();

        // Prepare statistics for values difference
        final StreamingStatistics diffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);

            // Estimate the AZEL value
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            diffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, diffStat.getMean(), 6.5e-8);
        Assertions.assertEquals(0.0, diffStat.getStandardDeviation(), 5.5e-8);
    }

    /** Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     * One way measurements.
     */
    @Test
    public void testStateDerivativesOneWay() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        for (final ObservedMeasurement<?> m : measurements) {
            Assertions.assertFalse(((RangeRate) m).isTwoWay());
        }
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assertions.assertEquals(2, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(state1 ->
                                                      measurement.
                                                          estimateWithoutDerivatives(new SpacecraftState[] { state1 }).
                                                          getEstimatedValue(),
                                                  1, propagator.getAttitudeProvider(),
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
        Assertions.assertEquals(0, maxRelativeError, 8.1e-6);

    }

    /** Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     * Two-ways measurements.
     */
    @Test
    public void testStateDerivativesTwoWays() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, true, satClkDrift),
                                                               1.0, 3.0, 300.0);
        for (final ObservedMeasurement<?> m : measurements) {
            Assertions.assertTrue(((RangeRate) m).isTwoWay());
        }
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            //
            //final AbsoluteDate date = measurement.getDate();
            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);

            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assertions.assertEquals(3, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(state1 ->
                                                      measurement.
                                                          estimateWithoutDerivatives(new SpacecraftState[] { state1 }).
                                                          getEstimatedValue(),
                                                  1, propagator.getAttitudeProvider(),
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
        Assertions.assertEquals(0, maxRelativeError, 8.1e-6);

    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     * One-way measurements.
     */
    @Test
    public void testParameterDerivativesOneWay() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
        creator.getSatellite().getClockDriftDriver().setSelected(true);
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
            station.getClockDriftDriver().setSelected(true);
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
            final GroundStation stationParameter = ((RangeRate) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                stationParameter.getClockDriftDriver(),
                stationParameter.getEastOffsetDriver(),
                stationParameter.getNorthOffsetDriver(),
                stationParameter.getZenithOffsetDriver(),
                measurement.getSatellites().get(0).getClockDriftDriver()
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                        return measurement.
                                               estimateWithoutDerivatives(new SpacecraftState[] { state }).
                                               getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assertions.assertEquals(0, maxRelativeError, 1.2e-6);

    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     * Two-ways measurements.
     */
    @Test
    public void testParameterDerivativesTwoWays() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
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
                                                               creator,
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((RangeRate) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                stationParameter.getEastOffsetDriver(),
                stationParameter.getNorthOffsetDriver(),
                stationParameter.getZenithOffsetDriver(),
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                        return measurement.
                                               estimateWithoutDerivatives(new SpacecraftState[] { state }).
                                               getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assertions.assertEquals(0, maxRelativeError, 1.2e-6);

    }

    /** Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     * One-way measurements with modifiers (tropospheric corrections).
     */
    @Test
    public void testStateDerivativesWithModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final RangeRateTroposphericDelayModifier modifier =
                new RangeRateTroposphericDelayModifier(ModifiedSaastamoinenModel.getStandardModel(), true);
            ((RangeRate) measurement).addModifier(modifier);

            //
            //final AbsoluteDate date = measurement.getDate();
            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final double[][] jacobian = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate( state1 -> measurement.
                            estimate(0, 0, new SpacecraftState[] { state1 }).
                                                       getEstimatedValue(), 1, propagator.getAttitudeProvider(),
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
        Assertions.assertEquals(0, maxRelativeError, 1.5e-7);

    }

    /** Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     * One-way measurements with estimated modifiers (tropospheric corrections).
     */
    @Test
    public void testStateDerivativesWithEstimatedModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            // Add modifiers if test implies it
            final GlobalMappingFunctionModel mappingFunction = new GlobalMappingFunctionModel();
            final EstimatedModel             tropoModel      = new EstimatedModel(mappingFunction, 5.0);

            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(tropoModel, true);
            ((RangeRate) measurement).addModifier(modifier);

            //
            //final AbsoluteDate date = measurement.getDate();
            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final double[][] jacobian = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(state1 -> measurement.
                                                      estimate(0, 0, new SpacecraftState[] { state1 }).
                                                      getEstimatedValue(), 1, propagator.getAttitudeProvider(),
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
        Assertions.assertEquals(0, maxRelativeError, 3.4e-7);

    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     * One-way measurements with modifiers (tropospheric corrections).
     */
    @Test
    public void testParameterDerivativesWithModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
        creator.getSatellite().getClockDriftDriver().setSelected(true);
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
            station.getClockDriftDriver().setSelected(true);
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

            final RangeRateTroposphericDelayModifier modifier =
                new RangeRateTroposphericDelayModifier(ModifiedSaastamoinenModel.getStandardModel(), true);
            ((RangeRate) measurement).addModifier(modifier);

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((RangeRate) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                stationParameter.getClockDriftDriver(),
                stationParameter.getEastOffsetDriver(),
                stationParameter.getNorthOffsetDriver(),
                stationParameter.getZenithOffsetDriver(),
                measurement.getSatellites().get(0).getClockDriftDriver()
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                        return measurement.
                                               estimateWithoutDerivatives(new SpacecraftState[] { state }).
                                               getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assertions.assertEquals(0, maxRelativeError, 1.2e-6);

    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     * One-way measurements with estimated modifiers (tropospheric corrections).
     */
    @Test
    public void testParameterDerivativesWithEstimatedModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            // Add modifiers if test implies it
            final GlobalMappingFunctionModel mappingFunction = new GlobalMappingFunctionModel();
            final EstimatedModel             tropoModel     = new EstimatedModel(mappingFunction, 10.0);

            final List<ParameterDriver> parameters = tropoModel.getParametersDrivers();
            for (ParameterDriver driver : parameters) {
                driver.setSelected(true);
            }

            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(tropoModel, true);
            ((RangeRate) measurement).addModifier(modifier);

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);

            final ParameterDriver[] drivers = new ParameterDriver[] {
                parameters.get(0)
            };
            for (int i = 0; i < 1; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                        return measurement.
                                               estimateWithoutDerivatives(new SpacecraftState[] { state }).
                                               getEstimatedValue()[0];
                                    }
                                }, 3, 0.1 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assertions.assertEquals(0, maxRelativeError, 2.2e-7);

    }

}


