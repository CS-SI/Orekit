package fr.cs.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class AllTests extends TestCase {
  public static Test suite() {

    TestSuite suite = new TestSuite("fr.cs.orekit.attitudes");

    suite.addTest(ThirdBodyPointingAttitudeTest.suite());
    suite.addTest(NadirPointingAttitudeTest.suite());
    suite.addTest(LOFAlinedAttitudeTest.suite());

    return suite;

  }
}
