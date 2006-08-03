package fr.cs.aerospace.orekit.propagation;

import java.io.IOException;


import junit.framework.*;
import org.spaceroots.mantissa.ode.DormandPrince853Integrator;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.IntegratorException;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.bodies.FixedPoleEarth;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.perturbations.CunninghamAttractionModel;
import fr.cs.aerospace.orekit.perturbations.PotentialCoefficientsTab;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

public class NumericalPropagatorTest extends TestCase {

    public NumericalPropagatorTest(String name) {
    super(name);
  }

  public void aaatestNoExtrapolation()
    throws DerivativeException, IntegratorException {
    
    try {
    // Definition of initial conditions
    //--------------------------------
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    Orbit initialOrbit =
      new Orbit(new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0),
                new EquinoctialParameters(position, velocity, mu));
    
    
    // Extrapolator definition
    // -----------------------
    NumericalPropagator extrapolator =
      new NumericalPropagator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                1.0e-8, 1.0e-8));
    
    // Extrapolation of the initial at the initial date
    // ------------------------------------------------
    Orbit finalOrbit = extrapolator.extrapolate(initialOrbit,
                                                initialOrbit.getDate(),
                                                (Orbit) null);
    // Initial orbit definition
    Vector3D initialPosition = initialOrbit.getPosition(mu);
    Vector3D initialVelocity = initialOrbit.getVelocity(mu);
    
    // Final orbit definition
    Vector3D finalPosition   = finalOrbit.getPosition(mu);
    Vector3D finalVelocity   = finalOrbit.getVelocity(mu);
    
    // Testing and printing the discrepancies
    // --------------------------------------
    assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
    assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
    assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
    assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
    assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
    assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);
    
//    System.out.println("Initial orbit at t = " + initialOrbit.getDate());
//    System.out.println("x = " + initialPosition.getX());
//    System.out.println("y = " + initialPosition.getY());
//    System.out.println("z = " + initialPosition.getZ());
//    System.out.println("vx = " + initialVelocity.getX());
//    System.out.println("vy = " + initialVelocity.getY());
//    System.out.println("vz = " + initialVelocity.getZ());  
//    System.out.println("Final position at t = " + finalOrbit.getDate());
//    System.out.println("x = " + finalPosition.getX());
//    System.out.println("y = " + finalPosition.getY());
//    System.out.println("z = " + finalPosition.getZ());
//    System.out.println("vx = " + finalVelocity.getX());
//    System.out.println("vy = " + finalVelocity.getY());
//    System.out.println("vz = " + finalVelocity.getZ());
    } catch (OrekitException oe) {System.err.println(oe.getMessage());}
  }
  
  public void testKepler()
    throws DerivativeException, IntegratorException {
        
try {
    // Definition of initial conditions
    // --------------------------------
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    Orbit initialOrbit =
      new Orbit(new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0),
                new EquinoctialParameters(position,  velocity, mu));
    
    // Extrapolator definition
    // -----------------------
    NumericalPropagator extrapolator =
      new NumericalPropagator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                1.0e-8, 1.0e-8));
    double dt = 3200;
    
    // Extrapolation of the initial at t+dt
    // ------------------------------------
    Orbit finalOrbit = extrapolator.extrapolate(initialOrbit,
                                               new AbsoluteDate(initialOrbit.getDate(),
                                                         dt), (Orbit) null);
    // Testing the discrepancies
    // -------------------------
//    System.out.println("Initial orbit at t = " + initialOrbit.getDate());
//    System.out.println("a = " + initialOrbit.getA());
//    System.out.println("e = " + initialOrbit.getE());
//    System.out.println("i = " + initialOrbit.getI());
//    System.out.println("Final position at t = " + finalOrbit.getDate());
//    System.out.println("a = " + finalOrbit.getA());
//    System.out.println("e = " + finalOrbit.getE());
//    System.out.println("i = " + finalOrbit.getI());
    assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
    assertEquals(initialOrbit.getE(),    finalOrbit.getE(),    1.0e-10);
    assertEquals(initialOrbit.getI(),    finalOrbit.getI(),    1.0e-10);
    
    // TODO 
