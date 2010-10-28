package org.orekit.time;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Test;


public class TDBScaleTest {

    @Test
    public void dummy() throws FileNotFoundException {
        PrintStream out = new PrintStream("/home/luc/x.dat");
        for (int d = 0; d < 366; ++d) {
            for (int h = 0; h < 24; ++h) {
                final double dtDays = d + h / 24.0;
                final double g1 = FastMath.toRadians(357.53 + 0.9856003 * dtDays);
                final double g2 = FastMath.toRadians(359.534953014 + 0.9856003 * dtDays);
                out.println(dtDays + " " +
                            (0.001658 * FastMath.sin(g1) + 0.000014 * FastMath.sin(2 * g1)) + " " +
                            (0.001658 * FastMath.sin(g2) + 0.000014 * FastMath.sin(2 * g2)));
            }
        }
        out.close();
    }

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
