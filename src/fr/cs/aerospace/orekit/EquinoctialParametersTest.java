package fr.cs.aerospace.orekit;

import junit.framework.*;

import org.spaceroots.mantissa.geometry.Vector3D;

public class EquinoctialParametersTest extends TestCase {

    public EquinoctialParametersTest(String name) {
    super(name);
  }

  public void testKeplerian() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    OrbitalParameters p = new EquinoctialParameters(position, velocity, mu);
    assertEquals(12123410.6232767,  p.getA(), 1.0e-7);
    assertEquals(0.351820307407621, p.getE(), 1.0e-13);
    assertEquals(0.519070199278675, p.getI(), 1.0e-13);
    assertEquals(-1.28824137432531, p.getRAAN(), 1.0e-13);
  }
  
  public void testAnomaly() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    EquinoctialParameters p = new EquinoctialParameters(position, velocity, mu);

    double e = p.getE();
    double sqrt = Math.sqrt((1 - e) / (1 + e));
    double paPraan = p.getPA() + p.getRAAN();

    double lv = 1.1;
    double lE = 2 * Math.atan(sqrt * Math.tan((lv - paPraan) / 2)) + paPraan;
    double lM = lE - e * Math.sin(lE - paPraan);

    p.setLv(lv);
    assertEquals(lv, p.getLv(), 1.0e-12);
    assertEquals(lE, p.getLE(), 1.0e-12);
    assertEquals(lM, p.getLM(), 1.0e-12);
    p.setLv(0);

    p.setLE(lE);
    assertEquals(lv, p.getLv(), 1.0e-12);
    assertEquals(lE, p.getLE(), 1.0e-12);
    assertEquals(lM, p.getLM(), 1.0e-12);
    p.setLv(0);

    p.setLM(lM);
    assertEquals(lv, p.getLv(), 1.0e-12);
    assertEquals(lE, p.getLE(), 1.0e-12);
    assertEquals(lM, p.getLM(), 1.0e-12);

  }
  
  public void testModules() {
    EquinoctialParameters p = new EquinoctialParameters(21864684.345,
                                                        1.0e-2, -4.2e-3,
                                                        -0.12, 0.19, 2.3);
    double ex = p.getEx();
    double ey = p.getEy();
    double lv = p.getLv();
    double ksi     = 1 + ex * Math.cos(lv) + ey * Math.sin(lv);
    double nu      = ex * Math.sin(lv) - ey * Math.cos(lv);
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);

    double a  = p.getA();
    double mu = 3.986e14;
    double na = Math.sqrt(mu / a);

    assertEquals(a * epsilon * epsilon / ksi,
                 p.getPosition(mu).getNorm(),
                 1.0e-5);
    assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                 p.getVelocity(mu).getNorm(),
                 1.0e-7);

  }

  public void testGeometry() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    Vector3D momentum = Vector3D.crossProduct(position, velocity);
    momentum.normalizeSelf();
    double mu = 3.986e14;
    EquinoctialParameters p = new EquinoctialParameters(position, velocity, mu);

    double apogeeRadius  = p.getA() * (1 + p.getE());
    double perigeeRadius = p.getA() * (1 - p.getE());

    // radius and orbital plane
    for (double lv = 0; lv < 2 * Math.PI; lv += 0.1) {
      p.setLv(lv);
      position = p.getPosition(mu);
      assertTrue(position.getNorm() <= apogeeRadius);
      assertTrue(position.getNorm() >= perigeeRadius);
      position.normalizeSelf();
      velocity = p.getVelocity(mu);
      velocity.normalizeSelf();
      assertEquals(0, Vector3D.dotProduct(position, momentum), 1.0e-7);
      assertEquals(0, Vector3D.dotProduct(velocity, momentum), 1.0e-7);
    }

  }

  public void testSymmetry() {

    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    EquinoctialParameters p = new EquinoctialParameters(position, velocity, mu);

    Vector3D positionOffset = new Vector3D(p.getPosition(mu));
    positionOffset.subtractFromSelf(position);
    Vector3D velocityOffset = new Vector3D(p.getVelocity(mu));
    velocityOffset.subtractFromSelf(velocity);

    assertTrue(positionOffset.getNorm() < 1.0e-10);
    assertTrue(velocityOffset.getNorm() < 1.0e-10);

  }
  
  public static Test suite() {
    return new TestSuite(EquinoctialParametersTest.class);
  }

}
