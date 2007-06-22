package fr.cs.aerospace.orekit.iers;

import java.text.ParseException;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.IERSDataResetter;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UTCTAIHistoryFilesLoaderTest extends TestCase {

  public void testRegular() throws OrekitException {
    checkSuccess("regular-data");
    assertEquals(-32.0, UTCScale.getInstance().offsetFromTAI(946684800), 10e-8);    
  }

  public void testCompressed() throws OrekitException {
    checkSuccess("compressed-data");
    assertEquals(-32.0, UTCScale.getInstance().offsetFromTAI(946684800), 10e-8);    
  }

  public void testNoData() throws OrekitException {
    checkSuccess("empty-directory");
    assertEquals(0.0, UTCScale.getInstance().offsetFromTAI(946684800), 10e-8);    
  }

  public void testUTCDate() throws OrekitException, ParseException {
    checkSuccess("regular-data");
    UTCScale scale = (UTCScale) UTCScale.getInstance();
    AbsoluteDate startDate = scale.getStartDate();
    double delta = startDate.minus(new AbsoluteDate("1972-01-01T00:00:00", scale));
    assertEquals(0, delta, 0);
  }

  private void checkSuccess(String directoryName) {
    try {
      IERSDataResetter.setUp(directoryName);
      assertNotNull(UTCScale.getInstance());
    } catch (OrekitException e) {
      fail(e.getMessage());
    }
  }

  public void tearDown() {
    IERSDataResetter.tearDown();
  }

  public static Test suite() {
    return new TestSuite(UTCTAIHistoryFilesLoaderTest.class);
  }

}
