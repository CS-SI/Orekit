package fr.cs.aerospace.orekit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit"); 

    suite.addTest(fr.cs.aerospace.orekit.time.AllTests.suite());
    suite.addTest(fr.cs.aerospace.orekit.bodies.AllTests.suite());
    suite.addTest(fr.cs.aerospace.orekit.orbits.AllTests.suite());
    suite.addTest(fr.cs.aerospace.orekit.propagation.AllTests.suite());
    // TODO activate perturbations tests
    //suite.addTest(fr.cs.aerospace.orekit.perturbations.AllTests.suite());
    return suite; 

  }
}
