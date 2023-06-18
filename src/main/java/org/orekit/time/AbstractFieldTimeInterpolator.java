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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.ImmutableFieldTimeStampedCache;

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

    // CHECKSTYLE: stop VisibilityModifier check
    /** The extrapolation threshold beyond which the propagation will fail. */
    protected final double extrapolationThreshold;

    /** Neighbor size. */
    protected final int interpolationPoints;

    /** Immutable time stamped cached samples. */
    protected ImmutableFieldTimeStampedCache<T, KK> cachedSamples;

    /** Unmodifiable list of neighbors. */
    protected List<T> neighborList;

    /** Field of the element. */
    protected Field<KK> field;

    /** Fielded zero. */
    protected KK zero;

    /** Fielded one. */
    protected KK one;

    // CHECKSTYLE: resume VisibilityModifier check

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
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, sampleSize);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public T interpolate(final FieldAbsoluteDate<KK> interpolationDate, final Stream<T> sample) {
        return interpolate(interpolationDate, sample.collect(Collectors.toList()));
    }

    /** {@inheritDoc}. */
    @Override
    public T interpolate(final FieldAbsoluteDate<KK> interpolationDate, final Collection<T> sample) {

        try {
            // Handle specific case that is not handled by the immutable time stamped cache constructor
            if (sample.size() < 2) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, sample.size());
            }

            // Create immutable time stamped cache
            cachedSamples = new ImmutableFieldTimeStampedCache<>(interpolationPoints, sample);

            // Find neighbors
            final FieldAbsoluteDate<KK> central         = getCentralDate(interpolationDate);
            final Stream<T>             neighborsStream = cachedSamples.getNeighbors(central);

            // Extract field and useful terms
            this.field = cachedSamples.getEarliest().getDate().getField();
            this.zero  = field.getZero();
            this.one   = field.getOne();

            // Convert to unmodifiable list
            neighborList = Collections.unmodifiableList(neighborsStream.collect(Collectors.toList()));

            return interpolate(interpolationDate);
        }
        catch (OrekitIllegalArgumentException exception) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, sample.size());
        }
    }

    /** {@inheritDoc} */
    public List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> getSubInterpolators() {
        return Collections.singletonList(this);
    }

    /** {@inheritDoc} */
    public int getNbInterpolationPoints() {
        return interpolationPoints;
    }

    /** {@inheritDoc} */
    public double getExtrapolationThreshold() {
        return extrapolationThreshold;
    }

    /**
     * Add all lowest level sub interpolators to the sub interpolator list.
     *
     * @param optionalSubInterpolator optional sub interpolator to add
     * @param subInterpolators list of sub interpolators
     * @param <T> type of the field element
     */
    protected <T extends CalculusFieldElement<T>> void addOptionalSubInterpolatorIfDefined(
            final Optional<? extends FieldTimeInterpolator<? extends FieldTimeStamped<T>, T>> optionalSubInterpolator,
            final List<FieldTimeInterpolator<? extends FieldTimeStamped<T>, T>> subInterpolators) {

        // Add all lowest level sub interpolators
        if (optionalSubInterpolator.isPresent()) {
            final FieldTimeInterpolator<? extends FieldTimeStamped<T>, T> subInterpolator = optionalSubInterpolator.get();
            subInterpolators.addAll(subInterpolator.getSubInterpolators());
        }

    }

    /**
     * Get the central date to use to find neighbors while taking into account extrapolation threshold.
     *
     * @param interpolationDate interpolation date
     *
     * @return central date to use to find neighbors
     */
    protected FieldAbsoluteDate<KK> getCentralDate(final FieldAbsoluteDate<KK> interpolationDate) {
        final FieldAbsoluteDate<KK> central;
        final FieldAbsoluteDate<KK> minDate = cachedSamples.getEarliest().getDate();
        final FieldAbsoluteDate<KK> maxDate = cachedSamples.getLatest().getDate();

        if (interpolationDate.compareTo(minDate) < 0 &&
                FastMath.abs(interpolationDate.durationFrom(minDate)).getReal() <= extrapolationThreshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance before minDate
            central = minDate;
        }
        else if (interpolationDate.compareTo(maxDate) > 0 &&
                FastMath.abs(interpolationDate.durationFrom(maxDate)).getReal() <= extrapolationThreshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance after maxDate
            central = maxDate;
        }
        else {
            central = interpolationDate;
        }

        return central;
    }

    /**
     * Interpolate instance at given date.
     *
     * @param date interpolation date
     *
     * @return interpolated instance at given date
     */
    protected abstract T interpolate(FieldAbsoluteDate<KK> date);

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
}