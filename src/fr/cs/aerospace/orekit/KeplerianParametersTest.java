package fr.cs.aerospace.orekit;

import junit.framework.*;

import org.spaceroots.mantissa.geometry.Vector3D;

public class KeplerianParametersTest extends TestCase {

    public KeplerianParametersTest(String name) {
    super(name);
  }

  public void testKeplerian() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);
    assertEquals(12123410.6232767,  p.getA(), 1.0e-7);
    assertEquals(0.351820307407621, p.getE(), 1.0e-13);
    assertEquals(0.519070199278675, p.getI(), 1.0e-13);
    assertEquals(-1.28824137432531, p.getRAAN(), 1.0e-13);
  }
  
  public void testAnomaly() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);

    double e = p.getE();
    double sqrt = Math.sqrt((1 - e) / (1 + e));

    double v = 1.1;
    double E = 2 * Math.atan(sqrt * Math.tan(v / 2));
    double M = E - e * Math.sin(E);

    p.setTrueAnomaly(v);
    assertEquals(v, p.getTrueAnomaly(), 1.0e-12);
    assertEquals(E, p.getEccentricAnomaly(), 1.0e-12);
    assertEquals(M, p.getMeanAnomaly(), 1.0e-12);
    p.setTrueAnomaly(0);

    p.setEccentricAnomaly(E);
    assertEquals(v, p.getTrueAnomaly(), 1.0e-12);
    assertEquals(E, p.getEccentricAnomaly(), 1.0e-12);
    assertEquals(M, p.getMeanAnomaly(), 1.0e-12);
    p.setTrueAnomaly(0);

    p.setMeanAnomaly(M);
    assertEquals(v, p.getTrueAnomaly(), 1.0e-12);
    assertEquals(E, p.getEccentricAnomaly(), 1.0e-12);
    assertEquals(M, p.getMeanAnomaly(), 1.0e-12);

  }
  
  public void testModules() {
    KeplerianParameters p = new KeplerianParameters(21864684.345,
                                                    1.0e-2, -4.2e-3,
                                                    -0.12, 0.19, 2.3);
    double e       = p.getE();
    double v       = p.getTrueAnomaly();
    double ksi     = 1 + e * Math.cos(v);
    double nu      = e * Math.sin(v);
    double epsilon = Math.sqrt((1 - e) * (1 + e));

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
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);

    double apogeeRadius  = p.getA() * (1 + p.getE());
    double perigeeRadius = p.getA() * (1 - p.getE());

    // radius and orbital plane
    for (double v = 0; v < 2 * Math.PI; v += 0.1) {
      p.setTrueAnomaly(v);
      position = p.getPosition(mu);
      assertTrue(position.getNorm() <= apogeeRadius);
      assertTrue(position.getNorm() >= perigeeRadius);
      position.normalizeSelf();
      velocity = p.getVelocity(mu);
      velocity.normalizeSelf();
      assertEquals(0, Vector3D.dotProduct(position, momentum), 1.0e-7);
      assertEquals(0, Vector3D.dotProduct(velocity, momentum), 1.0e-7);
    }

    // apsides
    p.setTrueAnomaly(0);
    assertEquals(perigeeRadius, p.getPosition(mu).getNorm(), 1.0e-7);
    p.setTrueAnomaly(Math.PI);
    assertEquals(apogeeRadius,  p.getPosition(mu).getNorm(), 1.0e-7);

    // nodes
    p.setTrueAnomaly(Math.PI - p.getPA());
    assertEquals(0,  p.getPosition(mu).getZ(), 1.0e-7);
    assertTrue(p.getVelocity(mu).getZ() < 0);
    p.setTrueAnomaly(2.0 * Math.PI- p.getPA());
    assertEquals(0,  p.getPosition(mu).getZ(), 1.0e-7);
    assertTrue(p.getVelocity(mu).getZ() > 0);

  }

  public void testSymmetry() {

    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);

    Vector3D positionOffset = new Vector3D(p.getPosition(mu));
    positionOffset.subtractFromSelf(position);
    Vector3D velocityOffset = new Vector3D(p.getVelocity(mu));
    velocityOffset.subtractFromSelf(velocity);

    assertTrue(positionOffset.getNorm() < 1.0e-10);
    assertTrue(velocityOffset.getNorm() < 1.0e-10);

  }
  
  public static Test suite() {
    return new TestSuite(KeplerianParametersTest.class);
  }

}
