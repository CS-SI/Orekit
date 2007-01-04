package fr.cs.aerospace.orekit.frames;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.iers.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

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

    AbsoluteDate date1 =new AbsoluteDate("2006-02-24T15:38:00",
                                             UTCScale.getInstance());
    ITRF2000Frame itrf2000 = new ITRF2000Frame(date1, true);
    Transform t0 = itrf2000.getTransformTo(Frame.getJ2000(), date1 );

    double dt = 10.0;
    AbsoluteDate date2 = new AbsoluteDate(date1, dt);
    Transform t1 = itrf2000.getTransformTo(Frame.getJ2000(), date2);
    Transform evolution = new Transform(t0.getInverse(), t1);

    assertEquals(0.0, evolution.transformPosition(new Vector3D(0,0,0)).getNorm(), 1.0e-10);
    assertTrue(Vector3D.dotProduct(Vector3D.plusK, evolution.transformVector(new Vector3D(6000,6000,0))) < 0.01);
    assertEquals(2 * Math.PI * dt / 86164, Vector3D.angle(
    		t0.transformVector(new Vector3D(6000,6000,0)), t1.transformVector(new Vector3D(6000,6000,0))), 
    		 																			1.0e-9);
    
  }

  public void testRoughOrientation() throws ParseException, OrekitException {

    AbsoluteDate date = new AbsoluteDate("2001-03-21T00:00:00",
                                             UTCScale.getInstance());
    ITRF2000Frame itrf2000 = new ITRF2000Frame(date, true);

    Vector3D u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusI) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusJ) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusI) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusJ) < Math.toRadians(2));

  }

  public void testRoughERA() throws ParseException, OrekitException {
  
    AbsoluteDate date = new AbsoluteDate("2001-03-21T00:00:00",
                                             UTCScale.getInstance());
    ITRF2000Frame itrf2000 = new ITRF2000Frame(date, true);

    assertEquals(180, Math.toDegrees(itrf2000.getEarthRotationAngle(date)), 2.0);

    date = new AbsoluteDate(date, 6 * 3600);
    assertEquals(-90, Math.toDegrees(itrf2000.getEarthRotationAngle(date)), 2.0);

    date = new AbsoluteDate(date, 6 * 3600);
    assertEquals(0, Math.toDegrees(itrf2000.getEarthRotationAngle(date)), 2.0);

    date = new AbsoluteDate(date, 6 * 3600);
    assertEquals(90, Math.toDegrees(itrf2000.getEarthRotationAngle(date)), 2.0);

  }
  
  public void testRoughTransformJ2OOO_TerVrai_one() throws OrekitException, ParseException {
	  	  
	  AbsoluteDate date = new AbsoluteDate("2003-10-14T02:00:00", UTCScale.getInstance());

	  ITRF2000Frame itrf = new ITRF2000Frame(date, true);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf, date);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(6500000.0,
			                          -1234567.0,
			                           4000000.0);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(3011109.360780633,
			                             -5889822.669411588,
			                              4002170.0385907636);

	  // Position tests
      checkVectors(posITRF, posTestCase, 2e-5, 12.0, 15.0);
      	  
  }

  public void testRoughTransformJ2000_TerRef_one() throws OrekitException, ParseException {

	  AbsoluteDate t0 = new AbsoluteDate("2003-10-14T02:00:00", UTCScale.getInstance());

	  ITRF2000Frame itrf = new ITRF2000Frame(t0, true);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf, t0);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(6500000.0,
			                          -1234567.0,
			                           4000000.0);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  
	  Vector3D posTestCase = new Vector3D(3011113.9718319275,
			                             -5889820.8187575004,
			                              4002169.292903322);

	  // Position tests
      checkVectors(posITRF, posTestCase, 2e-6, 12.0, 14.0);
      
     // velocity tests
      
      Vector3D speedJ2000 = new Vector3D(3609.28229,
    		                             3322.88979,
                                     	-7083.950661);
      
      Vector3D speedTestCase = new Vector3D(4410.401666334629,
    		                               -1033.6270183038084,
                                          -7082.627462818678);
      
      Rotation r0 = trans.getRotation();

      // compute local evolution using finite differences
      
      double h = 0.1;
      AbsoluteDate date = new AbsoluteDate(t0, -2 * h);
      Rotation evoM2h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
      double alphaM2h = -evoM2h.getAngle();
      Vector3D axisM2h = evoM2h.getAxis();
      date = new AbsoluteDate(t0, -h);
      Rotation evoM1h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
      double alphaM1h = -evoM1h.getAngle();
      Vector3D axisM1h = evoM1h.getAxis();
      date = new AbsoluteDate(t0,  h);
      Rotation evoP1h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
      double alphaP1h =  evoP1h.getAngle();
      Vector3D axisP1h = evoP1h.getAxis().negate();
      date = new AbsoluteDate(t0, 2 * h);
      Rotation evoP2h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
      double alphaP2h =  evoP2h.getAngle();
      Vector3D axisP2h = evoP2h.getAxis().negate();
      double w = (8 * (alphaP1h - alphaM1h) - (alphaP2h - alphaM2h)) / (12 * h);
      Vector3D axis = axisM2h.add(axisM1h).add(axisP1h.add(axisP2h)).normalize();
      Transform tr = new Transform(trans.getRotation() , new Vector3D(w ,axis));
      
      PVCoordinates pv = new PVCoordinates(posJ2000 , speedJ2000);
      
      PVCoordinates result = tr.transformPVCoordinates(pv);
      
      checkVectors(result.getVelocity(), speedTestCase, 1e-5, 0.02, 0.02);	
      
      result = trans.transformPVCoordinates(pv);
      checkVectors(result.getVelocity(), speedTestCase, 1e-5, 0.01, 0.02);
      
      
  }
   
  public void testRoughTransformJ2000_TerVrai1991() throws OrekitException, ParseException {
	  
	  AbsoluteDate date = new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 15002 * 86400 + 180 + 32.184 + 26);

	  ITRF2000Frame itrf = new ITRF2000Frame(date, true);	
	  
	  Transform trans = Frame.getJ2000().getTransformTo(itrf, date);
	  
	  // Positions
	  
	  Vector3D posJ2000 = new Vector3D(991396.024,
			                           488684.594,
			                           7109721.509);
	  
	  Vector3D posITRF = trans.transformPosition(posJ2000);
	  	  
	  Vector3D posTestCase = new Vector3D(-0.221938831683687e06,
                                          -0.108816598895859e07,
                           			       0.710889981500780e07);

	  // Position tests
      checkVectors(posITRF, posTestCase, 1e-4, 500.0, 600.0);

  }
  
  public void testGMS1() throws OrekitException, ParseException {
    AbsoluteDate date = new AbsoluteDate("2006-05-14T00:08:51.423", UTCScale.getInstance());
    ITRF2000Frame itrf = new ITRF2000Frame(date, false);   
    Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
    Vector3D posITRF = new Vector3D(6770000.000, -144000.000, 488000.000);
    Vector3D velITRF = new Vector3D(530.000, 4260.000, -5980.000);
    PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
    assertEquals(-4120240.360036977,  pv2000.getPosition().getX(), 1.0e-10);
    assertEquals(-5373504.716481836,  pv2000.getPosition().getY(), 1.0e-10);
    assertEquals(490761.07982380746,  pv2000.getPosition().getZ(), 1.0e-10);
    assertEquals(3509.5443642075716,  pv2000.getVelocity().getX(), 1.0e-10);
    assertEquals(-3247.8483989909146, pv2000.getVelocity().getY(), 1.0e-10);
    assertEquals(-5982.019512837689,  pv2000.getVelocity().getZ(), 1.0e-10);
  }

  public void testGMS2() throws OrekitException, ParseException {
    AbsoluteDate date = new AbsoluteDate("2006-05-14T00:16:08.631", UTCScale.getInstance());
    ITRF2000Frame itrf = new ITRF2000Frame(date, true);   
    Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
    Vector3D posITRF = new Vector3D(6254020.457, 1663297.258, -2070251.762);
    Vector3D velITRF = new Vector3D(-2861.533, 3913.691, -5536.168);
    PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
    assertEquals(-2166074.5292187054,  pv2000.getPosition().getX(), 1.0e-10);
    assertEquals(-6098691.112316115,  pv2000.getPosition().getY(), 1.0e-10);
    assertEquals(-2068661.3675358547,  pv2000.getPosition().getZ(), 1.0e-10);
    assertEquals(5287.320112599562,  pv2000.getVelocity().getX(), 1.0e-10);
    assertEquals(-11.208557244797248, pv2000.getVelocity().getY(), 1.0e-10);
    assertEquals(-5539.41752885036,  pv2000.getVelocity().getZ(), 1.0e-10);
  }

  public void testGMS3() throws OrekitException, ParseException {
    AbsoluteDate date = new AbsoluteDate("2006-05-14T00:26:06.833", UTCScale.getInstance());
    ITRF2000Frame itrf = new ITRF2000Frame(date, true);   
    Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
    Vector3D posITRF = new Vector3D(3376169.673, 3578504.767, -4685496.977);
    Vector3D velITRF = new Vector3D(-6374.220, 2284.616, -2855.447);
    PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
    assertEquals(1247881.068,  pv2000.getPosition().getX(), 100.0);
    assertEquals(-4758546.914, pv2000.getPosition().getY(), 250.0);
    assertEquals(-4686066.307, pv2000.getPosition().getZ(),   5.0);
    assertEquals(5655.84583,   pv2000.getVelocity().getX(),   0.1);
    assertEquals(4291.97158,   pv2000.getVelocity().getY(),   0.1);
    assertEquals(-2859.11413,  pv2000.getVelocity().getZ(),   0.01);
  }

  public void setUp() {
    System.setProperty("orekit.iers.directory",
                       new File(rootDir, "compressed-data").getAbsolutePath());
    AccessController.doPrivileged(new SingletonResetter());
  }

  public void tearDown() {
    System.setProperty("orekit.iers.directory", "");
    AccessController.doPrivileged(new SingletonResetter());
  }

  private static class SingletonResetter implements PrivilegedAction {
    public Object run() {
      try {
        Field instance;
        instance = UTCScale.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        instance.setAccessible(false);

        instance = IERSData.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        instance.setAccessible(false);
      } catch (SecurityException e) {

      } catch (NoSuchFieldException e) {

      } catch (IllegalArgumentException e) {

      } catch (IllegalAccessException e) {

      }
      return null;
    }
  }

  private void checkVectors(Vector3D pos1 , Vector3D pos2,
                            double deltaAngle, double deltaPos, double deltaNorm) {
	  
	  Vector3D d = pos1.subtract(pos2);
      Rotation r = new Rotation(pos1, pos2);
      
      assertEquals(pos1.getNorm(),pos2.getNorm(),1.0);
      
      assertEquals(0, d.getX(), deltaPos);
      assertEquals(0, d.getY(), deltaPos);
      assertEquals(0, d.getZ(), deltaPos);
      assertEquals(0, d.getNorm(), deltaNorm);
      
      assertEquals(0,r.getAngle(),deltaAngle);
    
  }
  
   public static Test suite() {
    return new TestSuite(ITRF2000FrameTest.class);
  }
  
}
