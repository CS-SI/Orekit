package fr.cs.aerospace.orekit.iers;

import java.io.BufferedReader;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.IERSDataResetter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IERSDirectoryCrawlerTest extends TestCase {

  public void testNoDirectory() {
    checkFailure("inexistant-directory");
  }

  public void testNotADirectory() {
    checkFailure("regular-data/UTC-TAI.history.gz");
  }

  private void checkFailure(String directoryName) {
    try {
      IERSDataResetter.setUp(directoryName);
      new IERSDirectoryCrawler().crawl(new IERSFileVisitor(".*") {
        protected void visit(BufferedReader reader) {
          // do nothing
        }
      });
      fail("an exeption should have been thrown");
    } catch (OrekitException e) {
      // expected behaviour
    } catch (Exception e) {
      e.printStackTrace();
      fail("wrong exception caught");
    }
  }

  public void tearDown() {
    IERSDataResetter.tearDown();
  }

  public static Test suite() {
    return new TestSuite(IERSDirectoryCrawlerTest.class);
  }

}
