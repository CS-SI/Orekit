/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.TLEContext;
import org.orekit.estimation.TLEEstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.RangeRateTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.StateFunction;

public class TLERangeRateTest {

    @Test
    public void testStateDerivativesOneWay() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);
        for (final ObservedMeasurement<?> m : measurements) {
            Assert.assertFalse(((RangeRate) m).isTwoWay());
        }
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assert.assertEquals(2, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) {
                    return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                }
            }, 1, propagator.getAttitudeProvider(),
               OrbitType.CARTESIAN, PositionAngle.TRUE, 15.0, 3).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                 finiteDifferencesJacobian[i][j]));
                }
            }

        }
        Assert.assertEquals(0, maxRelativeError, 1.6e-08);

    }


    @Test
    public void testStateDerivativesTwoWays() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, true),
                                                               1.0, 3.0, 300.0);
        for (final ObservedMeasurement<?> m : measurements) {
            Assert.assertTrue(((RangeRate) m).isTwoWay());
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
            Assert.assertEquals(3, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) {
                    return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                }
            }, 1, propagator.getAttitudeProvider(),
               OrbitType.CARTESIAN, PositionAngle.TRUE, 15.0, 3).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                 finiteDifferencesJacobian[i][j]));
                }
            }

        }
        Assert.assertEquals(0, maxRelativeError, 8.3e-08);

    }

    @Test
    public void testParameterDerivativesOneWay() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, false),
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
                stationParameter.getZenithOffsetDriver()
            };
            for (int i = 0; i < 3; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assert.assertEquals(1, measurement.getDimension());
                Assert.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver) {
                                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i]);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assert.assertEquals(0, maxRelativeError, 7.9e-07);

    }

    @Test
    public void testParameterDerivativesTwoWays() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, true),
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
                stationParameter.getZenithOffsetDriver()
            };
            for (int i = 0; i < 3; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assert.assertEquals(1, measurement.getDimension());
                Assert.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver) {
                                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i]);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assert.assertEquals(0, maxRelativeError, 7.8e-07);

    }

    @Test
    public void testStateDerivativesWithModifier() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(SaastamoinenModel.getStandardModel(), true);
            ((RangeRate) measurement).addModifier(modifier);

            //
            //final AbsoluteDate date = measurement.getDate();
            final double          meanDelay = 1; // measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final double[][] jacobian = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) {
                    return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                }
            }, 1, propagator.getAttitudeProvider(),
               OrbitType.CARTESIAN, PositionAngle.TRUE, 15.0, 3).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                 finiteDifferencesJacobian[i][j]));
                }
            }

        }
        Assert.assertEquals(0, maxRelativeError, 9.5e-08);

    }


    @Test
    public void testParameterDerivativesWithModifier() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range rate measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final RangeRateTroposphericDelayModifier modifier = new RangeRateTroposphericDelayModifier(SaastamoinenModel.getStandardModel(), true);
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
                stationParameter.getEastOffsetDriver(),
                stationParameter.getNorthOffsetDriver(),
                stationParameter.getZenithOffsetDriver()
            };
            for (int i = 0; i < 3; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assert.assertEquals(1, measurement.getDimension());
                Assert.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver) {
                                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i]);
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
            }

        }
        Assert.assertEquals(0, maxRelativeError, 7.9e-07);

    }

}
