package fr.cs.aerospace.orekit.frames;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.frames"); 

    suite.addTest(TransformTest.suite());
    suite.addTest(FrameTest.suite());
    return suite; 

  }
}
