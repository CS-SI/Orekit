/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.frames;

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CachedTransformProviderTest {

    private Frame         inertialFrame;
    private CountingFrame earth1;
    private CountingFrame earth2;

    @Test
    public void testSingleThread() {
        final CachedTransformProvider cachedTransformProvider =
                new CachedTransformProvider(earth1, inertialFrame,
                                            d -> earth1.getTransformTo(inertialFrame, d),
                                            20);
        Assertions.assertSame(earth1,        cachedTransformProvider.getOrigin());
        Assertions.assertSame(inertialFrame, cachedTransformProvider.getDestination());
        Assertions.assertEquals(20,  cachedTransformProvider.getCacheSize());
        final List<AbsoluteDate> dates = generateDates(new Well19937a(0x03fb4b0832dadcbe2L), 50, 5);
        for (final AbsoluteDate date : dates) {
            final Transform transform1   = cachedTransformProvider.getTransform(date);
            final Transform transform2   = earth2.getTransformTo(inertialFrame, date);
            final Transform backAndForth = new Transform(date, transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 3.0e-15);
        }
        Assertions.assertEquals(10, earth1.count);
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testMultiThread() throws InterruptedException, ExecutionException {
        final CachedTransformProvider cachedTransformProvider =
                new CachedTransformProvider(earth1, inertialFrame,
                                            d -> earth1.getTransformTo(inertialFrame, d),
                                            30);
        Assertions.assertEquals(30,  cachedTransformProvider.getCacheSize());
        final List<AbsoluteDate> dates = generateDates(new Well19937a(0x7d63ba984c6ae29eL), 300, 10);
        final List<Callable<Transform>> tasks = new ArrayList<>();
        for (final AbsoluteDate date : dates) {
            tasks.add(() -> cachedTransformProvider.getTransform(date));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        final List<Future<Transform>> futures = executorService.invokeAll(tasks);
        for (int i = 0; i < dates.size(); i++) {
            final Transform transform1   = futures.get(i).get();
            final Transform transform2   = earth2.getTransformTo(inertialFrame, dates.get(i));
            final Transform backAndForth = new Transform(dates.get(i), transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-14);
        }
        Assertions.assertTrue(earth1.count < 50, "this test may randomly fail due to multi-threading non-determinism");
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testExhaust() {
        final RandomGenerator random = new Well19937a(0x3b18a628c1a8b5e9L);
        final CachedTransformProvider cachedTransformProvider =
                new CachedTransformProvider(earth1, inertialFrame,
                                            d -> earth1.getTransformTo(inertialFrame, d),
                                            20);
        final List<AbsoluteDate> dates = generateDates(random,
                                                       10 * cachedTransformProvider.getCacheSize(),
                                                       50 * cachedTransformProvider.getCacheSize());

        // first batch, without exhausting
        final List<Transform> firstBatch = new ArrayList<>();
        for (int i = 0; i < cachedTransformProvider.getCacheSize(); i++) {
            firstBatch.add(cachedTransformProvider.getTransform(dates.get(i)));
        }
        for (int i = 0; i < 1000; i++) {
            // we should retrieve again and again the already computed instances
            final int k = random.nextInt(firstBatch.size());
            Assertions.assertSame(firstBatch.get(k), cachedTransformProvider.getTransform(dates.get(k)));
        }
        final Transform t14 = cachedTransformProvider.getTransform(dates.get(14));

        // now exhaust the instance, except we force entry 14 to remain in the cache by reusing it
        for (int i = 0; i < dates.size(); i++) {
            Assertions.assertNotNull(cachedTransformProvider.getTransform(dates.get(dates.size() - 1 - i)));
            Assertions.assertNotNull(cachedTransformProvider.getTransform(dates.get(14)));
        }

        for (int i = 0; i < 100; i++) {
            // we should get new instances from the first already known dates,
            // but they should correspond to similar transforms
            final int       k            = random.nextInt(firstBatch.size());
            final Transform t            = cachedTransformProvider.getTransform(dates.get(k));
            final Transform backAndForth = new Transform(dates.get(k), firstBatch.get(k), t.getInverse());
            if (k != 14) {
                Assertions.assertNotSame(firstBatch.get(k), t);
            }
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
            Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm(), 1.0e-20);
        }

        // entry 14 should still be in the cache
        Assertions.assertSame(t14, cachedTransformProvider.getTransform(dates.get(14)));

    }

    private List<AbsoluteDate> generateDates(final RandomGenerator random, final int total, final int history) {
        final List<AbsoluteDate> dates = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            final int index = i - random.nextInt(history);
            final AbsoluteDate date = index < 0 || index >= dates.size() ?
                                      AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Constants.JULIAN_DAY * random.nextDouble()) :
                                      dates.get(index);
            dates.add(date);
        }
        return dates;
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        this.inertialFrame     = FramesFactory.getEME2000();
        this.earth1            = new CountingFrame(earthFrame);
        this.earth2            = new CountingFrame(earthFrame);
    }

    @AfterEach
    public void tearDown() {
        inertialFrame = null;
        earth1        = null;
        earth2        = null;
    }

    /** Frame counting transform builds. */
    private static class CountingFrame extends Frame {
        int count;

        CountingFrame(final Frame frame) {
            super(frame, Transform.IDENTITY, "counting", false);
            count = 0;
        }

        public Transform getTransformTo(final Frame destination, final AbsoluteDate date) {
            ++count;
            return super.getTransformTo(destination, date);
        }

    }

}
