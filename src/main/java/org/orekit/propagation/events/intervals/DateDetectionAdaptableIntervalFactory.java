/* Copyright 2022-2024 Romain Serra
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
package org.orekit.propagation.events.intervals;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Factory for adaptable interval tuned for date(s) detection.
 *
 * @see org.orekit.propagation.events.DateDetector
 * @see org.orekit.propagation.events.FieldDateDetector
 * @author Romain Serra
 * @since 13.0
 */
public class DateDetectionAdaptableIntervalFactory {

    /** Default value for max check. */
    public static final double DEFAULT_MAX_CHECK = 1.0e10;

    /**
     * Private constructor.
     */
    private DateDetectionAdaptableIntervalFactory() {
        // factory class
    }

    /**
     * Return a candidate {@link AdaptableInterval} for single date detection.
     * @return adaptable interval
     */
    public static AdaptableInterval getSingleDateDetectionAdaptableInterval() {
        return AdaptableInterval.of(DEFAULT_MAX_CHECK);
    }

    /**
     * Return a candidate {@link AdaptableInterval} for multiple dates detection with a constant max. check.
     * @param timeStampeds event dates
     * @return adaptable interval
     */
    public static AdaptableInterval getDatesDetectionConstantInterval(final TimeStamped... timeStampeds) {
        if (timeStampeds == null || timeStampeds.length < 2) {
            return getSingleDateDetectionAdaptableInterval();
        }
        return AdaptableInterval.of(getMinGap(timeStampeds) * 0.5);
    }

    /**
     * Return a candidate {@link AdaptableInterval} for multiple dates detection.
     * @param timeStampeds event dates
     * @return adaptable interval
     */
    public static AdaptableInterval getDatesDetectionInterval(final TimeStamped... timeStampeds) {
        if (timeStampeds == null || timeStampeds.length < 2) {
            return getSingleDateDetectionAdaptableInterval();
        }
        final double minGap = getMinGap(timeStampeds);
        final SortedSet<TimeStamped> sortedSet = new TreeSet<>(new ChronologicalComparator());
        sortedSet.addAll(Arrays.asList(timeStampeds));
        return (state, isForward) -> {
            final AbsoluteDate date = state.getDate();
            double minDistance = Double.POSITIVE_INFINITY;
            if (isForward) {
                for (final TimeStamped ts : sortedSet) {
                    final AbsoluteDate nextDate = ts.getDate();
                    if (date.isBefore(nextDate)) {
                        minDistance = nextDate.durationFrom(date);
                        break;
                    }
                }
            } else {
                final List<TimeStamped> inverted = new ArrayList<>(sortedSet);
                Collections.reverse(inverted);
                for (final TimeStamped ts : inverted) {
                    final AbsoluteDate nextDate = ts.getDate();
                    if (date.isAfter(nextDate)) {
                        minDistance = date.durationFrom(nextDate);
                        break;
                    }
                }
            }
            return FastMath.abs(minDistance) + minGap / 2;
        };
    }

    /**
     * Return a candidate {@link FieldAdaptableInterval} for single date detection.
     * @param <T> field type
     * @return adaptable interval
     */
    public static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> getSingleDateDetectionFieldAdaptableInterval() {
        return FieldAdaptableInterval.of(DEFAULT_MAX_CHECK);
    }

    /**
     * Return a candidate {@link FieldAdaptableInterval} for multiple dates detection with a constant max. check.
     * @param timeStampeds event dates
     * @param <T> field type
     * @return adaptable interval
     */
    @SafeVarargs
    public static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> getDatesDetectionFieldConstantInterval(final FieldTimeStamped<T>... timeStampeds) {
        if (timeStampeds == null || timeStampeds.length < 2) {
            return getSingleDateDetectionFieldAdaptableInterval();
        }
        final double minGap = getMinGap(Arrays.stream(timeStampeds).map(t -> (TimeStamped) t.getDate().toAbsoluteDate())
                .toArray(TimeStamped[]::new));
        return FieldAdaptableInterval.of(minGap * 0.5);
    }

    /**
     * Return a candidate {@link FieldAdaptableInterval} for multiple dates detection.
     * @param timeStampeds event dates
     * @param <T> field type
     * @return adaptable interval
     */
    @SafeVarargs
    public static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> getDatesDetectionFieldInterval(final FieldTimeStamped<T>... timeStampeds) {
        if (timeStampeds == null || timeStampeds.length < 2) {
            return getSingleDateDetectionFieldAdaptableInterval();
        }
        return FieldAdaptableInterval.of(getDatesDetectionInterval(Arrays.stream(timeStampeds)
                .map(t -> (TimeStamped) t.getDate().toAbsoluteDate()).toArray(TimeStamped[]::new)));
    }

    /**
     * Compute min. gap between dated objects.
     * @param timeStampeds time stamped objects
     * @return minimym gap
     */
    private static double getMinGap(final TimeStamped... timeStampeds) {
        double minGap = DEFAULT_MAX_CHECK;
        for (final TimeStamped timeStamped : timeStampeds) {
            final Optional<Double> minDistance = Arrays.stream(timeStampeds)
                    .map(t -> (t.getDate() != timeStamped.getDate()) ? FastMath.abs(t.durationFrom(timeStamped)) : Double.POSITIVE_INFINITY)
                    .min(Double::compareTo);
            if (minDistance.isPresent()) {
                minGap = FastMath.min(minGap, minDistance.get());
            }
        }
        return minGap;
    }
}
