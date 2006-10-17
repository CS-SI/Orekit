package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;

import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.models.spacecraft.SimpleSpacecraft;
import fr.cs.aerospace.orekit.models.spacecraft.SolarRadiationPressureSpacecraft;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SolarRadiationPressureTest extends TestCase {

	public void testLightning() throws OrekitException, ParseException, DerivativeException, IntegratorException{
	    // Initialization
		AbsoluteDate date = new AbsoluteDate("2000-03-21T13:59:27.816" , UTCScale.getInstance());
	    OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
	    		Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                0.1, 2, Frame.getJ2000());
	    Orbit orbit = new Orbit(date , op);
	    Sun sun = new Sun();
	    OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765);
	    SolarRadiationPressure SRP =  new SolarRadiationPressure(
	    		sun , earth ,
	    	              (SolarRadiationPressureSpacecraft)new SimpleSpacecraft(1500.0, 50.0,
	                            	  0.5, 0.5, 0.5));
        
        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);
        assertEquals(86164, period,1);
	    
		// creation of the propagator
		KeplerianPropagator k = new KeplerianPropagator(orbit, mu);
		
		// intermediate variables
		AbsoluteDate currentDate;
		double changed = 1;
		int count=0;
		
		for(int t=1;t<3*period;t+=1000) {
			currentDate = new AbsoluteDate(date , t);
			try {				

				double ratio = SRP.getLightningRatio(k.getOrbit(currentDate).getPVCoordinates(mu).getPosition(),Frame.getJ2000(), currentDate );
					
				if(Math.floor(ratio)!=changed) {
					changed = Math.floor(ratio);
					if(changed == 0) {
						count++;
					}
				}			
			} catch (OrekitException e) {
			e.printStackTrace();
			}
		}
		assertTrue(3==count);
	} 
	
	public void testRoughOrbitalModifs() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {
		
		// initialization
		AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
	    OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
	    		Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                0.1, 2, Frame.getJ2000());
	    Orbit orbit = new Orbit(date , op);
	    Sun sun = new Sun();
	    
	    // creation of the force model
		SolarRadiationPressure SRP =  new SolarRadiationPressure(
	    		sun , new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765),
	    	              (SolarRadiationPressureSpacecraft)new SimpleSpacecraft(1500.0, 500.0,
	                            	  0.7, 0.7, 0.7));
		
		double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);
        
        assertEquals(86164, period,1);
		// creation of the propagator
		FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period/4, 0, 10e-5);
		NumericalPropagator calc = new NumericalPropagator(mu, integrator);
		calc.addForceModel(SRP);
		
		// Step Handler

		SolarStepHandler sh = new SolarStepHandler();
        AbsoluteDate finalDate = new AbsoluteDate(date , 365*period);
		calc.propagate(orbit , finalDate, Math.floor(15*period), sh );
		
	}
	
	public void checkRadius(double radius , double min , double max) {
		assertTrue(radius >= min);
		assertTrue(radius <= max);
	}
	
	private double mu = 3.98600E14;
	  	
	private class SolarStepHandler implements FixedStepHandler {

		private SolarStepHandler() {
		}
		
		public void handleStep(double t, double[]y, boolean isLastStep) {
			double radius = Math.sqrt((y[1]-0.00940313)*(y[1]-0.00940313) 
	        		+ (y[2]-0.013679)*(y[2]-0.013679));
	        checkRadius(radius , 0.00351 , 0.00394);
		}


	}  	  

	public static Test suite() {
		return new TestSuite(SolarRadiationPressureTest.class);
	}

}
