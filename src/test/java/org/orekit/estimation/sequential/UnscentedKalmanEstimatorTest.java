package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.MerweUnscentedTransform;
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
import org.orekit.orbits.CartesianOrbit;
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

    @Test
    public void testMissingUnscentedTransform() {
        try {
            Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");
            final OrbitType     orbitType     = OrbitType.CARTESIAN;
            final PositionAngle positionAngle = PositionAngle.TRUE;
            final boolean       perfectStart  = true;
            final double        minStep       = 1.e-6;
            final double        maxStep       = 60.;
            final double        dP            = 1.;
            final NumericalPropagatorBuilder propagatorBuilder =
                            context.createBuilder(orbitType, positionAngle, perfectStart,
                                                  minStep, maxStep, dP);
            new UnscentedKalmanEstimatorBuilder().
            addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6))).
            build();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_UNSCENTED_TRANSFORM_CONFIGURED, oe.getSpecifier());
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
        final double   posEps            = 5.08e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.98e-9;
        final double[] expectedsigmasPos = {3.179E-7, 2.995E-7, 10.276E-7};
        final double   sigmaPosEps       = 1.0e-10;
        final double[] expectedSigmasVel = {1.05689E-10, 3.89731E-10, 0.51126E-10};
        final double   sigmaVelEps       = 1.0e-15;
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
        final double        sigmaVel      = 0.01;

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(orbitType, positionAngle, perfectStart,
                                              minStep, maxStep, dP);
        
        // Create shifted initial state
        final Vector3D initialPosShifted = context.initialOrbit.getPVCoordinates().getPosition().add(new Vector3D(sigmaPos, sigmaPos, sigmaPos));
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
        final double   posEps            = 4.59e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.67e-6;
        final double[] expectedsigmasPos = {0.196283, 0.177933, 0.317294};
        final double   sigmaPosEps       = 1.0e-6;
        final double[] expectedSigmasVel = {7.34904E-5, 13.28603E-5, 4.28682E-5};
        final double   sigmaVelEps       = 1.0e-10;
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
        final double   posEps            = 6.39e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.53e-10;
        final double[] expectedsigmasPos = {0.4401258E-8, 8.6348156E-8, 13.6186622E-8};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {2.3262E-11, 0.9883E-11, 5.7841E-11};
        final double   sigmaVelEps       = 1.0e-15;
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
        final double   posEps            = 1.33e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 4.62e-10;
        final double[] expectedsigmasPos = {1.12928E-10, 15.00645E-10, 5.45261E-10};
        final double   sigmaPosEps       = 1.0e-15;
        final double[] expectedSigmasVel = {2.96E-13, 1.84E-13, 7.66E-13};
        final double   sigmaVelEps       = 1.0e-15;
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
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 7.44e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.74e-9;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765933e-4, 5.056124e-4};
        final double   sigmaVelEps       = 1e-10;
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
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 6.07e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.50e-10;
        final double[] expectedSigmasPos = {0.043885, 0.600764, 0.279020};
        final double   sigmaPosEps       = 1.0e-6;
        final double[] expectedSigmasVel = {7.17260E-5, 3.037315E-5, 19.49046e-5};
        final double   sigmaVelEps       = 1.0e-10;
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
    public void testCircularAzimuthElevation() {

        // Create context
        Context context = UnscentedEstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 5.93e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.02e-10;
        final double[] expectedSigmasPos = {0.012134, 0.511243, 0.264925};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {5.72891E-5, 1.58811E-5, 15.98658E-5};
        final double   sigmaVelEps       = 1e-10;
        UnscentedEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps,
                                           expectedSigmasPos, sigmaPosEps,
                                           expectedSigmasVel, sigmaVelEps);
    }
    
}
