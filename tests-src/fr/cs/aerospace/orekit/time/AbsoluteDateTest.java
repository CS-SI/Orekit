package fr.cs.aerospace.orekit.time;

import java.text.ParseException;
import java.util.Date;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.IERSDataResetter;

import junit.framework.*;

public class AbsoluteDateTest
  extends TestCase {

  public AbsoluteDateTest(String name) {
    super(name);
  }
  
  public void testStandardEpoch() {
    TimeScale tt = TTScale.getInstance();
    assertEquals(-210866760000000l, AbsoluteDate.JulianEpoch.toDate(tt).getTime());
    assertEquals(-3506716800000l,   AbsoluteDate.ModifiedJulianEpoch.toDate(tt).getTime());
    assertEquals(-631152000000l,    AbsoluteDate.CNES1950Epoch.toDate(tt).getTime());
    assertEquals(315964800000l,     AbsoluteDate.GPSEpoch.toDate(tt).getTime());
    assertEquals(946728000000l,     AbsoluteDate.J2000Epoch.toDate(tt).getTime());
  }
  
  public void testOutput() {
    TimeScale tt = TTScale.getInstance();
    assertEquals("1950-01-01T01:01:01.000",
                 new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 3661.0).toString(tt));
    assertEquals("2000-01-01T13:01:01.000",
                 new AbsoluteDate(AbsoluteDate.J2000Epoch, 3661.0).toString(tt));
  }
  
  public void testJ2000() {
    assertEquals("2000-01-01T12:00:00.000",
                 AbsoluteDate.J2000Epoch.toString(TTScale.getInstance()));
    assertEquals("2000-01-01T11:59:27.816",
                 AbsoluteDate.J2000Epoch.toString(TAIScale.getInstance()));
    assertEquals("2000-01-01T11:58:55.816",
                 AbsoluteDate.J2000Epoch.toString(utc));
  }

  public void testFraction() throws ParseException {
    AbsoluteDate d =
      new AbsoluteDate("2000-01-01T11:59:27.816", TAIScale.getInstance());
    assertEquals(0, d.minus(AbsoluteDate.J2000Epoch), 1.0e-10);
  }

  public void testScalesOffset() throws ParseException {
    AbsoluteDate date =
      new AbsoluteDate("2006-02-24T15:38:00", utc);
    assertEquals(33,
                 date.timeScalesOffset(TAIScale.getInstance(), utc),
                 1.0e-10);
  }

  public void testUTC() throws ParseException {
	  AbsoluteDate date =
		  new AbsoluteDate("2002-01-01T00:00:01", utc);
	  assertEquals("2002-01-01T00:00:01.000", date.toString());
  }
  
  public void test1970() throws ParseException {
	  AbsoluteDate date = new AbsoluteDate(new Date(0l), utc);
	  assertEquals("1970-01-01T00:00:00.000", date.toString());
  }

  public void setUp() throws OrekitException {
    IERSDataResetter.setUp("regular-data");
    utc = UTCScale.getInstance();
  }

  public void tearDown() {
    IERSDataResetter.tearDown();
    utc = null;
  }

  public static Test suite() {
    return new TestSuite(AbsoluteDateTest.class);
  }

  private TimeScale utc;

}
