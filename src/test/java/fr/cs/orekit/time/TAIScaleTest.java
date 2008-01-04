package fr.cs.orekit.time;

import junit.framework.*;

public class TAIScaleTest
  extends TestCase {

  public TAIScaleTest(String name) {
    super(name);
  }

  public void testSymetry() {
    // the loop is around the 1977-01-01 leap second introduction
    double tLeap = 220924815;
    TimeScale scale = TAIScale.getInstance();
    for (double taiTime = tLeap - 60; taiTime < tLeap + 60; taiTime += 0.3) {
      double dt1 = scale.offsetFromTAI(taiTime);
      double dt2 = scale.offsetToTAI(taiTime + dt1);
      assertEquals( 0.0, dt1 + dt2, 1.0e-10);
    }
  }

  public static Test suite() {
    return new TestSuite(TAIScaleTest.class);
  }

}
