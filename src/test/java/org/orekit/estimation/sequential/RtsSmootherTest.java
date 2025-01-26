package org.orekit.estimation.sequential;

import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

import java.util.List;

public class RtsSmootherTest {

    @Test
    void testNoUpdates() {
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType orbitType     = OrbitType.KEPLERIAN;
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

        // Add smoother
        final RtsSmoother rtsSmoother = new RtsSmoother(kalman);
        kalman.setObserver(rtsSmoother);

        // No measurements processed - should throw
        Assertions.assertThrows(MathIllegalStateException.class, rtsSmoother::backwardsSmooth);

        // Single measurement processed - should be OK
        kalman.estimationStep(measurements.get(0));
        Assertions.assertDoesNotThrow(rtsSmoother::backwardsSmooth);
    }
}
