package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.CardanEulerSingularityException;
import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.CircularOrbit;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class LofOffsetTest extends TestCase {

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
    public LofOffsetTest(String name) {
        super(name);
    }

    /** Test is the lof offset is the one expected
     */
    public void testZero() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        final PVCoordinates pvSatJ2000 = circ.getPVCoordinates();

        // Lof aligned attitude law
        final LofOffset lofAlignedLaw = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        final Rotation lofOffsetRot = lofAlignedLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Check that 
        final Vector3D p = pvSatJ2000.getPosition();
        final Vector3D v = pvSatJ2000.getVelocity();
        final Vector3D momentumJ2000 = Vector3D.crossProduct(p, v);
        final Vector3D momentumLof = lofOffsetRot.applyInverseTo(momentumJ2000);
        final double cosinus = Math.cos(Vector3D.dotProduct(momentumLof, Vector3D.plusK));
        assertEquals(1., cosinus, Utils.epsilonAngle);
        
    }
        /** Test is the lof offset is the one expected
     */
    public void testOffset() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        final PVCoordinates pvSatJ2000 = circ.getPVCoordinates();

        // Create target pointing attitude law
        // ************************************  
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
        final GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
            
        // Attitude law definition from geodetic point target 
        final TargetPointing targetLaw = new TargetPointing(geoTargetItrf2000B, earthShape);
        final Rotation targetRot = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();       
        
        // Create lof aligned attitude law
        // *******************************  
        final LofOffset lofAlignedLaw = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        final Rotation lofAlignedRot = lofAlignedLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Get rotation from LOF to target pointing attitude
        Rotation rollPitchYaw = targetRot.applyTo(lofAlignedRot.revert());
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];
        
        // Create lof offset attitude law with computed roll, pitch, yaw
        // **************************************************************  
        final LofOffset lofOffsetLaw = new LofOffset(RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.applyInverseTo(lofOffsetRot).getAngle();
        assertEquals(0., angleCompo, Utils.epsilonAngle);
        
        
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
        return new TestSuite(LofOffsetTest.class);
    }
}

