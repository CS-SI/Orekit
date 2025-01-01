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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.util.MathArrays;

import java.util.List;

/**bHermite interpolator of time stamped clock offsets.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @see HermiteInterpolator
 * @see TimeInterpolator
 * @since 12.1
 */
public class FieldClockOffsetHermiteInterpolator<T extends CalculusFieldElement<T>>
    extends AbstractFieldTimeInterpolator<FieldClockOffset<T>, T> {

    /**
     * Constructor with default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s).
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * </p>
     * <p>
     * If the number of interpolation points or derivatives availability is not sufficient,
     * the rate and acceleration of interpolated offset will be silently set to 0 (i.e.
     * model will be constant or linear only).
     * </p>
     * @param interpolationPoints number of interpolation points
     */
    public FieldClockOffsetHermiteInterpolator(final int interpolationPoints) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * </p>
     * <p>
     * If the number of interpolation points or derivatives availability is not sufficient,
     * the rate and acceleration of interpolated offset will be silently set to 0 (i.e.
     * model will be constant or linear only).
     * </p>
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     */
    public FieldClockOffsetHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        super(interpolationPoints, extrapolationThreshold);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldClockOffset<T> interpolate(final InterpolationData interpolationData) {
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();

        // Fill interpolator with sample
        final FieldAbsoluteDate<T>      interpolationDate = interpolationData.getInterpolationDate();
        final List<FieldClockOffset<T>> neighborList      = interpolationData.getNeighborList();
        for (FieldClockOffset<T> value : neighborList) {
            final T   deltaT = value.getDate().durationFrom(interpolationDate);
            final T[] offset = MathArrays.buildArray(interpolationDate.getField(), 1);
            offset[0] = value.getOffset();
            if (value.getRate() == null || value.getRate().isNaN()) {
                // no clock rate for this entry
                interpolator.addSamplePoint(deltaT, offset);
            } else {
                // clock rate is available
                final T[] rate = MathArrays.buildArray(interpolationDate.getField(), 1);
                rate[0] = value.getRate();
                if (value.getAcceleration() == null || value.getAcceleration().isNaN()) {
                    // no clock acceleration for this entry
                    interpolator.addSamplePoint(deltaT, offset, rate);
                } else {
                    // clock acceleration is available
                    final T[] acceleration = MathArrays.buildArray(interpolationDate.getField(), 1);
                    acceleration[0] = value.getAcceleration();
                    interpolator.addSamplePoint(deltaT, offset, rate, acceleration);
                }
            }
        }

        final T[][] y = interpolator.derivatives(interpolationDate.getField().getZero(), 2);
        return new FieldClockOffset<>(interpolationDate, y[0][0], y[1][0], y[2][0]);

    }

}
