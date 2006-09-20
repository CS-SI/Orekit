package fr.cs.aerospace.orekit.frames;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.iers.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ITRF2000FrameTest extends TestCase {

  private static final File rootDir;
  static {
    try {
      rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data", "/");
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException("unexpected failure");
    }
  }

  public void testRoughRotation() throws ParseException, OrekitException {

    FrameSynchronizer synchronizer =
      new FrameSynchronizer(new AbsoluteDate("2006-02-24T15:38:00",
                                             UTCScale.getInstance()));
    ITRF2000Frame itrf2000 = new ITRF2000Frame(synchronizer);
    Transform t0 = itrf2000.getTransformTo(Frame.getJ2000());

    double dt = 10.0;
    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), dt));
    Transform t1 = itrf2000.getTransformTo(Frame.getJ2000());
    Transform evolution = new Transform(t0.getInverse(), t1);

    assertEquals(0.0, evolution.getTranslation().getNorm(), 1.0e-10);
    assertTrue(Vector3D.angle(Vector3D.plusK, evolution.getRotation().getAxis()) < Math.toRadians(1.0));
    assertEquals(2 * Math.PI * dt / 86164, evolution.getRotation().getAngle(), 1.0e-9);
    
  }

  public void testRoughOrientation() throws ParseException, OrekitException {

    FrameSynchronizer synchronizer =
      new FrameSynchronizer(new AbsoluteDate("2001-03-21T00:00:00",
                                             UTCScale.getInstance()));
    ITRF2000Frame itrf2000 = new ITRF2000Frame(synchronizer);

    Vector3D u = itrf2000.getTransformTo(Frame.getJ2000()).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusI) < Math.toRadians(2));

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    u = itrf2000.getTransformTo(Frame.getJ2000()).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusJ) < Math.toRadians(2));

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    u = itrf2000.getTransformTo(Frame.getJ2000()).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusI) < Math.toRadians(2));

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    u = itrf2000.getTransformTo(Frame.getJ2000()).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusJ) < Math.toRadians(2));

  }

  public void testRoughERA() throws ParseException, OrekitException {

    FrameSynchronizer synchronizer =
      new FrameSynchronizer(new AbsoluteDate("2001-03-21T00:00:00",
                                             UTCScale.getInstance()));
    ITRF2000Frame itrf2000 = new ITRF2000Frame(synchronizer);

    assertEquals(180, Math.toDegrees(itrf2000.getEarthRotationAngle()), 2.0);

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    assertEquals(-90, Math.toDegrees(itrf2000.getEarthRotationAngle()), 2.0);

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    assertEquals(0, Math.toDegrees(itrf2000.getEarthRotationAngle()), 2.0);

    synchronizer.setDate(new AbsoluteDate(synchronizer.getDate(), 6 * 3600));
    assertEquals(90, Math.toDegrees(itrf2000.getEarthRotationAngle()), 2.0);

  }

  private void checkSameTransform(Transform transform1, Transform transform2) {	   
    assertEquals(0, Vector3D.subtract(transform1.getTranslation() , transform2.getTranslation()).getNorm(), 1.0e-10);
    assertEquals(0, transform1.getRotation().applyTo(transform2.getRotation().revert()).getAngle(), 1.0e-10);
  }

  public void setUp() {
    try {
      System.setProperty("orekit.iers.directory",
                         new File(rootDir, "compressed-data").getAbsolutePath());
      assertNotNull(IERSData.getInstance());
    } catch (OrekitException e) {
      fail(e.getMessage());
    }
  }

  public void tearDown() {
    try {

      // resetting the singletons to null
      Field instance = UTCScale.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
      instance.setAccessible(false);

      instance = IERSData.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
      instance.setAccessible(false);

    } catch (Exception e) {
      // ignored
    }
  }

   public static Test suite() {
    return new TestSuite(ITRF2000FrameTest.class);
  }
  
}
