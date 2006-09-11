package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ITRF2000Test extends TestCase {
  
  public void testITRF2000Constructor() {
	  
  }
  
  public void testRotationAngle() {

  }
  
  public void testNutation() {
	  
  }
  
  public void testTransform() {
	 
  }
  
  public void testUpdate() {
	 
  }
  
  private void checkSameTransform(Transform transform1, Transform transform2) {	   
	      assertEquals(0, Vector3D.subtract(transform1.getTranslation() , transform2.getTranslation()).getNorm(), 1.0e-10);
	      assertEquals(0, transform1.getTranslation().getX()-transform2.getTranslation().getX(), 1.0e-10);
	      assertEquals(0, transform1.getRotation().getAngle()-transform2.getRotation().getAngle(), 1.0e-10);
	      assertEquals(0, Vector3D.subtract(transform1.getRotation().getAxis(), transform2.getRotation().getAxis()).getNorm() , 1.0e-10);
	  }
	  
  public static Test suite() {
      return new TestSuite(FrameTest.class);
  }  
  
}
