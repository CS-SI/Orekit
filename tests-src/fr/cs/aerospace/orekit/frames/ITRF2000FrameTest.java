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
  
  public void testRoughTransformJ2OOO_ITRF() throws OrekitException, ParseException {
	  
	  // test case date : jul1950%jour = 15002_pm_entier
	  //                  jul1950%sec  = 180._pm_reel
	  //                  delta_tu1    = .5_pm_reel
	  //                  delta_tai    = 25._pm_reel
// TODO check this date convertion
	  AbsoluteDate date = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 86400.0 * 15002 + 180);
	  AbsoluteDate date2 = new AbsoluteDate("1991-01-28T00:03:00", UTCScale.getInstance());
	  double off = date.minus(date2);
	  System.out.println("offset : "  + off );
	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = itrf.getTransformTo(Frame.getJ2000());
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(991396.024,
			                           488684.594,
			                           7109721.509);
	  
	  Vector3D posITRF = trans.getInverse().transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(-0.221938831683687e06,
			                              -0.108816598895859e07,
			                               0.710889981500780e07);

	  // Position tests
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("écarts en mètres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle " + Math.toDegrees(r.getAngle()));
      assertEquals(posTestCase.getX(), posITRF.getX(), -(posTestCase.getX()*0.3));
	  assertEquals(posTestCase.getY(), posITRF.getY(), -(posTestCase.getY()*0.01));
	  assertEquals(posTestCase.getZ(), posITRF.getZ(), (posTestCase.getZ()*0.01));
	  
      // Speeds
	  
	  Vector3D sJ2000 = new Vector3D( 1963.575,
			                         -7174.14,
			                          218.695);
	  
	  Vector3D sITRF = trans.getInverse().transformVector(sJ2000);
	  
	  Vector3D sTestCase = new Vector3D(-0.696025140792288e04,
			                             0.284069914312733e04,
			                             0.216918435606272e03);

	  // Speed tests
	  assertEquals(sTestCase.getX(), sITRF.getX(), -(sTestCase.getX()*0.02));
	  assertEquals(sTestCase.getY(), sITRF.getY(), (sTestCase.getY()*0.01));
	  assertEquals(sTestCase.getZ(), sITRF.getZ(), (sTestCase.getZ()*0.4));
	  
  }

  public void testRoughTransformITRF_J2000() throws OrekitException {
	  
	  // test case date : jul1950%jour   = 15002_pm_entier
	  //                  jul1950%sec    = 43200._pm_reel
	  //                  delta_tu1      = .5_pm_reel
	  //                  delta_tai      = 25._pm_reel

// TODO check this date convertion 
	  AbsoluteDate date = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 86400*15002 + 43200);
	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = itrf.getTransformTo(Frame.getJ2000());
	  
	  // Positions
	  
	  Vector3D posITRF = new Vector3D(217012.946,
			                          1089159.055,
			                          7108899.815);
	  
	  Vector3D posJ2000 = trans.transformPosition(posITRF);
	  
	  Vector3D posTestCase = new Vector3D( 0.991398101724679e06,
			                               0.488685294427019e06,
			                               0.710972117121583e07);

	  // Position tests
	  assertEquals(posTestCase.getX(), posJ2000.getX(), (posTestCase.getX()*0.05));
	  assertEquals(posTestCase.getY(), posJ2000.getY(), (posTestCase.getY()*0.3));
	  assertEquals(posTestCase.getZ(), posJ2000.getZ(), (posTestCase.getZ()*0.01));
	  
      // Speeds
	  
	  Vector3D sITRF = new Vector3D( 6973.034,
			                        -2809.178,
			                          216.918);
	  
	  Vector3D sJ2000 = trans.transformVector(sITRF);
	  
	  Vector3D sTestCase = new Vector3D( 0.196357458494215e04,
			                            -0.717414099509734e04,
			                             0.218694710532406e03);

	  // Speed tests
	  assertEquals(sTestCase.getX(), sJ2000.getX(), (sTestCase.getX()*0.02));
	  assertEquals(sTestCase.getY(), sJ2000.getY(), -(sTestCase.getY()*0.02));
	  assertEquals(sTestCase.getZ(), sJ2000.getZ(), (sTestCase.getZ()*0.4));
	  
  }
  
/*  private void checkSameTransform(Transform transform1, Transform transform2) {	   
    assertEquals(0, Vector3D.subtract(transform1.getTranslation() , transform2.getTranslation()).getNorm(), 1.0e-10);
    assertEquals(0, transform1.getRotation().applyTo(transform2.getRotation().revert()).getAngle(), 1.0e-10);
  }*/

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
