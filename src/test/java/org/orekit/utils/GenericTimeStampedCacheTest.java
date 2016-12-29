/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;


public class GenericTimeStampedCacheTest {

    @Test
    public void testSingleCall() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(10, 3600.0, 13);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(1, checkDatesSingleThread(list, cache));
        Assert.assertEquals(1, cache.getGetNeighborsCalls());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
        Assert.assertEquals(10, cache.getMaxSlots());
        Assert.assertEquals(Constants.JULIAN_DAY, cache.getNewSlotQuantumGap(), 1.0e-10);
        Assert.assertEquals(Constants.JULIAN_YEAR, cache.getMaxSpan(), 1.0e-10);
    }

    @Test
    public void testPastInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.PAST_INFINITY,
                                                                 AbsoluteDate.J2000_EPOCH,
                                                                 10.0), AbsoluteDate.class);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        list.add(AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        list.add(AbsoluteDate.JULIAN_EPOCH);
        Assert.assertEquals(3, checkDatesSingleThread(list, cache));
        Assert.assertEquals(3, cache.getGetNeighborsCalls());
        try {
            cache.getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
            Assert.fail("expected TimeStampedCacheException");
        } catch (TimeStampedCacheException tce) {
            // expected behavior
        } catch (Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    @Test
    public void testFutureInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                                                                 AbsoluteDate.FUTURE_INFINITY, 10.0),
                                                   AbsoluteDate.class);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(2, checkDatesSingleThread(list, cache));
        Assert.assertEquals(2, cache.getGetNeighborsCalls());
        try {
            cache.getNeighbors(AbsoluteDate.JULIAN_EPOCH);
            Assert.fail("expected TimeStampedCacheException");
        } catch (TimeStampedCacheException tce) {
            // expected behavior
        } catch (Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    @Test
    public void testInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.PAST_INFINITY,
                                                                 AbsoluteDate.FUTURE_INFINITY,
                                                                 10.0), AbsoluteDate.class);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(+4.6e12));
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(-4.6e12));
        list.add(AbsoluteDate.JULIAN_EPOCH);
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(5, checkDatesSingleThread(list, cache));
        Assert.assertEquals(5, cache.getGetNeighborsCalls());
    }

    @Test
    public void testRegularCalls() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new SequentialMode(), 2));
        Assert.assertEquals(2000, cache.getGetNeighborsCalls());
        Assert.assertEquals(56, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(2000, cache.getGetNeighborsCalls());
        Assert.assertEquals(56, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(1, 3600, 13);
        Assert.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(2000, cache.getGetNeighborsCalls());
        Assert.assertEquals(8000, cache.getGenerateCalls());
        Assert.assertEquals(1999, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(30, 3600, 13);
        Assert.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertEquals(5000, cache.getGetNeighborsCalls());
        Assert.assertTrue(cache.getGenerateCalls() < 250);
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        Assert.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertEquals(5000, cache.getGetNeighborsCalls());
        Assert.assertTrue(cache.getGenerateCalls()  > 400);
        Assert.assertTrue(cache.getSlotsEvictions() > 300);
    }

    @Test
    public void testMultithreadedGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(50, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 30);
        Assert.assertEquals(n, cache.getGetNeighborsCalls());
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", calls = " + cache.getGenerateCalls() +
                          ", ratio = " + (n / cache.getGenerateCalls()) + ")",
                          cache.getGenerateCalls() < n / 20);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
                          " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
                          (cache.getSlotsEvictions() == 0 ? "" : (", ratio = " + (n / cache.getSlotsEvictions()))) + ")",
                          cache.getSlotsEvictions() < n / 1000);
    }

    @Test
    public void testMultithreadedBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 100);
        Assert.assertEquals(n, cache.getGetNeighborsCalls());
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
    public void testSmallShift() throws TimeStampedCacheException {
        double hour = 3600;
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(10, hour, 13);
        Assert.assertEquals(0, cache.getSlots());
        Assert.assertEquals(0, cache.getEntries());
        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        cache.getNeighbors(start);
        Assert.assertEquals(1, cache.getGetNeighborsCalls());
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(18, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals( +6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(-3 * 3600));
        Assert.assertEquals(2, cache.getGetNeighborsCalls());
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(18, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals( +6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(7 * 3600));
        Assert.assertEquals(3, cache.getGetNeighborsCalls());
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(25, cache.getEntries());
        Assert.assertEquals(5, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+13 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNotEnoughSlots() {
        createCache(0, 3600.0, 13);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNotEnoughNeighbors() {
        createCache(10, 3600.0, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void testNoEarliestEntry() {
        createCache(10, 3600.0, 3).getEarliest();
    }

    @Test(expected=IllegalStateException.class)
    public void testNoLatestEntry() {
        createCache(10, 3600.0, 3).getLatest();
    }

    @Test(expected=TimeStampedCacheException.class)
    public void testNoGeneratedData() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> nullGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
            public List<AbsoluteDate> generate(AbsoluteDate existing,
                                               AbsoluteDate date) {
                return new ArrayList<AbsoluteDate>();
            }
        };
        new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                           nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH);
    }

    @Test(expected=TimeStampedCacheException.class)
    public void testNoDataBefore() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> nullGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
            public List<AbsoluteDate> generate(AbsoluteDate existing,
                                               AbsoluteDate date) {
                return Arrays.asList(AbsoluteDate.J2000_EPOCH);
            }
        };
        new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                           nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(-10));
    }

    @Test(expected=TimeStampedCacheException.class)
    public void testNoDataAfter() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> nullGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
            public List<AbsoluteDate> generate(AbsoluteDate existing,
                                               AbsoluteDate date) {
                return Arrays.asList(AbsoluteDate.J2000_EPOCH);
            }
        };
        new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                           nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(+10));
    }

    @Test(expected=TimeStampedCacheException.class)
    public void testUnsortedEntries() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> reversedGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
            /** {@inheritDoc} */
            public List<AbsoluteDate> generate(AbsoluteDate existing, AbsoluteDate date) {
                List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                list.add(date);
                list.add(date.shiftedBy(-10.0));
                return list;
            }
        };

        new GenericTimeStampedCache<AbsoluteDate>(3, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                           reversedGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH);

    }

    @Test
    public void testDuplicatingGenerator() throws TimeStampedCacheException {

        final double step = 3600.0;

        TimeStampedGenerator<AbsoluteDate> duplicatingGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {

            /** {@inheritDoc} */
            public List<AbsoluteDate> generate(AbsoluteDate existing, AbsoluteDate date) {
                List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                if (existing == null) {
                    list.add(date);
                } else {
                    if (date.compareTo(existing) > 0) {
                        AbsoluteDate t = existing.shiftedBy(-10 * step);
                        do {
                            t = t.shiftedBy(step);
                            list.add(list.size(), t);
                        } while (t.compareTo(date) <= 0);
                    } else {
                        AbsoluteDate t = existing.shiftedBy(10 * step);
                        do {
                            t = t.shiftedBy(-step);
                            list.add(0, t);
                        } while (t.compareTo(date) >= 0);
                    }
                }
                return list;
            }

        };

        final GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(5, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   duplicatingGenerator, AbsoluteDate.class);

        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        final AbsoluteDate[] firstSet = cache.getNeighbors(start).toArray(new AbsoluteDate[0]);
        Assert.assertEquals(5, firstSet.length);
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(8, cache.getEntries());
        for (int i = 1; i < firstSet.length; ++i) {
            Assert.assertEquals(step, firstSet[i].durationFrom(firstSet[i - 1]), 1.0e-10);
        }

        final AbsoluteDate[] secondSet = cache.getNeighbors(cache.getLatest().shiftedBy(10 * step)).toArray(new AbsoluteDate[0]);
        Assert.assertEquals(5, secondSet.length);
        Assert.assertEquals(7, cache.getGenerateCalls());
        Assert.assertEquals(20, cache.getEntries());
        for (int i = 1; i < secondSet.length; ++i) {
            Assert.assertEquals(step, firstSet[i].durationFrom(firstSet[i - 1]), 1.0e-10);
        }

    }

    private int testMultipleSingleThread(GenericTimeStampedCache<AbsoluteDate> cache, Mode mode, int slots)
        throws TimeStampedCacheException {
        double step = ((Generator) cache.getGenerator()).getStep();
        AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return checkDatesSingleThread(mode.generateDates(base, 25 * step, 0.025 * step), cache);
    }

    private int testMultipleMultiThread(GenericTimeStampedCache<AbsoluteDate> cache, Mode mode,
                                        int slots, int threadPoolSize)
        throws TimeStampedCacheException {
        double step = ((Generator) cache.getGenerator()).getStep();
        AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return checkDatesMultiThread(mode.generateDates(base, 25 * step, 0.025 * step), cache, threadPoolSize);
    }

    private GenericTimeStampedCache<AbsoluteDate> createCache(int maxSlots, double step, int neighborsSize) {
        Generator generator =
                new Generator(AbsoluteDate.J2000_EPOCH.shiftedBy(-Constants.JULIAN_CENTURY),
                              AbsoluteDate.J2000_EPOCH.shiftedBy(+Constants.JULIAN_CENTURY),
                              step);
        return new GenericTimeStampedCache<AbsoluteDate>(neighborsSize, maxSlots, Constants.JULIAN_YEAR,
                                                  Constants.JULIAN_DAY, generator, AbsoluteDate.class);
    }

    private int checkDatesSingleThread(final List<AbsoluteDate> centralDates,
                                       final GenericTimeStampedCache<AbsoluteDate> cache)
        throws TimeStampedCacheException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();

        for (final AbsoluteDate central : centralDates) {
            final List<AbsoluteDate> neighbors = cache.getNeighbors(central);
            Assert.assertEquals(n, neighbors.size());
            for (final AbsoluteDate date : neighbors) {
                Assert.assertTrue(date.durationFrom(central) >= -(n + 1) * step);
                Assert.assertTrue(date.durationFrom(central) <= n * step);
            }
        }

        return centralDates.size();

    }

    private int checkDatesMultiThread(final List<AbsoluteDate> centralDates,
                                      final GenericTimeStampedCache<AbsoluteDate> cache,
                                      final int threadPoolSize)
        throws TimeStampedCacheException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AtomicReference<AbsoluteDate[]> failedDates = new AtomicReference<AbsoluteDate[]>();
        final AtomicReference<TimeStampedCacheException> caught = new AtomicReference<TimeStampedCacheException>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (final AbsoluteDate central : centralDates) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        final List<AbsoluteDate> neighbors = cache.getNeighbors(central);
                        Assert.assertEquals(n, neighbors.size());
                        for (final AbsoluteDate date : neighbors) {
                            if (date.durationFrom(central) < -(n + 1) * step ||
                                date.durationFrom(central) > n * step) {
                                AbsoluteDate[] dates = new AbsoluteDate[n + 1];
                                dates[0] = central;
                                System.arraycopy(neighbors, 0, dates, 1, n);
                                failedDates.set(dates);
                            }
                        }
                    } catch (TimeStampedCacheException tce) {
                        caught.set(tce);
                    }
                }
            });
        }

        try {
            executorService.shutdown();
            Assert.assertTrue(
                    "Not enough time for all threads to complete, try increasing the timeout",
                    executorService.awaitTermination(10, TimeUnit.MINUTES));
        } catch (InterruptedException ie) {
            Assert.fail(ie.getLocalizedMessage());
        }

        if (caught.get() != null) {
            throw caught.get();
        }

        if (failedDates.get() != null) {
            AbsoluteDate[] dates = failedDates.get();
            StringBuilder builder = new StringBuilder();
            String eol = System.getProperty("line.separator");
            builder.append("central = ").append(dates[0]).append(eol);
            builder.append("step = ").append(step).append(eol);
            builder.append("neighbors =").append(eol);
            for (int i = 1; i < dates.length; ++i) {
                builder.append("    ").append(dates[i]).append(eol);
            }
            Assert.fail(builder.toString());
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

        public List<AbsoluteDate> generate(AbsoluteDate existing, AbsoluteDate date) {
            List<AbsoluteDate> dates = new ArrayList<AbsoluteDate>();
            if (existing == null) {
                dates.add(date);
            } else if (date.compareTo(existing) >= 0) {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) > 0) {
                    previous = previous.shiftedBy(step);
                    if (previous.compareTo(earliest) >= 0 && previous.compareTo(latest) <= 0) {
                        dates.add(dates.size(), previous);
                    }
                }
            } else {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) < 0) {
                    previous = previous.shiftedBy(-step);
                    if (previous.compareTo(earliest) >= 0 && previous.compareTo(latest) <= 0) {
                        dates.add(0, previous);
                    }
                }
            }
            return dates;
        }

    }

    private interface Mode {
        List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step);
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
