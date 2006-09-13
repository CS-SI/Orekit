package fr.cs.aerospace.orekit.iers;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.iers"); 

    suite.addTest(IERSDataTest.suite());
    return suite; 

  }
}
