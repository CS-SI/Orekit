/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.frames;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedCache;


public class TidalCorrectionTest {

    // Computation date
    private AbsoluteDate date;

    @Test
    public void testPoleCorrection() {

        // compute the pole motion component for tidal correction
        final PoleCorrection tidalCorr = new TidalCorrection().getPoleCorrection(date);

        Assert.assertEquals(FastMath.toRadians(-204.09237446540885e-6 / 3600), tidalCorr.getXp(), 2.0e-14);
        Assert.assertEquals(FastMath.toRadians(-161.48436351246889e-6 / 3600), tidalCorr.getYp(), 0.7e-14);

    }

    @Test
    public void testDUT1() {

        // compute the dut1 component for tidal correction
        final double tidalCorr = new TidalCorrection().getDUT1(date);

        Assert.assertEquals(13.611556854809763e-6, tidalCorr, 1.5e-10);
    }

    @Test
    public void testCachePole() {

        final TidalCorrection tc = new TidalCorrection();

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr = tc.getPoleCorrection(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TimeScalesFactory.getTAI());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr2 = tc.getPoleCorrection(date2);

        Assert.assertFalse(poleCorr.getXp() == poleCorr2.getXp());
        Assert.assertFalse(poleCorr.getYp() == poleCorr2.getYp());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr3 = tc.getPoleCorrection(date2);

        Assert.assertTrue(poleCorr2.getXp() == poleCorr3.getXp());
        Assert.assertTrue(poleCorr2.getYp() == poleCorr3.getYp());

    }

    @Test
    public void testCacheDUT1() {

        final TidalCorrection tc = new TidalCorrection();

        // compute the dut1 component for tidal correction
        final double dut1Corr = tc.getDUT1(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TimeScalesFactory.getTAI());

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr2 = tc.getDUT1(date2);

        Assert.assertFalse(dut1Corr == dut1Corr2);

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr3 = tc.getDUT1(date2);

        Assert.assertTrue(dut1Corr2 == dut1Corr3);

    }

    @Test
    public void testCacheValidation() {
        // validates that results calculated using an internal cache are the same
        // as without such a cache (the cache is effectively disabled).

        final TidalCorrection tcCache = new TidalCorrection();
        final TidalCorrection tcDirect = new TidalCorrection();
        disableTimeStampedCache(tcDirect);

        for (int i = 0; i < 100; i++) {
            // compute the dut1 component for tidal correction
            double dut1Cache  = tcCache.getDUT1(date.shiftedBy(i * Constants.JULIAN_DAY));
            double dut1Direct = tcDirect.getDUT1(date.shiftedBy(i * Constants.JULIAN_DAY));

            Assert.assertEquals(dut1Direct, dut1Cache, 1e-12);

            // compute the dut1 component for tidal correction
            dut1Cache  = tcCache.getDUT1(date.shiftedBy(i * Constants.JULIAN_DAY + Constants.JULIAN_DAY / 2));
            dut1Direct = tcDirect.getDUT1(date.shiftedBy(i * Constants.JULIAN_DAY + Constants.JULIAN_DAY / 2));

            Assert.assertEquals(dut1Direct, dut1Cache, 1e-12);
        }

        for (int i = 0; i < 100; i++) {
            // compute the dut1 component for tidal correction
            double dut1Cache  = tcCache.getDUT1(date.shiftedBy(-i * Constants.JULIAN_DAY));
            double dut1Direct = tcDirect.getDUT1(date.shiftedBy(-i * Constants.JULIAN_DAY));

            Assert.assertEquals(dut1Direct, dut1Cache, 1e-12);

            // compute the dut1 component for tidal correction
            dut1Cache  = tcCache.getDUT1(date.shiftedBy(-i * Constants.JULIAN_DAY + Constants.JULIAN_DAY / 2));
            dut1Direct = tcDirect.getDUT1(date.shiftedBy(-i * Constants.JULIAN_DAY + Constants.JULIAN_DAY / 2));

            Assert.assertEquals(dut1Direct, dut1Cache, 1e-12);
        }
    }

