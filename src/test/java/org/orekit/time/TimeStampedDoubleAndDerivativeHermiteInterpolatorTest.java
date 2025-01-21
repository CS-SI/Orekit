/* Copyright 2002-2025 CS GROUP
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
import java.util.concurrent.atomic.DoubleAccumulator;

class TimeStampedDoubleAndDerivativeAndDerivativeHermiteInterpolatorTest {

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // When
        final TimeStampedDoubleAndDerivativeHermiteInterpolator interpolator = new TimeStampedDoubleAndDerivativeHermiteInterpolator();

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
        // Create interpolator
        final TimeInterpolator<TimeStampedDoubleAndDerivative> interpolator = new TimeStampedDoubleAndDerivativeHermiteInterpolator();

        // Create sample and interpolation dates
        final int                                  sampleSize  = 100;
        final AbsoluteDate                         initialDate = new AbsoluteDate();
        final List<TimeStampedDoubleAndDerivative> sample      = new ArrayList<>();
        final List<AbsoluteDate>                   dates       = new ArrayList<>();
        for (int i = 1; i < sampleSize + 1; i++) {
            sample.add(new TimeStampedDoubleAndDerivative(i * i, i / 30.0,
                                                          initialDate.shiftedBy(i * 60)));
            dates.add(initialDate.shiftedBy(i * 60));
        }

        // Create multithreading environment
        ExecutorService service = Executors.newFixedThreadPool(sampleSize);

        final DoubleAccumulator sum0  = new DoubleAccumulator(Double::sum, 0.0);
        final DoubleAccumulator sum1  = new DoubleAccumulator(Double::sum, 0.0);
        final List<Callable<Integer>> tasks = new ArrayList<>();
        for (final AbsoluteDate date : dates) {
            tasks.add(new ParallelTask(interpolator, sum0, sum1, sample, date));
        }

        // WHEN
        service.invokeAll(tasks);

        // THEN
        // Sum of 1*1 + 2*2 + 3*3 + ...
        // Sum of 1 / 30 + 2 / 30 + ...
        final double expectedSum0 = sampleSize * (sampleSize + 1) * (2 * sampleSize + 1) / 6.0;
        final double expectedSum1 = sampleSize * (sampleSize + 1)  / 60.0;
        try {
            // wait for proper ending
            service.shutdown();
            Assertions.assertTrue(service.awaitTermination(5, TimeUnit.SECONDS));
        } catch (InterruptedException ie) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
        Assertions.assertEquals(expectedSum0, sum0.get(), 1.0e-12);
        Assertions.assertEquals(expectedSum1, sum1.get(), 1.0e-12);
    }

    /** Custom class for multi threading testing purpose */
    private static class ParallelTask implements Callable<Integer> {

        private final TimeInterpolator<TimeStampedDoubleAndDerivative> interpolator;

        private final List<TimeStampedDoubleAndDerivative> sample;

        private final DoubleAccumulator sum0;

        private final DoubleAccumulator sum1;

        private final AbsoluteDate interpolationDate;

        private ParallelTask(final TimeInterpolator<TimeStampedDoubleAndDerivative> interpolator,
                             final DoubleAccumulator sum0, final DoubleAccumulator sum1,
                             final List<TimeStampedDoubleAndDerivative> sample,
                             final AbsoluteDate interpolationDate) {
            // Store interpolator
            this.interpolator      = interpolator;
            this.sum0              = sum0;
            this.sum1              = sum1;
            this.interpolationDate = interpolationDate;
            this.sample            = sample;
        }

        @Override
        public Integer call() {
            // Add result to sums
            final TimeStampedDoubleAndDerivative interpolated = interpolator.interpolate(interpolationDate, sample);
            sum0.accumulate(interpolated.getValue());
            sum1.accumulate(interpolated.getDerivative());
            return 1;
        }
    }
}
