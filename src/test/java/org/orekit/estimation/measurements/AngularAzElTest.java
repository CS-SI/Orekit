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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
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

public class AngularAzElTest {

    /** Compare observed values and estimated values.
     *  Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);

        propagator.clearStepHandlers();

        // Prepare statistics for right-ascension/declination values difference
        final StreamingStatistics azDiffStat = new StreamingStatistics();
        final StreamingStatistics elDiffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);

            // Estimate the AZEL value
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            azDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
            elDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[1] - measurement.getObservedValue()[1]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, azDiffStat.getMean(), 6.9e-9);
        Assertions.assertEquals(0.0, azDiffStat.getStandardDeviation(), 7.2e-9);
        Assertions.assertEquals(0.0, elDiffStat.getMean(), 5.4e-9);
        Assertions.assertEquals(0.0, elDiffStat.getStandardDeviation(), 3.3e-9);

        // Test measurement type
        Assertions.assertEquals(AngularAzEl.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    /** Test the values of the state derivatives using a numerical.
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.geoStationnaryContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // create perfect azimuth-elevation measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);

        propagator.clearStepHandlers();

        // Compute measurements.
        double[] AzerrorsP = new double[3 * measurements.size()];
        double[] AzerrorsV = new double[3 * measurements.size()];
        double[] ElerrorsP = new double[3 * measurements.size()];
        double[] ElerrorsV = new double[3 * measurements.size()];
        int AzindexP = 0;
        int AzindexV = 0;
        int ElindexP = 0;
        int ElindexV = 0;

        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((AngularAzEl) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // angular would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);
            final Vector3D     stationP  = stationParameter.getOffsetToInertial(state.getFrame(), datemeas, false).transformPosition(Vector3D.ZERO);
            final double       meanDelay = AbstractMeasurement.signalTimeOfFlight(state.getPVCoordinates(), stationP, datemeas);

            final AbsoluteDate date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                               state     = propagator.propagate(date);
            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assertions.assertEquals(2, estimated.getParticipants().length);
            final double[][]   jacobian  = estimated.getStateDerivatives(0);

            // compute a reference value using finite differences
            final double[][] finiteDifferencesJacobian =
                Differentiation.differentiate(new StateFunction() {
                    public double[] value(final SpacecraftState state) {
                        return measurement.
                               estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                               getEstimatedValue();
                    }
                }, measurement.getDimension(), propagator.getAttitudeProvider(), OrbitType.CARTESIAN,
                   PositionAngleType.TRUE, 250.0, 4).value(state);

            Assertions.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assertions.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            final double smallest = FastMath.ulp((double) 1.0);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    double relativeError = FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                              finiteDifferencesJacobian[i][j]);

                    if ((FastMath.sqrt(finiteDifferencesJacobian[i][j]) < smallest) && (FastMath.sqrt(jacobian[i][j]) < smallest) ){
                        relativeError = 0.0;
                    }

                    if (j < 3) {
                        if (i == 0) {
                            AzerrorsP[AzindexP++] = relativeError;
                        } else {
                            ElerrorsP[ElindexP++] = relativeError;
                        }
                    } else {
                        if (i == 0) {
                            AzerrorsV[AzindexV++] = relativeError;
                        } else {
                            ElerrorsV[ElindexV++] = relativeError;
                        }
                    }
                }
            }
        }

        // median errors on Azimuth
        Assertions.assertEquals(0.0, new Median().evaluate(AzerrorsP), 1.2e-10);
        Assertions.assertEquals(0.0, new Median().evaluate(AzerrorsV), 6.1e-5);

        // median errors on Elevation
        Assertions.assertEquals(0.0, new Median().evaluate(ElerrorsP), 7.4e-11);
        Assertions.assertEquals(0.0, new Median().evaluate(ElerrorsV), 2.3e-5);
    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivatives() {

        Context context = EstimationTestUtils.geoStationnaryContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // create perfect azimuth-elevation measurements
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
                                                               0.25, 3.0, 600.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((AngularAzEl) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // angular would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate    datemeas  = measurement.getDate();
            final SpacecraftState stateini  = propagator.propagate(datemeas);
            final Vector3D        stationP  = stationParameter.getOffsetToInertial(stateini.getFrame(), datemeas, false).transformPosition(Vector3D.ZERO);
            final double          meanDelay = AbstractMeasurement.signalTimeOfFlight(stateini.getPVCoordinates(), stationP, datemeas);

            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                stationParameter.getEastOffsetDriver(),
                stationParameter.getNorthOffsetDriver(),
                stationParameter.getZenithOffsetDriver()
            };
            for (int i = 0; i < 3; ++i) {
                final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                Assertions.assertEquals(2, measurement.getDimension());
                Assertions.assertEquals(2, gradient.length);

                for (final int k : new int[] {0, 1}) {
                    final ParameterFunction dMkdP =
                                    Differentiation.differentiate(new ParameterFunction() {
                                        /** {@inheritDoc} */
                                        @Override
                                        public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                            return measurement.
                                                   estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                                   getEstimatedValue()[k];
                                        }
                                    }, 3, 50.0 * drivers[i].getScale());
                    final double ref = dMkdP.value(drivers[i], date);

                    if (ref > 1.e-12) {
                        Assertions.assertEquals(ref, gradient[k], 3e-10 * FastMath.abs(ref));
                    }
                }
            }
        }
    }
}

