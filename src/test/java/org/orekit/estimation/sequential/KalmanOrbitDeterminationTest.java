/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.sequential;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.YawCompensation;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.estimation.measurements.modifiers.OutlierFilter;
import org.orekit.estimation.measurements.modifiers.RangeIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.forces.PolynomialParametricAcceleration;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
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
import org.orekit.frames.EOPHistory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.RinexLoader;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.models.earth.displacement.OceanLoading;
import org.orekit.models.earth.displacement.OceanLoadingCoefficientsBLQFactory;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.models.earth.ionosphere.KlobucharIonoCoefficientsLoader;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.MappingFunction;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.models.earth.weather.GlobalPressureTemperatureModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class KalmanOrbitDeterminationTest {

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2()
                    throws URISyntaxException, IllegalArgumentException, IOException,
                    OrekitException, ParseException {

        // Print results on console
        final boolean print = false;
        
        // input in tutorial resources directory/output
        final String inputPath = KalmanOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/od_test_Lageos2.in").toURI().getPath();
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
        ResultKalman kalmanLageos2 = run(input, orbitType, print,
                                         cartesianOrbitalP, cartesianOrbitalQ,
                                         null, null,
                                         measurementP, measurementQ);

        // Definition of the accuracy for the test
        final double distanceAccuracy = 0.86;
        final double velocityAccuracy = 4.12e-3;

        // Tests
        // Note: The reference initial orbit is the same as in the batch LS tests
        // -----
        
        // Number of measurements processed
        final int numberOfMeas  = 258;
        Assert.assertEquals(numberOfMeas, kalmanLageos2.getNumberOfMeasurements());

        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanLageos2.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel0 = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        
        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, null,
                                            kalmanLageos2.getEstimatedPV().getDate());
        final Vector3D refPos = refOrbit.getPVCoordinates().getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();
        
        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        Assert.assertEquals(0.0, dP, distanceAccuracy);
        Assert.assertEquals(0.0, dV, velocityAccuracy);
        
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
        list.addAll(kalmanLageos2.measurementsParameters.getDrivers());
        sortParametersChanges(list);
        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.298867,  -0.137456,  0.013315 };
        final double rangeBias = 0.002390;
        Assert.assertEquals(stationOffSet[0], list.get(0).getValue(), distanceAccuracy);
        Assert.assertEquals(stationOffSet[1], list.get(1).getValue(), distanceAccuracy);
        Assert.assertEquals(stationOffSet[2], list.get(2).getValue(), distanceAccuracy);
        Assert.assertEquals(rangeBias,        list.get(3).getValue(), distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        // Batch LS values
        //final double[] RefStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] RefStatRange = { -23.561314, 20.436464, 0.964164, 5.687187 };
        Assert.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], kalmanLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], kalmanLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], kalmanLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    @Test
    // Orbit determination for range, azimuth elevation measurements
    public void testW3B()
                    throws URISyntaxException, IllegalArgumentException, IOException,
                    OrekitException, ParseException {

        // Print results on console
        final boolean print = false;
        
        // Input in tutorial resources directory/output
        final String inputPath = KalmanOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/W3B/od_test_W3.in").toURI().getPath();
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
        ResultKalman kalmanW3B = run(input, orbitType, print,
                                     cartesianOrbitalP, cartesianOrbitalQ,
                                     propagationP, propagationQ,
                                     measurementP, measurementQ);

        // Tests
        // -----
        
        // Definition of the accuracy for the test
        final double distanceAccuracy = 0.1;
        final double angleAccuracy    = 1e-5; // degrees
        
        // Number of measurements processed
        final int numberOfMeas  = 521;
        Assert.assertEquals(numberOfMeas, kalmanW3B.getNumberOfMeasurements());


        // Test on propagator parameters
        // -----------------------------
        
        // Batch LS result
        // final double dragCoef  = -0.2154;
        final double dragCoef  = 0.1931;
        Assert.assertEquals(dragCoef, kalmanW3B.propagatorParameters.getDrivers().get(0).getValue(), 1e-3);
        final Vector3D leakAcceleration0 =
                        new Vector3D(kalmanW3B.propagatorParameters.getDrivers().get(1).getValue(),
                                     kalmanW3B.propagatorParameters.getDrivers().get(3).getValue(),
                                     kalmanW3B.propagatorParameters.getDrivers().get(5).getValue());
        // Batch LS results
        //Assert.assertEquals(8.002e-6, leakAcceleration0.getNorm(), 1.0e-8);
        Assert.assertEquals(5.994e-6, leakAcceleration0.getNorm(), 1.0e-8);
        final Vector3D leakAcceleration1 =
                        new Vector3D(kalmanW3B.propagatorParameters.getDrivers().get(2).getValue(),
                                     kalmanW3B.propagatorParameters.getDrivers().get(4).getValue(),
                                     kalmanW3B.propagatorParameters.getDrivers().get(6).getValue());
        // Batch LS results
        //Assert.assertEquals(3.058e-10, leakAcceleration1.getNorm(), 1.0e-12);
        Assert.assertEquals(1.831e-10, leakAcceleration1.getNorm(), 1.0e-12);

        // Test on measurements parameters
        // -------------------------------
        
        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>();
        list.addAll(kalmanW3B.measurementsParameters.getDrivers());
        sortParametersChanges(list);

        // Station CastleRock
        // Batch LS results
//        final double[] CastleAzElBias  = { 0.062701342, -0.003613508 };
//        final double   CastleRangeBias = 11274.4677;
        final double[] CastleAzElBias  = { 0.062635, -0.003672};
        final double   CastleRangeBias = 11289.3678;
        Assert.assertEquals(CastleAzElBias[0], FastMath.toDegrees(list.get(0).getValue()), angleAccuracy);
        Assert.assertEquals(CastleAzElBias[1], FastMath.toDegrees(list.get(1).getValue()), angleAccuracy);
        Assert.assertEquals(CastleRangeBias,   list.get(2).getValue(),                     distanceAccuracy);

        // Station Fucino
        // Batch LS results
//        final double[] FucAzElBias  = { -0.053526137, 0.075483886 };
//        final double   FucRangeBias = 13467.8256;
        final double[] FucAzElBias  = { -0.053298, 0.075589 };
        final double   FucRangeBias = 13482.0715;
        Assert.assertEquals(FucAzElBias[0], FastMath.toDegrees(list.get(3).getValue()), angleAccuracy);
        Assert.assertEquals(FucAzElBias[1], FastMath.toDegrees(list.get(4).getValue()), angleAccuracy);
        Assert.assertEquals(FucRangeBias,   list.get(5).getValue(),                     distanceAccuracy);

        // Station Kumsan
        // Batch LS results
//        final double[] KumAzElBias  = { -0.023574208, -0.054520756 };
//        final double   KumRangeBias = 13512.57594;
        final double[] KumAzElBias  = { -0.022805, -0.055057 };
        final double   KumRangeBias = 13502.7459;
        Assert.assertEquals(KumAzElBias[0], FastMath.toDegrees(list.get(6).getValue()), angleAccuracy);
        Assert.assertEquals(KumAzElBias[1], FastMath.toDegrees(list.get(7).getValue()), angleAccuracy);
        Assert.assertEquals(KumRangeBias,   list.get(8).getValue(),                     distanceAccuracy);

        // Station Pretoria
        // Batch LS results
//        final double[] PreAzElBias = { 0.030201539, 0.009747877 };
//        final double PreRangeBias = 13594.11889;
        final double[] PreAzElBias = { 0.030353, 0.009658 };
        final double PreRangeBias = 13609.2516;
        Assert.assertEquals(PreAzElBias[0], FastMath.toDegrees(list.get( 9).getValue()), angleAccuracy);
        Assert.assertEquals(PreAzElBias[1], FastMath.toDegrees(list.get(10).getValue()), angleAccuracy);
        Assert.assertEquals(PreRangeBias,   list.get(11).getValue(),                     distanceAccuracy);

        // Station Uralla
        // Batch LS results
//        final double[] UraAzElBias = { 0.167814449, -0.12305252 };
//        final double UraRangeBias = 13450.26738;
        final double[] UraAzElBias = { 0.167519, -0.122842 };
        final double UraRangeBias = 13441.7019;
        Assert.assertEquals(UraAzElBias[0], FastMath.toDegrees(list.get(12).getValue()), angleAccuracy);
        Assert.assertEquals(UraAzElBias[1], FastMath.toDegrees(list.get(13).getValue()), angleAccuracy);
        Assert.assertEquals(UraRangeBias,   list.get(14).getValue(),                     distanceAccuracy);

        // Test on statistic for the range residuals
        final long nbRange = 182;
        //statistics for the range residual (min, max, mean, std)
        final double[] RefStatRange = { -12.981, 18.046, -1.133, 5.312 };
        Assert.assertEquals(nbRange, kalmanW3B.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], kalmanW3B.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], kalmanW3B.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], kalmanW3B.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], kalmanW3B.getRangeStat().getStandardDeviation(), distanceAccuracy);

        //test on statistic for the azimuth residuals
        final long nbAzi = 339;
        //statistics for the azimuth residual (min, max, mean, std)
        final double[] RefStatAzi = { -0.041441, 0.023473, -0.004426, 0.009911 };
        Assert.assertEquals(nbAzi, kalmanW3B.getAzimStat().getN());
        Assert.assertEquals(RefStatAzi[0], kalmanW3B.getAzimStat().getMin(),               angleAccuracy);
        Assert.assertEquals(RefStatAzi[1], kalmanW3B.getAzimStat().getMax(),               angleAccuracy);
        Assert.assertEquals(RefStatAzi[2], kalmanW3B.getAzimStat().getMean(),              angleAccuracy);
        Assert.assertEquals(RefStatAzi[3], kalmanW3B.getAzimStat().getStandardDeviation(), angleAccuracy);

        //test on statistic for the elevation residuals
        final long nbEle = 339;
        final double[] RefStatEle = { -0.025399, 0.043345, 0.001011, 0.010636 };
        Assert.assertEquals(nbEle, kalmanW3B.getElevStat().getN());
        Assert.assertEquals(RefStatEle[0], kalmanW3B.getElevStat().getMin(),               angleAccuracy);
        Assert.assertEquals(RefStatEle[1], kalmanW3B.getElevStat().getMax(),               angleAccuracy);
        Assert.assertEquals(RefStatEle[2], kalmanW3B.getElevStat().getMean(),              angleAccuracy);
        Assert.assertEquals(RefStatEle[3], kalmanW3B.getElevStat().getStandardDeviation(), angleAccuracy);

        RealMatrix covariances = kalmanW3B.getCovariances();
        Assert.assertEquals(28, covariances.getRowDimension());
        Assert.assertEquals(28, covariances.getColumnDimension());

        // drag coefficient variance
        Assert.assertEquals(0.016349, covariances.getEntry(6, 6), 1.0e-5);

        // leak-X constant term variance
        Assert.assertEquals(2.047303E-13, covariances.getEntry(7, 7), 1.0e-16);

        // leak-Y constant term variance
        Assert.assertEquals(5.462497E-13, covariances.getEntry(9, 9), 1.0e-15);

        // leak-Z constant term variance
        Assert.assertEquals(1.717781E-11, covariances.getEntry(11, 11), 1.0e-15);
        
        
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
        final ParameterDriversList refPropagationParameters = kalmanW3B.propagatorParameters;
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
        final Vector3D refPos = refOrbit.getPVCoordinates().getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();
        
        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        
        // FIXME: debug - Comparison with batch LS is bad
        final double debugDistanceAccuracy = 234.73;
        final double debugVelocityAccuracy = 0.086;
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), debugDistanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), debugVelocityAccuracy);
        
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

    private class ResultKalman {
        private int numberOfMeasurements;
        private TimeStampedPVCoordinates estimatedPV;
        private StreamingStatistics rangeStat;
        private StreamingStatistics azimStat;
        private StreamingStatistics elevStat;
        private ParameterDriversList propagatorParameters  ;
        private ParameterDriversList measurementsParameters;
        private RealMatrix covariances;
        ResultKalman(ParameterDriversList  propagatorParameters,
                     ParameterDriversList  measurementsParameters,
                     int numberOfMeasurements, TimeStampedPVCoordinates estimatedPV,
                     StreamingStatistics rangeStat, StreamingStatistics rangeRateStat,
                     StreamingStatistics azimStat, StreamingStatistics elevStat,
                     StreamingStatistics posStat, StreamingStatistics velStat,
                     RealMatrix covariances) {

            this.propagatorParameters   = propagatorParameters;
            this.measurementsParameters = measurementsParameters;
            this.numberOfMeasurements   = numberOfMeasurements;
            this.estimatedPV            = estimatedPV;
            this.rangeStat              =  rangeStat;
            this.azimStat               = azimStat;
            this.elevStat               = elevStat;
            this.covariances            = covariances;
        }

        public int getNumberOfMeasurements() {
            return numberOfMeasurements;
        }

        public TimeStampedPVCoordinates getEstimatedPV() {
            return estimatedPV;
        }

        public StreamingStatistics getRangeStat() {
            return rangeStat;
        }

        public StreamingStatistics getAzimStat() {
            return azimStat;
        }

        public StreamingStatistics getElevStat() {
            return elevStat;
        }

        public RealMatrix getCovariances() {
            return covariances;
        }

    }

    /**
     * Function running the Kalman filter estimation.
     * @param input Input configuration file
     * @param orbitType Orbit type to use (calculation and display)
     * @param print Choose whether the results are printed on console or not
     * @param cartesianOrbitalP Orbital part of the initial covariance matrix in Cartesian formalism
     * @param cartesianOrbitalQ Orbital part of the process noise matrix in Cartesian formalism
     * @param propagationP Propagation part of the initial covariance matrix
     * @param propagationQ Propagation part of the process noise matrix
     * @param measurementP Measurement part of the initial covariance matrix
     * @param measurementQ Measurement part of the process noise matrix
     */
    private ResultKalman run(final File input, final OrbitType orbitType, final boolean print,
                             final RealMatrix cartesianOrbitalP, final RealMatrix cartesianOrbitalQ,
                             final RealMatrix propagationP, final RealMatrix propagationQ,
                             final RealMatrix measurementP, final RealMatrix measurementQ)
        throws IOException, IllegalArgumentException, OrekitException, ParseException {

        // Read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        parser.parseInput(input.getAbsolutePath(), new FileInputStream(input));

        // Log files
        final RangeLog     rangeLog     = new RangeLog();
        final RangeRateLog rangeRateLog = new RangeRateLog();
        final AzimuthLog   azimuthLog   = new AzimuthLog();
        final ElevationLog elevationLog = new ElevationLog();
        final PositionLog  positionLog  = new PositionLog();
        final VelocityLog  velocityLog  = new VelocityLog();

        // Gravity field
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-5c.gfc", true));
        final NormalizedSphericalHarmonicsProvider gravityField = createGravityField(parser);


        // Orbit initial guess
        Orbit initialGuess = createOrbit(parser, gravityField.getMu());

        // Convert to desired orbit type
        initialGuess = orbitType.convertType(initialGuess);

        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }

        // Central body
        final OneAxisEllipsoid body = createBody(parser);

        // Propagator builder
        final NumericalPropagatorBuilder propagatorBuilder =
                        createPropagatorBuilder(parser, conventions, gravityField, body, initialGuess);

        final Map<String, StationData>    stations                 = createStationsData(parser, conventions, body);
        final PVData                      pvData                   = createPVData(parser);
        final ObservableSatellite         satellite                = createObservableSatellite(parser);
        final Bias<Range>                 satRangeBias             = createSatRangeBias(parser);
        final OnBoardAntennaRangeModifier satAntennaRangeModifier  = createSatAntennaRangeModifier(parser);
        final Weights                     weights                  = createWeights(parser);
        final OutlierFilter<Range>        rangeOutliersManager     = createRangeOutliersManager(parser);
        final OutlierFilter<RangeRate>    rangeRateOutliersManager = createRangeRateOutliersManager(parser);
        final OutlierFilter<AngularAzEl>  azElOutliersManager      = createAzElOutliersManager(parser);
        final OutlierFilter<PV>           pvOutliersManager        = createPVOutliersManager(parser);

        // measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {
            if (Pattern.matches(RinexLoader.DEFAULT_RINEX_2_SUPPORTED_NAMES, fileName) ||
                Pattern.matches(RinexLoader.DEFAULT_RINEX_3_SUPPORTED_NAMES, fileName)) {
                // the measurements come from a Rinex file
                measurements.addAll(readRinex(new File(input.getParentFile(), fileName),
                                              parser.getString(ParameterKey.SATELLITE_ID_IN_RINEX_FILES),
                                              stations, satellite, satRangeBias, satAntennaRangeModifier, weights,
                                              rangeOutliersManager, rangeRateOutliersManager));
            } else {
                // the measurements come from an Orekit custom file
                measurements.addAll(readMeasurements(new File(input.getParentFile(), fileName),
                                                     stations, pvData, satellite,
                                                     satRangeBias, satAntennaRangeModifier, weights,
                                                     rangeOutliersManager,
                                                     rangeRateOutliersManager,
                                                     azElOutliersManager,
                                                     pvOutliersManager));
            }

        }

        // Building the Kalman filter:
        // - Gather the estimated measurement parameters in a list
        // - Prepare the initial covariance matrix and the process noise matrix
        // - Build the Kalman filter
        // --------------------------------------------------------------------

        // Build the list of estimated measurements
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : measurements) {
            final List<ParameterDriver> drivers = measurement.getParametersDrivers();
            for (ParameterDriver driver : drivers) {
                if (driver.isSelected()) {
                    // Add the driver
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }
        // Sort the list lexicographically
        estimatedMeasurementsParameters.sort();
        
        // Orbital covariance matrix initialization
        // Jacobian of the orbital parameters w/r to Cartesian
        final double[][] dYdC = new double[6][6];
        initialGuess.getJacobianWrtCartesian(propagatorBuilder.getPositionAngle(), dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        RealMatrix orbitalP = Jac.multiply(cartesianOrbitalP.multiply(Jac.transpose()));  

        // Orbital process noise matrix
        RealMatrix orbitalQ = Jac.multiply(cartesianOrbitalQ.multiply(Jac.transpose()));

        
        // Build the full covariance matrix and process noise matrix
        final int nbPropag = (propagationP != null)?propagationP.getRowDimension():0;
        final int nbMeas   = (measurementP != null)?measurementP.getRowDimension():0;
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6 + nbPropag + nbMeas,
                                                                 6 + nbPropag + nbMeas);
        final RealMatrix Q = MatrixUtils.createRealMatrix(6 + nbPropag + nbMeas,
                                                          6 + nbPropag + nbMeas);
        // Orbital part
        initialP.setSubMatrix(orbitalP.getData(), 0, 0);
        Q.setSubMatrix(orbitalQ.getData(), 0, 0);
        
        // Propagation part
        if (propagationP != null) {
            initialP.setSubMatrix(propagationP.getData(), 6, 6);
            Q.setSubMatrix(propagationQ.getData(), 6, 6);
        }
        
        // Measurement part
        if (measurementP != null) {
            initialP.setSubMatrix(measurementP.getData(), 6 + nbPropag, 6 + nbPropag);
            Q.setSubMatrix(measurementQ.getData(), 6 + nbPropag, 6 + nbPropag);
        }

        // Build the Kalman
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(estimatedMeasurementsParameters).
                        build();

        // Add an observer
        kalman.setObserver(new KalmanObserver() {

            /** Date of the first measurement.*/
            private AbsoluteDate t0;

            /** {@inheritDoc} */
            @Override
            @SuppressWarnings("unchecked")
            public void evaluationPerformed(final KalmanEstimation estimation) {

                // Current measurement number, date and status
                final EstimatedMeasurement<?> estimatedMeasurement = estimation.getCorrectedMeasurement();
                final int currentNumber        = estimation.getCurrentMeasurementNumber();
                final AbsoluteDate currentDate = estimatedMeasurement.getDate();
                final EstimatedMeasurement.Status currentStatus = estimatedMeasurement.getStatus();

                // Current estimated measurement
                final ObservedMeasurement<?>  observedMeasurement  = estimatedMeasurement.getObservedMeasurement();
                
                // Measurement type & Station name
                String measType    = "";
                String stationName = "";

                // Register the measurement in the proper measurement logger
                if (observedMeasurement instanceof Range) {

                    // Add the tuple (estimation, prediction) to the log
                    rangeLog.add(currentNumber,
                                 (EstimatedMeasurement<Range>) estimatedMeasurement);

                    // Measurement type & Station name
                    measType    = "RANGE";
                    stationName =  ((EstimatedMeasurement<Range>) estimatedMeasurement).getObservedMeasurement().
                                    getStation().getBaseFrame().getName();
                } else if (observedMeasurement instanceof RangeRate) {
                    rangeRateLog.add(currentNumber,
                                     (EstimatedMeasurement<RangeRate>) estimatedMeasurement);
                    measType    = "RANGE_RATE";
                    stationName =  ((EstimatedMeasurement<RangeRate>) estimatedMeasurement).getObservedMeasurement().
                                    getStation().getBaseFrame().getName();
                } else if (observedMeasurement instanceof AngularAzEl) {
                    azimuthLog.add(currentNumber,
                                   (EstimatedMeasurement<AngularAzEl>) estimatedMeasurement);
                    elevationLog.add(currentNumber,
                                     (EstimatedMeasurement<AngularAzEl>) estimatedMeasurement);
                    measType    = "AZ_EL";
                    stationName =  ((EstimatedMeasurement<AngularAzEl>) estimatedMeasurement).getObservedMeasurement().
                                    getStation().getBaseFrame().getName();
                } else if (observedMeasurement instanceof PV) {
                    positionLog.add(currentNumber,
                                    (EstimatedMeasurement<PV>) estimatedMeasurement);
                    velocityLog.add(currentNumber,
                                    (EstimatedMeasurement<PV>) estimatedMeasurement);
                    measType    = "PV";
                }

                // Print data on terminal
                // ----------------------

                // Header
                if (print) {
                    if (currentNumber == 1) {
                        // Set t0 to first measurement date
                        t0 = currentDate;

                        // Print header
                        final String formatHeader = "%-4s\t%-25s\t%15s\t%-10s\t%-10s\t%-20s\t%20s\t%20s";
                        String header = String.format(Locale.US, formatHeader,
                                                      "Nb", "Epoch", "Dt[s]", "Status", "Type", "Station",
                                                      "DP Corr", "DV Corr");
                        // Orbital drivers
                        for (DelegatingDriver driver : estimation.getEstimatedOrbitalParameters().getDrivers()) {
                            header += String.format(Locale.US, "\t%20s", driver.getName());
                            header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                        }

                        // Propagation drivers
                        for (DelegatingDriver driver : estimation.getEstimatedPropagationParameters().getDrivers()) {
                            header += String.format(Locale.US, "\t%20s", driver.getName());
                            header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                        }

                        // Measurements drivers
                        for (DelegatingDriver driver : estimation.getEstimatedMeasurementsParameters().getDrivers()) {
                            header += String.format(Locale.US, "\t%20s", driver.getName());
                            header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                        }

                        // Print header
                        System.out.println(header);
                    }

                    // Print current measurement info in terminal
                    String line = "";
                    // Line format
                    final String lineFormat = "%4d\t%-25s\t%15.3f\t%-10s\t%-10s\t%-20s\t%20.9e\t%20.9e";

                    // Orbital correction = DP & DV between predicted orbit and estimated orbit
                    final Vector3D predictedP = estimation.getPredictedSpacecraftStates()[0].getPVCoordinates().getPosition();
                    final Vector3D predictedV = estimation.getPredictedSpacecraftStates()[0].getPVCoordinates().getVelocity();
                    final Vector3D estimatedP = estimation.getCorrectedSpacecraftStates()[0].getPVCoordinates().getPosition();
                    final Vector3D estimatedV = estimation.getCorrectedSpacecraftStates()[0].getPVCoordinates().getVelocity();
                    final double DPcorr       = Vector3D.distance(predictedP, estimatedP);
                    final double DVcorr       = Vector3D.distance(predictedV, estimatedV);

                    line = String.format(Locale.US, lineFormat,
                                         currentNumber, currentDate.toString(), 
                                         currentDate.durationFrom(t0), currentStatus.toString(),
                                         measType, stationName,
                                         DPcorr, DVcorr);

                    // Handle parameters printing (value and error) 
                    int jPar = 0;
                    final RealMatrix Pest = estimation.getPhysicalEstimatedCovarianceMatrix();
                    // Orbital drivers
                    for (DelegatingDriver driver : estimation.getEstimatedOrbitalParameters().getDrivers()) {
                        line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                        line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                        jPar++;
                    }
                    // Propagation drivers
                    for (DelegatingDriver driver : estimation.getEstimatedPropagationParameters().getDrivers()) {
                        line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                        line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                        jPar++;
                    }
                    // Measurements drivers
                    for (DelegatingDriver driver : estimatedMeasurementsParameters.getDrivers()) {
                        line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                        line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                        jPar++;
                    }

                    // Print the line
                    System.out.println(line);
                }
            }
        });

        // Process the list measurements 
        final Orbit estimated = kalman.processMeasurements(measurements)[0].getInitialState().getOrbit();

        // Get the last estimated physical covariances
        final RealMatrix covarianceMatrix = kalman.getPhysicalEstimatedCovarianceMatrix();

        // Parameters and measurements.
        final ParameterDriversList propagationParameters   = kalman.getPropagationParametersDrivers(true);
        final ParameterDriversList measurementsParameters = kalman.getEstimatedMeasurementsParameters();

        // Eventually, print parameter changes, statistics and covariances
        if (print) {
            
            // Display parameter change for non orbital drivers
            int length = 0;
            for (final ParameterDriver parameterDriver : propagationParameters.getDrivers()) {
                length = FastMath.max(length, parameterDriver.getName().length());
            }
            for (final ParameterDriver parameterDriver : measurementsParameters.getDrivers()) {
                length = FastMath.max(length, parameterDriver.getName().length());
            }
            if (propagationParameters.getNbParams() > 0) {
                displayParametersChanges(System.out, "Estimated propagator parameters changes: ",
                                         true, length, propagationParameters);
            }
            if (measurementsParameters.getNbParams() > 0) {
                displayParametersChanges(System.out, "Estimated measurements parameters changes: ",
                                         true, length, measurementsParameters);
            }
            
            // Measurements statistics summary
            System.out.println("");
            rangeLog.displaySummary(System.out);
            rangeRateLog.displaySummary(System.out);
            azimuthLog.displaySummary(System.out);
            elevationLog.displaySummary(System.out);
            positionLog.displaySummary(System.out);
            velocityLog.displaySummary(System.out);
            
            // Covariances and sigmas
            displayFinalCovariances(System.out, kalman);
        }

        // Instantiation of the results
        return new ResultKalman(propagationParameters, measurementsParameters,
                                kalman.getCurrentMeasurementNumber(), estimated.getPVCoordinates(),
                                rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                                azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                                positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary(),
                                covarianceMatrix);
    }
    
    /**
     * Use the physical models in the input file
     * Incorporate the initial reference values
     * And run the propagation until the last measurement to get the reference orbit at the same date
     * as the Kalman filter
     * @param input Input configuration file
     * @param orbitType Orbit type to use (calculation and display)
     * @param refPosition Initial reference position
     * @param refVelocity Initial reference velocity
     * @param refPropagationParameters Reference propagation parameters
     * @param kalmanFinalDate The final date of the Kalman filter
     * @return The reference orbit at the same date as the Kalman filter
     * @throws IOException Input file cannot be opened
     * @throws IllegalArgumentException Issue in key/value reading of input file
     * @throws ParseException Parsing of the input file or measurement file failed
     */
    private Orbit runReference(final File input, final OrbitType orbitType,
                               final Vector3D refPosition, final Vector3D refVelocity,
                               final ParameterDriversList refPropagationParameters,
                               final AbsoluteDate kalmanFinalDate)
                    throws IOException, IllegalArgumentException, ParseException {

        // Read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        parser.parseInput(input.getAbsolutePath(), new FileInputStream(input));

        // Gravity field
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-5c.gfc", true));
        final NormalizedSphericalHarmonicsProvider gravityField = createGravityField(parser);


        // Orbit initial guess
        Orbit initialRefOrbit = new CartesianOrbit(new PVCoordinates(refPosition, refVelocity),
                                                   parser.getInertialFrame(ParameterKey.INERTIAL_FRAME),
                                                   parser.getDate(ParameterKey.ORBIT_DATE,
                                                                  TimeScalesFactory.getUTC()),
                                                   gravityField.getMu());

        // Convert to desired orbit type
        initialRefOrbit = orbitType.convertType(initialRefOrbit);

        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }

        // Central body
        final OneAxisEllipsoid body = createBody(parser);

        // Propagator builder
        final NumericalPropagatorBuilder propagatorBuilder =
                        createPropagatorBuilder(parser, conventions, gravityField, body, initialRefOrbit);

        // Force the selected propagation parameters to their reference values
        if (refPropagationParameters != null) {
            for (DelegatingDriver refDriver : refPropagationParameters.getDrivers()) {
                for (DelegatingDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
                    if (driver.getName().equals(refDriver.getName())) {
                        driver.setValue(refDriver.getValue());
                    }
                }
            }
        }

        // Build the reference propagator
        final NumericalPropagator propagator = 
                        propagatorBuilder.buildPropagator(propagatorBuilder.
                                                          getSelectedNormalizedParameters());
        
        // Propagate until last date and return the orbit
        return propagator.propagate(kalmanFinalDate).getOrbit();
    }


    /** Sort parameters changes.
     * @param parameters parameters list
     */
    private void sortParametersChanges(List<? extends ParameterDriver> parameters) {

        // sort the parameters lexicographically
        Collections.sort(parameters, new Comparator<ParameterDriver>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final ParameterDriver pd1, final ParameterDriver pd2) {
                return pd1.getName().compareTo(pd2.getName());
            }

        });
    }

    /** Display parameters changes.
     * @param stream output stream
     * @param header header message
     * @param sort if true, parameters will be sorted lexicographically
     * @param parameters parameters list
     */
    private void displayParametersChanges(final PrintStream out, final String header, final boolean sort,
                                          final int length, final ParameterDriversList parameters) {

        List<ParameterDriver> list = new ArrayList<ParameterDriver>(parameters.getDrivers());
        if (sort) {
            // sort the parameters lexicographically
            Collections.sort(list, new Comparator<ParameterDriver>() {
                /** {@inheritDoc} */
                @Override
                public int compare(final ParameterDriver pd1, final ParameterDriver pd2) {
                    return pd1.getName().compareTo(pd2.getName());
                }

            });
        }

        out.println(header);
        int index = 0;
        for (final ParameterDriver parameter : list) {
            if (parameter.isSelected()) {
                final double factor;
                if (parameter.getName().endsWith("/az bias") || parameter.getName().endsWith("/el bias")) {
                    factor = FastMath.toDegrees(1.0);
                } else {
                    factor = 1.0;
                }
                final double initial = parameter.getReferenceValue();
                final double value   = parameter.getValue();
                out.format(Locale.US, "  %2d %s", ++index, parameter.getName());
                for (int i = parameter.getName().length(); i < length; ++i) {
                    out.format(Locale.US, " ");
                }
                out.format(Locale.US, "  %+.12f  (final value:  % .12f)%n",
                           factor * (value - initial), factor * value);
            }
        }

    }
    
    /** Display covariances and sigmas as predicted by a Kalman filter at date t. 
     */
    private void displayFinalCovariances(final PrintStream logStream, final KalmanEstimator kalman) 
        {
        
//        // Get kalman estimated propagator
//        final NumericalPropagator kalmanProp = kalman.getProcessModel().getEstimatedPropagator();
//        
//        // Link the partial derivatives to this propagator
//        final String equationName = "kalman-derivatives";
//        PartialDerivativesEquations kalmanDerivatives = new PartialDerivativesEquations(equationName, kalmanProp);
//        
//        // Initialize the derivatives
//        final SpacecraftState rawState = kalmanProp.getInitialState();
//        final SpacecraftState stateWithDerivatives =
//                        kalmanDerivatives.setInitialJacobians(rawState);
//        kalmanProp.resetInitialState(stateWithDerivatives);
//        
//        // Propagate to target date
//        final SpacecraftState kalmanState = kalmanProp.propagate(targetDate);
//        
//        // Compute STM
//        RealMatrix STM = kalman.getProcessModel().getErrorStateTransitionMatrix(kalmanState, kalmanDerivatives);
//        
//        // Compute covariance matrix
//        RealMatrix P = kalman.getProcessModel().unNormalizeCovarianceMatrix(kalman.predictCovariance(STM,
//                                                                              kalman.getProcessModel().getProcessNoiseMatrix()));
        final RealMatrix P = kalman.getPhysicalEstimatedCovarianceMatrix();
        final String[] paramNames = new String[P.getRowDimension()];
        int index = 0;
        int paramSize = 0;
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getPropagationParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getEstimatedMeasurementsParameters().getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        if (paramSize < 20) {
            paramSize = 20;
        }
        
        // Header
        logStream.format("\n%s\n", "Kalman Final Covariances:");
//        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
//                         targetDate.toString(TimeScalesFactory.getUTC()));
        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
                         kalman.getCurrentDate().toString(TimeScalesFactory.getUTC()));
        
        // Covariances
        String strFormat = String.format("%%%2ds  ", paramSize);
        logStream.format(strFormat, "Covariances:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        String numFormat = String.format("%%%2d.6f  ", paramSize);
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j));
            }
            logStream.println("");
        }
        
        // Correlation coeff
        final double[] sigmas = new double[P.getRowDimension()];
        for (int i = 0; i < P.getRowDimension(); i++) {
            sigmas[i] = FastMath.sqrt(P.getEntry(i, i));
        }
        
        logStream.format("\n" + strFormat, "Corr coef:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j)/(sigmas[i]*sigmas[j]));
            }
            logStream.println("");
        }
        
        // Sigmas
        logStream.format("\n" + strFormat + "\n", "Sigmas: ");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat + numFormat + "\n", paramNames[i], sigmas[i]);
        }
        logStream.println("");
    }

    /** Create a propagator builder from input parameters
     * @param parser input file parser
     * @param conventions IERS conventions to use
     * @param gravityField gravity field
     * @param body central body
     * @param orbit first orbit estimate
     * @return propagator builder
     * @throws NoSuchElementException if input parameters are missing
     */
    private NumericalPropagatorBuilder createPropagatorBuilder(final KeyValueFileParser<ParameterKey> parser,
                                                               final IERSConventions conventions,
                                                               final NormalizedSphericalHarmonicsProvider gravityField,
                                                               final OneAxisEllipsoid body,
                                                               final Orbit orbit)
        throws NoSuchElementException {

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

        final double positionScale;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_ORBITAL_PARAMETERS_POSITION_SCALE)) {
            positionScale = dP;
        } else {
            positionScale = parser.getDouble(ParameterKey.ESTIMATOR_ORBITAL_PARAMETERS_POSITION_SCALE);
        }
        final NumericalPropagatorBuilder propagatorBuilder =
                        new NumericalPropagatorBuilder(orbit,
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       PositionAngle.MEAN,
                                                       positionScale);

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
                            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.feed(msafe.getSupportedNames(), msafe);
            Atmosphere atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), body);
            propagatorBuilder.addForceModel(new DragForce(atmosphere, new IsotropicDrag(area, cd)));
            if (cdEstimated) {
                for (final ParameterDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
                    if (driver.getName().equals(DragSensitive.DRAG_COEFFICIENT)) {
                        driver.setSelected(true);
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
                for (final ParameterDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
                    if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                        driver.setSelected(true);
                    }
                }
            }
        }

        // post-Newtonian correction force due to general relativity
        if (parser.containsKey(ParameterKey.GENERAL_RELATIVITY) && parser.getBoolean(ParameterKey.GENERAL_RELATIVITY)) {
            propagatorBuilder.addForceModel(new Relativity(gravityField.getMu()));
        }

        // extra polynomial accelerations
        if (parser.containsKey(ParameterKey.POLYNOMIAL_ACCELERATION_NAME)) {
            final String[]       names        = parser.getStringArray(ParameterKey.POLYNOMIAL_ACCELERATION_NAME);
            final Vector3D[]     directions   = parser.getVectorArray(ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_X,
                                                                      ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_Y,
                                                                      ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_Z);
            final List<String>[] coefficients = parser.getStringsListArray(ParameterKey.POLYNOMIAL_ACCELERATION_COEFFICIENTS, ',');
            final boolean[]      estimated    = parser.getBooleanArray(ParameterKey.POLYNOMIAL_ACCELERATION_ESTIMATED);

            for (int i = 0; i < names.length; ++i) {

                final PolynomialParametricAcceleration ppa =
                                new PolynomialParametricAcceleration(directions[i], true, names[i], null,
                                                                     coefficients[i].size() - 1);
                for (int k = 0; k < coefficients[i].size(); ++k) {
                    final ParameterDriver driver = ppa.getParameterDriver(names[i] + "[" + k + "]");
                    driver.setValue(Double.parseDouble(coefficients[i].get(k)));
                    driver.setSelected(estimated[i]);
                }
                propagatorBuilder.addForceModel(ppa);
            }
        }

        // attitude mode
        final AttitudeMode mode;
        if (parser.containsKey(ParameterKey.ATTITUDE_MODE)) {
            mode = AttitudeMode.valueOf(parser.getString(ParameterKey.ATTITUDE_MODE));
        } else {
            mode = AttitudeMode.NADIR_POINTING_WITH_YAW_COMPENSATION;
        }
        propagatorBuilder.setAttitudeProvider(mode.getProvider(orbit.getFrame(), body));

        return propagatorBuilder;

    }

    /** Create a gravity field from input parameters
     * @param parser input file parser
     * @return gravity field
     * @throws NoSuchElementException if input parameters are missing
     */
    private NormalizedSphericalHarmonicsProvider createGravityField(final KeyValueFileParser<ParameterKey> parser)
                    throws NoSuchElementException {
        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        return GravityFieldFactory.getNormalizedProvider(degree, order);
    }

    /** Create an orbit from input parameters
     * @param parser input file parser
     * @param mu     central attraction coefficient
     * @throws NoSuchElementException if input parameters are missing
     */
    private OneAxisEllipsoid createBody(final KeyValueFileParser<ParameterKey> parser)
                    throws NoSuchElementException {

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
     */
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser,
                              final double mu) throws NoSuchElementException {

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
     */
    private Bias<Range> createSatRangeBias(final KeyValueFileParser<ParameterKey> parser)
                    {

        // transponder delay
        final double transponderDelayBias;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS)) {
            transponderDelayBias = 0;
        } else {
            transponderDelayBias = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS);
        }

        final double transponderDelayBiasMin;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_MIN)) {
            transponderDelayBiasMin = Double.NEGATIVE_INFINITY;
        } else {
            transponderDelayBiasMin = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS_MIN);
        }

        final double transponderDelayBiasMax;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_MAX)) {
            transponderDelayBiasMax = Double.NEGATIVE_INFINITY;
        } else {
            transponderDelayBiasMax = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS_MAX);
        }

        // bias estimation flag
        final boolean transponderDelayBiasEstimated;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_ESTIMATED)) {
            transponderDelayBiasEstimated = false;
        } else {
            transponderDelayBiasEstimated = parser.getBoolean(ParameterKey.ONBOARD_RANGE_BIAS_ESTIMATED);
        }

        if (FastMath.abs(transponderDelayBias) >= Precision.SAFE_MIN || transponderDelayBiasEstimated) {
            // bias is either non-zero or will be estimated,
            // we really need to create a modifier for this
            final Bias<Range> bias = new Bias<Range>(new String[] {
                "transponder delay bias",
            },
                            new double[] {
                                transponderDelayBias
            },
                            new double[] {
                                1.0
            },
                            new double[] {
                                transponderDelayBiasMin
            },
                            new double[] {
                                transponderDelayBiasMax
            });
            bias.getParametersDrivers().get(0).setSelected(transponderDelayBiasEstimated);
            return bias;
        } else {
            // fixed zero bias, we don't need any modifier
            return null;
        }

    }

    /** Set up range modifier taking on-board antenna offset.
     * @param parser input file parser
     * @return range modifier (may be null if antenna offset is zero or undefined)
     */
    public OnBoardAntennaRangeModifier createSatAntennaRangeModifier(final KeyValueFileParser<ParameterKey> parser) {
        final Vector3D offset;
        if (!parser.containsKey(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X)) {
            offset = Vector3D.ZERO;
        } else {
            offset = parser.getVector(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Y,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Z);
        }
        return offset.getNorm() > 0 ? new OnBoardAntennaRangeModifier(offset) : null;
    }

    /** Set up stations.
     * @param parser input file parser
     * @param conventions IERS conventions to use
     * @param body central body
     * @return name to station data map
     * @throws NoSuchElementException if input parameters are missing
     */
    private Map<String, StationData> createStationsData(final KeyValueFileParser<ParameterKey> parser,
                                                        final IERSConventions conventions,
                                                        final OneAxisEllipsoid body)
        throws NoSuchElementException {

        final Map<String, StationData> stations       = new HashMap<String, StationData>();

        final String[]  stationNames                      = parser.getStringArray(ParameterKey.GROUND_STATION_NAME);
        final double[]  stationLatitudes                  = parser.getAngleArray(ParameterKey.GROUND_STATION_LATITUDE);
        final double[]  stationLongitudes                 = parser.getAngleArray(ParameterKey.GROUND_STATION_LONGITUDE);
        final double[]  stationAltitudes                  = parser.getDoubleArray(ParameterKey.GROUND_STATION_ALTITUDE);
        final boolean[] stationPositionEstimated          = parser.getBooleanArray(ParameterKey.GROUND_STATION_POSITION_ESTIMATED);
        final double[]  stationClockOffsets               = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET);
        final double[]  stationClockOffsetsMin            = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_MIN);
        final double[]  stationClockOffsetsMax            = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_MAX);
        final boolean[] stationClockOffsetEstimated       = parser.getBooleanArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_ESTIMATED);
        final double[]  stationRangeSigma                 = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_SIGMA);
        final double[]  stationRangeBias                  = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS);
        final double[]  stationRangeBiasMin               = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS_MIN);
        final double[]  stationRangeBiasMax               = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS_MAX);
        final boolean[] stationRangeBiasEstimated         = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_BIAS_ESTIMATED);
        final double[]  stationRangeRateSigma             = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_SIGMA);
        final double[]  stationRangeRateBias              = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS);
        final double[]  stationRangeRateBiasMin           = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_MIN);
        final double[]  stationRangeRateBiasMax           = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_MAX);
        final boolean[] stationRangeRateBiasEstimated     = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_ESTIMATED);
        final double[]  stationAzimuthSigma               = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_SIGMA);
        final double[]  stationAzimuthBias                = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS);
        final double[]  stationAzimuthBiasMin             = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS_MIN);
        final double[]  stationAzimuthBiasMax             = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS_MAX);
        final double[]  stationElevationSigma             = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_SIGMA);
        final double[]  stationElevationBias              = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS);
        final double[]  stationElevationBiasMin           = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS_MIN);
        final double[]  stationElevationBiasMax           = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS_MAX);
        final boolean[] stationAzElBiasesEstimated        = parser.getBooleanArray(ParameterKey.GROUND_STATION_AZ_EL_BIASES_ESTIMATED);
        final boolean[] stationElevationRefraction        = parser.getBooleanArray(ParameterKey.GROUND_STATION_ELEVATION_REFRACTION_CORRECTION);
        final boolean[] stationTroposphericModelEstimated = parser.getBooleanArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_MODEL_ESTIMATED);
        final double[]  stationTroposphericZenithDelay    = parser.getDoubleArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_ZENITH_DELAY);
        final boolean[] stationZenithDelayEstimated       = parser.getBooleanArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_DELAY_ESTIMATED);
        final boolean[] stationGlobalMappingFunction      = parser.getBooleanArray(ParameterKey.GROUND_STATION_GLOBAL_MAPPING_FUNCTION);
        final boolean[] stationNiellMappingFunction       = parser.getBooleanArray(ParameterKey.GROUND_STATION_NIELL_MAPPING_FUNCTION);
        final boolean[] stationWeatherEstimated           = parser.getBooleanArray(ParameterKey.GROUND_STATION_WEATHER_ESTIMATED);
        final boolean[] stationRangeTropospheric          = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_TROPOSPHERIC_CORRECTION);
        //final boolean[] stationIonosphericCorrection    = parser.getBooleanArray(ParameterKey.GROUND_STATION_IONOSPHERIC_CORRECTION);

        final TidalDisplacement tidalDisplacement;
        if (parser.containsKey(ParameterKey.SOLID_TIDES_DISPLACEMENT_CORRECTION) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_DISPLACEMENT_CORRECTION)) {
            final boolean removePermanentDeformation =
                            parser.containsKey(ParameterKey.SOLID_TIDES_DISPLACEMENT_REMOVE_PERMANENT_DEFORMATION) &&
                            parser.getBoolean(ParameterKey.SOLID_TIDES_DISPLACEMENT_REMOVE_PERMANENT_DEFORMATION);
            tidalDisplacement = new TidalDisplacement(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                                      Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                                      CelestialBodyFactory.getSun(),
                                                      CelestialBodyFactory.getMoon(),
                                                      conventions,
                                                      removePermanentDeformation);
        } else {
            tidalDisplacement = null;
        }

        final OceanLoadingCoefficientsBLQFactory blqFactory;
        if (parser.containsKey(ParameterKey.OCEAN_LOADING_CORRECTION) &&
            parser.getBoolean(ParameterKey.OCEAN_LOADING_CORRECTION)) {
            blqFactory = new OceanLoadingCoefficientsBLQFactory("^.*\\.blq$");
        } else {
            blqFactory = null;
        }

        final EOPHistory eopHistory = FramesFactory.findEOP(body.getBodyFrame());
        for (int i = 0; i < stationNames.length; ++i) {

            // displacements
            final StationDisplacement[] displacements;
            final OceanLoading oceanLoading = (blqFactory == null) ?
                                              null :
                                              new OceanLoading(body, blqFactory.getCoefficients(stationNames[i]));
            if (tidalDisplacement == null) {
                if (oceanLoading == null) {
                    displacements = new StationDisplacement[0];
                } else {
                    displacements = new StationDisplacement[] {
                        oceanLoading
                    };
                }
            } else {
                if (oceanLoading == null) {
                    displacements = new StationDisplacement[] {
                        tidalDisplacement
                    };
                } else {
                    displacements = new StationDisplacement[] {
                        tidalDisplacement, oceanLoading
                    };
                }
            }

            // the station itself
            final GeodeticPoint position = new GeodeticPoint(stationLatitudes[i],
                                                             stationLongitudes[i],
                                                             stationAltitudes[i]);
            final TopocentricFrame topo = new TopocentricFrame(body, position, stationNames[i]);
            final GroundStation station = new GroundStation(topo, eopHistory, displacements);
            station.getClockOffsetDriver().setReferenceValue(stationClockOffsets[i]);
            station.getClockOffsetDriver().setValue(stationClockOffsets[i]);
            station.getClockOffsetDriver().setMinValue(stationClockOffsetsMin[i]);
            station.getClockOffsetDriver().setMaxValue(stationClockOffsetsMax[i]);
            station.getClockOffsetDriver().setSelected(stationClockOffsetEstimated[i]);
            station.getEastOffsetDriver().setSelected(stationPositionEstimated[i]);
            station.getNorthOffsetDriver().setSelected(stationPositionEstimated[i]);
            station.getZenithOffsetDriver().setSelected(stationPositionEstimated[i]);

            // range
            final double rangeSigma = stationRangeSigma[i];
            final Bias<Range> rangeBias;
            if (FastMath.abs(stationRangeBias[i])   >= Precision.SAFE_MIN || stationRangeBiasEstimated[i]) {
                 rangeBias = new Bias<Range>(new String[] {
                                                 stationNames[i] + "/range bias",
                                             },
                                             new double[] {
                                                 stationRangeBias[i]
                                             },
                                             new double[] {
                                                 rangeSigma
                                             },
                                             new double[] {
                                                 stationRangeBiasMin[i]
                                             },
                                             new double[] {
                                                 stationRangeBiasMax[i]
                                             });
                 rangeBias.getParametersDrivers().get(0).setSelected(stationRangeBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeBias = null;
            }

            // range rate
            final double rangeRateSigma = stationRangeRateSigma[i];
            final Bias<RangeRate> rangeRateBias;
            if (FastMath.abs(stationRangeRateBias[i])   >= Precision.SAFE_MIN || stationRangeRateBiasEstimated[i]) {
                rangeRateBias = new Bias<RangeRate>(new String[] {
                                                        stationNames[i] + "/range rate bias"
                                                    },
                                                    new double[] {
                                                        stationRangeRateBias[i]
                                                    },
                                                    new double[] {
                                                        rangeRateSigma
                                                    },
                                                    new double[] {
                                                        stationRangeRateBiasMin[i]
                                                    },
                                                    new double[] {
                                                        stationRangeRateBiasMax[i]
                                                    });
                rangeRateBias.getParametersDrivers().get(0).setSelected(stationRangeRateBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeRateBias = null;
            }

            // angular biases
            final double[] azELSigma = new double[] {
                stationAzimuthSigma[i], stationElevationSigma[i]
            };
            final Bias<AngularAzEl> azELBias;
            if (FastMath.abs(stationAzimuthBias[i])   >= Precision.SAFE_MIN ||
                FastMath.abs(stationElevationBias[i]) >= Precision.SAFE_MIN ||
                stationAzElBiasesEstimated[i]) {
                azELBias = new Bias<AngularAzEl>(new String[] {
                                                 stationNames[i] + "/az bias",
                                                 stationNames[i] + "/el bias"
                                             },
                                             new double[] {
                                                 stationAzimuthBias[i],
                                                 stationElevationBias[i]
                                             },
                                             azELSigma,
                                             new double[] {
                                                 stationAzimuthBiasMin[i],
                                                 stationElevationBiasMin[i]
                                             },
                                             new double[] {
                                                 stationAzimuthBiasMax[i],
                                                 stationElevationBiasMax[i]
                                             });
                azELBias.getParametersDrivers().get(0).setSelected(stationAzElBiasesEstimated[i]);
                azELBias.getParametersDrivers().get(1).setSelected(stationAzElBiasesEstimated[i]);
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

                MappingFunction mappingModel = null;
                if (stationGlobalMappingFunction[i]) {
                    mappingModel = new GlobalMappingFunctionModel(stationLatitudes[i],
                                                                  stationLongitudes[i]);
                } else if (stationNiellMappingFunction[i]) {
                    mappingModel = new NiellMappingFunctionModel(stationLatitudes[i]);
                }

                final DiscreteTroposphericModel troposphericModel;
                if (stationTroposphericModelEstimated[i] && mappingModel != null) {

                    if(stationWeatherEstimated[i]) {
                        final GlobalPressureTemperatureModel weather = new GlobalPressureTemperatureModel(stationLatitudes[i],
                                                                                                          stationLongitudes[i],
                                                                                                          body.getBodyFrame());
                        weather.weatherParameters(stationAltitudes[i], parser.getDate(ParameterKey.ORBIT_DATE,
                                                                                      TimeScalesFactory.getUTC()));
                        final double temperature = weather.getTemperature();
                        final double pressure    = weather.getPressure();
                        troposphericModel = new EstimatedTroposphericModel(temperature, pressure, mappingModel,
                                                                           stationTroposphericZenithDelay[i]);
                    } else {
                        troposphericModel = new EstimatedTroposphericModel(mappingModel, stationTroposphericZenithDelay[i]);   
                    }

                    ParameterDriver totalDelay = troposphericModel.getParametersDrivers().get(0);
                    totalDelay.setSelected(stationZenithDelayEstimated[i]);
                    totalDelay.setName(stationNames[i].substring(0, 5) + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
                } else {
                    troposphericModel = SaastamoinenModel.getStandardModel();
                }

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
                               parser.getDouble(ParameterKey.AZIMUTH_MEASUREMENTS_BASE_WEIGHT),
                               parser.getDouble(ParameterKey.ELEVATION_MEASUREMENTS_BASE_WEIGHT)
        },
                           parser.getDouble(ParameterKey.PV_MEASUREMENTS_BASE_WEIGHT));
    }

    /** Set up outliers manager for range measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<Range> createRangeOutliersManager(final KeyValueFileParser<ParameterKey> parser)
                    {
        if (parser.containsKey(ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER) !=
                        parser.containsKey(ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION)) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER.toString().toLowerCase().replace('_', '.') +
                                      " and  " +
                                      ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION.toString().toLowerCase().replace('_', '.') +
                            " must be both present or both absent");
        }
        return new DynamicOutlierFilter<Range>(parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION),
                        parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for range-rate measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<RangeRate> createRangeRateOutliersManager(final KeyValueFileParser<ParameterKey> parser)
                    {
        if (parser.containsKey(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER) !=
                        parser.containsKey(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION)) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER.toString().toLowerCase().replace('_', '.') +
                                      " and  " +
                                      ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION.toString().toLowerCase().replace('_', '.') +
                            " must be both present or both absent");
        }
        return new DynamicOutlierFilter<RangeRate>(parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION),
                        parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for azimuth-elevation measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<AngularAzEl> createAzElOutliersManager(final KeyValueFileParser<ParameterKey> parser)
                    {
        if (parser.containsKey(ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER) !=
                        parser.containsKey(ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION)) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER.toString().toLowerCase().replace('_', '.') +
                                      " and  " +
                                      ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION.toString().toLowerCase().replace('_', '.') +
                            " must be both present or both absent");
        }
        return new DynamicOutlierFilter<AngularAzEl>(parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION),
                        parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for PV measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<PV> createPVOutliersManager(final KeyValueFileParser<ParameterKey> parser)
                    {
        if (parser.containsKey(ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER) !=
                        parser.containsKey(ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION)) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER.toString().toLowerCase().replace('_', '.') +
                                      " and  " +
                                      ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION.toString().toLowerCase().replace('_', '.') +
                            " must be both present or both absent");
        }
        return new DynamicOutlierFilter<PV>(parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION),
                        parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER));
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

    /** Set up satellite data.
     * @param parser input file parser
     * @return satellite data
     * @throws NoSuchElementException if input parameters are missing
     */
    private ObservableSatellite createObservableSatellite(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        ObservableSatellite obsSat = new ObservableSatellite(0);
        ParameterDriver clockOffsetDriver = obsSat.getClockOffsetDriver();
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET)) {
            clockOffsetDriver.setReferenceValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET));
            clockOffsetDriver.setValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_MIN)) {
            clockOffsetDriver.setMinValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET_MIN));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_MAX)) {
            clockOffsetDriver.setMaxValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET_MAX));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_ESTIMATED)) {
            clockOffsetDriver.setSelected(parser.getBoolean(ParameterKey.ON_BOARD_CLOCK_OFFSET_ESTIMATED));
        }
        return obsSat;
    }

    /** Read a RINEX measurements file.
     * @param file measurements file
     * @param satId satellite we are interested in
     * @param stations name to stations data map
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param satAntennaRangeModifier modifier for on-board antenna offset
     * @param weights base weights for measurements
     * @param rangeOutliersManager manager for range measurements outliers (null if none configured)
     * @param rangeRateOutliersManager manager for range-rate measurements outliers (null if none configured)
     * @return measurements list
     */
    private List<ObservedMeasurement<?>> readRinex(final File file, final String satId,
                                                   final Map<String, StationData> stations,
                                                   final ObservableSatellite satellite,
                                                   final Bias<Range> satRangeBias,
                                                   final OnBoardAntennaRangeModifier satAntennaRangeModifier,
                                                   final Weights weights,
                                                   final OutlierFilter<Range> rangeOutliersManager,
                                                   final OutlierFilter<RangeRate> rangeRateOutliersManager)
        throws UnsupportedEncodingException, IOException, OrekitException {
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(satId);
        final int prnNumber;
        switch (system) {
            case GPS:
            case GLONASS:
            case GALILEO:
                prnNumber = Integer.parseInt(satId.substring(1));
                break;
            case SBAS:
                prnNumber = Integer.parseInt(satId.substring(1)) + 100;
                break;
            default:
                prnNumber = -1;
        }
        final Iono iono = new Iono(false);
        final RinexLoader loader = new RinexLoader(new FileInputStream(file), file.getAbsolutePath());
        for (final ObservationDataSet observationDataSet : loader.getObservationDataSets()) {
            if (observationDataSet.getSatelliteSystem() == system    &&
                observationDataSet.getPrnNumber()       == prnNumber) {
                for (final ObservationData od : observationDataSet.getObservationData()) {
                    if (!Double.isNaN(od.getValue())) {
                        if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                            // this is a measurement we want
                            final String stationName = observationDataSet.getHeader().getMarkerName() + "/" + od.getObservationType();
                            StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + " not configured");
                            }
                            Range range = new Range(stationData.station, false, observationDataSet.getDate(),
                                                    od.getValue(), stationData.rangeSigma,
                                                    weights.rangeBaseWeight, satellite);
                            range.addModifier(iono.getRangeModifier(od.getObservationType().getFrequency(system),
                                                                    observationDataSet.getDate()));
                            if (satAntennaRangeModifier != null) {
                                range.addModifier(satAntennaRangeModifier);
                            }
                            if (stationData.rangeBias != null) {
                                range.addModifier(stationData.rangeBias);
                            }
                            if (satRangeBias != null) {
                                range.addModifier(satRangeBias);
                            }
                            if (stationData.rangeTroposphericCorrection != null) {
                                range.addModifier(stationData.rangeTroposphericCorrection);
                            }
                            addIfNonZeroWeight(range, measurements);

                        } else if (od.getObservationType().getMeasurementType() == MeasurementType.DOPPLER) {
                            // this is a measurement we want
                            final String stationName = observationDataSet.getHeader().getMarkerName() + "/" + od.getObservationType();
                            StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + " not configured");
                            }
                            RangeRate rangeRate = new RangeRate(stationData.station, observationDataSet.getDate(),
                                                                od.getValue(), stationData.rangeRateSigma,
                                                                weights.rangeRateBaseWeight, false, satellite);
                            rangeRate.addModifier(iono.getRangeRateModifier(od.getObservationType().getFrequency(system),
                                                                            observationDataSet.getDate()));
                            if (stationData.rangeRateBias != null) {
                                rangeRate.addModifier(stationData.rangeRateBias);
                            }
                            addIfNonZeroWeight(rangeRate, measurements);
                        }
                    }
                }
            }
        }

        return measurements;

    }

    /** Read a measurements file.
     * @param file measurements file
     * @param stations name to stations data map
     * @param pvData PV measurements data
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param satAntennaRangeModifier modifier for on-board antenna offset
     * @param weights base weights for measurements
     * @param rangeOutliersManager manager for range measurements outliers (null if none configured)
     * @param rangeRateOutliersManager manager for range-rate measurements outliers (null if none configured)
     * @param azElOutliersManager manager for azimuth-elevation measurements outliers (null if none configured)
     * @param pvOutliersManager manager for PV measurements outliers (null if none configured)
     * @return measurements list
     */
    private List<ObservedMeasurement<?>> readMeasurements(final File file,
                                                          final Map<String, StationData> stations,
                                                          final PVData pvData,
                                                          final ObservableSatellite satellite,
                                                          final Bias<Range> satRangeBias,
                                                          final OnBoardAntennaRangeModifier satAntennaRangeModifier,
                                                          final Weights weights,
                                                          final OutlierFilter<Range> rangeOutliersManager,
                                                          final OutlierFilter<RangeRate> rangeRateOutliersManager,
                                                          final OutlierFilter<AngularAzEl> azElOutliersManager,
                                                          final OutlierFilter<PV> pvOutliersManager)
        throws UnsupportedEncodingException, IOException, OrekitException {

        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
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
                    switch (fields[1]) {
                        case "RANGE" :
                            final Range range = new RangeParser().parseFields(fields, stations, pvData, satellite,
                                                                              satRangeBias, weights,
                                                                              line, lineNumber, file.getName());
                            if (satAntennaRangeModifier != null) {
                                range.addModifier(satAntennaRangeModifier);
                            }
                            if (rangeOutliersManager != null) {
                                range.addModifier(rangeOutliersManager);
                            }
                            addIfNonZeroWeight(range, measurements);
                            break;
                        case "RANGE_RATE" :
                            final RangeRate rangeRate = new RangeRateParser().parseFields(fields, stations, pvData, satellite,
                                                                                          satRangeBias, weights,
                                                                                          line, lineNumber, file.getName());
                            if (rangeOutliersManager != null) {
                                rangeRate.addModifier(rangeRateOutliersManager);
                            }
                            addIfNonZeroWeight(rangeRate, measurements);
                            break;
                        case "AZ_EL" :
                            final AngularAzEl angular = new AzElParser().parseFields(fields, stations, pvData, satellite,
                                                                                     satRangeBias, weights,
                                                                                     line, lineNumber, file.getName());
                            if (azElOutliersManager != null) {
                                angular.addModifier(azElOutliersManager);
                            }
                            addIfNonZeroWeight(angular, measurements);
                            break;
                        case "PV" :
                            final PV pv = new PVParser().parseFields(fields, stations, pvData, satellite,
                                                                     satRangeBias, weights,
                                                                     line, lineNumber, file.getName());
                            if (pvOutliersManager != null) {
                                pv.addModifier(pvOutliersManager);
                            }
                            addIfNonZeroWeight(pv, measurements);
                            break;
                        default :
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

    /** Add a measurement to a list if it has non-zero weight.
     * @param measurement measurement to add
     * @param measurements measurements list
     */
    private void addIfNonZeroWeight(final ObservedMeasurement<?> measurement, final List<ObservedMeasurement<?>> measurements) {
        double sum = 0;
        for (double w : measurement.getBaseWeight()) {
            sum += FastMath.abs(w);
        }
        if (sum > Precision.SAFE_MIN) {
            // we only consider measurements with non-zero weight
            measurements.add(measurement);
        }
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
        private final Bias<AngularAzEl> azELBias;

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
                           final double[] azElSigma, final Bias<AngularAzEl> azELBias,
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
    private static abstract class MeasurementsParser<T extends ObservedMeasurement<T>> {

        /** Parse the fields of a measurements line.
         * @param fields measurements line fields
         * @param stations name to stations data map
         * @param pvData PV measurements data
         * @param satellite satellite reference
         * @param satRangeBias range bias due to transponder delay
         * @param weight base weights for measurements
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         * @return parsed measurement
         */
        public abstract T parseFields(String[] fields,
                                      Map<String, StationData> stations,
                                      PVData pvData, ObservableSatellite satellite,
                                      Bias<Range> satRangeBias, Weights weight,
                                      String line, int lineNumber, String fileName);

        /** Check the number of fields.
         * @param expected expected number of fields
         * @param fields measurements line fields
         * @param line complete line
         * @param lineNumber line number
         * @param fileName file name
         */
        protected void checkFields(final int expected, final String[] fields,
                                   final String line, final int lineNumber, final String fileName)
                                                   {
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
         */
        protected AbsoluteDate getDate(final String date,
                                       final String line, final int lineNumber, final String fileName)
                                                       {
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
         */
        protected StationData getStationData(final String stationName,
                                             final Map<String, StationData> stations,
                                             final String line, final int lineNumber, final String fileName)
                                                             {
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

    /** Parser for range measurements. */
    private static class RangeParser extends MeasurementsParser<Range> {
        /** {@inheritDoc} */
        @Override
        public Range parseFields(final String[] fields,
                                 final Map<String, StationData> stations,
                                 final PVData pvData,
                                 final ObservableSatellite satellite,
                                 final Bias<Range> satRangeBias,
                                 final Weights weights,
                                 final String line,
                                 final int lineNumber,
                                 final String fileName) {
            checkFields(4, fields, line, lineNumber, fileName);
            final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
            final Range range = new Range(stationData.station, true,
                                          getDate(fields[0], line, lineNumber, fileName),
                                          Double.parseDouble(fields[3]) * 1000.0,
                                          stationData.rangeSigma,
                                          weights.rangeBaseWeight,
                                          satellite);
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
    }

    /** Parser for range rate measurements. */
    private static class RangeRateParser extends MeasurementsParser<RangeRate> {
        /** {@inheritDoc} */
        @Override
        public RangeRate parseFields(final String[] fields,
                                     final Map<String, StationData> stations,
                                     final PVData pvData,
                                     final ObservableSatellite satellite,
                                     final Bias<Range> satRangeBias,
                                     final Weights weights,
                                     final String line,
                                     final int lineNumber,
                                     final String fileName) {
            checkFields(4, fields, line, lineNumber, fileName);
            final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
            final RangeRate rangeRate = new RangeRate(stationData.station,
                                                      getDate(fields[0], line, lineNumber, fileName),
                                                      Double.parseDouble(fields[3]) * 1000.0,
                                                      stationData.rangeRateSigma,
                                                      weights.rangeRateBaseWeight,
                                                      true, satellite);
            if (stationData.rangeRateBias != null) {
                rangeRate.addModifier(stationData.rangeRateBias);
            }
            return rangeRate;
        }
    };

    /** Parser for azimuth-elevation measurements. */
    private static class AzElParser extends MeasurementsParser<AngularAzEl> {
        /** {@inheritDoc} */
        @Override
        public AngularAzEl parseFields(final String[] fields,
                                       final Map<String, StationData> stations,
                                       final PVData pvData,
                                       final ObservableSatellite satellite,
                                       final Bias<Range> satRangeBias,
                                       final Weights weights,
                                       final String line,
                                       final int lineNumber,
                                       final String fileName) {
            checkFields(5, fields, line, lineNumber, fileName);
            final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
            final AngularAzEl azEl = new AngularAzEl(stationData.station,
                                                     getDate(fields[0], line, lineNumber, fileName),
                                                     new double[] {
                                                         FastMath.toRadians(Double.parseDouble(fields[3])),
                                                         FastMath.toRadians(Double.parseDouble(fields[4]))
            },
                                                     stationData.azElSigma,
                                                     weights.azElBaseWeight,
                                                     satellite);
            if (stationData.refractionCorrection != null) {
                azEl.addModifier(stationData.refractionCorrection);
            }
            if (stationData.azELBias != null) {
                azEl.addModifier(stationData.azELBias);
            }
            return azEl;
        }
    };

    /** Parser for PV measurements. */
    private static class PVParser extends MeasurementsParser<PV> {
        /** {@inheritDoc} */
        @Override
        public PV parseFields(final String[] fields,
                              final Map<String, StationData> stations,
                              final PVData pvData,
                              final ObservableSatellite satellite,
                              final Bias<Range> satRangeBias,
                              final Weights weights,
                              final String line,
                              final int lineNumber,
                              final String fileName) {
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
                                                             weights.pvBaseWeight,
                                                             satellite);
        }
    };

    /** Local class for measurement-specific log.
     * @param T type of mesurement
     */
    private static abstract class MeasurementLog<T extends ObservedMeasurement<T>> {

        /** Map of estimated and predicted measurements. */
        private final SortedMap<Integer, EstimatedMeasurement<T>> evaluations;

        /** Measurements name. */
        private final String name;

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @param name measurement name
         * @exception IOException if output file cannot be created
         */
        MeasurementLog(final String name) throws IOException {
            this.evaluations = new TreeMap<>();
            this.name        = name;
        }

        /** Compute residual value.
         * @param estimation estimation to consider
         */
        abstract double residual(final EstimatedMeasurement<T> estimation);

        /** Add an evaluation.
         * @param measurementNb measurement number
         * @param estimation estimation to add
         */
        void add(final int measurementNb, final EstimatedMeasurement<T> estimation) {
            evaluations.put(measurementNb, estimation);
        }

        /** Create a  statistics summary
         */
        public StreamingStatistics createStatisticsSummary() {
            if (!evaluations.isEmpty()) {
                // compute statistics
                final StreamingStatistics stats = new StreamingStatistics(true);
                for (final Map.Entry<Integer, EstimatedMeasurement<T>> entries : evaluations.entrySet()) {
                    final EstimatedMeasurement<T> estimated = entries.getValue();
                    if (estimated.getObservedMeasurement().isEnabled()) {
                        stats.addValue(residual(estimated));
                    }
                }
                return stats;

            }
            return null;
        }
        
        /** Display summary statistics in the general log file.
         * @param logStream log stream
         */
        public void displaySummary(final PrintStream logStream) {
            if (!evaluations.isEmpty()) {

                // Compute statistics
                final StreamingStatistics stats = createStatisticsSummary();

                // Display statistics
                logStream.println("Measurements type: " + name);
                logStream.println("   number of measurements: " + stats.getN() + "/" + evaluations.size());
                logStream.println("   residuals min  value  : " + stats.getMin());
                logStream.println("   residuals max  value  : " + stats.getMax());
                logStream.println("   residuals mean value  : " + stats.getMean());
                logStream.println("   residuals σ           : " + stats.getStandardDeviation());
                logStream.println("   residuals median      : " + stats.getMedian());

            }
        }
    }

    /** Logger for range measurements. */
    class RangeLog extends MeasurementLog<Range> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        RangeLog() throws IOException {
            super("range");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<Range> evaluation) {
            return evaluation.getEstimatedValue()[0] - evaluation.getObservedMeasurement().getObservedValue()[0];
        }

    }

    /** Logger for range rate measurements. */
    class RangeRateLog extends MeasurementLog<RangeRate> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file 
         */
        RangeRateLog() throws IOException {
            super("range-rate");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<RangeRate> evaluation) {
            return evaluation.getEstimatedValue()[0] - evaluation.getObservedMeasurement().getObservedValue()[0];
        }

    }

    /** Logger for azimuth measurements. */
    class AzimuthLog extends MeasurementLog<AngularAzEl> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        AzimuthLog() throws IOException {
            super("azimuth");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<AngularAzEl> evaluation) {
            return FastMath.toDegrees(evaluation.getEstimatedValue()[0] - evaluation.getObservedMeasurement().getObservedValue()[0]);
        }

    }

    /** Logger for elevation measurements. */
    class ElevationLog extends MeasurementLog<AngularAzEl> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        ElevationLog() throws IOException {
            super( "elevation");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<AngularAzEl> evaluation) {
            return FastMath.toDegrees(evaluation.getEstimatedValue()[1] - evaluation.getObservedMeasurement().getObservedValue()[1]);
        }

    }

    /** Logger for position measurements. */
    class PositionLog extends MeasurementLog<PV> {

        /** Simple constructor.
         * @param home home directory
         * @param baseName output file base name (may be null)
         * @exception IOException if output file cannot be created
         */
        PositionLog() throws IOException {
            super( "position");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<PV> evaluation) {
            final double[] theoretical = evaluation.getEstimatedValue();
            final double[] observed    = evaluation.getObservedMeasurement().getObservedValue();
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
            super( "velocity");
        }

        /** {@inheritDoc} */
        @Override
        double residual(final EstimatedMeasurement<PV> evaluation) {
            final double[] theoretical = evaluation.getEstimatedValue();
            final double[] observed    = evaluation.getObservedMeasurement().getObservedValue();
            return Vector3D.distance(new Vector3D(theoretical[3], theoretical[4], theoretical[5]),
                                     new Vector3D(observed[3],    observed[4],    observed[5]));
        }

    }

    /** Ionospheric modifiers. */
    private static class Iono {

        /** Flag for two-way range-rate. */
        private final boolean twoWay;

        /** Map for range modifiers. */
        private final Map<Frequency, Map<DateComponents, RangeIonosphericDelayModifier>> rangeModifiers;

        /** Map for range-rate modifiers. */
        private final Map<Frequency, Map<DateComponents, RangeRateIonosphericDelayModifier>> rangeRateModifiers;

        /** Simple constructor.
         * @param twoWay flag for two-way range-rate
         */
        Iono(final boolean twoWay) {
            this.twoWay             = twoWay;
            this.rangeModifiers     = new HashMap<>();
            this.rangeRateModifiers = new HashMap<>();
        }

        /** Get range modifier for a measurement.
         * @param frequency frequency of the signal
         * @param date measurement date
         * @return range modifier
         */
        public RangeIonosphericDelayModifier getRangeModifier(final Frequency frequency,
                                                              final AbsoluteDate date)
            {
            final DateComponents dc = date.getComponents(TimeScalesFactory.getUTC()).getDate();
            ensureFrequencyAndDateSupported(frequency, dc);
            return rangeModifiers.get(frequency).get(dc);
        }

        /** Get range-rate modifier for a measurement.
         * @param frequency frequency of the signal
         * @param date measurement date
         * @return range-rate modifier
         */
        public RangeRateIonosphericDelayModifier getRangeRateModifier(final Frequency frequency,
                                                                      final AbsoluteDate date)
            {
            final DateComponents dc = date.getComponents(TimeScalesFactory.getUTC()).getDate();
            ensureFrequencyAndDateSupported(frequency, dc);
            return rangeRateModifiers.get(frequency).get(dc);
         }

        /** Create modifiers for a frequency and date if needed.
         * @param frequency frequency of the signal
         * @param dc date for which modifiers are required
         */
        private void ensureFrequencyAndDateSupported(final Frequency frequency, final DateComponents dc)
            {

            if (!rangeModifiers.containsKey(frequency)) {
                rangeModifiers.put(frequency, new HashMap<>());
                rangeRateModifiers.put(frequency, new HashMap<>());
            }

            if (!rangeModifiers.get(frequency).containsKey(dc)) {

                // load Klobuchar model for the L1 frequency
                final KlobucharIonoCoefficientsLoader loader = new KlobucharIonoCoefficientsLoader();
                loader.loadKlobucharIonosphericCoefficients(dc);
                final IonosphericModel model = new KlobucharIonoModel(loader.getAlpha(), loader.getBeta());

                // frequency
                final double f = frequency.getMHzFrequency() * 1.0e6;

                // create modifiers
                rangeModifiers.get(frequency).put(dc, new RangeIonosphericDelayModifier(model, f));
                rangeRateModifiers.get(frequency).put(dc, new RangeRateIonosphericDelayModifier(model, f, twoWay));

            }

        }

    }

    /** Attitude modes. */
    private static enum AttitudeMode {
        NADIR_POINTING_WITH_YAW_COMPENSATION() {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                            {
                return new YawCompensation(inertialFrame, new NadirPointing(inertialFrame, body));
            }
        },
        CENTER_POINTING_WITH_YAW_STEERING {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                            {
                return new YawSteering(inertialFrame,
                                       new BodyCenterPointing(inertialFrame, body),
                                       CelestialBodyFactory.getSun(),
                                       Vector3D.PLUS_I);
            }
        },
        LOF_ALIGNED_LVLH {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                            {
                return new LofOffset(inertialFrame, LOFType.LVLH);
            }
        },
        LOF_ALIGNED_QSW {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                            {
                return new LofOffset(inertialFrame, LOFType.QSW);
            }
        },
        LOF_ALIGNED_TNW {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                {
                return new LofOffset(inertialFrame, LOFType.TNW);
            }
        },
        LOF_ALIGNED_VNC {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                {
                return new LofOffset(inertialFrame, LOFType.VNC);
            }
        },
        LOF_ALIGNED_VVLH {
            public AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
                {
                return new LofOffset(inertialFrame, LOFType.VVLH);
            }
        };

        public abstract AttitudeProvider getProvider(final Frame inertialFrame, final OneAxisEllipsoid body)
           ;

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
        ATTITUDE_MODE,
        POLYNOMIAL_ACCELERATION_NAME,
        POLYNOMIAL_ACCELERATION_DIRECTION_X,
        POLYNOMIAL_ACCELERATION_DIRECTION_Y,
        POLYNOMIAL_ACCELERATION_DIRECTION_Z,
        POLYNOMIAL_ACCELERATION_COEFFICIENTS,
        POLYNOMIAL_ACCELERATION_ESTIMATED,
        ONBOARD_RANGE_BIAS,
        ONBOARD_RANGE_BIAS_MIN,
        ONBOARD_RANGE_BIAS_MAX,
        ONBOARD_RANGE_BIAS_ESTIMATED,
        ON_BOARD_ANTENNA_PHASE_CENTER_X,
        ON_BOARD_ANTENNA_PHASE_CENTER_Y,
        ON_BOARD_ANTENNA_PHASE_CENTER_Z,
        ON_BOARD_CLOCK_OFFSET,
        ON_BOARD_CLOCK_OFFSET_MIN,
        ON_BOARD_CLOCK_OFFSET_MAX,
        ON_BOARD_CLOCK_OFFSET_ESTIMATED,
        GROUND_STATION_NAME,
        GROUND_STATION_LATITUDE,
        GROUND_STATION_LONGITUDE,
        GROUND_STATION_ALTITUDE,
        GROUND_STATION_POSITION_ESTIMATED,
        GROUND_STATION_CLOCK_OFFSET,
        GROUND_STATION_CLOCK_OFFSET_MIN,
        GROUND_STATION_CLOCK_OFFSET_MAX,
        GROUND_STATION_CLOCK_OFFSET_ESTIMATED,
        GROUND_STATION_TROPOSPHERIC_MODEL_ESTIMATED,
        GROUND_STATION_GLOBAL_MAPPING_FUNCTION,
        GROUND_STATION_NIELL_MAPPING_FUNCTION,
        GROUND_STATION_WEATHER_ESTIMATED,
        GROUND_STATION_TROPOSPHERIC_ZENITH_DELAY,
        GROUND_STATION_TROPOSPHERIC_DELAY_ESTIMATED,
        GROUND_STATION_RANGE_SIGMA,
        GROUND_STATION_RANGE_BIAS,
        GROUND_STATION_RANGE_BIAS_MIN,
        GROUND_STATION_RANGE_BIAS_MAX,
        GROUND_STATION_RANGE_BIAS_ESTIMATED,
        GROUND_STATION_RANGE_RATE_SIGMA,
        GROUND_STATION_RANGE_RATE_BIAS,
        GROUND_STATION_RANGE_RATE_BIAS_MIN,
        GROUND_STATION_RANGE_RATE_BIAS_MAX,
        GROUND_STATION_RANGE_RATE_BIAS_ESTIMATED,
        GROUND_STATION_AZIMUTH_SIGMA,
        GROUND_STATION_AZIMUTH_BIAS,
        GROUND_STATION_AZIMUTH_BIAS_MIN,
        GROUND_STATION_AZIMUTH_BIAS_MAX,
        GROUND_STATION_ELEVATION_SIGMA,
        GROUND_STATION_ELEVATION_BIAS,
        GROUND_STATION_ELEVATION_BIAS_MIN,
        GROUND_STATION_ELEVATION_BIAS_MAX,
        GROUND_STATION_AZ_EL_BIASES_ESTIMATED,
        GROUND_STATION_ELEVATION_REFRACTION_CORRECTION,
        GROUND_STATION_RANGE_TROPOSPHERIC_CORRECTION,
        GROUND_STATION_IONOSPHERIC_CORRECTION,
        SOLID_TIDES_DISPLACEMENT_CORRECTION,
        SOLID_TIDES_DISPLACEMENT_REMOVE_PERMANENT_DEFORMATION,
        OCEAN_LOADING_CORRECTION,
        RANGE_MEASUREMENTS_BASE_WEIGHT,
        RANGE_RATE_MEASUREMENTS_BASE_WEIGHT,
        AZIMUTH_MEASUREMENTS_BASE_WEIGHT,
        ELEVATION_MEASUREMENTS_BASE_WEIGHT,
        PV_MEASUREMENTS_BASE_WEIGHT,
        PV_MEASUREMENTS_POSITION_SIGMA,
        PV_MEASUREMENTS_VELOCITY_SIGMA,
        RANGE_OUTLIER_REJECTION_MULTIPLIER,
        RANGE_OUTLIER_REJECTION_STARTING_ITERATION,
        RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER,
        RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION,
        AZ_EL_OUTLIER_REJECTION_MULTIPLIER,
        AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION,
        PV_OUTLIER_REJECTION_MULTIPLIER,
        PV_OUTLIER_REJECTION_STARTING_ITERATION,
        SATELLITE_ID_IN_RINEX_FILES,
        MEASUREMENTS_FILES,
        OUTPUT_BASE_NAME,
        ESTIMATOR_OPTIMIZATION_ENGINE,
        ESTIMATOR_LEVENBERG_MARQUARDT_INITIAL_STEP_BOUND_FACTOR,
        ESTIMATOR_ORBITAL_PARAMETERS_POSITION_SCALE,
        ESTIMATOR_NORMALIZED_PARAMETERS_CONVERGENCE_THRESHOLD,
        ESTIMATOR_MAX_ITERATIONS,
        ESTIMATOR_MAX_EVALUATIONS;
    }

}
