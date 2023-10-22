/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class GenericTimeStampedCacheTest {

    @Test
    public void testSingleCall() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(10, 3600.0, 13);
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assertions.assertEquals(1, checkDatesSingleThread(list, cache));
        Assertions.assertEquals(1, cache.getGetNeighborsCalls());
        Assertions.assertEquals(4, cache.getGenerateCalls());
        Assertions.assertEquals(0, cache.getSlotsEvictions());
        Assertions.assertEquals(10, cache.getMaxSlots());
        Assertions.assertEquals(Constants.JULIAN_DAY, cache.getNewSlotQuantumGap(), 1.0e-10);
        Assertions.assertEquals(Constants.JULIAN_YEAR, cache.getMaxSpan(), 1.0e-10);
    }

    @Test
    public void testPastInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.PAST_INFINITY,
                                                                 AbsoluteDate.J2000_EPOCH,
                                                                 10.0));
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        list.add(AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        list.add(AbsoluteDate.JULIAN_EPOCH);
        Assertions.assertEquals(3, checkDatesSingleThread(list, cache));
        Assertions.assertEquals(3, cache.getGetNeighborsCalls());
        try {
            cache.getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
            Assertions.fail("expected TimeStampedCacheException");
        } catch (TimeStampedCacheException tce) {
            // expected behavior
        } catch (Exception e) {
            Assertions.fail("wrong exception caught");
        }
    }

    @Test
    public void testFutureInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                                                                 AbsoluteDate.FUTURE_INFINITY, 10.0));
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assertions.assertEquals(2, checkDatesSingleThread(list, cache));
        Assertions.assertEquals(2, cache.getGetNeighborsCalls());
        try {
            cache.getNeighbors(AbsoluteDate.JULIAN_EPOCH);
            Assertions.fail("expected TimeStampedCacheException");
        } catch (TimeStampedCacheException tce) {
            // expected behavior
        } catch (Exception e) {
            Assertions.fail("wrong exception caught");
        }
    }

    @Test
    public void testInfinityRange() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache =
                new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                                                   new Generator(AbsoluteDate.PAST_INFINITY,
                                                                 AbsoluteDate.FUTURE_INFINITY,
                                                                 10.0));
        List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(+4.6e12));
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(-4.6e12));
        list.add(AbsoluteDate.JULIAN_EPOCH);
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assertions.assertEquals(5, checkDatesSingleThread(list, cache));
        Assertions.assertEquals(5, cache.getGetNeighborsCalls());
    }

    @Test
    public void testRegularCalls() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assertions.assertEquals(2000, testMultipleSingleThread(cache, new SequentialMode(), 2));
        Assertions.assertEquals(2000, cache.getGetNeighborsCalls());
        Assertions.assertEquals(56, cache.getGenerateCalls());
        Assertions.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(2, 3600, 13);
        Assertions.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assertions.assertEquals(2000, cache.getGetNeighborsCalls());
        Assertions.assertEquals(56, cache.getGenerateCalls());
        Assertions.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(1, 3600, 13);
        Assertions.assertEquals(2000, testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assertions.assertEquals(2000, cache.getGetNeighborsCalls());
        Assertions.assertEquals(8000, cache.getGenerateCalls());
        Assertions.assertEquals(1999, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(30, 3600, 13);
        Assertions.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assertions.assertEquals(5000, cache.getGetNeighborsCalls());
        Assertions.assertTrue(cache.getGenerateCalls() < 250);
        Assertions.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        Assertions.assertEquals(5000, testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assertions.assertEquals(5000, cache.getGetNeighborsCalls());
        Assertions.assertTrue(cache.getGenerateCalls()  > 400);
        Assertions.assertTrue(cache.getSlotsEvictions() > 300);
    }

    @Test
    public void testMultithreadedGoodConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(50, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 30);
        Assertions.assertEquals(n, cache.getGetNeighborsCalls());
        Assertions.assertTrue(cache.getGenerateCalls() < n / 20,
                "this test may fail randomly due to multi-threading non-determinism" +
                " (n = " + n + ", calls = " + cache.getGenerateCalls() +
                ", ratio = " + (n / cache.getGenerateCalls()) + ")");
        Assertions.assertTrue(cache.getSlotsEvictions() < n / 1000, 
                "this test may fail randomly due to multi-threading non-determinism" +
                " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
                (cache.getSlotsEvictions() == 0 ? "" : (", ratio = " + (n / cache.getSlotsEvictions()))) + ")");
    }

    @Test
    public void testMultithreadedBadConfiguration() throws TimeStampedCacheException {
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(3, 3600, 13);
        int n = testMultipleMultiThread(cache, new AlternateMode(), 50, 100);
        Assertions.assertEquals(n, cache.getGetNeighborsCalls());
        Assertions.assertTrue(cache.getGenerateCalls() > n / 15,
                "this test may fail randomly due to multi-threading non-determinism" +
                " (n = " + n + ", calls = " + cache.getGenerateCalls() +
                ", ratio = " + (n / cache.getGenerateCalls()) + ")");
        Assertions.assertTrue(cache.getSlotsEvictions() > n / 60, 
                "this test may fail randomly due to multi-threading non-determinism" +
                " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
                ", ratio = " + (n / cache.getSlotsEvictions()) + ")");
    }

    @Test
    public void testSmallShift() throws TimeStampedCacheException {
        double hour = 3600;
        GenericTimeStampedCache<AbsoluteDate> cache = createCache(10, hour, 13);
        Assertions.assertEquals(0, cache.getSlots());
        Assertions.assertEquals(0, cache.getEntries());
        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        cache.getNeighbors(start);
        Assertions.assertEquals(1, cache.getGetNeighborsCalls());
        Assertions.assertEquals(1, cache.getSlots());
        Assertions.assertEquals(18, cache.getEntries());
        Assertions.assertEquals(4, cache.getGenerateCalls());
        Assertions.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assertions.assertEquals( +6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(-3 * 3600));
        Assertions.assertEquals(2, cache.getGetNeighborsCalls());
        Assertions.assertEquals(1, cache.getSlots());
        Assertions.assertEquals(18, cache.getEntries());
        Assertions.assertEquals(4, cache.getGenerateCalls());
        Assertions.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assertions.assertEquals( +6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(7 * 3600));
        Assertions.assertEquals(3, cache.getGetNeighborsCalls());
        Assertions.assertEquals(1, cache.getSlots());
        Assertions.assertEquals(25, cache.getEntries());
        Assertions.assertEquals(5, cache.getGenerateCalls());
        Assertions.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assertions.assertEquals(+13 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
    }

    @Test
    public void testNotEnoughSlots() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCache(0, 3600.0, 13);
        });
    }

    @Test
    public void testNotEnoughNeighbors() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCache(10, 3600.0, 1);
        });
    }

    @Test
    public void testNoEarliestEntry() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            createCache(10, 3600.0, 3).getEarliest();
        });
    }

    @Test
    public void testNoLatestEntry() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            createCache(10, 3600.0, 3).getLatest();
        });
    }

    @Test
    public void testNoGeneratedData() throws TimeStampedCacheException {
        Assertions.assertThrows(TimeStampedCacheException.class, () -> {
            TimeStampedGenerator<AbsoluteDate> nullGenerator =
                    new TimeStampedGenerator<AbsoluteDate>() {
                        public List<AbsoluteDate> generate(AbsoluteDate existingDate,
                                AbsoluteDate date) {
                            return new ArrayList<AbsoluteDate>();
                        }
                    };
            new GenericTimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                    nullGenerator).getNeighbors(AbsoluteDate.J2000_EPOCH);
        });
    }

    @Test
    public void testNoDataBefore() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> nullGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
                    public List<AbsoluteDate> generate(AbsoluteDate existingDate,
                                                       AbsoluteDate date) {
                        return Collections.singletonList(AbsoluteDate.J2000_EPOCH);
                    }
                };
        AbsoluteDate central = AbsoluteDate.J2000_EPOCH.shiftedBy(-10);
        GenericTimeStampedCache<AbsoluteDate> cache = new GenericTimeStampedCache<>(
                2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY, nullGenerator);
        try {
            cache.getNeighbors(central);
            Assertions.fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            MatcherAssert.assertThat(e.getMessage(),
                    CoreMatchers.containsString(central.toString()));
        }
    }

    @Test
    public void testNoDataAfter() throws TimeStampedCacheException {
        TimeStampedGenerator<AbsoluteDate> nullGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {
            public List<AbsoluteDate> generate(AbsoluteDate existingDate,
                                               AbsoluteDate date) {
                return Collections.singletonList(AbsoluteDate.J2000_EPOCH);
            }
        };
        AbsoluteDate central = AbsoluteDate.J2000_EPOCH.shiftedBy(+10);
        GenericTimeStampedCache<AbsoluteDate> cache = new GenericTimeStampedCache<>(
                2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY, nullGenerator);
        try {
            cache.getNeighbors(central);
            Assertions.fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            MatcherAssert.assertThat(e.getMessage(),
                    CoreMatchers.containsString(central.toString()));
        }
    }

    @Test
    public void testUnsortedEntries() throws TimeStampedCacheException {
        Assertions.assertThrows(TimeStampedCacheException.class, () -> {
            TimeStampedGenerator<AbsoluteDate> reversedGenerator =
                    new TimeStampedGenerator<AbsoluteDate>() {
                        /** {@inheritDoc} */
                        public List<AbsoluteDate> generate(AbsoluteDate existingDate, AbsoluteDate date) {
                            List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                            list.add(date);
                            list.add(date.shiftedBy(-10.0));
                            return list;
                        }
                    };

            new GenericTimeStampedCache<AbsoluteDate>(3, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                    reversedGenerator).getNeighbors(AbsoluteDate.J2000_EPOCH);
        });
    }

    @Test
    public void testDuplicatingGenerator() throws TimeStampedCacheException {

        final double step = 3600.0;

        TimeStampedGenerator<AbsoluteDate> duplicatingGenerator =
                new TimeStampedGenerator<AbsoluteDate>() {

            /** {@inheritDoc} */
            public List<AbsoluteDate> generate(AbsoluteDate existingDate, AbsoluteDate date) {
                List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                if (existingDate == null) {
                    list.add(date);
                } else {
                    if (date.compareTo(existingDate) > 0) {
                        AbsoluteDate t = existingDate.shiftedBy(-10 * step);
                        do {
                            t = t.shiftedBy(step);
                            list.add(list.size(), t);
                        } while (t.compareTo(date) <= 0);
                    } else {
                        AbsoluteDate t = existingDate.shiftedBy(10 * step);
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
                                                   duplicatingGenerator);

        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        final List<AbsoluteDate> firstSet = cache.getNeighbors(start).collect(Collectors.toList());
        Assertions.assertEquals(5, firstSet.size());
        Assertions.assertEquals(4, cache.getGenerateCalls());
        Assertions.assertEquals(8, cache.getEntries());
        for (int i = 1; i < firstSet.size(); ++i) {
            Assertions.assertEquals(step, firstSet.get(i).durationFrom(firstSet.get(i - 1)), 1.0e-10);
        }

        final List<AbsoluteDate> secondSet = cache.getNeighbors(cache.getLatest().shiftedBy(10 * step)).collect(Collectors.toList());
        Assertions.assertEquals(5, secondSet.size());
        Assertions.assertEquals(7, cache.getGenerateCalls());
        Assertions.assertEquals(20, cache.getEntries());
        for (int i = 1; i < secondSet.size(); ++i) {
            Assertions.assertEquals(step, firstSet.get(i).durationFrom(firstSet.get(i - 1)), 1.0e-10);
        }

    }

    @Test
    @DisplayName("Test that the cache of the detector does not get corrupted when a propagation to infinity has been run")
    void testIssue1108() {
        // GIVEN
        final TLE aTle = new TLE("1 27424U 02022A   23173.43403823  .00001056  00000+0  23935-3 0  9994",
                           "2 27424  98.2874 117.8299 0001810 103.6635   5.7337 14.58117998124128");

        final Frame            itrf      = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final GeodeticPoint    site      = new GeodeticPoint(0.0, 0.0, 0.0);
        final TopocentricFrame siteFrame = new TopocentricFrame(ReferenceEllipsoid.getIers2010(itrf), site, "site");
        final ElevationDetector siteVisDetector =
              new ElevationDetector(60, 0.001, siteFrame).withConstantElevation(5.0);

        // Create TLE propagator
        final TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(aTle);
        tlePropagator.addEventDetector(siteVisDetector);

        try {
            // WHEN
            // Propagation from and to infinity throws an exception
            tlePropagator.propagate(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        } catch (OrekitIllegalArgumentException e) {
            // THEN
            TimeScale          utc                = TimeScalesFactory.getUTC();
            final AbsoluteDate ephemerisStartDate = new AbsoluteDate(23, 6, 15, 0, 0, 0, utc);
            final AbsoluteDate ephemerisEndDate   = new AbsoluteDate(23, 6, 15, 0, 0, 1, utc);

            // New propagation should not throw an ArithmeticException
            Assertions.assertDoesNotThrow(() -> tlePropagator.propagate(ephemerisStartDate, ephemerisEndDate));
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
                                                  Constants.JULIAN_DAY, generator);
    }

    private int checkDatesSingleThread(final List<AbsoluteDate> centralDates,
                                       final GenericTimeStampedCache<AbsoluteDate> cache)
        throws TimeStampedCacheException {

        final int n = cache.getMaxNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();

        for (final AbsoluteDate central : centralDates) {
            final List<AbsoluteDate> neighbors = cache.getNeighbors(central).collect(Collectors.toList());
            Assertions.assertEquals(n, neighbors.size());
            for (final AbsoluteDate date : neighbors) {
                Assertions.assertTrue(date.durationFrom(central) >= -(n + 1) * step);
                Assertions.assertTrue(date.durationFrom(central) <= n * step);
            }
        }

        return centralDates.size();

    }

    private int checkDatesMultiThread(final List<AbsoluteDate> centralDates,
                                      final GenericTimeStampedCache<AbsoluteDate> cache,
                                      final int threadPoolSize)
        throws TimeStampedCacheException {

        final int n = cache.getMaxNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AtomicReference<AbsoluteDate[]> failedDates = new AtomicReference<AbsoluteDate[]>();
        final AtomicReference<TimeStampedCacheException> caught = new AtomicReference<TimeStampedCacheException>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (final AbsoluteDate central : centralDates) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        final List<AbsoluteDate> neighbors = cache.getNeighbors(central).collect(Collectors.toList());
                        Assertions.assertEquals(n, neighbors.size());
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
            Assertions.assertTrue(executorService.awaitTermination(10, TimeUnit.MINUTES), 
                    "Not enough time for all threads to complete, try increasing the timeout");
        } catch (InterruptedException ie) {
            Assertions.fail(ie.getLocalizedMessage());
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
            Assertions.fail(builder.toString());
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

        public List<AbsoluteDate> generate(AbsoluteDate existingDate, AbsoluteDate date) {
            List<AbsoluteDate> dates = new ArrayList<AbsoluteDate>();
            if (existingDate == null) {
                dates.add(date);
            } else if (date.compareTo(existingDate) >= 0) {
                AbsoluteDate previous = existingDate;
                while (date.compareTo(previous) > 0) {
                    previous = previous.shiftedBy(step);
                    if (previous.compareTo(earliest) >= 0 && previous.compareTo(latest) <= 0) {
                        dates.add(dates.size(), previous);
                    }
                }
            } else {
                AbsoluteDate previous = existingDate;
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

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
