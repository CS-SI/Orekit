package org.orekit.time;

import org.junit.Assert;
import org.junit.Test;


public class TDBScaleTest {

    @Test
    public void testReference() {
        TimeScale scale = TimeScalesFactory.getTDB();
        Assert.assertEquals("TDB", scale.toString());
        Assert.assertEquals(32.183927340791372839, scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH), 1.0e-15);
    }

    @Test
    public void testDate5000000() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000);
        Assert.assertEquals(32.185364155950634549, scale.offsetFromTAI(date), 1.0e-13);
    }

    @Test
    public void testToTAI5000000() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = new AbsoluteDate(2000, 2, 28, 8, 53, 20.001364155950634549, scale);
        double dt = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000).durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

    @Test
    public void testToTAI() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 11, 59, 59.999927340791372839, scale);
        double dt = AbsoluteDate.J2000_EPOCH.durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

}
