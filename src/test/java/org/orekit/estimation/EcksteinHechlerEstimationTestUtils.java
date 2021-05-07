package org.orekit.estimation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer.Optimum;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.KalmanEstimator;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

public class EcksteinHechlerEstimationTestUtils {
    
    /**
     * Build an Eckstein-Hechler model
     * @param dataRoot data root
     * @return a configured context for the test
     */
    public static EcksteinHechlerContext myContext(final String dataRoot) {

        Utils.setDataRoot(dataRoot);
        EcksteinHechlerContext context = new EcksteinHechlerContext();
        context.conventions = IERSConventions.IERS_2010;
        context.utc = TimeScalesFactory.getUTC();
        context.ut1 = TimeScalesFactory.getUT1(context.conventions, true);
        context.displacements = new StationDisplacement[0];

        // Earth is spherical, rotating in one sidereal day
        context.earth   = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               FramesFactory.getITRF(context.conventions, true));

        // 6x0 gravity field (Consistent with Eckstein-Hechler model)
        context.gravity = GravityFieldFactory.getUnnormalizedProvider(6, 0);
        
        // Stations
        context.stations = Arrays.asList(context.createStation(51.50, -0.13, 200.0, "London"),
                                         context.createStation(-17.75, 296.85, 416.0, "Santa Cruz"));
        
        // TLE
        String line1 = "1 24876U 97035A   21123.52874340  .00000013  00000-0  00000-0 0  9997";
        String line2 = "2 24876  55.4682 171.6637 0047342  54.5360 305.9711  2.00562746174430";
        TLE tle = new TLE(line1, line2);
        
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        AbsoluteDate initDate = tle.getDate();
        
        // Geo-stationary Satellite Orbit, tightly above the station (l0-L0)
        context.initialOrbit = new KeplerianOrbit(propagator.getPVCoordinates(initDate, FramesFactory.getEME2000()),
                                                  FramesFactory.getEME2000(),
                                                  initDate,
                                                  context.gravity.getMu());

