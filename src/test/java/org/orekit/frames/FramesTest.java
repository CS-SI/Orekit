package org.orekit.frames;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;
import org.orekit.time.OffsetModel;
import org.orekit.time.TAIUTCDatFilesLoader.Parser;
import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/**
 * Unit tests for methods implemented in {@link Frames}.
 *
 * @author Evan Ward
 */
public class FramesTest {

    /** Time scales for testing. */
    private TimeScales timeScales;

    /**
     * Create {@link #timeScales}.
     *
     * @throws IOException on error.
     */
    @Before
    public void setUp() throws IOException {
        final String leapPath = "/USNO/tai-utc.dat";
        final String eopPath = "/rapid-data-columns/finals.daily";
        final ITRFVersionConfiguration configuration = new ITRFVersionConfiguration(
                "", ITRFVersion.ITRF_2014, Integer.MIN_VALUE, Integer.MAX_VALUE);
        final ItrfVersionProvider itrfVersionProvider = (name, mjd) -> configuration;
        final List<OffsetModel> leapSeconds = new Parser().parse(
                this.getClass().getResourceAsStream(leapPath),
                leapPath);
        this.timeScales = TimeScales.of(
                leapSeconds,
                (conventions, timeScales) -> {
                    try {
                        return EOPHistoryLoader.Parser
                                .newFinalsColumnsParser(
                                        conventions,
                                        itrfVersionProvider,
                                        timeScales,
                                        true)
                                .parse(
                                        this.getClass().getResourceAsStream(eopPath),
                                        eopPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** Check {@link Frames#of(TimeScales, Supplier)}. */
    @Test
    public void testOf() {
        // action
        Frames frames = Frames.of(timeScales, () -> null);
        Frame itrf = frames.getITRF(IERSConventions.IERS_2010, true);
        EOPHistory eopHistory = ((ITRFProvider) itrf.getTransformProvider()).getEOPHistory();
        Frame itrfEquinox = frames.getITRFEquinox(IERSConventions.IERS_2010, true);
        Frame itrfFull = frames.getITRF(IERSConventions.IERS_2010, false);
        EOPHistory eopFull = ((ITRFProvider) itrfFull.getTransformProvider()).getEOPHistory();

        // verify
        Assert.assertEquals(eopHistory.getConventions(), IERSConventions.IERS_2010);
        Assert.assertEquals(eopFull.getConventions(), IERSConventions.IERS_2010);
        Assert.assertEquals(eopHistory.getTimeScales(), timeScales);
        Assert.assertEquals(eopFull.getTimeScales(), timeScales);
        // share EOP history when conventions and tidal corrections are the same
        Assert.assertSame(
                timeScales.getUT1(IERSConventions.IERS_2010, true).getEOPHistory(),
                eopHistory);
        Assert.assertSame(
                eopHistory,
                ((ITRFProvider) itrfEquinox.getTransformProvider()).getEOPHistory());
        // changing tidal corrections still shares the same data.
        Assert.assertNotEquals(eopFull, eopHistory);
        final int n = 181;
        List<EOPEntry> entries = eopHistory.getEntries();
        List<EOPEntry> entriesFull = eopFull.getEntries();
        Assert.assertEquals(n, entries.size());
        Assert.assertEquals(n, entriesFull.size());
        for (int i = 0; i < n; i++) {
            Assert.assertSame(entries.get(i), entriesFull.get(i));
        }
        // ICRF
        Assert.assertEquals(null, frames.getICRF());
    }

}
