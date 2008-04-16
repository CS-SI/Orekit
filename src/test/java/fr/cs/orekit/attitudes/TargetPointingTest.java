package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.orbits.CircularParameters;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.Line;
import fr.cs.orekit.utils.PVCoordinates;

public class TargetPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Transform from J2000 to ITRF2000B 
    private Transform j2000ToItrf;
    
    /** Test class for body center pointing attitude law.
     */
    public TargetPointingTest(String name) {
        super(name);
    }

    /** Test if both constructors are equivalent
     */
    public void testConstructors() throws OrekitException {

        //  Satellite position
        // ********************
        CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        System.out.println("PV sat in J2000");
        System.out.println(pvSatJ2000);
     
        //  Attitude laws
        // *************** 
        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Target definition as a geodetic point AND as a position/velocity vector
        GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
        PVCoordinates pvTargetItrf2000B = new PVCoordinates(earthShape.transform(geoTargetItrf2000B), Vector3D.zero);
            
        // Attitude law definition from geodetic point target 
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(frameItrf2000B, geoTargetItrf2000B, earthShape);
        
        //  Attitude law definition from position/velocity target
        TargetPointing pvTargetAttitudeLaw = new TargetPointing(frameItrf2000B, pvTargetItrf2000B);
        
        // Check that both attitude are the same 
        // Get satellite rotation for target pointing law 
        Rotation rotPv = pvTargetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println("Rotation angle for pv definition = " + Math.toDegrees(rotPv.getAngle()));

        // Get satellite rotation for nadir pointing law
        Rotation rotGeo = geoTargetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println("Rotation angle for geo definition = " + Math.toDegrees(rotGeo.getAngle()));

        //
        Rotation rotCompo = rotGeo.applyInverseTo(rotPv);
        double angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (deg) = " + Math.toDegrees(angle));
        assertEquals(angle, 0.0, Utils.epsilonAngle);

    }

    /** Test if geodetic constructor works
     */
    public void testGeodeticConstructor() throws OrekitException {

        //  Satellite position
        // ******************** 
        CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        
        // Transform satellite position to position/velocity parameters in J2000 frame */
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
     
        //  Attitude law
        // ************** 
        
        // Elliptic earth shape */
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Target definition as a geodetic point 
        GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
        System.out.println("Defined target : lon = " + geoTargetItrf2000B.longitude 
                           + " lat = " + geoTargetItrf2000B.latitude 
                           + " alt = " + geoTargetItrf2000B.altitude);
            
        //  Attitude law definition 
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(frameItrf2000B, geoTargetItrf2000B, earthShape);
        
        // Check that observed ground point is the same as defined target 
        PVCoordinates pvObservedJ2000 = geoTargetAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        GeodeticPoint geoObserved = earthShape.transform(pvObservedJ2000.getPosition(), Frame.getJ2000(), date);
        System.out.println("Observed ground point : lon = " + geoObserved.longitude 
                                                + " lat = " + geoObserved.latitude 
                                                + " alt = " + geoObserved.altitude);

        assertEquals(geoObserved.longitude, geoTargetItrf2000B.longitude, Utils.epsilonAngle);
        assertEquals(geoObserved.latitude, geoTargetItrf2000B.latitude, Utils.epsilonAngle);
        assertEquals(geoObserved.altitude, geoTargetItrf2000B.altitude, 1.e-8);

    }

    /** Test with nadir target : Check that when the target is the same as nadir target,
     * satellite attitude is the same as nadir attitude.
     */
    public void testNadirTarget() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Satellite on any position 
        CircularParameters circ =
            new CircularParameters(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularParameters.TRUE_LONGITUDE_ARGUMENT, Frame.getJ2000());
 
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        
        
        //  Target attitude law with target under satellite nadir 
        // ******************************************************* 
        // Definition of nadir target 
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(frameItrf2000B, earthShape);
        
        // Check nadir target 
        PVCoordinates pvNadirTarget = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        GeodeticPoint geoNadirTarget = earthShape.transform(pvNadirTarget.getPosition(), frameItrf2000B, date);
        System.out.println("Nadir target longitude = " + Math.toDegrees(geoNadirTarget.longitude));
        System.out.println("Nadir target latitude = " + Math.toDegrees(geoNadirTarget.latitude));
        System.out.println("Nadir target altitude = " + Math.toDegrees(geoNadirTarget.altitude));
        
        // Create target attitude law 
        TargetPointing targetAttitudeLaw = new TargetPointing(Frame.getJ2000(), geoNadirTarget, earthShape);

        // Get satellite rotation for target pointing law 
        Rotation rotTarget = targetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println("Target pointing rotation angle = " + Math.toDegrees(rotTarget.getAngle()));

        // Get satellite rotation for nadir pointing law 
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println("Nadir pointing rotation angle = " + Math.toDegrees(rotNadir.getAngle()));

        // For a target under satellite nadir, nadir pointing attitude 
        // and nadir pointing attitude shall be the same, 
        // i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotTarget.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (deg) = " + Math.toDegrees(angle));
        assertEquals(angle, 0.0, Utils.epsilonAngle);

        
    }
       
    /** Test if defined target belongs to the direction pointed by the satellite
     */
    public void testTargetInPointingDirection() throws OrekitException {

        // Create computation date 
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());
        
        // Reference frame = ITRF 2000B
        Frame frameItrf2000B = Frame.getReferenceFrame(Frame.ITRF2000B, date);

        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Create target pointing attitude law 
        GeodeticPoint geoTarget = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
        TargetPointing targetAttitudeLaw = new TargetPointing(Frame.getJ2000(), geoTarget, earthShape);
        
        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        System.out.println("PV sat in J2000");
        System.out.println(pvSatJ2000);
        
        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from J2000 frame to satellite frame
        Rotation rotSatJ2000 = targetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Transform Z axis from satellite frame to ITRF2000B 
        Vector3D ZSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.plusK);
        System.out.println("Zsat in J2000");
        System.out.println(ZSatJ2000.getX());
        System.out.println(ZSatJ2000.getY());
        System.out.println(ZSatJ2000.getZ());
        
        // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatJ2000.getPosition(), ZSatJ2000);
        
        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        double distance = pointingLine.distance(earthShape.transform(geoTarget));
        System.out.println("distance");
        System.out.println(distance);
        
        boolean test = (distance < 1.e-7);
        System.out.println("test");
        System.out.println(test);
        assertEquals(test, true);
    }

    /** Test the difference between pointing over two longitudes separated by 5°
     */
    public void testSlewedTarget() throws OrekitException {

        // Spheric earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
        
        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularParameters circ =
            new CircularParameters(42164000.0, 0.5e-8, -0.5e-8, 0., 0.,
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        System.out.println("PV sat in J2000");
        System.out.println(pvSatJ2000);
        
        // Create nadir pointing attitude law 
        // ********************************** 
        NadirPointing nadirAttitudeLaw = new NadirPointing(frameItrf2000B, earthShape);
        
        // Get observed ground point from nadir pointing law
        PVCoordinates pvNadirObservedJ2000 = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        PVCoordinates pvNadirObservedItrf2000B = j2000ToItrf.transformPVCoordinates(pvNadirObservedJ2000);
        
        GeodeticPoint geoNadirObserved = earthShape.transform(pvNadirObservedItrf2000B.getPosition(), frameItrf2000B, date);
        System.out.println("Nadir target lon = " + Math.toDegrees(geoNadirObserved.longitude)
                                     + " lat = " + Math.toDegrees(geoNadirObserved.latitude)
                                     + " alt = " + Math.toDegrees(geoNadirObserved.altitude));

        // Create target pointing attitude law with target equal to nadir target 
        // ********************************************************************* 
        TargetPointing targetLawRef = new TargetPointing(frameItrf2000B, pvNadirObservedItrf2000B);
        
        // Get attitude rotation in J2000
        Rotation rotSatRefJ2000 = targetLawRef.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println("Ref rotation axis x = " + rotSatRefJ2000.getAxis().getX()
                                          + " y = " + rotSatRefJ2000.getAxis().getY()
                                          + " z = " + rotSatRefJ2000.getAxis().getZ());
        System.out.println("Ref rotation angle = " + rotSatRefJ2000.getAngle());
      
        // Create target pointing attitude law with target 5° from nadir target 
        // ******************************************************************** 
        GeodeticPoint geoTarget = new GeodeticPoint(geoNadirObserved.longitude - Math.toRadians(5),
                                                    geoNadirObserved.latitude, geoNadirObserved.altitude);
        PVCoordinates pvTargetItrf2000B = new PVCoordinates(earthShape.transform(geoTarget), Vector3D.zero);
        TargetPointing targetLaw = new TargetPointing(frameItrf2000B, pvTargetItrf2000B);
        
        // Get attitude rotation 
        Rotation rotSatJ2000 = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        System.out.println(" 5° target rotation axis x = " + rotSatJ2000.getAxis().getX()
                           + " y = " + rotSatJ2000.getAxis().getY()
                           + " z = " + rotSatJ2000.getAxis().getZ());
        System.out.println("5° target rotation angle = " + rotSatJ2000.getAngle());
        
        // Compute difference between both attitude laws 
        // *********************************************
        // "Difference" between axes 
        Vector3D crossProd = Vector3D.crossProduct(rotSatJ2000.getAxis(), rotSatRefJ2000.getAxis());
        double norm = Math.sqrt(Vector3D.dotProduct(crossProd, crossProd));
        System.out.println("Cross product = " + crossProd.getX() 
                           + " " + crossProd.getY() 
                           + " " + crossProd.getZ());
        System.out.println("Norm = " + norm);
        
        assertEquals(norm, 0.0, 1.e-2);
        
        // Difference between axes
        //  expected
        double tanDeltaExpected = (6378136.460/(42164000.0-6378136.460))*Math.tan(Math.toRadians(5));
        double deltaExpected = Math.atan(tanDeltaExpected);
        System.out.println("Delta expected = " + Math.toDegrees(deltaExpected));
        
        //  real
        double deltaReal = rotSatJ2000.applyInverseTo(rotSatRefJ2000).getAngle();
        System.out.println("Delta real = " + Math.toDegrees(deltaReal));
        
        assertEquals(deltaReal, deltaExpected, 1.e-4);
        
    } 

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

            // Transform from J2000 to ITRF2000B
            j2000ToItrf = Frame.getJ2000().getTransformTo(frameItrf2000B, date);

        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        j2000ToItrf = null;
    }

    public static Test suite() {
        return new TestSuite(TargetPointingTest.class);
    }
}

