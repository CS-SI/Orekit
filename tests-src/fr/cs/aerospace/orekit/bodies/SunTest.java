package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.RDate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SunTest extends TestCase {

  public SunTest(String name) {
    super(name);
  }

  public void testSpring() {
    checkDirection(6868800.0, 0.99998858, -0.00021267, 0.00477363);
  }

  public void testSummer() {
    checkDirection(14731200.0, 0.02817610, 0.91707719, 0.39771287);
  }

  public void testAutomn() {
    checkDirection(22766400.0, -0.99919507, 0.03836434, 0.01172111);
  }

  public void testWinter() {
    checkDirection(30628800.0, -0.02050325, -0.91726256, -0.39775496);
  }

  public void checkDirection(double offsetJ2000, double x, double y, double z) {
    Vector3D sun = new Sun().getPosition(new RDate(RDate.J2000Epoch, offsetJ2000));
    sun.normalizeSelf();
    assertEquals(x, sun.getX(), 1.0e-7);
    assertEquals(y, sun.getY(), 1.0e-7);
    assertEquals(z, sun.getZ(), 1.0e-7);
  }

  public static Test suite() {
    return new TestSuite(SunTest.class);
  }

}

