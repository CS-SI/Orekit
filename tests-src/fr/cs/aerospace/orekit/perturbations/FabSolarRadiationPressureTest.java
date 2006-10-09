package fr.cs.aerospace.orekit.perturbations;

import java.text.ParseException;

import models.bodies.Sun;
import models.satellite.SimpleSpacecraft;
import models.satellite.SolarRadiationPressureSatellite;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.CartesianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import fr.cs.aerospace.orekit.utils.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FabSolarRadiationPressureTest extends TestCase {

	public void testLightning() throws OrekitException, ParseException{
	
		double mu = 3.98600E14;    
	    AbsoluteDate date = new AbsoluteDate("2000-01-01T13:59:27.816" , UTCScale.getInstance());
    	    
	    Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
	    position.multiplySelf(0.5);
	    Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
	    position.negateSelf();
	    velocity.negateSelf();
	    PVCoordinates pv = new PVCoordinates(position, velocity);
	    OrbitalParameters op = new CartesianParameters(pv , Frame.getJ2000() , mu);
	    Orbit orbit = new Orbit(date , op);
	    Sun sun = new Sun();
	    FabSolarRadiationPressure SRP =  new FabSolarRadiationPressure(
	    		sun , new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765),
	    	              (SolarRadiationPressureSatellite)new SimpleSpacecraft(1500.0, 50.0,
	                            	  0.5, 0.5, 0.5));
	    Vector3D SatSun = Vector3D.subtract(sun.getPosition(date, Frame.getJ2000()), pv.getPosition());
	
        KeplerianPropagator k = new KeplerianPropagator(orbit , mu);
	    
	    for (int i = 0 ; i<1000; i++ ) {
	    	date = new AbsoluteDate(date , 10);
	    	if (i==836){
	    		for (int j = 0 ;j<100;j++){
	    			date = new AbsoluteDate(date , 1);
		    	System.out.println();

		    	System.out.println(" date    : " + date);
		    	pv = k.getOrbit(date).getPVCoordinates(mu);
		    	
		    	SatSun = Vector3D.subtract(sun.getPosition(date, Frame.getJ2000()), pv.getPosition());
		    	System.out.println(" pv pos : " + Vector.toString(pv.getPosition()));
		    	System.out.println(" sunpos : " +Vector.toString(sun.getPosition(date, Frame.getJ2000())));
		    	System.out.println(" satsun : " +Vector.toString(SatSun));
		    	System.out.println(" i =  " + i + " ligth ratio =  " + SRP.getLightningRatio(pv.getPosition() , SatSun));
	    		}
	    	}

	    }
  
	} 
	    	    
	  public static Test suite() {
		  return new TestSuite(FabSolarRadiationPressureTest.class);
	  }
}
