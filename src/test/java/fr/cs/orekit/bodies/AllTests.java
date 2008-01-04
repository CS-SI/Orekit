package fr.cs.orekit.bodies;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {

    TestSuite suite = new TestSuite("fr.cs.orekit.bodies");

    suite.addTest(OneAxisEllipsoidTest.suite());
    suite.addTest(SunTest.suite());
    suite.addTest(MoonTest.suite());

    return suite;

  }
}
