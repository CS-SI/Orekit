package fr.cs.orekit.frames;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {

    TestSuite suite = new TestSuite("fr.cs.orekit.frames");

    suite.addTest(fr.cs.orekit.frames.series.AllTests.suite());
    suite.addTest(TransformTest.suite());
    suite.addTest(FrameTest.suite());
    suite.addTest(ITRF2000FrameTest.suite());
    return suite;

  }
}
