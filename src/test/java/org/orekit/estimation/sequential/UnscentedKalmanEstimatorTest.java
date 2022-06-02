package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.UnscentedEstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.TimeStampedPVCoordinates;

public class UnscentedKalmanEstimatorTest {


    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new UnscentedKalmanEstimatorBuilder().
            build();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        System.out.println(context.initialOrbit);
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
        // final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double[] {0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001}); 
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6);
        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.80e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.28e-4;
        final double[] expectedsigmasPos = {0., 0., 0.};
        final double   sigmaPosEps       = 1e-4;
        final double[] expectedSigmasVel = {0., 0., 0.};
        final double   sigmaVelEps       = 1e-4;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final double        sigmaPos      = 10.;
        final double        sigmaVel      = 0.1;
        
        
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);
        
        // Create shifted initial state
        final Vector3D initialPosShifted = context.initialOrbit.getPVCoordinates().getPosition().add(new Vector3D(sigmaPos, sigmaPos, sigmaPos));
        final Vector3D initialVelShifted = context.initialOrbit.getPVCoordinates().getVelocity().add(new Vector3D(sigmaVel, sigmaVel, sigmaVel));

        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(context.initialOrbit.getDate(), initialPosShifted, initialVelShifted);
        
        final KeplerianOrbit shiftedOrbit = new KeplerianOrbit(pv, context.initialOrbit.getFrame(), context.initialOrbit.getMu());
        

        // Create perfect PV measurements
        System.out.println(context.initialOrbit);
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
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);

        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double[] {sigmaPos*sigmaPos, sigmaPos*sigmaPos, sigmaPos*sigmaPos, sigmaVel*sigmaVel, sigmaVel*sigmaVel, sigmaVel*sigmaVel}); 
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        
        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));
        // final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6);
        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
        
        propagatorBuilder.resetOrbit(shiftedOrbit);
        // Build the Kalman filter
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.6e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 8e-6;
        final double[] expectedsigmasPos = {0., 0., 0.};
        final double   sigmaPosEps       = 0.5;
        final double[] expectedSigmasVel = {0., 0., 0.};
        final double   sigmaVelEps       = 0.5;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
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
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.80e-5;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.28e-5;
        final double[] expectedsigmasPos = {0., 0., 0.};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {0., 0., 0.};
        final double   sigmaVelEps       = 1e-6;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
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
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.80e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.28e-6;
        final double[] expectedsigmasPos = {0., 0., 0.};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {0., 0., 0.};
        final double   sigmaVelEps       = 1e-6;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedsigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
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
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.5e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.1e-8;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765933e-4, 5.056124e-4};
        final double   sigmaVelEps       = 1e-9;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
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
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = UnscentedEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                UnscentedEstimationTestUtils.createMeasurements(propagator,
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
        final UnscentedKalmanEstimator kalman = new UnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 9.1e-5;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.2e-8;
        final double[] expectedSigmasPos = {0.356902, 1.297507, 1.798551};
        final double   sigmaPosEps       = 1.5e-2;
        final double[] expectedSigmasVel = {2.468745e-4, 5.810027e-4, 3.887394e-4};
        final double   sigmaVelEps       = 1e-5;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }
    

}
