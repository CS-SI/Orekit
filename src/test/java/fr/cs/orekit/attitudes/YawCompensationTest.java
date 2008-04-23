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
import fr.cs.orekit.orbits.Orbit;
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
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    // Transform from J2000 to ITRF2000B 
    private Transform j2000ToItrf;
    
    // Satellite position
    CircularParameters circ;
    PVCoordinates pvSatJ2000;
    PVCoordinates pvSatItrf2000B;
    
    // Earth shape
    OneAxisEllipsoid earthShape;
    
    /** Test that pointed target and observed ground point remain the same 
     * with or without yaw compensation
     */
    public void testTarget() throws OrekitException {
        System.out.println("Test target :");
        System.out.println("----------- :");

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
        System.out.println("No yaw target");
        System.out.println(noYawTarget);

        // with yaw compensation
        PVCoordinates yawTarget = yawCompensLaw.getTargetInBodyFrame(date, pvSatItrf2000B, frameItrf2000B);
        System.out.println("Yaw target");
        System.out.println(yawTarget);

        // Check difference
        PVCoordinates targetDiff = new PVCoordinates(1.0, yawTarget, -1.0, noYawTarget);
        double normTargetDiffPos = targetDiff.getPosition().getNorm();
        double normTargetDiffVel = targetDiff.getVelocity().getNorm();
       
        assertTrue((normTargetDiffPos < Utils.epsilonTest)&&(normTargetDiffVel < Utils.epsilonTest));

        //  Check observed ground point
        // *****************************
        // without yaw compensation
        PVCoordinates noYawObserved = nadirLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);
        System.out.println("No yaw observed ground point");
        System.out.println(noYawObserved);

        // with yaw compensation
        PVCoordinates yawObserved = yawCompensLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);
        System.out.println("Yaw observed ground point");
        System.out.println(yawObserved);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(1.0, yawObserved, -1.0, noYawObserved);
        double normObservedDiffPos = observedDiff.getPosition().getNorm();
        double normObservedDiffVel = observedDiff.getVelocity().getNorm();
       
        assertTrue((normObservedDiffPos < Utils.epsilonTest)&&(normObservedDiffVel < Utils.epsilonTest));
   }

    /** Test that pointed target and observed ground point remain the same 
     * with or without yaw compensation
     */
    public void testCompensMinMax() throws OrekitException {
        System.out.println("");
        System.out.println("Test compens min/max :");
        System.out.println("-------------------- :");

        Orbit orbit = new Orbit(date, circ);

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        GeodeticPoint geoSat = earthShape.transform(pvSatItrf2000B.getPosition(), frameItrf2000B, date);
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw, earthShape);

        
        // Extrapolation over one orbital period (sec)
        double n = Math.sqrt(mu/Math.pow(orbit.getA(), 3));
        double duration = 2.0*Math.PI/n;
        System.out.println("Orbital period (min) : " + duration/60.0);     
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(orbit, mu), mu);
        
        // Extrapolation initializations
        double delta_t = 15.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(date, 0.0); // extrapolation start date

        // Min initialization
        double yawMin = 1.e+12;
        double latMin = 0.;
        
        while (extrapDate.minus(date) < duration)  {
            extrapDate = new AbsoluteDate(extrapDate, delta_t);

            // Extrapolated orbit state at date
            SpacecraftState extrapOrbit = extrapolator.getSpacecraftState(extrapDate);
            PVCoordinates extrapPvSatJ2000 = extrapOrbit.getPVCoordinates(mu);
            
            double extrapLat = earthShape.transform(extrapPvSatJ2000.getPosition(), Frame.getJ2000(), extrapDate).latitude;
            
            // Get attitude rotations from non yaw compensated / yaw compensated laws
            Rotation rotNoYaw = nadirLaw.getState(extrapDate, extrapPvSatJ2000, Frame.getJ2000()).getRotation();
            Rotation rotYaw = yawCompensLaw.getState(extrapDate, extrapPvSatJ2000, Frame.getJ2000()).getRotation();
            
            // Compute yaw compensation angle -- rotations composition
            Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
            Vector3D compoAxis = compoRot.getAxis();
            double compoAngle = compoRot.getAngle();
            if (Vector3D.dotProduct(compoAxis, Vector3D.plusK) < 0) {
                compoAxis = compoAxis.negate();
                compoAngle = -compoAngle;
            }
            
            System.out.println(extrapDate + " yaw (deg) = " + Math.toDegrees(compoAngle)
                                          + " latitude (deg) = " + Math.toDegrees(extrapLat)
                                          + " composition axis = " + compoAxis.getX() + " "
                                                                   + compoAxis.getY() + " "
                                                                   + compoAxis.getZ() + " " 
                                          );
            
            // Update minimum yaw compensation angle
            if (Math.abs(compoAngle) <= yawMin){
                yawMin = Math.abs(compoAngle);
                latMin = extrapLat;
            }           
         }
        
        System.out.println(" Minimum yaw angle (deg) = " + + Math.toDegrees(yawMin) 
                           + " at latitude (deg) : " + Math.toDegrees(latMin));

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

            //  Satellite position
            circ =
                new CircularParameters(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                       Math.toRadians(5.300), CircularParameters.MEAN_LONGITUDE_ARGUMENT, Frame.getJ2000());
            
            // Transform satellite position to position/velocity parameters in J2000 frame
            pvSatJ2000 = circ.getPVCoordinates(mu);
            pvSatItrf2000B = j2000ToItrf.transformPVCoordinates(pvSatJ2000);
         
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
        j2000ToItrf = null;
        circ = null;
        pvSatJ2000 = null;
        pvSatItrf2000B = null;
    }

    public static Test suite() {
        return new TestSuite(YawCompensationTest.class);
    }
}

