package fr.cs.aerospace.orekit;

import junit.framework.*;

import org.spaceroots.mantissa.geometry.Vector3D;

public class CartesianParametersTest extends TestCase {

    public CartesianParametersTest(String name) {
    super(name);
  }

  public void testCartesian() {
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.986e14;
    CartesianParameters p = new CartesianParameters(position, velocity, mu);
    assertEquals(12123410.6232767,  p.getA(), 1.0e-7);
    assertEquals(0.351820307407621, p.getE(), 1.0e-13);
    assertEquals(0.519070199278675, p.getI(), 1.0e-13);
    assertEquals(-1.28824137432531, p.getRAAN(), 1.0e-13);
  }

  public static Test suite() {
    return new TestSuite(CartesianParametersTest.class);
  }

}
