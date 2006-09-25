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
import fr.cs.aerospace.orekit.time.TAIScale;
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
  
//  public void testRoughTransformJ2OOO_TerVrai_one() throws OrekitException, ParseException {
//	  	  
//	  AbsoluteDate date = new AbsoluteDate("2003-10-14T02:00:00", TAIScale.getInstance());
//
//	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
//	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
//	  
//	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
//	  
//	  // Positions
//	  
//	  Vector3D posJ2000 = new Vector3D(6500000.0,
//			                          -1234567.0,
//			                           4000000.0);
//	  
//	  Vector3D posITRF = trans.transformPosition(posJ2000);
//	  
//	  Vector3D posTestCase = new Vector3D(3011109.360780633,
//			                             -5889822.669411588,
//			                              4002170.0385907636);
//
//	  // Position tests
//	  
//	/*  System.out.println("------------------------------------------");
//	  System.out.println("");
//	  System.out.println("tests position Ter Vrai");
//	  
//      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
//      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
//      Rotation r = new Rotation(posITRF, posTestCase);
//      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
//      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//    
//      checkClosePosition(posITRF, posTestCase, 2e-5, 100.0, 100.0);
//      
//      // speed tests
//      
//      Vector3D speedJ2000 = new Vector3D(3609.28229,
//    		                             3322.88979,
//                                     	-7083.950661);
//      
//      Vector3D speedTestCase = new Vector3D(4410.401666334629,
//    		                               -1033.6270183038084,
//                                 ss          -7082.627462818678);
//      
//      Vector3D axe= trans.getRotation().getAxis();
//      
//      fSynch.setDate(new AbsoluteDate("2003-10-14T01:59:27.500", TAIScale.getInstance()));
//      
//      double A = Frame.getJ2000().getTransformTo(itrf).getRotation().getAngle();
//      
//      fSynch.setDate(new AbsoluteDate("2003-10-14T01:59:28.500", TAIScale.getInstance()));
//      
//      double w = Frame.getJ2000().getTransformTo(itrf).getRotation().getAngle() - A ;
//      
//      System.out.println("omega : " + w);
//      
//      Vector3D rot = Vector3D.multiply(w,axe);
//      
//      
//      Vector3D speedITRF = trans.transformVector(Vector3D.add(speedJ2000,Vector3D.crossProduct(rot,posJ2000)));
//      
//      System.out.println("vit en metres " + speedITRF.getX() + " " + speedITRF.getY() + " " + speedITRF.getZ());
//
//      checkClosePosition(speedITRF, speedTestCase, 1e-3, 1.0, 1.0);*/
//      	  
//  }

  public void testRoughTransformJ2000_TerRef_one() throws OrekitException, ParseException {

	  AbsoluteDate date = new AbsoluteDate("2003-10-14T02:00:00", TAIScale.getInstance());

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
      
      checkClosePosition(posITRF, posTestCase, 2e-5, 100.0, 100.0);
	  
  }
   
//  public void testRoughTransformJ2000_TerVrai1991() throws OrekitException, ParseException {
//	  
//	  AbsoluteDate date = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 15002*86400 + 180);
//
//	  FrameSynchronizer fSynch = new FrameSynchronizer(date);
//	  ITRF2000Frame itrf = new ITRF2000Frame(fSynch);	
//	  
//	  Transform trans = Frame.getJ2000().getTransformTo(itrf);
//	  
//	  // Positions
//	  
//	  Vector3D posJ2000 = new Vector3D(991396.024,
//			                           488684.594,
//			                           7109721.509);
//	  
//	  Vector3D posITRF = trans.transformPosition(posJ2000);
//	  	  
//	  Vector3D posTestCase = new Vector3D(-0.221938831683687e06,
//                                          -0.108816598895859e07,
//                           			       0.710889981500780e07);
//
//	  // Position tests
//	  System.out.println("------------------------------------------");
//	  System.out.println("");
//	  System.out.println("tests position 1991");
//	  
//      Vector3D d = Vector3D.subtract(posITRF, posTestCase);
//      System.out.println("Ecarts position en metres " + d.getX() + " " + d.getY() + " " + d.getZ() + " " + d.getNorm());
//      Rotation r = new Rotation(posITRF, posTestCase);
//      System.out.println("axe rotation position" + r.getAxis().getX() + " " + r.getAxis().getY() + " " + r.getAxis().getZ());
//      System.out.println("angle rotation position " + Math.toDegrees(r.getAngle()));
//      
//      checkClosePosition(posITRF, posTestCase, 1e-4, 500.0, 600.0);
//
//  }
  
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
  
  private void checkClosePosition(Vector3D pos1 , Vector3D pos2, double deltaAngle, double deltaPos, double deltaNorm){
	  
	  Vector3D d = Vector3D.subtract(pos1, pos2);
      Rotation r = new Rotation(pos1, pos2);
      
      assertEquals(pos1.getNorm(),pos2.getNorm(),1.0);
      
      assertEquals(0, d.getX(), deltaPos);
      assertEquals(0, d.getY(), deltaPos);
      assertEquals(0, d.getZ(), deltaPos);
      assertEquals(0,d.getNorm(),deltaNorm);
      
      assertEquals(0,r.getAngle(),deltaAngle);
    
  }
  
   public static Test suite() {
    return new TestSuite(ITRF2000FrameTest.class);
  }
  
}
