package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.CircularParameters;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class LofOffsetPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Earth shape
    OneAxisEllipsoid earthSpheric;
    
    /** Test class for body center pointing attitude law.
     */
    public LofOffsetPointingTest(String name) {
        super(name);
    }

    /** Test if both constructors are equivalent
     */
    public void testLof() throws OrekitException {

        //  Satellite position
        CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);

        System.out.println("Date = " + date);
        System.out.println("PV sat = " + pvSatJ2000.getPosition().getX() + " " +
                                         pvSatJ2000.getPosition().getY() + " " +
                                         pvSatJ2000.getPosition().getZ() + " " +
                                         pvSatJ2000.getVelocity().getX() + " " +
                                         pvSatJ2000.getVelocity().getY() + " " +
                                         pvSatJ2000.getVelocity().getZ() + " ");
        
        System.out.println("");
        
        // Create lof aligned law
        //************************
        LofOffset lofLaw = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        LofOffsetPointing lofPointing = new LofOffsetPointing(earthSpheric, lofLaw, Vector3D.plusK);
        Rotation lofRot = lofPointing.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        PVCoordinates lofTarget = lofPointing.getTargetInBodyFrame(date, pvSatJ2000, Frame.getJ2000());
        System.out.println("Lof target = " + lofTarget);
        GeodeticPoint lofGeoTarget = earthSpheric.transform(lofTarget.getPosition(), earthSpheric.getBodyFrame(), date);
        System.out.println("Lof geodetic target lat (deg) = " + Math.toDegrees(lofGeoTarget.getLatitude())
                           + " lon (deg) = " + Math.toDegrees(lofGeoTarget.getLongitude())
                           + " alt (m) = " + lofGeoTarget.getAltitude());

        PVCoordinates lofObserved = lofPointing.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        System.out.println("Lof observed = " + lofObserved);
        GeodeticPoint lofGeoObserved = earthSpheric.transform(lofObserved.getPosition(), Frame.getJ2000(), date);
        System.out.println("Lof geodetic observed lat (deg) = " + Math.toDegrees(lofGeoObserved.getLatitude())
                           + " lon (deg) = " + Math.toDegrees(lofGeoObserved.getLongitude())
                           + " alt (m) = " + lofGeoObserved.getAltitude());

        // Compare to body center pointing law
        //*************************************
        BodyCenterPointing centerLaw = new BodyCenterPointing(earthSpheric.getBodyFrame());
        Rotation centerRot = centerLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        PVCoordinates centerObserved = centerLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        System.out.println("Center observed" + centerObserved);

        double angleBodyCenter = centerRot.applyInverseTo(lofRot).getAngle();
        System.out.println("Angle compo (deg) = " + Math.toDegrees(angleBodyCenter));

        // Compare to nadir pointing law
        //*******************************
        NadirPointing nadirLaw = new NadirPointing(earthSpheric);
        Rotation nadirRot = nadirLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        PVCoordinates nadirObserved = nadirLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        System.out.println("Nadir observed" + nadirObserved);
        GeodeticPoint nadirGeoObserved = earthSpheric.transform(nadirObserved.getPosition(), Frame.getJ2000(), date);
        System.out.println("Nadir geodetic observed lat (deg) = " + Math.toDegrees(nadirGeoObserved.getLatitude())
                           + " lon (deg) = " + Math.toDegrees(nadirGeoObserved.getLongitude())
                           + " alt (m) = " + nadirGeoObserved.getAltitude());

        double angleNadir = nadirRot.applyInverseTo(lofRot).getAngle();
        System.out.println("Angle compo (deg) = " + Math.toDegrees(angleNadir));

   } 
    
    /** Test if both constructors are equivalent
     */
//    public void testTarget() throws OrekitException {
//
//        //  Satellite position
//        CircularParameters circ =
//            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
//                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
//        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
//        
//        // Target pointing law
//        //*********************
//        GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
//        TargetPointing targetLaw = new TargetPointing(geoTargetItrf2000B, earthShape);
//
//        Rotation targetRot = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
//        
//        // Create lof offset law
//        //***********************
//        
//    } 

    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Body mu
            mu = 3.9860047e14;
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getReferenceFrame(Frame.ITRF2000B, date);

            // Elliptic earth shape
            earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
            
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
        return new TestSuite(LofOffsetPointingTest.class);
    }
}

