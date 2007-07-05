package fr.cs.orekit.orbits;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.orekit.orbits"); 

    suite.addTest(CartesianParametersTest.suite());
    suite.addTest(KeplerianParametersTest.suite());
    suite.addTest(EquinoctialParametersTest.suite());
    suite.addTest(CircularParametersTest.suite());
        
    return suite; 

  }
}
