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
package org.orekit.attitudes;

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinatesHermiteInterpolator;

/**
 * This class handles an attitude provider interpolating from a predefined table.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @see TabulatedLofOffset
 * @since 6.1
 */
public class TabulatedProvider implements BoundedAttitudeProvider {

    /** Cached attitude table. */
    private final transient ImmutableTimeStampedCache<? extends TimeStampedAngularCoordinates> table;

    /** Filter for derivatives from the sample to use in interpolation. */
    private final AngularDerivativesFilter filter;

    /** First date of the range. */
    private final AbsoluteDate minDate;

    /** Last date of the range. */
    private final AbsoluteDate maxDate;

    /** Builder for filtered attitudes. */
    private final AttitudeBuilder builder;

    /** Creates new instance.
     * <p>
     * This constructor uses the first and last point samples as the min and max dates.
     * </p>
     * @param referenceFrame reference frame for tabulated attitudes
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param filter filter for derivatives from the sample to use in interpolation
     * @see #TabulatedProvider(List, int, AngularDerivativesFilter, AbsoluteDate, AbsoluteDate, AttitudeBuilder)
     */
    public TabulatedProvider(final Frame referenceFrame, final List<? extends TimeStampedAngularCoordinates> table,
                             final int n, final AngularDerivativesFilter filter) {
        this(table, n, filter, table.get(0).getDate(), table.get(table.size() - 1).getDate(),
             new FixedFrameBuilder(referenceFrame));
    }

    /** Creates new instance.
     * @param table tabulated attitudes
     * @param n number of attitude to use for interpolation
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param minDate min date to use
     * @param maxDate max date to use
     * @param builder builder to use
     * @since 11.0
     */
    public TabulatedProvider(final List<? extends TimeStampedAngularCoordinates> table,
                             final int n, final AngularDerivativesFilter filter,
                             final AbsoluteDate minDate, final AbsoluteDate maxDate,
                             final AttitudeBuilder builder) {
        this.table          = new ImmutableTimeStampedCache<TimeStampedAngularCoordinates>(n, table);
        this.filter         = filter;
        this.minDate        = minDate;
        this.maxDate        = maxDate;
        this.builder        = builder;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> sample = table.getNeighbors(date).collect(Collectors.toList());

        // create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), filter);

        // interpolate
        final TimeStampedAngularCoordinates interpolated = interpolator.interpolate(date, sample);

        // build the attitude
        return builder.build(frame, pvProv, interpolated);

    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date,
                                                                        final Frame frame) {

        // get attitudes sample on which interpolation will be performed
        final List<TimeStampedFieldAngularCoordinates<T>> sample =
                        table.
                        getNeighbors(date.toAbsoluteDate()).
                        map(ac -> new TimeStampedFieldAngularCoordinates<>(date.getField(), ac)).
                        collect(Collectors.toList());

        // create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<T>, T> interpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), filter);

        // interpolate
        final TimeStampedFieldAngularCoordinates<T> interpolated = interpolator.interpolate(date, sample);

        // build the attitude
        return builder.build(frame, pvProv, interpolated);

    }

    /** {@inheritDoc} */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

}
