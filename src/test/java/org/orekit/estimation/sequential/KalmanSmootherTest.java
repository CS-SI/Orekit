package org.orekit.estimation.sequential;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

import java.util.List;

public class KalmanSmootherTest {

    /**
     * Perfect range rate measurements with a perfect start
     * Cartesian formalism
     */
    @Test
    public void testCartesianRangeRate() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType orbitType     = OrbitType.CARTESIAN;
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
        //final AbstractSequentialEstimator kalmanEstimator = new KalmanEstimatorBuilder().
        //        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
        //        build();
        final AbstractSequentialEstimator kalmanEstimator = new UnscentedKalmanEstimatorBuilder()
                .addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q))
                .unscentedTransformProvider(new MerweUnscentedTransform(6))
                .build();
        final KalmanSmoother kalmanSmoother = new KalmanSmoother(kalmanEstimator);

        // Observer to print out debugging info
        kalmanSmoother.setObserver(estimation -> {
            System.out.printf("%22.15e", kalmanSmoother.getKalmanFilter().getCorrected().getTime());

            csvVector(estimation.getPhysicalPredictedState());
            csvMatrix(estimation.getPhysicalPredictedCovarianceMatrix());
            csvMatrix(estimation.getPhysicalStateCrossCovariance());
            csvVector(estimation.getPhysicalEstimatedState());
            csvMatrix(estimation.getPhysicalEstimatedCovarianceMatrix());
            System.out.println();
        });

        // Print initial state
        //final KalmanModel testModel = (KalmanModel) kalmanSmoother.getP
        System.out.printf("%22.15e", kalmanSmoother.getKalmanFilter().getCorrected().getTime());
        csvVector(MatrixUtils.createRealVector(6));
        csvMatrix(MatrixUtils.createRealMatrix(6, 6));
        csvMatrix(MatrixUtils.createRealMatrix(6, 6));
        csvVector(kalmanSmoother.getPhysicalEstimatedState());
        csvMatrix(kalmanSmoother.getPhysicalEstimatedCovarianceMatrix());
        System.out.println();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.5e-6;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 5.1e-10;
        final double[] expectedSigmasPos = {0.324407, 1.347014, 1.743326};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {2.85688e-4,  5.765933e-4, 5.056124e-4};
        final double   sigmaVelEps       = 1e-10;
        checkSmootherFit(context, kalmanSmoother, measurements,
                refOrbit, positionAngleType,
                expectedDeltaPos, posEps,
                expectedDeltaVel, velEps,
                expectedSigmasPos, sigmaPosEps,
                expectedSigmasVel, sigmaVelEps);

        // Test backwards smoothing
        List<PhysicalEstimatedState> smoothedStates = kalmanSmoother.backwardsSmooth();

        // Print out debugging info
        System.out.println();
        AbsoluteDate startDate = smoothedStates.get(0).getDate();
        for (PhysicalEstimatedState state : smoothedStates) {
            double dt = state.durationFrom(startDate);
            System.out.printf("%22.15e", dt);
            csvVector(state.getState());
            csvMatrix(state.getCovarianceMatrix());
            System.out.println();
        }

    }


    public static void csvMatrix(final RealMatrix matrix) {
        for (int row = 0; row < matrix.getRowDimension(); row++) {
            for (int col = 0; col < matrix.getColumnDimension(); col++) {
                System.out.printf(", %22.15e", matrix.getEntry(row, col));
            }
        }
    }

    public static void csvVector(final RealVector vector) {
        for (int row = 0; row < vector.getDimension(); row++) {
            System.out.printf(", %22.15e", vector.getEntry(row));
        }
    }



    /**
     * Checker for Kalman estimator validation
     * @param context context used for the test
     * @param kalman Kalman filter
     * @param measurements List of observed measurements to be processed by the Kalman
     * @param refOrbit Reference orbits at last measurement date
     * @param expectedDeltaPos Expected position difference between estimated orbit and reference orbit
     * @param posEps Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and reference orbit
     * @param velEps Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps Tolerance on expected covariance matrix on velocity
     */
    public static void checkSmootherFit(final Context context, final KalmanSmoother kalman,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit refOrbit, final PositionAngleType positionAngleType,
                                      final double expectedDeltaPos, final double posEps,
                                      final double expectedDeltaVel, final double velEps,
                                      final double[] expectedSigmasPos,final double sigmaPosEps,
                                      final double[] expectedSigmasVel,final double sigmaVelEps)
    {
        checkSmootherFit(context, kalman, measurements,
                new Orbit[] { refOrbit },
                new PositionAngleType[] {positionAngleType},
                new double[] { expectedDeltaPos }, new double[] { posEps },
                new double[] { expectedDeltaVel }, new double[] { velEps },
                new double[][] { expectedSigmasPos }, new double[] { sigmaPosEps },
                new double[][] { expectedSigmasVel }, new double[] { sigmaVelEps });
    }

    /**
     * Checker for Kalman estimator validation
     * @param context context used for the test
     * @param kalman Kalman filter
     * @param measurements List of observed measurements to be processed by the Kalman
     * @param refOrbit Reference orbits at last measurement date
     * @param expectedDeltaPos Expected position difference between estimated orbit and reference orbits
     * @param posEps Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and reference orbits
     * @param velEps Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps Tolerance on expected covariance matrix on velocity
     */
    public static void checkSmootherFit(final Context context, final KalmanSmoother kalman,
                                        final List<ObservedMeasurement<?>> measurements,
                                        final Orbit[] refOrbit, final PositionAngleType[] positionAngleType,
                                        final double[] expectedDeltaPos, final double[] posEps,
                                        final double[] expectedDeltaVel, final double []velEps,
                                        final double[][] expectedSigmasPos,final double[] sigmaPosEps,
                                        final double[][] expectedSigmasVel,final double[] sigmaVelEps)
    {

        // Add the measurements to the Kalman filter
        Propagator[] estimated = kalman.processMeasurements(measurements);

        /*
        // Check the number of measurements processed by the filter
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());

        for (int k = 0; k < refOrbit.length; ++k) {
            // Get the last estimation
            final Orbit    estimatedOrbit    = estimated[k].getInitialState().getOrbit();
            final Vector3D estimatedPosition = estimatedOrbit.getPosition();
            final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();

            // Get the last covariance matrix estimation
            final RealMatrix estimatedP = kalman.getPhysicalEstimatedCovarianceMatrix();

            // Convert the orbital part to Cartesian formalism
            // Assuming all 6 orbital parameters are estimated by the filter
            final double[][] dCdY = new double[6][6];
            estimatedOrbit.getJacobianWrtParameters(positionAngleType[k], dCdY);
            final RealMatrix Jacobian = MatrixUtils.createRealMatrix(dCdY);
            final RealMatrix estimatedCartesianP =
                    Jacobian.
                            multiply(estimatedP.getSubMatrix(0, 5, 0, 5)).
                            multiply(Jacobian.transpose());

            // Get the final sigmas (ie.sqrt of the diagonal of the Cartesian orbital covariance matrix)
            final double[] sigmas = new double[6];
            for (int i = 0; i < 6; i++) {
                sigmas[i] = FastMath.sqrt(estimatedCartesianP.getEntry(i, i));
            }
//          // FIXME: debug print values
//          final double dPos = Vector3D.distance(refOrbit[k].getPosition(), estimatedPosition);
//          final double dVel = Vector3D.distance(refOrbit[k].getPVCoordinates().getVelocity(), estimatedVelocity);
//          System.out.println("Nb Meas = " + kalman.getCurrentMeasurementNumber());
//          System.out.println("dPos    = " + dPos + " m");
//          System.out.println("dVel    = " + dVel + " m/s");
//          System.out.println("sigmas  = " + sigmas[0] + " "
//                          + sigmas[1] + " "
//                          + sigmas[2] + " "
//                          + sigmas[3] + " "
//                          + sigmas[4] + " "
//                          + sigmas[5]);
//          //debug

            // Check the final orbit estimation & PV sigmas
            final double deltaPosK = Vector3D.distance(refOrbit[k].getPosition(), estimatedPosition);
            final double deltaVelK = Vector3D.distance(refOrbit[k].getPVCoordinates().getVelocity(), estimatedVelocity);
            Assertions.assertEquals(expectedDeltaPos[k], deltaPosK, posEps[k]);
            Assertions.assertEquals(expectedDeltaVel[k], deltaVelK, velEps[k]);

            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(expectedSigmasPos[k][i], sigmas[i],   sigmaPosEps[k]);
                Assertions.assertEquals(expectedSigmasVel[k][i], sigmas[i+3], sigmaVelEps[k]);
            }
        }

         */
    }
}
