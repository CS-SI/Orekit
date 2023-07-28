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
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.*;

public class AngularAzElTest {

    /** Compare observed values and estimated values.
     *  Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
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
            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });

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
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
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
                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                    }
                }, measurement.getDimension(), propagator.getAttitudeProvider(), OrbitType.CARTESIAN,
                   PositionAngle.TRUE, 250.0, 4).value(state);

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
        Assertions.assertEquals(0.0, new Median().evaluate(AzerrorsP), 1.1e-10);
        Assertions.assertEquals(0.0, new Median().evaluate(AzerrorsV), 5.7e-5);

        // median errors on Elevation
        Assertions.assertEquals(0.0, new Median().evaluate(ElerrorsP), 3.5e-11);
        Assertions.assertEquals(0.0, new Median().evaluate(ElerrorsV), 1.4e-5);
    }

    /** Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivatives() {

        Context context = EstimationTestUtils.geoStationnaryContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
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
                                            return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[k];
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

    /**
     * Test the estimated values when the observed angular AzEl value is provided at TX (Transmit),
     * RX (Receive (default)), transit (bounce)
     */
    @Test
    public void testTimeTagSpecifications(){
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        SpacecraftState state = new SpacecraftState(context.initialOrbit);
        ObservableSatellite obsSat = new ObservableSatellite(0);

        for (GroundStation gs : context.getStations()) {

            gs.getPrimeMeridianOffsetDriver().setReferenceDate(state.getDate());
            gs.getPrimeMeridianDriftDriver().setReferenceDate(state.getDate());

            gs.getPolarOffsetXDriver().setReferenceDate(state.getDate());
            gs.getPolarDriftXDriver().setReferenceDate(state.getDate());

            gs.getPolarOffsetYDriver().setReferenceDate(state.getDate());
            gs.getPolarDriftYDriver().setReferenceDate(state.getDate());

            Transform gsToInertialTransform = gs.getOffsetToInertial(state.getFrame(), state.getDate());
            TimeStampedPVCoordinates stationPosition = gsToInertialTransform.transformPVCoordinates(new TimeStampedPVCoordinates(state.getDate(), Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

            double staticDistance = stationPosition.getPosition().distance(state.getPVCoordinates().getPosition());
            double staticTimeOfFlight = staticDistance / Constants.SPEED_OF_LIGHT;

            //Transmit (TX)
            AngularAzEl azElTx = new AngularAzEl(gs, state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat, TimeTagSpecificationType.TX);
            //Transmit Receive Apparent (TXRX)
            AngularAzEl azElTxRx = new AngularAzEl(gs, state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat, TimeTagSpecificationType.TXRX);
            //Receive (RX)
            AngularAzEl azElRx = new AngularAzEl(gs, state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat, TimeTagSpecificationType.RX);
            //Transit (Bounce)
            AngularAzEl azElT = new AngularAzEl(gs, state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat, TimeTagSpecificationType.TRANSIT);

            EstimatedMeasurement<AngularAzEl> estAzElTx = azElTx.estimate(0, 0, new SpacecraftState[]{state});
            EstimatedMeasurement<AngularAzEl> estAzElTxRx = azElTxRx.estimate(0, 0, new SpacecraftState[]{state});
            EstimatedMeasurement<AngularAzEl> estAzElRx = azElRx.estimate(0, 0, new SpacecraftState[]{state});
            EstimatedMeasurement<AngularAzEl> estAzElTransit = azElT.estimate(0, 0, new SpacecraftState[]{state});

            //Calculate Range calculated at transit and receive by shifting the state forward/backwards assuming time of flight = r/c
            SpacecraftState tx = state.shiftedBy(staticTimeOfFlight);
            SpacecraftState rx = state.shiftedBy(-staticTimeOfFlight);

            //Calculate the expected values, varying the estimated state + the time the ground station position is evaluated at.

            //state.getDate() == transmit (TX) time, state.getDate().shiftedBy(2*staticTimeOfFlight) == reception time
            double transitAzTxRx = MathUtils.normalizeAngle(gs.getBaseFrame().getAzimuth(tx.getPVCoordinates().getPosition(), tx.getFrame(), state.getDate().shiftedBy(2*staticTimeOfFlight)), 0.0);
            double transitElTxRx = MathUtils.normalizeAngle(gs.getBaseFrame().getElevation(tx.getPVCoordinates().getPosition(), tx.getFrame(), state.getDate().shiftedBy(2*staticTimeOfFlight)), 0.0);

            //state.getDate() == transmit (TX) time
            double transitAzTx = MathUtils.normalizeAngle(gs.getBaseFrame().getAzimuth(tx.getPVCoordinates().getPosition(), tx.getFrame(), state.getDate()), 0.0);
            double transitElTx = MathUtils.normalizeAngle(gs.getBaseFrame().getElevation(tx.getPVCoordinates().getPosition(), tx.getFrame(), state.getDate()), 0.0);

            //state.getDate() == reception time
            double transitAzRx = MathUtils.normalizeAngle(gs.getBaseFrame().getAzimuth(rx.getPVCoordinates().getPosition(), rx.getFrame(), state.getDate()), 0.0);
            double transitElRx = MathUtils.normalizeAngle(gs.getBaseFrame().getElevation(rx.getPVCoordinates().getPosition(), rx.getFrame(), state.getDate()), 0.0);

            //state.getDate() == transit time
            double transitAzT = MathUtils.normalizeAngle(gs.getBaseFrame().getAzimuth(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate()), 0.0);
            double transitElT = MathUtils.normalizeAngle(gs.getBaseFrame().getElevation(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate()), 0.0);


            //Static time of flight does not take into account motion during tof. Very small differences expected however
            //delta expected vs actual <<< difference between TX, RX and transit predictions by a few orders of magnitude.
            Assert.assertEquals("TX", transitAzTx, estAzElTx.getEstimatedValue()[0], 2e-8);
            Assert.assertEquals("TX", transitElTx, estAzElTx.getEstimatedValue()[1], 2e-8);

            Assert.assertEquals("TXRX", transitAzTxRx, estAzElTxRx.getEstimatedValue()[0], 2e-8);
            Assert.assertEquals("TXRX", transitElTxRx, estAzElTxRx.getEstimatedValue()[1], 2e-8);

            Assert.assertEquals("RX", transitAzRx, estAzElRx.getEstimatedValue()[0], 2e-8);
            Assert.assertEquals("RX", transitElRx, estAzElRx.getEstimatedValue()[1], 2e-8);

            Assert.assertEquals("Transit", transitAzT, estAzElTransit.getEstimatedValue()[0], 2e-8);
            Assert.assertEquals("Transit", transitElT, estAzElTransit.getEstimatedValue()[1], 2e-8);

            //Test providing pre corrected states + an arbitarily shifted case - since this should have no significant effect on the value.
            EstimatedMeasurement<AngularAzEl> estAzElTxShifted = azElTx.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(staticTimeOfFlight)});
            EstimatedMeasurement<AngularAzEl> estAzElRxShifted = azElRx.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(-staticTimeOfFlight)});
            EstimatedMeasurement<AngularAzEl> estAzElTransitShifted = azElT.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(0.1)});

            //tolerances are required since shifting the state forwards and backwards produces slight estimated value changes
            Assert.assertEquals("TX shifted", estAzElTxShifted.getEstimatedValue()[0], estAzElTx.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("TX shifted", estAzElTxShifted.getEstimatedValue()[1], estAzElTx.getEstimatedValue()[1], 1e-11);

            Assert.assertEquals("RX shifted", estAzElRxShifted.getEstimatedValue()[0], estAzElRx.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("RX shifted", estAzElRxShifted.getEstimatedValue()[1], estAzElRx.getEstimatedValue()[1], 1e-11);

            Assert.assertEquals("Transit shifted", estAzElTransitShifted.getEstimatedValue()[0], estAzElTransit.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("Transit shifted", estAzElTransitShifted.getEstimatedValue()[1], estAzElTransit.getEstimatedValue()[1], 1e-11);

            //Show the effect of the change in time tag specification is far greater than the test tolerance due to usage
            //of a static time of flight correction.
            Assert.assertTrue("Proof of difference - Azimuth", (Math.abs(transitAzRx - transitAzTx) > 1e-5));
            Assert.assertTrue("Proof of difference - Elevation", (Math.abs(transitAzRx - transitAzTx) > 1e-5));
        }
    }
}

