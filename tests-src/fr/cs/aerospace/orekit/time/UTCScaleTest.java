package fr.cs.aerospace.orekit.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.IERSDataResetter;

import junit.framework.*;

public class UTCScaleTest
  extends TestCase {

  public UTCScaleTest(String name) {
    super(name);
    utc = null;
  }
  
  public void testNoLeap() throws ParseException {
    AbsoluteDate d1 = new AbsoluteDate("1999-12-31T23:59:59", utc);
    AbsoluteDate d2 = new AbsoluteDate("2000-01-01T00:00:01", utc);
    assertEquals(2.0, d2.minus(d1), 1.0e-10);
  }
  
  public void testLeap2006() throws ParseException {
    AbsoluteDate leapDate =
      new AbsoluteDate("2006-01-01T00:00:00", utc);
    AbsoluteDate d1 = new AbsoluteDate(leapDate, -1);
    AbsoluteDate d2 = new AbsoluteDate(leapDate, +1);
    assertEquals(2.0, d2.minus(d1), 1.0e-10);

    AbsoluteDate d3 = new AbsoluteDate("2005-12-31T23:59:59", utc);
    AbsoluteDate d4 = new AbsoluteDate("2006-01-01T00:00:01", utc);
    assertEquals(3.0, d4.minus(d3), 1.0e-10);

  }

  public void testDuringLeap() throws ParseException {
    AbsoluteDate d = new AbsoluteDate("1983-06-30T23:59:59", utc);
    assertEquals("1983-06-30T23:59:59.000", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.251", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.502", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.753", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.004", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.255", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.506", d.toString(utc));
    d = new AbsoluteDate(d ,0.251);
    assertEquals("1983-06-30T23:59:59.757", d.toString(utc));
  }

  public void testSymetry() {
    // the loop is around the 1977-01-01 leap second introduction
    double tLeap = 220924815;
    TimeScale scale = utc;
    for (double taiTime = tLeap - 60; taiTime < tLeap + 60; taiTime += 0.3) {
      double dt1 = scale.offsetFromTAI(taiTime);
      double dt2 = scale.offsetToTAI(taiTime + dt1);
      if ((taiTime > tLeap) && (taiTime <= tLeap + 1.0)) {
        // we are "inside" the leap second, the TAI scale goes on
        // but the UTC scale "replays" the previous second, before the step
        assertEquals(-1.0, dt1 + dt2, 1.0e-10);
      } else {
        assertEquals( 0.0, dt1 + dt2, 1.0e-10);        
      }
    }
  }

  public void testOffsets() throws ParseException {
    checkOffset("1970-01-01",   0);
    checkOffset("1972-03-05", -10);
    checkOffset("1972-07-14", -11);
    checkOffset("1979-12-31", -18);
    checkOffset("1980-01-22", -19);
    checkOffset("2006-07-07", -33);
  }

  private void checkOffset(String date, double offset) {
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      double time = format.parse(date).getTime() * 0.001;
      assertEquals(offset,
                   utc.offsetFromTAI(time),
                   1.0e-10);
    } catch (ParseException pe) {
      fail(pe.getMessage());
    }
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
    return new TestSuite(UTCScaleTest.class);
  }

  private TimeScale utc;

}
