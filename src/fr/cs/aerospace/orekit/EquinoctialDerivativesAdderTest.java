package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.*;

public class EquinoctialDerivativesAdderTest extends TestCase {

  public EquinoctialDerivativesAdderTest(String name) {
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
  
  public void testGaussTNW() {

    double[] yDot = new double[6];
    adder.initDerivatives(yDot);
    
    double T =  0.1;
    double N = -0.08;
    double W =  0.03;
    adder.addTNWAcceleration(T, N, W);

    // reference derivatives
    KeplerianParameters keplerianParameters =
      new KeplerianParameters(orbit.getA(), orbit.getE(),
                              orbit.getI(), orbit.getPA(),
                              orbit.getRAAN(), orbit.getTrueAnomaly());
    OrbitDerivativesAdder keplerianAdder =
      keplerianParameters.getDerivativesAdder(mu);
    double[] yDotKep = new double[6];
    keplerianAdder.initDerivatives(yDotKep);
    keplerianAdder.addTNWAcceleration(T, N, W);

    checkWithKeplerian(yDot, yDotKep, keplerianParameters);

  }

  public void testGaussQSW() {
    double[] yDot = new double[6];
    adder.initDerivatives(yDot);

    double Q =  0.1;
    double S = -0.08;
    double W =  0.03;
    adder.addQSWAcceleration(Q, S, W);

    // reference derivatives
    KeplerianParameters keplerianParameters =
      new KeplerianParameters(orbit.getA(), orbit.getE(),
                              orbit.getI(), orbit.getPA(),
                              orbit.getRAAN(), orbit.getTrueAnomaly());
    OrbitDerivativesAdder keplerianAdder =
      keplerianParameters.getDerivativesAdder(mu);
    double[] yDotKep = new double[6];
    keplerianAdder.initDerivatives(yDotKep);
    keplerianAdder.addQSWAcceleration(Q, S, W);

    checkWithKeplerian(yDot, yDotKep, keplerianParameters);

  }

  private void checkWithKeplerian(double[] yDot, double[] yDotKep,
                                  KeplerianParameters keplerianParameters) {

    double a     = keplerianParameters.getA();
    double e     = keplerianParameters.getE();
    double i     = keplerianParameters.getI();
    double pa    = keplerianParameters.getPA();
    double raan  = keplerianParameters.getRAAN();
    double v     = keplerianParameters.getTrueAnomaly();

    double cosPR = Math.cos(pa + raan);
    double sinPR = Math.sin(pa + raan);
    double cosR  = Math.cos(raan);
    double sinR  = Math.sin(raan);
    double tan2  = Math.tan(i / 2);
    double tansquare = tan2 * tan2;

    System.out.println("Validation Equinoctial");
    System.out.println("da/dt : " + yDotKep[0] + " " + yDot[0]);
    System.out.println("dex/dt : " + (cosPR * yDotKep[1] - e * sinPR * 
                      (yDotKep[3] + yDotKep[4])) + " " + yDot[1] );
    System.out.println("dey/dt : " + (sinPR * yDotKep[1] + e * cosPR * (yDotKep[3] + 
                       yDotKep[4])) + " " + yDot[2] );
    //System.out.println("dhx/dt : " + ((1 + tan2) * cosR * yDotKep[2]
    //                   - 2 * Math.sqrt(tan2) * sinR * yDotKep[4]) + " " + 
    //                   yDot[3] );
    //System.out.println("dhy/dt : " + ((1 + tan2) * sinR * yDotKep[2]
    //                   + 2 * Math.sqrt(tan2) * cosR * yDotKep[4]) + " " + 
    //                   yDot[4] );
    System.out.println("dhx/dt : " + (0.5 * (1 + tansquare) * cosR * yDotKep[2]
                       - tan2 * sinR * yDotKep[4]) + " " + 
                       yDot[3] );
    System.out.println("dhy/dt : " + (0.5 * (1 + tansquare) * sinR * yDotKep[2]
                       + tan2 * cosR * yDotKep[4]) + " " + 
                       yDot[4] );
    System.out.println("dl/dt : " + (yDotKep[3] + yDotKep[4] + yDotKep[5]) + " " + 
                       yDot[5] );
        
    assertEquals(yDotKep[0],
                 yDot[0], 1.0e-10);
    assertEquals(cosPR * yDotKep[1] - e * sinPR * (yDotKep[3] + yDotKep[4]),
                 yDot[1], 1.0e-10);
    assertEquals(sinPR * yDotKep[1] + e * cosPR * (yDotKep[3] + yDotKep[4]),
                 yDot[2], 1.0e-10);
    //assertEquals((1 + tan2) * cosR * yDotKep[2]
    //             - 2 * Math.sqrt(tan2) * sinR * yDotKep[4],
    //             yDot[3], 1.0e-10);
    //assertEquals((1 + tan2) * sinR * yDotKep[2]
    //             + 2 * Math.sqrt(tan2) * cosR * yDotKep[4],
    //             yDot[4], 1.0e-10);
    assertEquals(0.5 * (1 + tansquare) * cosR * yDotKep[2]
                 - tan2 * sinR * yDotKep[4],
                 yDot[3], 1.0e-10);
    assertEquals(0.5 * (1 + tansquare) * sinR * yDotKep[2]
                 + tan2 * cosR * yDotKep[4],
                 yDot[4], 1.0e-10);
    assertEquals(yDotKep[3] + yDotKep[4] + yDotKep[5],
                 yDot[5], 1.0e-10);

  }

  public void setUp() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    mu = 3.986e14;
    orbit = new EquinoctialParameters(position, velocity, mu);
    adder = new EquinoctialDerivativesAdder(orbit, mu);
  }
  
  public void tearDown() {
    mu    = Double.NaN;
    orbit = null;
    adder = null;
  }
  
  public static Test suite() {
    return new TestSuite(EquinoctialDerivativesAdderTest.class);
  }

  private double mu;
  private EquinoctialParameters       orbit;
  private EquinoctialDerivativesAdder adder;

}
