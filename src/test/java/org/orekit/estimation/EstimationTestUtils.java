/* Copyright 2002-2023 CS GROUP
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
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.KalmanEstimator;
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
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility class for orbit determination tests. */
public class EstimationTestUtils {

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
        context.gravity = GravityFieldFactory.getNormalizedProvider(20, 20);
        context.initialOrbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                                  0.250, 1.375, 0.0625, PositionAngleType.TRUE,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, context.utc),
                                                  context.gravity.getMu());

        context.stations = Arrays.asList(//context.createStation(-18.59146, -173.98363,   76.0, "Leimatu`a"),
                                         context.createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                         context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur")
                                         //context.createStation( -4.01583,  103.12833, 3173.0, "Gunung Dempo")
                        );

        // Turn-around range stations
        // Map entry = primary station
        // Map value = secondary station associated
        context.TARstations = new HashMap<GroundStation, GroundStation>();

        context.TARstations.put(context.createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                context.createStation(-54.815833,  -68.317778, 6.0, "Ushuaïa"));

        context.TARstations.put(context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur"),
                                context.createStation( 61.405833,   -6.705278,  470.0, "Sumba"));

        // Bistatic range rate stations
        // key/first    = emitter station
        // value/second = receiver station
        context.BRRstations = new Pair<GroundStation, GroundStation>(context.createStation(40.0, 0.0, 0.0, "Emitter"),
                                                                     context.createStation(45.0, 0.0, 0.0, "Receiver"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.TDOAstations = new Pair<GroundStation, GroundStation>(context.createStation(40.0, 0.0, 0.0, "TDOA_Prime"),
                                                                      context.createStation(45.0, 0.0, 0.0, "TDOA_Second"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.FDOAstations = context.TDOAstations;

        return context;

    }

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
            private static final long serialVersionUID = 1L;
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
        context.gravity = GravityFieldFactory.getNormalizedProvider(20, 20);

        // semimajor axis for a geostationnary satellite
        double da = FastMath.cbrt(context.gravity.getMu() / (omega * omega));

        //context.stations = Arrays.asList(context.createStation(  0.0,  0.0, 0.0, "Lat0_Long0"),
        //                                 context.createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur")
        //                );
        context.stations = Collections.singletonList(context.createStation(0.0, 0.0, 0.0, "Lat0_Long0") );

        // Station position & velocity in EME2000
        final Vector3D geovelocity = new Vector3D (0., 0., 0.);

        // Compute the frames transformation from station frame to EME2000
        Transform topoToEME =
        context.stations.get(0).getBaseFrame().getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, context.utc));

        // Station position in EME2000 at reference date
        Vector3D stationPositionEME = topoToEME.transformPosition(Vector3D.ZERO);

        // Satellite position and velocity in Station Frame
        final Vector3D sat_pos = new Vector3D(0., 0., da-stationPositionEME.getNorm());
        final Vector3D acceleration = new Vector3D(-context.gravity.getMu(), sat_pos);
        final PVCoordinates pv_sat_topo = new PVCoordinates(sat_pos, geovelocity, acceleration);

        // satellite position in EME2000
        final PVCoordinates pv_sat_iner = topoToEME.transformPVCoordinates(pv_sat_topo);

        // Geo-stationary Satellite Orbit, tightly above the station (l0-L0)
        context.initialOrbit = new KeplerianOrbit(pv_sat_iner,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, context.utc),
                                                  context.gravity.getMu());

        context.stations = Collections.singletonList(context.createStation(10.0, 45.0, 0.0, "Lat10_Long45") );

        // Turn-around range stations
        // Map entry = primary station
        // Map value = secondary station associated
        context.TARstations = new HashMap<GroundStation, GroundStation>();

        context.TARstations.put(context.createStation(  41.977, 13.600,  671.354, "Fucino"),
                                context.createStation(  43.604,  1.444,  263.0  , "Toulouse"));

        context.TARstations.put(context.createStation(  49.867,  8.65 ,  144.0  , "Darmstadt"),
                                context.createStation( -25.885, 27.707, 1566.633, "Pretoria"));

        // Bistatic range rate stations
        // key/first    = emitter station
        // value/second = receiver station
        context.BRRstations = new Pair<GroundStation, GroundStation>(context.createStation(40.0, 0.0, 0.0, "Emitter"),
                                                                     context.createStation(45.0, 0.0, 0.0, "Receiver"));

        // TDOA stations
        // key/first    = primary station that dates the measurement
        // value/second = secondary station associated
        context.TDOAstations = new Pair<GroundStation, GroundStation>(context.createStation(40.0, 0.0, 0.0, "TDOA_Prime"),
                                                                      context.createStation(45.0, 0.0, 0.0, "TDOA_Second"));

        return context;

    }

    public static Propagator createPropagator(final Orbit initialOrbit,
                                              final PropagatorBuilder propagatorBuilder) {

        // override orbital parameters
        double[] orbitArray = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(initialOrbit,
                                                         propagatorBuilder.getPositionAngleType(),
                                                         orbitArray, null);
        for (int i = 0; i < orbitArray.length; ++i) {
        	// here orbital paramaters drivers have only 1 estimated values on the all time period for orbit determination
            propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(i).setValue(orbitArray[i]);
        }

        return propagatorBuilder.buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

    }

    public static List<ObservedMeasurement<?>> createMeasurements(final Propagator propagator,
                                                                  final MeasurementCreator creator,
                                                                  final double startPeriod, final double endPeriod,
                                                                  final double step) {

        propagator.setStepHandler(step, creator);
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
     * @param context Context used for the test
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
    public static void checkFit(final Context context, final BatchLSEstimator estimator,
                                final int iterations, final int evaluations,
                                final double expectedRMS,      final double rmsEps,
                                final double expectedMax,      final double maxEps,
                                final double expectedDeltaPos, final double posEps,
                                final double expectedDeltaVel, final double velEps) {

        final Orbit estimatedOrbit = estimator.estimate()[0].getInitialState().getOrbit();
        final Vector3D estimatedPosition = estimatedOrbit.getPosition();
        final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();

        Assertions.assertEquals(iterations, estimator.getIterationsCount());
        Assertions.assertEquals(evaluations, estimator.getEvaluationsCount());
        Optimum optimum = estimator.getOptimum();
        Assertions.assertEquals(iterations, optimum.getIterations());
        Assertions.assertEquals(evaluations, optimum.getEvaluations());

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

        final double rms      = FastMath.sqrt(sum / k);
        final double deltaPos = Vector3D.distance(context.initialOrbit.getPosition(), estimatedPosition);
        final double deltaVel = Vector3D.distance(context.initialOrbit.getPVCoordinates().getVelocity(), estimatedVelocity);
        Assertions.assertEquals(expectedRMS,      rms,      rmsEps);
        Assertions.assertEquals(expectedMax,      max,      maxEps);
        Assertions.assertEquals(expectedDeltaPos, deltaPos, posEps);
        Assertions.assertEquals(expectedDeltaVel, deltaVel, velEps);

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
    public static void checkKalmanFit(final Context context, final KalmanEstimator kalman,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit refOrbit, final PositionAngleType positionAngleType,
                                      final double expectedDeltaPos, final double posEps,
                                      final double expectedDeltaVel, final double velEps,
                                      final double[] expectedSigmasPos,final double sigmaPosEps,
                                      final double[] expectedSigmasVel,final double sigmaVelEps)
        {
        checkKalmanFit(context, kalman, measurements,
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
    public static void checkKalmanFit(final Context context, final KalmanEstimator kalman,
                                      final List<ObservedMeasurement<?>> measurements,
                                      final Orbit[] refOrbit, final PositionAngleType[] positionAngleType,
                                      final double[] expectedDeltaPos, final double[] posEps,
                                      final double[] expectedDeltaVel, final double []velEps,
                                      final double[][] expectedSigmasPos,final double[] sigmaPosEps,
                                      final double[][] expectedSigmasVel,final double[] sigmaVelEps)
                                                      {

        // Add the measurements to the Kalman filter
        Propagator[] estimated = kalman.processMeasurements(measurements);

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
    }

}

