/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.SummaryStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.Angular;
import org.orekit.estimation.measurements.Bias;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.MarshallSolarActivityFutureEstimation;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;


public class OrbitDeterminationTest {

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {

        // input in tutorial resources directory/output
        final String inputPath = OrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/Lageos2:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultOD odLageos2 = run(input);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 0.1;
        final double velocityAccuracy = 1e-4;

        //test on the convergence
        final int numberOfIte  = 3;
        final int numberOfEval = 4;

        Assert.assertEquals(numberOfIte, odLageos2.getNumberOfIteration());
        Assert.assertEquals(numberOfEval, odLageos2.getNumberOfEvaluation());

        //test on the estimated position and velocity
        final Vector3D estimatedPos = odLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odLageos2.getEstimatedPV().getVelocity();
        final Vector3D refPos = new Vector3D(-5532124.989973327, 10025700.01763335, -3578940.840115321);
        final Vector3D refVel = new Vector3D(-3871.2736402553, -607.8775965705, 4280.9744110925);
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on measurements parameters
        final double[] stationOffSet = { -1.351682,  -2.180542,  -5.278784 };
        final double rangeBias = -7.923393;
        Assert.assertEquals(stationOffSet[0], odLageos2.measurementsParameters.get(0).getValue()[0], distanceAccuracy);
        Assert.assertEquals(stationOffSet[1], odLageos2.measurementsParameters.get(0).getValue()[1], distanceAccuracy);
        Assert.assertEquals(stationOffSet[2], odLageos2.measurementsParameters.get(0).getValue()[2], distanceAccuracy);
        Assert.assertEquals(rangeBias,        odLageos2.measurementsParameters.get(1).getValue()[0], distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        final double[] RefStatRange = { -2.795816, 6.171529, 0.310848, 1.657809 };
        Assert.assertEquals(nbRange, odLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], odLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], odLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], odLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    @Test
    // Orbit determination for range, azimuth elevation measurements
    public void testW3B()
        throws URISyntaxException, IllegalArgumentException, IOException,
              OrekitException, ParseException {

        // input in tutorial resources directory/output
        final String inputPath = OrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/W3B/od_test_W3.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/W3B:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultOD odsatW3 = run(input);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 1e-1;
        final double velocityAccuracy = 1e-4;
        // angle unit is radian
        final double angleAccuracy = 1e-5;
        final double dimensionLessCoef = 1e-3;

        //test on the convergence
        final int numberOfIte  = 4;
        final int numberOfEval = 5;

        Assert.assertEquals(numberOfIte, odsatW3.getNumberOfIteration());
        Assert.assertEquals(numberOfEval, odsatW3.getNumberOfEvaluation());

        //test on the estimated position and velocity
        final Vector3D estimatedPos = odsatW3.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = odsatW3.getEstimatedPV().getVelocity();
        final Vector3D refPos = new Vector3D( -40541691.15225173, -9903912.495473776, 208037.5511875451);
        final Vector3D refVel = new Vector3D( 759.0248098953, -1476.58298279, 54.7065550778);
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);


        //test on propagator parameters
        final double dragCoef  = 0.215421;
        final double SRPCoef = 112.336693;
        Assert.assertEquals(dragCoef, odsatW3.propagatorParameters.get(0).getValue()[0], dimensionLessCoef);
        Assert.assertEquals(SRPCoef,  odsatW3.propagatorParameters.get(1).getValue()[0], dimensionLessCoef);

        //test on measurements parameters
        //station CastleRock
        final double[] CastleAzElBias = { 0.061500,  -0.004955};
        final double CastleRangeBias = 11461.679992;
        Assert.assertEquals(CastleAzElBias[0], FastMath.toDegrees(odsatW3.measurementsParameters.get(0).getValue()[0]), angleAccuracy);
        Assert.assertEquals(CastleAzElBias[1], FastMath.toDegrees(odsatW3.measurementsParameters.get(0).getValue()[1]), angleAccuracy);
        Assert.assertEquals(CastleRangeBias,   odsatW3.measurementsParameters.get(1).getValue()[0],                     distanceAccuracy);

        //station Fucino
        final double[] FucAzElBias = {-0.055555,  0.075471};
        final double FucRangeBias = 13461.362346;
        Assert.assertEquals(FucAzElBias[0], FastMath.toDegrees(odsatW3.measurementsParameters.get(2).getValue()[0]), angleAccuracy);
        Assert.assertEquals(FucAzElBias[1], FastMath.toDegrees(odsatW3.measurementsParameters.get(2).getValue()[1]), angleAccuracy);
        Assert.assertEquals(FucRangeBias,   odsatW3.measurementsParameters.get(3).getValue()[0],                     distanceAccuracy);

        //station Kumsan
        final double[] KumAzElBias = { -0.025227,  -0.054883};
        final double KumRangeBias = 13521.236722;
        Assert.assertEquals(KumAzElBias[0], FastMath.toDegrees(odsatW3.measurementsParameters.get(4).getValue()[0]), angleAccuracy);
        Assert.assertEquals(KumAzElBias[1], FastMath.toDegrees(odsatW3.measurementsParameters.get(4).getValue()[1]), angleAccuracy);
        Assert.assertEquals(KumRangeBias,   odsatW3.measurementsParameters.get(5).getValue()[0],                     distanceAccuracy);

        //station Pretoria
        final double[] PreAzElBias = { 0.029580,  0.011505};
        final double PreRangeBias = 13373.739538;
        Assert.assertEquals(PreAzElBias[0], FastMath.toDegrees(odsatW3.measurementsParameters.get(6).getValue()[0]), angleAccuracy);
        Assert.assertEquals(PreAzElBias[1], FastMath.toDegrees(odsatW3.measurementsParameters.get(6).getValue()[1]), angleAccuracy);
        Assert.assertEquals(PreRangeBias,   odsatW3.measurementsParameters.get(7).getValue()[0],                     distanceAccuracy);

