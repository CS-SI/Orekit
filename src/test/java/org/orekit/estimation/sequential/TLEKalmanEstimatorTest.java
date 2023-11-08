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
package org.orekit.estimation.sequential;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.TLEContext;
import org.orekit.estimation.TLEEstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
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

import java.util.ArrayList;
import java.util.List;

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
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder = context.createBuilder(minStep, maxStep, dP);

        // Create perfect PV measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final TLEPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

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
        final double   posEps            = 9.61e-2; // With numerical propagator: 5.80e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.86e-5; // With numerical propagator: 2.28e-11;
        TLEEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Perfect range measurements with a biased start
     * Keplerian formalism
     */
    @Test
    public void testRange() {

        // Create context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(minStep, maxStep, dP);

        // Create perfect range measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final TLEPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
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
        final double   posEps            = 0.32; // With numerical propagator: 1.77e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.45e-5; // With numerical propagator: 7.93e-8;
        TLEEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     * Keplerian formalism
     */
    @Test
    public void testRangeWithOnBoardAntennaOffset() {

        // Create context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(minStep, maxStep, dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // Antenna phase center definition
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // Create perfect range measurements with antenna offset
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = EstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
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
        final TLEPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
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
        final double   posEps            = 0.69; // With numerical propagator: 4.57e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.69e-4; // With numerical propagator: 7.29e-6;
        TLEEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testRangeAndRangeRate() {

        // Create context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(minStep, maxStep, dP);

        // Create perfect range & range rate measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, 0.0),
                                                               1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final TLEPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Change X position of 10m as in the batch test
        ParameterDriver xDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
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
        final double   posEps            = 0.45; // With numerical propagator: 1.2e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.86e-4; // With numerical propagator: 4.2e-10;
        TLEEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Test of a wrapped exception in a Kalman observer
     */
    @Test
    public void testWrappedException() {

        // Create context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(minStep, maxStep, dP);

        // Create perfect range measurements
        Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
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
            TLEEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               initialOrbit, positionAngleType,
                                               0., 0.,
                                               0., 0.);
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
