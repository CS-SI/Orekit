package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
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

public class BodyCenterPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Orbit 
    private CircularParameters circ;

    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    // Transform from J2000 to ITRF2000B 
    private Transform j2000ToItrf;
    
    // Earth center pointing attitude law 
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test class for body center pointing attitude law.
     */
    public BodyCenterPointingTest(String name) {
        super(name);
    }

    /** Test if target is body center
     */
    public void testTarget() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        
        // Call get target method 
        PVCoordinates target = earthCenterAttitudeLaw.getTargetInBodyFrame(date, pvSatJ2000, Frame.getJ2000());
        System.out.println("target");
        System.out.println(target);

        // Check that target is body center
        double normPos = target.getPosition().getNorm();
        double normVel = target.getVelocity().getNorm();
        boolean test = ((normPos < Utils.epsilonTest) && (normVel < Utils.epsilonTest));
        System.out.println("test");
        System.out.println(test);
        assertEquals(test, true);

    }


    /** Test if body center belongs to the direction pointed by the satellite
     */
    public void testBodyCenterInPointingDirection() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);
        System.out.println("PV sat in J2000");
        System.out.println(pvSatJ2000);
        
        //  Pointing direction
        // ******************** 
        // Get satellite attitude rotation, i.e rotation from J2000 frame to satellite frame
        Rotation rotSatJ2000 = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Transform Z axis from satellite frame to J2000 
        Vector3D zSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.plusK);
        
        // Transform Z axis from J2000 to ITRF2000B
        Vector3D zSatItrf2000B = j2000ToItrf.transformPosition(zSatJ2000);
        
        System.out.println("Zsat in ITRF2000B");
        System.out.println(zSatItrf2000B.getX());
        System.out.println(zSatItrf2000B.getY());
        System.out.println(zSatItrf2000B.getZ());

        // Transform satellite position/velocity from J2000 to ITRF2000B 
        PVCoordinates pvSatItrf2000B = j2000ToItrf.transformPVCoordinates(pvSatJ2000);
        System.out.println("PV sat in ITRF2000B");
        System.out.println(pvSatItrf2000B);
                
       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatItrf2000B.getPosition(), zSatItrf2000B);
        
        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        double distance = pointingLine.distance(Vector3D.zero);
        System.out.println("distance");
        System.out.println(distance);
        
        boolean test = (distance < 1.e-8);
        System.out.println("test");
        System.out.println(test);
        assertEquals(test, true);
    }

    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Satellite position as circular parameters
            double raan = 270.;
            circ =
                new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(raan),
                                       Math.toRadians(5.300 - raan), CircularParameters.MEAN_LONGITUDE_ARGUMENT,
                                       Frame.getJ2000());
            
            mu = 3.9860047e14;
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getReferenceFrame(Frame.ITRF2000B, date);

            // Transform from J2000 to ITRF2000B
            j2000ToItrf = Frame.getJ2000().getTransformTo(frameItrf2000B, date);

            // Create earth center pointing attitude law */
            earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        j2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

    public static Test suite() {
        return new TestSuite(BodyCenterPointingTest.class);
    }

}

