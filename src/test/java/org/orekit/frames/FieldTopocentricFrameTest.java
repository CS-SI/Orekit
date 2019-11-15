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
package org.orekit.frames;



import java.io.IOException;

import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;


public class FieldTopocentricFrameTest {

    // Computation date
    private FieldAbsoluteDate<Decimal64> date;

    // Reference frame = ITRF
    private Frame itrf;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Body mu
    private Decimal64 mu;
    
    // Field
    private Field<Decimal64> field;


    @Test
    public void testZero() {

        final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(0),
        		                                                             new Decimal64(0),
        		                                                             new Decimal64(0));
        final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "zero");

        // Check that frame directions are aligned
        final Decimal64 xDiff = FieldVector3D.dotProduct(topoFrame.getEast(), FieldVector3D.getPlusJ(field));
        final Decimal64 yDiff = FieldVector3D.dotProduct(topoFrame.getNorth(),FieldVector3D.getPlusK(field));
        final Decimal64 zDiff = FieldVector3D.dotProduct(topoFrame.getZenith(),FieldVector3D.getPlusI(field));
        Assert.assertEquals(1., xDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(1., yDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(1., zDiff.getReal(), Utils.epsilonTest);
   }

    @Test
    public void testPole() {
    	
        final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(FastMath.PI/2),
        		                                                             new Decimal64(0),
        		                                                             new Decimal64(0));
        final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "north pole");

        // Check that frame directions are aligned
        final Decimal64 xDiff = FieldVector3D.dotProduct(topoFrame.getEast(), FieldVector3D.getPlusJ(field));
        final Decimal64 yDiff = FieldVector3D.dotProduct(topoFrame.getSouth(), FieldVector3D.getPlusI(field));
        final Decimal64 zDiff = FieldVector3D.dotProduct(topoFrame.getZenith(), FieldVector3D.getPlusK(field));
        Assert.assertEquals(1., xDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(1., yDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(1., zDiff.getReal(), Utils.epsilonTest);
   }

    @Test
    public void testNormalLatitudes() {

        // First point at latitude 45°
        final FieldGeodeticPoint<Decimal64> point1 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
        									                                  new Decimal64(FastMath.toRadians(30.)),
        									                                  new Decimal64(0));
        final FieldTopocentricFrame<Decimal64> topoFrame1 = new FieldTopocentricFrame<>(earthSpheric, point1, "lat 45");

        // Second point at latitude -45° and same longitude
        final FieldGeodeticPoint<Decimal64> point2 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(-45.)),
        		                                                              new Decimal64(FastMath.toRadians(30.)),
                                                                              new Decimal64(0));
        final FieldTopocentricFrame<Decimal64> topoFrame2 = new FieldTopocentricFrame<>(earthSpheric, point2, "lat -45");

        // Check that frame North and Zenith directions are all normal to each other, and East are the same
        final Decimal64 xDiff = FieldVector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getEast());
        final Decimal64 yDiff = FieldVector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final Decimal64 zDiff = FieldVector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(0., yDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(0., zDiff.getReal(), Utils.epsilonTest);
  }

    @Test
    public void testOppositeLongitudes() {

        // First point at latitude 45°
    	final FieldGeodeticPoint<Decimal64> point1 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
                                                                              new Decimal64(FastMath.toRadians(30.)),
                                                                              new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame1 = new FieldTopocentricFrame<>(earthSpheric, point1, "lat 45");
        final FieldGeodeticPoint<Decimal64> p1 = topoFrame1.getPoint();
        Assert.assertEquals(point1.getLatitude().getReal(), p1.getLatitude().getReal(), 1.0e-15);
        Assert.assertEquals(point1.getLongitude().getReal(), p1.getLongitude().getReal(), 1.0e-15);
        Assert.assertEquals(point1.getAltitude().getReal(), p1.getAltitude().getReal(), 1.0e-15);

        // Second point at latitude -45° and same longitude
        final FieldGeodeticPoint<Decimal64> point2 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
        		                                                              new Decimal64(FastMath.toRadians(210.)),
                                                                              new Decimal64(0));
        final FieldTopocentricFrame<Decimal64> topoFrame2 = new FieldTopocentricFrame<>(earthSpheric, point2, "lon 210");

        // Check that frame North and Zenith directions are all normal to each other,
        // and East of the one is West of the other
        final Decimal64 xDiff = FieldVector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final Decimal64 yDiff = FieldVector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final Decimal64 zDiff = FieldVector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(0., yDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(0., zDiff.getReal(), Utils.epsilonTest);
  }

    @Test
    public void testAntipodes()
        {

        // First point at latitude 45° and longitude 30
    	final FieldGeodeticPoint<Decimal64> point1 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
                                                                              new Decimal64(FastMath.toRadians(30.)),
                                                                              new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame1 = new FieldTopocentricFrame<>(earthSpheric, point1, "lon 30");

        // Second point at latitude -45° and longitude 210
    	final FieldGeodeticPoint<Decimal64> point2 = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(-45.)),
                                                                              new Decimal64(FastMath.toRadians(210.)),
                                                                              new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame2 = new FieldTopocentricFrame<>(earthSpheric, point2, "lon 210");

        // Check that frame Zenith directions are opposite to each other,
        // and East and North are the same
    	final Decimal64 xDiff = FieldVector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final Decimal64 yDiff = FieldVector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final Decimal64 zDiff = FieldVector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(1., yDiff.getReal(), Utils.epsilonTest);
        Assert.assertEquals(-1., zDiff.getReal(), Utils.epsilonTest);

        Assert.assertEquals(1, FieldVector3D.dotProduct(topoFrame1.getNadir(), topoFrame2.getZenith()).getReal(), Utils.epsilonTest);
        Assert.assertEquals(1, FieldVector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getNadir()).getReal(), Utils.epsilonTest);

    }

    @Test
    public void testSiteAtZenith()
        {

        // Surface point at latitude 45°
    	final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
                                                                              new Decimal64(FastMath.toRadians(30.)),
                                                                              new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final FieldGeodeticPoint<Decimal64> satPoint = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
                                                                                new Decimal64(FastMath.toRadians(30.)),
                                                                                new Decimal64(800000.));

        // Zenith point elevation = 90 deg
        final Decimal64 site = topoFrame.getElevation(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., site.getReal(), Utils.epsilonAngle);

        // Zenith point range = defined altitude
        final Decimal64 range = topoFrame.getRange(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(800000., range.getReal(), 1e-8);
    }

    @Test
    public void testAzimuthEquatorial()
        {

        // Surface point at latitude 0
    	final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(0.)),
    			                                                             new Decimal64(FastMath.toRadians(30.)),
    			                                                             new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
    	FieldGeodeticPoint<Decimal64> infPoint = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(0.)),
                                                                                new Decimal64(FastMath.toRadians(50.)),
                                                                                new Decimal64(1000000000));

        // Azimuth = pi/2
    	Decimal64 azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., azi.getReal(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        Decimal64 site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude().subtract(infPoint.getLongitude()).getReal()), site.getReal(), 1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(0.)),
                                            new Decimal64(FastMath.toRadians(10.)),
                                            new Decimal64(1000000000));

        // Azimuth = pi/2
        azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(3*FastMath.PI/2., azi.getReal(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude().subtract(infPoint.getLongitude()).getReal()), site.getReal(), 1.e-2);

    }

   
    @Test
    public void testAzimuthPole()
        {

        // Surface point at latitude 0
    	final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(89.999)),
                                                                             new Decimal64(0),
                                                                             new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
    	FieldGeodeticPoint<Decimal64> satPoint = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(29.)),
                                                                          new Decimal64(FastMath.toRadians(30.)),
                                                                          new Decimal64(800000.));

        // Azimuth =
    	Decimal64 azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude().getReal(), azi.getReal(), 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(28.)),
                                            new Decimal64(FastMath.toRadians(-30.)),
                                            new Decimal64(800000.));

        // Azimuth =
        azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude().getReal(), azi.getReal(), 1.e-5);

    }

    @Test
    public void testDoppler()
        {

    	final Decimal64 zero = field.getZero();
    	
        // Surface point at latitude 45, longitude 5
    	final FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(new Decimal64(FastMath.toRadians(45.)),
    			                                                             new Decimal64(FastMath.toRadians(5.)),
    			                                                             new Decimal64(0));
    	final FieldTopocentricFrame<Decimal64> topoFrame = new FieldTopocentricFrame<>(earthSpheric, point, "lon 5 lat 45");

        // Point at 30 deg longitude
        // ***************************
        final FieldCircularOrbit<Decimal64> orbit =
                new FieldCircularOrbit<>(zero.add(7178000.0), zero.add(0.5e-8), zero.add(-0.5e-8),
                                         zero.add(FastMath.toRadians(50.)), zero.add(FastMath.toRadians(120.)),
                                         zero.add(FastMath.toRadians(90.)), PositionAngle.MEAN,
                                         FramesFactory.getEME2000(), date, zero.add(mu));

        // Transform satellite position to position/velocity parameters in body frame
        final FieldTransform<Decimal64> eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), date);
        final FieldPVCoordinates<Decimal64> pvSatItrf = eme2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute range rate directly
        //********************************************
        final Decimal64 dop = topoFrame.getRangeRate(pvSatItrf, earthSpheric.getBodyFrame(), date);

        // Compare to finite difference computation (2 points)
        //*****************************************************
        final double dt = 0.1;
        FieldKeplerianPropagator<Decimal64> extrapolator = new FieldKeplerianPropagator<>(orbit);

        // Extrapolate satellite position a short while after reference date
        FieldAbsoluteDate<Decimal64> dateP = date.shiftedBy(dt);
        FieldTransform<Decimal64> j2000ToItrfP = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateP);
        FieldSpacecraftState<Decimal64> orbitP = extrapolator.propagate(dateP);
        FieldVector3D<Decimal64> satPointGeoP = j2000ToItrfP.transformPVCoordinates(orbitP.getPVCoordinates()).getPosition();

        // Retropolate satellite position a short while before reference date
        FieldAbsoluteDate<Decimal64> dateM = date.shiftedBy(-dt);
        FieldTransform<Decimal64> j2000ToItrfM = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateM);
        FieldSpacecraftState<Decimal64> orbitM = extrapolator.propagate(dateM);
        FieldVector3D<Decimal64> satPointGeoM = j2000ToItrfM.transformPVCoordinates(orbitM.getPVCoordinates()).getPosition();

        // Compute ranges at both instants
        Decimal64 rangeP = topoFrame.getRange(satPointGeoP, earthSpheric.getBodyFrame(), dateP);
        Decimal64 rangeM = topoFrame.getRange(satPointGeoM, earthSpheric.getBodyFrame(), dateM);
        final Decimal64 dopRef2 = (rangeP.subtract(rangeM)).divide(2. * dt);
        Assert.assertEquals(dopRef2.getReal(), dop.getReal(), 1.e-3);

    }

    @Test
    public void testEllipticEarth()  {

    	final Decimal64 zero = field.getZero();
    	
        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final FieldGeodeticPoint<Decimal64> satPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(30.)),
        		                                                                   zero.add(FastMath.toRadians(15.)),
        		                                                                   zero.add(800000.));
        final FieldVector3D<Decimal64> satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                                                       zero.add(FastMath.toRadians(5.)),
                                                                       zero.add(800000.));
        FieldTopocentricFrame<Decimal64> topoElliptic = new FieldTopocentricFrame<>(earthElliptic, point, "elliptic, equatorial lon 5");
        FieldTopocentricFrame<Decimal64> topoSpheric = new FieldTopocentricFrame<>(earthSpheric, point, "spheric, equatorial lon 5");

        // Compare azimuth/elevation/range of satellite point : shall be strictly identical
        // ***************************************************
        Decimal64 aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        Decimal64 aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli.getReal(), aziSphe.getReal(), Utils.epsilonAngle);

        Decimal64 eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        Decimal64 eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli.getReal(), eleSphe.getReal(), Utils.epsilonAngle);

        Decimal64 disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        Decimal64 disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli.getReal(), disSphe.getReal(), Utils.epsilonTest);

        // Infinite point separated by -20 deg in longitude
        // *************************************************
        FieldGeodeticPoint<Decimal64> infPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                                                             zero.add(FastMath.toRadians(-15.)),
                                                                             zero.add(1000000000.));
        FieldVector3D<Decimal64>  infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(3*FastMath.PI/2., aziElli.getReal(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude().subtract(infPointGeo.getLongitude()).getReal()), eleElli.getReal(), 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                               zero.add(FastMath.toRadians(25.)),
                                               zero.add(1000000000.));
        infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., aziElli.getReal(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude().subtract(infPointGeo.getLongitude()).getReal()), eleElli.getReal(), 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(89.999)), zero, zero);
        topoSpheric  = new FieldTopocentricFrame<>(earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new FieldTopocentricFrame<>(earthElliptic, point, "lon 0 lat 90");

        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli.getReal(), aziSphe.getReal(), 1.e-7);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli.getReal(), eleSphe.getReal(), 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli.getReal(), disSphe.getReal(), 20.e+3);

        // *********************
        // Test at any position
        // *********************
        point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(60)), zero.add(FastMath.toRadians(30.)), zero);
        topoSpheric  = new FieldTopocentricFrame<>(earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new FieldTopocentricFrame<>(earthElliptic, point, "lon 10 lat 45");

        // Compare azimuth/elevation/range of satellite point : slight difference
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli.getReal(), aziSphe.getReal(), 1.e-2);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli.getReal(), eleSphe.getReal(), 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli.getReal(), disSphe.getReal(), 20.e+3);

    }
    
    @Test
    public void testPointAtDistance() {

    	Decimal64 zero = field.getZero();
    	
        RandomGenerator random = new Well1024a(0xa1e6bd5cd0578779l);
        final OneAxisEllipsoid earth =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                 itrf);
        final FieldAbsoluteDate<Decimal64> date = FieldAbsoluteDate.getJ2000Epoch(field);

        for (int i = 0; i < 20; ++i) {
            // we don't need uniform point on the sphere, just a few different test configurations
        	Decimal64 latitude  = zero.add(FastMath.PI * (0.5 - random.nextDouble()));
        	Decimal64 longitude = zero.add(2 * FastMath.PI * random.nextDouble());
            FieldTopocentricFrame<Decimal64> topo = new FieldTopocentricFrame<>(earth,
                                                         new FieldGeodeticPoint<>(latitude, longitude, zero),
                                                         "topo");
            FieldTransform<Decimal64> transform = earth.getBodyFrame().getTransformTo(topo, date);
            for (int j = 0; j < 20; ++j) {
            	Decimal64 elevation      = zero.add(FastMath.PI * (0.5 - random.nextDouble()));
            	Decimal64 azimuth        = zero.add(2 * FastMath.PI * random.nextDouble());
            	Decimal64 range          = zero.add(500000.0 * (1.0 + random.nextDouble()));
                FieldVector3D<Decimal64> absolutePoint = earth.transform(topo.pointAtDistance(azimuth, elevation, range));
                FieldVector3D<Decimal64> relativePoint = transform.transformPosition(absolutePoint);
                Decimal64 rebuiltElevation = topo.getElevation(relativePoint, topo, FieldAbsoluteDate.getJ2000Epoch(field));
                Decimal64 rebuiltAzimuth   = topo.getAzimuth(relativePoint, topo, FieldAbsoluteDate.getJ2000Epoch(field));
                Decimal64 rebuiltRange     = topo.getRange(relativePoint, topo, FieldAbsoluteDate.getJ2000Epoch(field));
                Assert.assertEquals(elevation.getReal(), rebuiltElevation.getReal(), 1.0e-12);
                Assert.assertEquals(azimuth.getReal(), MathUtils.normalizeAngle(rebuiltAzimuth.getReal(), azimuth.getReal()), 1.0e-12);
                Assert.assertEquals(range.getReal(), rebuiltRange.getReal(), 1.0e-12 * range.getReal());
            }
        }
    }

    @Test
    public void testIssue145() {
    	Decimal64 zero = field.getZero();
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               itrf);
        FieldGeodeticPoint<Decimal64> point = new FieldGeodeticPoint<>(zero, zero, zero);
        FieldTopocentricFrame<Decimal64> staFrame = new FieldTopocentricFrame<>(earth, point, "test");
        FieldGeodeticPoint<Decimal64> gp = staFrame.computeLimitVisibilityPoint(zero.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000),
                                                                zero, zero.add(FastMath.toRadians(5.0)));
        Assert.assertEquals(0.0, gp.getLongitude().getReal(), 1.0e-15);
        Assert.assertTrue(gp.getLatitude().getReal() > 0);
        Assert.assertEquals(0.0, staFrame.getNorth().distance(FieldVector3D.getPlusK(field)).getReal(), 1.0e-15);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Elliptic earth shape
            earthSpheric = new OneAxisEllipsoid(6378136.460, 0., itrf);

            
            field = Decimal64Field.getInstance();
            
            // Reference date
            date = new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC()));

            // Body mu
            mu = new Decimal64(3.9860047e14);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @Test
    public void testVisibilityCircle() throws IOException {

        // a few random from International Laser Ranging Service
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame[] ilrs = {
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(52.3800), FastMath.toRadians(3.0649), 133.745),
                                 "Potsdam"),
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(36.46273), FastMath.toRadians(-6.20619), 64.0),
                                 "San Fernando"),
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(35.5331), FastMath.toRadians(24.0705), 157.0),
                                 "Chania")
        };

        PolynomialFunction distanceModel =
                new PolynomialFunction(new double[] { 7.0892e+05, 3.1913, -8.2181e-07, 1.4033e-13 });
        for (TopocentricFrame station : ilrs) {
            for (double altitude = 500000; altitude < 2000000; altitude += 100000) {
                for (double azimuth = 0; azimuth < 2 * FastMath.PI; azimuth += 0.05) {
                    GeodeticPoint p = station.computeLimitVisibilityPoint(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + altitude,
                                                                          azimuth, FastMath.toRadians(5.0));
                    double d = station.getRange(earth.transform(p), earth.getBodyFrame(), AbsoluteDate.J2000_EPOCH);
                    Assert.assertEquals(distanceModel.value(altitude), d, 40000.0);
                }
            }
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        earthSpheric = null;
        field = null;
    }


}
