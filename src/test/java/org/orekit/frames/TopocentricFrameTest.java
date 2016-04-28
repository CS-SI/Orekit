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
package org.orekit.frames;



import java.io.IOException;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class TopocentricFrameTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
    private Frame itrf;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Body mu
    private double mu;


    @Test
    public void testZero() {

        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "zero");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.PLUS_K);
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_I);
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
   }

    @Test
    public void testPole() {

        final GeodeticPoint point = new GeodeticPoint(FastMath.PI/2., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "north pole");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.PLUS_I.negate());
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_K);
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
   }

    @Test
    public void testNormalLatitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lat 45");

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(-45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lat -45");

        // Check that frame North and Zenith directions are all normal to each other, and East are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getEast());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(0., yDiff, Utils.epsilonTest);
        Assert.assertEquals(0., zDiff, Utils.epsilonTest);
  }

    @Test
    public void testOppositeLongitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");
        final GeodeticPoint p1 = topoFrame1.getPoint();
        Assert.assertEquals(point1.getLatitude(), p1.getLatitude(), 1.0e-15);
        Assert.assertEquals(point1.getLongitude(), p1.getLongitude(), 1.0e-15);
        Assert.assertEquals(point1.getAltitude(), p1.getAltitude(), 1.0e-15);

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");

        // Check that frame North and Zenith directions are all normal to each other,
        // and East of the one is West of the other
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(0., yDiff, Utils.epsilonTest);
        Assert.assertEquals(0., zDiff, Utils.epsilonTest);
  }

    @Test
    public void testAntipodes()
        throws OrekitException {

        // First point at latitude 45° and longitude 30
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");

        // Second point at latitude -45° and longitude 210
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(-45.), FastMath.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");

        // Check that frame Zenith directions are opposite to each other,
        // and East and North are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(-1., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testSiteAtZenith()
        throws OrekitException {

        // Surface point at latitude 45°
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final GeodeticPoint satPoint = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 800000.);

        // Zenith point elevation = 90 deg
        final double site = topoFrame.getElevation(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., site, Utils.epsilonAngle);

        // Zenith point range = defined altitude
        final double range = topoFrame.getRange(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(800000., range, 1e-8);
  }

    @Test
    public void testAzimuthEquatorial()
        throws OrekitException {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        GeodeticPoint infPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(50.), 1000000000.);

        // Azimuth = pi/2
        double azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        double site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPoint.getLongitude()), site, 1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(10.), 1000000000.);

        // Azimuth = pi/2
        azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(3*FastMath.PI/2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPoint.getLongitude()), site, 1.e-2);

    }

    @Test
    public void testAzimuthPole()
        throws OrekitException {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        GeodeticPoint satPoint = new GeodeticPoint(FastMath.toRadians(28.), FastMath.toRadians(30.), 800000.);

        // Azimuth =
        double azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude(), azi, 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new GeodeticPoint(FastMath.toRadians(28.), FastMath.toRadians(-30.), 800000.);

        // Azimuth =
        azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude(), azi, 1.e-5);

    }

    @Test
    public void testDoppler()
        throws OrekitException {

        // Surface point at latitude 45, longitude 5
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");

        // Point at 30 deg longitude
        // ***************************
        final CircularOrbit orbit =
            new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, FastMath.toRadians(50.), FastMath.toRadians(120.),
                                   FastMath.toRadians(90.), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in body frame
        final Transform eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), date);
        final PVCoordinates pvSatItrf = eme2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute range rate directly
        //********************************************
        final double dop = topoFrame.getRangeRate(pvSatItrf, earthSpheric.getBodyFrame(), date);

        // Compare to finite difference computation (2 points)
        //*****************************************************
        final double dt = 0.1;
        KeplerianPropagator extrapolator = new KeplerianPropagator(orbit);

        // Extrapolate satellite position a short while after reference date
        AbsoluteDate dateP = date.shiftedBy(dt);
        Transform j2000ToItrfP = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateP);
        SpacecraftState orbitP = extrapolator.propagate(dateP);
        Vector3D satPointGeoP = j2000ToItrfP.transformPVCoordinates(orbitP.getPVCoordinates()).getPosition();

        // Retropolate satellite position a short while before reference date
        AbsoluteDate dateM = date.shiftedBy(-dt);
        Transform j2000ToItrfM = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateM);
        SpacecraftState orbitM = extrapolator.propagate(dateM);
        Vector3D satPointGeoM = j2000ToItrfM.transformPVCoordinates(orbitM.getPVCoordinates()).getPosition();

        // Compute ranges at both instants
        double rangeP = topoFrame.getRange(satPointGeoP, earthSpheric.getBodyFrame(), dateP);
        double rangeM = topoFrame.getRange(satPointGeoM, earthSpheric.getBodyFrame(), dateM);
        final double dopRef2 = (rangeP - rangeM) / (2. * dt);
        Assert.assertEquals(dopRef2, dop, 1.e-3);

    }

    @Test
    public void testEllipticEarth()  throws OrekitException {

        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final GeodeticPoint satPointGeo = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(15.), 800000.);
        final Vector3D satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(5.), 0.);
        TopocentricFrame topoElliptic = new TopocentricFrame(earthElliptic, point, "elliptic, equatorial lon 5");
        TopocentricFrame topoSpheric = new TopocentricFrame(earthSpheric, point, "spheric, equatorial lon 5");

        // Compare azimuth/elevation/range of satellite point : shall be strictly identical
        // ***************************************************
        double aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        double aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli, aziSphe, Utils.epsilonAngle);

        double eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        double eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli, eleSphe, Utils.epsilonAngle);

        double disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        double disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli, disSphe, Utils.epsilonTest);

        // Infinite point separated by -20 deg in longitude
        // *************************************************
        GeodeticPoint infPointGeo = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(-15.), 1000000000.);
        Vector3D infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(3*FastMath.PI/2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPointGeo.getLongitude()), eleElli, 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(25.), 1000000000.);
        infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        Assert.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPointGeo.getLongitude()), eleElli, 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 0 lat 90");

        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli, aziSphe, 1.e-7);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli, eleSphe, 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli, disSphe, 20.e+3);


        // *********************
        // Test at any position
        // *********************
        point = new GeodeticPoint(FastMath.toRadians(60), FastMath.toRadians(30.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 10 lat 45");

        // Compare azimuth/elevation/range of satellite point : slight difference
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(aziElli, aziSphe, 1.e-2);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(eleElli, eleSphe, 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        Assert.assertEquals(disElli, disSphe, 20.e+3);

    }

    @Test
    public void testPointAtDistance() throws OrekitException {

        RandomGenerator random = new Well1024a(0xa1e6bd5cd0578779l);
        final OneAxisEllipsoid earth =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                 itrf);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        for (int i = 0; i < 20; ++i) {
            // we don't need uniform point on the sphere, just a few different test configurations
            double latitude  = FastMath.PI * (0.5 - random.nextDouble());
            double longitude = 2 * FastMath.PI * random.nextDouble();
            TopocentricFrame topo = new TopocentricFrame(earth,
                                                         new GeodeticPoint(latitude, longitude, 0.0),
                                                         "topo");
            Transform transform = earth.getBodyFrame().getTransformTo(topo, date);
            for (int j = 0; j < 20; ++j) {
                double elevation      = FastMath.PI * (0.5 - random.nextDouble());
                double azimuth        = 2 * FastMath.PI * random.nextDouble();
                double range          = 500000.0 * (1.0 + random.nextDouble());
                Vector3D absolutePoint = earth.transform(topo.pointAtDistance(azimuth, elevation, range));
                Vector3D relativePoint = transform.transformPosition(absolutePoint);
                double rebuiltElevation = topo.getElevation(relativePoint, topo, AbsoluteDate.J2000_EPOCH);
                double rebuiltAzimuth   = topo.getAzimuth(relativePoint, topo, AbsoluteDate.J2000_EPOCH);
                double rebuiltRange     = topo.getRange(relativePoint, topo, AbsoluteDate.J2000_EPOCH);
                Assert.assertEquals(elevation, rebuiltElevation, 1.0e-12);
                Assert.assertEquals(azimuth, MathUtils.normalizeAngle(rebuiltAzimuth, azimuth), 1.0e-12);
                Assert.assertEquals(range, rebuiltRange, 1.0e-12 * range);
            }
        }
    }

    @Test
    public void testIssue145() throws OrekitException {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               itrf);
        TopocentricFrame staFrame = new TopocentricFrame(earth, new GeodeticPoint(0.0,0.0,0.0), "test");
        GeodeticPoint gp = staFrame.computeLimitVisibilityPoint(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000,
                                                                0.0, FastMath.toRadians(5.0));
        Assert.assertEquals(0.0, gp.getLongitude(), 1.0e-15);
        Assert.assertTrue(gp.getLatitude() > 0);
        Assert.assertEquals(0.0, staFrame.getNorth().distance(Vector3D.PLUS_K), 1.0e-15);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Elliptic earth shape
            earthSpheric = new OneAxisEllipsoid(6378136.460, 0., itrf);

            // Reference date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            mu = 3.9860047e14;

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @Test
    public void testVisibilityCircle() throws OrekitException, IOException {

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
    }


}
