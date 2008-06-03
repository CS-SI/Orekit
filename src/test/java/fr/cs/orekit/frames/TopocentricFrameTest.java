package fr.cs.orekit.frames;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.orbits.CircularOrbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class TopocentricFrameTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Body mu 
    private double mu;

 
    /** Test topocentric frame at origin (longitude 0, latitude 0, altitude 0) */
    public void testZero() {
        
        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "zero");
        
        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.plusJ);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.plusK);
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.plusI);
        assertEquals(1., xDiff, Utils.epsilonTest);
        assertEquals(1., yDiff, Utils.epsilonTest);
        assertEquals(1., zDiff, Utils.epsilonTest);
   }

    /** Test topocentric frame at pole (longitude 0, latitude 90, altitude 0) */
    public void testPole() {
        
        final GeodeticPoint point = new GeodeticPoint(0., Math.PI/2., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "north pole");
        
        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.plusJ);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.plusI.negate());
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.plusK);
        assertEquals(1., xDiff, Utils.epsilonTest);
        assertEquals(1., yDiff, Utils.epsilonTest);
        assertEquals(1., zDiff, Utils.epsilonTest);
   }

    /** Test topocentric frame at two points separated by 90 deg in latitude */
    public void testNormalLatitudes() {
        
        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lat 45");
        
        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(-45.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lat -45");
      
        // Check that frame North and Zenith directions are all normal to each other, and East are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getEast());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());
        
        assertEquals(1., xDiff, Utils.epsilonTest);
        assertEquals(0., yDiff, Utils.epsilonTest);
        assertEquals(0., zDiff, Utils.epsilonTest);
  }

    /** Test topocentric frame at two points separated by 180 deg in longitude */
    public void testOppositeLongitudes() {
        
        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");
        
        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(Math.toRadians(210.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");
      
        // Check that frame North and Zenith directions are all normal to each other, 
        // and East of the one is West of the other
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());
        
        assertEquals(1., xDiff, Utils.epsilonTest);
        assertEquals(0., yDiff, Utils.epsilonTest);
        assertEquals(0., zDiff, Utils.epsilonTest);
  }

    /** Test with a point at zenith position over the surface point */
    public void testAntipodes() 
        throws OrekitException {
        
        // First point at latitude 45° and longitude 30
        final GeodeticPoint point1 = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");
        
        // Second point at latitude -45° and longitude 210
        final GeodeticPoint point2 = new GeodeticPoint(Math.toRadians(210.), Math.toRadians(-45.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");
      
        // Check that frame Zenith directions are opposite to each other, 
        // and East and North are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());
        
        assertEquals(1., xDiff, Utils.epsilonTest);
        assertEquals(1., yDiff, Utils.epsilonTest);
        assertEquals(-1., zDiff, Utils.epsilonTest);
    }
        
        /** Test with a point at zenith position over the surface point */
    public void testSiteAtZenith() 
        throws OrekitException {
        
        // Surface point at latitude 45°
        final GeodeticPoint point = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 45");
        
        // Point at 800 km over zenith
        final GeodeticPoint satPoint = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(45.), 800000.); 
        //System.out.println(  "sat lon = " + Math.toDegrees(satPoint.getLongitude()) 
        //                   + " sat lat (deg) = " + Math.toDegrees(satPoint.getLatitude())
        //                   + " sat alt = " + satPoint.getAltitude()) ;
        
        // Zenith point elevation = 90 deg
        final double site = topoFrame.getElevation(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat site (deg) = " + Math.toDegrees(site));
        assertEquals(Math.PI/2., site, Utils.epsilonAngle);

        // Zenith point range = defined altitude
        final double range = topoFrame.getRange(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat range (km) = " + range/1000.);
        assertEquals(800000., range, 1e-8);
  }
    
    /** Test in equatorial plane with a point at infinite */
    public void testAzimuthEquatorial() 
        throws OrekitException {
        
        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(0.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 0");
        
        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        GeodeticPoint infPoint = new GeodeticPoint(Math.toRadians(50.), Math.toRadians(0.), 1000000000.);
        
        // Azimuth = pi/2
        double azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat azimuth (deg) = " + Math.toDegrees(azi));
        assertEquals(Math.PI/2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        double site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat site (deg) = " + Math.toDegrees(site));
        assertEquals(Math.PI/2. - Math.abs(point.getLongitude() - infPoint.getLongitude()), site, 1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new GeodeticPoint(Math.toRadians(10.), Math.toRadians(0.), 1000000000.);
        
        // Azimuth = pi/2
        azi = topoFrame.getAzimuth(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat azimuth (deg) = " + Math.toDegrees(azi));
        // assertEquals(3*Math.PI/2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        site = topoFrame.getElevation(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat site (deg) = " + Math.toDegrees(site));
        assertEquals(Math.PI/2. - Math.abs(point.getLongitude() - infPoint.getLongitude()), site, 1.e-2);

    }    

    /** Test with a polar surface point */
    public void testAzimuthPole() 
        throws OrekitException {
        
        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(Math.toRadians(0.), Math.toRadians(89.999), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");
        
        // Point at 30 deg longitude
        // **************************
        GeodeticPoint satPoint = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(28.), 800000.);
        
        // Azimuth = 
        double azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat azimuth (deg) = " + Math.toDegrees(azi));
        assertEquals(Math.PI - satPoint.getLongitude(), azi, 1.e-5);
 
        // Point at -30 deg longitude
        // ***************************
        satPoint = new GeodeticPoint(Math.toRadians(-30.), Math.toRadians(28.), 800000.);
        
        // Azimuth = 
        azi = topoFrame.getAzimuth(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);
        //System.out.println("Sat azimuth (deg) = " + Math.toDegrees(azi));
        assertEquals(Math.PI - satPoint.getLongitude(), azi, 1.e-5);
 
    }    
    
    /** Test range rate computation */
    public void testDoppler() 
        throws OrekitException {
        
        // Surface point at latitude 45, longitude 5
        final GeodeticPoint point = new GeodeticPoint(Math.toRadians(5.), Math.toRadians(45.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");
        
        // Point at 30 deg longitude
        // ***************************
        final CircularOrbit orbit =
            new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, Math.toRadians(50.), Math.toRadians(120.),
                                   Math.toRadians(90.), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        // Transform satellite position to position/velocity parameters in body frame
        final Transform j2000ToItrf = Frame.getJ2000().getTransformTo(earthSpheric.getBodyFrame(), date);
        final PVCoordinates pvSatItrf = j2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());
        
        // Compute range rate directly
        //********************************************
        final double dop = topoFrame.getRangeRate(pvSatItrf, earthSpheric.getBodyFrame(), date);
        
        // Compare to finite difference computation (2 points)
        //*****************************************************
        final double dt = 0.1;
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(orbit));
        
        // Extrapolate satellite position a short while after reference date
        AbsoluteDate dateP = new AbsoluteDate(date, dt);
        Transform j2000ToItrfP = Frame.getJ2000().getTransformTo(earthSpheric.getBodyFrame(), dateP);
        SpacecraftState orbitP = extrapolator.getSpacecraftState(dateP);
        Vector3D satPointGeoP = j2000ToItrfP.transformPVCoordinates(orbitP.getPVCoordinates()).getPosition();
        
        // Retropolate satellite position a short while before reference date
        AbsoluteDate dateM = new AbsoluteDate(date, -dt);
        Transform j2000ToItrfM = Frame.getJ2000().getTransformTo(earthSpheric.getBodyFrame(), dateM);
        SpacecraftState orbitM = extrapolator.getSpacecraftState(dateM);
        Vector3D satPointGeoM = j2000ToItrfM.transformPVCoordinates(orbitM.getPVCoordinates()).getPosition();
        
        // Compute ranges at both instants
        double rangeP = topoFrame.getRange(satPointGeoP, earthSpheric.getBodyFrame(), dateP);
        double rangeM = topoFrame.getRange(satPointGeoM, earthSpheric.getBodyFrame(), dateM);
        final double dopRef2 = (rangeP - rangeM) / (2. * dt);
        assertEquals(dopRef2, dop, 1.e-3);
        
    }

    /** Test for elliptic earth */
    public void testEllipticEarth() 
        throws OrekitException {
        
        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
        
        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final GeodeticPoint satPointGeo = new GeodeticPoint(Math.toRadians(15.), Math.toRadians(30.), 800000.);
        final Vector3D satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        GeodeticPoint point = new GeodeticPoint(Math.toRadians(5.), Math.toRadians(0.), 0.);
        TopocentricFrame topoElliptic = new TopocentricFrame(earthElliptic, point, "elliptic, equatorial lon 5");
        TopocentricFrame topoSpheric = new TopocentricFrame(earthSpheric, point, "spheric, equatorial lon 5");
        
        // Compare azimuth/elevation/range of satellite point : shall be strictly identical 
        // ***************************************************
        double aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        double aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(aziElli, aziSphe, Utils.epsilonAngle);
        
        double eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        double eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(eleElli, eleSphe, Utils.epsilonAngle);
        
        double disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        double disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(disElli, disSphe, Utils.epsilonTest);
        
        // Infinite point separated by -20 deg in longitude
        // *************************************************
        GeodeticPoint infPointGeo = new GeodeticPoint(Math.toRadians(-15.), Math.toRadians(0.), 1000000000.);
        Vector3D infPoint = earthElliptic.transform(infPointGeo);
        
        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        assertEquals(3*Math.PI/2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        assertEquals(Math.PI/2. - Math.abs(point.getLongitude() - infPointGeo.getLongitude()), eleElli, 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new GeodeticPoint(Math.toRadians(25.), Math.toRadians(0.), 1000000000.);
        infPoint = earthElliptic.transform(infPointGeo);
        
        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), date);
        assertEquals(Math.PI/2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), date);
        assertEquals(Math.PI/2. - Math.abs(point.getLongitude() - infPointGeo.getLongitude()), eleElli, 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new GeodeticPoint(Math.toRadians(0.), Math.toRadians(89.999), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 0 lat 90");
        
        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(aziElli, aziSphe, 1.e-7);
        
        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(eleElli, eleSphe, 1.e-2);
        
        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(disElli, disSphe, 20.e+3);
        
        
        // *********************
        // Test at any position
        // *********************
        point = new GeodeticPoint(Math.toRadians(30.), Math.toRadians(60), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 10 lat 45");
        
        // Compare azimuth/elevation/range of satellite point : slight difference 
        // ***************************************************       
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), date);
        aziSphe = topoSpheric.getAzimuth(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(aziElli, aziSphe, 1.e-2);
        
        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), date);
        eleSphe = topoSpheric.getElevation(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(eleElli, eleSphe, 1.e-2);
        
        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), date);
        disSphe = topoSpheric.getRange(satPoint, earthSpheric.getBodyFrame(), date);
        assertEquals(disElli, disSphe, 20.e+3);
        
    }

    public void setUp() {
        try {

            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getReferenceFrame(Frame.ITRF2000B, date);

            // Elliptic earth shape
            earthSpheric = new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);

            // Reference date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Body mu
            mu = 3.9860047e14;
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }
    
    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        earthSpheric = null;
    }


    public static Test suite() {
        return new TestSuite(TopocentricFrameTest.class);
    }

}