//    assertEquals(initialOrbit.getPA(),   finalOrbit.getPA(),   1.0e-10);
//    assertEquals(initialOrbit.getRAAN(), finalOrbit.getRAAN(), 1.0e-10);
//    double n = Math.sqrt(mu / initialOrbit.getA()) / initialOrbit.getA();
//    assertEquals(initialOrbit.getMeanAnomaly() + n * dt,
//                 finalOrbit.getMeanAnomaly(), 4.0e-10);
} catch (OrekitException oe) {System.err.println(oe.getMessage());}
  }
   
  public void aaatestExtrapolatorWithPotential()
    throws DerivativeException, IntegratorException {
   try{
    // Definition of initial conditions
    // --------------------------------
    Vector3D position = new Vector3D(1.0e7, 0.0, 0.0);
    Vector3D velocity = new Vector3D(0.0, 8000.0, 0.0);
    //Attitude attitude = new Attitude();
    double mu = 3.986e14;
    double equatorialRadius = 6378.13E3;
        
    
    Orbit initialOrbit =
      new Orbit(new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0),
                new EquinoctialParameters(position,  velocity, mu));
    System.out.println("Initial orbit at t = " + initialOrbit.getDate());
    System.out.println("a = " + initialOrbit.getA());
    System.out.println("e = " + initialOrbit.getE());
    System.out.println("i = " + initialOrbit.getI());
    
    // Extrapolator definition
    // -----------------------
    NumericalPropagator extrapolator =
      new NumericalPropagator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                1.0e-8, 1.0e-8));
    double dt = 60;
    
    // Extrapolation of the initial at t+dt
    // ------------------------------------
    // Add Forces

    PotentialCoefficientsTab GEM10Tab = 
    new PotentialCoefficientsTab("D:\\Mes Documents\\EDelente\\JAVA\\GEM10B.txt");
    
    GEM10Tab.read();
    int ndeg = GEM10Tab.getNdeg();
    double[] J   = new double[ndeg];
    double[][] C = new double[ndeg][ndeg];
    double[][] S = new double[ndeg][ndeg];
    
    C = GEM10Tab.getNormalizedClm();
    S = GEM10Tab.getNormalizedSlm();

    J[0] = 0.0;
    J[1] = 0.0;
    for (int i = 2; i < ndeg; i++) {
        J[i] = - C[i][0];
    }
    
    CunninghamAttractionModel CBP =
      new CunninghamAttractionModel(mu, new FixedPoleEarth(), equatorialRadius, C, S);
    
    extrapolator.addForceModel(CBP);
    
    Orbit finalOrbit = extrapolator.extrapolate(initialOrbit,
                                               new AbsoluteDate(initialOrbit.getDate(),
                                                         dt), (Orbit) null);
    // Testing the discrepancies
    // -------------------------
    assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
    assertEquals(initialOrbit.getE(),    finalOrbit.getE(),    1.0e-10);
    assertEquals(initialOrbit.getI(),    finalOrbit.getI(),    1.0e-10);
    // TODO a voir ...
//    assertEquals(initialOrbit.getPA(),   finalOrbit.getPA(),   1.0e-10);
//    assertEquals(initialOrbit.getRAAN(), finalOrbit.getRAAN(), 1.0e-10);
//    double n = Math.sqrt(mu / initialOrbit.getA()) / initialOrbit.getA();
//    assertEquals(initialOrbit.getMeanAnomaly() + n * dt,
//                 finalOrbit.getMeanAnomaly(), 4.0e-10);
   }  catch (OrekitException oe) {
     System.err.println(oe.getMessage());
   }  catch (IOException ioe) {
     System.err.println(ioe.getMessage());
   }
  }
  
  
  public static Test suite() {
    return new TestSuite(NumericalPropagatorTest.class);
  }

}

