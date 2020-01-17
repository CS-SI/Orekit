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
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.DSSTRangeMeasurementCreator;
import org.orekit.estimation.measurements.DSSTRangeRateMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class DSSTKalmanEstimatorTest {

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
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart,
                                                                      minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
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
        final double   posEps            = 1.1e-7; // With numerical propagator: 5.80e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 4.3e-11; // With numerical propagator: 2.28e-11;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Perfect range measurements with a biased start
     * Keplerian formalism
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
                                                               1.0, 4.0, 60.0);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        
        // Equinoctial initial covariance matrix
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
        final double   posEps            = 1.11e-4; // With numerical propagator: 1.77e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 4.46e-8; // With numerical propagator: 7.93e-8;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     * Keplerian formalism 
     */
    @Test
    public void testKeplerianRangeWithOnBoardAntennaOffset() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(perfectStart,
                                              minStep, maxStep, dP);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        
        // Antenna phase center definition
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);
        
        // Create perfect range measurements with antenna offset
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context, antennaPhaseCenter, 0.0),
                                                               1.0, 3.0, 300.0);

        // Add antenna offset to the measurements
        final OnBoardAntennaRangeModifier obaModifier = new OnBoardAntennaRangeModifier(antennaPhaseCenter);
        for (final ObservedMeasurement<?> range : measurements) {
            ((Range) range).addModifier(obaModifier);
        }
        
        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
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
        final Orbit initialOrbit = OrbitType.EQUINOCTIAL.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        
        // Equinoctial initial covariance matrix
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
        final double   posEps            = 4.57e-3; // With numerical propagator: 4.57e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 7.82e-6; // With numerical propagator: 7.29e-6;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }
    
    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testKeplerianRangeAndRangeRate() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range & range rate measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
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
        final double   posEps            = 6.21e-3; // With numerical propagator: 1.2e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.15e-6; // With numerical propagator: 4.2e-10;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /**
     * Test of a wrapped exception in a Kalman observer
     */
    @Test
    public void testWrappedException() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
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
            DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               context.initialOrbit, positionAngle,
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