        //station Uralla
        final double[] UraAzElBias = { 0.168236,  -0.121083};
        final double UraRangeBias = 13294.762426;
        Assert.assertEquals(UraAzElBias[0], FastMath.toDegrees(odsatW3.measurementsParameters.get(8).getValue()[0]), angleAccuracy);
        Assert.assertEquals(UraAzElBias[1], FastMath.toDegrees(odsatW3.measurementsParameters.get(8).getValue()[1]), angleAccuracy);
        Assert.assertEquals(UraRangeBias,   odsatW3.measurementsParameters.get(9).getValue()[0],                     distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 182;
        //statistics for the range residual (min, max, mean, std)
        final double[] RefStatRange = { -18.407301888801157, 11.21531879529357, -1.3004374373196126E-5, 4.396892027885725 };
        Assert.assertEquals(nbRange, odsatW3.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odsatW3.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], odsatW3.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], odsatW3.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], odsatW3.getRangeStat().getStandardDeviation(), distanceAccuracy);

        //test on statistic for the azimuth residuals
        final long nbAzi = 339;
        //statistics for the azimuth residual (min, max, mean, std)
        final double[] RefStatAzi = { -0.04334642535664204, 0.025348556908738606, -3.471009794826308E-11, 0.010532646772836074 };
        Assert.assertEquals(nbAzi, odsatW3.getAzimStat().getN());
        Assert.assertEquals(RefStatAzi[0], odsatW3.getAzimStat().getMin(),               angleAccuracy);
        Assert.assertEquals(RefStatAzi[1], odsatW3.getAzimStat().getMax(),               angleAccuracy);
        Assert.assertEquals(RefStatAzi[2], odsatW3.getAzimStat().getMean(),              angleAccuracy);
        Assert.assertEquals(RefStatAzi[3], odsatW3.getAzimStat().getStandardDeviation(), angleAccuracy);

        //test on statistic for the azimuth residuals
        final long nbEle = 339;
        //statistics for the azimuth residual (min, max, mean, std)
        final double[] RefStatEle = { -0.024901037294786422, 0.0549815182281244, -1.2496574162566745E-11, 0.011712542337682996 };
        Assert.assertEquals(nbEle, odsatW3.getElevStat().getN());
        Assert.assertEquals(RefStatEle[0], odsatW3.getElevStat().getMin(),               angleAccuracy);
        Assert.assertEquals(RefStatEle[1], odsatW3.getElevStat().getMax(),               angleAccuracy);
        Assert.assertEquals(RefStatEle[2], odsatW3.getElevStat().getMean(),              angleAccuracy);
        Assert.assertEquals(RefStatEle[3], odsatW3.getElevStat().getStandardDeviation(), angleAccuracy);

    }

   private class ResultOD {
       private int numberOfIteration;
       private int numberOfEvaluation;
       private PVCoordinates estimatedPV;
       private SummaryStatistics rangeStat;
       private SummaryStatistics azimStat;
       private SummaryStatistics elevStat;
       private  List<ParameterDriver> propagatorParameters  ;
       private  List<ParameterDriver> measurementsParameters;
       ResultOD(List<ParameterDriver>  propagatorParameters,
                List<ParameterDriver>  measurementsParameters,
                int numberOfIteration, int numberOfEvaluation, PVCoordinates estimatedPV,
                SummaryStatistics rangeStat, SummaryStatistics rangeRateStat,
                SummaryStatistics azimStat, SummaryStatistics elevStat,
                SummaryStatistics posStat, SummaryStatistics velStat) {

           this.propagatorParameters   = propagatorParameters;
           this.measurementsParameters = measurementsParameters;
           this.numberOfIteration      = numberOfIteration;
           this.numberOfEvaluation     = numberOfEvaluation;
           this.estimatedPV            = estimatedPV;
           this.rangeStat              =  rangeStat;
           this.azimStat               = azimStat;
           this.elevStat               = elevStat;
       }


    public int getNumberOfIteration() {
        return numberOfIteration;
    }


    public int getNumberOfEvaluation() {
        return numberOfEvaluation;
    }


    public PVCoordinates getEstimatedPV() {
        return estimatedPV;
    }


    public SummaryStatistics getRangeStat() {
        return rangeStat;
    }



    public SummaryStatistics getAzimStat() {
        return azimStat;
    }


    public SummaryStatistics getElevStat() {
        return elevStat;
    }


   }

    private ResultOD run(final File input)
        throws IOException, IllegalArgumentException, OrekitException, ParseException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        parser.parseInput(input.getAbsolutePath(), new FileInputStream(input));

        // log file

        final RangeLog     rangeLog     = new RangeLog();
        final RangeRateLog rangeRateLog = new RangeRateLog();
        final AzimuthLog   azimuthLog   = new AzimuthLog();
        final ElevationLog elevationLog = new ElevationLog();
        final PositionLog  positionLog  = new PositionLog();
        final VelocityLog  velocityLog  = new VelocityLog();

        // gravity field
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-5c.gfc", true));
        final NormalizedSphericalHarmonicsProvider gravityField = createGravityField(parser);


        // Orbit initial guess
        final Orbit initialGuess = createOrbit(parser, gravityField.getMu());


        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }


        // central body
        final OneAxisEllipsoid body = createBody(parser);


        // propagator builder
        // Utils.setDataRoot("orbit-determination-data");

        final NumericalPropagatorBuilder propagatorBuilder =
                        createPropagatorBuilder(parser, conventions, gravityField, body, initialGuess);

        // estimator
        final BatchLSEstimator estimator = createEstimator(parser, propagatorBuilder);


        // measurements
        final List<Measurement<?>> measurements = new ArrayList<Measurement<?>>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {
            measurements.addAll(readMeasurements(new File(input.getParentFile(), fileName),
                                                 createStationsData(parser, body),
                                                 createPVData(parser),
                                                 createSatRangeBias(parser),
                                                 createWeights(parser)));
        }
        for (Measurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }

        Orbit estimated = estimator.estimate(initialGuess).getInitialState().getOrbit();

        // compute some statistics
        for (final Map.Entry<Measurement<?>, Evaluation<?>> entry : estimator.getLastEvaluations().entrySet()) {
            if (entry.getKey() instanceof Range) {
                @SuppressWarnings("unchecked")
                Evaluation<Range> evaluation = (Evaluation<Range>) entry.getValue();
                rangeLog.add(evaluation);
            } else if (entry.getKey() instanceof RangeRate) {
                @SuppressWarnings("unchecked")
                Evaluation<RangeRate> evaluation = (Evaluation<RangeRate>) entry.getValue();
                rangeRateLog.add(evaluation);
            } else if (entry.getKey() instanceof Angular) {
                @SuppressWarnings("unchecked")
                Evaluation<Angular> evaluation = (Evaluation<Angular>) entry.getValue();
                azimuthLog.add(evaluation);
                elevationLog.add(evaluation);
            } else if (entry.getKey() instanceof PV) {
                @SuppressWarnings("unchecked")
                Evaluation<PV> evaluation = (Evaluation<PV>) entry.getValue();
                positionLog.add(evaluation);
                velocityLog.add(evaluation);
            }
        }

        // parmaters and measurements.
        final List<ParameterDriver> propagatorParameters   = estimator.getPropagatorParameters(true);
        final List<ParameterDriver> measurementsParameters = estimator.getMeasurementsParameters(true);

        //sort of the measurements parameters
        sortParametersChanges( measurementsParameters);

        //instation of results
        return new ResultOD(propagatorParameters, measurementsParameters,
                            estimator.getIterationsCount(), estimator.getEvaluationsCount(), estimated.getPVCoordinates(),
                            rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                            azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                            positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary());
    }

    /** Sort parameters changes.
     * @param parameters parameters list
     */
    private void sortParametersChanges(List<ParameterDriver> parameters) {

        // sort the parameters lexicographically
        Collections.sort(parameters, new Comparator<ParameterDriver>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final ParameterDriver pd1, final ParameterDriver pd2) {
                return pd1.getName().compareTo(pd2.getName());
            }

        });
    }


    /** Create a propagator builder from input parameters
     * @param parser input file parser
     * @param conventions IERS conventions to use
     * @param gravityField gravity field
     * @param body central body
     * @param orbit first orbit estimate
     * @return propagator builder
     * @throws NoSuchElementException if input parameters are missing
     * @throws OrekitException if body frame cannot be created
     */
    private NumericalPropagatorBuilder createPropagatorBuilder(final KeyValueFileParser<ParameterKey> parser,
                                                               final IERSConventions conventions,
                                                               final NormalizedSphericalHarmonicsProvider gravityField,
                                                               final OneAxisEllipsoid body,
                                                               final Orbit orbit)
        throws NoSuchElementException, OrekitException {

        final double minStep;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_MIN_STEP)) {
            minStep = 0.001;
        } else {
            minStep = parser.getDouble(ParameterKey.PROPAGATOR_MIN_STEP);
        }

        final double maxStep;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_MAX_STEP)) {
            maxStep = 300;
        } else {
            maxStep = parser.getDouble(ParameterKey.PROPAGATOR_MAX_STEP);
        }

        final double dP;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_POSITION_ERROR)) {
            dP = 10.0;
        } else {
            dP = parser.getDouble(ParameterKey.PROPAGATOR_POSITION_ERROR);
        }

        final NumericalPropagatorBuilder propagatorBuilder =
                        new NumericalPropagatorBuilder(gravityField.getMu(),
                                                       orbit.getFrame(),
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       orbit.getType(),
                                                       PositionAngle.MEAN);

        // initial mass
        final double mass;
        if (!parser.containsKey(ParameterKey.MASS)) {
            mass = 1000.0;
        } else {
            mass = parser.getDouble(ParameterKey.MASS);
        }
        propagatorBuilder.setMass(mass);

        // gravity field force model
        propagatorBuilder.addForceModel(new HolmesFeatherstoneAttractionModel(body.getBodyFrame(), gravityField));

        // ocean tides force model
        if (parser.containsKey(ParameterKey.OCEAN_TIDES_DEGREE) &&
            parser.containsKey(ParameterKey.OCEAN_TIDES_ORDER)) {
            final int degree = parser.getInt(ParameterKey.OCEAN_TIDES_DEGREE);
            final int order  = parser.getInt(ParameterKey.OCEAN_TIDES_ORDER);
            if (degree > 0 && order > 0) {
                propagatorBuilder.addForceModel(new OceanTides(body.getBodyFrame(),
                                                               gravityField.getAe(), gravityField.getMu(),
                                                               degree, order, conventions,
                                                               TimeScalesFactory.getUT1(conventions, true)));
            }
        }

        // solid tides force model
        List<CelestialBody> solidTidesBodies = new ArrayList<CelestialBody>();
        if (parser.containsKey(ParameterKey.SOLID_TIDES_SUN) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_SUN)) {
            solidTidesBodies.add(CelestialBodyFactory.getSun());
        }
        if (parser.containsKey(ParameterKey.SOLID_TIDES_MOON) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_MOON)) {
            solidTidesBodies.add(CelestialBodyFactory.getMoon());
        }
        if (!solidTidesBodies.isEmpty()) {
            propagatorBuilder.addForceModel(new SolidTides(body.getBodyFrame(),
                                                           gravityField.getAe(), gravityField.getMu(),
                                                           gravityField.getTideSystem(), conventions,
                                                           TimeScalesFactory.getUT1(conventions, true),
                                                           solidTidesBodies.toArray(new CelestialBody[solidTidesBodies.size()])));
        }

        // third body attraction
        if (parser.containsKey(ParameterKey.THIRD_BODY_SUN) &&
            parser.getBoolean(ParameterKey.THIRD_BODY_SUN)) {
            propagatorBuilder.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }
        if (parser.containsKey(ParameterKey.THIRD_BODY_MOON) &&
            parser.getBoolean(ParameterKey.THIRD_BODY_MOON)) {
            propagatorBuilder.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        // drag
        if (parser.containsKey(ParameterKey.DRAG) && parser.getBoolean(ParameterKey.DRAG)) {
            final double  cd          = parser.getDouble(ParameterKey.DRAG_CD);
            final double  area        = parser.getDouble(ParameterKey.DRAG_AREA);
            final boolean cdEstimated = parser.getBoolean(ParameterKey.DRAG_CD_ESTIMATED);

            MarshallSolarActivityFutureEstimation msafe =
                            new MarshallSolarActivityFutureEstimation("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)",
            MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.feed(msafe.getSupportedNames(), msafe);
            Atmosphere atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), body);
            propagatorBuilder.addForceModel(new DragForce(atmosphere, new IsotropicDrag(area, cd)));
            if (cdEstimated) {
                for (final ParameterDriver driver : propagatorBuilder.getParametersDrivers()) {
                    if (driver.getName().equals(DragSensitive.DRAG_COEFFICIENT)) {
                        driver.setEstimated(true);
                    }
                }
            }
        }

        // solar radiation pressure
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE) && parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE)) {
            final double  cr          = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR);
            final double  area        = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_AREA);
            final boolean cREstimated = parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE_CR_ESTIMATED);

            propagatorBuilder.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                                                       body.getEquatorialRadius(),
                                                                       new IsotropicRadiationSingleCoefficient(area, cr)));
            if (cREstimated) {
                for (final ParameterDriver driver : propagatorBuilder.getParametersDrivers()) {
                    if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                        driver.setEstimated(true);
                    }
                }
            }
        }

        // post-Newtonian correction force due to general relativity
        if (parser.containsKey(ParameterKey.GENERAL_RELATIVITY) && parser.getBoolean(ParameterKey.GENERAL_RELATIVITY)) {
            propagatorBuilder.addForceModel(new Relativity(gravityField.getMu()));
        }

        return propagatorBuilder;

    }

    /** Create a gravity field from input parameters
     * @param parser input file parser
     * @return gravity field
     * @throws NoSuchElementException if input parameters are missing
     * @throws OrekitException if body frame cannot be created
     */
    private NormalizedSphericalHarmonicsProvider createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException, OrekitException {
        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        return GravityFieldFactory.getNormalizedProvider(degree, order);
    }

    /** Create an orbit from input parameters
     * @param parser input file parser
     * @param mu     central attraction coefficient
     * @throws NoSuchElementException if input parameters are missing
     * @throws OrekitException if body frame cannot be created
     */
    private OneAxisEllipsoid createBody(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException, OrekitException {

        final Frame bodyFrame;
        if (!parser.containsKey(ParameterKey.BODY_FRAME)) {
            bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        } else {
            bodyFrame = parser.getEarthFrame(ParameterKey.BODY_FRAME);
        }

        final double equatorialRadius;
        if (!parser.containsKey(ParameterKey.BODY_EQUATORIAL_RADIUS)) {
            equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        } else {
            equatorialRadius = parser.getDouble(ParameterKey.BODY_EQUATORIAL_RADIUS);
        }

        final double flattening;
        if (!parser.containsKey(ParameterKey.BODY_INVERSE_FLATTENING)) {
            flattening = Constants.WGS84_EARTH_FLATTENING;
        } else {
            flattening = 1.0 / parser.getDouble(ParameterKey.BODY_INVERSE_FLATTENING);
        }

        return new OneAxisEllipsoid(equatorialRadius, flattening, bodyFrame);

    }

    /** Create an orbit from input parameters
     * @param parser input file parser
     * @param mu     central attraction coefficient
     * @throws NoSuchElementException if input parameters are missing
     * @throws OrekitException if inertial frame cannot be created
     */
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser,
                              final double mu)
        throws NoSuchElementException, OrekitException {

        final Frame frame;
        if (!parser.containsKey(ParameterKey.INERTIAL_FRAME)) {
            frame = FramesFactory.getEME2000();
        } else {
            frame = parser.getInertialFrame(ParameterKey.INERTIAL_FRAME);
        }

        // Orbit definition
        PositionAngle angleType = PositionAngle.MEAN;
        if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
            angleType = PositionAngle.valueOf(parser.getString(ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
        }
        if (parser.containsKey(ParameterKey.ORBIT_KEPLERIAN_A)) {
            return new KeplerianOrbit(parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_A),
                                      parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_E),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_I),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_PA),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_RAAN),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_ANOMALY),
                                      angleType,
                                      frame,
                                      parser.getDate(ParameterKey.ORBIT_DATE,
                                                     TimeScalesFactory.getUTC()),
                                      mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_EQUINOCTIAL_A)) {
            return new EquinoctialOrbit(parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_A),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EX),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EY),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HX),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HY),
                                        parser.getAngle(ParameterKey.ORBIT_EQUINOCTIAL_LAMBDA),
                                        angleType,
                                        frame,
                                        parser.getDate(ParameterKey.ORBIT_DATE,
                                                       TimeScalesFactory.getUTC()),
                                        mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_CIRCULAR_A)) {
            return new CircularOrbit(parser.getDouble(ParameterKey.ORBIT_CIRCULAR_A),
                                     parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EX),
                                     parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EY),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_I),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_RAAN),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_ALPHA),
                                     angleType,
                                     frame,
                                     parser.getDate(ParameterKey.ORBIT_DATE,
                                                    TimeScalesFactory.getUTC()),
                                     mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_TLE_LINE_1)) {
            final String line1 = parser.getString(ParameterKey.ORBIT_TLE_LINE_1);
            final String line2 = parser.getString(ParameterKey.ORBIT_TLE_LINE_2);
            final TLE tle = new TLE(line1, line2);

            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
           // propagator.setEphemerisMode();

            AbsoluteDate initDate = tle.getDate();
            SpacecraftState initialState = propagator.getInitialState();


            //Transformation from TEME to frame.
            Transform t =FramesFactory.getTEME().getTransformTo(FramesFactory.getEME2000(), initDate.getDate());
            return new CartesianOrbit( t.transformPVCoordinates(initialState.getPVCoordinates()) ,
                                      frame,
                                      initDate,
                                       mu);


        } else {
            final double[] pos = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PX),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PY),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PZ)};
            final double[] vel = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VX),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VY),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VZ)};

            return new CartesianOrbit(new PVCoordinates(new Vector3D(pos), new Vector3D(vel)),
                                      frame,
                                      parser.getDate(ParameterKey.ORBIT_DATE,
                                                     TimeScalesFactory.getUTC()),
                                      mu);
        }

    }

    /** Set up range bias due to transponder delay.
     * @param parser input file parser
     * @param range bias (may be null if bias is fixed to zero)
     * @exception OrekitException if bias initial value cannot be set
     */
    private Bias<Range> createSatRangeBias(final KeyValueFileParser<ParameterKey> parser)
        throws OrekitException {

        // transponder delay
        final double transponderDelayBias;
        if (!parser.containsKey(ParameterKey.TRANSPONDER_DELAY_BIAS)) {
            transponderDelayBias = 0;
        } else {
            transponderDelayBias = parser.getDouble(ParameterKey.TRANSPONDER_DELAY_BIAS);
        }

        // bias estimation flag
        final boolean transponderDelayBiasEstimated;
        if (!parser.containsKey(ParameterKey.TRANSPONDER_DELAY_BIAS_ESTIMATED)) {
            transponderDelayBiasEstimated = false;
        } else {
            transponderDelayBiasEstimated = parser.getBoolean(ParameterKey.TRANSPONDER_DELAY_BIAS_ESTIMATED);
        }

        if (FastMath.abs(transponderDelayBias) >= Precision.SAFE_MIN || transponderDelayBiasEstimated) {
            // bias is either non-zero or will be estimated,
            // we really need to create a modifier for this
            final Bias<Range> bias = new Bias<Range>("transponder delay bias", transponderDelayBias);
            bias.getDriver().setEstimated(transponderDelayBiasEstimated);
            return bias;
        } else {
            // fixed zero bias, we don't need any modifier
            return null;
        }

    }

    /** Set up stations.
     * @param parser input file parser
     * @param body central body
     * @return name to station data map
     * @exception OrekitException if some frame transforms cannot be computed
     * @throws NoSuchElementException if input parameters are missing
     */
    private Map<String, StationData> createStationsData(final KeyValueFileParser<ParameterKey> parser,
                                                        final OneAxisEllipsoid body)
        throws OrekitException, NoSuchElementException {

        final Map<String, StationData> stations       = new HashMap<String, StationData>();

        final String[]  stationNames                  = parser.getStringArray(ParameterKey.GROUND_STATION_NAME);
        final double[]  stationLatitudes              = parser.getAngleArray(ParameterKey.GROUND_STATION_LATITUDE);
        final double[]  stationLongitudes             = parser.getAngleArray(ParameterKey.GROUND_STATION_LONGITUDE);
        final double[]  stationAltitudes              = parser.getDoubleArray(ParameterKey.GROUND_STATION_ALTITUDE);
        final boolean[] stationPositionEstimated      = parser.getBooleanArray(ParameterKey.GROUND_STATION_POSITION_ESTIMATED);
        final double[]  stationRangeSigma             = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_SIGMA);
        final double[]  stationRangeBias              = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS);
        final boolean[] stationRangeBiasEstimated     = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_BIAS_ESTIMATED);
        final double[]  stationRangeRateSigma         = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_SIGMA);
        final double[]  stationRangeRateBias          = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS);
        final boolean[] stationRangeRateBiasEstimated = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_ESTIMATED);
        final double[]  stationAzimuthSigma           = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_SIGMA);
        final double[]  stationAzimuthBias            = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS);
        final double[]  stationElevationSigma         = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_SIGMA);
        final double[]  stationElevationBias          = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS);
        final boolean[] stationAzElBiasesEstimated    = parser.getBooleanArray(ParameterKey.GROUND_STATION_AZ_EL_BIASES_ESTIMATED);
        final boolean[] stationElevationRefraction    = parser.getBooleanArray(ParameterKey.GROUND_STATION_ELEVATION_REFRACTION_CORRECTION);
        final boolean[] stationRangeTropospheric      = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_TROPOSPHERIC_CORRECTION);
        //final boolean[] stationIonosphericCorrection    = parser.getBooleanArray(ParameterKey.GROUND_STATION_IONOSPHERIC_CORRECTION);

        for (int i = 0; i < stationNames.length; ++i) {

            // the station itself
            final GeodeticPoint position = new GeodeticPoint(stationLatitudes[i],
                                                             stationLongitudes[i],
                                                             stationAltitudes[i]);
            final TopocentricFrame topo = new TopocentricFrame(body, position, stationNames[i]);
            final GroundStation station = new GroundStation(topo);
            station.getPositionOffsetDriver().setEstimated(stationPositionEstimated[i]);

            // range
            final double rangeSigma = stationRangeSigma[i];
            final Bias<Range> rangeBias;
            if (FastMath.abs(stationRangeBias[i])   >= Precision.SAFE_MIN || stationRangeBiasEstimated[i]) {
                 rangeBias = new Bias<Range>(stationNames[i] + "/range bias",
                                             stationRangeBias[i]);
                 rangeBias.getDriver().setEstimated(stationRangeBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeBias = null;
            }

            // range rate
            final double rangeRateSigma = stationRangeRateSigma[i];
            final Bias<RangeRate> rangeRateBias;
            if (FastMath.abs(stationRangeRateBias[i])   >= Precision.SAFE_MIN || stationRangeRateBiasEstimated[i]) {
                rangeRateBias = new Bias<RangeRate>(stationNames[i] + "/range rate bias",
                                                    stationRangeRateBias[i]);
                rangeRateBias.getDriver().setEstimated(stationRangeRateBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeRateBias = null;
            }

            // angular biases
            final double[] azELSigma = new double[] {
                stationAzimuthSigma[i], stationElevationSigma[i]
            };
            final Bias<Angular> azELBias;
            if (FastMath.abs(stationAzimuthBias[i])   >= Precision.SAFE_MIN ||
                FastMath.abs(stationElevationBias[i]) >= Precision.SAFE_MIN ||
                stationAzElBiasesEstimated[i]) {
                azELBias = new Bias<Angular>(stationNames[i] + "/az-el bias",
                                             stationAzimuthBias[i], stationElevationBias[i]);
                azELBias.getDriver().setEstimated(stationAzElBiasesEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                azELBias = null;
            }

            //Refraction correction
            final AngularRadioRefractionModifier refractionCorrection;
            if (stationElevationRefraction[i]) {
                final double                     altitude        = station.getBaseFrame().getPoint().getAltitude();
                final AtmosphericRefractionModel refractionModel = new EarthITU453AtmosphereRefraction(altitude);
                refractionCorrection = new AngularRadioRefractionModifier(refractionModel);
            } else {
                refractionCorrection = null;
            }


            //Tropospheric correction
            final RangeTroposphericDelayModifier rangeTroposphericCorrection;
            if (stationRangeTropospheric[i]) {

                final SaastamoinenModel troposphericModel = SaastamoinenModel.getStandardModel();

                rangeTroposphericCorrection = new  RangeTroposphericDelayModifier(troposphericModel);
            } else {
                rangeTroposphericCorrection = null;
            }


        stations.put(stationNames[i], new StationData(station,
                                                      rangeSigma,     rangeBias,
                                                      rangeRateSigma, rangeRateBias,
                                                      azELSigma,      azELBias,
                                                      refractionCorrection, rangeTroposphericCorrection));
        }
        return stations;

    }

    /** Set up weights.
     * @param parser input file parser
     * @return base weights
     * @throws NoSuchElementException if input parameters are missing
     */
    private Weights createWeights(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        return new Weights(parser.getDouble(ParameterKey.RANGE_MEASUREMENTS_BASE_WEIGHT),
                           parser.getDouble(ParameterKey.RANGE_RATE_MEASUREMENTS_BASE_WEIGHT),
                           new double[] {
                               parser.getAngle(ParameterKey.AZIMUTH_MEASUREMENTS_BASE_WEIGHT),
                               parser.getAngle(ParameterKey.ELEVATION_MEASUREMENTS_BASE_WEIGHT)
                           },
                           parser.getDouble(ParameterKey.PV_MEASUREMENTS_BASE_WEIGHT));
    }

    /** Set up PV data.
     * @param parser input file parser
     * @return PV data
     * @throws NoSuchElementException if input parameters are missing
     */
    private PVData createPVData(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        return new PVData(parser.getDouble(ParameterKey.PV_MEASUREMENTS_POSITION_SIGMA),
                          parser.getDouble(ParameterKey.PV_MEASUREMENTS_VELOCITY_SIGMA));
    }

    /** Set up estimator.
     * @param parser input file parser
     * @param propagatorBuilder propagator builder
     * @return estimator
     * @throws NoSuchElementException if input parameters are missing
     * @throws OrekitException if some propagator parameters cannot be retrieved
     */
    private BatchLSEstimator createEstimator(final KeyValueFileParser<ParameterKey> parser,
                                             final NumericalPropagatorBuilder propagatorBuilder)
        throws NoSuchElementException, OrekitException {
        final double relativeConvergence;
        if (! parser.containsKey(ParameterKey.ESTIMATOR_RMS_RELATIVE_CONVERGENCE_THRESHOLD)) {
            relativeConvergence = 1.0e-14;
        } else {
            relativeConvergence = parser.getDouble(ParameterKey.ESTIMATOR_RMS_RELATIVE_CONVERGENCE_THRESHOLD);
        }
        final double absoluteConvergence;
        if (! parser.containsKey(ParameterKey.ESTIMATOR_RMS_ABSOLUTE_CONVERGENCE_THRESHOLD)) {
            absoluteConvergence = 1.0e-12;
        } else {
            absoluteConvergence = parser.getDouble(ParameterKey.ESTIMATOR_RMS_ABSOLUTE_CONVERGENCE_THRESHOLD);
        }
        final int maxIterations;
        if (! parser.containsKey(ParameterKey.ESTIMATOR_MAX_ITERATIONS)) {
            maxIterations = 10;
        } else {
            maxIterations = parser.getInt(ParameterKey.ESTIMATOR_MAX_ITERATIONS);
        }
        final int maxEvaluations;
        if (! parser.containsKey(ParameterKey.ESTIMATOR_MAX_EVALUATIONS)) {
            maxEvaluations = 20;
        } else {
            maxEvaluations = parser.getInt(ParameterKey.ESTIMATOR_MAX_EVALUATIONS);
        }

        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        estimator.setConvergenceThreshold(relativeConvergence, absoluteConvergence);
        estimator.setMaxIterations(maxIterations);
        estimator.setMaxEvaluations(maxEvaluations);

        return estimator;

    }

    /** Read a measurements file.
     * @param file measurements file
     * @param stations name to stations data map
     * @param pvData PV measurements data
     * @param satRangeBias range bias due to transponder delay
     * @param weights base weights for measurements
     * @return measurements list
     */
    private List<Measurement<?>> readMeasurements(final File file,
                                                  final Map<String, StationData> stations,
                                                  final PVData pvData,
                                                  final Bias<Range> satRangeBias,
                                                  final Weights weights)
        throws UnsupportedEncodingException, IOException, OrekitException {

        final List<Measurement<?>> measurements = new ArrayList<Measurement<?>>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            int lineNumber = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    String[] fields = line.split("\\s+");
                    if (fields.length < 2) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, file.getName(), line);
                    }
                    try {
                        final MeasurementsParser parser  = MeasurementsParser.valueOf(fields[1]);
                        final Measurement<?> measurement = parser.parseFields(fields, stations, pvData,
                                                                              satRangeBias, weights,
                                                                              line, lineNumber, file.getName());
                        double sum = 0;
                        for (double w : measurement.getBaseWeight()) {
                            sum += FastMath.abs(w);
                        }
                        if (sum > Precision.SAFE_MIN) {
                            // we only consider measurements with non-zero weight
                            measurements.add(measurement);
                        }
                    } catch (IllegalArgumentException iae) {
                        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                  "unknown measurement type " + fields[1] +
                                                  " at line " + lineNumber +
                                                  " in file " + file.getName());
                    }
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        if (measurements.isEmpty()) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      "not measurements read from file " + file.getAbsolutePath());
        }

        return measurements;

    }

    /** Container for stations-related data. */
    private static class StationData {

        /** Ground station. */
        private final GroundStation station;

        /** Range sigma. */
        private final double rangeSigma;

        /** Range bias (may be if bias is fixed to zero). */
        private final Bias<Range> rangeBias;

        /** Range rate sigma. */
        private final double rangeRateSigma;

        /** Range rate bias (may be null if bias is fixed to zero). */
        private final Bias<RangeRate> rangeRateBias;

        /** Azimuth-elevation sigma. */
        private final double[] azElSigma;

        /** Azimuth-elevation bias (may be null if bias is fixed to zero). */
        private final Bias<Angular> azELBias;

        /** Elevation refraction correction (may be null). */
        private final AngularRadioRefractionModifier refractionCorrection;

        /** Tropospheric correction (may be null). */
        private final RangeTroposphericDelayModifier rangeTroposphericCorrection;

        /** Simple constructor.
         * @param station ground station
         * @param rangeSigma range sigma
         * @param rangeBias range bias (may be null if bias is fixed to zero)
         * @param rangeRateSigma range rate sigma
         * @param rangeRateBias range rate bias (may be null if bias is fixed to zero)
         * @param azElSigma azimuth-elevation sigma
         * @param azELBias azimuth-elevation bias (may be null if bias is fixed to zero)
         * @param refractionCorrection refraction correction for elevation (may be null)
         * @param rangeTroposphericCorrection tropospheric correction  for the range (may be null)
         */
        public StationData(final GroundStation station,
                           final double rangeSigma, final Bias<Range> rangeBias,
                           final double rangeRateSigma, final Bias<RangeRate> rangeRateBias,
                           final double[] azElSigma, final Bias<Angular> azELBias,
                           final AngularRadioRefractionModifier refractionCorrection,
                           final RangeTroposphericDelayModifier rangeTroposphericCorrection) {
            this.station                     = station;
            this.rangeSigma                  = rangeSigma;
            this.rangeBias                   = rangeBias;
            this.rangeRateSigma              = rangeRateSigma;
            this.rangeRateBias               = rangeRateBias;
            this.azElSigma                   = azElSigma.clone();
            this.azELBias                    = azELBias;
            this.refractionCorrection        = refractionCorrection;
            this.rangeTroposphericCorrection = rangeTroposphericCorrection;
        }

    }

    /** Container for base weights. */
    private static class Weights {

        /** Base weight for range measurements. */
        private final double rangeBaseWeight;

        /** Base weight for range rate measurements. */
        private final double rangeRateBaseWeight;

        /** Base weight for azimuth-elevation measurements. */
        private final double[] azElBaseWeight;

        /** Base weight for PV measurements. */
        private final double pvBaseWeight;

        /** Simple constructor.
         * @param rangeBaseWeight base weight for range measurements
         * @param rangeRateBaseWeight base weight for range rate measurements
         * @param azElBaseWeight base weight for azimuth-elevation measurements
         * @param pvBaseWeight base weight for PV measurements
         */
        public Weights(final double rangeBaseWeight,
                       final double rangeRateBaseWeight,
                       final double[] azElBaseWeight,
                       final double pvBaseWeight) {
            this.rangeBaseWeight     = rangeBaseWeight;
            this.rangeRateBaseWeight = rangeRateBaseWeight;
            this.azElBaseWeight      = azElBaseWeight.clone();
            this.pvBaseWeight        = pvBaseWeight;
        }

    }

    /** Container for Position-velocity data. */
    private static class PVData {

        /** Position sigma. */
        private final double positionSigma;

        /** Velocity sigma. */
        private final double velocitySigma;

        /** Simple constructor.
         * @param positionSigma position sigma
         * @param velocitySigma velocity sigma
         */
        public PVData(final double positionSigma, final double velocitySigma) {
            this.positionSigma = positionSigma;
            this.velocitySigma = velocitySigma;
        }

    }

    /** Measurements types. */
    private static enum MeasurementsParser {

        /** Parser for range measurements. */
        RANGE() {
            /** {@inheritDoc} */
            @Override
            public Range parseFields(final String[] fields,
                                     final Map<String, StationData> stations,
                                     final PVData pvData,
                                     final Bias<Range> satRangeBias,
                                     final Weights weights,
                                     final String line,
                                     final int lineNumber,
                                     final String fileName)
                throws OrekitException {
                checkFields(4, fields, line, lineNumber, fileName);
                final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
                final Range range = new Range(stationData.station,
                                              getDate(fields[0], line, lineNumber, fileName),
                                              Double.parseDouble(fields[3]) * 1000.0,
                                              stationData.rangeSigma,
                                              weights.rangeBaseWeight);
                if (stationData.rangeBias != null) {
                    range.addModifier(stationData.rangeBias);
                }
                if (satRangeBias != null) {
                    range.addModifier(satRangeBias);
                }
                if (stationData.rangeTroposphericCorrection != null) {
                    range.addModifier(stationData.rangeTroposphericCorrection);
                }
                return range;
            }
        },

        /** Parser for range rate measurements. */
        RANGE_RATE() {
            /** {@inheritDoc} */
            @Override
            public RangeRate parseFields(final String[] fields,
                                         final Map<String, StationData> stations,
                                         final PVData pvData,
                                         final Bias<Range> satRangeBias,
                                         final Weights weights,
                                         final String line,
                                         final int lineNumber,
                                         final String fileName)
                throws OrekitException {
                checkFields(4, fields, line, lineNumber, fileName);
                final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
                final RangeRate rangeRate = new RangeRate(stationData.station,
                                                          getDate(fields[0], line, lineNumber, fileName),
                                                          Double.parseDouble(fields[3]) * 1000.0,
                                                          stationData.rangeRateSigma,
                                                          weights.rangeRateBaseWeight,
                                                          true);
                if (stationData.rangeRateBias != null) {
                    rangeRate.addModifier(stationData.rangeRateBias);
                }
                return rangeRate;
            }
        },

        /** Parser for azimuth-elevation measurements. */
        AZ_EL() {
            /** {@inheritDoc} */
            @Override
            public Angular parseFields(final String[] fields,
                                       final Map<String, StationData> stations,
                                       final PVData pvData,
                                       final Bias<Range> satRangeBias,
                                       final Weights weights,
                                       final String line,
                                       final int lineNumber,
                                       final String fileName)
                throws OrekitException {
                checkFields(5, fields, line, lineNumber, fileName);
                final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
                final Angular azEl = new Angular(stationData.station,
                                                 getDate(fields[0], line, lineNumber, fileName),
                                                 new double[] {
                                                     FastMath.toRadians(Double.parseDouble(fields[3])),
                                                     FastMath.toRadians(Double.parseDouble(fields[4]))
                                                 },
                                                 stationData.azElSigma,
                                                 weights.azElBaseWeight);
                if (stationData.refractionCorrection != null) {
                    azEl.addModifier(stationData.refractionCorrection);
                }
                if (stationData.azELBias != null) {
                    azEl.addModifier(stationData.azELBias);
                }
                return azEl;
            }
        },

        /** Parser for PV measurements. */
        PV() {
            /** {@inheritDoc} */
            @Override
            public org.orekit.estimation.measurements.PV parseFields(final String[] fields,
                                                                     final Map<String, StationData> stations,
                                                                     final PVData pvData,
                                                                     final Bias<Range> satRangeBias,
                                                                     final Weights weights,
                                                                     final String line,
                                                                     final int lineNumber,
                                                                     final String fileName)
                throws OrekitException {
                // field 2, which corresponds to stations in other measurements, is ignored
                // this allows the measurements files to be columns aligned
                // by inserting something like "----" instead of a station name
                checkFields(9, fields, line, lineNumber, fileName);
                return new org.orekit.estimation.measurements.PV(getDate(fields[0], line, lineNumber, fileName),
                                                                 new Vector3D(Double.parseDouble(fields[3]) * 1000.0,
                                                                              Double.parseDouble(fields[4]) * 1000.0,
                                                                              Double.parseDouble(fields[5]) * 1000.0),
                                                                 new Vector3D(Double.parseDouble(fields[6]) * 1000.0,
                                                                              Double.parseDouble(fields[7]) * 1000.0,
                                                                              Double.parseDouble(fields[8]) * 1000.0),
                                                                 pvData.positionSigma,
                                                                 pvData.velocitySigma,
                                                                 weights.pvBaseWeight);
            }
        };

        /** Parse the fields of a measurements line.
         * @param fields measurements line fields
         * @param stations name to stations data map
         * @param pvData PV measurements data
         * @param satRangeBias range bias due to transponder delay
         * @param weight base weights for measurements
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         * @return parsed measurement
         * @exception OrekitException if the fields do not represent a valid measurements line
         */
        public abstract Measurement<?> parseFields(String[] fields,
                                                   Map<String, StationData> stations,
                                                   PVData pvData,
                                                   Bias<Range> satRangeBias, Weights weight,
                                                   String line, int lineNumber, String fileName)
            throws OrekitException;

        /** Check the number of fields.
         * @param expected expected number of fields
         * @param fields measurements line fields
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         * @exception OrekitException if the number of fields does not match the expected number
         */
        protected void checkFields(final int expected, final String[] fields,
                                   final String line, final int lineNumber, final String fileName)
            throws OrekitException {
            if (fields.length != expected) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, fileName, line);
            }
        }

        /** Get the date for the line.
         * @param date date field
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         * @return parsed measurement
         * @exception OrekitException if the date cannot be parsed
         */
        protected AbsoluteDate getDate(final String date,
                                       final String line, final int lineNumber, final String fileName)
            throws OrekitException {
            try {
                return new AbsoluteDate(date, TimeScalesFactory.getUTC());
            } catch (OrekitException oe) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          "wrong date " + date +
                                          " at line " + lineNumber +
                                          " in file " + fileName +
                                          "\n" + line);
            }
        }

        /** Get the station data for the line.
         * @param stationName name of the station
         * @param stations name to stations data map
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         * @return parsed measurement
         * @exception OrekitException if the station is not known
         */
        protected StationData getStationData(final String stationName,
                                             final Map<String, StationData> stations,
                                             final String line, final int lineNumber, final String fileName)
            throws OrekitException {
            final StationData stationData = stations.get(stationName);
            if (stationData == null) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          "unknown station " + stationName +
                                          " at line " + lineNumber +
                                          " in file " + fileName +
                                          "\n" + line);
            }
            return stationData;
        }

    }


    /** Local class for measurement-specific log.
     * @param T type of mesurement
     */
    private static abstract class MeasurementLog<T extends Measurement<T>> {

        /** Residuals. */
        private final SortedSet<Evaluation<T>> evaluations;

        /** Measurements name. */
        private final String name;


        /** Simple constructor.
         * @param name measurement name
         * @exception IOException if output file cannot be created
         */
        MeasurementLog(final String name) throws IOException {
            this.evaluations = new TreeSet<Evaluation<T>>(new ChronologicalComparator());
            this.name        = name;
        }

        /** Get the measurement name.
         * @return measurement name
         */
        public String getName() {
            return name;
        }

        /** Compute residual value.
         * @param evaluation evaluation to consider
         */
        abstract double residual(final Evaluation<T> evaluation);

        /** Add an evaluation.
         * @param evaluation evaluation to add
         */
        void add(final Evaluation<T> evaluation) {
            evaluations.add(evaluation);
        }

        /**Create a  statistics summary
         */
        public SummaryStatistics createStatisticsSummary() {
            if (!evaluations.isEmpty()) {
                // compute statistics
                final SummaryStatistics stats = SummaryStatistics.create();
                for (final Evaluation<T> evaluation : evaluations) {
                    stats.addValue(residual(evaluation));
                }
               return stats;

            }
            return null;
        }
    }

    /** Logger for range measurements. */
    class RangeLog extends MeasurementLog<Range> {

        /** Simple constructor.
         * @exception IOException if output file cannot be created
         */
        RangeLog() throws IOException {
            super("range");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<Range> evaluation) {
            return evaluation.getValue()[0] - evaluation.getMeasurement().getObservedValue()[0];
        }

    }

    /** Logger for range rate measurements. */
    class RangeRateLog extends MeasurementLog<RangeRate> {

        /** Simple constructor.
         * @exception IOException if output file cannot be created
         */
        RangeRateLog() throws IOException {
            super("range-rate");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<RangeRate> evaluation) {
            return evaluation.getValue()[0] - evaluation.getMeasurement().getObservedValue()[0];
        }

    }

    /** Logger for azimuth measurements. */
    class AzimuthLog extends MeasurementLog<Angular> {

        /** Simple constructor.
         * @exception IOException if output file cannot be created
         */
        AzimuthLog() throws IOException {
            super("azimuth");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<Angular> evaluation) {
            return FastMath.toDegrees(evaluation.getValue()[0] - evaluation.getMeasurement().getObservedValue()[0]);
        }

    }

    /** Logger for elevation measurements. */
    class ElevationLog extends MeasurementLog<Angular> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        ElevationLog() throws IOException {
            super("elevation");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<Angular> evaluation) {
            return FastMath.toDegrees(evaluation.getValue()[1] - evaluation.getMeasurement().getObservedValue()[1]);
        }

    }

    /** Logger for position measurements. */
    class PositionLog extends MeasurementLog<PV> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        PositionLog() throws IOException  {
            super("position");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<PV> evaluation) {
            final double[] theoretical = evaluation.getValue();
            final double[] observed    = evaluation.getMeasurement().getObservedValue();
            return Vector3D.distance(new Vector3D(theoretical[0], theoretical[1], theoretical[2]),
                                     new Vector3D(observed[0],    observed[1],    observed[2]));
        }

    }

    /** Logger for velocity measurements. */
    class VelocityLog extends MeasurementLog<PV> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        VelocityLog() throws IOException {
            super("velocity");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final Evaluation<PV> evaluation) {
            final double[] theoretical = evaluation.getValue();
            final double[] observed    = evaluation.getMeasurement().getObservedValue();
            return Vector3D.distance(new Vector3D(theoretical[3], theoretical[4], theoretical[5]),
                                     new Vector3D(observed[3],    observed[4],    observed[5]));
        }

    }

    /** Input parameter keys. */
    private static enum ParameterKey {
        ORBIT_DATE,
        ORBIT_CIRCULAR_A,
        ORBIT_CIRCULAR_EX,
        ORBIT_CIRCULAR_EY,
        ORBIT_CIRCULAR_I,
        ORBIT_CIRCULAR_RAAN,
        ORBIT_CIRCULAR_ALPHA,
        ORBIT_EQUINOCTIAL_A,
        ORBIT_EQUINOCTIAL_EX,
        ORBIT_EQUINOCTIAL_EY,
        ORBIT_EQUINOCTIAL_HX,
        ORBIT_EQUINOCTIAL_HY,
        ORBIT_EQUINOCTIAL_LAMBDA,
        ORBIT_KEPLERIAN_A,
        ORBIT_KEPLERIAN_E,
        ORBIT_KEPLERIAN_I,
        ORBIT_KEPLERIAN_PA,
        ORBIT_KEPLERIAN_RAAN,
        ORBIT_KEPLERIAN_ANOMALY,
        ORBIT_ANGLE_TYPE,
        ORBIT_TLE_LINE_1,
        ORBIT_TLE_LINE_2,
        ORBIT_CARTESIAN_PX,
        ORBIT_CARTESIAN_PY,
        ORBIT_CARTESIAN_PZ,
        ORBIT_CARTESIAN_VX,
        ORBIT_CARTESIAN_VY,
        ORBIT_CARTESIAN_VZ,
        MASS,
        IERS_CONVENTIONS,
        INERTIAL_FRAME,
        PROPAGATOR_MIN_STEP,
        PROPAGATOR_MAX_STEP,
        PROPAGATOR_POSITION_ERROR,
        BODY_FRAME,
        BODY_EQUATORIAL_RADIUS,
        BODY_INVERSE_FLATTENING,
        CENTRAL_BODY_DEGREE,
        CENTRAL_BODY_ORDER,
        OCEAN_TIDES_DEGREE,
        OCEAN_TIDES_ORDER,
        SOLID_TIDES_SUN,
        SOLID_TIDES_MOON,
        THIRD_BODY_SUN,
        THIRD_BODY_MOON,
        DRAG,
        DRAG_CD,
        DRAG_CD_ESTIMATED,
        DRAG_AREA,
        SOLAR_RADIATION_PRESSURE,
        SOLAR_RADIATION_PRESSURE_CR,
        SOLAR_RADIATION_PRESSURE_CR_ESTIMATED,
        SOLAR_RADIATION_PRESSURE_AREA,
        GENERAL_RELATIVITY,
        TRANSPONDER_DELAY_BIAS,
        TRANSPONDER_DELAY_BIAS_ESTIMATED,
        GROUND_STATION_NAME,
        GROUND_STATION_LATITUDE,
        GROUND_STATION_LONGITUDE,
        GROUND_STATION_ALTITUDE,
        GROUND_STATION_POSITION_ESTIMATED,
        GROUND_STATION_RANGE_SIGMA,
        GROUND_STATION_RANGE_BIAS,
        GROUND_STATION_RANGE_BIAS_ESTIMATED,
        GROUND_STATION_RANGE_RATE_SIGMA,
        GROUND_STATION_RANGE_RATE_BIAS,
        GROUND_STATION_RANGE_RATE_BIAS_ESTIMATED,
        GROUND_STATION_AZIMUTH_SIGMA,
        GROUND_STATION_AZIMUTH_BIAS,
        GROUND_STATION_ELEVATION_SIGMA,
        GROUND_STATION_ELEVATION_BIAS,
        GROUND_STATION_AZ_EL_BIASES_ESTIMATED,
        GROUND_STATION_ELEVATION_REFRACTION_CORRECTION,
        GROUND_STATION_RANGE_TROPOSPHERIC_CORRECTION,
        GROUND_STATION_IONOSPHERIC_CORRECTION,
        RANGE_MEASUREMENTS_BASE_WEIGHT,
        RANGE_RATE_MEASUREMENTS_BASE_WEIGHT,
        AZIMUTH_MEASUREMENTS_BASE_WEIGHT,
        ELEVATION_MEASUREMENTS_BASE_WEIGHT,
        PV_MEASUREMENTS_BASE_WEIGHT,
        PV_MEASUREMENTS_POSITION_SIGMA,
        PV_MEASUREMENTS_VELOCITY_SIGMA,
        OUTLIER_REJECTION_MULTIPLIER,
        OUTLIER_REJECTION_STARTING_ITERATION,
        MEASUREMENTS_FILES,
        OUTPUT_BASE_NAME,
        ESTIMATOR_RMS_ABSOLUTE_CONVERGENCE_THRESHOLD,
        ESTIMATOR_RMS_RELATIVE_CONVERGENCE_THRESHOLD,
        ESTIMATOR_MAX_ITERATIONS,
        ESTIMATOR_MAX_EVALUATIONS;
    }

}
