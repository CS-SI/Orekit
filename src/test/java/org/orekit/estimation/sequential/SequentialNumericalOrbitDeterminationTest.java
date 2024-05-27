/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultKalman;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.empirical.AccelerationModel;
import org.orekit.forces.empirical.ParametricAcceleration;
import org.orekit.forces.empirical.PolynomialAccelerationModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.KnockeRediffusedForceModel;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class SequentialNumericalOrbitDeterminationTest extends AbstractOrbitDetermination<NumericalPropagatorBuilder> {

    /** Gravity field. */
    private NormalizedSphericalHarmonicsProvider gravityField;

    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        gravityField = GravityFieldFactory.getNormalizedProvider(degree, order);

    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return gravityField.getMu();
    }

    /** {@inheritDoc} */
    @Override
    protected NumericalPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                                 final ODEIntegratorBuilder builder,
                                                                 final double positionScale) {
        return new NumericalPropagatorBuilder(referenceOrbit, builder, PositionAngleType.MEAN,
                                              positionScale);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final NumericalPropagatorBuilder propagatorBuilder,
                                final double mass) {
        propagatorBuilder.setMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setGravity(final NumericalPropagatorBuilder propagatorBuilder,
                                               final OneAxisEllipsoid body) {
        final ForceModel gravityModel = new HolmesFeatherstoneAttractionModel(body.getBodyFrame(), gravityField);
        propagatorBuilder.addForceModel(gravityModel);
        return gravityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setOceanTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final int degree, final int order) {
        final ForceModel tidesModel = new OceanTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     degree, order, conventions,
                                                     TimeScalesFactory.getUT1(conventions, true));
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolidTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final CelestialBody[] solidTidesBodies) {
        final ForceModel tidesModel = new SolidTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     gravityField.getTideSystem(), conventions,
                                                     TimeScalesFactory.getUT1(conventions, true),
                                                     solidTidesBodies);
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setThirdBody(final NumericalPropagatorBuilder propagatorBuilder,
                                                 final CelestialBody thirdBody) {
        final ForceModel thirdBodyModel = new ThirdBodyAttraction(thirdBody);
        propagatorBuilder.addForceModel(thirdBodyModel);
        return thirdBodyModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setDrag(final NumericalPropagatorBuilder propagatorBuilder,
                                            final Atmosphere atmosphere, final DragSensitive spacecraft) {
        final ForceModel dragModel = new DragForce(atmosphere, spacecraft);
        propagatorBuilder.addForceModel(dragModel);
        return dragModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolarRadiationPressure(final NumericalPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                              final OneAxisEllipsoid earth, final RadiationSensitive spacecraft) {
        final ForceModel srpModel = new SolarRadiationPressure(sun, earth, spacecraft);
        propagatorBuilder.addForceModel(srpModel);
        return srpModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setAlbedoInfrared(final NumericalPropagatorBuilder propagatorBuilder,
                                                      final CelestialBody sun, final double equatorialRadius,
                                                      final double angularResolution,
                                                      final RadiationSensitive spacecraft) {
        final ForceModel albedoIR = new KnockeRediffusedForceModel(sun, spacecraft, equatorialRadius, angularResolution);
        propagatorBuilder.addForceModel(albedoIR);
        return albedoIR.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setRelativity(final NumericalPropagatorBuilder propagatorBuilder) {
        final ForceModel relativityModel = new Relativity(gravityField.getMu());
        propagatorBuilder.addForceModel(relativityModel);
        return relativityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setPolynomialAcceleration(final NumericalPropagatorBuilder propagatorBuilder,
                                                              final String name, final Vector3D direction, final int degree) {
        final AccelerationModel accModel = new PolynomialAccelerationModel(name, null, degree);
        final ForceModel polynomialModel = new ParametricAcceleration(direction, true, accModel);
        propagatorBuilder.addForceModel(polynomialModel);
        return polynomialModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final NumericalPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }


    @Test
    public void testLageos2Extended() throws URISyntaxException, IOException {

        // Position/velocity accuracy
        final double distanceAccuracy = 0.541;
        final double velocityAccuracy = 4.12e-3;

        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.2983710,  -0.137384,  0.0156606 };
        final double rangeBias = -0.00129569;

        // Batch LS values
        //final double[] refStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] refStatRange = { -23.537659, 20.444514, 0.973118, 5.686005 };

        testLageos2(distanceAccuracy, velocityAccuracy, stationOffSet, rangeBias, refStatRange, false, false);
    }

    @Test
    public void testLageos2Unscented() throws URISyntaxException, IOException {

        // Position/velocity accuracy
        final double distanceAccuracy = 0.482;
        final double velocityAccuracy = 3.97e-3;

        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.302324,  -0.127179,  0.0172399 };
        final double rangeBias = -0.008282;

        // Batch LS values
        //final double[] refStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] refStatRange = { -16.560410, 21.362738, 0.665356, 5.657324 };

        testLageos2(distanceAccuracy, velocityAccuracy, stationOffSet, rangeBias, refStatRange, false, true);
    }


    // Orbit determination for Lageos2 based on SLR (range) measurements
    protected void testLageos2(final double distanceAccuracy, final double velocityAccuracy,
                               final double[] stationOffSet, final double rangeBias, final double[] refStatRange,
                               final boolean print, final boolean isUnscented) throws URISyntaxException, IOException {

        // input in resources directory
        final String inputPath = SequentialNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/kalman_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;

        // Initial orbital Cartesian covariance matrix
        // These covariances are derived from the deltas between initial and reference orbits
        // So in a way they are "perfect"...
        // Cartesian covariance matrix initialization
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
                                               measurementP, measurementQ, isUnscented);

        // Tests
        // Note: The reference initial orbit is the same as in the batch LS tests
        // -----

        // Number of measurements processed
        final int numberOfMeas  = 258;
        Assertions.assertEquals(numberOfMeas, kalmanLageos2.getNumberOfMeasurements());

        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanLageos2.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel0 = new Vector3D(-3871.275109, -607.880985, 4280.972530);

        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, null,
                                            kalmanLageos2.getEstimatedPV().getDate());
        final Vector3D refPos = refOrbit.getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();

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

        // Accuracy for tests
        final double parametersAccuracy = 1e-6;

        // Test on measurements parameters
        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>(kalmanLageos2.getMeasurementsParameters().getDrivers());
        sortParametersChanges(list);

        Assertions.assertEquals(stationOffSet[0], list.get(0).getValue(), parametersAccuracy);
        Assertions.assertEquals(stationOffSet[1], list.get(1).getValue(), parametersAccuracy);
        Assertions.assertEquals(stationOffSet[2], list.get(2).getValue(), parametersAccuracy);
        Assertions.assertEquals(rangeBias,        list.get(3).getValue(), parametersAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        Assertions.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assertions.assertEquals(refStatRange[0], kalmanLageos2.getRangeStat().getMin(),               parametersAccuracy);
        Assertions.assertEquals(refStatRange[1], kalmanLageos2.getRangeStat().getMax(),               parametersAccuracy);
        Assertions.assertEquals(refStatRange[2], kalmanLageos2.getRangeStat().getMean(),              parametersAccuracy);
        Assertions.assertEquals(refStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), parametersAccuracy);

    }

    @Test
    public void testW3BExtended() throws URISyntaxException, IOException {
        // Batch LS result: -0.2154;
        final double dragCoef  = 0.1932;

        // Batch LS results: 8.002e-6
        final double leakAccelerationNorm0 = 5.994e-6;

        // Batch LS results: 3.058e-10
        final double leakAccelerationNorm1 = 1.836e-10;

        // Batch LS results
        // final double[] CastleAzElBias  = { 0.062701342, -0.003613508 };
        // final double   CastleRangeBias = 11274.4677;
        final double[] castleAzElBias  = { 0.062636, -0.003672};
        final double   castleRangeBias = 11289.3471;

        // Batch LS results
        // final double[] FucAzElBias  = { -0.053526137, 0.075483886 };
        // final double   FucRangeBias = 13467.8256;
        final double[] fucAzElBias  = { -0.053298, 0.075589 };
        final double   fucRangeBias = 13482.0805;

        // Batch LS results
        // final double[] KumAzElBias  = { -0.023574208, -0.054520756 };
        // final double   KumRangeBias = 13512.57594;
        final double[] kumAzElBias  = { -0.022805, -0.055057 };
        final double   kumRangeBias = 13502.6845;

        // Batch LS results
        // final double[] PreAzElBias = { 0.030201539, 0.009747877 };
        // final double PreRangeBias = 13594.11889;
        final double[] preAzElBias = { 0.030353, 0.009658 };
        final double   preRangeBias = 13609.2762;

        // Batch LS results
        // final double[] UraAzElBias = { 0.167814449, -0.12305252 };
        // final double UraRangeBias = 13450.26738;
        final double[] uraAzElBias = { 0.167519, -0.122842 };
        final double   uraRangeBias = 13441.6666;

        //statistics for the range residual (min, max, mean, std)
        final double[] refStatRange = { -12.9805, 18.0538, -1.1332, 5.3125 };

        //statistics for the azimuth residual (min, max, mean, std)
        final double[] refStatAzi = { -0.041442, 0.023473, -0.004426, 0.009911 };

        //statistics for the elevation residual (min, max, mean, std)
        final double[] refStatEle = { -0.025399, 0.043346, 0.001011, 0.010636 };

        // Expected covariance
        final double dragVariance = 0.016350;
        final double leakXVariance = 2.047E-13;
        final double leakYVariance = 5.462E-13;
        final double leakZVariance = 1.71778E-11;

        // Prediction position/velocity accuracies
        // FIXME: debug - Comparison with batch LS is bad
        final double predictionDistanceAccuracy = 234.57;
        final double predictionVelocityAccuracy = 0.086;

        testW3B(dragCoef, leakAccelerationNorm0, leakAccelerationNorm1,
                castleAzElBias, castleRangeBias, fucAzElBias, fucRangeBias, kumAzElBias, kumRangeBias,
                preAzElBias, preRangeBias, uraAzElBias, uraRangeBias,
                refStatRange, refStatAzi, refStatEle, dragVariance,
                leakXVariance, leakYVariance, leakZVariance,
                predictionDistanceAccuracy, predictionVelocityAccuracy, false, false);
    }

    @Test
    public void testW3BUnscented() throws URISyntaxException, IOException {
        // Batch LS result: -0.2154;
        final double dragCoef  = -0.0214;

        // Batch LS results: 8.002e-6
        final double leakAccelerationNorm0 = 5.954e-6;

        // Batch LS results: 3.058e-10
        final double leakAccelerationNorm1 = 1.619e-10;

        // Batch LS results
        // final double[] CastleAzElBias  = { 0.062701342, -0.003613508 };
        // final double   CastleRangeBias = 11274.4677;
        final double[] castleAzElBias  = { 0.062344, -0.004106};
        final double   castleRangeBias = 11333.0998;

        // Batch LS results
        // final double[] FucAzElBias  = { -0.053526137, 0.075483886 };
        // final double   FucRangeBias = 13467.8256;
        final double[] fucAzElBias  = { -0.053870, 0.075641 };
        final double   fucRangeBias = 13461.7291;

        // Batch LS results
        // final double[] KumAzElBias  = { -0.023574208, -0.054520756 };
        // final double   KumRangeBias = 13512.57594;
        final double[] kumAzElBias  = { -0.023393, -0.055078 };
        final double   kumRangeBias = 13515.6244;

        // Batch LS results
        // final double[] PreAzElBias = { 0.030201539, 0.009747877 };
        // final double PreRangeBias = 13594.11889;
        final double[] preAzElBias = { 0.030250, 0.010083 };
        final double   preRangeBias = 13534.0334;

        // Batch LS results
        // final double[] UraAzElBias = { 0.167814449, -0.12305252 };
        // final double UraRangeBias = 13450.26738;
        final double[] uraAzElBias = { 0.167700, -0.122408 };
        final double   uraRangeBias = 13417.6695;

        //statistics for the range residual (min, max, mean, std)
        final double[] refStatRange = { -144.9733, 14.7486, -3.8990, 11.9050 };

        //statistics for the azimuth residual (min, max, mean, std)
        final double[] refStatAzi = { -0.041873, 0.018087, -0.004536, 0.008995 };

        //statistics for the elevation residual (min, max, mean, std)
        final double[] refStatEle = { -0.025583, 0.043560, 0.001857, 0.010625 };

        // Expected covariance
        final double dragVariance = 0.018813;
        final double leakXVariance = 2.117E-13;
        final double leakYVariance = 5.540E-13;
        final double leakZVariance = 1.73244E-11;

        // Prediction position/velocity accuracies
        // FIXME: debug - Comparison with batch LS is bad
        final double predictionDistanceAccuracy = 285.31;
        final double predictionVelocityAccuracy = 0.101;

        testW3B(dragCoef, leakAccelerationNorm0, leakAccelerationNorm1,
                castleAzElBias, castleRangeBias, fucAzElBias, fucRangeBias, kumAzElBias, kumRangeBias,
                preAzElBias, preRangeBias, uraAzElBias, uraRangeBias,
                refStatRange, refStatAzi, refStatEle, dragVariance,
                leakXVariance, leakYVariance, leakZVariance,
                predictionDistanceAccuracy, predictionVelocityAccuracy, false, true);
    }


    // Orbit determination for range, azimuth elevation measurements
    protected void testW3B(final double dragCoef, final double leakAccelerationNorm0, final double leakAccelerationNorm1,
                           final double[] castleAzElBias, final double castleRangeBias,
                           final double[] fucAzElBias, final double fucRangeBias,
                           final double[] kumAzElBias, final double kumRangeBias,
                           final double[] preAzElBias, final double preRangeBias,
                           final double[] uraAzElBias, final double uraRangeBias,
                           final double[] refStatRange, final double[] refStatAzi, final double[] refStatEle,
                           final double dragVariance,
                           final double leakXVariance, final double leakYVariance, final double leakZVariance,
                           final double predictionDistanceAccuracy, final double predictionVelocityAccuracy,
                           final boolean print, final boolean isUnscented) throws URISyntaxException, IOException {

        // input in resources directory
        final String inputPath = SequentialNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/W3B/od_test_W3.in").toURI().getPath();
        final File input  = new File(inputPath);

        // Configure Orekit data access
        Utils.setDataRoot("orbit-determination/W3B:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;

        // Initial orbital Cartesian covariance matrix
        // These covariances are derived from the deltas between initial and reference orbits
        // So in a way they are "perfect"...
        // Cartesian covariance matrix initialization
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            FastMath.pow(2.4e4, 2), FastMath.pow(1.e5, 2), FastMath.pow(4.e4, 2),
            FastMath.pow(3.5, 2), FastMath.pow(2., 2), FastMath.pow(0.6, 2)
        });

        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });

        // Propagation covariance and process noise matrices
        final RealMatrix propagationP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            FastMath.pow(2., 2), // Cd
            FastMath.pow(5.7e-6, 2), FastMath.pow(1.1e-11, 2),   // leak-X
            FastMath.pow(7.68e-7, 2), FastMath.pow(1.26e-10, 2), // leak-Y
            FastMath.pow(5.56e-6, 2), FastMath.pow(2.79e-10, 2)  // leak-Z
        });
        final RealMatrix propagationQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            FastMath.pow(1e-3, 2), // Cd
            0., 0., 0., 0., 0., 0.  // Leaks
        });

        // Measurement covariance and process noise matrices
        // az/el bias sigma = 0.06deg
        // range bias sigma = 100m
        final double angularVariance = FastMath.pow(FastMath.toRadians(0.06), 2);
        final double rangeVariance   = FastMath.pow(500., 2);
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            angularVariance, angularVariance, rangeVariance,
            angularVariance, angularVariance, rangeVariance,
            angularVariance, angularVariance, rangeVariance,
            angularVariance, angularVariance, rangeVariance,
            angularVariance, angularVariance, rangeVariance,
        });
        // Process noise sigma: 1e-6 for all
        final double measQ = FastMath.pow(1e-6, 2);
        final RealMatrix measurementQ = MatrixUtils.
                        createRealIdentityMatrix(measurementP.getRowDimension()).
                        scalarMultiply(measQ);


        // Kalman orbit determination run.
        ResultKalman kalmanW3B = runKalman(input, orbitType, print,
                                           cartesianOrbitalP, cartesianOrbitalQ,
                                           propagationP, propagationQ,
                                           measurementP, measurementQ, isUnscented);

        // Tests
        // -----

        // Definition of the accuracy for the test
        final double distanceAccuracy = 1e-4;
        final double angleAccuracy    = 1e-6; // degrees

        // Number of measurements processed
        final int numberOfMeas  = 521;
        Assertions.assertEquals(numberOfMeas, kalmanW3B.getNumberOfMeasurements());


        // Test on propagator parameters
        // -----------------------------
        final ParameterDriversList propagatorParameters = kalmanW3B.getPropagatorParameters();
        Assertions.assertEquals(dragCoef, propagatorParameters.getDrivers().get(0).getValue(), 1e-4);
        final Vector3D leakAcceleration0 =
                        new Vector3D(propagatorParameters.getDrivers().get(1).getValue(),
                                     propagatorParameters.getDrivers().get(3).getValue(),
                                     propagatorParameters.getDrivers().get(5).getValue());
        Assertions.assertEquals(leakAccelerationNorm0, leakAcceleration0.getNorm(), 1.0e-9);

        final Vector3D leakAcceleration1 =
                        new Vector3D(propagatorParameters.getDrivers().get(2).getValue(),
                                     propagatorParameters.getDrivers().get(4).getValue(),
                                     propagatorParameters.getDrivers().get(6).getValue());
        Assertions.assertEquals(leakAccelerationNorm1, leakAcceleration1.getNorm(), 1.0e-13);

        // Test on measurements parameters
        // -------------------------------

        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>(kalmanW3B.getMeasurementsParameters().getDrivers());
        sortParametersChanges(list);

        // Station CastleRock
        Assertions.assertEquals(castleAzElBias[0], FastMath.toDegrees(list.get(0).getValue()), angleAccuracy);
        Assertions.assertEquals(castleAzElBias[1], FastMath.toDegrees(list.get(1).getValue()), angleAccuracy);
        Assertions.assertEquals(castleRangeBias,   list.get(2).getValue(),                     distanceAccuracy);

        // Station Fucino
        Assertions.assertEquals(fucAzElBias[0], FastMath.toDegrees(list.get(3).getValue()), angleAccuracy);
        Assertions.assertEquals(fucAzElBias[1], FastMath.toDegrees(list.get(4).getValue()), angleAccuracy);
        Assertions.assertEquals(fucRangeBias,   list.get(5).getValue(),                     distanceAccuracy);

        // Station Kumsan
        Assertions.assertEquals(kumAzElBias[0], FastMath.toDegrees(list.get(6).getValue()), angleAccuracy);
        Assertions.assertEquals(kumAzElBias[1], FastMath.toDegrees(list.get(7).getValue()), angleAccuracy);
        Assertions.assertEquals(kumRangeBias,   list.get(8).getValue(),                     distanceAccuracy);

        // Station Pretoria
        Assertions.assertEquals(preAzElBias[0], FastMath.toDegrees(list.get( 9).getValue()), angleAccuracy);
        Assertions.assertEquals(preAzElBias[1], FastMath.toDegrees(list.get(10).getValue()), angleAccuracy);
        Assertions.assertEquals(preRangeBias,   list.get(11).getValue(),                     distanceAccuracy);

        // Station Uralla
        Assertions.assertEquals(uraAzElBias[0], FastMath.toDegrees(list.get(12).getValue()), angleAccuracy);
        Assertions.assertEquals(uraAzElBias[1], FastMath.toDegrees(list.get(13).getValue()), angleAccuracy);
        Assertions.assertEquals(uraRangeBias,   list.get(14).getValue(),                     distanceAccuracy);


        // Test on statistic for the range residuals
        final long nbRange = 182;
        Assertions.assertEquals(nbRange, kalmanW3B.getRangeStat().getN());
        Assertions.assertEquals(refStatRange[0], kalmanW3B.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(refStatRange[1], kalmanW3B.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(refStatRange[2], kalmanW3B.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(refStatRange[3], kalmanW3B.getRangeStat().getStandardDeviation(), distanceAccuracy);

        //test on statistic for the azimuth residuals
        final long nbAzi = 339;
        Assertions.assertEquals(nbAzi, kalmanW3B.getAzimStat().getN());
        Assertions.assertEquals(refStatAzi[0], kalmanW3B.getAzimStat().getMin(),               angleAccuracy);
        Assertions.assertEquals(refStatAzi[1], kalmanW3B.getAzimStat().getMax(),               angleAccuracy);
        Assertions.assertEquals(refStatAzi[2], kalmanW3B.getAzimStat().getMean(),              angleAccuracy);
        Assertions.assertEquals(refStatAzi[3], kalmanW3B.getAzimStat().getStandardDeviation(), angleAccuracy);

        //test on statistic for the elevation residuals
        final long nbEle = 339;
        Assertions.assertEquals(nbEle, kalmanW3B.getElevStat().getN());
        Assertions.assertEquals(refStatEle[0], kalmanW3B.getElevStat().getMin(),               angleAccuracy);
        Assertions.assertEquals(refStatEle[1], kalmanW3B.getElevStat().getMax(),               angleAccuracy);
        Assertions.assertEquals(refStatEle[2], kalmanW3B.getElevStat().getMean(),              angleAccuracy);
        Assertions.assertEquals(refStatEle[3], kalmanW3B.getElevStat().getStandardDeviation(), angleAccuracy);

        RealMatrix covariances = kalmanW3B.getCovariances();
        Assertions.assertEquals(28, covariances.getRowDimension());
        Assertions.assertEquals(28, covariances.getColumnDimension());

        // drag coefficient variance
        Assertions.assertEquals(dragVariance, covariances.getEntry(6, 6), 1.0e-6);

        // leak-X constant term variance
        Assertions.assertEquals(leakXVariance, covariances.getEntry(7, 7), 1.0e-16);

        // leak-Y constant term variance
        Assertions.assertEquals(leakYVariance, covariances.getEntry(9, 9), 1.0e-16);

        // leak-Z constant term variance
        Assertions.assertEquals(leakZVariance, covariances.getEntry(11, 11), 1.0e-16);

        // Test on orbital parameters
        // Done at the end to avoid changing the estimated propagation parameters
        // ----------------------------------------------------------------------

        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanW3B.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanW3B.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-40541446.255, -9905357.41, 206777.413);
        final Vector3D refVel0 = new Vector3D(759.0685, -1476.5156, 54.793);

        // Gather the selected propagation parameters and initialize them to the values found
        // with the batch LS method
        final ParameterDriversList refPropagationParameters = kalmanW3B.getPropagatorParameters();
        final double dragCoefRef = -0.215433133145843;
        final double[] leakXRef = {+5.69040439901955E-06, 1.09710906802403E-11};
        final double[] leakYRef = {-7.66440256777678E-07, 1.25467464335066E-10};
        final double[] leakZRef = {-5.574055079952E-06  , 2.78703463746911E-10};

        for (DelegatingDriver driver : refPropagationParameters.getDrivers()) {
            switch (driver.getName()) {
                case "drag coefficient" : driver.setValue(dragCoefRef); break;
                case "leak-X[0]"        : driver.setValue(leakXRef[0]); break;
                case "leak-X[1]"        : driver.setValue(leakXRef[1]); break;
                case "leak-Y[0]"        : driver.setValue(leakYRef[0]); break;
                case "leak-Y[1]"        : driver.setValue(leakYRef[1]); break;
                case "leak-Z[0]"        : driver.setValue(leakZRef[0]); break;
                case "leak-Z[1]"        : driver.setValue(leakZRef[1]); break;
            }
        }

        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, refPropagationParameters,
                                            kalmanW3B.getEstimatedPV().getDate());

        // Test on last orbit
        final Vector3D refPos = refOrbit.getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();

        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        Assertions.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), predictionDistanceAccuracy);
        Assertions.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), predictionVelocityAccuracy);

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
    }

}
