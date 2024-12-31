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
import java.util.*;

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
        final double distanceAccuracy = 2.47;
        final double velocityAccuracy = 1.1e-3;

        // Smoother position/velocity accuracy
        final double smoothDistanceAccuracy = 5.13;
        final double smoothVelocityAccuracy = 2.6e-3;
        final double distanceStd = 12.79;
        final double velocityStd = 6.2e-3;

        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.043889,  0.044726,  -0.037800 };
        final double rangeBias = 0.041171;

        // Batch LS values
        //final double[] refStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] refStatRange = { -5.910596, 3.306810, -0.037121, 1.454286 };

        testLageos2(distanceAccuracy, velocityAccuracy, stationOffSet, rangeBias, refStatRange,
                smoothDistanceAccuracy, smoothVelocityAccuracy, distanceStd, velocityStd,
                false, false);
    }

    @Test
    public void testLageos2Unscented() throws URISyntaxException, IOException {

        // Position/velocity accuracy
        final double distanceAccuracy = 2.46;
        final double velocityAccuracy = 1.8e-4;

        // Smoother position/velocity accuracy
        final double smoothDistanceAccuracy = 3.97;
        final double smoothVelocityAccuracy = 2.0e-3;
        final double distanceStd = 19.65;
        final double velocityStd = 0.011;

        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.044850,  0.030216,  -0.035853 };
        final double rangeBias = 0.035252;

        // Batch LS values
        //final double[] refStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] refStatRange = { -6.212086, 3.196686, -0.012196, 1.456780 };

        testLageos2(distanceAccuracy, velocityAccuracy, stationOffSet, rangeBias, refStatRange,
                smoothDistanceAccuracy, smoothVelocityAccuracy, distanceStd, velocityStd,
                false, true);
    }


    // Orbit determination for Lageos2 based on SLR (range) measurements
    protected void testLageos2(final double distanceAccuracy, final double velocityAccuracy,
                               final double[] stationOffSet, final double rangeBias, final double[] refStatRange,
                               final double smoothDistanceAccuracy, final double smoothVelocityAccuracy,
                               final double smoothDistanceStd, final double smoothVelocityStd,
                               final boolean print, final boolean isUnscented) throws URISyntaxException, IOException {

        // input in resources directory
        final String inputPath = SequentialNumericalOrbitDeterminationTest.class.getClassLoader()
                .getResource("orbit-determination/Lageos2/kalman_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;

        // Cartesian covariance matrix initialization
        final double posVar = FastMath.pow(1e3, 2);
        final double velVar = FastMath.pow(1.0, 2);
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                posVar, posVar, posVar, velVar, velVar, velVar
        });

        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealMatrix(6, 6);

        // Initial measurement covariance matrix and process noise matrix
        final double measVar = FastMath.pow(1.0, 2);
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                measVar, measVar, measVar, measVar
        });
        final RealMatrix measurementQ = MatrixUtils.createRealMatrix(4, 4);

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

        Assertions.assertEquals(0.0, dP, distanceAccuracy);
        Assertions.assertEquals(0.0, dV, velocityAccuracy);

        // Run the reference to initial date
        final Orbit initialOrbit = runReference(input, orbitType, refPos0, refVel0, null,
                kalmanLageos2.getSmoothedState().getDate());
        final Vector3D initialPos = initialOrbit.getPosition();
        final Vector3D initialVel = initialOrbit.getPVCoordinates().getVelocity();

        // Check smoother distances
        final double[] smoothedState = kalmanLageos2.getSmoothedState().getState().toArray();
        final Vector3D smoothedPos = new Vector3D(smoothedState[0], smoothedState[1], smoothedState[2]);
        final Vector3D smoothedVel = new Vector3D(smoothedState[3], smoothedState[4], smoothedState[5]);
        final double dPSmooth = Vector3D.distance(initialPos, smoothedPos);
        final double dVSmooth = Vector3D.distance(initialVel, smoothedVel);

        // Check smoother variances
        final RealMatrix smoothedCov = kalmanLageos2.getSmoothedState().getCovarianceMatrix();
        final double posStd = FastMath.sqrt(smoothedCov.getEntry(0, 0) +
                smoothedCov.getEntry(1, 1) + smoothedCov.getEntry(2, 2));
        final double velStd = FastMath.sqrt(smoothedCov.getEntry(3, 3) +
                smoothedCov.getEntry(4, 4) + smoothedCov.getEntry(5, 5));

        // Print smoother orbit deltas
        if (print) {
            System.out.println("Smoother performances:");
            System.out.format("\t%-30s\n",
                    "ΔEstimated / Reference & std. dev.");
            System.out.format(Locale.US, "\t%-10s %20.6f %20.6f\n",
                    "ΔP [m]", dPSmooth, posStd);
            System.out.format(Locale.US, "\t%-10s %20.6f %20.6f\n",
                    "ΔV [m/s]", dVSmooth, velStd);
        }

        Assertions.assertEquals(0.0, dPSmooth, smoothDistanceAccuracy);
        Assertions.assertEquals(0.0, dVSmooth, smoothVelocityAccuracy);
        Assertions.assertEquals(0.0, posStd, smoothDistanceStd);
        Assertions.assertEquals(0.0, velStd, smoothVelocityStd);


        // Accuracy for tests
        final double parametersAccuracy = 1e-6;

        // Test on measurements parameters
        final List<DelegatingDriver> list = new ArrayList<>(kalmanLageos2.getMeasurementsParameters().getDrivers());
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
        final double dragCoef  = 2.0010;

        // Batch LS results: 8.002e-6
        final double leakAccelerationNorm0 = 2.7e-9;

        // Batch LS results: 3.058e-10
        final double leakAccelerationNorm1 = 8.1e-13;

        // Batch LS results
        // final double[] CastleAzElBias  = { 0.062701342, -0.003613508 };
        // final double   CastleRangeBias = 11274.4677;
        final double[] castleAzElBias  = { 0.086136, -0.032682};
        final double   castleRangeBias = 11473.6163;

        // Batch LS results
        // final double[] FucAzElBias  = { -0.053526137, 0.075483886 };
        // final double   FucRangeBias = 13467.8256;
        final double[] fucAzElBias  = { -0.067443, 0.064581 };
        final double   fucRangeBias = 13468.9624;

        // Batch LS results
        // final double[] KumAzElBias  = { -0.023574208, -0.054520756 };
        // final double   KumRangeBias = 13512.57594;
        final double[] kumAzElBias  = { -0.000102, -0.066097 };
        final double   kumRangeBias = 13527.0004;

        // Batch LS results
        // final double[] PreAzElBias = { 0.030201539, 0.009747877 };
        // final double PreRangeBias = 13594.11889;
        final double[] preAzElBias = { 0.029973, 0.011140 };
        final double   preRangeBias = 13370.1890;

        // Batch LS results
        // final double[] UraAzElBias = { 0.167814449, -0.12305252 };
        // final double UraRangeBias = 13450.26738;
        final double[] uraAzElBias = { 0.148459, -0.138353 };
        final double   uraRangeBias = 13314.9300;

        //statistics for the range residual (min, max, mean, std)
        final double[] refStatRange = { -0.5948, 35.4751, 0.3061, 2.7790 };

        //statistics for the azimuth residual (min, max, mean, std)
        final double[] refStatAzi = { -0.024691, 0.020452, -0.001111, 0.006750 };

        //statistics for the elevation residual (min, max, mean, std)
        final double[] refStatEle = { -0.030255, 0.026288, 0.002044, 0.007260 };

        // Expected covariance
        final double dragVariance = 0.999722;
        final double leakXVariance = 0.9999e-12;
        final double leakYVariance = 1e-12;
        final double leakZVariance = 1e-12;

        // Prediction position/velocity accuracies
        // FIXME: debug - Comparison with batch LS is bad
        final double predictionDistanceAccuracy = 2127.85;
        final double predictionVelocityAccuracy = 1.073;

        testW3B(dragCoef, leakAccelerationNorm0, leakAccelerationNorm1,
                castleAzElBias, castleRangeBias, fucAzElBias, fucRangeBias, kumAzElBias, kumRangeBias,
                preAzElBias, preRangeBias, uraAzElBias, uraRangeBias,
                refStatRange, refStatAzi, refStatEle, dragVariance,
                leakXVariance, leakYVariance, leakZVariance,
                predictionDistanceAccuracy, predictionVelocityAccuracy, false, false);
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
        final double positionP = FastMath.pow(1e4, 2);
        final double velocityP = FastMath.pow(10.0, 2);
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                positionP, positionP, positionP, velocityP, velocityP, velocityP
        });

        // Orbital Cartesian process noise matrix (Q)
        final double positionQ = FastMath.pow(100.0, 2);
        final double velocityQ = FastMath.pow(1.0, 2);
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
                positionQ, positionQ, positionQ, velocityQ, velocityQ, velocityQ
        });

        // Propagation covariance and process noise matrices
        final RealMatrix propagationP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                FastMath.pow(1.0, 2), // Cd
                FastMath.pow(1e-6, 2), FastMath.pow(1e-10, 2),   // leak-X
                FastMath.pow(1e-6, 2), FastMath.pow(1e-10, 2), // leak-Y
                FastMath.pow(1e-6, 2), FastMath.pow(1e-10, 2)  // leak-Z
        });
        final RealMatrix propagationQ = MatrixUtils.createRealMatrix(7, 7);

        // Measurement covariance and process noise matrices
        final double angularVariance = FastMath.pow(1e-2, 2);
        final double rangeVariance   = FastMath.pow(10.0, 2);
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
                angularVariance, angularVariance, rangeVariance,
                angularVariance, angularVariance, rangeVariance,
                angularVariance, angularVariance, rangeVariance,
                angularVariance, angularVariance, rangeVariance,
                angularVariance, angularVariance, rangeVariance,
        });
        final RealMatrix measurementQ = MatrixUtils.createRealMatrix(15, 15);

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

        final List<DelegatingDriver> list = new ArrayList<>(kalmanW3B.getMeasurementsParameters().getDrivers());
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

        Assertions.assertEquals(0.0, dP, predictionDistanceAccuracy);
        Assertions.assertEquals(0.0, dV, predictionVelocityAccuracy);

    }

}
