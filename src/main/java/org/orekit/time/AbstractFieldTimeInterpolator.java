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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.FieldSortedListTrimmer;
import org.orekit.utils.ImmutableFieldTimeStampedCache;

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
 * @param <KK> type of the field element
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractFieldTimeInterpolator<T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>>
        implements FieldTimeInterpolator<T, KK> {

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
    public AbstractFieldTimeInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        this.interpolationPoints    = interpolationPoints;
        this.extrapolationThreshold = extrapolationThreshold;
    }

    /**
     * Method checking if given interpolator is compatible with given sample size.
     *
     * @param interpolator interpolator
     * @param sampleSize sample size
     * @param <T> type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> void checkInterpolatorCompatibilityWithSampleSize(
            final FieldTimeInterpolator<? extends FieldTimeStamped<T>, T> interpolator,
            final int sampleSize) {

        // Retrieve all sub-interpolators (or a singleton list with given interpolator if there are no sub-interpolators)
        final List<FieldTimeInterpolator<? extends FieldTimeStamped<T>, T>> subInterpolators =
                interpolator.getSubInterpolators();
        for (final FieldTimeInterpolator<? extends FieldTimeStamped<T>, T> subInterpolator : subInterpolators) {
            if (sampleSize < subInterpolator.getNbInterpolationPoints()) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, sampleSize);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The stream must yield elements in chronological order.
     */
    @Override
    public T interpolate(final FieldAbsoluteDate<KK> interpolationDate, final Stream<T> sample) {
        return interpolate(interpolationDate, sample.collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Precondition:</strong> {@code sample} must be sorted in chronological order. Passing an unsorted
     * sample yields undefined neighbors and may throw
     * {@link org.orekit.errors.TimeStampedCacheException}.
     */
    @Override
    public T interpolate(final FieldAbsoluteDate<KK> interpolationDate, final Collection<T> sample) {
        final InterpolationData interpolationData = new InterpolationData(interpolationDate, sample);
        return interpolate(interpolationData);
    }

    /**
     * Get the central date to use to find neighbors while taking into account extrapolation threshold.
     *
     * @param date interpolation date
     * @param cachedSamples cached samples
     * @param threshold extrapolation threshold
     * @param <T> type of time stamped element
     * @param <KK> type of calculus field element
     *
     * @return central date to use to find neighbors
     * @since 12.0.1
     */
    public static <T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>> FieldAbsoluteDate<KK> getCentralDate(
            final FieldAbsoluteDate<KK> date,
            final ImmutableFieldTimeStampedCache<T, KK> cachedSamples,
            final double threshold) {
        return getCentralDate(
                date,
                cachedSamples.getEarliest().getDate(),
                cachedSamples.getLatest().getDate(),
                threshold);
    }

    /**
     * Get the central date to use to find neighbors while taking into account extrapolation threshold.
     *
     * @param date interpolation date
     * @param minDate earliest date in the sample.
     * @param maxDate latest date in the sample.
     * @param threshold extrapolation threshold
     * @param <KK> type of calculus field element
     *
     * @return central date to use to find neighbors
     * @since 12.0.1
     */
    public static <KK extends CalculusFieldElement<KK>> FieldAbsoluteDate<KK> getCentralDate(
            final FieldAbsoluteDate<KK> date,
            final FieldAbsoluteDate<KK> minDate,
            final FieldAbsoluteDate<KK> maxDate,
            final double threshold) {
        final FieldAbsoluteDate<KK> central;

        if (date.compareTo(minDate) < 0 && FastMath.abs(date.durationFrom(minDate)).getReal() <= threshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance before minDate
            central = minDate;
        } else if (date.compareTo(maxDate) > 0 && FastMath.abs(date.durationFrom(maxDate)).getReal() <= threshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance after maxDate
            central = maxDate;
        } else {
            central = date;
        }

        return central;
    }

    /** {@inheritDoc} */
    public List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> getSubInterpolators() {
        return Collections.singletonList(this);
    }

    /** {@inheritDoc} */
    public int getNbInterpolationPoints() {
        final List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> subInterpolators = getSubInterpolators();
        // In case the interpolator does not have sub interpolators
        if (subInterpolators.size() == 1) {
            return interpolationPoints;
        }
        // Otherwise find maximum number of interpolation points among sub interpolators
        else {
            final Optional<Integer> optionalMaxNbInterpolationPoints =
                    subInterpolators.stream().map(FieldTimeInterpolator::getNbInterpolationPoints).max(Integer::compareTo);
            if (optionalMaxNbInterpolationPoints.isPresent()) {
                return optionalMaxNbInterpolationPoints.get();
            } else {
                // This should never happen
                throw new OrekitInternalError(null);
            }
        }
    }

    /**
     * Get the number of interpolation points for this instance only i.e., not taking into account sub-interpolators.
     *
     * @return required the number of interpolation points for this instance only i.e., not taking into account
     * sub-interpolators.
     */
    public int getInternalNbInterpolationPoints() {
        return interpolationPoints;
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
     * @param <S> type of the field element
     */
    protected <S extends CalculusFieldElement<S>> void addOptionalSubInterpolatorIfDefined(
            final FieldTimeInterpolator<? extends FieldTimeStamped<S>, S> subInterpolator,
            final List<FieldTimeInterpolator<? extends FieldTimeStamped<S>, S>> subInterpolators) {
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
    protected KK getTimeParameter(final FieldAbsoluteDate<KK> interpolatingTime,
                                  final FieldAbsoluteDate<KK> previousDate,
                                  final FieldAbsoluteDate<KK> nextDate) {

        return interpolatingTime.durationFrom(previousDate).divide(nextDate.getDate().durationFrom(previousDate));
    }

    /**
     * Nested class used to store interpolation data.
     * <p>
     * It makes the interpolator thread safe.
     */
    public class InterpolationData {

        /** Interpolation date. */
        private final FieldAbsoluteDate<KK> interpolationDate;

        /** Unmodifiable list of neighbors. */
        private final List<T> neighborList;

        /** Field of the element. */
        private final Field<KK> field;

        /** Fielded zero. */
        private final KK zero;

        /** Fielded one. */
        private final KK one;

        /**
         * Constructor (Collection variant).
         * <p>
         * If {@code sample} is already a {@link List}, it is used directly; otherwise it is copied into a new
         * {@link ArrayList}. Forwards to {@link #InterpolationData(FieldAbsoluteDate, List)} — see that constructor
         * for the sorted-sample precondition.
         *
         * @param interpolationDate interpolation date
         * @param sample            time stamped sample (chronologically sorted)
         */
        protected InterpolationData(final FieldAbsoluteDate<KK> interpolationDate, final Collection<T> sample) {
            this(interpolationDate, (sample instanceof List) ? (List<T>) sample : new ArrayList<>(sample));
        }

        /**
         * Constructor.
         * <p>
         * <strong>Precondition:</strong> {@code sample} must be sorted in chronological order. Passing an unsorted
         * sample yields undefined neighbors and may throw
         * {@link org.orekit.errors.TimeStampedCacheException}. Prior implementations silently sorted the input;
         * this is no longer the case.
         *
         * @param interpolationDate interpolation date
         * @param sample            time stamped sample (chronologically sorted)
         */
        protected InterpolationData(final FieldAbsoluteDate<KK> interpolationDate, final List<T> sample) {

            // Check if there is enough sample points
            final int nbInterpolationPoints = getNbInterpolationPoints();
            if (sample.size() < nbInterpolationPoints) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                                                         sample.size(), nbInterpolationPoints);
            }

            // Shortcut to see if sample size is equal to number of interpolation points
            if (sample.size() == nbInterpolationPoints) {
                this.neighborList = Collections.unmodifiableList(sample);
            } else {
                final FieldAbsoluteDate<KK> central = getCentralDate(interpolationDate,
                                                                     sample.get(0).getDate(),
                                                                     sample.get(sample.size() - 1).getDate(),
                                                                     extrapolationThreshold);

                // Trimmer returns a sublist view, so wrap (don't copy) for immutability.
                final FieldSortedListTrimmer trimmer = new FieldSortedListTrimmer(nbInterpolationPoints);
                this.neighborList = Collections.unmodifiableList(trimmer.getNeighborsSubList(central, sample));
            }

            // Extract field and useful terms
            this.field = interpolationDate.getField();
            this.zero  = field.getZero();
            this.one   = field.getOne();

            // Store interpolation date
            this.interpolationDate = interpolationDate;
        }

        /** Get interpolation date.
         * @return interpolation date
         */
        public FieldAbsoluteDate<KK> getInterpolationDate() {
            return interpolationDate;
        }

        /** Get neighbor list.
         * @return neighbor list
         */
        public List<T> getNeighborList() {
            return neighborList;
        }

        /** Get field.
         * @return field
         */
        public Field<KK> getField() {
            return field;
        }

        /** Get zero.
         * @return zero
         */
        public KK getZero() {
            return zero;
        }

        /** Get one.
         * @return one
         */
        public KK getOne() {
            return one;
        }
    }
}
