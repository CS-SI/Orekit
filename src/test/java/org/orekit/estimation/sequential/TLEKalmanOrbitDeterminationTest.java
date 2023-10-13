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
package org.orekit.estimation.sequential;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultKalman;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class TLEKalmanOrbitDeterminationTest extends AbstractOrbitDetermination<TLEPropagatorBuilder> {

    /** Initial TLE. */
    public TLE templateTLE;

    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        // TLE OD does not need gravity field
    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return TLEConstants.MU;
    }

    /** {@inheritDoc} */
    @Override
    protected TLEPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                           final ODEIntegratorBuilder builder,
                                                           final double positionScale) {
        return new TLEPropagatorBuilder(templateTLE, PositionAngleType.MEAN, positionScale,
                                        new FixedPointTleGenerationAlgorithm());
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final TLEPropagatorBuilder propagatorBuilder,
                                final double mass) {

     // TLE OD does not need to set mass
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setGravity(final TLEPropagatorBuilder propagatorBuilder,
                                               final OneAxisEllipsoid body) {
        return Collections.emptyList();

    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setOceanTides(final TLEPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final int degree, final int order) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Ocean tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolidTides(final TLEPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final CelestialBody[] solidTidesBodies) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "Solid tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setThirdBody(final TLEPropagatorBuilder propagatorBuilder,
                                                 final CelestialBody thirdBody) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setDrag(final TLEPropagatorBuilder propagatorBuilder,
                                            final Atmosphere atmosphere, final DragSensitive spacecraft) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolarRadiationPressure(final TLEPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                              final OneAxisEllipsoid body, final RadiationSensitive spacecraft) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setAlbedoInfrared(final TLEPropagatorBuilder propagatorBuilder,
                                                      final CelestialBody sun, final double equatorialRadius,
                                                      final double angularResolution,
                                                      final RadiationSensitive spacecraft) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Albedo and infrared not implemented in TLE Propagator");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setRelativity(final TLEPropagatorBuilder propagatorBuilder) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Relativity not implemented in TLE Propagator");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setPolynomialAcceleration(final TLEPropagatorBuilder propagatorBuilder,
                                                              final String name, final Vector3D direction, final int degree) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Polynomial acceleration not implemented in TLE Propagator");
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final TLEPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = false;

        // input in resources directory
        final String inputPath = KalmanNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/tle_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // initiate TLE
        final String line1 = "1 22195U 92070B   16045.51027931 -.00000009  00000-0  00000+0 0  9990";
        final String line2 = "2 22195  52.6508 132.9147 0137738 336.2706   1.6348  6.47294052551192";
        templateTLE = new TLE(line1, line2);
        templateTLE.getParametersDrivers().get(0).setSelected(false);

        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;

        // Initial orbital Cartesian covariance matrix
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e4, 4e3, 1, 5e-3, 6e-5, 1e-4
        });

        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });

        // Initial measurement covariance matrix and process noise matrix
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
           1., 1., 1., 1.
        });
        final RealMatrix measurementQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-6, 1e-6, 1e-6, 1e-6
         });

        // Kalman orbit determination run.
        ResultKalman kalmanLageos2 = runKalman(input, orbitType, print,
                                               cartesianOrbitalP, cartesianOrbitalQ,
                                               null, null,
                                               measurementP, measurementQ, false);

        // Definition of the accuracy for the test
        // Initial TLE error at last measurement date is 3997m
        final double distanceAccuracy = 300.38;
        final double velocityAccuracy = 0.148;

        // Tests

        // Number of measurements processed
        final int numberOfMeas  = 95;
        Assertions.assertEquals(numberOfMeas, kalmanLageos2.getNumberOfMeasurements());

        //test on the estimated position and velocity
        final TimeStampedPVCoordinates odPV = kalmanLageos2.getEstimatedPV();
        final Vector3D estimatedPos = odPV.getPosition();
        final Vector3D estimatedVel = odPV.getVelocity();

        // Reference position and velocity at estimation date
        final Vector3D refPos0 = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel0 = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        final TimeStampedPVCoordinates refPV = createRef(odPV.getDate(), refPos0, refVel0);


        final Vector3D refPos = refPV.getPosition();
        final Vector3D refVel = refPV.getVelocity();

        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        Assertions.assertEquals(0.0, dP, distanceAccuracy);
        Assertions.assertEquals(0.0, dV, velocityAccuracy);

        // Print orbit deltas
        if (print) {
            System.out.println("Test performances:");
            System.out.format("\t%-30s\n",
                            "ΔEstimated / Reference");
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔP [m]", dP);
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔV [m/s]", dV);
        }

        // Test on measurements parameters
        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>();
        list.addAll(kalmanLageos2.getMeasurementsParameters().getDrivers());
        sortParametersChanges(list);
        final double[] stationOffSet = { 0.214786,  1.057400,  -0.54545 };
        final double rangeBias = 0.12005;
        Assertions.assertEquals(stationOffSet[0], list.get(0).getValue(), distanceAccuracy);
        Assertions.assertEquals(stationOffSet[1], list.get(1).getValue(), distanceAccuracy);
        Assertions.assertEquals(stationOffSet[2], list.get(2).getValue(), distanceAccuracy);
        Assertions.assertEquals(rangeBias,        list.get(3).getValue(), distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 95;
        // Batch LS values
        //final double[] RefStatRange = { -67.7496, 87.1117, 6.4482E-5, 33.6349 };
        final double[] RefStatRange = { -70.790, 55.667, 5.873, 36.540 };
        Assertions.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], kalmanLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[1], kalmanLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[2], kalmanLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(RefStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    @Test
    // Orbit determination for GNSS satellite G07 based on SLR (range) measurements
    public void testGNSS() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = false;

        // input in resources directory
        final String inputPath = KalmanNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/analytical/tle_od_test_GPS07.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // initiate TLE
        final String line1 = "1 32711U 08012A   16044.40566018 -.00000039 +00000-0 +00000-0 0  9993";
        final String line2 = "2 32711 055.4362 301.3402 0091581 207.7162 151.8496 02.00563594058026";
        templateTLE = new TLE(line1, line2);
        templateTLE.getParametersDrivers().get(0).setSelected(false);

        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;

        // Initial orbital Keplerian covariance matrix
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e4, 4e3, 1, 5e-3, 6e-5, 1e-4
        });

        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });

        // Initial measurement covariance matrix and process noise matrix
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
           1., 1., 1., 1., 1., 1.
        });
        final RealMatrix measurementQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-6, 1e-6, 1e-6, 1e-6, 1e-6, 1e-6
         });

        // Kalman orbit determination run.
        ResultKalman kalmanGNSS = runKalman(input, orbitType, print,
                                            cartesianOrbitalP, cartesianOrbitalQ,
                                            null, null,
                                            measurementP, measurementQ, false);

        // Definition of the accuracy for the test
        // Initial TLE error at last measurement date is 1053.6m
        final double distanceAccuracy = 67.33;

        // Tests

        // Number of multiplexed measurements processed
        final int numberOfMeas  = 661;
        Assertions.assertEquals(numberOfMeas, kalmanGNSS.getNumberOfMeasurements());

        //test on the estimated position
        TimeStampedPVCoordinates odPV = kalmanGNSS.getEstimatedPV();
        final StaticTransform transform = FramesFactory.getTEME().getStaticTransformTo(FramesFactory.getGCRF(), odPV.getDate());
        final Vector3D estimatedPos = transform.transformPosition(odPV.getPosition());

        // Reference position from GPS ephemeris (esa18836.sp3)
        final Vector3D refPos = new Vector3D(2167703.453226041, 19788555.311260417, 17514805.616900872);

        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        Assertions.assertEquals(0.0, dP, distanceAccuracy);

        // Print orbit deltas
        if (print) {
            System.out.println("Test performances:");
            System.out.format("\t%-30s\n",
                            "ΔEstimated / Reference");
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔP [m]", dP);
        }

        //test on statistic for the range residuals
        final long nbRange = 8211;

        final double[] RefStatRange = { -44.073, 51.349, 0.242, 8.602 };
        Assertions.assertEquals(nbRange, kalmanGNSS.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], kalmanGNSS.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[1], kalmanGNSS.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[2], kalmanGNSS.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(RefStatRange[3], kalmanGNSS.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    // Creates reference PV from reference PV with numerical propagation in TEME
    public TimeStampedPVCoordinates createRef(final AbsoluteDate date, final Vector3D refPos0, final Vector3D refVel0) {

        // Initial orbit
        final AbsoluteDate initDate = new AbsoluteDate(2016, 02, 14, 12, 14, 48.132, TimeScalesFactory.getUTC());
        final CartesianOrbit initOrbit= new CartesianOrbit(new PVCoordinates(refPos0, refVel0), FramesFactory.getEME2000(), initDate, TLEConstants.MU);
        final SpacecraftState initState = new SpacecraftState(initOrbit);

        // Numerical propagator initialization
        double[][] tolerance = NumericalPropagator.tolerances(0.001, initOrbit, OrbitType.CARTESIAN);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initState);

        // Force models
        final int    degree                          = 20;
        final int    order                           = 20;
        final double spacecraftArea                  = 1.0;
        final double spacecraftDragCoefficient       = 2.0;
        final double spacecraftReflectionCoefficient = 2.0;

        // Earth gravity field
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final NormalizedSphericalHarmonicsProvider harmonicsGravityProvider = GravityFieldFactory.getNormalizedProvider(degree, order);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(), harmonicsGravityProvider));

        // Sun and Moon attraction
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // Atmospheric Drag
        ForceModel drag = new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(), earth),
                                        new IsotropicDrag(spacecraftArea, spacecraftDragCoefficient));
        propagator.addForceModel(drag);

        // Solar radiation pressure
        propagator.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), earth,
                                                    new IsotropicRadiationSingleCoefficient(spacecraftArea, spacecraftReflectionCoefficient)));

        // Propagation
        TimeStampedPVCoordinates endPV = propagator.propagate(date).getPVCoordinates();
        final Transform transform = FramesFactory.getEME2000().getTransformTo(FramesFactory.getTEME(), date);
        endPV = transform.transformPVCoordinates(endPV);
        return endPV;
    }
}
