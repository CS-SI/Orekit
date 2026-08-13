/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation.sequential;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class TLEKalmanEstimatorTest {

    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new KalmanEstimatorBuilder().
            build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    /**
     * Perfect PV measurements with a perfect start
     * Keplerian formalism
     */
    @Test
    public void testPV() {

        // Create context
        Context context = EstimationTestUtils.contextFromTle("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);

        // Create perfect PV measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit, propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.getLast().getDate()).getOrbit();

        // Covariance and process noise matrices initialization, expressed in Cartesian formalism
        final RealMatrix initialCartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-5, 1e-5, 1e-5
        });
        final RealMatrix initialCartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8
        });

        // the filter state vector holds the builder parameters, not the Cartesian coordinates,
        // so both matrices have to be converted: P(B) = dB/dC P(C) (dB/dC)^T
        final RealMatrix dBdC = propagatorBuilder.getOrbitalStateFactory().getJacobianWrtCartesian();
        final RealMatrix initialP = dBdC.multiply(initialCartesianP.multiply(dBdC.transpose()));
        final RealMatrix Q = dBdC.multiply(initialCartesianQ.multiply(dBdC.transpose()));


        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.70e-1;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 8.47e-5;
        final double[] expectedSigmasPos = {0.179554, 0.130144, 0.416558};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.469155E-4, 2.548258E-4, 2.165680E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, expectedDeltaPos, posEps,
                                                   expectedDeltaVel, velEps,
                                                   expectedSigmasPos, sigmaPosEps,
                                                   expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect range measurements with a biased start
     * Keplerian formalism
     */
    @Test
    public void testRange() {

        // Create context
        Context context = EstimationTestUtils.contextFromTle("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);

        // Create perfect range measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.getLast().getDate()).getOrbit();

        // FIXME: Bias the start as in the batch test, where the X position was shifted by 10 m.
        // The builder parameters are TLE mean elements, whose first one is the mean motion,
        // so the shift is applied as an equivalent semi-major axis change. Differentiating
        // n = sqrt(mu/a^3) gives dn = -3/2 (n/a) da. This is not the very same perturbation
        // as a 10 m shift along X, but it is of the same order of magnitude, which is all
        // this test needs to start away from the solution.
        final ParameterDriver meanMotionDriver =
            propagatorBuilder.getOrbitalStateFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        final double meanMotion = meanMotionDriver.getValue();
        final double semiMajorAxis = FastMath.cbrt(TLEPropagator.getMU() / (meanMotion * meanMotion));
        final double deltaMeanMotion = -1.5 * meanMotion / semiMajorAxis * 10;
        meanMotionDriver.setValue(meanMotion + deltaMeanMotion);
        meanMotionDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Covariance matrix initialization, expressed in Cartesian formalism
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // the filter state vector holds the builder parameters, not the Cartesian coordinates,
        // so the covariance has to be converted: P(B) = dB/dC P(C) (dB/dC)^T
        final RealMatrix dBdC = propagatorBuilder.getOrbitalStateFactory().getJacobianWrtCartesian();
        final RealMatrix initialP = dBdC.multiply(cartesianP.multiply(dBdC.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 3.61e-1;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 8.22e-5;
        final double[] expectedSigmasPos = {0.741634, 0.282909, 0.564608,};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.188267E-4, 1.308106E-4, 1.300569E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, expectedDeltaPos, posEps,
                                                   expectedDeltaVel, velEps,
                                                   expectedSigmasPos, sigmaPosEps,
                                                   expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     * Keplerian formalism
     */
    @Test
    public void testRangeWithOnBoardAntennaOffset() {

        // Create context
        Context context = EstimationTestUtils.contextFromTle("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getOrbitalStateFactory().getFrame(),
                                                            LOFType.LVLH));

        // Antenna phase center definition
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // Create perfect range measurements with antenna offset
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context,
                                                                                                 Vector3D.ZERO, null,
                                                                                                 antennaPhaseCenter, null,
                                                                                                 0),
                                                               1.0, 3.0, 300.0);

        // Add antenna offset to the measurements
        final PhaseCentersRangeModifier obaModifier = new PhaseCentersRangeModifier(FrequencyPattern.ZERO_CORRECTION,
                                                                                    new FrequencyPattern(antennaPhaseCenter,
                                                                                                         null));
        for (final ObservedMeasurement<?> range : measurements) {
            ((Range) range).addModifier(obaModifier);
        }

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.getLast().getDate()).getOrbit();

        // FIXME: Bias the start as in the batch test, where the X position was shifted by 10 m.
        // The builder parameters are TLE mean elements, whose first one is the mean motion,
        // so the shift is applied as an equivalent semi-major axis change. Differentiating
        // n = sqrt(mu/a^3) gives dn = -3/2 (n/a) da. This is not the very same perturbation
        // as a 10 m shift along X, but it is of the same order of magnitude, which is all
        // this test needs to start away from the solution.
        final ParameterDriver meanMotionDriver =
            propagatorBuilder.getOrbitalStateFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        final double meanMotion = meanMotionDriver.getValue();
        final double semiMajorAxis = FastMath.cbrt(TLEPropagator.getMU() / (meanMotion * meanMotion));
        final double deltaMeanMotion = -1.5 * meanMotion / semiMajorAxis * 10;
        meanMotionDriver.setValue(meanMotion + deltaMeanMotion);
        meanMotionDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Covariance matrix initialization, expressed in Cartesian formalism
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // the filter state vector holds the builder parameters, not the Cartesian coordinates,
        // so the covariance has to be converted: P(B) = dB/dC P(C) (dB/dC)^T
        final RealMatrix dBdC = propagatorBuilder.getOrbitalStateFactory().getJacobianWrtCartesian();
        final RealMatrix initialP = dBdC.multiply(cartesianP.multiply(dBdC.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 9.39e-1;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 3.16e-4;
        final double[] expectedSigmasPos = {1.250697, 1.197468, 1.543244};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {7.114008E-4, 4.470950E-4, 4.333291E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, expectedDeltaPos, posEps,
                                                   expectedDeltaVel, velEps,
                                                   expectedSigmasPos, sigmaPosEps,
                                                   expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testRangeAndRangeRate() {

        // Create context
        Context context = EstimationTestUtils.contextFromTle("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);

        // Create perfect range & range rate measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, 0.0),
                                                               1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.getLast().getDate()).getOrbit();

        // FIXME: Bias the start as in the batch test, where the X position was shifted by 10 m.
        // The builder parameters are TLE mean elements, whose first one is the mean motion,
        // so the shift is applied as an equivalent semi-major axis change. Differentiating
        // n = sqrt(mu/a^3) gives dn = -3/2 (n/a) da. This is not the very same perturbation
        // as a 10 m shift along X, but it is of the same order of magnitude, which is all
        // this test needs to start away from the solution.
        final ParameterDriver meanMotionDriver =
            propagatorBuilder.getOrbitalStateFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        final double meanMotion = meanMotionDriver.getValue();
        final double semiMajorAxis = FastMath.cbrt(TLEPropagator.getMU() / (meanMotion * meanMotion));
        final double deltaMeanMotion = -1.5 * meanMotion / semiMajorAxis * 10;
        meanMotionDriver.setValue(meanMotion + deltaMeanMotion);
        meanMotionDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Covariance matrix initialization, expressed in Cartesian formalism
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // the filter state vector holds the builder parameters, not the Cartesian coordinates,
        // so the covariance has to be converted: P(B) = dB/dC P(C) (dB/dC)^T
        final RealMatrix dBdC = propagatorBuilder.getOrbitalStateFactory().getJacobianWrtCartesian();
        final RealMatrix initialP = dBdC.multiply(cartesianP.multiply(dBdC.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 0.45;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.86e-4;
        final double[] expectedSigmasPos = {1.250697, 1.197467, 1.543243};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {7.114004E-4, 4.470947E-4, 4.333288E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, expectedDeltaPos, posEps,
                                                   expectedDeltaVel, velEps,
                                                   expectedSigmasPos, sigmaPosEps,
                                                   expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Test of a wrapped exception in a Kalman observer
     */
    @Test
    public void testWrappedException() {

        // Create context
        Context context = EstimationTestUtils.contextFromTle("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);

        // Create perfect range measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        // Build the Kalman filter
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder,
                                                  new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6)));
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList(), null);
        final KalmanEstimator kalman = kalmanBuilder.build();
        kalman.setObserver(estimation -> {
                throw new DummyException();
            });


        try {
            // Filter the measurements and expect an exception to occur
            EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                       initialOrbit, 0., 0., 0., 0., new double[0], 0., new double[0], 0.);
        } catch (DummyException de) {
            // expected
        }

    }

    private static class DummyException extends OrekitException {
        @Serial
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }

}
