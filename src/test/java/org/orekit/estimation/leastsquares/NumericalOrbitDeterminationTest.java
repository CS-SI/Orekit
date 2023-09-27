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
package org.orekit.estimation.leastsquares;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultBatchLeastSquares;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class NumericalOrbitDeterminationTest extends AbstractOrbitDetermination<NumericalPropagatorBuilder> {

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
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {

    	// input in resources directory
        final String inputPath = NumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odLageos2 = runBLS(input, false);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 0.62;
        final double velocityAccuracy = 1.4e-4;

        //test on the convergence
        final int numberOfIte  = 5;
        final int numberOfEval = 5;

        Assertions.assertEquals(numberOfIte, odLageos2.getNumberOfIteration());
        Assertions.assertEquals(numberOfEval, odLageos2.getNumberOfEvaluation());

        //test on the estimated position and velocity
        final Vector3D estimatedPos = odLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odLageos2.getEstimatedPV().getVelocity();
        // Ref position from "lageos2_cpf_160213_5451.jax"
        final Vector3D refPos = new Vector3D(7526994.072, -9646309.832, 1464110.239);
        final Vector3D refVel = new Vector3D(3033.794, 1715.265, -4447.659);
        Assertions.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assertions.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 95;
        final double[] RefStatRange = { -0.756, 0.845, 0.0, 0.261 };
        Assertions.assertEquals(nbRange, odLageos2.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], odLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[1], odLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[2], odLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(RefStatRange[3], odLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);
    }

    @Test
    // Orbit determination for GNSS satellite based on range measurements
    public void testGNSS()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {

    	// input in resources directory
        final String inputPath = NumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/GNSS/od_test_GPS07.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odGNSS = runBLS(input, false);

        //test
        //definition of the accuracy for the test

        final double distanceAccuracy = 1.06;
        final double velocityAccuracy = 2.26e-3;

        //test on the convergence
        final int numberOfIte  = 2;
        final int numberOfEval = 3;

        Assertions.assertEquals(numberOfIte, odGNSS.getNumberOfIteration());
        Assertions.assertEquals(numberOfEval, odGNSS.getNumberOfEvaluation());

        //test on the estimated position and velocity (reference from IGS-MGEX file com18836.sp3)
        final Vector3D estimatedPos = odGNSS.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odGNSS.getEstimatedPV().getVelocity();
        final Vector3D refPos = new Vector3D(-2747606.680868164, 22572091.30648564, 13522761.402325712);
        final Vector3D refVel = new Vector3D(-2729.5151218788005, 1142.6629459030657, -2523.9055974487947);
        Assertions.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assertions.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on statistic for the range residuals
        final long nbRangeInit     = 8981;
        final long nbRangeExcluded = 305;
        final double[] RefStatRange = { -3.847, 8.307, 0.0, 0.873 };
        Assertions.assertEquals(nbRangeInit - nbRangeExcluded, odGNSS.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], odGNSS.getRangeStat().getMin(),               1.0e-3);
        Assertions.assertEquals(RefStatRange[1], odGNSS.getRangeStat().getMax(),               1.0e-3);
        Assertions.assertEquals(RefStatRange[2], odGNSS.getRangeStat().getMean(),              1.0e-3);
        Assertions.assertEquals(RefStatRange[3], odGNSS.getRangeStat().getStandardDeviation(), 1.0e-3);

    }

    @Test
    // Orbit determination for range, azimuth elevation measurements
    public void testW3B()
        throws URISyntaxException, IllegalArgumentException, IOException,
              OrekitException, ParseException {

    	// input in resources directory
        final String inputPath = NumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/W3B/od_test_W3.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/W3B:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odsatW3 = runBLS(input, false);   
        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 0.1;
        final double velocityAccuracy = 1e-4;
        final double angleAccuracy    = 1e-5;

        //test on the convergence (with some margins)
        Assertions.assertTrue(odsatW3.getNumberOfIteration()  <  6);
        Assertions.assertTrue(odsatW3.getNumberOfEvaluation() < 10);

        //test on the estimated position and velocity
        final Vector3D estimatedPos = odsatW3.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odsatW3.getEstimatedPV().getVelocity();
        final Vector3D refPos = new Vector3D(-40541446.255, -9905357.41, 206777.413);
        final Vector3D refVel = new Vector3D(759.0685, -1476.5156, 54.793);
        Assertions.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assertions.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on propagator parameters
        final double dragCoef  = -0.2154;
        final ParameterDriversList propagatorParameters = odsatW3.getPropagatorParameters();
        Assertions.assertEquals(dragCoef, propagatorParameters.getDrivers().get(0).getValue(), 1e-3);
        final Vector3D leakAcceleration0 =
                        new Vector3D(propagatorParameters.getDrivers().get(1).getValue(),
                                     propagatorParameters.getDrivers().get(3).getValue(),
                                     propagatorParameters.getDrivers().get(5).getValue());
        //Assertions.assertEquals(7.215e-6, leakAcceleration.getNorm(), 1.0e-8);
        Assertions.assertEquals(8.002e-6, leakAcceleration0.getNorm(), 1.0e-8);
        final Vector3D leakAcceleration1 =
                        new Vector3D(propagatorParameters.getDrivers().get(2).getValue(),
                                     propagatorParameters.getDrivers().get(4).getValue(),
                                     propagatorParameters.getDrivers().get(6).getValue());
        Assertions.assertEquals(3.058e-10, leakAcceleration1.getNorm(), 1.0e-12);

        //test on measurements parameters
        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>();
        list.addAll(odsatW3.getMeasurementsParameters().getDrivers());
        sortParametersChanges(list);

        //station CastleRock
        final double[] CastleAzElBias  = { 0.062701342, -0.003613508 };
        final double   CastleRangeBias = 11274.4677;
        Assertions.assertEquals(CastleAzElBias[0], FastMath.toDegrees(list.get(0).getValue()), angleAccuracy);
        Assertions.assertEquals(CastleAzElBias[1], FastMath.toDegrees(list.get(1).getValue()), angleAccuracy);
        Assertions.assertEquals(CastleRangeBias,   list.get(2).getValue(),                     distanceAccuracy);

        //station Fucino
        final double[] FucAzElBias  = { -0.053526137, 0.075483886 };
        final double   FucRangeBias = 13467.8256;
        Assertions.assertEquals(FucAzElBias[0], FastMath.toDegrees(list.get(3).getValue()), angleAccuracy);
        Assertions.assertEquals(FucAzElBias[1], FastMath.toDegrees(list.get(4).getValue()), angleAccuracy);
        Assertions.assertEquals(FucRangeBias,   list.get(5).getValue(),                     distanceAccuracy);

        //station Kumsan
        final double[] KumAzElBias  = { -0.023574208, -0.054520756 };
        final double   KumRangeBias = 13512.57594;
        Assertions.assertEquals(KumAzElBias[0], FastMath.toDegrees(list.get(6).getValue()), angleAccuracy);
        Assertions.assertEquals(KumAzElBias[1], FastMath.toDegrees(list.get(7).getValue()), angleAccuracy);
        Assertions.assertEquals(KumRangeBias,   list.get(8).getValue(),                     distanceAccuracy);

        //station Pretoria
        final double[] PreAzElBias = { 0.030201539, 0.009747877 };
        final double PreRangeBias = 13594.11889;
        Assertions.assertEquals(PreAzElBias[0], FastMath.toDegrees(list.get( 9).getValue()), angleAccuracy);
        Assertions.assertEquals(PreAzElBias[1], FastMath.toDegrees(list.get(10).getValue()), angleAccuracy);
        Assertions.assertEquals(PreRangeBias,   list.get(11).getValue(),                     distanceAccuracy);

        //station Uralla
        final double[] UraAzElBias = { 0.167814449, -0.12305252 };
        final double UraRangeBias = 13450.26738;
        Assertions.assertEquals(UraAzElBias[0], FastMath.toDegrees(list.get(12).getValue()), angleAccuracy);
        Assertions.assertEquals(UraAzElBias[1], FastMath.toDegrees(list.get(13).getValue()), angleAccuracy);
        Assertions.assertEquals(UraRangeBias,   list.get(14).getValue(),                     distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 182;
        //statistics for the range residual (min, max, mean, std)
        final double[] RefStatRange = { -18.39149369, 12.54165259, -4.32E-05, 4.374712716 };
        Assertions.assertEquals(nbRange, odsatW3.getRangeStat().getN());
        Assertions.assertEquals(RefStatRange[0], odsatW3.getRangeStat().getMin(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[1], odsatW3.getRangeStat().getMax(),               distanceAccuracy);
        Assertions.assertEquals(RefStatRange[2], odsatW3.getRangeStat().getMean(),              distanceAccuracy);
        Assertions.assertEquals(RefStatRange[3], odsatW3.getRangeStat().getStandardDeviation(), distanceAccuracy);

        //test on statistic for the azimuth residuals
        final long nbAzi = 339;
        //statistics for the azimuth residual (min, max, mean, std)
        final double[] RefStatAzi = { -0.043033616, 0.025297558, -1.39E-10, 0.010063041 };
        Assertions.assertEquals(nbAzi, odsatW3.getAzimStat().getN());
        Assertions.assertEquals(RefStatAzi[0], odsatW3.getAzimStat().getMin(),               angleAccuracy);
        Assertions.assertEquals(RefStatAzi[1], odsatW3.getAzimStat().getMax(),               angleAccuracy);
        Assertions.assertEquals(RefStatAzi[2], odsatW3.getAzimStat().getMean(),              angleAccuracy);
        Assertions.assertEquals(RefStatAzi[3], odsatW3.getAzimStat().getStandardDeviation(), angleAccuracy);

        //test on statistic for the elevation residuals
        final long nbEle = 339;
        final double[] RefStatEle = { -0.025061971, 0.056294405, -4.10E-11, 0.011604931 };
        Assertions.assertEquals(nbEle, odsatW3.getElevStat().getN());
        Assertions.assertEquals(RefStatEle[0], odsatW3.getElevStat().getMin(),               angleAccuracy);
        Assertions.assertEquals(RefStatEle[1], odsatW3.getElevStat().getMax(),               angleAccuracy);
        Assertions.assertEquals(RefStatEle[2], odsatW3.getElevStat().getMean(),              angleAccuracy);
        Assertions.assertEquals(RefStatEle[3], odsatW3.getElevStat().getStandardDeviation(), angleAccuracy);

        RealMatrix covariances = odsatW3.getCovariances();
        Assertions.assertEquals(28, covariances.getRowDimension());
        Assertions.assertEquals(28, covariances.getColumnDimension());

        // drag coefficient variance
        Assertions.assertEquals(0.687998, covariances.getEntry(6, 6), 1.0e-4);

        // leak-X constant term variance
        Assertions.assertEquals(2.0540e-12, covariances.getEntry(7, 7), 1.0e-16);

        // leak-Y constant term variance
        Assertions.assertEquals(2.4930e-11, covariances.getEntry(9, 9), 1.0e-15);

        // leak-Z constant term variance
        Assertions.assertEquals(7.6720e-11, covariances.getEntry(11, 11), 1.0e-15);
    }

}
