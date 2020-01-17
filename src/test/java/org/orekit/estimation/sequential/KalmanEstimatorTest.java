/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.Comparator;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.AngularRaDecMeasurementCreator;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class KalmanEstimatorTest {

    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new KalmanEstimatorBuilder().
            build();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final NumericalPropagator referencePropagator = propagatorBuilder.
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
                        estimatedMeasurementsParameters(new ParameterDriversList()).
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        
        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
        
        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList()).
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        
        // Antenna phase center definition
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);
        
        // Create perfect range measurements with antenna offset
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context, antennaPhaseCenter, 0.0),
                                                               1.0, 3.0, 300.0);

        // Add antenna offset to the measurements
        final OnBoardAntennaRangeModifier obaModifier = new OnBoardAntennaRangeModifier(antennaPhaseCenter);
        for (final ObservedMeasurement<?> range : measurements) {
            ((Range) range).addModifier(obaModifier);
        }
        
        // Reference propagator for estimation performances
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        
        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
        
        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(new ParameterDriversList()).
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
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
                        estimatedMeasurementsParameters(new ParameterDriversList()).
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
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
                        estimatedMeasurementsParameters(new ParameterDriversList()).
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularRaDecMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

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
        initialOrbit.getJacobianWrtCartesian(positionAngle, dYdC);
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder measPropagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator rangePropagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                measPropagatorBuilder);
        final List<ObservedMeasurement<?>> rangeMeasurements =
                        EstimationTestUtils.createMeasurements(rangePropagator,
                                                               new RangeMeasurementCreator(context),
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
                                                               new RangeRateMeasurementCreator(context, false),
                                                               0.0, 4.0, 700.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(rangeMeasurements);
        measurements.addAll(angularMeasurements);
        measurements.addAll(rangeRateMeasurements);
        measurements.sort(Comparator.naturalOrder());

        // Reference propagator for estimation performances
        final NumericalPropagator referencePropagator = measPropagatorBuilder.
                        buildPropagator(measPropagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Biased propagator for the Kalman
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, false,
                                              minStep, maxStep, dP);
        
        // Cartesian covariance matrix initialization
        // Initial sigmas: 1000m on position, 0.01m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e6, 1e6, 1e6, 1e-4, 1e-4, 1e-4
        });
        
        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(positionAngle, dYdC);
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
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range & range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final NumericalPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
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
        final double   posEps            = 1.2e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 4.2e-10;
        final double[] expectedSigmasPos = {0.341528, 8.175341, 4.634528};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {1.167859e-3, 1.036492e-3, 2.834413e-3};
        final double   sigmaVelEps       = 1e-9;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }

    @Test
    public void testMultiSat() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
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
        closePropagator.setEphemerisMode();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = closePropagator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        measurements.addAll(EstimationTestUtils.createMeasurements(propagator1,
                                                               new RangeMeasurementCreator(context),
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
        Assert.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assert.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngle.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assert.assertEquals(4.7246,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getPosition(),
                                              before.getPVCoordinates().getPosition()),
                            1.0e-3);
        Assert.assertEquals(0.0010514,
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
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbits, new PositionAngle[] { PositionAngle.TRUE, PositionAngle.TRUE },
                                           new double[] { 38.3,  172.3 }, new double[] { 0.1,  0.1 },
                                           new double[] { 0.015, 0.068 }, new double[] { 1.0e-3, 1.0e-3 },
                                           new double[][] {
                                               { 5.6e5, 1.8e5, 1.9e5 },
                                               { 5.3e5, 1.8e5, 1.8e5 }
                                           }, new double[] { 1e4, 1e4 },
                                           new double[][] {
                                               { 8.8e2, 1.8e2, 3.1e2 },
                                               { 8.8e2, 1.8e2, 3.1e2 }
                                           }, new double[] { 1.0e1, 1.0e1 });

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(referenceDate), 1.0e-15);
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        // Build the Kalman filter
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder,
                                                  new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6)));
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        final KalmanEstimator kalman = kalmanBuilder.build();
        kalman.setObserver(estimation -> {
                throw new DummyException();
            });
        
        
        try {
            // Filter the measurements and expect an exception to occur
            EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               context.initialOrbit, positionAngle,
                                               0., 0.,
                                               0., 0.,
                                               new double[3], 0.,
                                               new double[3], 0.);
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


