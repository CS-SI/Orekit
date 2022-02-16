/* Copyright 2002-2022 CS GROUP
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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.*;

public class AngularRaDecTest {

    /** Test the values of radec measurements.
     *  Added after bug 473 was reported by John Grimes.
     */
    @Test
    public void testBug473OnValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularRaDecMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);

        propagator.clearStepHandlers();

        // Prepare statistics for right-ascension/declination values difference
        final StreamingStatistics raDiffStat  = new StreamingStatistics();
        final StreamingStatistics decDiffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);
            
            // Estimate the RADEC value
            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            
            // Store the difference between estimated and observed values in the stats
            raDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
            decDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[1] - measurement.getObservedValue()[1]));
        }

        // Mean and std errors check
        Assert.assertEquals(0.0, raDiffStat.getMean(), 6.9e-11);
        Assert.assertEquals(0.0, raDiffStat.getStandardDeviation(), 8.5e-11);
        
        Assert.assertEquals(0.0, decDiffStat.getMean(), 4.5e-11);
        Assert.assertEquals(0.0, decDiffStat.getStandardDeviation(), 3e-11);
    }
    
    /** Test the values of the state derivatives using a numerical.
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularRaDecMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);

        propagator.clearStepHandlers();

        // Compute measurements.
        double[] RaerrorsP = new double[3 * measurements.size()];
        double[] RaerrorsV = new double[3 * measurements.size()];
        double[] DecerrorsP = new double[3 * measurements.size()];
        double[] DecerrorsV = new double[3 * measurements.size()];
        int RaindexP = 0;
        int RaindexV = 0;
        int DecindexP = 0;
        int DecindexV = 0;

        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((AngularRaDec) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // angular would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);
            final Vector3D     stationP  = stationParameter.getOffsetToInertial(state.getFrame(), datemeas).transformPosition(Vector3D.ZERO);
            final double       meanDelay = AbstractMeasurement.signalTimeOfFlight(state.getPVCoordinates(), stationP, datemeas);

            final AbsoluteDate date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                               state     = propagator.propagate(date);
            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assert.assertEquals(2, estimated.getParticipants().length);
            final double[][]   jacobian  = estimated.getStateDerivatives(0);

            // compute a reference value using finite differences
            final double[][] finiteDifferencesJacobian =
                Differentiation.differentiate(new StateFunction() {
                    public double[] value(final SpacecraftState state) {
                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                    }
                }, measurement.getDimension(), propagator.getAttitudeProvider(), OrbitType.CARTESIAN,
                   PositionAngle.TRUE, 250.0, 4).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

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
                            RaerrorsP[RaindexP++] = relativeError;
                        } else {
                            DecerrorsP[DecindexP++] = relativeError;
                        }
                    } else {
                        if (i == 0) {
                            RaerrorsV[RaindexV++] = relativeError;
                        } else {
                            DecerrorsV[DecindexV++] = relativeError;
                        }
                    }
                }
            }
        }

        // median errors on right-ascension
        Assert.assertEquals(0.0, new Median().evaluate(RaerrorsP), 4.8e-11);
        Assert.assertEquals(0.0, new Median().evaluate(RaerrorsV), 2.2e-5);

        // median errors on declination
        Assert.assertEquals(0.0, new Median().evaluate(DecerrorsP), 1.5e-11);
        Assert.assertEquals(0.0, new Median().evaluate(DecerrorsV), 5.4e-6);
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

        // create perfect right-ascension/declination measurements
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
                                                               new AngularRaDecMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);
        propagator.clearStepHandlers();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((AngularRaDec) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // angular would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate    datemeas  = measurement.getDate();
            final SpacecraftState stateini  = propagator.propagate(datemeas);
            final Vector3D        stationP  = stationParameter.getOffsetToInertial(stateini.getFrame(), datemeas).transformPosition(Vector3D.ZERO);
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
                Assert.assertEquals(2, measurement.getDimension());
                Assert.assertEquals(2, gradient.length);

                for (final int k : new int[] {0, 1}) {
                    final ParameterFunction dMkdP =
                                    Differentiation.differentiate(new ParameterFunction() {
                                        /** {@inheritDoc} */
                                        @Override
                                        public double value(final ParameterDriver parameterDriver) {
                                            return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[k];
                                        }
                                    }, 3, 50.0 * drivers[i].getScale());
                    final double ref = dMkdP.value(drivers[i]);

                    if (ref > 1.e-12) {
                        Assert.assertEquals(ref, gradient[k], 3e-9 * FastMath.abs(ref));
                    }
                }
            }
        }
    }

    /**
     * Test the estimated values when the observed angular ra dec value is provided at TX (Transmit),
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

            AngularRaDec raDecTx = new AngularRaDec(gs, state.getFrame(), state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat,TimeTagSpecificationType.TX);
            AngularRaDec raDecRx = new AngularRaDec(gs, state.getFrame(), state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat,TimeTagSpecificationType.RX);
            AngularRaDec raDecT = new AngularRaDec(gs, state.getFrame(), state.getDate(), new double[]{0.0,0.0}, new double[]{0.0,0.0},new double[]{0.0,0.0},obsSat,TimeTagSpecificationType.TRANSIT);

            EstimatedMeasurement<AngularRaDec> estRaDecTx = raDecTx.estimate(0, 0, new SpacecraftState[]{state});
            EstimatedMeasurement<AngularRaDec> estRaDecRx = raDecRx.estimate(0, 0, new SpacecraftState[]{state});
            EstimatedMeasurement<AngularRaDec> estRaDecTransit = raDecT.estimate(0, 0, new SpacecraftState[]{state});

            //Calculate Range calculated at transit and receive by shifting the state forward/backwards assuming time of flight = r/c
            SpacecraftState tx = state.shiftedBy(staticTimeOfFlight);
            SpacecraftState rx = state.shiftedBy(-staticTimeOfFlight);

            double[] transitRaDecTx = calcRaDec(tx, stationPosition.shiftedBy(2*staticTimeOfFlight).getPosition(), tx.getFrame(), tx.getDate());
            double[] transitRaDecRx = calcRaDec(rx, stationPosition.getPosition(), tx.getFrame(), rx.getDate());
            double[] transitRaDecT = calcRaDec(state, stationPosition.getPosition(), tx.getFrame(), state.getDate());

            //Static time of flight does not take into account motion during tof. Very small differences expected however
            //delta expected vs actual <<< difference between TX, RX and transit predictions by a few orders of magnitude.

            Assert.assertEquals("TX", transitRaDecTx[0], estRaDecTx.getEstimatedValue()[0], 1e-9);
            Assert.assertEquals("TX", transitRaDecTx[1], estRaDecTx.getEstimatedValue()[1], 1e-9);

            Assert.assertEquals("RX", transitRaDecRx[0], estRaDecRx.getEstimatedValue()[0], 1e-9);
            Assert.assertEquals("RX", transitRaDecRx[1], estRaDecRx.getEstimatedValue()[1], 1e-9);

            Assert.assertEquals("Transit", transitRaDecT[0], estRaDecTransit.getEstimatedValue()[0], 1e-9);
            Assert.assertEquals("Transit", transitRaDecT[1], estRaDecTransit.getEstimatedValue()[1], 1e-9);

            //Test providing pre corrected states + an arbitarily shifted case - since this should have no significant effect on the value.
            EstimatedMeasurement<AngularRaDec> estRaDecTxShifted = raDecTx.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(staticTimeOfFlight)});
            EstimatedMeasurement<AngularRaDec> estRaDecRxShifted = raDecRx.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(-staticTimeOfFlight)});
            EstimatedMeasurement<AngularRaDec> estRaDecTransitShifted = raDecT.estimate(0, 0, new SpacecraftState[]{state.shiftedBy(0.1)});

            //tolerances are required since the initial downlink time calculate will fit numerically differently depending on start point.
            Assert.assertEquals("TX shifted", estRaDecTxShifted.getEstimatedValue()[0], estRaDecTx.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("TX shifted", estRaDecTxShifted.getEstimatedValue()[1], estRaDecTx.getEstimatedValue()[1], 1e-11);

            Assert.assertEquals("RX shifted", estRaDecRxShifted.getEstimatedValue()[0], estRaDecRx.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("RX shifted", estRaDecRxShifted.getEstimatedValue()[1], estRaDecRx.getEstimatedValue()[1], 1e-11);

            Assert.assertEquals("Transit shifted", estRaDecTransitShifted.getEstimatedValue()[0], estRaDecTransit.getEstimatedValue()[0], 1e-11);
            Assert.assertEquals("Transit shifted", estRaDecTransitShifted.getEstimatedValue()[1], estRaDecTransit.getEstimatedValue()[1], 1e-11);


            //Show the effect of the change in time tag specification is far greater than the test tolerance due to usage
            //of a static time of flight correction.
            Assert.assertTrue("Proof of difference - RA", (Math.abs(transitRaDecRx[0] - transitRaDecTx[0]) > 1e-5));
            Assert.assertTrue("Proof of difference - DEC", (Math.abs(transitRaDecRx[1] - transitRaDecTx[1]) > 1e-5));
        }
    }

    private static double[] calcRaDec(SpacecraftState state, Vector3D station, Frame referenceFrame, AbsoluteDate date){
        Vector3D statePos = state.getPVCoordinates().getPosition();
        Vector3D staSatInertial = statePos.subtract(station);

        // Field transform from inertial to reference frame at station's reception date
        Transform inertialToReferenceDownlink =
                state.getFrame().getTransformTo(referenceFrame, date);

        // Station-satellite vector in reference frame
        Vector3D raDec = inertialToReferenceDownlink.transformPosition(staSatInertial);
        return new double[]{raDec.getAlpha(), raDec.getDelta()};
    }
}