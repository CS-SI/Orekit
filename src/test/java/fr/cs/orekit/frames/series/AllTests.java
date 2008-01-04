package fr.cs.orekit.frames.series;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {

    TestSuite suite = new TestSuite("fr.cs.orekit.frames.nutation");

    suite.addTest(DevelopmentTest.suite());
    return suite;

  }
}
