package fr.cs.orekit.iers;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {

    TestSuite suite = new TestSuite("fr.cs.orekit.iers");

    suite.addTest(IERSDirectoryCrawlerTest.suite());
    suite.addTest(UTCTAIHistoryFilesLoaderTest.suite());
    suite.addTest(EOPC04FilesLoaderTest.suite());
    suite.addTest(BulletinBFilesLoaderTest.suite());
    return suite;

  }
}
