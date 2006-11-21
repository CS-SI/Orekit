package fr.cs.aerospace.orekit.maneuvers;

import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.maneuvers.ConstantThrustManeuver;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ConstantThrustManeuverTest extends TestCase {
  
  public void testRoughBehaviour() throws DerivativeException, IntegratorException, OrekitException, ParseException {
    double mu =  3.9860064e+14;
    double isp = 318;
    double mass = 2500;
    double a = 24396159;
    double e = 0.72831215;
    double i = Math.toRadians(7);
    double omega = Math.toRadians(180);
    double OMEGA = Math.toRadians(261);
    double lv = 0;

    double duration = 3653.99;
    double f = 420;
    double delta = Math.toRadians(-7.4978);
    double alpha = Math.toRadians(351);
    
    Vector3D dir = new Vector3D ( Math.cos(alpha)*Math.cos(delta),
                                  Math.cos(alpha)*Math.sin(delta),
                                  Math.sin(delta));
       
    OrbitalParameters transPar = new KeplerianParameters(a, e, i,
                                                     omega, OMEGA,
                                                     lv, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());
    
    AbsoluteDate initDate = new AbsoluteDate("2004-01-01T23:30:00.000" , UTCScale.getInstance());
    AbsoluteDate fireDate = new AbsoluteDate("2004-01-02T04:15:34.080" , UTCScale.getInstance());
    
    SpacecraftState transOrb = new SpacecraftState(new Orbit(initDate, transPar), mass);
    
    ConstantThrustManeuver man = new ConstantThrustManeuver(fireDate,
                                                      duration, f, isp, dir , ConstantThrustManeuver.INERTIAL);
    GraggBulirschStoerIntegrator gragg = new GraggBulirschStoerIntegrator(1e-50, 1000, 0, 1e-08);

    NumericalPropagator pro = new NumericalPropagator(mu, gragg);                     
    
    pro.addForceModel(man);
    
    SpacecraftState finalorb = pro.propagate(transOrb, new AbsoluteDate(fireDate, 3800));

    assertEquals(2007.882454, finalorb .getMass(), 1e-6);
    assertEquals(2.6792, Math.toDegrees(Utils.trimAngle(finalorb.getI(), Math.PI)), 1e-4);
    assertEquals(28969, finalorb.getA()/1000, 1);
    
  }
  
  public static Test suite() {
    return new TestSuite(ConstantThrustManeuverTest.class);
  }

}
