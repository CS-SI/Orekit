package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.*;
import fr.cs.aerospace.orekit.perturbations.*;
import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.*;
import java.util.*;

public class DragTest extends TestCase {

    public DragTest(String name) {
    super(name);
  }
   
    public void aaatestDrag() {       
//    //----------------------------------
//
//
//       
//       Drag drag = new Drag(0.0004, 42000.0, 7500.0);
//       System.out.println("rho(5000000m) = : " + drag.getAtmosphere().getRho(5000000.0));
//       System.out.println("hscale = : " + drag.getAtmosphere().getHscale());
//       double equatorialRadius = 6378.13E3;
//       double mu = 3.98600E14;
//       RDate date = new RDate(RDate.J2000Epoch, 0.0);
//
//       Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//       Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//       Attitude attitude = new Attitude();
//
//       OrbitalParameters op = new CartesianParameters();
//       op.reset(position, velocity, mu);
//       OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
//           
//       // Acceleration
//       double xDotDot = 0;
//       double yDotDot = 0;
//       double zDotDot = 0;
//
//     drag.addContribution(date, position, velocity, attitude, adder);
       
  } 

   public void testDragElements() {       
//
//       
//   System.out.println("Test n°1: ");
//   System.out.println("----------");
//   
//   Drag drag = new Drag(0.0004, 42000.0, 7500.0);
//   System.out.println("rho(5000000m) = : " + drag.getAtmosphere().getRho(5000000.0));
//   System.out.println("hscale = : " + drag.getAtmosphere().getHscale());
//   
//   double equatorialRadius = 6378.13E3;
//   double mu = 3.98600E14;
//   RDate date = new RDate(RDate.J2000Epoch, 0.0);
//
//   Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//   Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//   Attitude attitude = new Attitude();
//    
//    // Creation of a simple vehicle
//    SimpleVehicle vehicle = new SimpleVehicle(1500.0, 3.0, 2.0, 0.2, 0.3);
//    System.out.println("Ref coeff = : " + vehicle.getReflCoef());    
//    System.out.println("Drag coeff = : " + vehicle.getDragCoef());
//    
//    // Calculation of rho
//    double x = position.getX();
//    double y = position.getY();
//    double z = position.getZ();
//    double h = position.getNorm() - Constants.CentralBodyradius;
//    double rho = drag.getAtmosphere().getRho(h);
//    
//    // Acceleration
//    double xDotDot = 0;
//    double yDotDot = 0;
//    double zDotDot = 0;
//    
//    // Definition of the SRP force
//    double[] Fsrp = new double[3];
//
//    double halfRhoVSCx = 0.5 * rho * velocity.getNorm() * vehicle.getSurface() * 
//                         vehicle.getDragCoef();
//
//    Fsrp[0] = - halfRhoVSCx * velocity.getX();
//    Fsrp[1] = - halfRhoVSCx * velocity.getY();
//    Fsrp[2] = - halfRhoVSCx * velocity.getZ();
//
//    // Retrieval of the acceleration
//    xDotDot = - Fsrp[0] / vehicle.getMass();
//    yDotDot = - Fsrp[1] / vehicle.getMass();
//    zDotDot = - Fsrp[2] / vehicle.getMass();

   }
   
  public static Test suite() {
    return new TestSuite(DragTest.class);
  }

}
