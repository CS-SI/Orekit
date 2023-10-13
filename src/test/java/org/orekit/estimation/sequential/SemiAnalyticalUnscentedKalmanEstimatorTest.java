package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class SemiAnalyticalUnscentedKalmanEstimatorTest {

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Residuals statistics. */
        private StreamingStatistics stats;

        /** Constructor. */
        public Observer() {
            this.stats = new StreamingStatistics();
        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getPredictedMeasurement();

            // Check
            if (estimatedMeasurement.getObservedMeasurement() instanceof Range) {
                final double[] estimated = estimatedMeasurement.getEstimatedValue();
                final double[] observed  = estimatedMeasurement.getObservedValue();
                // Calculate residual
                final double res = observed[0] - estimated[0];
                stats.addValue(res);
            }

        }

        /** Get the mean value of the residual.
         * @return the mean value of the residual in meters
         */
        public double getMeanResidual() {
            return stats.getMean();
        }

    }

    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
            build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
        	Assertions.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    @Test
    public void testMissingUnscentedTransform() {
        try {
            DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");
            final boolean       perfectStart  = true;
            final double        minStep       = 1.e-6;
            final double        maxStep       = 60.;
            final double        dP            = 1.;
            final DSSTPropagatorBuilder propagatorBuilder =
                            context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart,
                                                  minStep, maxStep, dP);
            new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
            addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(MatrixUtils.createRealMatrix(6, 6))).
            build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
        	Assertions.assertEquals(OrekitMessages.NO_UNSCENTED_TRANSFORM_CONFIGURED, oe.getSpecifier());
        }
    }

    @Test
    public void testKeplerianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and DSST propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.0e-15;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.0e-15;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 4.99e-8);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());

    }

    @Test
    public void testRangeWithZonal() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        builder.addForceModel(new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0)));

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0)));

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(new MerweUnscentedTransform(6)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.1e-7;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 3.9e-11;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 2.59e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());

    }

    @Test
    public void testRangeWithTesseral() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final UnnormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getUnnormalizedProvider(2, 2);
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        builder.addForceModel(new DSSTZonal(gravityField));
        builder.addForceModel(new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                gravityField.getMaxDegree(),
                gravityField.getMaxOrder(), 2,  FastMath.min(12, gravityField.getMaxDegree() + 2),
                gravityField.getMaxDegree(), gravityField.getMaxOrder(), FastMath.min(4, gravityField.getMaxDegree() - 2)));

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();
        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(new DSSTZonal(gravityField));
        propagatorBuilder.addForceModel(new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                gravityField.getMaxDegree(),
                gravityField.getMaxOrder(), 2,  FastMath.min(12, gravityField.getMaxDegree() + 2),
                gravityField.getMaxDegree(), gravityField.getMaxOrder(), FastMath.min(4, gravityField.getMaxDegree() - 2)));

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
        
        final MerweUnscentedTransform utProvider = new MerweUnscentedTransform(6, 0.5, 2., 0.);
        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        unscentedTransformProvider(utProvider).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.2e-9;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.7e-12;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 2.55e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());

    }

}
