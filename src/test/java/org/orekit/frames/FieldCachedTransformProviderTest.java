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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public class FieldCachedTransformProviderTest {

    private Frame         inertialFrame;
    private CountingFrame earth1;
    private CountingFrame earth2;

    @Test
    public void testSingleThread() {
        doTestSingleThread(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSingleThread(final Field<T> field) {
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        Assertions.assertSame(earth1,        cachedTransformProvider.getOrigin());
        Assertions.assertSame(inertialFrame, cachedTransformProvider.getDestination());
        Assertions.assertEquals(20,          cachedTransformProvider.getCacheSize());
        final List<FieldAbsoluteDate<T>> dates = generateDates(field, new Well19937a(0x03fb4b0832dadcbe2L), 50, 5);
        for (final FieldAbsoluteDate<T> date : dates) {
            final FieldTransform<T> transform1   = cachedTransformProvider.getTransform(date);
            final FieldTransform<T> transform2   = earth2.getTransformTo(inertialFrame, date);
            final FieldTransform<T> backAndForth = new FieldTransform<>(date, transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 3.0e-15);
        }
        Assertions.assertEquals(10, earth1.count);
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testSingleThreadKinematic() {
        doTestSingleThreadKinematic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSingleThreadKinematic(final Field<T> field) {
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        Assertions.assertSame(earth1,        cachedTransformProvider.getOrigin());
        Assertions.assertSame(inertialFrame, cachedTransformProvider.getDestination());
        Assertions.assertEquals(20,          cachedTransformProvider.getCacheSize());
        final List<FieldAbsoluteDate<T>> dates = generateDates(field, new Well19937a(0x03fb4b0832dadcbe2L), 50, 5);
        for (final FieldAbsoluteDate<T> date : dates) {
            final FieldKinematicTransform<T> transform1   = cachedTransformProvider.getKinematicTransform(date);
            final FieldKinematicTransform<T> transform2   = earth2.getKinematicTransformTo(inertialFrame, date);
            final FieldKinematicTransform<T> backAndForth = FieldKinematicTransform.compose(date, transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 3.0e-15);
        }
        Assertions.assertEquals(10, earth1.count);
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testSingleThreadStatic() {
        doTestSingleThreadStatic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSingleThreadStatic(final Field<T> field) {
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        Assertions.assertSame(earth1,        cachedTransformProvider.getOrigin());
        Assertions.assertSame(inertialFrame, cachedTransformProvider.getDestination());
        Assertions.assertEquals(20,          cachedTransformProvider.getCacheSize());
        final List<FieldAbsoluteDate<T>> dates = generateDates(field, new Well19937a(0x03fb4b0832dadcbe2L), 50, 5);
        for (final FieldAbsoluteDate<T> date : dates) {
            final FieldStaticTransform<T> transform1   = cachedTransformProvider.getStaticTransform(date);
            final FieldStaticTransform<T> transform2   = earth2.getStaticTransformTo(inertialFrame, date);
            final FieldStaticTransform<T> backAndForth = FieldStaticTransform.compose(date, transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 3.0e-15);
        }
        Assertions.assertEquals(10, earth1.count);
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testMultiThread() throws InterruptedException, ExecutionException {
        doTestMultiThread(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMultiThread(final Field<T> field)
        throws InterruptedException, ExecutionException {
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(30);
        Assertions.assertEquals(30, cachedTransformProvider.getCacheSize());
        final List<FieldAbsoluteDate<T>> dates = generateDates(field, new Well19937a(0x7d63ba984c6ae29eL), 300, 10);
        final List<Callable<FieldTransform<T>>> tasks = new ArrayList<>();
        for (final FieldAbsoluteDate<T> date : dates) {
            tasks.add(() -> cachedTransformProvider.getTransform(date));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        final List<Future<FieldTransform<T>>> futures = executorService.invokeAll(tasks);
        for (int i = 0; i < dates.size(); i++) {
            final FieldTransform<T> transform1   = futures.get(i).get();
            final FieldTransform<T> transform2   = earth2.getTransformTo(inertialFrame, dates.get(i));
            final FieldTransform<T> backAndForth = new FieldTransform<>(dates.get(i), transform1, transform2.getInverse());
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-14);
        }
        Assertions.assertTrue(earth1.count < 50, "this test may randomly fail due to multi-threading non-determinism");
        Assertions.assertEquals(dates.size(), earth2.count);
    }

    @Test
    public void testExhaust() {
        doTestExhaust(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestExhaust(final Field<T> field) {
        final RandomGenerator random = new Well19937a(0x3b18a628c1a8b5e9L);
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        final List<FieldAbsoluteDate<T>> dates = generateDates(field,
                                                               random,
                                                               10 * cachedTransformProvider.getCacheSize(),
                                                               50 * cachedTransformProvider.getCacheSize());

        // first batch, without exhausting
        final List<FieldTransform<T>> firstBatch = new ArrayList<>();
        for (int i = 0; i < cachedTransformProvider.getCacheSize(); i++) {
            firstBatch.add(cachedTransformProvider.getTransform(dates.get(i)));
        }
        for (int i = 0; i < 1000; i++) {
            // we should retrieve again and again the already computed instances
            final int k = random.nextInt(firstBatch.size());
            Assertions.assertSame(firstBatch.get(k), cachedTransformProvider.getTransform(dates.get(k)));
        }
        final FieldTransform<T> t14 = cachedTransformProvider.getTransform(dates.get(14));

        // now exhaust the instance, except we force entry 14 to remain in the cache by reusing it
        for (int i = 0; i < dates.size(); i++) {
            Assertions.assertNotNull(cachedTransformProvider.getTransform(dates.get(dates.size() - 1 - i)));
            Assertions.assertNotNull(cachedTransformProvider.getTransform(dates.get(14)));
        }

        for (int i = 0; i < 100; i++) {
            // we should get new instances from the first already known dates,
            // but they should correspond to similar transforms
            final int               k            = random.nextInt(firstBatch.size());
            final FieldTransform<T> t            = cachedTransformProvider.getTransform(dates.get(k));
            final FieldTransform<T> backAndForth = new FieldTransform<>(dates.get(k), firstBatch.get(k), t.getInverse());
            if (k != 14) {
                Assertions.assertNotSame(firstBatch.get(k), t);
            }
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
            Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm().getReal(), 1.0e-20);
        }

        // entry 14 should still be in the cache
        Assertions.assertSame(t14, cachedTransformProvider.getTransform(dates.get(14)));

    }

    @Test
    public void testExhaustKinematic() {
        doTestExhaustKinematic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestExhaustKinematic(final Field<T> field) {
        final RandomGenerator random = new Well19937a(0x3b18a628c1a8b5e9L);
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        final List<FieldAbsoluteDate<T>> dates = generateDates(field,
                                                               random,
                                                               10 * cachedTransformProvider.getCacheSize(),
                                                               50 * cachedTransformProvider.getCacheSize());

        // first batch, without exhausting
        final List<FieldKinematicTransform<T>> firstBatch = new ArrayList<>();
        for (int i = 0; i < cachedTransformProvider.getCacheSize(); i++) {
            firstBatch.add(cachedTransformProvider.getKinematicTransform(dates.get(i)));
        }
        for (int i = 0; i < 1000; i++) {
            // we should retrieve again and again the already computed instances
            final int k = random.nextInt(firstBatch.size());
            Assertions.assertSame(firstBatch.get(k), cachedTransformProvider.getKinematicTransform(dates.get(k)));
        }
        final FieldKinematicTransform<T> t14 = cachedTransformProvider.getKinematicTransform(dates.get(14));

        // now exhaust the instance, except we force entry 14 to remain in the cache by reusing it
        for (int i = 0; i < dates.size(); i++) {
            Assertions.assertNotNull(cachedTransformProvider.getKinematicTransform(dates.get(dates.size() - 1 - i)));
            Assertions.assertNotNull(cachedTransformProvider.getKinematicTransform(dates.get(14)));
        }

        for (int i = 0; i < 100; i++) {
            // we should get new instances from the first already known dates,
            // but they should correspond to similar transforms
            final int                        k            = random.nextInt(firstBatch.size());
            final FieldKinematicTransform<T> t            = cachedTransformProvider.getKinematicTransform(dates.get(k));
            final FieldKinematicTransform<T> backAndForth = FieldKinematicTransform.compose(dates.get(k), firstBatch.get(k), t.getInverse());
            if (k != 14) {
                Assertions.assertNotSame(firstBatch.get(k), t);
            }
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
            Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);
        }

        // entry 14 should still be in the cache
        Assertions.assertSame(t14, cachedTransformProvider.getKinematicTransform(dates.get(14)));

    }

    @Test
    public void testExhaustStatic() {
        doTestExhaustStatic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestExhaustStatic(final Field<T> field) {
        final RandomGenerator random = new Well19937a(0x3b18a628c1a8b5e9L);
        final FieldCachedTransformProvider<T> cachedTransformProvider = buildCache(20);
        final List<FieldAbsoluteDate<T>> dates = generateDates(field,
                                                               random,
                                                               10 * cachedTransformProvider.getCacheSize(),
                                                               50 * cachedTransformProvider.getCacheSize());

        // first batch, without exhausting
        final List<FieldStaticTransform<T>> firstBatch = new ArrayList<>();
        for (int i = 0; i < cachedTransformProvider.getCacheSize(); i++) {
            firstBatch.add(cachedTransformProvider.getStaticTransform(dates.get(i)));
        }
        for (int i = 0; i < 1000; i++) {
            // we should retrieve again and again the already computed instances
            final int k = random.nextInt(firstBatch.size());
            Assertions.assertSame(firstBatch.get(k), cachedTransformProvider.getStaticTransform(dates.get(k)));
        }
        final FieldStaticTransform<T> t14 = cachedTransformProvider.getStaticTransform(dates.get(14));

        // now exhaust the instance, except we force entry 14 to remain in the cache by reusing it
        for (int i = 0; i < dates.size(); i++) {
            Assertions.assertNotNull(cachedTransformProvider.getStaticTransform(dates.get(dates.size() - 1 - i)));
            Assertions.assertNotNull(cachedTransformProvider.getStaticTransform(dates.get(14)));
        }

        for (int i = 0; i < 100; i++) {
            // we should get new instances from the first already known dates,
            // but they should correspond to similar transforms
            final int                     k            = random.nextInt(firstBatch.size());
            final FieldStaticTransform<T> t            = cachedTransformProvider.getStaticTransform(dates.get(k));
            final FieldStaticTransform<T> backAndForth = FieldStaticTransform.compose(dates.get(k), firstBatch.get(k), t.getInverse());
            if (k != 14) {
                Assertions.assertNotSame(firstBatch.get(k), t);
            }
            Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
            Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);
        }

        // entry 14 should still be in the cache
        Assertions.assertSame(t14, cachedTransformProvider.getTransform(dates.get(14)));

    }

    private <T extends CalculusFieldElement<T>> FieldCachedTransformProvider<T> buildCache(final int size) {
        final Function<FieldAbsoluteDate<T>, FieldTransform<T>> fullGenerator =
                date -> earth1.getTransformTo(inertialFrame, date);
        final Function<FieldAbsoluteDate<T>, FieldKinematicTransform<T>> kinematicGenerator =
                date -> earth1.getKinematicTransformTo(inertialFrame, date);
        final Function<FieldAbsoluteDate<T>, FieldStaticTransform<T>> staticGenerator =
                date -> earth1.getStaticTransformTo(inertialFrame, date);
        return new FieldCachedTransformProvider<>(earth1, inertialFrame,
                                                  fullGenerator, kinematicGenerator, staticGenerator,
                                                  size);
    }

    private <T extends CalculusFieldElement<T>> List<FieldAbsoluteDate<T>> generateDates(final Field<T> field,
                                                                                         final RandomGenerator random,
                                                                                         final int total,
                                                                                         final int history) {
        final List<FieldAbsoluteDate<T>> dates = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            final int index = i - random.nextInt(history);
            final FieldAbsoluteDate<T> date = index < 0 || index >= dates.size() ?
                                      FieldAbsoluteDate.getArbitraryEpoch(field).
                                              shiftedBy(Constants.JULIAN_DAY * random.nextDouble()) :
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

        public <T extends CalculusFieldElement<T>> FieldTransform<T>
        getTransformTo(final Frame destination, final FieldAbsoluteDate<T> date) {
            ++count;
            return super.getTransformTo(destination, date);
        }

        public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T>
        getKinematicTransformTo(final Frame destination, final FieldAbsoluteDate<T> date) {
            ++count;
            return super.getKinematicTransformTo(destination, date);
        }

        public <T extends CalculusFieldElement<T>> FieldStaticTransform<T>
        getStaticTransformTo(final Frame destination, final FieldAbsoluteDate<T> date) {
            ++count;
            return super.getStaticTransformTo(destination, date);
        }

    }

}
