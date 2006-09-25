package fr.cs.aerospace.orekit.frames;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Rotation;
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
  
  public void testRoughTransformJ2OOO_TerVrai() throws OrekitException, ParseException {
	  
//	  date 14 Octobre 2003 � 02h00 UTC
//	  (soit j = 19644, s = 7200.0)
//	 pos = (6500000.0, -1234567.0, 4000000.0) en m�tres

//
//	 UT1 - UTC = -0.362603591667 s
//	 TAI - UTC = 32 s

	  checkSuccess("regular-data"); 
	  AbsoluteDate date = new AbsoluteDate("2003-10-14T02:00:00", UTCScale.getInstance());

	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(6500000.0,
			                          -1234567.0,
			                           4000000.0);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(3011109.359428156,
                                         -5889822.670622288,
                                          4002170.037826595);

	  // Position tests
	  System.out.println("tests position");
	  
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(posTestCase.getNorm(),posITRF.getNorm(),10e-5);
	  
      // Speeds
// 	 vit = (3609.28229, 3322.88979, -7083.95061) en m�tres par secondes
      
	  Vector3D sJ2000 = new Vector3D( 4410.401666575693,
			                         -1033.6270182391435,
			                         -7082.627462730339);
	  
	  Vector3D sITRF = trans.transformVector(sJ2000);
	  
	  Vector3D sTestCase = new Vector3D(-0.696025140792288e04,
			                             0.284069914312733e04,
			                             0.216918435606272e03);

	  // Speed tests
	  System.out.println("tests vitesse");
	  
	  d = Vector3D.subtract(sITRF, sTestCase);
      System.out.println("Ecarts vitesse en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      r = new Rotation(sITRF, sTestCase);
      System.out.println("axe rotation vitesse " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation vitesse " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(sTestCase.getNorm(),sITRF.getNorm(),10e-5);
	  
  }

  public void testRoughTransformJ2000_TerRef() throws OrekitException, ParseException {
//	  date 14 Octobre 2003 � 02h00 UTC
//	  (soit j = 19644, s = 7200.0)
//	 pos = (6500000.0, -1234567.0, 4000000.0) en m�tres

//
//	 UT1 - UTC = -0.362603591667 s
//	 TAI - UTC = 32 s

	  AbsoluteDate date = new AbsoluteDate("2003-10-14T02:00:00", UTCScale.getInstance());

	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(6500000.0,
			                          -1234567.0,
			                           4000000.0);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(3011113.9704794497,
			                               -5889820.819968201,
			                               4002169.292139155);

	  // Position tests
	  System.out.println("tests position");
	  
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(posTestCase.getNorm(),posITRF.getNorm(),10e-5);
	  
      // Speeds
// 	 vit = (3609.28229, 3322.88979, -7083.95061) en m�tres par secondes
      
	  Vector3D sJ2000 = new Vector3D( 3609.28229,
			                          3322.88979,
			                         -7083.95061);
	  
	  Vector3D sITRF = trans.transformVector(sJ2000);
	  
	  Vector3D sTestCase = new Vector3D(4410.393506412667,
			                           -1033.6302933345141 ,
			                           -7082.632066151947);

	  // Speed tests
	  System.out.println("tests vitesse");
	  
	  d = Vector3D.subtract(sITRF, sTestCase);
      System.out.println("Ecarts vitesse en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      r = new Rotation(sITRF, sTestCase);
      System.out.println("axe rotation vitesse " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation vitesse " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(sTestCase.getNorm(),sITRF.getNorm(),10e-5);
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
  
  private void checkSuccess(String directoryName) {
    try {
      System.setProperty("orekit.iers.directory",
                         new File(rootDir, directoryName).getAbsolutePath());
      assertNotNull(IERSData.getInstance());
    } catch (OrekitException e) {
      fail(e.getMessage());
    }
  }

   public static Test suite() {
    return new TestSuite(ITRF2000FrameTest.class);
  }
  
}
