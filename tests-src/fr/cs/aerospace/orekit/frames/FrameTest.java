package fr.cs.aerospace.orekit.frames;

import java.util.Random;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FrameTest extends TestCase {
  
  public void testSameFrameRoot() {
    Random random = new Random(0x29448c7d58b95565l);
    Frame  frame  = Frame.getJ2000();
    checkNoTransform(frame.getTransformTo(frame), random);
  }
  
  public void testSameFrameNoRoot() {
    Random random = new Random(0xc6e88d0f53e29116l);
    Transform t   = randomTransform(random);
    Frame frame   = new Frame(Frame.getJ2000(), t);
    checkNoTransform(frame.getTransformTo(frame), random);
  }

  public void testSimilarFrames() {
    Random random = new Random(0x1b868f67a83666e5l);
    Transform t   = randomTransform(random);
    Frame frame1  = new Frame(Frame.getJ2000(), t);
    Frame frame2  = new Frame(Frame.getJ2000(), t);
    checkNoTransform(frame1.getTransformTo(frame2), random);
  }

  public void testFromParent() {
    Random random = new Random(0xb92fba1183fe11b8l);
    Transform fromJ2000  = randomTransform(random);
    Frame frame = new Frame(Frame.getJ2000(), fromJ2000);
    Transform toJ2000 = frame.getTransformTo(Frame.getJ2000());
    checkNoTransform(new Transform(fromJ2000, toJ2000), random);
  }

  public void testDecomposedTransform() {
    Random random = new Random(0xb7d1a155e726da57l);
    Transform t1  = randomTransform(random);
    Transform t2  = randomTransform(random);
    Transform t3  = randomTransform(random);
    Frame frame1 =
      new Frame(Frame.getJ2000(), new Transform(new Transform(t1, t2), t3));
    Frame frame2 =
      new Frame(new Frame(new Frame(Frame.getJ2000(), t1), t2), t3);
    checkNoTransform(frame1.getTransformTo(frame2), random);
  }

  private Transform randomTransform(Random random) {
    Transform transform = new Transform();
    for (int i = random.nextInt(10); i > 0; --i) {
      if (random.nextBoolean()) {
        Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                  random.nextDouble() * 1000.0,
                                  random.nextDouble() * 1000.0);
        transform = new Transform(transform, new Transform(u));
      } else {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q);
        transform = new Transform(transform, new Transform(r));
      }
    }
    return transform;
  }

  private void checkNoTransform(Transform transform, Random random) {
    for (int i = 0; i < 100; ++i) {
      Vector3D a = new Vector3D(random.nextDouble(),
                                random.nextDouble(),
                                random.nextDouble());
      Vector3D b = transform.transformDirection(a);
      assertEquals(0, Vector3D.subtract(a, b).getNorm(), 1.0e-10);
      Vector3D c = transform.transformPosition(a);
      assertEquals(0, Vector3D.subtract(a, c).getNorm(), 1.0e-10);
    }
  }
  
  public static Test suite() {
    return new TestSuite(FrameTest.class);
  }
  
}
