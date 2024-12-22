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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.*;
import org.orekit.estimation.measurements.*;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
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
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class UnscentedKalmanEstimatorTest {

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
        final UnscentedKalmanEstimatorBuilder builder = new UnscentedKalmanEstimatorBuilder();
        builder.addPropagationConfiguration(propagatorBuilder,
                new ConstantProcessNoise(MatrixUtils.createRealMatrix(1, 1)));
        builder.unscentedTransformProvider(new MerweUnscentedTransform(1));
        final UnscentedKalmanEstimator estimator = builder.build();
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
        RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1.e-8, 1.e-8
        });

        // Build the unscented filter
        final UnscentedKalmanEstimator unscented = new UnscentedKalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q))
                .unscentedTransformProvider(new MerweUnscentedTransform(2))
                .build();

        // Do the estimation
        unscented.estimationStep(measurement);

        // Unchanged orbital parameters (two body propagation)
        final KeplerianOrbit initialOrbit = (KeplerianOrbit) context.initialOrbit;
        Assertions.assertEquals(initialOrbit.getA(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("a").getValue(), 1e-8);
        Assertions.assertEquals(initialOrbit.getI(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("i").getValue());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNode(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("Ω").getValue());
        Assertions.assertEquals(initialOrbit.getPerigeeArgument(),
                propagatorBuilder.getOrbitalParametersDrivers().findByName("ω").getValue(), 1e-15);

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
        RealMatrix Q1 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-8, 1e-8
        });
        RealMatrix Q2 = MatrixUtils.createRealDiagonalMatrix(new double[]{
                1e-8, 1e-8, 1e-8
        });

        // Build the Kalman filter
        final UnscentedKalmanEstimator unscented = new UnscentedKalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder1, new ConstantProcessNoise(initialP1, Q1))
                .addPropagationConfiguration(propagatorBuilder2, new ConstantProcessNoise(initialP2, Q2))
                .unscentedTransformProvider(new MerweUnscentedTransform(5))
                .build();

        // Do the estimation
        unscented.estimationStep(combinedMeasurement);

        // Unchanged orbital parameters
        Assertions.assertEquals(refOrbit1.getA(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("a[0]").getValue(), 1e-8);
        Assertions.assertEquals(refOrbit1.getI(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("i[0]").getValue());
        Assertions.assertEquals(refOrbit1.getRightAscensionOfAscendingNode(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("Ω[0]").getValue());
        Assertions.assertEquals(refOrbit1.getPerigeeArgument(),
                propagatorBuilder1.getOrbitalParametersDrivers().findByName("ω[0]").getValue(), 1e-15);

        Assertions.assertEquals(refOrbit2.getA(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("a[1]").getValue(), 1e-8);
        Assertions.assertEquals(refOrbit2.getI(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("i[1]").getValue());
        Assertions.assertEquals(refOrbit2.getRightAscensionOfAscendingNode(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("Ω[1]").getValue());
        Assertions.assertEquals(refOrbit2.getPerigeeArgument(),
                propagatorBuilder2.getOrbitalParametersDrivers().findByName("ω[1]").getValue(), 1e-15);

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
    public void testMissingPropagatorBuilder() {
        try {
            new UnscentedKalmanEstimatorBuilder().
            build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
        	Assertions.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    @Test
    public void testMissingUnscentedTransform() {
        try {
            Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");
            final OrbitType     orbitType     = OrbitType.CARTESIAN;
            final PositionAngleType positionAngleType = PositionAngleType.TRUE;
            final boolean       perfectStart  = true;
            final double        minStep       = 1.e-6;
            final double        maxStep       = 60.;
            final double        dP            = 1.;
            final NumericalPropagatorBuilder propagatorBuilder =
                            context.createBuilder(orbitType, positionAngleType, perfectStart,
                                                  minStep, maxStep, dP);
            new UnscentedKalmanEstimatorBuilder().
            addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6))).
            build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
        	Assertions.assertEquals(OrekitMessages.NO_UNSCENTED_TRANSFORM_CONFIGURED, oe.getSpecifier());
        }
    }

    /**
     * Perfect PV measurements with a perfect start.
     */
    @Test
    public void testPV() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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

        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);
        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6);
        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 2.7e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 9.7e-11;
        final double[] expectedsigmasPos = {0.0, 0.0, 0.0};
        final double   sigmaPosEps       = 1.0e-10;
        final double[] expectedSigmasVel = {0.0, 0.0, 0.0};
        final double   sigmaVelEps       = 1.0e-15;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }
    
    /**
     * Shifted PV measurements.
     */
    @Test
    public void testShiftedPV() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final double        sigmaPos      = 10.;
        final double        sigmaVel      = 0.01;

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngleType, perfectStart,
                                              minStep, maxStep, dP);
        
        // Create shifted initial state
        final Vector3D initialPosShifted = context.initialOrbit.getPosition().add(new Vector3D(sigmaPos, sigmaPos, sigmaPos));
        final Vector3D initialVelShifted = context.initialOrbit.getPVCoordinates().getVelocity().add(new Vector3D(sigmaVel, sigmaVel, sigmaVel));

        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(context.initialOrbit.getDate(), initialPosShifted, initialVelShifted);
        
        final CartesianOrbit shiftedOrbit = new CartesianOrbit(pv, context.initialOrbit.getFrame(), context.initialOrbit.getMu());
        
        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
        
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Initial covariance matrix
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double[] {sigmaPos*sigmaPos, sigmaPos*sigmaPos, sigmaPos*sigmaPos, sigmaVel*sigmaVel, sigmaVel*sigmaVel, sigmaVel*sigmaVel}); 

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
        
        propagatorBuilder.resetOrbit(shiftedOrbit);
        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 3.58e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.30e-6;
        final double[] expectedsigmasPos = {0.184985, 0.167475, 0.297570};
        final double   sigmaPosEps       = 1.0e-6;
        final double[] expectedSigmasVel = {6.93330E-5, 12.37128E-5, 4.11890E-5};
        final double   sigmaVelEps       = 1.0e-10;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());

    }

    /**
     * Perfect Range measurements with a perfect start.
     */
    @Test
    public void testCartesianRange() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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

        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               0.0, 1.0, 60.0);
        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6); 

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 6.2e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.7e-11;
        final double[] expectedsigmasPos = {0.0, 0.0, 0.0};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {0.0, 0.0, 0.0};
        final double   sigmaVelEps       = 1.0e-15;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    /**
     * Perfect Range measurements with a perfect start.
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               0.0, 1.0, 60.0);
        // Reference propagator for estimation performances
        final Propagator referencePropagator = propagatorBuilder.buildPropagator();
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6); 

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.0e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.4e-11;
        final double[] expectedsigmasPos = {0.0, 0.0, 0.0};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {0.0, 0.0, 0.0};
        final double   sigmaVelEps       = 1.0e-15;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }
    
    /**
     * Perfect range rate measurements with a perfect start
     * Cartesian formalism
     */
    @Test
    public void testCartesianRangeRate() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
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
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 2.0e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.3e-10;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765934e-4, 5.056124e-4};
        final double   sigmaVelEps       = 1e-10;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }
    
    /**
     * Perfect azimuth/elevation measurements with a perfect start
     */
    @Test
    public void testCartesianAzimuthElevation() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               0.0, 1.0, 60.0);

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
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.96e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.76e-10;
        final double[] expectedSigmasPos = {0.043885, 0.600764, 0.279020};
        final double   sigmaPosEps       = 1.0e-6;
        final double[] expectedSigmasVel = {7.17260E-5, 3.037315E-5, 19.49047e-5};
        final double   sigmaVelEps       = 1.0e-10;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }
    
    /**
     * Perfect azimuth/elevation measurements with a perfect start
     */
    @Test
    public void testCircularAzimuthElevation() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               0.0, 1.0, 60.0);

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
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.5e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.2e-11;
        final double[] expectedSigmasPos = {0.045182, 0.615288, 0.295163};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {7.25356E-5, 3.11525E-5, 19.81870E-5};
        final double   sigmaVelEps       = 1e-10;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);

        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    @Test
    public void testMultiSat() {

        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
        final Propagator closePropagator = UnscentedEstimationTestUtils.createPropagator(closeOrbit,
                                                                                         propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder1);
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> measurements =
        		UnscentedEstimationTestUtils.createMeasurements(propagator1,
                                                                new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                           localClockOffset,
                                                                                                           remoteClockOffset),
                                                                1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                    propagatorBuilder1);
        measurements.addAll(UnscentedEstimationTestUtils.createMeasurements(propagator1,
                                                                            new TwoWayRangeMeasurementCreator(context),
                                                                            1.0, 3.0, 60.0));
        measurements.sort(Comparator.naturalOrder());

        // create orbit estimator
        final RealMatrix processNoiseMatrix = MatrixUtils.createRealMatrix(6, 6);
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
        		        unscentedTransformProvider(new MerweUnscentedTransform(12)).
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
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbits, new PositionAngleType[] { PositionAngleType.TRUE, PositionAngleType.TRUE },
                                           new double[] { 38.3,  172.3 }, new double[] { 0.1,  0.1 },
                                           new double[] { 0.015, 0.068 }, new double[] { 1.0e-3, 1.0e-3 },
                                           new double[][] {
                                               { 0.0, 0.0, 0.0 },
                                               { 0.0, 0.0, 0.0 }
                                           }, new double[] { 1e-8, 1e-8 },
                                           new double[][] {
                                               { 0.0, 0.0, 0.0 },
                                               { 0.0, 0.0, 0.0 }
                                           }, new double[] { 1.0e-12, 1.0e-12 });

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

        Assertions.assertEquals(12, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(12, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());

    }

    /**
     * Test of a wrapped exception in a Kalman observer
     */
    @Test
    public void testWrappedException() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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

        // estimated bias
        final Bias<Range> rangeBias = new Bias<>(new String[] {"rangeBias"}, new double[] {0.0},
        	                                     new double[] {1.0},
        	                                     new double[] {0.0}, new double[] {10000.0});
        rangeBias.getParametersDrivers().get(0).setSelected(true);


        // List of estimated measurement parameters
        final ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(rangeBias.getParametersDrivers().get(0));

        // Create perfect range measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
        		UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context,
                                                                                                 Vector3D.ZERO, null,
                                                                                                 Vector3D.ZERO, null,
                                                                                                 2.0),
                                                               1.0, 3.0, 300.0);

        measurements.forEach(measurement -> ((Range) measurement).addModifier(rangeBias));
        propagatorBuilder.getAllForceModels().forEach(force -> force.getParametersDrivers().forEach(parameter -> parameter.setSelected(true)));
        // Build the Kalman filter
        final UnscentedKalmanEstimatorBuilder kalmanBuilder = new UnscentedKalmanEstimatorBuilder();
        kalmanBuilder.unscentedTransformProvider(new MerweUnscentedTransform(8));
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder,
                                                  new ConstantProcessNoise(MatrixUtils.createRealMatrix(7, 7)));
        kalmanBuilder.estimatedMeasurementsParameters(drivers, new ConstantProcessNoise(MatrixUtils.createRealIdentityMatrix(1)));
        final UnscentedKalmanEstimator kalman = kalmanBuilder.build();
        kalman.setObserver(estimation -> {
                throw new DummyException();
            });


        try {
            // Filter the measurements and expect an exception to occur
        	UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               context.initialOrbit, positionAngleType,
                                               0., 0.,
                                               0., 0.,
                                               new double[3], 0.,
                                               new double[3], 0.);
        } catch (DummyException de) {
            // expected
        }

    }

    /**
     * This test verifies issue 1034. The purpose is to verify the consistency of the covariance
     * of the decorated measurement.
     */
    @Test
    public void testIssue1034() {

        UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Reference date
        final AbsoluteDate reference = AbsoluteDate.J2000_EPOCH;

        // Create a station
        final OneAxisEllipsoid shape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final GroundStation station = new GroundStation(new TopocentricFrame(shape, new GeodeticPoint(1.44, 0.2, 100.0), "topo"));

        // Create three different measurement types
        final double sigmaPos = 2.0;
        final double sigmaVel = 2.0e-3;
        final double sigmaRange = 2.0;
        final Position pos = new Position(reference, Vector3D.PLUS_I, sigmaPos, 1.0, new ObservableSatellite(0));
        final PV pv = new PV(reference, Vector3D.PLUS_I, Vector3D.PLUS_J, sigmaPos, sigmaVel, 1.0, new ObservableSatellite(0));
        final Range range = new Range(station, false, reference, 100.0, sigmaRange, 1.0, new ObservableSatellite(0));

        // Decorated measurements
        final MeasurementDecorator decoratedPos = KalmanEstimatorUtil.decorateUnscented(pos, reference);
        final MeasurementDecorator decoratedPv = KalmanEstimatorUtil.decorateUnscented(pv, reference);
        final MeasurementDecorator decoratedRange = KalmanEstimatorUtil.decorateUnscented(range, reference);

        // Verify Position
        for (int row = 0; row < decoratedPos.getCovariance().getRowDimension(); row++) {
            Assertions.assertEquals(sigmaPos * sigmaPos, decoratedPos.getCovariance().getEntry(row, row));
        }

        // Verify PV
        for (int row = 0; row < decoratedPv.getCovariance().getRowDimension(); row++) {
            if (row < 3) {
                Assertions.assertEquals(sigmaPos * sigmaPos, decoratedPv.getCovariance().getEntry(row, row));
            } else {
                Assertions.assertEquals(sigmaVel * sigmaVel, decoratedPv.getCovariance().getEntry(row, row));
            }
        }

        // Verify Range
        Assertions.assertEquals(sigmaRange * sigmaRange, decoratedRange.getCovariance().getEntry(0, 0));

    }

    /**
     * Test that the states passed to the process noise covariance calculation are the previous and predicted.
     */
    @Test
    public void testProcessNoiseStates() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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

        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                        new PVMeasurementCreator(),
                        0.0, 1.0, 300.0);

        // Process noise
        ProcessNoise processNoise = new ProcessNoise();

        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                addPropagationConfiguration(propagatorBuilder, processNoise).
                unscentedTransformProvider(new MerweUnscentedTransform(6)).
                build();

        // Single estimation step on the 10th measurement
        kalman.estimationStep(measurements.get(10));

        // Make sure previous and current are not the same
        Assertions.assertEquals(measurements.get(0).getDate(), processNoise.getPrevious().getDate());
        Assertions.assertEquals(measurements.get(10).getDate(), processNoise.getCurrent().getDate());
        Assertions.assertNotEquals(processNoise.getPrevious().getPosition().getX(),
                                   processNoise.getCurrent().getPosition().getX());
    }

    private static class DummyException extends OrekitException {
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }


    private static class ProcessNoise implements CovarianceMatrixProvider {

        private SpacecraftState previous;
        private SpacecraftState current;

        public SpacecraftState getPrevious() {
            return previous;
        }

        public SpacecraftState getCurrent() {
            return current;
        }

        @Override
        public RealMatrix getInitialCovarianceMatrix(SpacecraftState initial) {
            return MatrixUtils.createRealMatrix(6, 6);
        }

        @Override
        public RealMatrix getProcessNoiseMatrix(SpacecraftState previous, SpacecraftState current) {
            this.previous = previous;
            this.current = current;
            return MatrixUtils.createRealMatrix(6, 6);
        }
    }

}
