package org.orekit.time;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link LazyLoadedTimeScales}.
 *
 * @author Evan Ward
 */
public class LazyLoadedTimeScalesTest {

    /** Subject under test. */
    private LazyLoadedTimeScales timeScales;

    /** Create subject under test. */
    @BeforeEach
    public void setUp() {
        LazyLoadedDataContext defaultContext =
                (LazyLoadedDataContext) Utils.setDataRoot("regular-data");
        timeScales = defaultContext.getTimeScales();
    }

    /**
     * Check {@link LazyLoadedTimeScales#getGMST(IERSConventions, boolean)} uses it's
     * parameters. See issue #627.
     */
    @Test
    public void testGetGMST() {
        DateTimeComponents reference = new DateTimeComponents(2000, 1, 1, 12, 0, 0.0);
        List<TimeScale> scales = new ArrayList<>();

        for (IERSConventions conventions : IERSConventions.values()) {
            for (boolean simpleEop : new boolean[]{true, false}) {
                // setup
                GMSTScale gmst = timeScales.getGMST(conventions, simpleEop);
                UT1Scale ut1 = timeScales.getUT1(conventions, simpleEop);
                AbsoluteDate date = new AbsoluteDate(reference, ut1);
                String message = conventions + " " + simpleEop;

                // verify
                Assertions.assertSame(gmst, timeScales.getGMST(conventions, simpleEop), message);
                double expected = 24110.54841 + ut1.offsetFromTAI(date);
                Assertions.assertEquals(expected, gmst.offsetFromTAI(date), 0, message);
                Assertions.assertTrue(!scales.contains(gmst), message + " " + scales);
                scales.add(gmst);
            }
        }
    }

    /** Check {@link LazyLoadedTimeScales#getUT1(IERSConventions, boolean)}. */
    @Test
    public void testGetUt1() {
        UTCScale utc = timeScales.getUTC();
        DateTimeComponents reference = new DateTimeComponents(2004, 2, 1);
        AbsoluteDate date = new AbsoluteDate(reference, utc);
        List<TimeScale> scales = new ArrayList<>();

        for (IERSConventions conventions : IERSConventions.values()) {
            for (boolean simpleEop : new boolean[]{true, false}) {
                // setup
                UT1Scale ut1 = timeScales.getUT1(conventions, simpleEop);
                String message = conventions + " " + simpleEop;

                // verify
                Assertions.assertSame(ut1, timeScales.getUT1(conventions, simpleEop), message);
                Assertions.assertSame(ut1.getEOPHistory().getConventions(), conventions);
                double expected = utc.offsetFromTAI(date);
                if (conventions != IERSConventions.IERS_1996) {
                    expected += -0.4051590;
                }
                if (!simpleEop) {
                    expected += conventions.getEOPTidalCorrection(timeScales).value(date)[2];
                }
                Assertions.assertEquals(expected, ut1.offsetFromTAI(date), 0, message);
                Assertions.assertTrue(!scales.contains(ut1), message + " " + scales);
                scales.add(ut1);
            }
        }
    }

    /**
     * Tests fix for issue-1296
     */
    @RepeatedTest(10)
    void testGetUtcLazyInit() throws InterruptedException, ExecutionException {
        LazyLoadedDataContext defaultContext =
            (LazyLoadedDataContext) Utils.setDataRoot("regular-data");

        LazyLoadedTimeScales ts = defaultContext.getTimeScales();

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        try {
            List<Future<UTCScale>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(executorService.submit(ts::getUTC));
            }

            for (Future<?> f : futures) {
                try {
                    assertNotNull(f.get());
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ConcurrentModificationException) {
                        fail("ConcurrentModificationException detected, not thread safe");
                    }
                    throw e;
                }
            }

        } finally {
            executorService.shutdown();
        }
    }

}
