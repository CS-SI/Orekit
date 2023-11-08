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
import org.orekit.estimation.Context;
import org.orekit.estimation.UnscentedEstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
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
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class UnscentedKalmanEstimatorTest {

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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final double   posEps            = 3.63e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.42e-9;
        final double[] expectedsigmasPos = {1.762E-7, 1.899E-7, 7.398E-7};
        final double   sigmaPosEps       = 1.0e-10;
        final double[] expectedSigmasVel = {0.90962E-10, 2.61847E-10, 0.37545E-10};
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final double   posEps            = 8.35e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 3.39e-10;
        final double[] expectedsigmasPos = {0.1938703E-8, 12.7585598E-8, 17.0372647E-8};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {3.32084E-11, 0.3787E-11, 8.0020E-11};
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final double   posEps            = 1.74e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 6.06e-10;
        final double[] expectedsigmasPos = {8.869538E-9, 1.18524507E-7, 4.32132152E-8};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {1.5213E-11, 7.738E-12, 4.0380E-11};
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final double   posEps            = 5.43e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.96e-9;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765933e-4, 5.056124e-4};
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        final double   posEps            = 6.05e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.07e-10;
        final double[] expectedSigmasPos = {0.012134, 0.511243, 0.264925};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {5.72891E-5, 1.58811E-5, 15.98658E-5};
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
            propagatorBuilder1.
            buildPropagator(propagatorBuilder1.getSelectedNormalizedParameters()).
            propagate(measurements.get(measurements.size()-1).getDate()).getOrbit(),
            propagatorBuilder2.
            buildPropagator(propagatorBuilder2.getSelectedNormalizedParameters()).
            propagate(measurements.get(measurements.size()-1).getDate()).getOrbit()
        };
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbits, new PositionAngleType[] { PositionAngleType.TRUE, PositionAngleType.TRUE },
                                           new double[] { 38.3,  172.3 }, new double[] { 0.1,  0.1 },
                                           new double[] { 0.015, 0.068 }, new double[] { 1.0e-3, 1.0e-3 },
                                           new double[][] {
                                               { 1.5e-7, 0.6e-7, 4.2e-7 },
                                               { 1.5e-7, 0.5e-7, 4.2e-7 }
                                           }, new double[] { 1e-8, 1e-8 },
                                           new double[][] {
                                               { 1.9e-11, 17.5e-11, 3.1e-11 },
                                               { 2.0e-11, 17.5e-11, 2.8e-11 }
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
        final Bias<Range> rangeBias = new Bias<Range>(new String[] {"rangeBias"}, new double[] {0.0},
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

    private static class DummyException extends OrekitException {
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }

}
