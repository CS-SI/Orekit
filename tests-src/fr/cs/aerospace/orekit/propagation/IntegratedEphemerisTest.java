package fr.cs.aerospace.orekit.propagation;

import java.io.FileNotFoundException;
import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IntegratedEphemerisTest extends TestCase {
	
	public void testNormalKeplerIntegration() throws DerivativeException, IntegratorException, OrekitException, FileNotFoundException {
	      
		  // Definition of initial conditions with position and velocity

	      Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
	      Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
	      double mu = 3.9860047e14;
	      
	      AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
	      SpacecraftState initialOrbit =
	        new SpacecraftState(new Orbit(initDate, 
	                  new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu)));
	      
	      // Keplerian propagator definition

	      KeplerianPropagator keplerEx = new KeplerianPropagator(initialOrbit, mu);
		
		 // Numerical propagator definition
	     
		FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 86400, 0, 10e-13);
		NumericalPropagator numericEx = new NumericalPropagator(mu, integrator);
			
		// Integrated ephemeris

		IntegratedEphemeris ephemeris = new IntegratedEphemeris();
		
		// Propagation
		
		AbsoluteDate finalDate = new AbsoluteDate(initDate , 86400);
		numericEx.propagate(initialOrbit , finalDate , ephemeris );
		SpacecraftState keplerIntermediateOrbit;
		SpacecraftState numericIntermediateOrbit;
		AbsoluteDate intermediateDate;
//		// Print
//		
//		FileOutputStream out = new FileOutputStream("/home/fab/testNumericGeostat.txt");
//		PrintWriter print = new PrintWriter(out);
//				
		// tests 
		
		for (int i = 1; i<=86400; i++) {
			intermediateDate = new AbsoluteDate(initDate , i);
			keplerIntermediateOrbit = keplerEx.getSpacecraftState(intermediateDate);
			numericIntermediateOrbit = ephemeris.getSpacecraftState(intermediateDate);

			Vector3D test = Vector3D.subtract(keplerIntermediateOrbit.getPVCoordinates(mu).getPosition(),
					numericIntermediateOrbit.getPVCoordinates(mu).getPosition());
			assertEquals(0, test.getNorm(), 10e-2);
//			print.print(i + " ");
//			print.print(test.getNorm());
//			print.println();
		}
//		print.close();
		
//		 out = new FileOutputStream("/home/fab/testPrecision.txt");
//		 print = new PrintWriter(out);
				
		// tests 
		
//		AbsoluteDate intermediateDate = new AbsoluteDate(initDate , 86400/2);
//		Orbit keplerIntermediateOrbit = keplerEx.getOrbit(intermediateDate);
//		
//		for (int i = 1; i<=13; i+=1) {
//			double order = Math.pow(10 , -i);
//			integrator = new GraggBulirschStoerIntegrator(1, 86400, 0, order);
//			numericEx = new NumericalPropagator(mu, integrator);
//			
//			Orbit numericIntermediateOrbit = new Orbit(); 
//			numericEx.propagate(initialOrbit , intermediateDate, numericIntermediateOrbit);
//
//			Vector3D test = Vector3D.subtract(keplerIntermediateOrbit.getPVCoordinates(mu).getPosition(),
//					numericIntermediateOrbit.getPVCoordinates(mu).getPosition());
//			//assertEquals(0, test.getNorm(), 10e-1);
//			print.print(i + " ");
//			print.print(test.getNorm());
//			print.println();
//		}
//		print.close();
						
		
		// test inv
		intermediateDate = new AbsoluteDate(initDate , 41589);
		keplerIntermediateOrbit = keplerEx.getSpacecraftState(intermediateDate);
		initialOrbit = keplerEx.getSpacecraftState(finalDate);
		numericEx.propagate(initialOrbit , initDate , ephemeris );
		numericIntermediateOrbit = ephemeris.getSpacecraftState(intermediateDate);

		Vector3D test = Vector3D.subtract(keplerIntermediateOrbit.getPVCoordinates(mu).getPosition(),
				numericIntermediateOrbit.getPVCoordinates(mu).getPosition());
		assertEquals(0, test.getNorm(), 10e-2);
		
	}
	
	
	public static Test suite() {
	    return new TestSuite(IntegratedEphemerisTest.class);
	}
}
