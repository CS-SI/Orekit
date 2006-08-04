package fr.cs.aerospace.orekit.time;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.time"); 

    suite.addTest(AbsoluteDateTest.suite());
    suite.addTest(UTCScaleTest.suite());
    suite.addTest(TAIScaleTest.suite());
    suite.addTest(TTScaleTest.suite());
    suite.addTest(TCGScaleTest.suite());
    return suite; 

  }
}
