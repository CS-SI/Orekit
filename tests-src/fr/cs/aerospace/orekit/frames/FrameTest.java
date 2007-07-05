package fr.cs.aerospace.orekit.frames;

import java.util.Random;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FrameTest extends TestCase {
  
  public void testSameFrameRoot() throws OrekitException {
    Random random = new Random(0x29448c7d58b95565l);
    Frame  frame  = Frame.getJ2000();
    checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
  }
  
  public void testSameFrameNoRoot() throws OrekitException {
    Random random = new Random(0xc6e88d0f53e29116l);
    Transform t   = randomTransform(random);
    Frame frame   = new Frame(Frame.getJ2000(), t, null);
    checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
  }

  public void testSimilarFrames() throws OrekitException {
    Random random = new Random(0x1b868f67a83666e5l);
    Transform t   = randomTransform(random);
    Frame frame1  = new Frame(Frame.getJ2000(), t, null);
    Frame frame2  = new Frame(Frame.getJ2000(), t, null);
    checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
  }

  public void testFromParent() throws OrekitException {
    Random random = new Random(0xb92fba1183fe11b8l);
    Transform fromJ2000  = randomTransform(random);
    Frame frame = new Frame(Frame.getJ2000(), fromJ2000, null);
    Transform toJ2000 = frame.getTransformTo(Frame.getJ2000(), new AbsoluteDate());
    checkNoTransform(new Transform(fromJ2000, toJ2000), random);
  }

  public void testDecomposedTransform() throws OrekitException {
    Random random = new Random(0xb7d1a155e726da57l);
    Transform t1  = randomTransform(random);
    Transform t2  = randomTransform(random);
    Transform t3  = randomTransform(random);
    Frame frame1 =
      new Frame(Frame.getJ2000(), new Transform(new Transform(t1, t2), t3), null);
    Frame frame2 =
      new Frame(new Frame(new Frame(Frame.getJ2000(), t1, null), t2, null), t3, null);
    checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
  }
  
  public void testFindCommon() throws OrekitException {
	  
    Random random = new Random(0xb7d1a155e726da57l);
    Transform t1  = randomTransform(random);
    Transform t2  = randomTransform(random);
    Transform t3  = randomTransform(random);    
    
	Frame R1 = new Frame(Frame.getJ2000(),t1,"R1");
	Frame R2 = new Frame(R1,t2,"R2");
	Frame R3 = new Frame(R2,t3,"R3");
	  
	  Transform T = R1.getTransformTo(R3, new AbsoluteDate());
      
      Transform S = new Transform(t2,t3);
	  
      checkNoTransform(new Transform(T, S.getInverse()) , random);

  }

  public void testVeis1950() throws OrekitException {
    Transform t = Frame.getReferenceFrame(Frame.VEIS1950, new AbsoluteDate()).getTransformTo(Frame.getJ2000(), new AbsoluteDate());
    Vector3D i50    = t.transformVector(Vector3D.plusI);
    Vector3D j50    = t.transformVector(Vector3D.plusJ);
    Vector3D k50    = t.transformVector(Vector3D.plusK);
    Vector3D i50Ref = new Vector3D( 0.9999256489473456,
                                    0.011181451214217871,
                                    4.8653597990872734e-3);
    Vector3D j50Ref = new Vector3D(-0.011181255200285388,
                                    0.9999374855347822,
                                   -6.748721516262951e-5);
    Vector3D k50Ref = new Vector3D(-4.865810248725263e-3,
                                    1.3081367862337385e-5,
                                    0.9999881617896792);
    assertEquals(0, i50.subtract(i50Ref).getNorm(), 1.0e-15);
    assertEquals(0, j50.subtract(j50Ref).getNorm(), 1.0e-15);
    assertEquals(0, k50.subtract(k50Ref).getNorm(), 1.0e-15);
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
        Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
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
      Vector3D b = transform.transformVector(a);
      assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
      Vector3D c = transform.transformPosition(a);
      assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
    }
  }
  
  public static Test suite() {
    return new TestSuite(FrameTest.class);
  }
  
}
