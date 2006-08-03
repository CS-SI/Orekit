package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.time.AbsoluteDate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FixedPoleEarthTest extends TestCase {

  public FixedPoleEarthTest(String name) {
    super(name);
  }

  public void testReference() {
    FixedPoleEarth model    = new FixedPoleEarth();
    AbsoluteDate   date     = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 2000.4 * 86400);
    Rotation       rotation = model.getOrientation(date);
    assertEquals(0.0, Vector3D.angle(Vector3D.plusK, rotation.getAxis()), 1.0e-10);
    assertEquals(0.972050007670873, rotation.getAngle(), 1.0e-12);
 }

  public void testIncreasing() {
    FixedPoleEarth model = new FixedPoleEarth();
    double previousAngle = 0;
    for (double d = 18775.3188 ; d < 18775.8173; d += 0.01) {
      AbsoluteDate date     = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, d * 86400);
      Rotation     rotation = model.getOrientation(date);
      assertEquals(0.0, Vector3D.angle(Vector3D.plusK, rotation.getAxis()), 1.0e-10);
      assertTrue(rotation.getAngle() > previousAngle);
      previousAngle = rotation.getAngle();
    }
  }

  public void testDecreasing() {
    FixedPoleEarth model = new FixedPoleEarth();
    double previousAngle = Math.PI;
    for (double d = 18775.8174; d < 18776.3159; d += 0.01) {
      AbsoluteDate date     = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, d * 86400);
      Rotation     rotation = model.getOrientation(date);
      assertEquals(0.0, Vector3D.angle(Vector3D.minusK, rotation.getAxis()), 1.0e-10);
      assertTrue(rotation.getAngle() < previousAngle);
      previousAngle = rotation.getAngle();
    }
  }

  public void testFiniteDifference() {
    FixedPoleEarth model = new FixedPoleEarth();
    double dt    = 17.3;
    AbsoluteDate date1     = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 18775.9234 * 86400);
    Rotation     rotation1 = model.getOrientation(date1);
    AbsoluteDate date2     = new AbsoluteDate(date1, dt);
    Rotation     rotation2 = model.getOrientation(date2);
    double angle = rotation2.applyTo(rotation1.revert()).getAngle();
    Vector3D rotationVector = model.getRotationVector(date1);
    assertEquals(0, Vector3D.angle(Vector3D.plusK, rotationVector), 1.0e-10);
    assertEquals(angle / dt, rotationVector.getNorm(), 1.0e-10);
  }

  public static Test suite() {
    return new TestSuite(FixedPoleEarthTest.class);
  }

}

