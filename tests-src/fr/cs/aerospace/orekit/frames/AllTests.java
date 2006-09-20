package fr.cs.aerospace.orekit.frames;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.frames"); 

    suite.addTest(fr.cs.aerospace.orekit.frames.series.AllTests.suite());
    suite.addTest(TransformTest.suite());
    suite.addTest(FrameTest.suite());
    suite.addTest(ITRF2000FrameTest.suite());
    return suite; 

  }
}
