package fr.cs.aerospace.orekit.time;

import java.text.ParseException;

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
    assertEquals(new String("1950-01-01T01:01:01.000"),
                 new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 3661.0).toString(tt));
    assertEquals(new String("2000-01-01T13:01:01.000"),
                 new AbsoluteDate(AbsoluteDate.J2000Epoch, 3661.0).toString(tt));
  }
  
  public void testJ2000() {
    assertEquals(new String("2000-01-01T12:00:00.000"),
                 AbsoluteDate.J2000Epoch.toString(TTScale.getInstance()));
    assertEquals(new String("2000-01-01T11:59:27.816"),
                 AbsoluteDate.J2000Epoch.toString(TAIScale.getInstance()));
    assertEquals(new String("2000-01-01T11:58:55.816"),
                 AbsoluteDate.J2000Epoch.toString(UTCScale.getInstance()));
  }

  public void testScalesOffset() throws ParseException {
    AbsoluteDate date =
      new AbsoluteDate("2006-02-24T15:38:00", UTCScale.getInstance());
    assertEquals(33,
                 date.timeScalesOffset(TAIScale.getInstance(),
                                       UTCScale.getInstance()),
                 1.0e-10);
  }

  public static Test suite() {
    return new TestSuite(AbsoluteDateTest.class);
  }

}
