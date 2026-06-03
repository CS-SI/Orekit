/* Copyright 2002-2026 CS GROUP
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
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Characterization tests for {@link AbstractTimeInterpolator.InterpolationData} pinning the observable behavior of the
 * constructor. They must pass against both the pre-optimization implementation and the optimized one.
 */
class AbstractTimeInterpolatorTest {

    private static final int NB_POINTS = 4;

    private static final double STEP = 60.0;

    @Test
    void testEmptySampleThrowsNotEnoughCachedNeighbors() {
        // GIVEN
        // Empty input now falls through to the size-vs-nbInterpolationPoints check, sharing the same
        // error path as any other under-sized sample.
        final TestInterpolator interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate     date         = AbsoluteDate.ARBITRARY_EPOCH;

        // WHEN & THEN
        final OrekitIllegalArgumentException ex =
                Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                        () -> interpolator.interpolate(date, Collections.emptyList()));
        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS, ex.getSpecifier());
        Assertions.assertEquals(0,         ex.getParts()[0]);
        Assertions.assertEquals(NB_POINTS, ex.getParts()[1]);
    }

    @Test
    void testSampleEqualToInterpolationPointsCount() {
        // GIVEN
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, NB_POINTS);
        final AbsoluteDate       date         = base.shiftedBy(STEP * 1.5);

        // WHEN
        final List<AbsoluteDate> neighbors = interpolator.captureNeighbors(date, sample);

        // THEN
        Assertions.assertEquals(NB_POINTS, neighbors.size());
        for (int i = 0; i < NB_POINTS; i++) {
            Assertions.assertEquals(sample.get(i), neighbors.get(i));
        }
    }

    @Test
    void testSampleLargerThanInterpolationPointsCentered() {
        // GIVEN
        // Regularly-spaced sample (one entry every STEP seconds from base).
        // For an interpolation date centered between indices 5 and 6 of a 10-entry sample with NB_POINTS=4,
        // SortedListTrimmer picks the window centred around the entry whose date is just below the interpolation
        // date (index 5): start = max(0, 5 - (4-1)/2) = 4, end = 4 + 4 = 8 → indices [4, 5, 6, 7].
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, 10);
        final AbsoluteDate       date         = base.shiftedBy(STEP * 5.5);

        // WHEN
        final List<AbsoluteDate> neighbors = interpolator.captureNeighbors(date, sample);

        // THEN
        Assertions.assertEquals(NB_POINTS, neighbors.size());
        for (int i = 0; i < NB_POINTS; i++) {
            Assertions.assertEquals(sample.get(4 + i), neighbors.get(i));
        }
    }

    @Test
    void testSampleLargerThanInterpolationPointsBeforeMinDateWithinThreshold() {
        // GIVEN
        // Interpolation date is half a millisecond *before* minDate, within the default 1ms threshold, so
        // getCentralDate clamps it to minDate. The returned window must be the earliest NB_POINTS entries.
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, 10);
        final AbsoluteDate       date         = base.shiftedBy(-5e-4);

        // WHEN
        final List<AbsoluteDate> neighbors = interpolator.captureNeighbors(date, sample);

        // THEN
        Assertions.assertEquals(NB_POINTS, neighbors.size());
        for (int i = 0; i < NB_POINTS; i++) {
            Assertions.assertEquals(sample.get(i), neighbors.get(i));
        }
    }

    @Test
    void testSampleLargerThanInterpolationPointsAfterMaxDateWithinThreshold() {
        // GIVEN
        // Interpolation date half a millisecond after maxDate; clamped to maxDate. Returned window is the latest
        // NB_POINTS entries.
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final int                size         = 10;
        final List<AbsoluteDate> sample       = buildSortedSample(base, size);
        final AbsoluteDate       date         = sample.get(size - 1).shiftedBy(5e-4);

        // WHEN
        final List<AbsoluteDate> neighbors = interpolator.captureNeighbors(date, sample);

        // THEN
        Assertions.assertEquals(NB_POINTS, neighbors.size());
        for (int i = 0; i < NB_POINTS; i++) {
            Assertions.assertEquals(sample.get(size - NB_POINTS + i), neighbors.get(i));
        }
    }

    @Test
    void testSampleLargerThanInterpolationPointsBeyondThresholdBefore() {
        // GIVEN
        // Interpolation date is 10 seconds before minDate — well beyond the 1ms threshold.
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, 10);
        final AbsoluteDate       date         = base.shiftedBy(-10.0);

        // WHEN & THEN
        final TimeStampedCacheException ex =
                Assertions.assertThrows(TimeStampedCacheException.class,
                                        () -> interpolator.captureNeighbors(date, sample));
        Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, ex.getSpecifier());
    }

    @Test
    void testSampleLargerThanInterpolationPointsBeyondThresholdAfter() {
        // GIVEN
        // Interpolation date is 10 seconds after maxDate — well beyond the 1ms threshold.
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final int                size         = 10;
        final List<AbsoluteDate> sample       = buildSortedSample(base, size);
        final AbsoluteDate       date         = sample.get(size - 1).shiftedBy(10.0);

        // WHEN & THEN
        final TimeStampedCacheException ex =
                Assertions.assertThrows(TimeStampedCacheException.class,
                                        () -> interpolator.captureNeighbors(date, sample));
        Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, ex.getSpecifier());
    }

    @Test
    void testSampleSmallerThanInterpolationPoints() {
        // GIVEN
        // 2 entries < NB_POINTS (4).
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, 2);
        final AbsoluteDate       date         = base.shiftedBy(STEP);

        // WHEN & THEN
        final OrekitIllegalArgumentException ex =
                Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                        () -> interpolator.captureNeighbors(date, sample));
        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS, ex.getSpecifier());
    }

    @Test
    void testNeighborListIsUnmodifiable() {
        // GIVEN
        final TestInterpolator   interpolator = new TestInterpolator(NB_POINTS);
        final AbsoluteDate       base         = AbsoluteDate.ARBITRARY_EPOCH;
        final List<AbsoluteDate> sample       = buildSortedSample(base, 10);
        final AbsoluteDate       date         = base.shiftedBy(STEP * 4.5);

        // WHEN
        final List<AbsoluteDate> neighbors = interpolator.captureNeighbors(date, sample);

        // THEN
        Assertions.assertThrows(UnsupportedOperationException.class,
                                () -> neighbors.add(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> neighbors.remove(0));
    }

    private static List<AbsoluteDate> buildSortedSample(final AbsoluteDate base, final int size) {
        final AbsoluteDate[] entries = new AbsoluteDate[size];
        for (int i = 0; i < size; i++) {
            entries[i] = base.shiftedBy(i * STEP);
        }
        return new ArrayList<>(Arrays.asList(entries));
    }

    /**
     * Minimal subclass of {@link AbstractTimeInterpolator} whose {@link #interpolate} simply hands back the captured
     * neighbor list (via a side-channel field) so we can inspect it.
     */
    private static final class TestInterpolator extends AbstractTimeInterpolator<AbsoluteDate> {

        private List<AbsoluteDate> lastNeighborList;

        TestInterpolator(final int nbInterpolationPoints) {
            super(nbInterpolationPoints, AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
        }

        @Override
        protected AbsoluteDate interpolate(final AbstractTimeInterpolator<AbsoluteDate>.InterpolationData data) {
            lastNeighborList = data.getNeighborList();
            return data.getInterpolationDate();
        }

        List<AbsoluteDate> captureNeighbors(final AbsoluteDate date, final Collection<AbsoluteDate> sample) {
            interpolate(date, sample);
            return lastNeighborList;
        }
    }
}
