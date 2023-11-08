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
package org.orekit.time;

import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class TimeStampedFieldHermiteInterpolatorTest {
    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // When
        final TimeStampedFieldHermiteInterpolator<Binary64> interpolator = new TimeStampedFieldHermiteInterpolator<>();

        // Then
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                                interpolator.getNbInterpolationPoints());
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
    }

    @RepeatedTest(10)
    @DisplayName("test interpolator in multi-threaded environment")
    void testIssue1164() throws InterruptedException {
        // GIVEN
        // Create field instance
        final Field<Binary64> field = Binary64Field.getInstance();

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> interpolator =
                new TimeStampedFieldHermiteInterpolator<>();

        // Create sample and interpolation dates
        final int                               sampleSize  = 100;
        final FieldAbsoluteDate<Binary64>       initialDate = new FieldAbsoluteDate<>(field);
        final List<TimeStampedField<Binary64>>  sample      = new ArrayList<>();
        final List<FieldAbsoluteDate<Binary64>> dates       = new ArrayList<>();
        for (int i = 0; i < sampleSize + 1; i++) {
            sample.add(new TimeStampedField<>(new Binary64(i * i), initialDate.shiftedBy(i * 60)));
            dates.add(initialDate.shiftedBy(i * 60));
        }

        // Create multithreading environment
        ExecutorService service = Executors.newFixedThreadPool(sampleSize);

        final AtomicInteger           sum   = new AtomicInteger(0);
        final List<Callable<Integer>> tasks = new ArrayList<>();
        for (final FieldAbsoluteDate<Binary64> date : dates) {
            tasks.add(new ParallelTask(interpolator, sum, sample, date));
        }

        // WHEN
        service.invokeAll(tasks);

        // THEN
        // Sum of 1*1 + 2*2 + 3*3 + ...
        final int expectedSum = sampleSize * (sampleSize + 1) * (2 * sampleSize + 1) / 6;
        Assertions.assertEquals(expectedSum, sum.get());
        try {
            // wait for proper ending
            service.shutdown();
            service.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    /** Custom class for multi threading testing purpose */
    private static class ParallelTask implements Callable<Integer> {

        private final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> interpolator;

        private final List<TimeStampedField<Binary64>> sample;

        private final AtomicInteger sum;

        private final FieldAbsoluteDate<Binary64> interpolationDate;

        private ParallelTask(final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> interpolator,
                             final AtomicInteger sum, final List<TimeStampedField<Binary64>> sample,
                             final FieldAbsoluteDate<Binary64> interpolationDate) {
            // Store interpolator
            this.interpolator      = interpolator;
            this.sum               = sum;
            this.interpolationDate = interpolationDate;
            this.sample            = sample;
        }

        @Override
        public Integer call() {
            // Add result to sum
            final int valueToAdd = (int) interpolator.interpolate(interpolationDate, sample).getValue().getReal();
            sum.getAndAdd(valueToAdd);
            return 1;
        }
    }

}