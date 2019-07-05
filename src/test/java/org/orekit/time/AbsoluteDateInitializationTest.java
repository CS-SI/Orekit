package org.orekit.time;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;

/**
 * Only reliably tests initialization if only a single test method is run.
 *
 * @author Evan Ward
 */
public class AbsoluteDateInitializationTest {


    @Test
    public void testAbsoluteDateInitializationWithoutLeapSeconds() {
        // setup
        Utils.setDataRoot("no-data");

        // just some code that makes an assertion using AbsoluteDate,
        // the real code under test is AbsoluteDate initialization.
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(new AbsoluteDate(date, 10).durationFrom(date), 10.0, 0.0);
    }

}
