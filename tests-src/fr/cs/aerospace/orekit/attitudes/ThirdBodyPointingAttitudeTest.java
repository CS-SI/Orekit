package fr.cs.aerospace.orekit.attitudes;

import java.io.FileNotFoundException;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.attitudes.ThirdBodyPointingAttitude;
import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.orbits.CircularParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ThirdBodyPointingAttitudeTest extends TestCase {
  
  public void testSimpleBehaviour() throws ParseException, OrekitException, FileNotFoundException {
    
    // parameters
    final double a = 12000000;
    final double ex = 1e-3;
    final double ey = 1e-3;
    final double OMEGA = 0;
    final double l = 0;
    final double mu = Utils.mu;
    
    final OrbitalParameters op = new CircularParameters(a, ex, ey, 0, OMEGA, l, 
                                                  CircularParameters.TRUE_LONGITUDE_ARGUMENT
                                                  , Frame.getJ2000());
    
    final AbsoluteDate initDate = new AbsoluteDate("2001-03-21T00:00:00",
                                         UTCScale.getInstance());
    
    final Orbit o = new Orbit(initDate, op);
    
    final double period = 2*Math.PI*Math.sqrt(a*a*a/mu);
    final double spin = 2*Math.PI/period;    
    final ThirdBody sun = new Sun();
    
    AttitudeKinematicsProvider att = 
      new ThirdBodyPointingAttitude(sun, initDate, o.getPVCoordinates(mu),
                                         Frame.getJ2000(), spin,
                                    Vector3D.plusI, Vector3D.plusJ, Vector3D.plusJ);
    
    final SpacecraftState initState = new SpacecraftState(o, 1000, 
                                  att.getAttitudeKinematics(initDate, o.getPVCoordinates(mu), 
                                                            o.getFrame()));
    
    KeplerianPropagator kep = new KeplerianPropagator(initState, mu);
    kep.setAkProvider(att);
    // Spin tests
   AbsoluteDate interDate;
   SpacecraftState interState;
   Vector3D interJtrans;
   for(int i=0; i<=period; i++) {
     interDate= new AbsoluteDate(initDate, i);
     interState = kep.getSpacecraftState(interDate);
     interJtrans = interState.getAttitude().applyInverseTo(Vector3D.plusJ); 
     assertEquals(0, (interJtrans.getY()-Math.cos(i/(period)*2*Math.PI)), 5e-05);
     assertEquals(0, (interJtrans.getZ()+Math.sin(i/(period)*2*Math.PI)), 1);
   }
   // sun pointing tests
   
   final AbsoluteDate finDate = new AbsoluteDate("2001-09-21T00:00:00",
                                           UTCScale.getInstance());
   final AbsoluteDate medDate = new AbsoluteDate(initDate , finDate.minus(initDate)/2);
   final SpacecraftState medState = kep.getSpacecraftState(medDate);
   
   Vector3D sunPos = Vector3D.subtract(sun.getPosition(medDate, Frame.getJ2000()) ,
                                       medState.getPVCoordinates(mu).getPosition());
   Vector3D transX = medState.getAttitude().applyInverseTo(Vector3D.plusI);
   assertEquals(0, Vector3D.angle(transX , sunPos), 1e-15);
   
   final SpacecraftState finState = kep.getSpacecraftState(finDate);    
   sunPos = Vector3D.subtract(sun.getPosition(finDate, Frame.getJ2000()) ,
                              finState.getPVCoordinates(mu).getPosition());
   
   transX = finState.getAttitude().applyInverseTo(Vector3D.plusI);
   assertEquals(0, Vector3D.angle(transX , sunPos), 1e-10);
  }
  
  public static Test suite() {
    return new TestSuite(ThirdBodyPointingAttitudeTest.class);
  }
}
