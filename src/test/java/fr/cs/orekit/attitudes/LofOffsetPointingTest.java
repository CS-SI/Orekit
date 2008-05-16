package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
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
        final CircularParameters circ =
            new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
        final PVCoordinates pvSatJ2000 = circ.getPVCoordinates(mu);

        // Create lof aligned law
        //************************
        final LofOffset lofLaw = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        final LofOffsetPointing lofPointing = new LofOffsetPointing(earthSpheric, lofLaw, Vector3D.plusK);
        final Rotation lofRot = lofPointing.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
 
        // Compare to body center pointing law
        //*************************************
        final BodyCenterPointing centerLaw = new BodyCenterPointing(earthSpheric.getBodyFrame());
        final Rotation centerRot = centerLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        final double angleBodyCenter = centerRot.applyInverseTo(lofRot).getAngle();
        assertEquals(0., angleBodyCenter, Utils.epsilonAngle);

        // Compare to nadir pointing law
        //*******************************
        final NadirPointing nadirLaw = new NadirPointing(earthSpheric);
        final Rotation nadirRot = nadirLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        final double angleNadir = nadirRot.applyInverseTo(lofRot).getAngle();
        assertEquals(0., angleNadir, Utils.epsilonAngle);

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
        return new TestSuite(LofOffsetPointingTest.class);
    }
}

