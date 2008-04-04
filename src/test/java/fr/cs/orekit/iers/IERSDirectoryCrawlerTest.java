package fr.cs.orekit.iers;

import java.io.BufferedReader;

import fr.cs.orekit.errors.OrekitException;
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
            new IERSDirectoryCrawler().crawl(new DoNothingCrawler(".*"));
            fail("an exception should have been thrown");
        } catch (OrekitException e) {
            // expected behavior
        } catch (Exception e) {
            e.printStackTrace();
            fail("wrong exception caught");
        }
    }

    private static class DoNothingCrawler extends IERSFileCrawler {
        public DoNothingCrawler(String pattern) {
            super(pattern);
        }
        protected void visit(BufferedReader reader) {
            // do nothing
        }
    }

    public void tearDown() {
        IERSDataResetter.tearDown();
    }

    public static Test suite() {
        return new TestSuite(IERSDirectoryCrawlerTest.class);
    }

}
