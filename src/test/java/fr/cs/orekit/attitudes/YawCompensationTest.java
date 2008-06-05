package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.CircularOrbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class YawCompensationTest extends TestCase {

    /** Test class for body center pointing attitude law.
     */
    public YawCompensationTest(String name) {
        super(name);
    }

    // Computation date 
    private AbsoluteDate date;
    
    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    // Satellite position
    CircularOrbit circOrbit;
    PVCoordinates pvSatItrf2000B;
    
    // Earth shape
    OneAxisEllipsoid earthShape;
    
    /** Test that pointed target and observed ground point remain the same 
     * with or without yaw compensation
     */
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw, earthShape);
       
        //  Check target
        // **************
        // without yaw compensation
        PVCoordinates noYawTarget = nadirLaw.getTargetInBodyFrame(date, pvSatItrf2000B, frameItrf2000B);
         
        // with yaw compensation
        PVCoordinates yawTarget = yawCompensLaw.getTargetInBodyFrame(date, pvSatItrf2000B, frameItrf2000B);

        // Check difference
        PVCoordinates targetDiff = new PVCoordinates(1.0, yawTarget, -1.0, noYawTarget);
        double normTargetDiffPos = targetDiff.getPosition().getNorm();
        double normTargetDiffVel = targetDiff.getVelocity().getNorm();
       
        assertTrue((normTargetDiffPos < Utils.epsilonTest)&&(normTargetDiffVel < Utils.epsilonTest));

        //  Check observed ground point
        // *****************************
        // without yaw compensation
        PVCoordinates noYawObserved = nadirLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);

        // with yaw compensation
        PVCoordinates yawObserved = yawCompensLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(1.0, yawObserved, -1.0, noYawObserved);
        double normObservedDiffPos = observedDiff.getPosition().getNorm();
        double normObservedDiffVel = observedDiff.getVelocity().getNorm();
       
        assertTrue((normObservedDiffPos < Utils.epsilonTest)&&(normObservedDiffVel < Utils.epsilonTest));
   }

    /** Test that maximum yaw compensation is at ascending/descending node, 
     * and minimum yaw compensation is at maximum latitude.
     */
    public void testCompensMinMax() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw, earthShape);

        
        // Extrapolation over one orbital period (sec)
        double n = Math.sqrt(circOrbit.getMu()/Math.pow(circOrbit.getA(), 3));
        double duration = 2.0*Math.PI/n;
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(circOrbit));
        
        // Extrapolation initializations
        double delta_t = 15.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(date, 0.0); // extrapolation start date

        // Min initialization
        double yawMin = 1.e+12;
        double latMin = 0.;
        
        while (extrapDate.minus(date) < duration)  {
            extrapDate = new AbsoluteDate(extrapDate, delta_t);

            // Extrapolated orbit state at date
            SpacecraftState extrapOrbit = extrapolator.propagate(extrapDate);
            PVCoordinates extrapPvSatJ2000 = extrapOrbit.getPVCoordinates();
            
            // Satellite latitude at date
            double extrapLat = earthShape.transform(extrapPvSatJ2000.getPosition(), Frame.getJ2000(), extrapDate).getLatitude();
            
            // Compute yaw compensation angle -- rotations composition
            double yawAngle = yawCompensLaw.getYawAngle(extrapDate, extrapPvSatJ2000, Frame.getJ2000());
                        
            // Update minimum yaw compensation angle
            if (Math.abs(yawAngle) <= yawMin) {
                yawMin = Math.abs(yawAngle);
                latMin = extrapLat;
            }           

            //     Checks
            // ------------------
            
            // 1/ Check yaw values around ascending node (max yaw)
            if ( (Math.abs(extrapLat) < Math.toRadians(2.))
                    && (extrapPvSatJ2000.getVelocity().getZ() >= 0. ) )
            {
                assertTrue((Math.abs(yawAngle) >= Math.toRadians(2.8488911779424484)) 
                        && (Math.abs(yawAngle) <= Math.toRadians(2.8531667023319462)));
            }
            
            // 2/ Check yaw values around maximum positive latitude (min yaw)
            if ( extrapLat > Math.toRadians(50.) )
            {
                assertTrue((Math.abs(yawAngle) <= Math.toRadians(0.2627860468476059)) 
                        && (Math.abs(yawAngle) >= Math.toRadians(0.0032285415586776798)));
            }
            
            // 3/ Check yaw values around descending node (max yaw)
            if ( (Math.abs(extrapLat) < Math.toRadians(2.))
                    && (extrapPvSatJ2000.getVelocity().getZ() <= 0. ) )
            {
                assertTrue((Math.abs(yawAngle) >= Math.toRadians(2.8485147320218904)) 
                             && (Math.abs(yawAngle) <= Math.toRadians(2.8535230849061097)));
            }
         
            // 4/ Check yaw values around maximum negative latitude (min yaw)
            if ( extrapLat < Math.toRadians(-50.) )
            {
                assertTrue((Math.abs(yawAngle) <= Math.toRadians(0.2358843505723192)) 
                             && (Math.abs(yawAngle) >= Math.toRadians(0.014143120809540489)));
            }

        }
        
        // 5/ Check that minimum yaw compensation value is around maximum latitude
        assertEquals(Math.toRadians(0.0032285415586776798), yawMin, Utils.epsilonAngle);
        assertEquals(Math.toRadians(50.21484459355221), latMin, Utils.epsilonAngle);

    }

    /** Test that compensation rotation axis is Zsat, yaw axis
     */
    public void testCompensAxis() throws OrekitException {

        PVCoordinates pvSatJ2000 = circOrbit.getPVCoordinates();

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw, earthShape);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        Rotation rotYaw = yawCompensLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
            
        // Compose rotations composition
        Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        Vector3D yawAxis = compoRot.getAxis();

        // Check axis
        assertEquals(0., yawAxis.getX(), Utils.epsilonTest);
        assertEquals(0., yawAxis.getY(), Utils.epsilonTest);
        assertEquals(1., yawAxis.getZ(), Utils.epsilonTest);

    }
    
    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Body mu
            final double mu = 3.9860047e14;
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getReferenceFrame(Frame.ITRF2000B, date);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       Frame.getJ2000(), date, mu);
            
            pvSatItrf2000B = circOrbit.getPVCoordinates(frameItrf2000B);
            
            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        circOrbit = null;
        pvSatItrf2000B = null;
        earthShape = null;
    }

    public static Test suite() {
        return new TestSuite(YawCompensationTest.class);
    }
}

