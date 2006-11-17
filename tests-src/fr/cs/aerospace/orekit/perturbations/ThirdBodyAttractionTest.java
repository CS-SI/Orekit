package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.perturbations.ThirdBodyAttraction;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.bodies.Moon;
import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ThirdBodyAttractionTest extends TestCase {
  
  public void testSunContrib() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {
    
    // initialization
    AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                     Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                     0.1, 2, Frame.getJ2000());
    Orbit orbit = new Orbit(date , op);
    Sun sun = new Sun();
    
    // creation of the force model
    ThirdBodyAttraction TBA =  new ThirdBodyAttraction(sun);
    
    double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
    NumericalPropagator calc = new NumericalPropagator(mu, integrator);
    calc.addForceModel(TBA);
    
    // Step Handler
    
    TBAStepHandler sh = new TBAStepHandler(TBAStepHandler.SUN);
    AbsoluteDate finalDate = new AbsoluteDate(date , 2*365*period);
    calc.propagate(new SpacecraftState(orbit) , finalDate, Math.floor(period), sh );
    
  }
  public void testMoonContrib() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {
    
    // initialization
    AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                     Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                     0.1, 2, Frame.getJ2000());
    Orbit orbit = new Orbit(date , op);
    Moon moon = new Moon();
    
    // creation of the force model
    ThirdBodyAttraction TBA =  new ThirdBodyAttraction(moon);
    
    double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
    NumericalPropagator calc = new NumericalPropagator(mu, integrator);
    calc.addForceModel(TBA);
    
    // Step Handler
    
    TBAStepHandler sh = new TBAStepHandler(TBAStepHandler.MOON);
    AbsoluteDate finalDate = new AbsoluteDate(date , 365*period);
    calc.propagate(new SpacecraftState(orbit) , finalDate, Math.floor(period), sh );
    
  }
  
  private double mu = 3.98600E14;
  
  private class TBAStepHandler implements FixedStepHandler {
    
    public static final int MOON = 1;
    public static final int SUN = 2;
    public static final int SUNandMOON = 3;
    private int type;
    
    private TBAStepHandler(int type) throws FileNotFoundException {
      this.type = type;
    }
    
    public void handleStep(double t, double[]y, boolean isLastStep) {
      if (type == MOON) {
        assertEquals(0, xMoon(t)-y[3], 1e-4);
        assertEquals(0, yMoon(t)-y[4], 1e-4);
      }
      if (type == SUN) {
        assertEquals(0, xSun(t)-y[3], 1e-4);
        assertEquals(0, ySun(t)-y[4], 1e-4);
      }
      if (type == SUNandMOON) {
        
      }
    }
    
    private double xMoon(double t) {
      return -0.909227e-3 - 0.309607e-10 * t + 2.68116e-5 * 
      Math.cos(5.29808e-6*t) - 1.46451e-5 * Math.sin(5.29808e-6*t);
    }
    
    private double yMoon(double t) {
      return 1.48482e-3 + 1.57598e-10 * t + 1.47626e-5 * 
      Math.cos(5.29808e-6*t) - 2.69654e-5 * Math.sin(5.29808e-6*t);
    }
    
    private double xSun(double t) {
      return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 * 
      Math.cos(3.9820426e-7*t) - 7.59983e-5 * Math.sin(3.9820426e-7*t);
    }
    
    private double ySun(double t) {
      return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 * 
      Math.cos(3.9820426e-7*t) + 17.6083e-5 * Math.sin(3.9820426e-7*t);
    }
    
  }     
  
  
  public static Test suite() {
    return new TestSuite(ThirdBodyAttractionTest.class);
  }
}
