/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well1024a;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


public class TimeStampedCacheTest {

    @Test
    public void testSingleCall() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(10, 3600.0, 13);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(1, checkDatesSingleThread(list, cache));
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testRegularCalls() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new SequentialMode(), 2));
        Assert.assertEquals(44, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsGoodConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(44, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsBadConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(1, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(8000, cache.getGenerateCalls());
        Assert.assertEquals(1999, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsGoodConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(30, 3600, 13);
        Assert.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertTrue(cache.getGenerateCalls() < 250);
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsBadConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        Assert.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertTrue(cache.getGenerateCalls()  > 400);
        Assert.assertTrue(cache.getSlotsEvictions() > 300);
    }

    @Test
    public void testMultithreadedGoodConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(50, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 30);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", calls = " + cache.getGenerateCalls() +
                          ", ratio = " + (n / cache.getGenerateCalls()) + ")",
                          cache.getGenerateCalls() < n / 40);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
                          (cache.getSlotsEvictions() == 0 ? "" : (", ratio = " + (n / cache.getSlotsEvictions()))) + ")",
                          cache.getSlotsEvictions() < n / 1000);
    }

    @Test
    public void testMultithreadedBadConfiguration() throws OrekitException {
        TimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 100);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", calls = " + cache.getGenerateCalls() +
                          ", ratio = " + (n / cache.getGenerateCalls()) + ")",
                          cache.getGenerateCalls() > n / 15);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
                          ", ratio = " + (n / cache.getSlotsEvictions()) + ")",
                          cache.getSlotsEvictions() > n / 60);
    }

    @Test
    public void testSmallShift() throws OrekitException {
        double hour = 3600;
        TimeStampedCache<AbsoluteDate> cache = createCache(10, hour, 13);
        Assert.assertEquals(0, cache.getSlots());
        Assert.assertEquals(0, cache.getEntries());
        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        cache.getNeighbors(start);
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(20, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals( -1 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+12 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(6 * 3600));
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(20, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals( -1 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+12 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(7 * 3600));
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(21, cache.getEntries());
        Assert.assertEquals(5, cache.getGenerateCalls());
        Assert.assertEquals( -1 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+13 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
    }

    @Test(expected=OrekitException.class)
    public void testNotEnoughSlots() throws OrekitException {
        createCache(0, 3600.0, 13);
    }

    @Test(expected=OrekitException.class)
    public void testNotEnoughNeighbors() throws OrekitException {
        createCache(10, 3600.0, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void testNoEarliestEntry() throws OrekitException {
        createCache(10, 3600.0, 3).getEarliest();
    }

    @Test(expected=IllegalStateException.class)
    public void testNoLatestEntry() throws OrekitException {
        createCache(10, 3600.0, 3).getLatest();
    }

    private int testMultipleSingleThread(TimeStampedCache<AbsoluteDate> cache, Mode mode, int slots)
        throws OrekitException {
        double step = ((Generator) cache.getGenerator()).getStep();
        AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return checkDatesSingleThread(mode.generateDates(base, 25 * step, 0.025 * step), cache);
    }

    private int testMultipleMultiThread(TimeStampedCache<AbsoluteDate> cache, Mode mode,
                                        int slots, int threadPoolSize)
        throws OrekitException {
        double step = ((Generator) cache.getGenerator()).getStep();
        AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return checkDatesMultiThread(mode.generateDates(base, 25 * step, 0.025 * step), cache, threadPoolSize);
    }

    private TimeStampedCache<AbsoluteDate> createCache(int maxSlots, double step, int neighborsSize)
        throws OrekitException {
        Generator generator =
                new Generator(AbsoluteDate.J2000_EPOCH.shiftedBy(-Constants.JULIAN_CENTURY),
                              AbsoluteDate.J2000_EPOCH.shiftedBy(+Constants.JULIAN_CENTURY),
                              step);
        return new TimeStampedCache<AbsoluteDate>(maxSlots, Constants.JULIAN_YEAR, AbsoluteDate.class,
                                                  generator, neighborsSize);
    }

    private int checkDatesSingleThread(final List<AbsoluteDate> centralDates,
                                       final TimeStampedCache<AbsoluteDate> cache)
        throws OrekitException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();

        for (final AbsoluteDate central : centralDates) {
            final AbsoluteDate[] neighbors = cache.getNeighbors(central);
            Assert.assertEquals(n, neighbors.length);
            for (final AbsoluteDate date : neighbors) {
                Assert.assertTrue(date.durationFrom(central) >= -(n + 1) * step);
                Assert.assertTrue(date.durationFrom(central) <= n * step);
            }
        }

        return centralDates.size();

    }

    private int checkDatesMultiThread(final List<AbsoluteDate> centralDates,
                                      final TimeStampedCache<AbsoluteDate> cache,
                                      final int threadPoolSize)
        throws OrekitException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AtomicReference<AbsoluteDate[]> failedDates = new AtomicReference<AbsoluteDate[]>();
        final AtomicReference<OrekitException> caught    = new AtomicReference<OrekitException>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (final AbsoluteDate central : centralDates) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        final AbsoluteDate[] neighbors = cache.getNeighbors(central);
                        Assert.assertEquals(n, neighbors.length);
                        for (final AbsoluteDate date : neighbors) {
                            if (date.durationFrom(central) < -(n + 1) * step ||
                                date.durationFrom(central) > n * step) {
                                failedDates.set(new AbsoluteDate[] { date, central });
                            }
                        }
                    } catch (OrekitException oe) {
                        caught.set(oe);
                    }
                }
            });
        }

        try {
            executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Assert.fail(ie.getLocalizedMessage());
        }

        if (caught.get() != null) {
            throw caught.get();
        }

        if (failedDates.get() != null) {
            AbsoluteDate[] dates = failedDates.get();
            Assert.fail(dates[0] + " <-> " + dates[1]);
        }

        return centralDates.size();

    }

    private static class Generator implements TimeStampedGenerator<AbsoluteDate> {

        private final AbsoluteDate earliest;
        private final AbsoluteDate latest;
        private final double step;

        public Generator(final AbsoluteDate earliest, final AbsoluteDate latest, final double step) {
            this.earliest = earliest;
            this.latest   = latest;
            this.step     = step;
        }

        public double getStep() {
            return step;
        }

        public AbsoluteDate getEarliest() {
            return earliest;
        }

        public AbsoluteDate getLatest() {
            return latest;
        }

        public List<AbsoluteDate> generate(AbsoluteDate existing, AbsoluteDate date) {
            List<AbsoluteDate> dates = new ArrayList<AbsoluteDate>();
            if (existing == null) {
                dates.add(date);
            } else if (date.compareTo(existing) >= 0) {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) > 0) {
                    previous = previous.shiftedBy(step);
                    dates.add(previous);
                }
            } else {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) < 0) {
                    previous = previous.shiftedBy(-step);
                    dates.add(previous);
                }
            }
            return dates;
        }

    }

    private interface Mode {
        public List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step);
     }

    private class SequentialMode implements Mode {

        public List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step) {
            List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (final AbsoluteDate initial : base) {
                for (double dt = 0; dt < duration; dt += step) {
                    list.add(initial.shiftedBy(dt));
                }
            }
            return list;
        }

    }

    private class AlternateMode implements Mode {

        public List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step) {
            List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (double dt = 0; dt < duration; dt += step) {
                for (final AbsoluteDate initial : base) {
                    list.add(initial.shiftedBy(dt));
                }
            }
            return list;
        }

    }

    private class RandomMode implements Mode {

        private RandomGenerator random;

        public RandomMode(long seed) {
            random = new Well1024a(seed);
        }

        public List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step) {
            List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (int i = 0; i < base.length * duration / step; ++i) {
                int j     = random.nextInt(base.length);
                double dt = random.nextDouble() * duration;
                    list.add(base[j].shiftedBy(dt));
            }
            return list;
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
