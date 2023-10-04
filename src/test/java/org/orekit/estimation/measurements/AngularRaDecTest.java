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

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.generation.AngularRaDecBuilder;
import org.orekit.frames.*;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

public class AngularRaDecTest {

    /** Test the values of radec measurements.
     *  Added after bug 473 was reported by John Grimes.
     */
    @Test
    public void testBug473OnValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
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
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(0, 0,
                                                                                                 new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            raDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
            decDiffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[1] - measurement.getObservedValue()[1]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, raDiffStat.getMean(), 6.9e-11);
        Assertions.assertEquals(0.0, raDiffStat.getStandardDeviation(), 8.5e-11);

        Assertions.assertEquals(0.0, decDiffStat.getMean(), 4.5e-11);
        Assertions.assertEquals(0.0, decDiffStat.getStandardDeviation(), 3e-11);
    }

    /** Test the values of the state derivatives using a numerical.
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
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
        Assertions.assertEquals(0.0, new Median().evaluate(RaerrorsP), 4.8e-11);
        Assertions.assertEquals(0.0, new Median().evaluate(RaerrorsV), 2.2e-5);

        // median errors on declination
        Assertions.assertEquals(0.0, new Median().evaluate(DecerrorsP), 1.9e-11);
        Assertions.assertEquals(0.0, new Median().evaluate(DecerrorsV), 9.0e-6);

        // Test measurement type
        Assertions.assertEquals(AngularRaDec.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
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
                        Assertions.assertEquals(ref, gradient[k], 3e-9 * FastMath.abs(ref));
                    }
                }
            }
        }
    }

    /** Test issue 1026 where RA-Dec built with a reference frame not Earth-centered may lead to completely wrong
     * values.
     */
    @Test
    public void testIssue1026() {

        //Context context = EstimationTestUtils.eccentricContext("regular-data/de431-ephemerides");
        Utils.setDataRoot("regular-data");

        final double[] pos = {Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 5e5, 1000., 0.};
        final double[] vel = {0., 10., 0.};
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(pos[0], pos[1], pos[2]),
                new Vector3D(vel[0], vel[1], vel[2]));
        final AbsoluteDate epoch = new AbsoluteDate(new DateComponents(2000, 1, 1), TimeScalesFactory.getUTC());
        final Frame gcrf = FramesFactory.getGCRF();
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, gcrf,
                epoch, Constants.EGM96_EARTH_MU);
        final SpacecraftState spacecraftState = new SpacecraftState(orbit);
        final OrekitStepInterpolator fakeInterpolator = new OrekitStepInterpolator() {
            public OrekitStepInterpolator restrictStep(SpacecraftState newPreviousState, SpacecraftState newCurrentState) { return null; }
            public boolean isPreviousStateInterpolated() { return false; }
            public boolean isForward() { return true; }
            public boolean isCurrentStateInterpolated() { return false; }
            public SpacecraftState getPreviousState() { return spacecraftState; }
            public SpacecraftState getInterpolatedState(AbsoluteDate date) { return spacecraftState; }
            public SpacecraftState getCurrentState() { return spacecraftState; }
        };

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.IERS2010_EARTH_EQUATORIAL_RADIUS,
                Constants.IERS2010_EARTH_FLATTENING,
                FramesFactory.getITRF(ITRFVersion.ITRF_2020, IERSConventions.IERS_2010, false));

        final GeodeticPoint point = new GeodeticPoint(0., 0., 100.);
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "name");
        final GroundStation station = new GroundStation(baseFrame);

        final Frame[] frames = {FramesFactory.getEME2000(), FramesFactory.getGCRF(), FramesFactory.getICRF(), FramesFactory.getTOD(false)};
        final double[][] raDec = new double[frames.length][];
        for (int i = 0; i < frames.length; i++) {
            // build RA-Dec with specific reference frame
            final ObservableSatellite os = new ObservableSatellite(0);
            final AngularRaDecBuilder builder = new AngularRaDecBuilder(null, station, frames[i],
                    new double[]{1., 1.}, new double[]{1., 1.}, os);
            builder.init(spacecraftState.getDate(), spacecraftState.getDate());
            final double[] moreRaDec = builder.build(spacecraftState.getDate(),
                                                     Collections.singletonMap(os, fakeInterpolator)).getObservedValue();
            // convert in common frame
            final StaticTransform transform = frames[i].getStaticTransformTo(orbit.getFrame(), epoch);
            final Vector3D transformedLoS = transform.transformVector(new Vector3D(moreRaDec[0], moreRaDec[1]));
            raDec[i] = new double[] {FastMath.toDegrees(transformedLoS.getAlpha()),
                    FastMath.toDegrees(transformedLoS.getDelta())};
        }

        final double tolAngleDeg = 1e-2 / 3600.; // 0.01 arcsecond
        for (int i = 1; i < raDec.length; i++) {
            Assertions.assertEquals(raDec[i][0], raDec[0][0], tolAngleDeg);
            Assertions.assertEquals(raDec[i][1], raDec[0][1], tolAngleDeg);
        }

    }

}

