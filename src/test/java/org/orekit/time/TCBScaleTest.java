package org.orekit.time;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.utils.Constants;


public class TCBScaleTest {

    @Test
    public void testReference() {
        TimeScale tcb = TimeScalesFactory.getTCB();
        TimeScale tdb = TimeScalesFactory.getTDB();
        Assert.assertEquals("TCB", tcb.toString());
        AbsoluteDate refTCB = new AbsoluteDate("1977-01-01T00:00:32.184", tcb);
        AbsoluteDate refTDB = new AbsoluteDate("1977-01-01T00:00:32.184", tdb);
        Assert.assertEquals(0.0, refTCB.durationFrom(refTDB), 1.0e-12);
    }

    @Test
    public void testRate() {
        TimeScale tcb = TimeScalesFactory.getTCB();
        TimeScale tdb = TimeScalesFactory.getTDB();
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (double deltaT = 1.0; deltaT < 10.0; deltaT += 0.3) {
            AbsoluteDate t1 = t0.shiftedBy(deltaT);
            double tdbRate = t1.offsetFrom(t0, tdb) / deltaT;
            double tcbRate = t1.offsetFrom(t0, tcb) / deltaT;
            Assert.assertEquals(tdbRate + 1.550505e-8, tcbRate, 1.0e-14);
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getTCB();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

}
