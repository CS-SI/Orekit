package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.SimpleSpacecraft;
import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.bodies.Sun;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.orbits.CartesianDerivativesAdder;
import fr.cs.aerospace.orekit.orbits.CartesianParameters;
import fr.cs.aerospace.orekit.orbits.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.*;

public class SolarRadiationPressureTest extends TestCase {

    public SolarRadiationPressureTest(String name) {
    super(name);
  }
    
    
public void testSolarRadiationPressure() throws OrekitException{
    
//    double equatorialRadius = 6378.13E3;
    double mu = 3.98600E14;    
    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0);
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
    Attitude attitude = new Attitude();
    OrbitalParameters op = new CartesianParameters();
    op.reset(position, velocity, mu);
    OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
       
    // Acceleration initialisation
//    double xDotDot = 0;
//    double yDotDot = 0;
//    double zDotDot = 0;
        
    // Creation of the solar radiation pressure model
    SolarRadiationPressure SRP =
      new SolarRadiationPressure(new Sun(),
                                 new OneAxisEllipsoid(6378136.46,
                                                      1.0 / 298.25765),
                                 new SimpleSpacecraft(1500.0, 50.0,
                                                      0.5, 0.5, 0.5));
    
    // Add the pressure contribution to the acceleration
    SRP.addContribution(date, position, velocity, attitude, adder);

  } 
    
   public void testSolarRadiationPressureElements() throws OrekitException {
    //----------------------------------

//    double equatorialRadius = 6378.13E3;
//    double mu = 3.98600E14;
    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0);
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//    Attitude attitude = new Attitude();
    
    // Testing the definition of SolarRadiationPressure
    System.out.println("Testing creation");
    System.out.println("================");
    SolarRadiationPressure SRP =
      new SolarRadiationPressure(new Sun(),
                                 new OneAxisEllipsoid(6378136.46,
                                                      1.0 / 298.25765),
                                 new SimpleSpacecraft(1500.0, 50.0,
                                                      0.5, 0.5, 0.5));
    SWF[] testswf = SRP.getSwitchingFunctions();
    System.out.println("First switching function= " + testswf[0]);
    System.out.println("Second switching function= " + testswf[1]);
    
//    // Testing the retrieval of satsun vector
//    System.out.println("");
//    System.out.println("Testing the calculation of satsun vector");
//    System.out.println("========================================");
//    Vector3D satSunVector = SRP.getSatSunVector(date, position);
//    System.out.println("satSunVector(x,y,z)= (" + satSunVector.getX() + ", " + 
//    satSunVector.getX() + ", " + satSunVector.getX() + ")");
//
//    // Testing the retrieval of satsun/satcentralbody angle
//    System.out.println("");
//    System.out.println("Testing the calculation of satsun vector");
//    System.out.println("========================================");
//    double angle = SRP.getSatSunSatCentralAngle(date, position);
//    System.out.println("Sat-Sun / Sat-Central Body angle= " + angle);
        
    // Testing the retrieval of switching functions
    System.out.println("");
    System.out.println("Testing the retrieval of switching functions");
    System.out.println("============================================");
    SWF[] switches = SRP.getSwitchingFunctions();
    System.out.println("g de Switchingfunction 1= " + switches[0].g(date, position, velocity));
    System.out.println("g de Switchingfunction 2= " + switches[1].g(date, position, velocity));
  }
    
  public static Test suite() {
    return new TestSuite(SolarRadiationPressureTest.class);
  }

}
