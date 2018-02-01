/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.AngularRaDecMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.estimation.sequential.KalmanEstimator;
import org.orekit.estimation.sequential.KalmanEstimatorBuilder;
import org.orekit.estimation.sequential.KalmanObserver;
import org.orekit.estimation.sequential.KalmanEstimator.KalmanEvaluation;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class KalmanEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     * Keplerian formalism
     * @throws OrekitException
     */
    @Test
    public void testKeplerianPV() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
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
     * @throws OrekitException
     */
    @Test
    public void testKeplerianRange() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.77e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.93e-8;
        final double[] expectedSigmasPos = {0.742488, 0.281910, 0.563217};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.206622e-4, 1.306669e-4, 1.293996e-4};
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
     * @throws OrekitException
     */
    @Test
    public void testKeplerianRangeWithOnBoardAntennaOffset() throws OrekitException {

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
                                                               new RangeMeasurementCreator(context, antennaPhaseCenter),
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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.57e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.29e-6;
        final double[] expectedSigmasPos = {1.105194, 0.930785, 1.254579};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {6.193718e-4, 4.088774e-4, 3.299135e-4};
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
     * @throws OrekitException
     */
    @Test
    public void testCartesianRangeRate() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 9.50e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 3.49e-7;
        final double[] expectedSigmasPos = {0.324398, 1.347031, 1.743310};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.856883e-4,  5.765844e-4, 5.056186e-4};
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
     * @throws OrekitException
     */
    @Test
    public void testCircularAzimuthElevation() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
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
     * @throws OrekitException
     */
    @Test
    public void testEquinoctialRightAscensionDeclination() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.53e-5;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.04e-9;
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
    
    /** Perfect Range, Azel and range rate measurements with a biased start
     *  Start: position/velocity biased with: [+1000,0,0] m and [0,0,0.01] m/s
     *  Keplerian formalism
     */
    @Test
    public void testKeplerianRangeAzElAndRangeRate() throws OrekitException {

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
        measurements.sort(new ChronologicalComparator());

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 2.91e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.52e-6;
        final double[] expectedSigmasPos = {1.747570, 0.666879, 1.696182};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {5.413666e-4, 4.088359e-4, 4.315316e-4};
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
     * @throws OrekitException
     */
    @Test
    public void testKeplerianRangeAndRangeRate() throws OrekitException {

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(initialP);
        kalmanBuilder.processNoiseMatrix(Q);
        final KalmanEstimator kalman = kalmanBuilder.build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.96e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.06e-6;
        final double[] expectedSigmasPos = {0.341538, 8.175281, 4.634384};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {1.167838e-3, 1.036437e-3, 2.834385e-3};
        final double   sigmaVelEps       = 1e-9;
        EstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }
   
    /**
     * Test of a wrapped exception in a Kalman observer
     * @throws OrekitException
     */
    @Test
    public void testWrappedException() throws OrekitException {

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
        kalmanBuilder.builder(propagatorBuilder);
        kalmanBuilder.estimatedMeasurementsParameters(new ParameterDriversList());
        kalmanBuilder.initialCovarianceMatrix(MatrixUtils.createRealMatrix(6, 6));
        kalmanBuilder.processNoiseMatrix(MatrixUtils.createRealMatrix(6, 6));
        final KalmanEstimator kalman = kalmanBuilder.build();
        kalman.setObserver(new KalmanObserver() {
            /** {@inheritDoc} */
            @Override
            public void evaluationPerformed(Orbit[] predictedOrbits,
                                            Orbit[] estimatedOrbits,
                                            ParameterDriversList estimatedOrbitalParameters,
                                            ParameterDriversList estimatedPropagationParameters,
                                            ParameterDriversList estimatedMeasurementsParameters,
                                            EstimatedMeasurement<?>  predictedMeasurement,
                                            EstimatedMeasurement<?>  estimatedMeasurement,
                                            KalmanEvaluation kalmanEvaluation) throws DummyException {
                throw new DummyException();
            }
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


