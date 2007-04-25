package fr.cs.aerospace.orekit.perturbations;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 
    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.perturbations"); 
    suite.addTest(DragTest.suite());
    suite.addTest(SolarRadiationPressureTest.suite());
    suite.addTest(ThirdBodyAttractionTest.suite());
    suite.addTest(CunninghamAttractionModelTest.suite());
    suite.addTest(DrozinerAttractionModelTest.suite());
    suite.addTest(DTM2000AtmosphereTest.suite());
    suite.addTest(JB2006AtmosphereTest.suite());
     return suite; 
  }
}
