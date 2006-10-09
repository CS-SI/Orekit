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
import junit.framework.*;

public class SolarRadiationPressureTest extends TestCase {

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
	    SolarRadiationPressure SRP =  new SolarRadiationPressure(
	    		sun , new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765),
	    		(SolarRadiationPressureSatellite)new SimpleSpacecraft(1500.0, 50.0,
                  	  0.5, 0.5, 0.5));
	    Vector3D SatSun = Vector3D.subtract(sun.getPosition(date, Frame.getJ2000()), pv.getPosition());
	
        KeplerianPropagator k = new KeplerianPropagator(orbit , mu);
	    
	    for (int i = 0 ; i<1000; i++ ) {
	    	date = new AbsoluteDate(date , 10);
//	    	if (i==836){
//	    		for (int j = 0 ;j<100;j++){
	    			date = new AbsoluteDate(date , 1);
		    	System.out.println();

		    	System.out.println(" date    : " + date);
		    	pv = k.getOrbit(date).getPVCoordinates(mu);
		    	
		    	SatSun = Vector3D.subtract(sun.getPosition(date, Frame.getJ2000()), pv.getPosition());
		    	System.out.println(" pv pos : " + Vector.toString(pv.getPosition()));
		    	System.out.println(" sunpos : " +Vector.toString(sun.getPosition(date, Frame.getJ2000())));
		    	System.out.println(" satsun : " +Vector.toString(SatSun));
		    	System.out.println(" i =  " + i + " ligth ratio =  " + SRP.getLightningRatio(date , pv.getPosition() , SatSun));
//	    		}
//	    	}

	    }
	}
//    public SolarRadiationPressureTest(String name) {
//    super(name);
//  }
//    
//    
//public void aatestSolarRadiationPressure() throws OrekitException{
//    
////    double equatorialRadius = 6378.13E3;
//    double mu = 3.98600E14;    
//    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0);
//    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//    OrbitalParameters op = new CartesianParameters();
//    op.reset(new PVCoordinates(position, velocity),Frame.getJ2000(), mu);
//    OrbitDerivativesAdder adder = op.getDerivativesAdder(mu);
//       
    // Acceleration initialisation
//    double xDotDot = 0;
//    double yDotDot = 0;
//    double zDotDot = 0;
        
    // Creation of the solar radiation pressure model
    // TODO fab commented it  SolarRadiationPressure SRP =
   // TODO fab commented it   new SolarRadiationPressure(new Sun(),
    	 // TODO fab commented it         ew OneAxisEllipsoid(6378136.46,
    	 // TODO fab commented it                          1.0 / 298.25765),
    	 // TODO fab commented it              new SimpleSpacecraft(1500.0, 50.0,
    	 // TODO fab commented it              0.5, 0.5, 0.5));
    
    // Add the pressure contribution to the acceleration
   // TODO fab commented it  SRP.addContribution(date, new PVCoordinates(position, velocity), attitude, adder);

////  } 
////    
////   public void testSolarRadiationPressureElements() throws OrekitException {
////    //----------------------------------
//
////    double equatorialRadius = 6378.13E3;
////    double mu = 3.98600E14;
//    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0);
//    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
////    Attitude attitude = new Attitude();
//    
//    // Testing the definition of SolarRadiationPressure
//    System.out.println("Testing creation");
//    System.out.println("================");
//	 // TODO fab commented it:
//    /*   SolarRadiationPressure SRP =
//    	new SolarRadiationPressure(new Sun(),
//                                 new OneAxisEllipsoid(6378136.46,
//                                                      1.0 / 298.25765),
//                                 new SimpleSpacecraft(1500.0, 50.0,
//                                                      0.5, 0.5, 0.5));
//    SWF[] testswf = SRP.getSwitchingFunctions();
//    System.out.println("First switching function= " + testswf[0]);
//    System.out.println("Second switching function= " + testswf[1]);*/
//    
////    // Testing the retrieval of satsun vector
////    System.out.println("");
////    System.out.println("Testing the calculation of satsun vector");
////    System.out.println("========================================");
////    Vector3D satSunVector = SRP.getSatSunVector(date, position);
////    System.out.println("satSunVector(x,y,z)= (" + satSunVector.getX() + ", " + 
////    satSunVector.getX() + ", " + satSunVector.getX() + ")");
////
////    // Testing the retrieval of satsun/satcentralbody angle
////    System.out.println("");
////    System.out.println("Testing the calculation of satsun vector");
////    System.out.println("========================================");
////    double angle = SRP.getSatSunSatCentralAngle(date, position);
////    System.out.println("Sat-Sun / Sat-Central Body angle= " + angle);
//        
//    // Testing the retrieval of switching functions
//    System.out.println("");
//    System.out.println("Testing the retrieval of switching functions");
//    System.out.println("============================================");
//    //TODO fab comented it:
//    /*SWF[] switches = SRP.getSwitchingFunctions();
//    System.out.println("g de Switchingfunction 1= " + switches[0].g(date, new PVCoordinates(position, velocity)));
//    System.out.println("g de Switchingfunction 2= " + switches[1].g(date, new PVCoordinates(position, velocity)));*/
//  }
//    
  public static Test suite() {
    return new TestSuite(SolarRadiationPressureTest.class);
  }

}
