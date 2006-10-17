package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.bodies.Moon;
import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
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
        calc.propagate(orbit , finalDate, Math.floor(period), sh );
	    
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
      calc.propagate(orbit , finalDate, Math.floor(period), sh );
        
    }
  
	private double mu = 3.98600E14;
  	
    private class TBAStepHandler implements FixedStepHandler {

      public static final int MOON = 1;
      public static final int SUN = 2;
      public static final int SUNandMOON = 3;
      private PrintWriter writer;
      
        private TBAStepHandler(int type) throws FileNotFoundException {
          if (type == MOON) {
            FileOutputStream out = new FileOutputStream("/home/fab/resultMoon.txt");
            writer = new PrintWriter(out);
          }
          if (type == SUN) {
            FileOutputStream out = new FileOutputStream("/home/fab/resultSun.txt");
            writer = new PrintWriter(out);
          }
          if (type == SUNandMOON) {
            FileOutputStream out = new FileOutputStream("/home/fab/resultSunAndMoon.txt");
            writer = new PrintWriter(out);
          }
        }
        
        public void handleStep(double t, double[]y, boolean isLastStep) {
          writer.print(t);
          writer.print(" ");
          writer.print(y[3]);
          writer.print(" ");
          writer.print(y[4]);
          writer.println();
          if(isLastStep) {
            writer.close();
          }
        }


    }     

    
	public static Test suite() {
		return new TestSuite(ThirdBodyAttractionTest.class);
	}
}
