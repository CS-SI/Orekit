/* Copyright 2002-2026 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer.Optimum;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.KalmanEstimator;
import org.orekit.estimation.sequential.SemiAnalyticalKalmanEstimator;
import org.orekit.estimation.sequential.SemiAnalyticalUnscentedKalmanEstimator;
import org.orekit.estimation.sequential.UnscentedKalmanEstimator;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.forces.radiation.IsotropicRadiationClassicalConvention;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.orbits.*;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.intervals.ElevationDetectionAdaptableIntervalFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Utility class for orbit determination tests. */
public class EstimationTestUtils {

    /**
     * Generate a context with a high eccentricity (i.e. 0.125) orbit.
     * @param dataRoot path for the data
     * @return a context with a small eccentricity orbit
     */
    public static Context eccentricContext(final String dataRoot) {

        Utils.setDataRoot(dataRoot);
        Context context = new Context();
        context.conventions = IERSConventions.IERS_2010;
        context.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(context.conventions, true));
        context.sun = CelestialBodyFactory.getSun();
        context.moon = CelestialBodyFactory.getMoon();
        context.radiationSensitive = new IsotropicRadiationClassicalConvention(2.0, 0.2, 0.8);
        context.dragSensitive = new IsotropicDrag(2.0, 1.2);
        final EOPHistory eopHistory = FramesFactory.getEOPHistory(context.conventions, true);
        context.utc = TimeScalesFactory.getUTC();
        context.ut1 = TimeScalesFactory.getUT1(eopHistory);
        context.displacements = new StationDisplacement[] {
            new TidalDisplacement(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                  Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                  Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                  context.sun, context.moon,
                                  context.conventions, false)
        };
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        context.normalizedProvider = GravityFieldFactory.getNormalizedProvider(20, 20);
        context.unnormalizedProvider = GravityFieldFactory.getUnnormalizedProvider(context.normalizedProvider);
        context.initialOrbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                                  0.250, 1.375, 0.0625, PositionAngleType.TRUE,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, context.utc),
                                                  context.normalizedProvider.getMu());

        context.stations = Arrays.asList(context.createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                         context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur"));

        // Turn-around range stations
        // Map entry = primary station
        // Map value = secondary station associated
        context.TARstations = new HashMap<>();
        context.TARstations.put(context.createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                context.createStation(-54.815833,  -68.317778, 6.0, "Ushuaïa"));
        context.TARstations.put(context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur"),
                                context.createStation( 61.405833,   -6.705278,  470.0, "Sumba"));

        // Bistatic range rate stations
        // key/first    = emitter station
        // value/second = receiver station
        context.BRRstations = new Pair<>(context.createStation(40.0, 0.0, 0.0, "Emitter"),
                                         context.createStation(45.0, 0.0, 0.0, "Receiver"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.TDOAstations = new Pair<>(context.createStation(40.0, 0.0, 0.0, "TDOA_Prime"),
                                          context.createStation(45.0, 0.0, 0.0, "TDOA_Second"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.FDOAstations = context.TDOAstations;
        
        final Vector3D deltaP1 = new Vector3D(1000.0, 2000.0, 3000.0);
        final Vector3D deltaV1 = new Vector3D(0.1, 0.17, -0.03);
        final Vector3D deltaP2 = new Vector3D(-10000.0, -500.0, -6000.0);
        final Vector3D deltaV2 = new Vector3D(-0.1, 0.01, 0.3);

        context.satellites = Arrays.asList(
                context.createObserverSatellite(deltaP1, deltaV1, "satellite-1", Force.DRAG),
                context.createObserverSatellite(deltaP2, deltaV2, "satellite-2", Force.DRAG));

        return context;
    }

    /**
     * Generate a context with a high eccentricity (i.e. 0.125) orbit for DSST.
     * <p>
     * The method calls {@link #eccentricContext(String)} and only override the initial orbit.
     * </p>
     * @param dataRoot path for the data
     * @return a context with a small eccentricity orbit
     */
    // FIXME this method shall not exist. It highlights an issue with DSST-based orbit determination.
    public static Context dsstEccentricContext(final String dataRoot) {
        Context context = eccentricContext(dataRoot);
        Orbit orbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                         0.250, 1.375, 0.0625, PositionAngleType.MEAN,
                                         FramesFactory.getEME2000(),
                                         new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, context.utc),
                                         context.unnormalizedProvider.getMu());
        context.initialOrbit = OrbitType.EQUINOCTIAL.convertType(orbit);
        return context;
    }

    /**
     * Generate a context with a small eccentricity (i.e. 0.001) orbit.
     * <p>
     * The method calls {@link #eccentricContext(String)} and only override the initial orbit.
     * </p>
     * @param dataRoot path for the data
     * @return a context with a small eccentricity orbit
     */
    public static Context smallEccentricContext(final String dataRoot) {
        Context context = eccentricContext(dataRoot);
        context.initialOrbit = new KeplerianOrbit(15000000.0, 0.001, 1.25,
                0.250, 1.375, 0.0625, PositionAngleType.TRUE,
                FramesFactory.getEME2000(),
                new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, context.utc),
                context.unnormalizedProvider.getMu());
        return context;
    }

    /**
     * Generate a context from a TLE.
     * <p>
     * The method calls {@link #eccentricContext(String)} and only override the initial orbit with the TLE.
     * </p>
     * @param dataRoot path for the data
     * @return a context with a small eccentricity orbit
     */
    public static Context contextFromTle(final String dataRoot) {
        Context context = EstimationTestUtils.eccentricContext(dataRoot);
        String line1 = "1 07276U 74026A   00055.48318287  .00000000  00000-0  22970+3 0  9994";
        String line2 = "2 07276  71.6273  78.7838 1248323  14.0598   3.8405  4.72707036231812";
        context.initialTLE = new TLE(line1, line2);
        context.initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        return context;
    }

    /**
     * Create a GEO context.
     * @param dataRoot path for the data
     * @return a GEO context
     */
    public static Context geoStationnaryContext(final String dataRoot) {

        Utils.setDataRoot(dataRoot);
        Context context = new Context();
        context.conventions = IERSConventions.IERS_2010;
        context.utc = TimeScalesFactory.getUTC();
        context.ut1 = TimeScalesFactory.getUT1(context.conventions, true);
        context.displacements = new StationDisplacement[0];
        String Myframename = "MyEarthFrame";
        final AbsoluteDate datedef = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, context.utc);
        final double omega = Constants.WGS84_EARTH_ANGULAR_VELOCITY;
        final Vector3D rotationRate = new Vector3D(0.0, 0.0, omega);

        TransformProvider MyEarthFrame = new TransformProvider() {
            public Transform getTransform(final AbsoluteDate date) {
                final double rotationduration = date.durationFrom(datedef);
                final Vector3D alpharot = new Vector3D(rotationduration, rotationRate);
                final Rotation rotation = new Rotation(Vector3D.PLUS_K, -alpharot.getZ(),
                                                       RotationConvention.VECTOR_OPERATOR);
                return new Transform(date, rotation, rotationRate);
            }
            public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                final T rotationduration = date.durationFrom(datedef);
                final FieldVector3D<T> alpharot = new FieldVector3D<>(rotationduration, rotationRate);
                final FieldRotation<T> rotation = new FieldRotation<>(FieldVector3D.getPlusK(date.getField()),
                                                                      alpharot.getZ().negate(),
                                                                      RotationConvention.VECTOR_OPERATOR);
                return new FieldTransform<>(date, rotation, new FieldVector3D<>(date.getField(), rotationRate));
            }
        };
        Frame FrameTest = new Frame(FramesFactory.getEME2000(), MyEarthFrame, Myframename, true);

        // Earth is spherical, rotating in one sidereal day
        context.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0.0, FrameTest);
        context.sun = CelestialBodyFactory.getSun();
        context.moon = CelestialBodyFactory.getMoon();
        context.radiationSensitive = new IsotropicRadiationClassicalConvention(2.0, 0.2, 0.8);
        context.dragSensitive = new IsotropicDrag(2.0, 1.2);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        context.normalizedProvider = GravityFieldFactory.getNormalizedProvider(20, 20);

        // semimajor axis for a geostationnary satellite
        double da = FastMath.cbrt(context.normalizedProvider.getMu() / (omega * omega));

        context.stations = Collections.singletonList(context.createStation(0.0, 0.0, 0.0, "Lat0_Long0") );

        // Compute the frames transformation from station frame to EME2000
        Transform topoToEME = context.stations.get(0).getBaseFrame().getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, context.utc));

        // Station position in EME2000 at reference date
        Vector3D stationPositionEME = topoToEME.transformPosition(Vector3D.ZERO);

        // Satellite position and velocity in Station Frame
        final Vector3D sat_pos = new Vector3D(0., 0., da-stationPositionEME.getNorm());
        final Vector3D acceleration = new Vector3D(-context.normalizedProvider.getMu(), sat_pos);
        final PVCoordinates pv_sat_topo = new PVCoordinates(sat_pos, Vector3D.ZERO, acceleration);

        // satellite position in EME2000
        final PVCoordinates pv_sat_iner = topoToEME.transformPVCoordinates(pv_sat_topo);

        // Geo-stationary Satellite Orbit, tightly above the station (l0-L0)
        context.initialOrbit = new KeplerianOrbit(pv_sat_iner,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, context.utc),
                                                  context.normalizedProvider.getMu());

        context.stations = Collections.singletonList(context.createStation(10.0, 45.0, 0.0, "Lat10_Long45") );

        // Turn-around range stations
        // Map entry = primary station
        // Map value = secondary station associated
        context.TARstations = new HashMap<>();

        context.TARstations.put(context.createStation(  41.977, 13.600,  671.354, "Fucino"),
                                context.createStation(  43.604,  1.444,  263.0  , "Toulouse"));

        context.TARstations.put(context.createStation(  49.867,  8.65 ,  144.0  , "Darmstadt"),
                                context.createStation( -25.885, 27.707, 1566.633, "Pretoria"));

        // Bistatic range rate stations
        // key/first    = emitter station
        // value/second = receiver station
        context.BRRstations = new Pair<>(context.createStation(40.0, 0.0, 0.0, "Emitter"),
                                         context.createStation(45.0, 0.0, 0.0, "Receiver"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.TDOAstations = new Pair<>(context.createStation(40.0, 0.0, 0.0, "TDOA_Prime"),
                                          context.createStation(45.0, 0.0, 0.0, "TDOA_Second"));

        return context;
    }

    /**
     * Creates a propagator from the given initial orbit and propagator builder.
     *
     * @param initialOrbit the initial orbit
     * @param propagatorBuilder the propagator builder used to create the propagator
     * @return the propagator constructed with the specified initial orbit and configuration
     */
    public static Propagator createPropagator(final Orbit initialOrbit,
                                              final PropagatorBuilder propagatorBuilder) {

        // Override orbital parameters
        double[] orbitArray = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(initialOrbit,
                                                         propagatorBuilder.getPositionAngleType(),
                                                         orbitArray, null);
        for (int i = 0; i < orbitArray.length; ++i) {
        	// Here orbital parameters drivers have only 1 estimated values on the all time period for orbit determination
            propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(i).setValue(orbitArray[i]);
        }

        // Return the built propagator
        return propagatorBuilder.buildPropagator();
    }

    /**
     * Generates a list of observed measurements.
     *
     * @param propagator      the propagator used to compute the spacecraft state over the desired time period
     * @param creator         the measurement creator responsible for defining how measurements are produced
     * @param startPeriod     the start time of the measurement generation, expressed as a multiple of the orbit's Keplerian period
     * @param endPeriod       the end time of the measurement generation, expressed as a multiple of the orbit's Keplerian period
     * @param step            the time step between consecutive measurements, in seconds
     * @return a list of observed measurements generated over the specified time period
     */
    public static List<ObservedMeasurement<?>> createMeasurements(final Propagator propagator,
                                                                  final MeasurementCreator creator,
                                                                  final double startPeriod, final double endPeriod,
                                                                  final double step) {

        // Generates measurements during propagation
        propagator.setStepHandler(step, creator);
        final double       period = propagator.getInitialState().getOrbit().getKeplerianPeriod();
        final AbsoluteDate start  = propagator.getInitialState().getDate().shiftedBy(startPeriod * period);
        final AbsoluteDate end    = propagator.getInitialState().getDate().shiftedBy(endPeriod   * period);
        propagator.propagate(start, end);

        // Loop on measurements to update driver reference dates
        final List<ObservedMeasurement<?>> measurements = creator.getMeasurements();
        for (final ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(propagator.getInitialState().getDate());
                }
            }
        }

        // Return the generated measurements
        return measurements;
    }

    /**
     * Checker for batch LS estimator validation.
     *
     * @param print            If true, results are printed
     * @param context          Context used for the test
     * @param estimator        Batch LS estimator
     * @param iterations       Number of iterations expected
     * @param evaluations      Number of evaluations expected
     * @param expectedRMS      Expected RMS value
     * @param rmsEps           Tolerance on expected RMS
     * @param expectedMax      Expected weighted residual maximum
     * @param maxEps           Tolerance on weighted residual maximum
     * @param expectedDeltaPos Expected position difference between estimated orbit and initial orbit
     * @param posEps           Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and initial orbit
     * @param velEps           Tolerance on expected velocity difference
     */
    public static void checkFit(final boolean print, final Context context, final BatchLSEstimator estimator,
                                final int iterations, final int evaluations,
                                final double expectedRMS, final double rmsEps,
                                final double expectedMax, final double maxEps,
                                final double expectedDeltaPos, final double posEps,
                                final double expectedDeltaVel, final double velEps) {

        // Retrieve estimated values
        final Orbit estimatedOrbit = estimator.estimate()[0].getInitialState().getOrbit();
        final Vector3D estimatedPosition = estimatedOrbit.getPosition();
        final Vector3D estimatedVelocity = estimatedOrbit.getVelocity();

        // Handle residuals
        int    k   = 0;
        double sum = 0;
        double max = 0;
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
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

        // Compute interesting values for validation
        final double rms      = FastMath.sqrt(sum / k);
        final double deltaPos = Vector3D.distance(context.initialOrbit.getPosition(), estimatedPosition);
        final double deltaVel = Vector3D.distance(context.initialOrbit.getVelocity(), estimatedVelocity);

        // Print if necessary
        if (print) {
            System.out.format("%25s %25s %25s %25s\n", "", "Actual", "Expected", "Tolerance");
            System.out.format("%25s %25d %25d %25d\n", "iterations", estimator.getIterationsCount(), iterations, 0);
            System.out.format("%25s %25d %25d %25d\n", "evaluations", estimator.getEvaluationsCount(), evaluations, 0);
            System.out.format("%25s %25e %25e %25e\n", "RMS", rms, expectedRMS, rmsEps);
            System.out.format("%25s %25e %25e %25e\n", "max residuals", max, expectedMax, maxEps);
            System.out.format("%25s %25e %25e %25e\n", "delta pos", deltaPos, expectedDeltaPos, posEps);
            System.out.format("%25s %25e %25e %25e\n", "delta vel", deltaVel, expectedDeltaVel, velEps);
        }

        // Verify
        Assertions.assertEquals(expectedRMS,      rms,      rmsEps);
        Assertions.assertEquals(expectedMax,      max,      maxEps);
        Assertions.assertEquals(expectedDeltaPos, deltaPos, posEps);
        Assertions.assertEquals(expectedDeltaVel, deltaVel, velEps);
        Assertions.assertEquals(iterations, estimator.getIterationsCount());
        Assertions.assertEquals(evaluations, estimator.getEvaluationsCount());
        Optimum optimum = estimator.getOptimum();
        Assertions.assertEquals(iterations, optimum.getIterations());
        Assertions.assertEquals(evaluations, optimum.getEvaluations());
    }

    /**
     * Checker for Extended Kalman Filter (EKF) validation.
     *
     * @param print             If true, output is printed
     * @param kalman            Extended Kalman Filter
     * @param measurements      List of observed measurements to be processed by the Kalman
     * @param refOrbit          Reference orbits at last measurement date
     * @param expectedDeltaPos  Expected position difference between estimated orbit and reference orbit
     * @param posEps            Tolerance on expected position difference
     * @param expectedDeltaVel  Expected velocity difference between estimated orbit and reference orbit
     * @param velEps            Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps       Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps       Tolerance on expected covariance matrix on velocity
     */
    public static void checkExtendedKalmanFit(boolean print, final KalmanEstimator kalman,
                                              final List<ObservedMeasurement<?>> measurements,
                                              final Orbit refOrbit, final PositionAngleType positionAngleType,
                                              final double expectedDeltaPos, final double posEps,
                                              final double expectedDeltaVel, final double velEps,
                                              final double[] expectedSigmasPos, final double sigmaPosEps,
                                              final double[] expectedSigmasVel, final double sigmaVelEps) {
        checkExtendedKalmanFit(print, kalman, measurements, new Orbit[] { refOrbit }, new PositionAngleType[] {positionAngleType},
                               new double[] { expectedDeltaPos }, new double[] { posEps },
                               new double[] { expectedDeltaVel }, new double[] { velEps },
                               new double[][] { expectedSigmasPos }, new double[] { sigmaPosEps },
                               new double[][] { expectedSigmasVel }, new double[] { sigmaVelEps });
    }

    /**
     * Checker for Extended Kalman Filter (EKF) validation.
     *
     * @param print             If true, output is printed
     * @param kalman            Extended Kalman Filter
     * @param measurements      List of observed measurements to be processed by the Kalman
     * @param refOrbit          Reference orbits at last measurement date
     * @param expectedDeltaPos  Expected position difference between estimated orbit and reference orbits
     * @param posEps            Tolerance on expected position difference
     * @param expectedDeltaVel  Expected velocity difference between estimated orbit and reference orbits
     * @param velEps            Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps       Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps       Tolerance on expected covariance matrix on velocity
     */
    public static void checkExtendedKalmanFit(final boolean print, final KalmanEstimator kalman,
                                              final List<ObservedMeasurement<?>> measurements,
                                              final Orbit[] refOrbit, final PositionAngleType[] positionAngleType,
                                              final double[] expectedDeltaPos, final double[] posEps,
                                              final double[] expectedDeltaVel, final double []velEps,
                                              final double[][] expectedSigmasPos, final double[] sigmaPosEps,
                                              final double[][] expectedSigmasVel, final double[] sigmaVelEps) {
        Propagator[] estimated = kalman.processMeasurements(measurements);
        checkKalmanFit(print, estimated, kalman.getPhysicalEstimatedCovarianceMatrix(), measurements, refOrbit, positionAngleType,
                       kalman.getCurrentMeasurementNumber(), expectedDeltaPos, posEps, expectedDeltaVel, velEps,
                       expectedSigmasPos, sigmaPosEps, expectedSigmasVel, sigmaVelEps);
    }

    /**
     * Checker for Unscented Kalman Filter (UKF) validation
     *
     * @param print             If true, output is printed
     * @param kalman            Unscented Kalman Filter
     * @param measurements      List of observed measurements to be processed by the
     *                          Kalman
     * @param refOrbit          Reference orbits at last measurement date
     * @param expectedDeltaPos  Expected position difference between estimated orbit
     *                          and reference orbits
     * @param posEps            Tolerance on expected position difference
     * @param expectedDeltaVel  Expected velocity difference between estimated orbit
     *                          and reference orbits
     * @param velEps            Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps       Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps       Tolerance on expected covariance matrix on velocity
     */
    public static void checkUnscentedKalmanFit(final boolean print, final UnscentedKalmanEstimator kalman,
                                               final List<ObservedMeasurement<?>> measurements, final Orbit refOrbit,
                                               final PositionAngleType positionAngleType,
                                               final double expectedDeltaPos, final double posEps,
                                               final double expectedDeltaVel, final double velEps,
                                               final double[] expectedSigmasPos, final double sigmaPosEps,
                                               final double[] expectedSigmasVel, final double sigmaVelEps) {
        checkUnscentedKalmanFit(print, kalman, measurements, new Orbit[] { refOrbit }, new PositionAngleType[] {positionAngleType},
                                new double[] { expectedDeltaPos }, new double[] { posEps },
                                new double[] { expectedDeltaVel }, new double[] { velEps },
                                new double[][] { expectedSigmasPos }, new double[] { sigmaPosEps },
                                new double[][] { expectedSigmasVel }, new double[] { sigmaVelEps });
    }

    /**
     * Checker for Unscented Kalman Filter (UKF) validation
     *
     * @param print             If true, output is printed
     * @param kalman            Unscented Kalman Filter
     * @param measurements      List of observed measurements to be processed by the
     *                          Kalman
     * @param refOrbit          Reference orbits at last measurement date
     * @param expectedDeltaPos  Expected position difference between estimated orbit
     *                          and reference orbits
     * @param posEps            Tolerance on expected position difference
     * @param expectedDeltaVel  Expected velocity difference between estimated orbit
     *                          and reference orbits
     * @param velEps            Tolerance on expected velocity difference
     * @param expectedSigmasPos Expected values for covariance matrix on position
     * @param sigmaPosEps       Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel Expected values for covariance matrix on velocity
     * @param sigmaVelEps       Tolerance on expected covariance matrix on velocity
     */
    public static void checkUnscentedKalmanFit(final boolean print, final UnscentedKalmanEstimator kalman,
                                               final List<ObservedMeasurement<?>> measurements, final Orbit[] refOrbit, final PositionAngleType[] positionAngleType,
                                               final double[] expectedDeltaPos, final double[] posEps, final double[] expectedDeltaVel, final double[] velEps,
                                               final double[][] expectedSigmasPos, final double[] sigmaPosEps, final double[][] expectedSigmasVel,
                                               final double[] sigmaVelEps) {
        Propagator[] estimated = kalman.processMeasurements(measurements);
        checkKalmanFit(print, estimated, kalman.getPhysicalEstimatedCovarianceMatrix(), measurements, refOrbit, positionAngleType,
                       kalman.getCurrentMeasurementNumber(), expectedDeltaPos, posEps, expectedDeltaVel, velEps,
                       expectedSigmasPos, sigmaPosEps, expectedSigmasVel, sigmaVelEps);

    }

    /**
     * Checker for Extended Semi-analytical Kalman Filter (ESKF) validation
     *
     * @param print            If true, output is printed
     * @param kalman           Kalman filter
     * @param measurements     List of observed measurements to be processed by the Kalman
     * @param refOrbit         Reference orbits at last measurement date
     * @param expectedDeltaPos Expected position difference between estimated orbit and reference orbit
     * @param posEps           Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and reference orbit
     * @param velEps           Tolerance on expected velocity difference
     */
    public static void checkExtendedSemiAnalyticalKalmanFit(final boolean print, final SemiAnalyticalKalmanEstimator kalman,
                                                            final List<ObservedMeasurement<?>> measurements,
                                                            final Orbit refOrbit, final PositionAngleType positionAngleType,
                                                            final double expectedDeltaPos, final double posEps,
                                                            final double expectedDeltaVel, final double velEps,
                                                            final double[] expectedSigmasPos, final double sigmaPosEps,
                                                            final double[] expectedSigmasVel, final double sigmaVelEps) {
        Propagator estimated = kalman.processMeasurements(measurements);
        checkKalmanFit(print, new Propagator[]{estimated}, kalman.getPhysicalEstimatedCovarianceMatrix(), measurements,
                       new Orbit[]{refOrbit}, new PositionAngleType[]{positionAngleType}, kalman.getCurrentMeasurementNumber(),
                       new double[] {expectedDeltaPos}, new double[] {posEps},
                       new double[] {expectedDeltaVel}, new double[] {velEps},
                       new double[][] {expectedSigmasPos}, new double[] {sigmaPosEps},
                       new double[][] {expectedSigmasVel}, new double[] {sigmaVelEps});
    }

    /**
     * Checker for Unscented Semi-analytical Kalman Filter (USKF) validation
     *
     * @param print            If true, output is printed
     * @param kalman           Kalman filter
     * @param measurements     List of observed measurements to be processed by the Kalman
     * @param refOrbit         Reference orbits at last measurement date
     * @param expectedDeltaPos Expected position difference between estimated orbit and reference orbit
     * @param posEps           Tolerance on expected position difference
     * @param expectedDeltaVel Expected velocity difference between estimated orbit and reference orbit
     * @param velEps           Tolerance on expected velocity difference
     */
    public static void checkUnscentedSemiAnalyticalKalmanFit(final boolean print, final SemiAnalyticalUnscentedKalmanEstimator kalman,
                                                             final List<ObservedMeasurement<?>> measurements,
                                                             final Orbit refOrbit, final PositionAngleType positionAngleType,
                                                             final double expectedDeltaPos, final double posEps,
                                                             final double expectedDeltaVel, final double velEps,
                                                             final double[] expectedSigmasPos, final double sigmaPosEps,
                                                             final double[] expectedSigmasVel, final double sigmaVelEps) {
        Propagator estimated = kalman.processMeasurements(measurements);
        checkKalmanFit(print, new Propagator[]{estimated}, kalman.getPhysicalEstimatedCovarianceMatrix(), measurements,
                new Orbit[]{refOrbit}, new PositionAngleType[]{positionAngleType}, kalman.getCurrentMeasurementNumber(),
                new double[] {expectedDeltaPos}, new double[] {posEps},
                new double[] {expectedDeltaVel}, new double[] {velEps},
                new double[][] {expectedSigmasPos}, new double[] {sigmaPosEps},
                new double[][] {expectedSigmasVel}, new double[] {sigmaVelEps});
    }

    /**
     * Checker for Kalman Filter validation.
     *
     * @param print                      If true, output is printed
     * @param estimated                  Estimated states
     * @param estimatedP                 Physical estimated covariance matrix
     * @param measurements               List of observed measurements to be processed by the Kalman
     * @param refOrbit                   Reference orbits at last measurement date
     * @param positionAngleType          Position angle type
     * @param expectedMeasurementsNumber Expected number of measurements
     * @param expectedDeltaPos           Expected position difference between estimated orbit and reference orbits
     * @param posEps                     Tolerance on expected position difference
     * @param expectedDeltaVel           Expected velocity difference between estimated orbit and reference orbits
     * @param velEps                     Tolerance on expected velocity difference
     * @param expectedSigmasPos          Expected values for covariance matrix on position
     * @param sigmaPosEps                Tolerance on expected covariance matrix on position
     * @param expectedSigmasVel          Expected values for covariance matrix on velocity
     * @param sigmaVelEps                Tolerance on expected covariance matrix on velocity
     */
    public static void checkKalmanFit(final boolean print, final Propagator[] estimated, final RealMatrix estimatedP,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit[] refOrbit, final PositionAngleType[] positionAngleType,
                                      final int expectedMeasurementsNumber,
                                      final double[] expectedDeltaPos, final double[] posEps,
                                      final double[] expectedDeltaVel, final double[] velEps,
                                      final double[][] expectedSigmasPos, final double[] sigmaPosEps,
                                      final double[][] expectedSigmasVel, final double[] sigmaVelEps) {

        // Check the number of measurements processed by the filter
        Assertions.assertEquals(measurements.size(), expectedMeasurementsNumber);

        for (int k = 0; k < refOrbit.length; ++k) {

            // Get the last estimation
            final Orbit    estimatedOrbit    = estimated[k].getInitialState().getOrbit();
            final Vector3D estimatedPosition = estimatedOrbit.getPosition();
            final Vector3D estimatedVelocity = estimatedOrbit.getVelocity();

            // Convert the orbital part to Cartesian formalism
            // Assuming the filter estimates all 6 orbital parameters
            final double[][] dCdY = new double[6][6];
            estimatedOrbit.getJacobianWrtParameters(positionAngleType[k], dCdY);
            final RealMatrix Jacobian = MatrixUtils.createRealMatrix(dCdY);
            final RealMatrix estimatedCartesianP = Jacobian.multiply(estimatedP.getSubMatrix(0, 5, 0, 5)).multiply(Jacobian.transpose());

            // Get the final sigmas (ie.sqrt of the diagonal of the Cartesian orbital covariance matrix)
            final double[] sigmas = new double[6];
            for (int i = 0; i < 6; i++) {
                sigmas[i] = FastMath.sqrt(estimatedCartesianP.getEntry(i, i));
            }

            // Computes delta on position and velocity
            final double deltaPosK = Vector3D.distance(refOrbit[k].getPosition(), estimatedPosition);
            final double deltaVelK = Vector3D.distance(refOrbit[k].getVelocity(), estimatedVelocity);

            // Print if necessary
            if (print) {
                System.out.format("%25s %25s %25s %25s\n", "", "Actual", "Expected", "Tolerance");
                System.out.format("%25s %25e %25e %25e\n", "delta pos", deltaPosK, expectedDeltaPos[k], posEps[k]);
                System.out.format("%25s %25e %25e %25e\n", "delta vel", deltaVelK, expectedDeltaVel[k], velEps[k]);
                System.out.println("sigmas  = " + sigmas[0] + " " + sigmas[1] + " " + sigmas[2] + " " + sigmas[3] + " " + sigmas[4] + " " + sigmas[5]);
            }

            // Check the final orbit estimation & PV sigmas
            Assertions.assertEquals(expectedDeltaPos[k], deltaPosK, posEps[k]);
            Assertions.assertEquals(expectedDeltaVel[k], deltaVelK, velEps[k]);
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(expectedSigmasPos[k][i], sigmas[i],   sigmaPosEps[k]);
                Assertions.assertEquals(expectedSigmasVel[k][i], sigmas[i+3], sigmaVelEps[k]);
            }
        }
    }

    /** Get an elevation detector.
     * @param topo ground station
     * @param minElevation detection elevation
     * @return elevation detector
     */
    public static ElevationDetector getElevationDetector(final TopocentricFrame topo, final double minElevation) {
        return new ElevationDetector(topo).
            withThreshold(AbstractDetector.DEFAULT_THRESHOLD).
            withMaxCheck(ElevationDetectionAdaptableIntervalFactory.getAdaptableInterval(topo,
                                                                                         ElevationDetectionAdaptableIntervalFactory.DEFAULT_ELEVATION_SWITCH_INF,
                                                                                         ElevationDetectionAdaptableIntervalFactory.DEFAULT_ELEVATION_SWITCH_SUP,
                                                                                         10.0)).
            withConstantElevation(minElevation);
    }

}

