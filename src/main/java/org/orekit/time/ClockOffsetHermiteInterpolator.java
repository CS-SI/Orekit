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

import org.hipparchus.analysis.interpolation.HermiteInterpolator;

import java.util.List;

/**bHermite interpolator of time stamped clock offsets.
 * @author Luc Maisonobe
 * @see HermiteInterpolator
 * @see TimeInterpolator
 * @since 12.1
 */
public class ClockOffsetHermiteInterpolator extends AbstractTimeInterpolator<ClockOffset> {

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
    public ClockOffsetHermiteInterpolator(final int interpolationPoints) {
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
    public ClockOffsetHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        super(interpolationPoints, extrapolationThreshold);
    }

    /** {@inheritDoc} */
    @Override
    protected ClockOffset interpolate(final InterpolationData interpolationData) {
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Fill interpolator with sample
        final AbsoluteDate                         interpolationDate = interpolationData.getInterpolationDate();
        final List<ClockOffset> neighborList      = interpolationData.getNeighborList();
        for (ClockOffset value : neighborList) {
            final double deltaT = value.getDate().durationFrom(interpolationDate);
            final double[] offset = new double[] { value.getOffset() };
            if (Double.isNaN(value.getRate())) {
                // no clock rate for this entry
                interpolator.addSamplePoint(deltaT, offset);
            } else {
                // clock rate is available
                final double[] rate = new double[] { value.getRate() };
                if (Double.isNaN(value.getAcceleration())) {
                    // no clock acceleration for this entry
                    interpolator.addSamplePoint(deltaT, offset, rate);
                } else {
                    // clock acceleration is available
                    final double[] acceleration = new double[] { value.getAcceleration() };
                    interpolator.addSamplePoint(deltaT, offset, rate, acceleration);
                }
            }
        }

        final double[][] y = interpolator.derivatives(0, 2);
        return new ClockOffset(interpolationDate, y[0][0], y[1][0], y[2][0]);

    }

}
