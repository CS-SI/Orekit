package fr.cs.aerospace.orekit.extrapolation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.extrapolation"); 

    suite.addTest(NumericalExtrapolatorTest.suite());

    return suite; 

  }
}
