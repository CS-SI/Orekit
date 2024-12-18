/* Copyright 2002-2024 CS GROUP
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
import java.util.Arrays;
import java.util.Comparator;
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
import org.orekit.estimation.Force;
import org.orekit.estimation.TLEEstimationTestUtils;
import org.orekit.estimation.measurements.*;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class KalmanEstimatorTest {

    @Test
    void testEstimationStepWithBStarOnly() {
        // GIVEN
        TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        String line1 = "1 07276U 74026A   00055.48318287  .00000000  00000-0  22970+3 0  9994";
        String line2 = "2 07276  71.6273  78.7838 1248323  14.0598   3.8405  4.72707036231812";
        final TLE tle = new TLE(line1, line2);
        final TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(tle,
                PositionAngleType.TRUE, 1., new FixedPointTleGenerationAlgorithm());
        for (final ParameterDriver driver: propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            driver.setSelected(false);
        }
        propagatorBuilder.getPropagationParametersDrivers().getDrivers().get(0).setSelected(true);
        final KalmanEstimatorBuilder builder = new KalmanEstimatorBuilder();
        builder.addPropagationConfiguration(propagatorBuilder,
                new ConstantProcessNoise(MatrixUtils.createRealMatrix(1, 1)));
        final KalmanEstimator estimator = builder.build();
        final AbsoluteDate measurementDate = tle.getDate().shiftedBy(1.0);
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final Position positionMeasurement = new Position(measurementDate, propagator.getPosition(measurementDate,
                propagator.getFrame()), 1., 1., new ObservableSatellite(0));
        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> estimator.estimationStep(positionMeasurement));
    }

    @Test
    public void testTwoOrbitalParameters() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean perfectStart = true;
        final double minStep = 1.e-6;
        final double maxStep = 60.;
        final double dP = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                context.createBuilder(orbitType, positionAngleType, perfectStart,
                        minStep, maxStep, dP);

        // Create an imperfect PV measurement
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final AbsoluteDate measurementDate = context.initialOrbit.getDate().shiftedBy(600.0);
        final SpacecraftState state = propagator.propagate(measurementDate);
        final ObservedMeasurement<?> measurement = new PV(measurementDate,
                state.getPosition().add(new Vector3D(10.0, -10.0, 5.0)),
                state.getPVCoordinates().getVelocity().add(new Vector3D(-10.0, 5.0, -5.0)),
                5.0, 5.0, 1.0, new ObservableSatellite(0));

        // Unselect all orbital propagation parameters
        propagatorBuilder.getOrbitalParametersDrivers().getDrivers()
                .forEach(driver -> driver.setSelected(false));

        // Select eccentricity and anomaly
        propagatorBuilder.getOrbitalParametersDrivers().findByName("e").setSelected(true);
        propagatorBuilder.getOrbitalParametersDrivers().findByName("v").setSelected(true);

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-2, 1e-5
        });

        // Process noise matrix
        final RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1.e-8, 1.e-8
        });

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                build();

        // Do the estimation
        kalman.estimationStep(measurement);

        // Unchanged orbital parameters (two-body propagation)
        final KeplerianOrbit initialOrbit = (KeplerianOrbit) context.initialOrbit;
        Assertions.assertEquals(initialOrbit.getA(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("a").getValue());
        Assertions.assertEquals(initialOrbit.getI(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("i").getValue());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNode(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("Ω").getValue());
        Assertions.assertEquals(initialOrbit.getPerigeeArgument(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("ω").getValue());

        // Changed orbital parameters
        Assertions.assertNotEquals(initialOrbit.getE(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("e").getValue());
        Assertions.assertNotEquals(initialOrbit.getTrueAnomaly(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("v").getValue());
    }

    @Test
    public void testTwoOrbitalParametersMulti() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builders
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean perfectStart = true;
        final double minStep = 1.e-6;
        final double maxStep = 60.;
        final double dP = 1.;
        final NumericalPropagatorBuilder propagatorBuilder1 =
                context.createBuilder(orbitType, positionAngleType, perfectStart,
                        minStep, maxStep, dP, Force.POTENTIAL);

        final NumericalPropagatorBuilder propagatorBuilder2 =
                context.createBuilder(orbitType, positionAngleType, perfectStart,
                        minStep, maxStep, dP, Force.POTENTIAL, Force.SOLAR_RADIATION_PRESSURE);

        // Create imperfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder1);
        final AbsoluteDate measurementDate = context.initialOrbit.getDate().shiftedBy(600.0);
        final SpacecraftState state = propagator.propagate(measurementDate);
        final ObservedMeasurement<?> measurement1 = new PV(measurementDate,
                state.getPosition().add(new Vector3D(10.0, -10.0, 5.0)),
                state.getPVCoordinates().getVelocity().add(new Vector3D(-10.0, 5.0, -5.0)),
                5.0, 5.0, 1.0, new ObservableSatellite(0));
        final ObservedMeasurement<?> measurement2 = new PV(measurementDate,
                state.getPosition().add(new Vector3D(-10.0, 20.0, -1.0)),
                state.getPVCoordinates().getVelocity().add(new Vector3D(10.0, 50.0, -10.0)),
                5.0, 5.0, 1.0, new ObservableSatellite(1));
        final ObservedMeasurement<?> combinedMeasurement =
                new MultiplexedMeasurement(Arrays.asList(measurement1, measurement2));

        // Unselect all orbital propagation parameters
        propagatorBuilder1.getOrbitalParametersDrivers().getDrivers()
                .forEach(driver -> driver.setSelected(false));
        propagatorBuilder2.getOrbitalParametersDrivers().getDrivers()
                .forEach(driver -> driver.setSelected(false));

        // Select eccentricity and anomaly
        propagatorBuilder1.getOrbitalParametersDrivers().findByName("e").setSelected(true);
        propagatorBuilder1.getOrbitalParametersDrivers().findByName("v").setSelected(true);

        propagatorBuilder2.getOrbitalParametersDrivers().findByName("e").setSelected(true);
        propagatorBuilder2.getOrbitalParametersDrivers().findByName("v").setSelected(true);

        // Select reflection coefficient for second sat
        propagatorBuilder2.getPropagationParametersDrivers().findByName("reflection coefficient").setSelected(true);

        // Record the propagation parameter values
        final double[] parameterValues1 = propagatorBuilder1.getPropagationParametersDrivers().getDrivers().stream()
                .mapToDouble(ParameterDriver::getValue)
                .toArray();
        final double[] parameterValues2 = propagatorBuilder2.getPropagationParametersDrivers().getDrivers().stream()
                .mapToDouble(ParameterDriver::getValue)
                .toArray();


        // Reference position/velocity at measurement date
        final Propagator referencePropagator1 = propagatorBuilder1.buildPropagator();
        final KeplerianOrbit refOrbit1 = (KeplerianOrbit) referencePropagator1.propagate(measurementDate).getOrbit();

        final Propagator referencePropagator2 = propagatorBuilder2.buildPropagator();
        final KeplerianOrbit refOrbit2 = (KeplerianOrbit) referencePropagator2.propagate(measurementDate).getOrbit();

        // Covariance matrix initialization
        final RealMatrix initialP1 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-2, 1e-5
        });
        final RealMatrix initialP2 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-2, 1e-5, 1e-5
        });

        // Process noise matrix
        final RealMatrix Q1 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-8, 1e-8
        });
        final RealMatrix Q2 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-8, 1e-8, 1e-8
        });

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder1, new ConstantProcessNoise(initialP1, Q1))
                .addPropagationConfiguration(propagatorBuilder2, new ConstantProcessNoise(initialP2, Q2))
                .build();

        // Do the estimation
        kalman.estimationStep(combinedMeasurement);

        // Unchanged orbital parameters
        Assertions.assertEquals(refOrbit1.getA(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("a[0]").getValue());
        Assertions.assertEquals(refOrbit1.getI(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("i[0]").getValue());
        Assertions.assertEquals(refOrbit1.getRightAscensionOfAscendingNode(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("Ω[0]").getValue());
        Assertions.assertEquals(refOrbit1.getPerigeeArgument(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("ω[0]").getValue());

        Assertions.assertEquals(refOrbit2.getA(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("a[1]").getValue());
        Assertions.assertEquals(refOrbit2.getI(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("i[1]").getValue());
        Assertions.assertEquals(refOrbit2.getRightAscensionOfAscendingNode(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("Ω[1]").getValue());
        Assertions.assertEquals(refOrbit2.getPerigeeArgument(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("ω[1]").getValue());

        // Changed orbital parameters
        Assertions.assertNotEquals(refOrbit1.getE(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("e[0]").getValue());
        Assertions.assertNotEquals(refOrbit1.getTrueAnomaly(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("v[0]").getValue());

        Assertions.assertNotEquals(refOrbit2.getE(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("e[1]").getValue());
        Assertions.assertNotEquals(refOrbit2.getTrueAnomaly(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("v[1]").getValue());

        // Propagation parameters
        final List<DelegatingDriver> drivers1 = propagatorBuilder1.getPropagationParametersDrivers().getDrivers();
        for (int i = 0; i < parameterValues1.length; ++i) {
            double postEstimation = drivers1.get(i).getValue();
            Assertions.assertEquals(parameterValues1[i], postEstimation);
        }

        final List<DelegatingDriver> drivers2 = propagatorBuilder2.getPropagationParametersDrivers().getDrivers();
        for (int i = 0; i < parameterValues2.length; ++i) {
            double postEstimation = drivers2.get(i).getValue();
            if (drivers2.get(i).getName().equals("reflection coefficient")) {
                Assertions.assertNotEquals(parameterValues2[i], postEstimation);
            } else {
                Assertions.assertEquals(parameterValues2[i], postEstimation);
            }
        }
    }

    @Test
    public void testWrongProcessCovarianceDimension() {
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean perfectStart = true;
        final double minStep = 1.e-6;
        final double maxStep = 60.;
        final double dP = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                context.createBuilder(orbitType, positionAngleType, perfectStart,
                        minStep, maxStep, dP);

        // Create an imperfect PV measurement
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final AbsoluteDate measurementDate = context.initialOrbit.getDate().shiftedBy(600.0);
        final SpacecraftState state = propagator.propagate(measurementDate);
        final ObservedMeasurement<PV> measurement = new PV(measurementDate,
                state.getPosition().add(new Vector3D(10.0, -10.0, 5.0)),
                state.getPVCoordinates().getVelocity().add(new Vector3D(-10.0, 5.0, -5.0)),
               5.0, 5.0, 1.0, new ObservableSatellite(0));

        // Unselect all orbital propagation parameters
        propagatorBuilder.getOrbitalParametersDrivers().getDrivers()
                .forEach(driver -> driver.setSelected(false));

        // Select eccentricity and anomaly
        propagatorBuilder.getOrbitalParametersDrivers().findByName("e").setSelected(true);
        propagatorBuilder.getOrbitalParametersDrivers().findByName("v").setSelected(true);

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-2, 1e-5
        });
        final RealMatrix badInitialP = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-2, 1e-5, 1e6
        });

        // Process noise matrix
        final RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1.e-8, 1.e-8
        });
        final RealMatrix badQ = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1.e-8, 1.e-8, 1e-6
        });
        final RealMatrix measQ = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1.e-8
        });

        // Initialise the Kalman builder
        final KalmanEstimatorBuilder kalmanBuilderBadInitial = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(badInitialP, Q));

        // Build the filter (should not succeed)
        OrekitException thrown = Assertions.assertThrows(OrekitException.class, kalmanBuilderBadInitial::build);
        Assertions.assertTrue(thrown.getMessage().contains("Process covariance expecting dimension 2, got 3"));

        // Build the Kalman filter
        final KalmanEstimator kalmanBadProcessNoise = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, badQ))
                .build();

        // Run the filter (should not succeed)
        thrown = Assertions.assertThrows(OrekitException.class, () -> kalmanBadProcessNoise.estimationStep(measurement));
        Assertions.assertTrue(thrown.getMessage().contains("Process covariance expecting dimension 2, got 3"));

        // Initialize the Kalman builder
        final KalmanEstimatorBuilder kalmanBadProcessNoiseWithMeasurementProcessNoise = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(badInitialP, badQ))
                .estimatedMeasurementsParameters(new ParameterDriversList(), new ConstantProcessNoise(measQ, measQ));

        // Build the filter (should not succeed)
        thrown = Assertions.assertThrows(OrekitException.class, kalmanBadProcessNoiseWithMeasurementProcessNoise::build);
        Assertions.assertTrue(thrown.getMessage().contains("Process covariance expecting dimension 2, got 3"));

        // Add a measurement parameter
        final Bias<PV> pvBias = new Bias<>(new String[]{"x bias"},
                new double[]{0.0}, new double[]{1.0},
                new double[]{1.0}, new double[]{1.0});
        pvBias.getParameterDriver("x bias").setSelected(true);
        measurement.addModifier(pvBias);

        // Initialize the Kalman builder
        ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(pvBias.getParameterDriver("x bias"));
        final KalmanEstimator estimatorBadMeasurementNoise = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, badQ))
                .estimatedMeasurementsParameters(drivers, new ConstantProcessNoise(measQ, measQ)).build();

        // Run the filter (should not succeed)
        thrown = Assertions.assertThrows(OrekitException.class, () -> estimatorBadMeasurementNoise.estimationStep(measurement));
        Assertions.assertTrue(thrown.getMessage().contains("Process covariance expecting dimension 2, got 3"));
    }

    @Test
    public void testWrongMeasurementCovarianceDimension() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                context.createBuilder(orbitType, positionAngleType, perfectStart,
                        minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                        new TwoWayRangeMeasurementCreator(context),
                        1.0, 1.1, 60.0);

        // Add a measurement parameter
        final Bias<Range> rangeBias = new Bias<>(new String[]{"range bias"}, new double[]{0.0}, new double[]{1.0},
                new double[]{Double.NEGATIVE_INFINITY}, new double[]{Double.POSITIVE_INFINITY});
        rangeBias.getParameterDriver("range bias").setSelected(true);
        measurements.forEach(measurement -> ((Range) measurement).addModifier(rangeBias));

        ParameterDriversList measurementParameters = new ParameterDriversList();
        measurementParameters.add(rangeBias.getParameterDriver("range bias"));

        // Change semi-major axis of 1.2m as in the batch test
        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialPStateOnly = Jac.multiply(cartesianP.multiply(Jac.transpose()));
        final RealMatrix initialP = MatrixUtils.createRealIdentityMatrix(7);
        initialP.setSubMatrix(initialPStateOnly.getData(), 0, 0);

        // Process noise matrix is set to 0 here
        final RealMatrix QStateOnly = MatrixUtils.createRealMatrix(6, 6);
        final RealMatrix Q = MatrixUtils.createRealMatrix(7, 7);

        // Initialise the Kalman builder
        final KalmanEstimatorBuilder kalmanBuilderBadInitial = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialPStateOnly, Q))
                .estimatedMeasurementsParameters(measurementParameters, null);

        // Build the filter (should not succeed)
        final OrekitException thrownBadInitial =
                Assertions.assertThrows(OrekitException.class, kalmanBuilderBadInitial::build);
        Assertions.assertTrue(thrownBadInitial.getMessage().contains("Process covariance expecting dimension 7, got 6"));

        // Build the Kalman filter
        final KalmanEstimator kalmanBadProcessNoise = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, QStateOnly))
                .estimatedMeasurementsParameters(measurementParameters, null)
                .build();

        // Run the filter (should not succeed)
        final OrekitException thrownBadQ = Assertions.assertThrows(OrekitException.class,
                () -> kalmanBadProcessNoise.processMeasurements(measurements));
        Assertions.assertTrue(thrownBadQ.getMessage().contains("Process covariance expecting dimension 7, got 6"));

        // Measurement covariance providers
        final ConstantProcessNoise badMeasurementInitialNoise =
                new ConstantProcessNoise(MatrixUtils.createRealIdentityMatrix(2),
                        MatrixUtils.createRealIdentityMatrix(1));
        final ConstantProcessNoise badMeasurementProcessNoise =
                new ConstantProcessNoise(MatrixUtils.createRealIdentityMatrix(1),
                        MatrixUtils.createRealIdentityMatrix(2));

        // Initialise the Kalman builder
        final KalmanEstimatorBuilder kalmanBuilderBadInitialMeas = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialPStateOnly, QStateOnly))
                .estimatedMeasurementsParameters(measurementParameters, badMeasurementInitialNoise);

        // Build the filter (should not succeed)
        final OrekitException thrownBadInitialMeas =
                Assertions.assertThrows(OrekitException.class, kalmanBuilderBadInitialMeas::build);
        Assertions.assertTrue(thrownBadInitialMeas.getMessage()
                .contains("Measurement covariance expecting dimension 1, got 2"));

        // Build the Kalman filter
        final KalmanEstimator kalmanBadProcessNoiseMeas = new KalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialPStateOnly, QStateOnly))
                .estimatedMeasurementsParameters(measurementParameters, badMeasurementProcessNoise)
                .build();

        // Run the filter (should not succeed)
        final OrekitException thrownBadQMeas = Assertions.assertThrows(OrekitException.class,
                () -> kalmanBadProcessNoiseMeas.processMeasurements(measurements));
        Assertions.assertTrue(thrownBadQMeas.getMessage()
                .contains("Measurement covariance expecting dimension 1, got 2"));
    }

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
    public void testKeplerianPV() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

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
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.80e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.28e-11;
        final double[] expectedsigmasPos = {0.998872, 0.933655, 0.997516};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {9.478853e-4, 9.910788e-4, 5.0438709e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect range measurements with a biased start
     * Keplerian formalism
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Change semi-major axis of 1.2m as in the batch test
        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.77e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.93e-8;
        final double[] expectedSigmasPos = {0.742488, 0.281914, 0.563213};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.206636e-4, 1.306656e-4, 1.293981e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
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
    public void testKeplerianRangeWithOnBoardAntennaOffset() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));

        // Antenna phase center definition
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // Create perfect range measurements with antenna offset
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
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
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Change semi-major axis of 1.2m as in the batch test
        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            10., 10., 10., 1e-3, 1e-3, 1e-3
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = OrbitType.KEPLERIAN.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.57e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.29e-6;
        final double[] expectedSigmasPos = {1.105198, 0.930797, 1.254581};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {6.193757e-4, 4.088798e-4, 3.299140e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect range rate measurements with a perfect start
     * Cartesian formalism
     */
    @Test
    public void testCartesianRangeRate() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               creator,
                                                               1.0, 3.0, 300.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-4, 1e-4, 1e-4, 1e-10, 1e-10, 1e-10
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix
        final RealMatrix cartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-6, 1.e-6, 1.e-6, 1.e-12, 1.e-12, 1.e-12
        });
        final RealMatrix Q = Jac.multiply(cartesianQ.multiply(Jac.transpose()));

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.5e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.1e-10;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765933e-4, 5.056124e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect azimuth/elevation measurements with a perfect start
     * Circular formalism
     */
    @Test
    public void testCircularAzimuthElevation() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CIRCULAR;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-4, 1e-4, 1e-4, 1e-10, 1e-10, 1e-10
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix
        final RealMatrix cartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-6, 1.e-6, 1.e-6, 1.e-12, 1.e-12, 1.e-12
        });
        final RealMatrix Q = Jac.multiply(cartesianQ.multiply(Jac.transpose()));

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.78e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.54e-10;
        final double[] expectedSigmasPos = {0.356902, 1.297507, 1.798551};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.468745e-4, 5.810027e-4, 3.887394e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Perfect right-ascension/declination measurements with a perfect start
     * Equinoctial formalism
     */
    @Test
    public void testEquinoctialRightAscensionDeclination() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularRaDecMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-4, 1e-4, 1e-4, 1e-10, 1e-10, 1e-10
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(positionAngleType, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix
        final RealMatrix cartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-6, 1.e-6, 1.e-6, 1.e-12, 1.e-12, 1.e-12
        });
        final RealMatrix Q = Jac.multiply(cartesianQ.multiply(Jac.transpose()));

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.8e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.6e-10;
        final double[] expectedSigmasPos = {0.356902, 1.297508, 1.798552};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.468746e-4, 5.810028e-4, 3.887394e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    /** Perfect Range, Azel and range rate measurements with a biased start
     *  Start: position/velocity biased with: [+1000,0,0] m and [0,0,0.01] m/s
     *  Keplerian formalism
     */
    @Test
    public void testKeplerianRangeAzElAndRangeRate() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder measPropagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator rangePropagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                measPropagatorBuilder);
        final List<ObservedMeasurement<?>> rangeMeasurements =
                        EstimationTestUtils.createMeasurements(rangePropagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               0.0, 4.0, 300.0);
        // Create perfect az/el measurements
        final Propagator angularPropagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                measPropagatorBuilder);
        final List<ObservedMeasurement<?>> angularMeasurements =
                        EstimationTestUtils.createMeasurements(angularPropagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               0.0, 4.0, 500.0);
        // Create perfect range rate measurements
        final Propagator rangeRatePropagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                    measPropagatorBuilder);
        final List<ObservedMeasurement<?>> rangeRateMeasurements =
                        EstimationTestUtils.createMeasurements(rangeRatePropagator,
                                                               new RangeRateMeasurementCreator(context, false, 3.2e-10),
                                                               0.0, 4.0, 700.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
        measurements.addAll(rangeMeasurements);
        measurements.addAll(angularMeasurements);
        measurements.addAll(rangeRateMeasurements);
        measurements.sort(Comparator.naturalOrder());

        // Reference propagator for estimation performances
        final Propagator referencePropagator = measPropagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Biased propagator for the Kalman
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, false,
                                              minStep, maxStep, dP);

        // Cartesian covariance matrix initialization
        // Initial sigmas: 1000m on position, 0.01m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e6, 1e6, 1e6, 1e-4, 1e-4, 1e-4
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(positionAngleType, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Orbital initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix
        final RealMatrix cartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });
        final RealMatrix Q = Jac.multiply(cartesianQ.multiply(Jac.transpose()));

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 2.94e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.8e-6;
        final double[] expectedSigmasPos = {1.747575, 0.666887, 1.696202};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {5.413689e-4, 4.088394e-4, 4.315366e-4};
        final double   sigmaVelEps       = 1e-10;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
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
    public void testKeplerianRangeAndRangeRate() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range & range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, 3.2e-10),
                                                               1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-8, 1e-8, 1e-8
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix
        final RealMatrix cartesianQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });
        final RealMatrix Q = Jac.multiply(cartesianQ.multiply(Jac.transpose()));

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.6e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.7e-10;
        final double[] expectedSigmasPos = {0.341528, 8.175341, 4.634528};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {1.167859e-3, 1.036492e-3, 2.834413e-3};
        final double   sigmaVelEps       = 1e-9;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    @Test
    public void testMultiSat() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final AbsoluteDate referenceDate = propagatorBuilder1.getInitialOrbitDate();

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        measurements.addAll(EstimationTestUtils.createMeasurements(propagator1,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 60.0));
        measurements.sort(Comparator.naturalOrder());

        // create orbit estimator
        final RealMatrix processNoiseMatrix = MatrixUtils.createRealDiagonalMatrix(new double[] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder1, new ConstantProcessNoise(processNoiseMatrix)).
                        addPropagationConfiguration(propagatorBuilder2, new ConstantProcessNoise(processNoiseMatrix)).
                        build();

        List<DelegatingDriver> parameters = kalman.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.get(0);
        Assertions.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assertions.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(4.7246,
                            Vector3D.distance(closeOrbit.getPosition(),
                                              before.getPosition()),
                            1.0e-3);
        Assertions.assertEquals(0.0010514,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              before.getPVCoordinates().getVelocity()),
                            1.0e-6);

        Orbit[] refOrbits = new Orbit[] {
            propagatorBuilder1.buildPropagator().propagate(measurements.get(measurements.size()-1).getDate()).getOrbit(),
            propagatorBuilder2.buildPropagator().propagate(measurements.get(measurements.size()-1).getDate()).getOrbit()
        };
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbits, new PositionAngleType[] { PositionAngleType.TRUE, PositionAngleType.TRUE },
                                           new double[] { 38.3,  172.3 }, new double[] { 0.1,  0.1 },
                                           new double[] { 0.015, 0.068 }, new double[] { 1.0e-3, 1.0e-3 },
                                           new double[][] {
                                               { 6.9e5, 6.0e5, 12.5e5 },
                                               { 6.8e5, 5.9e5, 12.7e5 }
                                           }, new double[] { 1e4, 1e4 },
                                           new double[][] {
                                               { 5.0e2, 5.3e2, 1.4e2 },
                                               { 5.0e2, 5.3e2, 1.4e2 }
                                           }, new double[] { 1.0e1, 1.0e1 });

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(referenceDate), 1.0e-15);
            }
        }

    }

    /**
     * Test of a wrapped exception in a Kalman observer
     */
    @Test
    public void testWrappedException() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        // Build the Kalman filter
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder,
                                                  new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6)));
        final KalmanEstimator kalman = kalmanBuilder.build();
        kalman.setObserver(estimation -> {
                throw new DummyException();
            });


        try {
            // Filter the measurements and expect an exception to occur
            EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               context.initialOrbit, positionAngleType,
                                               0., 0.,
                                               0., 0.,
                                               new double[3], 0.,
                                               new double[3], 0.);
        } catch (DummyException de) {
            // expected
        }

    }

    @Test
    public void testIssue695() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create a position measurement
        final Position position = new Position(context.initialOrbit.getDate(),
                                               new Vector3D(1.0, -1.0, 0.0),
                                               0.5, 1.0, new ObservableSatellite(0));

        // Decorated measurement
        MeasurementDecorator decorated = KalmanEstimatorUtil.decorate(position, context.initialOrbit.getDate());

        // Verify time
        Assertions.assertEquals(0.0, decorated.getTime(), 1.0e-15);
        // Verify covariance matrix
        final RealMatrix covariance = decorated.getCovariance();
        for (int i = 0; i < covariance.getRowDimension(); i++) {
            for (int j = 0; j < covariance.getColumnDimension(); j++) {
                if (i == j) {
                    Assertions.assertEquals(1.0, covariance.getEntry(i, j), 1.0e-15);
                } else {
                    Assertions.assertEquals(0.0, covariance.getEntry(i, j), 1.0e-15);
                }
            }
        }

    }

    @Test
    public void tesIssue696() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final AbsoluteDate referenceDate = propagatorBuilder1.getInitialOrbitDate();

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final InterSatellitesRangeMeasurementCreator creator = new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset);

        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator1, creator,
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        measurements.addAll(EstimationTestUtils.createMeasurements(propagator1,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 60.0));
        measurements.sort(Comparator.naturalOrder());

        // Estimate clock drivers
        creator.getLocalSatellite().getClockOffsetDriver().setSelected(true);
        creator.getRemoteSatellite().getClockOffsetDriver().setSelected(true);

        // Estimated measurement parameter
        final ParameterDriversList estimatedMeasurementParameters = new ParameterDriversList();
        estimatedMeasurementParameters.add(creator.getLocalSatellite().getClockOffsetDriver());
        estimatedMeasurementParameters.add(creator.getRemoteSatellite().getClockOffsetDriver());

        // create orbit estimator
        final RealMatrix processNoiseMatrix = MatrixUtils.createRealDiagonalMatrix(new double[] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });
        final RealMatrix measurementNoiseMatrix = MatrixUtils.createRealDiagonalMatrix(new double[] {
           1.e-15, 1.e-15
        });
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder1, new ConstantProcessNoise(processNoiseMatrix)).
                        addPropagationConfiguration(propagatorBuilder2, new ConstantProcessNoise(processNoiseMatrix)).
                        estimatedMeasurementsParameters(estimatedMeasurementParameters, new ConstantProcessNoise(measurementNoiseMatrix)).
                        build();

        List<DelegatingDriver> parameters = kalman.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.get(0);
        Assertions.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assertions.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(4.7246,
                            Vector3D.distance(closeOrbit.getPosition(),
                                              before.getPosition()),
                            1.0e-3);
        Assertions.assertEquals(0.0010514,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              before.getPVCoordinates().getVelocity()),
                            1.0e-6);

        Orbit[] refOrbits = new Orbit[] {
            propagatorBuilder1.buildPropagator().propagate(measurements.get(measurements.size()-1).getDate()).getOrbit(),
            propagatorBuilder2.buildPropagator().propagate(measurements.get(measurements.size()-1).getDate()).getOrbit()
        };
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbits, new PositionAngleType[] { PositionAngleType.TRUE, PositionAngleType.TRUE },
                                           new double[] { 38.3,  172.3 }, new double[] { 0.1,  0.1 },
                                           new double[] { 0.015, 0.068 }, new double[] { 1.0e-3, 1.0e-3 },
                                           new double[][] {
                                               { 6.9e5, 6.0e5, 12.5e5 },
                                               { 6.8e5, 5.9e5, 12.7e5 }
                                           }, new double[] { 1e4, 1e4 },
                                           new double[][] {
                                               { 5.0e2, 5.3e2, 1.4e2 },
                                               { 5.0e2, 5.3e2, 1.4e2 }
                                           }, new double[] { 1.0e1, 1.0e1 });

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(referenceDate), 1.0e-15);
            }
        }

    }

    @Test
    public void tesIssue850() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create propagator builder
        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        propagatorBuilder1.getPropagationParametersDrivers().getDrivers().forEach(driver -> driver.setSelected(true));

        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        propagatorBuilder2.getPropagationParametersDrivers().getDrivers().forEach(driver -> driver.setSelected(true));

        // Generate measurement for both propagators
        final Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder1);

        final List<ObservedMeasurement<?>> measurements1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new PositionMeasurementCreator(),
                                                               0.0, 3.0, 300.0);

        final Propagator propagator2 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                            propagatorBuilder2);

        final List<ObservedMeasurement<?>> measurements2 =
                         EstimationTestUtils.createMeasurements(propagator2,
                                                                new PositionMeasurementCreator(),
                                                                0.0, 3.0, 300.0);

        // Create a multiplexed measurement
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
        measurements.add(measurements1.get(0));
        measurements.add(measurements2.get(0));
        final ObservedMeasurement<?> multiplexed = new MultiplexedMeasurement(measurements);

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-5, 1e-5, 1e-5, 1e-8
        });

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8
        });


        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder1, new ConstantProcessNoise(initialP, Q)).
                        addPropagationConfiguration(propagatorBuilder2, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Perform an estimation at the first measurment epoch (estimated states must be identical to initial orbit)
        Propagator[] estimated = kalman.estimationStep(multiplexed);
        final Vector3D pos1 = estimated[0].getInitialState().getPosition();
        final Vector3D pos2 = estimated[1].getInitialState().getPosition();

        // Verify
        Assertions.assertEquals(0.0, pos1.distance(pos2), 1.0e-12);
        Assertions.assertEquals(0.0, pos1.distance(context.initialOrbit.getPosition()), 1.0e-12);
        Assertions.assertEquals(0.0, pos2.distance(context.initialOrbit.getPosition()), 1.0e-12);

    }

    private static class DummyException extends OrekitException {
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }
}


