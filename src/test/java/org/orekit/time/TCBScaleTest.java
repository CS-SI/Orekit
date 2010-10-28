package org.orekit.time;

import org.junit.Assert;
import org.junit.Test;


public class TCBScaleTest {

    @Test
    public void testReference() {
        TimeScale scale = TimeScalesFactory.getTCB();
        Assert.assertEquals("TCB", scale.toString());
        AbsoluteDate refTCB = new AbsoluteDate("1977-01-01T00:00:32.184", scale);
        AbsoluteDate refTAI = new AbsoluteDate("1977-01-01T00:00:00.000", TimeScalesFactory.getTAI());
        Assert.assertEquals(0.0, refTCB.durationFrom(refTAI), 6.0e-5);
    }

    @Test
    public void testRate() {
        TimeScale scale = TimeScalesFactory.getTCB();
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (double deltaT = 1.0; deltaT < 10.0; deltaT += 0.3) {
            AbsoluteDate t1 = t0.shiftedBy(deltaT);
            double tdbRate = t1.offsetFrom(t0, TimeScalesFactory.getTDB()) / deltaT;
            double tcbRate = t1.offsetFrom(t0, scale) / deltaT;
            Assert.assertEquals(tdbRate + 1.550505e-8, tcbRate, 1.0e-14);
        }
    }

}
