package org.orekit.time;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.frames.EOPHistoryLoader;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;
import org.orekit.frames.ItrfVersionProvider;
import org.orekit.time.TAIUTCDatFilesLoader.Parser;
import org.orekit.utils.IERSConventions;

/**
 * Unit tests for {@link PreloadedTimeScales}
 *
 * @author Evan Ward
 */
public class PreloadedTimeScalesTest {

    /** Subject under test. */
    private TimeScales timeScales;

    /**
     * Create the time scales under test.
     *
     * @throws IOException on error.
     */
    @Before
    public void setUp() throws IOException {
        final String leapPath = "/USNO/tai-utc.dat";
        final String eopPath =
                "/regular-data/Earth-orientation-parameters/yearly/finals2000A.2002.xml";
        final ITRFVersionConfiguration configuration = new ITRFVersionConfiguration(
                "", ITRFVersion.ITRF_2014, Integer.MIN_VALUE, Integer.MAX_VALUE);
        final ItrfVersionProvider itrfVersionProvider = (name, mjd) -> configuration;
        final List<OffsetModel> leapSeconds = new Parser().parse(
                PreloadedTimeScales.class.getResourceAsStream(leapPath),
                leapPath);
        this.timeScales = TimeScales.of(
                leapSeconds,
                (conventions, timeScales) -> {
                    try {
                        return EOPHistoryLoader.Parser
                                .newFinalsXmlParser(conventions, itrfVersionProvider, timeScales)
                                .parse(PreloadedTimeScales.class.getResourceAsStream(eopPath), eopPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** Rough check that the time scales work as expected. */
    @Test
    public void testTime() {
        // actions
        AbsoluteDate javaEpoch = timeScales.getJavaEpoch();
        AbsoluteDate j2000Epoch = timeScales.getJ2000Epoch();
        TAIScale tai = timeScales.getTAI();
        UTCScale utc = timeScales.getUTC();
        UT1Scale ut1 = timeScales.getUT1(IERSConventions.IERS_2010, true);
        AbsoluteDate date = new AbsoluteDate(2002, 12, 31, utc);
        AbsoluteDate date2 = new AbsoluteDate(1977, 1, 1, tai);
        AbsoluteDate date3 = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, ut1);

        // verify
        Assert.assertEquals("1970-01-01T00:00:00.000", javaEpoch.toString(utc));
        Assert.assertEquals(-32, utc.offsetFromTAI(date), 0);
        Assert.assertEquals(-32.2889120, ut1.offsetFromTAI(date), 1e-14);
        Assert.assertEquals(0, tai.offsetFromTAI(date), 0);
        Assert.assertEquals(-32, timeScales.getUTC().offsetFromTAI(date), 0);
        Assert.assertEquals(-32 + 24110.54841, timeScales.getGMST(IERSConventions.IERS_2010, true).offsetFromTAI(date3), 0);
        Assert.assertEquals(-32 + 3 * 3600, timeScales.getGLONASS().offsetFromTAI(date), 0);
        Assert.assertEquals(-19, timeScales.getGPS().offsetFromTAI(date), 0);
        Assert.assertEquals(-19, timeScales.getQZSS().offsetFromTAI(date), 0);
        Assert.assertEquals(-19, timeScales.getGST().offsetFromTAI(date), 0);
        Assert.assertEquals(-19, timeScales.getIRNSS().offsetFromTAI(date), 0);
        Assert.assertEquals(32.184, timeScales.getTT().offsetFromTAI(date), 0);
        Assert.assertEquals(32.184, timeScales.getTDB().offsetFromTAI(j2000Epoch.shiftedBy(216525.908119)), 0);
        Assert.assertEquals(32.184, timeScales.getTCB().offsetFromTAI(date2), 1e-4);
        Assert.assertEquals(32.184, timeScales.getTCG().offsetFromTAI(date2), 0);
    }

    /** Check the UT1 creation logic. */
    @Test
    public void testUt1() {
        // setup
        UT1Scale ut12010Simple = timeScales.getUT1(IERSConventions.IERS_2010, true);
        UT1Scale ut12010Full = timeScales.getUT1(IERSConventions.IERS_2010, false);
        UT1Scale ut12003Simple = timeScales.getUT1(IERSConventions.IERS_2003, true);
        UT1Scale ut12003Full = timeScales.getUT1(IERSConventions.IERS_2003, false);
        UT1Scale ut11996Simple = timeScales.getUT1(IERSConventions.IERS_1996, true);
        UT1Scale ut11996Full = timeScales.getUT1(IERSConventions.IERS_1996, false);
        UTCScale utc = timeScales.getUTC();
        AbsoluteDate date = new AbsoluteDate(2002, 12, 31, utc);


        // verify
        Assert.assertSame(ut12010Simple, timeScales.getUT1(IERSConventions.IERS_2010, true));
        Assert.assertSame(ut12010Full, timeScales.getUT1(IERSConventions.IERS_2010, false));
        Assert.assertSame(ut12003Simple, timeScales.getUT1(IERSConventions.IERS_2003, true));
        Assert.assertSame(ut12003Full, timeScales.getUT1(IERSConventions.IERS_2003, false));
        Assert.assertSame(ut11996Simple, timeScales.getUT1(IERSConventions.IERS_1996, true));
        Assert.assertSame(ut11996Full, timeScales.getUT1(IERSConventions.IERS_1996, false));


        Assert.assertEquals(-32.2889120, ut12010Simple.offsetFromTAI(date), 1e-14);
        Assert.assertEquals(-32.2889120, ut12003Simple.offsetFromTAI(date), 1e-14);
        Assert.assertEquals(-32.2889120, ut11996Simple.offsetFromTAI(date), 1e-14);
        Assert.assertEquals(-2e-5, -32.2889120 - ut12010Full.offsetFromTAI(date), 1e-5);
        Assert.assertEquals(-2e-5, -32.2889120 - ut12003Full.offsetFromTAI(date), 1e-5);
        Assert.assertEquals(-2e-5, -32.2889120 - ut11996Full.offsetFromTAI(date), 1e-5);

    }

}
