/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.ImmutableTimeStampedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for time interpolator.
 *
 * @param <T> interpolated time stamped type
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractTimeInterpolator<T extends TimeStamped> implements TimeInterpolator<T> {

    /** Default extrapolation time threshold: 1ms. */
    public static final double DEFAULT_EXTRAPOLATION_THRESHOLD_SEC = 1e-3;

    /** Default number of interpolation points. */
    public static final int DEFAULT_INTERPOLATION_POINTS = 2;

    /** The extrapolation threshold beyond which the propagation will fail. */
    private final double extrapolationThreshold;

    /** Neighbor size. */
    private final int interpolationPoints;

    /**
     * Constructor.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     */
    public AbstractTimeInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        this.interpolationPoints    = interpolationPoints;
        this.extrapolationThreshold = extrapolationThreshold;
    }

    /**
     * Method checking if given interpolator is compatible with given sample size.
     *
     * @param interpolator interpolator
     * @param sampleSize sample size
     */
    public static void checkInterpolatorCompatibilityWithSampleSize(
            final TimeInterpolator<? extends TimeStamped> interpolator,
            final int sampleSize) {

        // Retrieve all sub-interpolators (or a singleton list with given interpolator if there are no sub-interpolators)
        final List<TimeInterpolator<? extends TimeStamped>> subInterpolators = interpolator.getSubInterpolators();
        for (final TimeInterpolator<? extends TimeStamped> subInterpolator : subInterpolators) {
            if (sampleSize < subInterpolator.getNbInterpolationPoints()) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, sampleSize);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public T interpolate(final AbsoluteDate interpolationDate, final Stream<T> sample) {
        return interpolate(interpolationDate, sample.collect(Collectors.toList()));
    }

    /** {@inheritDoc}. */
    @Override
    public T interpolate(final AbsoluteDate interpolationDate, final Collection<T> sample) {
        final InterpolationData interpolationData = new InterpolationData(interpolationDate, sample);
        return interpolate(interpolationData);
    }

    /**
     * Get the central date to use to find neighbors while taking into account extrapolation threshold.
     *
     * @param date interpolation date
     * @param cachedSamples cached samples
     * @param threshold extrapolation threshold
     * @param <T> type of element
     *
     * @return central date to use to find neighbors
     * @since 12.0.1
     */
    public static <T extends TimeStamped> AbsoluteDate getCentralDate(final AbsoluteDate date,
                                                                      final ImmutableTimeStampedCache<T> cachedSamples,
                                                                      final double threshold) {
        final AbsoluteDate minDate = cachedSamples.getEarliest().getDate();
        final AbsoluteDate maxDate = cachedSamples.getLatest().getDate();
        return getCentralDate(date, minDate, maxDate, threshold);
    }

    /**
     * Get the central date to use to find neighbors while taking into account extrapolation threshold.
     *
     * @param date interpolation date
     * @param minDate earliest date in the sample.
     * @param maxDate latest date in the sample.
     * @param threshold extrapolation threshold
     * @param <T> type of element
     *
     * @return central date to use to find neighbors
     * @since 12.0.1
     */
    public static <T extends TimeStamped> AbsoluteDate getCentralDate(final AbsoluteDate date,
                                                                      final AbsoluteDate minDate,
                                                                      final AbsoluteDate maxDate,
                                                                      final double threshold) {
        final AbsoluteDate central;

        if (date.compareTo(minDate) < 0 && FastMath.abs(date.durationFrom(minDate)) <= threshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance before minDate
            central = minDate;
        } else if (date.compareTo(maxDate) > 0 && FastMath.abs(date.durationFrom(maxDate)) <= threshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance after maxDate
            central = maxDate;
        } else {
            central = date;
        }

        return central;
    }

    /** {@inheritDoc} */
    public List<TimeInterpolator<? extends TimeStamped>> getSubInterpolators() {
        return Collections.singletonList(this);
    }

    /** {@inheritDoc} */
    public int getNbInterpolationPoints() {
        final List<TimeInterpolator<? extends TimeStamped>> subInterpolators = getSubInterpolators();
        // In case the interpolator does not have sub interpolators
        if (subInterpolators.size() == 1) {
            return interpolationPoints;
        }
        // Otherwise find maximum number of interpolation points among sub interpolators
        else {
            final Optional<Integer> optionalMaxNbInterpolationPoints =
                    subInterpolators.stream().map(TimeInterpolator::getNbInterpolationPoints).max(Integer::compareTo);
            if (optionalMaxNbInterpolationPoints.isPresent()) {
                return optionalMaxNbInterpolationPoints.get();
            } else {
                // This should never happen
                throw new OrekitInternalError(null);
            }
        }
    }

    /** {@inheritDoc} */
    public double getExtrapolationThreshold() {
        return extrapolationThreshold;
    }

    /**
     * Add all lowest level sub interpolators to the sub interpolator list.
     *
     * @param subInterpolator optional sub interpolator to add
     * @param subInterpolators list of sub interpolators
     */
    protected void addOptionalSubInterpolatorIfDefined(final TimeInterpolator<? extends TimeStamped> subInterpolator,
                                                       final List<TimeInterpolator<? extends TimeStamped>> subInterpolators) {
        // Add all lowest level sub interpolators
        if (subInterpolator != null) {
            subInterpolators.addAll(subInterpolator.getSubInterpolators());
        }
    }

    /**
     * Interpolate instance from given interpolation data.
     *
     * @param interpolationData interpolation data
     *
     * @return interpolated instance from given interpolation data.
     */
    protected abstract T interpolate(InterpolationData interpolationData);

    /**
     * Get the time parameter which lies between [0:1] by normalizing the difference between interpolating time and previous
     * date by the Δt between tabulated values.
     *
     * @param interpolatingTime time at which we want to interpolate a value (between previous and next tabulated dates)
     * @param previousDate previous tabulated value date
     * @param nextDate next tabulated value date
     *
     * @return time parameter which lies between [0:1]
     */
    protected double getTimeParameter(final AbsoluteDate interpolatingTime,
                                      final AbsoluteDate previousDate,
                                      final AbsoluteDate nextDate) {
        return interpolatingTime.durationFrom(previousDate) / nextDate.getDate().durationFrom(previousDate);
    }

    /**
     * Nested class used to store interpolation data.
     * <p>
     * It makes the interpolator thread safe.
     */
    public class InterpolationData {

        /** Interpolation date. */
        private final AbsoluteDate interpolationDate;

        /** Neighbor list around interpolation date. */
        private final List<T> neighborList;

        /**
         * Constructor.
         *
         * @param interpolationDate interpolation date
         * @param sample time stamped sample
         */
        protected InterpolationData(final AbsoluteDate interpolationDate, final Collection<T> sample) {
            // Handle specific case that is not handled by the immutable time stamped cache constructor
            if (sample.isEmpty()) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, 0);
            }

            // TODO performance: create neighborsList without copying sample.
            if (sample.size() == interpolationPoints) {
                // shortcut for simple case
                // copy list to make neighborList immutable
                this.neighborList = Collections.unmodifiableList(new ArrayList<>(sample));
            } else {
                // else, select sample.

                // Create immutable time stamped cache
                final ImmutableTimeStampedCache<T> cachedSamples =
                        new ImmutableTimeStampedCache<>(interpolationPoints, sample);

                // Find neighbors
                final AbsoluteDate central = AbstractTimeInterpolator.getCentralDate(
                        interpolationDate,
                        cachedSamples,
                        extrapolationThreshold);
                final Stream<T> neighborsStream = cachedSamples.getNeighbors(central);

                // Convert to unmodifiable list
                this.neighborList = Collections.unmodifiableList(neighborsStream.collect(Collectors.toList()));
            }

            // Store interpolation date
            this.interpolationDate = interpolationDate;
        }

        /**
         * Get the central date to use to find neighbors while taking into account extrapolation threshold.
         *
         * @param date interpolation date
         *
         * @return central date to use to find neighbors
         *
         * @deprecated This method appears to be unused and may be removed in Orekit 13.0.
         * Please Comment on forum.orekit.org if you have a use case for this method.
         */
        @Deprecated
        protected AbsoluteDate getCentralDate(final AbsoluteDate date) {
            return AbstractTimeInterpolator.getCentralDate(date,
                    neighborList.get(0).getDate(),
                    neighborList.get(neighborList.size() - 1).getDate(),
                    extrapolationThreshold);
        }

        /** Get interpolation date.
         * @return interpolation date
         */
        public AbsoluteDate getInterpolationDate() {
            return interpolationDate;
        }

        /** Get cached samples.
         * @return cached samples
         *
         * @deprecated This method appears to be unused and may be removed in Orekit 13.0.
         * Please Comment on forum.orekit.org if you have a use case for this method.
         */
        @Deprecated
        public ImmutableTimeStampedCache<T> getCachedSamples() {
            return new ImmutableTimeStampedCache<>(interpolationPoints, neighborList);
        }

        /** Get neighbor list.
         * @return neighbor list
         */
        public List<T> getNeighborList() {
            return neighborList;
        }

    }
}
