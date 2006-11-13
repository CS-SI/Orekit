package fr.cs.aerospace.orekit.maneuvers;

import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.ClassicalRungeKuttaIntegrator;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.forces.maneuver.ConstantThrustManeuver;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.spacecraft.ManeuverSpacecraft;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.SimpleSpacecraft;
import junit.framework.TestCase;


public class ConstantThrustManeuverTest extends TestCase {
  public void testRoughBehaviour() throws DerivativeException, IntegratorException, OrekitException {
    
    double isp = 307;
    double g0 = 9.81;
    double mass = 1300;
    double rp = 20000000;
    double ra = 42164000;
    double a = (rp+ra)/2.0;
    double e = (ra-rp)/(ra+rp);
    double piOn4 = Math.PI/4;
    double deltaV = Math.sqrt(mu/ra)-Math.sqrt(2*mu*rp/(ra*(ra+rp)));    
    double duration = 15.0;
    double deltaM = mass*(1-Math.exp(-deltaV/(g0*isp)));
    double outflow = -deltaM/duration;
    
    ManeuverSpacecraft craft = new SimpleSpacecraft(mass, 0, 0, 0, 0, isp, outflow, g0);
    
    OrbitalParameters transPar = new KeplerianParameters(a, e, piOn4,
                                                     piOn4, piOn4,
                                                     piOn4, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());
    
    AbsoluteDate startDate = new AbsoluteDate(AbsoluteDate.J2000Epoch);
    
    Orbit transOrb = new Orbit(startDate, transPar,  mass );
    
    OrbitalParameters geoParam = new KeplerianParameters(a, 0.001, piOn4,
                                        piOn4, piOn4,
                                        piOn4, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());
        
    Orbit apo = determineApogee(transOrb);
    
    ConstantThrustManeuver man = new ConstantThrustManeuver(apo.getDate(),
                                                         duration, craft, new Vector3D(1,0,0));
    
    NumericalPropagator pro = new NumericalPropagator(mu,
                 new GraggBulirschStoerIntegrator(1.0e-60, 1, 10000, 10000));
//                                            new ClassicalRungeKuttaIntegrator(1));                        
    pro.addForceModel(man);
    
    Orbit finalorb = pro.propagate(transOrb, new AbsoluteDate(apo.getDate(), 1000));
    System.out.println("trans : "+geoParam.getA() + "   " + transPar.getE());
    System.out.println("geo : "+geoParam.getA() + "   " + geoParam.getE());
    System.out.println("result : "+finalorb.getA() + "   " + finalorb.getE());
    System.out.println("mass : "+finalorb.getMass());
     
  }
  
  public Orbit determineApogee(Orbit orb) throws PropagationException {
    KeplerianPropagator k = new KeplerianPropagator(orb, mu);
    double period = 2*Math.PI*Math.sqrt(orb.getA()*orb.getA()*orb.getA()/mu);
    Orbit resultOrb= new Orbit(orb);
    for(int t=0;t<period;t++) {
      AbsoluteDate interDate = new AbsoluteDate(orb.getDate(), t);
      Orbit interOrb = k.getOrbit(interDate); 
      if(interOrb.getPVCoordinates(mu).getVelocity().getNorm()<=
            resultOrb.getPVCoordinates(mu).getVelocity().getNorm()) {
        resultOrb = interOrb;
      }
    }
    return resultOrb;
  }
  
  private double mu =  3.986004415e+14;
}
