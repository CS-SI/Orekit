/* Copyright 2002-2020 CS GROUP
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.NoSuchElementException;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultBatchLeastSquares;
import org.orekit.files.sp3.SP3File;
import org.orekit.files.sp3.SP3File.SP3Ephemeris;
import org.orekit.files.sp3.SP3Parser;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class TLEOrbitDeterminationTest extends AbstractOrbitDetermination<TLEPropagatorBuilder> {
    
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
                                                            final double positionScale, final boolean estimateOrbit) {
        return new TLEPropagatorBuilder(templateTLE, PositionAngle.MEAN,
                                         positionScale, estimateOrbit);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final TLEPropagatorBuilder propagatorBuilder,
                                final double mass) {
        
     // TLE OD does not need to set mass
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setGravity(final TLEPropagatorBuilder propagatorBuilder,
                                           final OneAxisEllipsoid body) {

        return new ParameterDriver[0];

    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setOceanTides(final TLEPropagatorBuilder propagatorBuilder,
                                              final IERSConventions conventions,
                                              final OneAxisEllipsoid body,
                                              final int degree, final int order) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Ocean tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setSolidTides(final TLEPropagatorBuilder propagatorBuilder,
                                              final IERSConventions conventions,
                                              final OneAxisEllipsoid body,
                                              final CelestialBody[] solidTidesBodies) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "Solid tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setThirdBody(final TLEPropagatorBuilder propagatorBuilder,
                                             final CelestialBody thirdBody) {
        
        return new ParameterDriver[0];
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setDrag(final TLEPropagatorBuilder propagatorBuilder,
                                        final Atmosphere atmosphere, final DragSensitive spacecraft) {

        return new ParameterDriver[0];
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setSolarRadiationPressure(final TLEPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                          final double equatorialRadius, final RadiationSensitive spacecraft) {

        return new ParameterDriver[0];
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setRelativity(final TLEPropagatorBuilder propagatorBuilder) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Relativity not implemented in TLE Propagator");
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver[] setPolynomialAcceleration(final TLEPropagatorBuilder propagatorBuilder,
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

    /*
   
    @Test
    // Orbit determination using only mean elements for Lageos2 based on SLR (range) measurements
    // For better accuracy, adding short period terms is necessary
    public void testLageos2()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {
        
        final File home       = new File(System.getProperty("user.home"));
        final File orekitData = new File(home, "../home/thomas/orekit-data");
        DataContext.
        getDefault().
        getDataProvidersManager().
        addProvider(new DirectoryCrawler(orekitData));
        
        // initiate TLE
        final String line1 = "1 22195U 92070B   16045.51027931 -.00000009  00000-0  00000+0 0  9990";
        final String line2 = "2 22195  52.6508 132.9147 0137738 336.2706   1.6348  6.47294052551192";
        templateTLE = new TLE(line1, line2);
        templateTLE.getParametersDrivers()[0].setSelected(false);

        // input in resources directory
        final String inputPath = TLEOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/tle_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
        
        testEphemGen();

        //orbit determination run.
        ResultBatchLeastSquares odLageos2 = runBLS(input, false);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 116;
        final double velocityAccuracy = 1.4;

        //test on the convergence
        final int numberOfIte  = 5;
        final int numberOfEval = 5;

        Assert.assertEquals(numberOfIte, odLageos2.getNumberOfIteration());
        Assert.assertEquals(numberOfEval, odLageos2.getNumberOfEvaluation());

        //test on the estimated position and velocity
        TimeStampedPVCoordinates odPV = odLageos2.getEstimatedPV();


        //final Vector3D refPos = new Vector3D(-5532124.989973327, 10025700.01763335, -3578940.840115321);
        //final Vector3D refVel = new Vector3D(-3871.2736402553, -607.8775965705, 4280.9744110925);

        
        final Vector3D refPos = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        
        // calculate first guess and final error with respect to reference Position
        final Transform temeToEme2000 = FramesFactory.getTEME().getTransformTo(FramesFactory.getEME2000(), odPV.getDate());
        odPV = temeToEme2000.transformPVCoordinates(odPV);
        final Vector3D estimatedPos = odPV.getPosition();
        final Vector3D estimatedVel = odPV.getVelocity();
        TimeStampedPVCoordinates firstGuessPV = TLEPropagator.selectExtrapolator(templateTLE).getInitialState().getPVCoordinates();
        firstGuessPV = temeToEme2000.transformPVCoordinates(firstGuessPV);
        final double firstGuessError = Vector3D.distance(firstGuessPV.getPosition(), refPos);
        final double finalError = Vector3D.distance(estimatedPos, refPos);
        System.out.println("      ");
        System.out.println("------------------ First Guess Error with respect to Reference position---------------- ");
        System.out.format("%n Position Error = %f (m)%n   ", firstGuessError);
        System.out.println("      ");
        System.out.println("--------------------- Final Error with respect to Reference position------------------- ");
        System.out.format("%n Position Error = %f (m)%n%n", finalError);
        
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        //final double[] RefStatRange = { -2.795816, 6.171529, 0.310848, 1.657809 };
        final double[] RefStatRange = { -295.1214, 307.5015, 0.038483, 117.8985 };
        Assert.assertEquals(nbRange, odLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], odLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], odLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], odLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }
    
    */
    
    @Test
    // Orbit determination using only mean elements for GNSS satellite based on range measurements
    // For better accuracy, adding short period terms is necessary
    public void testGNSS()
        throws URISyntaxException, IllegalArgumentException, IOException,
               OrekitException, ParseException {
        
        final File home       = new File(System.getProperty("user.home"));
        final File orekitData = new File(home, "../home/thomas/orekit-data");
        DataContext.
        getDefault().
        getDataProvidersManager().
        addProvider(new DirectoryCrawler(orekitData));
        
        // initiate TLE
        final String line1 = "1 32711U 08012A   16044.40566026 -.00000039  00000-0  00000+0 0  9991";
        final String line2 = "2 32711  55.4362 301.3402 0091577 207.7302 151.8353  2.00563580 58013";
        templateTLE = new TLE(line1, line2);
        templateTLE.getParametersDrivers()[0].setSelected(false);

        // input in resources directory
        final String inputPath = TLEOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/analytical/tle_od_test_GPS.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        //orbit determination run.
        ResultBatchLeastSquares odGNSS = runBLS(input, true, true);

        //test
        //definition of the accuracy for the test
        final double distanceAccuracy = 59;
        final double velocityAccuracy = 0.23;

        //test on the convergence
        final int numberOfIte  = 1;
        final int numberOfEval = 14;

        //Assert.assertEquals(numberOfIte, odGNSS.getNumberOfIteration());
        //Assert.assertEquals(numberOfEval, odGNSS.getNumberOfEvaluation());
        
        //test on the estimated position and velocity (reference from IGS-MGEX file com18836.sp3)
        TimeStampedPVCoordinates odPV = odGNSS.getEstimatedPV();
        final Transform transform = FramesFactory.getTEME().getTransformTo(FramesFactory.getGCRF(), odPV.getDate());
        odPV = transform.transformPVCoordinates(odPV);
        final Vector3D estimatedPos = odPV.getPosition();
        final Vector3D estimatedVel = odPV.getVelocity();
         
        // create reference position from GPS ephemris
        final String ex = "/sp3/esa18836.sp3";
        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);
        SP3Ephemeris ephemeris = file.getSatellites().get("G07");
        BoundedPropagator propagator = ephemeris.getPropagator();
        final TimeStampedPVCoordinates ephem = propagator.propagate(odPV.getDate()).getPVCoordinates();
        final Vector3D refPos = ephem.getPosition();
        final Vector3D refVel = ephem.getVelocity();
        
        //Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        //Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), velocityAccuracy);
        
        // calculate first guess and final error with respect to reference Position
        TimeStampedPVCoordinates firstGuessPV = TLEPropagator.selectExtrapolator(templateTLE).getInitialState().getPVCoordinates();
        firstGuessPV = transform.transformPVCoordinates(firstGuessPV);
        final double firstGuessError = Vector3D.distance(firstGuessPV.getPosition(), refPos);
        final double finalError = Vector3D.distance(estimatedPos, refPos);
        final double displacement = Vector3D.distance(firstGuessPV.getPosition(), estimatedPos);
        
        System.out.println("      ");
        System.out.println("------------------ First Guess Error with respect to Reference position---------------- ");
        System.out.format("%n Position Error = %f (m)%n   ", firstGuessError);
        System.out.println("      ");
        System.out.println("--------------------- Final Error with respect to Reference position------------------- ");
        System.out.format("%n Position Error = %f (m)%n%n", finalError);
        System.out.println("-------------------- Final Error with respect to First Guess position------------------ ");
        System.out.format("%n Position displacement = %f (m)%n%n", displacement);
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), distanceAccuracy);
        
        //test on statistic for the range residuals
        final long nbRange = 4009;
        final double[] RefStatRange = { -83.945, 59.365, 0.0, 20.857 };
        Assert.assertEquals(nbRange, odGNSS.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], odGNSS.getRangeStat().getMin(),               1.0e-3);
        Assert.assertEquals(RefStatRange[1], odGNSS.getRangeStat().getMax(),               1.0e-3);
        Assert.assertEquals(RefStatRange[2], odGNSS.getRangeStat().getMean(),              0.23);
        Assert.assertEquals(RefStatRange[3], odGNSS.getRangeStat().getStandardDeviation(), 1.0e-3);
       
    }
    
    /*
    private void testEphemGen() throws OrekitException, IOException {
        
        final AbsoluteDate dateRef = new AbsoluteDate(2016, 02, 13, 02, 31, 30, TimeScalesFactory.getUTC());
        
        final String ex = "/sp3/esa18836.sp3";
        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);

        final Transform eme2000toGCRF = FramesFactory.getEME2000().getTransformTo(FramesFactory.getGCRF(), dateRef);
        Vector3D refPos = new Vector3D(-2747606.680868164, 22572091.30648564, 13522761.402325712);
        Vector3D refVel = new Vector3D(-2729.5151218788005, 1142.6629459030657, -2523.9055974487947);
        
        refPos = eme2000toGCRF.transformPosition(refPos);

        TimeScale gps = TimeScalesFactory.getGPS();
        TimeScale utc = TimeScalesFactory.getUTC();
        final double dtgps = gps.offsetFromTAI(dateRef);
        final double dtutc = utc.offsetFromTAI(dateRef);
        final double dt = dtutc - dtgps;

        // action
        final SP3File file = parser.parse(inEntry);

        // verify
        SP3Ephemeris ephemeris = file.getSatellites().get("G07");
        BoundedPropagator propagator = ephemeris.getPropagator();
        final TimeStampedPVCoordinates ephem = propagator.propagate(dateRef).getPVCoordinates();
        final Vector3D ephemPos = ephem.getPosition();
        final double dist = Vector3D.distance(ephemPos, refPos);

        System.out.println("-----------------------------");
        System.out.println(dateRef);
        System.out.println(propagator.getFrame());
        System.out.println(ephem.getDate());
        System.out.println("-----------------------------");
        System.out.println(dist);
        System.out.println("-----------------------------");
        
    }
    */
}
