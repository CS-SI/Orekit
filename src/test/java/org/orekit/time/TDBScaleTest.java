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

    @Test
    public void testSofa() {

        // the reference data for this test was obtained by running the following program
        // with version 2012-03-01 of the SOFA library in C
        //        double tai1, tai2, tttdb;
        //
        //        tai1 = 2448939.5;
        //        tai2 = 0.123;
        //        tttdb = iauDtdb(tai1, tai2, 0.0, 0.0, 0.0, 0.0);
        //
        //        printf("iauDtdb(%.20g, %.20g, 0.0, 0.0, 0.0, 0.0)\n  --> %.20g\n", tai1, tai2, tttdb);
        // which displays the following result:
        //        iauDtdb(2448939.5, 0.12299999999999999822, 0.0, 0.0, 0.0, 0.0)
        //        --> -0.001279984433218163669

        // the difference with SOFA is quite big (10 microseconds) because SOFA uses
        // the full Fairhead & Bretagnon model from 1990, including planetary effects,
        // whereas in Orekit we use only the conventional definition from IAU general
        // assembly 2006. So this difference is expected

        AbsoluteDate date = new AbsoluteDate(1992, 11, 13, 2, 57, 7.2,
                                             TimeScalesFactory.getTAI());
        double delta = TimeScalesFactory.getTDB().offsetFromTAI(date) -
                       TimeScalesFactory.getTT().offsetFromTAI(date);
        Assert.assertEquals(-0.001279984433218163669, delta, 1.0e-5);

    }

}