    /**
     * Disables the internal TimeStampedCache for validation purposes.
     * For this purpose the newSlotInterval is set to a very low value (1 min), so that
     * every time new tidal correction data has to be created, a new slot is used.
     * @param tc the {@link TidalCorrection} object for which the cache shall be disabled
     */
    private void disableTimeStampedCache(final TidalCorrection tc) {
        try {
            Class<?> clazz = tc.getClass();
            Field field = clazz.getDeclaredField("cache");
            field.setAccessible(true);
            @SuppressWarnings("rawtypes")
            TimeStampedCache<?> cache = (TimeStampedCache) field.get(tc);
            clazz = cache.getClass();
            field = clazz.getDeclaredField("newSlotQuantumGap");
            field.setAccessible(true);
            field.set(cache, FastMath.round(60 / 1e-6));
        } catch (IllegalAccessException iae) {
            Assert.fail(iae.getMessage());
        } catch (NoSuchFieldException nfe) {
            Assert.fail(nfe.getMessage());
        }
    }

    /**
     * Check that {@link TidalCorrection#getDUT1(AbsoluteDate)} is thread safe.
     */
    @Test
    public void testConcurrentTidalCorrections() throws Exception {

        // set up
        final TidalCorrection tide = new TidalCorrection();
        final int threads = 10;
        final int timesPerThread = 100;

        // each thread uses a separate date, shifted 60 seconds apart
        final double[] expected = new double[threads];
        for (int i = 0; i < threads; i++) {
            expected[i] = tide.getDUT1(date.shiftedBy(i * 60));
        }

        // build jobs for concurrent execution
        final List<Callable<Boolean>> jobs = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < threads; i++) {
            final int job = i;
            jobs.add(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    for (int j = 0; j < timesPerThread; j++) {
                        final double actual = tide.getDUT1(date.shiftedBy(job * 60));
                        assertEquals(expected[job], actual, 0);
                    }
                    return true;
                }
            });
        }

        // action
        final List<Future<Boolean>> futures = Executors.newFixedThreadPool(threads).invokeAll(jobs);

        // verify - necessary to throw AssertionErrors from the Callable
        for (Future<Boolean> future : futures) {
            assertEquals(true, future.get());
        }
    }

    /**
     * Performance test, kept as a reference.
     */
    @Test
    @Ignore
    public void testSingleThreadPerformance() {

        final TidalCorrection tide = new TidalCorrection();

        // runtime stats
        final int n = 100000;
        long time = System.nanoTime();
        for (int i = 0; i < n; i++) {
            tide.getDUT1(date);
        }
        time = System.nanoTime() - time;
        System.out.format("same date took %f s%n", time * 1e-9);
        time = System.nanoTime();
        for (int i = 0; i < n; i++) {
            // date out side of cache range
            tide.getDUT1(date.shiftedBy(i * Constants.JULIAN_DAY)); //Constants.JULIAN_DAY));
        }
        time = System.nanoTime() - time;
        System.out.format("different date took %f s%n", time * 1e-9);
    }

    /**
     * Performance test, kept as a reference.
     */
    @Test
    @Ignore
    public void testConcurrentPerformance() throws Exception {

        // set up
        final TidalCorrection tide = new TidalCorrection();
        final int threads = 10;
        final int timesPerThread = 100000;

        // build jobs for concurrent execution
        final List<Callable<Long>> jobs = new ArrayList<Callable<Long>>();
        for (int i = 0; i < threads; i++) {
            final int shift = i * timesPerThread;
            jobs.add(new Callable<Long>() {
                public Long call() throws Exception {
                    final long time = System.nanoTime();
                    for (int j = 0; j < timesPerThread; j++) {
                        tide.getDUT1(date.shiftedBy(shift + j * Constants.JULIAN_DAY));
                    }
                    return System.nanoTime() - time;
                }
            });
        }

        // action
        final List<Future<Long>> futures = Executors.newFixedThreadPool(threads).invokeAll(jobs);

        // verify - necessary to throw AssertionErrors from the Callable
        long worst = 0;
        for (Future<Long> future : futures) {
            final long time = future.get();
            if (time > worst)
                worst = time;
        }
        System.out.format("tsc interp: worst time: %f s%n", worst * 1e-9);
    }

    @Before
    public void setUp() {
        date = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getTAI());
    }

    @After
    public void tearDown() {
        date = null;
    }

}
