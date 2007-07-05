package fr.cs.orekit.tle;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.orekit.tle"); 

    suite.addTest(tleTest.suite());
    
    return suite; 

  }
}
