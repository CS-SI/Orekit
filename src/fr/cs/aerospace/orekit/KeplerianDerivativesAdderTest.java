package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.*;

public class KeplerianDerivativesAdderTest extends TestCase {

    private double T;
    private double N;
    private double W;
    
  public KeplerianDerivativesAdderTest(String name) {
    super(name);
  }

  public void testKepler() {
    double[] yDot = new double[6];
    adder.initDerivatives(yDot);
    adder.addKeplerContribution();
    for (int i = 0; i < 5; ++i) {
      assertEquals(0, yDot[i], 1.0e-12);
    }
    assertEquals(9.8592598399357e-4, yDot[5], 1.0e-17);
  }
  
  public void testchainingGaussTNW() {
    for (int ntest = 0; ntest < 8; ntest ++) {
        defineTNW(ntest);
        aaatestGaussTNW(T, N, W);
    }
  }
  
   public void defineTNW(int ntest) {
      if (ntest == 0) {
           T = 0.0;
           N = 0.0;
           W = 0.0;
       }
       if (ntest == 1) {
           T = 10.0;
           N = 0.0;
           W = 0.0;
       }
       if (ntest == 2) {
           T = - 10.0;
           N = 0.0;
           W = 0.0;
       }
       if (ntest == 3) {
           T = 0.0;
           N = 10.0;
           W = 0.0;
       }
       if (ntest == 4) {
           T = 0.0;
           N = - 10.0;
           W = 0.0;
       } 
       if (ntest == 5) {
           T = 0.0;
           N = 0.0;
           W = 10.0;
       }
       if (ntest == 6) {
           T = 0.0;
           N = 0.0;
           W = - 10.0;
       }
       if (ntest == 7) {
           T = 0.3;
           N = -0.08;
           W = 0.05;
       }
      
  }
   
  
  public void aaatestGaussTNW(double T, double N, double W) {

    double[] yD1 = new double[6];
    adder.initDerivatives(yD1);

    //double T =  0.1;
    //double N = -0.08;
    //double W =  0.03;
    adder.addTNWAcceleration(T, N, W);
    adder.addKeplerContribution();

    // reference derivatives
    CartesianParameters cartesianParameters =
      new CartesianParameters(orbit.getPosition(mu),
                              orbit.getVelocity(mu),
                              mu);
    OrbitDerivativesAdder cartesianAdder =
      cartesianParameters.getDerivativesAdder(mu);
    double[] yD2 = new double[6];
    cartesianAdder.initDerivatives(yD2);
    cartesianAdder.addTNWAcceleration(T, N, W);
    cartesianAdder.addKeplerContribution();

    checkWithCartesian(yD1, yD2, cartesianParameters);
  }

  public void testGaussQSW() {
    double[] yD1 = new double[6];
    adder.initDerivatives(yD1);

    double Q =  0.1;
    double S = -0.08;
    double W =  0.03;
    adder.addQSWAcceleration(Q, S, W);

    // reference derivatives
    CartesianParameters cartesianParameters =
      new CartesianParameters(orbit.getPosition(mu),
                              orbit.getVelocity(mu),
                              mu);
    OrbitDerivativesAdder cartesianAdder =
      cartesianParameters.getDerivativesAdder(mu);
    double[] yD2 = new double[6];
    cartesianAdder.initDerivatives(yD2);
    cartesianAdder.addQSWAcceleration(Q, S, W);

    checkWithCartesian(yD1, yD2, cartesianParameters);

  }
  
  private void checkWithCartesian(double[] yD1, double[] yD2,
                                  CartesianParameters cartesianParameters) {

    double dt = 0.00001;
    Vector3D oldP = cartesianParameters.getPosition();
    Vector3D p = new Vector3D(oldP.getX() + dt * (yD2[0] + 0.5 * dt * yD2[3]), 
                              oldP.getY() + dt * (yD2[1] + 0.5 * dt * yD2[4]), 
                              oldP.getZ() + dt * (yD2[2] + 0.5 * dt * yD2[5]));
    Vector3D oldV = cartesianParameters.getVelocity();
    Vector3D v = new Vector3D(oldV.getX() + dt * yD2[3], 
                              oldV.getY() + dt * yD2[4],
                              oldV.getZ() + dt * yD2[5]);
    KeplerianParameters newOrbit = new KeplerianParameters(p, v, mu);

    //
    //System.out.println("excentricité t+dt : " + newOrbit.getE());
    //System.out.println("excentricité t    : " + orbit.getE());
    //System.out.println("anomalie vraie orbit: " + orbit.getTrueAnomaly());
    //System.out.println("anomalie vraie new orbit    : " + newOrbit.getTrueAnomaly());
    //
    System.out.println("Validation Keplerien");
    System.out.println("da/dt    : " + ((newOrbit.getA() - orbit.getA()) / dt) + " " + yD1[0]);
    System.out.println("de/dt    : " + ((newOrbit.getE() - orbit.getE()) / dt) + " " + yD1[1]);
    System.out.println("di/dt    : " + ((newOrbit.getI() - orbit.getI()) / dt) + " " + yD1[2]);
    System.out.println("dpa/dt   : " + ((newOrbit.getPA() - orbit.getPA()) / dt) + " " + yD1[3]);
    System.out.println("draan/dt : " + ((newOrbit.getRAAN() - orbit.getRAAN()) / dt) + " " + yD1[4]);
    System.out.println("dv/dt    : " + ((newOrbit.getTrueAnomaly() - orbit.getTrueAnomaly()) / dt) + " " + yD1[5]);
    assertEquals((newOrbit.getA() - orbit.getA()) / dt,
                 yD1[0], 1.0e-9 * Math.abs(orbit.getA()));
    assertEquals((newOrbit.getE() - orbit.getE()) / dt,
                 yD1[1], 1.0e-9 * Math.abs(orbit.getE()));
    assertEquals((newOrbit.getI() - orbit.getI()) / dt,
                 yD1[2], 1.0e-9 * Math.abs(orbit.getI()));
    assertEquals((newOrbit.getPA() - orbit.getPA()) / dt,
                 yD1[3], 1.0e-9 * Math.abs(orbit.getPA()));
    assertEquals((newOrbit.getRAAN() - orbit.getRAAN()) / dt,
                 yD1[4], 1.0e-9 * Math.abs(orbit.getRAAN()));
    assertEquals((newOrbit.getTrueAnomaly() - orbit.getTrueAnomaly()) / dt,
                 yD1[5], 1.0e-9 * Math.abs(orbit.getTrueAnomaly()));

  }

  public void setUp() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    mu = 3.986e14;
    orbit = new KeplerianParameters(position, velocity, mu);
    adder = new KeplerianDerivativesAdder(orbit, mu);
  }
  
  public void tearDown() {
    mu    = Double.NaN;
    orbit = null;
    adder = null;
  }
  
  public static Test suite() {
    return new TestSuite(KeplerianDerivativesAdderTest.class);
  }

  private double mu;
  private KeplerianParameters       orbit;
  private KeplerianDerivativesAdder adder;

}
