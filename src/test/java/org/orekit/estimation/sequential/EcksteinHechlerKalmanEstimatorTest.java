package org.orekit.estimation.sequential;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.EcksteinHechlerContext;
import org.orekit.estimation.EcksteinHechlerEstimationTestUtils;
import org.orekit.estimation.measurements.EcksteinHechlerRangeMeasurementCreator;
import org.orekit.estimation.measurements.EcksteinHechlerRangeRateMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.utils.ParameterDriversList;


public class EcksteinHechlerKalmanEstimatorTest {

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
        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final double        dP            = 1.;
        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(dP);

        // Create perfect PV measurements
        final Orbit initialOrbit = context.initialOrbit;
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(initialOrbit,
                                                                                    propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements = EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                                                          new PVMeasurementCreator(),
                                                                                                          0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final EcksteinHechlerPropagator referencePropagator = propagatorBuilder.
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
        final double   posEps            = 5.74e-4; // With numerical propagator: 5.80e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.45e-6; // With numerical propagator: 2.28e-11;
        EcksteinHechlerEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
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
        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final double        dP            = 1.;
        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(dP);

        // Create perfect range measurements
        Orbit initialOrbit = context.initialOrbit;
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                              new EcksteinHechlerRangeMeasurementCreator(context),
                                                                              0.0, 2, 360);

        // Reference propagator for estimation performances
        final EcksteinHechlerPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-5, 1e-5, 1e-5
        });        

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.1e-7; // With numerical propagator: 5.80e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 4.3e-11; // With numerical propagator: 2.28e-11;
        EcksteinHechlerEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
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
        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final double        dP            = 1.;
        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(dP);

        // Create perfect range & range rate measurements
        Orbit initialOrbit = context.initialOrbit;
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurementsRange =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                        new EcksteinHechlerRangeMeasurementCreator(context),
                                                                        1.0, 3.0, 300.0);

        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                        new EcksteinHechlerRangeRateMeasurementCreator(context, false, 3.2e-10),
                                                                        1.0, 3.0, 300.0);

        // Concatenate measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // Reference propagator for estimation performances
        final EcksteinHechlerPropagator referencePropagator = propagatorBuilder.
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
        initialOrbit = orbitType.convertType(initialOrbit);
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
        final double   posEps            = 0.65; // With numerical propagator: 1.2e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.23e-4; // With numerical propagator: 4.2e-10;
        EcksteinHechlerEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
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
        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final double        dP            = 1.;
        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(dP);

        // Create perfect range measurements
        Orbit initialOrbit = context.initialOrbit;
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                              new PVMeasurementCreator(),
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
            EcksteinHechlerEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                               initialOrbit, positionAngle,
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
