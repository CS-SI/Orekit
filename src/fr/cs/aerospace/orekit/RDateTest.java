package fr.cs.aerospace.orekit;

import junit.framework.*;

public class RDateTest
  extends TestCase {

  public RDateTest(String name) {
    super(name);
  }

  public void testStandardEpoch() {
    assertEquals(-210866760000000l, RDate.JulianEpoch.getTime());
    assertEquals(-3506716800000l,   RDate.ModifiedJulianEpoch.getTime());
    assertEquals(-631152000000l,    RDate.CNES1950Epoch.getTime());
    assertEquals(315964800000l,     RDate.GPSEpoch.getTime());
    assertEquals(946728000000l,     RDate.J2000Epoch.getTime());
  }
  
  public void testInvariance() {
    RDate t = new RDate(RDate.CNES1950Epoch, 3661.0);
    t.setEpoch(RDate.J2000Epoch);
    assertEquals(-1577876339.0, t.getOffset(), 1.0e-5);
    t.setEpoch(RDate.CNES1950Epoch);
    assertEquals(3661.0, t.getOffset(), 1.0e-5);
  }
  
  public void testOutput() {
    assertEquals(new String("1950-01-01T01:01:01"),
                 new RDate(RDate.CNES1950Epoch, 3661.0).toString());
    assertEquals(new String("2000-01-01T13:01:01"),
                 new RDate(RDate.J2000Epoch, 3661.0).toString());
  }
  
  public static Test suite() {
    return new TestSuite(RDateTest.class);
  }

}