        return context;

    }

    /**
     * Create the propagator
     * @param initialOrbit initial orbit to initialize the propagator
     * @param propagatorBuilder propagator builder
     * @return the configured propagator
     */
    public static Propagator createPropagator(final Orbit initialOrbit,
                                              final PropagatorBuilder propagatorBuilder) {

        // override orbital parameters
        double[] orbitArray = new double[6];

         propagatorBuilder.getOrbitType().mapOrbitToArray(initialOrbit, propagatorBuilder.getPositionAngle(),
                                                          orbitArray, null);
        for (int i = 0; i < orbitArray.length; ++i) {
            propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(i).setValue(orbitArray[i]);
        }

        return propagatorBuilder.buildPropagator(propagatorBuilder.getSelectedNormalizedParameters()); 

    }

    /**
     * Create the measurements used for the validation
     * @param propagator propagator used to generate the measurements
     * @param creator measurement creator
     * @param startPeriod start period 
     * @param endPeriod end period
     * @param step measurement step
     * @return the list of observed measurements
     */
    public static List<ObservedMeasurement<?>> createMeasurements(final Propagator propagator,
                                                                  final MeasurementCreator creator,
                                                                  final double startPeriod, final double endPeriod,
                                                                  final double step) {

        propagator.setMasterMode(step, creator);
        final double       period = propagator.getInitialState().getKeplerianPeriod();
        final AbsoluteDate start  = propagator.getInitialState().getDate().shiftedBy(startPeriod * period);
        final AbsoluteDate end    = propagator.getInitialState().getDate().shiftedBy(endPeriod   * period);
        propagator.propagate(start, end);

        final List<ObservedMeasurement<?>> measurements = creator.getMeasurements();

        for (final ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(propagator.getInitialState().getDate());
                }
            }
        }

        return measurements;

    }
    
    /**
     * Checker for batch LS estimator validation
     * @param context EcksteinHechlerContext used for the test
     * @param estimator Batch LS estimator
     * @param iterations Number of iterations expected
     * @param evaluations Number of evaluations expected
     * @param expectedRMS Expected RMS value
     * @param rmsEps Tolerance on expected RMS
     * @param expectedMax Expected weighted residual maximum
     * @param maxEps Tolerance on weighted residual maximum
     * @param expectedDeltaPos Expected position difference between estimated orbit and initial orbit
     * @param posEps Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and initial orbit
     * @param velEps Tolerance on expected velocity difference
     */
    public static void checkFit(final EcksteinHechlerContext context, final BatchLSEstimator estimator,
                                final int iterations, final int evaluations,
                                final double expectedRMS,      final double rmsEps,
                                final double expectedMax,      final double maxEps,
                                final double expectedDeltaPos, final double posEps,
                                final double expectedDeltaVel, final double velEps) {

        final Orbit initialOrbit = context.initialOrbit;
        final Orbit estimatedOrbit = estimator.estimate()[0].getInitialState().getOrbit();
        final Vector3D estimatedPosition = estimatedOrbit.getPVCoordinates().getPosition();
        final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();

        Assert.assertEquals(iterations, estimator.getIterationsCount());
        Assert.assertEquals(evaluations, estimator.getEvaluationsCount());
        Optimum optimum = estimator.getOptimum();
        Assert.assertEquals(iterations, optimum.getIterations());
        Assert.assertEquals(evaluations, optimum.getEvaluations());

        int    k   = 0;
        double sum = 0;
        double max = 0;
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry :
             estimator.getLastEstimations().entrySet()) {
            final ObservedMeasurement<?>  m = entry.getKey();
            final EstimatedMeasurement<?> e = entry.getValue();
            final double[]    weight      = m.getBaseWeight();
            final double[]    sigma       = m.getTheoreticalStandardDeviation();
            final double[]    observed    = m.getObservedValue();
            final double[]    theoretical = e.getEstimatedValue();
            for (int i = 0; i < m.getDimension(); ++i) {
                final double weightedResidual = weight[i] * (theoretical[i] - observed[i]) / sigma[i];
                ++k;
                sum += weightedResidual * weightedResidual;
                max = FastMath.max(max, FastMath.abs(weightedResidual));
            }
        }

        Assert.assertEquals(expectedRMS,
                            FastMath.sqrt(sum / k),
                            rmsEps);
        Assert.assertEquals(expectedMax,
                            max,
                            maxEps);
        Assert.assertEquals(expectedDeltaPos,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(), estimatedPosition),
                            posEps);
        Assert.assertEquals(expectedDeltaVel,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(), estimatedVelocity),
                            velEps);

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
     */
    public static void checkKalmanFit(final EcksteinHechlerContext context, final KalmanEstimator kalman,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit refOrbit, final PositionAngle positionAngle,
                                      final double expectedDeltaPos, final double posEps,
                                      final double expectedDeltaVel, final double velEps) {
        checkKalmanFit(context, kalman, measurements,
                       new Orbit[] { refOrbit },
                       new PositionAngle[] { positionAngle },
                       new double[] { expectedDeltaPos }, new double[] { posEps },
                       new double[] { expectedDeltaVel }, new double[] { velEps });
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
     */
    public static void checkKalmanFit(final EcksteinHechlerContext context, final KalmanEstimator kalman,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit[] refOrbit, final PositionAngle[] positionAngle,
                                      final double[] expectedDeltaPos, final double[] posEps,
                                      final double[] expectedDeltaVel, final double []velEps) {

        // Add the measurements to the Kalman filter
        Propagator[] estimated = kalman.processMeasurements(measurements);
        
        // Check the number of measurements processed by the filter
        Assert.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());

        for (int k = 0; k < refOrbit.length; ++k) {
            // Get the last estimation
            final Orbit    estimatedOrbit    = estimated[k].getInitialState().getOrbit();
            final Vector3D estimatedPosition = estimatedOrbit.getPVCoordinates().getPosition();
            final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();        

            // Get the last covariance matrix estimation
            final RealMatrix estimatedP = kalman.getPhysicalEstimatedCovarianceMatrix();

            // Convert the orbital part to Cartesian formalism
            // Assuming all 6 orbital parameters are estimated by the filter
            final double[][] dCdY = new double[6][6];
            estimatedOrbit.getJacobianWrtParameters(positionAngle[k], dCdY);
            final RealMatrix Jacobian = MatrixUtils.createRealMatrix(dCdY);
            // Cartesian orbital covariance matrix
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
//          final double dPos = Vector3D.distance(refOrbit[k].getPVCoordinates().getPosition(), estimatedPosition);
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
            final double deltaPosK = Vector3D.distance(refOrbit[k].getPVCoordinates().getPosition(), estimatedPosition);
            final double deltaVelK = Vector3D.distance(refOrbit[k].getPVCoordinates().getVelocity(), estimatedVelocity);
            Assert.assertEquals(expectedDeltaPos[k], deltaPosK, posEps[k]);
            Assert.assertEquals(expectedDeltaVel[k], deltaVelK, velEps[k]);

        }
    }
    
}