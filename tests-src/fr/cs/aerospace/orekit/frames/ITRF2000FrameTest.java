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
	  	  
//	  Entrees obligatoires:
//		  model:       17
//		  jul1950:     19644 7200.0
//		  delta_tu1:   -0.362603591667
//		  delta_tai:   32.0
//		  pole U (rad)  1.1521374925313776E-6
//		  pole V (rad) -4.6241250136360905E-7
//		  position en J2000
//		  pos_J2000 X:   6500000.0
//		  pos_J2000 Y:   -1234567.0
//		  pos_J2000 Z:   4000000.0
//		  
//		  Vitesse en J2000
//		  vit_J2000 X:   3609.28229
//		  vit_J2000 Y:   3322.88979
//		  vit_J2000 Z:   -7083.950661
//		  
//		  
//		  position TER VRAI
//		  X:   3011109.360780633
//		  Y:   -5889822.669411588
//		  Z:   4002170.0385907636
//		  vitesse TER VRAI
//		  VX:   4410.401666816698
//		  VY:   -1033.6270181745385
//		  VZ:   -7082.627462641988
//		  
//		  code retour: 0
//		  
//		  
//		  position TER REF
//		  X:   3011113.9718319275
//		  Y:   -5889820.8187575004
//		  Z:   4002169.292903322
//		  vitesse TER REF
//		  VX:   4410.393506653672
//		  VY:   -1033.630293269909
//		  VZ:   -7082.632066063596
//		  
//		  code retour: 0

	//  for(int i = 0; i<59; i++){
		  	  
	  AbsoluteDate date = new AbsoluteDate("2003-10-14T01:58:56", UTCScale.getInstance());

	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(6500000.0,
			                          -1234567.0,
			                           4000000.0);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(3011109.360780633,
			                             -5889822.669411588,
			                              4002170.0385907636);

	  // Position tests
	  System.out.println("------------------------------------------");
	  System.out.println("");
	  System.out.println("tests position Ter Vrai");
	  
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(posTestCase.getNorm(),posITRF.getNorm(),1.0e-5);
	//  }
//      // Speeds
//      
//	  Vector3D sJ2000 = new Vector3D(3609.28229,
//                                     3322.88979,
//                                    -7083.950661);
//	  
//	  Vector3D sITRF = trans.transformVector(sJ2000);
//	  
//	  Vector3D sTestCase = new Vector3D(4410.401666816698,
//                                        -1033.6270181745385,
//                                        -7082.627462641988);
//
//	  // Speed tests
//	  System.out.println("------------------------------------------");
//	  System.out.println(""); 
//	  System.out.println("tests vitesse Ter Vrai");
//	  
//	  d = Vector3D.subtract(sITRF, sTestCase);
//      System.out.println("Ecarts vitesse en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
//      r = new Rotation(sITRF, sTestCase);
//      System.out.println("axe rotation vitesse " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
//      System.out.println("angle rotation vitesse " + Math.toDegrees(r.getAngle()));
////      assertEquals(0, d.getX(), 100);
////      assertEquals(0, d.getY(), 100);
////      assertEquals(0, d.getZ(), 100);
////      assertEquals(sTestCase.getNorm(),sITRF.getNorm(),1.0e-5);
	  
  }

  public void testRoughTransformJ2000_TerRef() throws OrekitException, ParseException {

	  AbsoluteDate date = new AbsoluteDate("2003-10-14T01:58:56", UTCScale.getInstance());

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
	  
	  System.out.println("------------------------------------------");
	  System.out.println("");
	  System.out.println("tests position Ter Ref");
	  
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(posTestCase.getNorm(),posITRF.getNorm(),1.0e-5);
	  
//      // Speeds
//      
//	  Vector3D sJ2000 = new Vector3D( 3609.28229,
//			                          3322.88979,
//			                         -7083.95061);
//	  
//	  Vector3D sITRF = trans.transformVector(sJ2000);
//	  
//	  Vector3D sTestCase = new Vector3D(4410.393506412667,
//			                           -1033.6302933345141 ,
//			                           -7082.632066151947);
//
//	  // Speed tests
//	  System.out.println("------------------------------------------");
//	  System.out.println("");
//	  System.out.println("tests vitesse ter Ref");
//	  
//	  d = Vector3D.subtract(sITRF, sTestCase);
//      System.out.println("Ecarts vitesse en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
//      r = new Rotation(sITRF, sTestCase);
//      System.out.println("axe rotation vitesse " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
//      System.out.println("angle rotation vitesse " + Math.toDegrees(r.getAngle()));
////      assertEquals(0, d.getX(), 100);
////      assertEquals(0, d.getY(), 100);
////      assertEquals(0, d.getZ(), 100);
//      assertEquals(sTestCase.getNorm(),sITRF.getNorm(),1.0e-5);
  }
 
  public void testRoughTransformJ2000_TerVrai1991() throws OrekitException, ParseException {

//	  jul1950%jour = 15002_pm_entier
//	  jul1950%sec  = 180._pm_reel
//	  delta_tu1    = .5_pm_reel
//	  delta_tai    = 25._pm_reel
//	  pos_J2000(1) = 991396.024_pm_reel
//	  pos_J2000(2) = 488684.594_pm_reel
//	  pos_J2000(3) = 7109721.509_pm_reel
//	  vit_J2000(1) = 1963.575_pm_reel
//	  vit_J2000(2) = -7174.14_pm_reel
//	  vit_J2000(3) = 218.695_pm_reel
	  
	  AbsoluteDate date = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 15002*86400 + 180);

	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(991396.024,
			                           488684.594,
			                           7109721.509);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
//	  -0.221938831683687e06
//	   -0.108816598895859e07
//	    0.710889981500780e07
	  
	  Vector3D posTestCase = new Vector3D(-0.221938831683687e06,
                                          -0.108816598895859e07,
                           			       0.710889981500780e07);

	  // Position tests
	  System.out.println("------------------------------------------");
	  System.out.println("");
	  System.out.println("tests position 1991");
	  
      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
      Rotation r = new Rotation(posITRF, posTestCase);
      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      assertEquals(0, d.getX(), 100);
//      assertEquals(0, d.getY(), 100);
//      assertEquals(0, d.getZ(), 100);
      assertEquals(posTestCase.getNorm(),posITRF.getNorm(),0.00001);
	
//      // Speeds
//      
//	  Vector3D sJ2000 = new Vector3D( 3609.28229,
//			                          3322.88979,
//			                         -7083.95061);
//	  
//	  Vector3D sITRF = trans.transformVector(sJ2000);
//
////	   -0.696025140792288e04
////	    0.284069914312733e04
////	    0.216918435606272e03
//	  
//	  Vector3D sTestCase = new Vector3D(-0.696025140792288e04,
//                                         0.284069914312733e04,
//                                         0.216918435606272e03);
//
//	  // Speed tests
//	  System.out.println("------------------------------------------");
//	  System.out.println("");
//	  System.out.println("tests vitesse 1991");
//	  
//	  d = Vector3D.subtract(sITRF, sTestCase);
//      System.out.println("Ecarts vitesse en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
//      r = new Rotation(sITRF, sTestCase);
//      System.out.println("axe rotation vitesse " + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
//      System.out.println("angle rotation vitesse " + Math.toDegrees(r.getAngle()));
////      assertEquals(0, d.getX(), 100);
////      assertEquals(0, d.getY(), 100);
////      assertEquals(0, d.getZ(), 100);
////      assertEquals(sTestCase.getNorm(),sITRF.getNorm(),10e-5);
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
