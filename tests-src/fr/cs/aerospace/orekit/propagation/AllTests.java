package fr.cs.aerospace.orekit.propagation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.extrapolation"); 

    suite.addTest(KeplerianPropagatorTest.suite());
    suite.addTest(EcksteinHechlerPropagatorTest.suite());
    // TODO valider ces tests ...
    suite.addTest(NumericalPropagatorTest.suite());
    suite.addTest(IntegratedEphemerisTest.suite());
    suite.addTest(TabulatedEphemerisTest.suite());

    return suite; 

  }
}
