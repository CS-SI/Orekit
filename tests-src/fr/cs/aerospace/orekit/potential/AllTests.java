package fr.cs.aerospace.orekit.potential;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
  public static Test suite() { 
    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.perturbations"); 
    suite.addTest(SHMFormatReaderTest.suite());
    suite.addTest(EGMFormatReaderTest.suite());

    return suite; 
  }
}
