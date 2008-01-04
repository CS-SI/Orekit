package fr.cs.orekit.time;

import junit.framework.*;

public class ChunkedTimeTest
extends TestCase {

  public ChunkedTimeTest(String name) {
    super(name);
  }

  public void testOutOfRange() {
    checkConstructorCompletion(-1, 10, 10, false);
    checkConstructorCompletion(24, 10, 10, false);
    checkConstructorCompletion(10, -1, 10, false);
    checkConstructorCompletion(10, 60, 10, false);
    checkConstructorCompletion(10, 10, -1, false);
    checkConstructorCompletion(10, 10, 60, false);
  }

  public void testInRange() {
    checkConstructorCompletion(10, 10, 10, true);
  }

  public void testValues() {
    assertEquals(    0.0, new ChunkedTime( 0, 0, 0).getSecondsInDay(), 1.0e-10);
    assertEquals(21600.0, new ChunkedTime( 6, 0, 0).getSecondsInDay(), 1.0e-10);
    assertEquals(43200.0, new ChunkedTime(12, 0, 0).getSecondsInDay(), 1.0e-10);
    assertEquals(64800.0, new ChunkedTime(18, 0, 0).getSecondsInDay(), 1.0e-10);
    assertEquals(86399.9, new ChunkedTime(23, 59, 59.9).getSecondsInDay(), 1.0e-10);
  }

  public void testString() {
    assertEquals("00:00:00.000", new ChunkedTime(0).toString());
    assertEquals("06:00:00.000", new ChunkedTime(21600).toString());
    assertEquals("12:00:00.000", new ChunkedTime(43200).toString());
    assertEquals("18:00:00.000", new ChunkedTime(64800).toString());
    assertEquals("23:59:59.900", new ChunkedTime(86399.9).toString());
  }

  private void checkConstructorCompletion(int hour, int minute, double second,
                                          boolean expectedCompletion) {
    try {
      ChunkedTime time = new ChunkedTime(hour, minute, second);
      assertEquals(hour,   time.hour);
      assertEquals(minute, time.minute);
      assertEquals(second, time.second, 1.0e-10);
      assertTrue(expectedCompletion);
    } catch (IllegalArgumentException iae) {
      assertTrue(! expectedCompletion);
    } catch (Exception e) {
      fail("wrong exception caught");
    }
  }

  public static Test suite() {
    return new TestSuite(ChunkedTimeTest.class);
  }

}
