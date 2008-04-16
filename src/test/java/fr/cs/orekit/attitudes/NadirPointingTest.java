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
import fr.cs.orekit.orbits.KeplerianParameters;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class NadirPointingTest extends TestCase {

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
    public NadirPointingTest(String name) {
        super(name);
    }

    /** Test in the case of a spheric earth : nadir pointing shall be 
     * the same as earth center pointing
     */
    public void testSphericEarth() throws OrekitException {

        // Spheric earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(frameItrf2000B, earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
        
        // Create satellite position as circular parameters 
        CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        
        // Get nadir attitude */
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude */
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (deg) = " + Math.toDegrees(angle));
        assertEquals(angle, 0.0, Utils.epsilonAngle);

}
    
    /** Test in the case of an elliptic earth : nadir pointing shall be :
     *   - the same as earth center pointing in case of equatorial or polar position
     *   - different from earth center pointing in any other case
     */
    public void testNonSphericEarth() throws OrekitException {

        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(frameItrf2000B, earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
        
        //  Satellite on equatorial position
        // ********************************** 
        System.out.println("Equatorial case");
        
        KeplerianParameters kep =
            new KeplerianParameters(7178000.0, 1.e-8, Math.toRadians(50.), 0., 0.,
                                    0., KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());
 
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = kep.getPVCoordinates(mu);
        
        // Get nadir attitude 
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude 
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (rad) = " + angle);
        assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on polar position
        // ***************************** 
        System.out.println("Polar case");

        CircularParameters circ =
            new CircularParameters(7178000.0, 1.e-5, 0., Math.toRadians(90.), 0.,
                                   Math.toRadians(90.), CircularParameters.TRUE_LONGITUDE_ARGUMENT, Frame.getJ2000());
 
        // Transform satellite position to position/velocity parameters in J2000 frame */
        pvSatJ2000 = circ.getPVCoordinates(mu);
        
        // Check satellite latitude 
        GeodeticPoint geoSat = earthShape.transform(pvSatJ2000.getPosition(), Frame.getJ2000(), date);
        System.out.println("Polar satellite latitude (deg) = " + Math.toDegrees(geoSat.latitude));
        System.out.println("Polar satellite longitude (deg) = " + Math.toDegrees(geoSat.longitude));
        System.out.println("Polar satellite altitude (deg) = " + Math.toDegrees(geoSat.altitude));
        
        
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude 
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at polar position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation 
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (rad) = " + angle);
        assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on any position
        // *************************** 
        System.out.println("Any case");

        circ =
            new CircularParameters(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularParameters.TRUE_LONGITUDE_ARGUMENT, Frame.getJ2000());
 
        // Transform satellite position to position/velocity parameters in J2000 frame 
        pvSatJ2000 = circ.getPVCoordinates(mu);
        
        // Check satellite latitude 
        geoSat = earthShape.transform(pvSatJ2000.getPosition(), Frame.getJ2000(), date);
        System.out.println("Polar satellite latitude (deg) = " + Math.toDegrees(geoSat.latitude));
        System.out.println("Polar satellite longitude (deg) = " + Math.toDegrees(geoSat.longitude));
        System.out.println("Polar satellite altitude (deg) = " + Math.toDegrees(geoSat.altitude));
        
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at any position, earth center pointing attitude and nadir pointing 
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth 
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        System.out.println("Composed rotation angle (deg) = " + Math.toDegrees(angle));
        assertEquals(angle, Math.toRadians(0.16797386586252272), Utils.epsilonAngle);
    }
    
    
       
    /** Vertical test : check that Z satellite axis is colinear to local vertical axis,
        which direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)), 
        where lon et lat stand for observed point coordinates 
        (i.e satellite ones, since they are the same by construction,
        but that's what is to test.
     */
    public void testVertical() throws OrekitException {

        /* Elliptic earth shape */
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        /* Create earth center pointing attitude law */
        NadirPointing nadirAttitudeLaw = new NadirPointing(frameItrf2000B, earthShape);

        /*  Satellite on any position */
        CircularParameters circ =
            new CircularParameters(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularParameters.TRUE_LONGITUDE_ARGUMENT, Frame.getJ2000());
 
        /* Transform satellite position to position/velocity parameters in J2000 frame */
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        
        /*  Vertical test
         * *************** */
        /* Get observed ground point position/velocity */
        PVCoordinates pvTargetJ2000 = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        PVCoordinates pvTargetItrf2000B = j2000ToItrf.transformPVCoordinates(pvTargetJ2000);
        
        /* Convert to geodetic coordinates */
        GeodeticPoint geoTarget = earthShape.transform(pvTargetItrf2000B.getPosition(), frameItrf2000B, date);

        /* Compute local vertical axis */
        double xVert = Math.cos(geoTarget.longitude)*Math.cos(geoTarget.latitude);
        double yVert = Math.sin(geoTarget.longitude)*Math.cos(geoTarget.latitude);
        double zVert = Math.sin(geoTarget.latitude);
        Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);
        System.out.println("Local vertical axis = " + targetVertical.getX()
                                              + " " + targetVertical.getY() 
                                              + " " + targetVertical.getZ());
        
        /* Get attitude rotation state */
        Rotation rotSatJ2000 = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
                
        /* Get satellite Z axis in J2000 frame */
        Vector3D zSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.plusK);
        Vector3D zSatItrf2000B = j2000ToItrf.transformVector(zSatJ2000);
        
        System.out.println("Satellite Z axis = " + zSatItrf2000B.getX() 
                                           + " " + zSatItrf2000B.getY() 
                                           + " " + zSatItrf2000B.getZ());
        
        /* Check that satellite Z axis is colinear to local vertical axis */
        double angle= Vector3D.angle(zSatItrf2000B, targetVertical);
        System.out.println("angle = " + angle);
        
        assertEquals(Math.sin(angle), 0.0, Utils.epsilonTest);
        
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
        return new TestSuite(NadirPointingTest.class);
    }
}

