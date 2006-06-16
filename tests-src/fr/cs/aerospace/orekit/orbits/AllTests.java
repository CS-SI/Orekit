package fr.cs.aerospace.orekit.orbits;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.orbits"); 

    suite.addTest(CartesianParametersTest.suite());
    suite.addTest(KeplerianParametersTest.suite());
    suite.addTest(EquinoctialParametersTest.suite());
    suite.addTest(CircularParametersTest.suite());
    
    // TODO a valider
    suite.addTest(KeplerianDerivativesAdderTest.suite());
    suite.addTest(EquinoctialDerivativesAdderTest.suite());
    
    return suite; 

  }
}
