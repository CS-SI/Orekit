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
import java.util.concurrent.atomic.AtomicInteger;

class TimeStampedDoubleHermiteInterpolatorTest {

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // When
        final TimeStampedDoubleHermiteInterpolator interpolator = new TimeStampedDoubleHermiteInterpolator();

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
        final TimeInterpolator<TimeStampedDouble> interpolator = new TimeStampedDoubleHermiteInterpolator();

        // Create sample and interpolation dates
        final int                     sampleSize  = 100;
        final AbsoluteDate            initialDate = new AbsoluteDate();
        final List<TimeStampedDouble> sample      = new ArrayList<>();
        final List<AbsoluteDate>      dates       = new ArrayList<>();
        for (int i = 0; i < sampleSize + 1; i++) {
            sample.add(new TimeStampedDouble(i * i, initialDate.shiftedBy(i * 60)));
            dates.add(initialDate.shiftedBy(i * 60));
        }

        // Create multithreading environment
        ExecutorService service = Executors.newFixedThreadPool(sampleSize);

        final AtomicInteger           sum   = new AtomicInteger(0);
        final List<Callable<Integer>> tasks = new ArrayList<>();
        for (final AbsoluteDate date : dates) {
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

        private final TimeInterpolator<TimeStampedDouble> interpolator;

        private final List<TimeStampedDouble> sample;

        private final AtomicInteger sum;

        private final AbsoluteDate interpolationDate;

        private ParallelTask(final TimeInterpolator<TimeStampedDouble> interpolator, final AtomicInteger sum,
                             final List<TimeStampedDouble> sample, final AbsoluteDate interpolationDate) {
            // Store interpolator
            this.interpolator      = interpolator;
            this.sum               = sum;
            this.interpolationDate = interpolationDate;
            this.sample            = sample;
        }

        @Override
        public Integer call() {
            // Add result to sum
            final int valueToAdd = (int) interpolator.interpolate(interpolationDate, sample).getValue();
            sum.getAndAdd(valueToAdd);
            return 1;
        }
    }
}