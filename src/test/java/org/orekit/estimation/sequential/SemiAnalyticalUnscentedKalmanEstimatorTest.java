package org.orekit.estimation.sequential;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

public class SemiAnalyticalUnscentedKalmanEstimatorTest {

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Residuals statistics. */
        private StreamingStatistics stats;
        
        private List<Double> residualsID;
        private List<Double> residualsS;

        private List<Double> timeID;
        private List<Double> timeS;

        private Boolean isFirstEvaluation;
        private AbsoluteDate initialDate;

        /** Constructor. */
        public Observer() {
            this.stats             = new StreamingStatistics();
            this.residualsID       = new ArrayList<>();
            this.residualsS        = new ArrayList<>();
            this.timeID            = new ArrayList<>();
            this.timeS             = new ArrayList<>();
            this.isFirstEvaluation = true;
        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getPredictedMeasurement();
            if (isFirstEvaluation) {
                initialDate = estimatedMeasurement.getDate();
                isFirstEvaluation = false;
            }
            // Check
            if (estimatedMeasurement.getObservedMeasurement() instanceof Range) {
                final double[] estimated = estimatedMeasurement.getEstimatedValue();
                final double[] observed  = estimatedMeasurement.getObservedValue();
                // Calculate residual
                final double res = observed[0] - estimated[0];
                stats.addValue(res);
                System.out.println(((Range)estimatedMeasurement.getObservedMeasurement()).getStation().getBaseFrame().getName());
                if (((Range)estimatedMeasurement.getObservedMeasurement()).getStation().getBaseFrame().getName().equals("Isla Desolación")) {
                    residualsID.add(res);
                    timeID.add(estimatedMeasurement.getDate().durationFrom(initialDate)/86400);
                }
                else {
                    residualsS.add(res);
                    timeS.add(estimatedMeasurement.getDate().durationFrom(initialDate)/86400);
                }
            }

        }

        /** Get the mean value of the residual.
         * @return the mean value of the residual in meters
         */
        public double getMeanResidual() {
            return stats.getMean();
        }
        
        public void printResults(String path, String path2) {
            PrintWriter writerID = null;
            PrintWriter writerS = null;
            try {
                String encoding = "UTF-8";
                writerID = new PrintWriter(path, encoding); 
                writerS = new PrintWriter(path2, encoding); 
            }
            catch (IOException e){
                System.out.println("An error occurred.");
                e.printStackTrace();
              }
            for (int i = 0; i < residualsID.size(); i++) {
                writerID.print(timeID.get(i));
                writerID.print(" ");
                writerID.println(residualsID.get(i));
            }
            for (int i = 0; i < residualsS.size(); i++) {
                writerS.print(timeS.get(i));
                writerS.print(" ");
                writerS.println(residualsS.get(i));
            }
            writerS.close();
            writerID.close();
        }
    }
    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
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
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart,
                                              minStep, maxStep, dP);
        final DSSTPropagatorBuilder propagatorBuilderRef =
                context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart,
                                      minStep, maxStep, dP);
        // Create perfect PV measurements
        System.out.println("Initial orbit: " + context.initialOrbit);
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        System.out.println("Initial orbit in the builder: " + propagator.getInitialState());
        final List<ObservedMeasurement<?>> measurements =
                DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 6.0, 60.0);
        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilderRef.
                        buildPropagator(propagatorBuilderRef.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Covariance matrix initialization
        // final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double[] {0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001}); 
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6);
        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  
        
        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4e-9;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.5e-12;

        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    
    /**
     * Perfect Range measurements with a perfect start.
     */
    @Test
    public void testCartesianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               0.0, 1.0, 300.0);
        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();
        
        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6); 

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);
  

        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4e-3;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.28e-5;

        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }
    
    /**
     * Perfect range rate measurements with a perfect start
     * Cartesian formalism
     */
    @Test
    public void testCartesianRangeRate() {

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
                        context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double satClkDrift = 3.2e-10;
        final RangeRateMeasurementCreator creator = new RangeRateMeasurementCreator(context, false, satClkDrift);
        final List<ObservedMeasurement<?>> measurements =
                DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               creator,
                                                               1.0, 3.0, 300.0);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
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
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        
        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4e-4;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2e-7;

        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }
    
    /**
     * Perfect range measurements.
     * Only the Newtonian Attraction is used.
     * Case 1 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and DSST propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new RangeMeasurementCreator(context),
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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 6e-9;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2e-12;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assert.assertEquals(0.0, observer.getMeanResidual(), 5.38e-8);
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assert.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assert.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assert.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assert.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assert.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assert.assertNotNull(kalman.getPhysicalEstimatedState());
        observer.printResults("../../python-workspace/residuals_USKF_case_1_ID.txt", "../../python-workspace/residuals_USKF_case_1_S.txt");

    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model compare to the previous test
     * Case 2 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testRangeWithZonal() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new RangeMeasurementCreator(context),
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

        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);

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
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 7.6e-1;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.5e-4;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        //Assert.assertEquals(0.0, observer.getMeanResidual(), 8.51e-3);
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assert.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assert.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assert.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assert.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assert.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assert.assertNotNull(kalman.getPhysicalEstimatedState());


        observer.printResults("../../python-workspace/residuals_USKF_case_2_ID.txt", "../../python-workspace/residuals_USKF_case_2_S.txt");
    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model
     * In addition, J21 and J22 are also added
     * Case 3 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testRangeWithTesseral() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new RangeMeasurementCreator(context),
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

        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity 
        // Process noise matrix is set to 0 here
//        final RealMatrix cartesianP = MatrixUtils.createRealMatrix(6, 6);
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
        
        final MerweUnscentedTransform utProvider = new MerweUnscentedTransform(6, 0.5, 2., 0.);
        // Build the Kalman filter
        final SemiAnalyticalUnscentedKalmanEstimator kalman = new SemiAnalyticalUnscentedKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).unscentedTransformProvider(utProvider).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.8e-1;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.75e-4;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        //Assert.assertEquals(0.0, observer.getMeanResidual(), 8.81e-3);
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assert.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assert.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assert.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assert.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assert.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assert.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assert.assertNotNull(kalman.getPhysicalEstimatedState());
        
        observer.printResults("../../python-workspace/residuals_USKF_case_3_ID.txt", "../../python-workspace/residuals_USKF_case_3_S.txt");

    }
}
