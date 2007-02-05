package fr.cs.aerospace.orekit.tle;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.tle"); 

    suite.addTest(tleTest.suite());
    
    return suite; 

  }
}
