package fr.cs.aerospace.orekit.frames.series;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.frames.nutation"); 

    suite.addTest(DevelopmentTest.suite());
    return suite; 

  }
}
