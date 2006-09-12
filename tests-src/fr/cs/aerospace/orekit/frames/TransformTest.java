package fr.cs.aerospace.orekit.frames;

import java.util.Random;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TransformTest extends TestCase {

  public void testIdentityTranslation() {
    checkNoTransform(new Transform(new Vector3D(0, 0, 0)),
                     new Random(0xfd118eac6b5ec136l));
  }

  public void testIdentityRotation() {
    checkNoTransform(new Transform(new Rotation(1, 0, 0, 0, false)),
                     new Random(0xfd118eac6b5ec136l));
  }

  public void testSimpleComposition() {
    Transform transform =
      new Transform(new Transform(new Rotation(Vector3D.plusK, 0.5 * Math.PI)),
                    new Transform(Vector3D.plusI));
    Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
    Vector3D v = new Vector3D(0.0, 1.0, 1.0);
    assertEquals(0, Vector3D.subtract(u, v).getNorm(), 1.0e-15);
  }

  public void testRandomComposition() {

    Random random = new Random(0x171c79e323a1123l);
    for (int i = 0; i < 10; ++i) {

      // build a complex transform by compositing primitive ones
      int n = random.nextInt(10);
      Transform[] transforms = new Transform[n];
      Transform combined = new Transform();
      for (int k = 0; k < n; ++k) {
        transforms[k] = random.nextBoolean()
                      ? new Transform(randomVector(random))
                      : new Transform(randomRotation(random));
        combined = new Transform(combined, transforms[k]);
      }

      // check the composition
      for (int j = 0; j < 10; ++j) {
        Vector3D a = new Vector3D(random.nextDouble(),
                                  random.nextDouble(),
                                  random.nextDouble());
        Vector3D bRef = a;
        Vector3D cRef = a;
        for (int k = 0; k < n; ++k) {
          bRef = transforms[k].transformVector(bRef);
          cRef = transforms[k].transformPosition(cRef);
        }

        Vector3D bCombined = combined.transformVector(a);
        Vector3D cCombined = combined.transformPosition(a);

        assertEquals(0, Vector3D.subtract(bCombined, bRef).getNorm(), 1.0e-11);
        assertEquals(0, Vector3D.subtract(cCombined, cRef).getNorm(), 1.0e-11);

      }
    }

  }

  public void testReverse() {
    Random random = new Random(0x9f82ba2b2c98dac5l);
    Transform t1  = new Transform(randomVector(random));
    Transform t2  = new Transform(randomRotation(random));
    Transform t3  = new Transform(randomVector(random));
    Transform t   = new Transform(new Transform(t1, t2), t3);
    checkNoTransform(new Transform(t, t.getInverse()), random);
  }

  public void testTranslation() {
    Random rnd = new Random(0x7e9d737ba4147787l);
    for (int i = 0; i < 10; ++i) {
      Vector3D delta = randomVector(rnd);
      Transform transform = new Transform(delta);
      for (int j = 0; j < 10; ++j) {
        Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
        Vector3D b = transform.transformVector(a);
        assertEquals(0, Vector3D.subtract(b, a).getNorm(), 1.0e-10);
        Vector3D c = transform.transformPosition(a);
        assertEquals(0,
                     Vector3D.subtract(Vector3D.subtract(c, a), delta).getNorm(),
                     1.0e-13);
      }
    }
  }

  public void testRotation() {
    Random rnd = new Random(0x73d5554d99427af0l);
    for (int i = 0; i < 10; ++i) {

      Rotation r    = randomRotation(rnd);
      Vector3D axis = r.getAxis();
      double angle  = r.getAngle();

      Transform transform = new Transform(r);
      for (int j = 0; j < 10; ++j) {
        Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
        Vector3D b = transform.transformVector(a);
        assertEquals(Vector3D.angle(axis, a), Vector3D.angle(axis, b), 1.0e-13);
        Vector3D aOrtho = Vector3D.crossProduct(axis, a);
        Vector3D bOrtho = Vector3D.crossProduct(axis, b);
        assertEquals(angle, Vector3D.angle(aOrtho, bOrtho), 1.0e-13);
        Vector3D c = transform.transformPosition(a);
        assertEquals(0, Vector3D.subtract(c, b).getNorm(), 1.0e-13);
      }

    }
  }

  private Vector3D randomVector(Random random) {
    return new Vector3D(random.nextDouble() * 1000.0,
                        random.nextDouble() * 1000.0,
                        random.nextDouble() * 1000.0);
  }

  private Rotation randomRotation(Random random) {
    double q0 = random.nextDouble() * 2 - 1;
    double q1 = random.nextDouble() * 2 - 1;
    double q2 = random.nextDouble() * 2 - 1;
    double q3 = random.nextDouble() * 2 - 1;
    double q  = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
    return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
  }

  private void checkNoTransform(Transform transform, Random random) {
    for (int i = 0; i < 100; ++i) {
      Vector3D a = new Vector3D(random.nextDouble(),
                                random.nextDouble(),
                                random.nextDouble());
      Vector3D b = transform.transformVector(a);
      assertEquals(0, Vector3D.subtract(a, b).getNorm(), 1.0e-12);
      Vector3D c = transform.transformPosition(a);
      assertEquals(0, Vector3D.subtract(a, c).getNorm(), 1.0e-12);
    }
  }
  
  public static Test suite() {
    return new TestSuite(TransformTest.class);
  }
  
}
