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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
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
import org.orekit.orbits.PositionAngleType;
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
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
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

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-5, 1e-5, 1e-5
        });

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8
        });


        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 9.61e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.86e-5;
        final double[] expectedSigmasPos = {0.387328, 0.447026, 0.398594};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.452197E-4, 3.233220E-4, 2.367788E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, positionAngleType,
                                                   expectedDeltaPos, posEps,
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
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
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

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver =
            propagatorBuilder.getOrbitalParameterFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        xDriver.setValue(xDriver.getValue() + 10.0);
        xDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(cartesianP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 0.32;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.45e-5;
        final double[] expectedSigmasPos = {0.741641, 0.282908, 0.564609,};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.188273E-4, 1.308109E-4, 1.300570E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, positionAngleType,
                                                   expectedDeltaPos, posEps,
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
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createTleBuilder(dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getOrbitalParameterFactory().getFrame(), LOFType.LVLH));

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

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver = propagatorBuilder.getOrbitalParameterFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        xDriver.setValue(xDriver.getValue() + 10.0);
        xDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(cartesianP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList(), null).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 0.69;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.69e-4;
        final double[] expectedSigmasPos = {1.250710, 1.197467, 1.543259};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {7.114021E-4, 4.470958E-4, 4.333322E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, positionAngleType,
                                                   expectedDeltaPos, posEps,
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
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
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

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver = propagatorBuilder.getOrbitalParameterFactory().getOrbitalParametersDrivers().getDrivers().getFirst();
        xDriver.setValue(xDriver.getValue() + 10.0);
        xDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(cartesianP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 0.45;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.86e-4;
        final double[] expectedSigmasPos = {1.250710, 1.197466, 1.543258};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {7.114018E-4, 4.470955E-4, 4.333319E-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkExtendedKalmanFit(false, kalman, measurements,
                                                   refOrbit, positionAngleType,
                                                   expectedDeltaPos, posEps,
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
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
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
                                                       initialOrbit, positionAngleType,
                                                       0., 0., 0., 0., new double[0], 0., new double[0], 0.);
        } catch (DummyException de) {
            // expected
        }

    }

    private static class DummyException extends OrekitException {
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }

}
