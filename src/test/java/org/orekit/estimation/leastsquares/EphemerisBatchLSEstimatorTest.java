/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.EphemerisContext;
import org.orekit.estimation.KeplerianEstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.EphemerisPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;

public class EphemerisBatchLSEstimatorTest {

    private AbsoluteDate     initDate;
    private AbsoluteDate     finalDate;
    private Frame            inertialFrame;
    private Propagator       propagator;
    private EphemerisContext context;

    @BeforeEach
    public void setUp() throws IllegalArgumentException, OrekitException {
        Utils.setDataRoot("regular-data");

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                 TimeComponents.H00,
                 TimeScalesFactory.getUTC());

        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;
        double mu  = 3.9860047e14;
        inertialFrame = FramesFactory.getEME2000();

        Orbit initialState = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                            inertialFrame, initDate, mu);
        propagator = new KeplerianPropagator(initialState);

        context = new EphemerisContext();

    }

    @Test
    public void testRangeWithBias() {

        double dt = finalDate.durationFrom(initDate);
        double timeStep = dt / 20.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for(double t = 0 ; t <= dt; t+=timeStep) {
            states.add(propagator.propagate(initDate.shiftedBy(t)));
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(3, inertialFrame, inertialFrame);

        final Ephemeris ephemeris = new Ephemeris(states, interpolator);

        final double refBias = 1234.56;
        final List<ObservedMeasurement<?>> measurements =
                        KeplerianEstimationTestUtils.createMeasurements(ephemeris,
                                                                        new TwoWayRangeMeasurementCreator(context,
                                                                                                          Vector3D.ZERO, null,
                                                                                                          Vector3D.ZERO, null,
                                                                                                          refBias),
                                                                        1.0, 5.0, 10.0);

        // estimated bias
        final Bias<Range> rangeBias = new Bias<Range>(new String[] {"rangeBias"}, new double[] {0.0},
        	                                          new double[] {1.0},
        	                                          new double[] {0.0}, new double[] {10000.0});
        rangeBias.getParametersDrivers().get(0).setSelected(true);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                new EphemerisPropagatorBuilder(states, interpolator));
        for (final ObservedMeasurement<?> range : measurements) {
        	((Range) range).addModifier(rangeBias);
            estimator.addMeasurement(range);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(30);
        estimator.setMaxEvaluations(30);
        
        // estimate
        estimator.estimate();

        // verify
        Assertions.assertEquals(refBias, estimator.getMeasurementsParametersDrivers(true).getDrivers().get(0).getValue(), 5.0e-6);
        Assertions.assertEquals(1, estimator.getMeasurementsParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getPropagatorParametersDrivers(true).getNbParams());

    }

    @Test
    public void testRangeRateWithClockDrift() {

        double dt = finalDate.durationFrom(initDate);
        double timeStep = dt / 20.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for(double t = 0 ; t <= dt; t+=timeStep) {
            states.add(propagator.propagate(initDate.shiftedBy(t)));
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(3, inertialFrame, inertialFrame);

        final Ephemeris ephemeris = new Ephemeris(states, interpolator);

        final double refClockBias = 653.47e-11;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, refClockBias);
        creator.getSatellite().getClockDriftDriver().setSelected(true);
        final List<ObservedMeasurement<?>> measurements =
                        KeplerianEstimationTestUtils.createMeasurements(ephemeris, creator,
                                                                        1.0, 5.0, 10.0);


        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                new EphemerisPropagatorBuilder(states, interpolator));
        for (final ObservedMeasurement<?> rangeRate : measurements) {
            estimator.addMeasurement(rangeRate);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(30);
        estimator.setMaxEvaluations(30);
        
        // estimate
        
        estimator.estimate();

        // verify
        Assertions.assertEquals(refClockBias, estimator.getMeasurementsParametersDrivers(true).getDrivers().get(0).getValue(), 6.0e-16);
        Assertions.assertEquals(1, estimator.getMeasurementsParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getPropagatorParametersDrivers(true).getNbParams());

    }

    @Test
    public void testAzElWithBias() {

        double dt = finalDate.durationFrom(initDate);
        double timeStep = dt / 20.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for(double t = 0 ; t <= dt; t+=timeStep) {
            states.add(propagator.propagate(initDate.shiftedBy(t)));
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(3, inertialFrame, inertialFrame);

        final Ephemeris ephemeris = new Ephemeris(states, interpolator);

        final double refAzBias = FastMath.toRadians(0.3);
        final double refElBias = FastMath.toRadians(0.1);
        final List<ObservedMeasurement<?>> measurements =
                        KeplerianEstimationTestUtils.createMeasurements(ephemeris,
                                                                        new AngularAzElMeasurementCreator(context, refAzBias, refElBias),
                                                                        1.0, 5.0, 10.0);

        // estimated bias
        final Bias<AngularAzEl> azElBias = new Bias<>(new String[] {"azBias", "elBias"}, new double[] {0.0, 0.0},
        	                                          new double[] {1.0, 1.0},
        	                                          new double[] {0.0, 0.0}, new double[] {2.0, 2.0});
        azElBias.getParametersDrivers().get(0).setSelected(true);
        azElBias.getParametersDrivers().get(1).setSelected(true);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                new EphemerisPropagatorBuilder(states, interpolator));
        for (final ObservedMeasurement<?> azEl : measurements) {
        	((AngularAzEl) azEl).addModifier(azElBias);
            estimator.addMeasurement(azEl);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(30);
        estimator.setMaxEvaluations(30);
        
        // estimate
        estimator.estimate();

        // verify
        Assertions.assertEquals(refAzBias, estimator.getMeasurementsParametersDrivers(true).getDrivers().get(0).getValue(), 1.0e-8);
        Assertions.assertEquals(refElBias, estimator.getMeasurementsParametersDrivers(true).getDrivers().get(1).getValue(), 1.0e-7);
        Assertions.assertEquals(2, estimator.getMeasurementsParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, estimator.getPropagatorParametersDrivers(true).getNbParams());

    }

}
