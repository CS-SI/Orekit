/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.util.stream.Stream;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;

/** Interpolator for {@link SP3Coordinate SP3 coordinates}.
 * <p>
 * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation points
 * (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
 * and numerical problems (including NaN appearing).
 *
 * @author Luc Maisonobe
 * @see HermiteInterpolator
 * @see SP3Coordinate
 * @since 12.0
 */
public class SP3CoordinateHermiteInterpolator extends AbstractTimeInterpolator<SP3Coordinate> {

    /** Flag for using velocity and clock rate. */
    private final boolean useRates;

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * </p>
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param useRates if true, use velocity and clock rates for interpolation
     */
    public SP3CoordinateHermiteInterpolator(final int interpolationPoints,
                                            final double extrapolationThreshold,
                                            final boolean useRates) {
        super(interpolationPoints, extrapolationThreshold);
        this.useRates = useRates;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation ensuring velocity remains the exact
     * derivative of position.
     * <p>
     * Note that even if first time derivatives (velocities) from sample can be ignored, the interpolated instance always
     * includes interpolated derivatives. This feature can be used explicitly to compute these derivatives when it would be
     * too complex to compute them from an analytical formula: just compute a few sample points from the explicit formula and
     * set the derivatives to zero in these sample points, then use interpolation to add derivatives consistent with the
     * positions.
     */
    @Override
    protected SP3Coordinate interpolate(final InterpolationData interpolationData) {

        // Get date
        final AbsoluteDate date = interpolationData.getInterpolationDate();

        // Convert sample to stream
        final Stream<SP3Coordinate> sample = interpolationData.getNeighborList().stream();

        // Set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Add sample points
        if (useRates) {
            // populate sample with position, clock, velocity and clock rate data
            sample.forEach(c -> {
                interpolator.addSamplePoint(c.getDate().durationFrom(date),
                                            new double[] {
                                                c.getPosition().getX(),
                                                c.getPosition().getY(),
                                                c.getPosition().getZ(),
                                                c.getClockCorrection(),
                                            },
                                            new double[] {
                                                c.getVelocity().getX(),
                                                c.getVelocity().getY(),
                                                c.getVelocity().getZ(),
                                                c.getClockRateChange(),
                                            });
            });
        } else {
            // populate sample with position and clock data, ignoring velocity and clock rate
            sample.forEach(c -> {
                interpolator.addSamplePoint(c.getDate().durationFrom(date),
                                            new double[] {
                                                c.getPosition().getX(),
                                                c.getPosition().getY(),
                                                c.getPosition().getZ(),
                                                c.getClockCorrection(),
                                            });
            });
        }

        // Interpolate
        final double[][] interpolated = interpolator.derivatives(0.0, 1);

        // Build a new interpolated instance
        return new SP3Coordinate(date,
                                 new Vector3D(interpolated[0][0], interpolated[0][1], interpolated[0][2]), null,
                                 new Vector3D(interpolated[1][0], interpolated[1][1], interpolated[1][2]), null,
                                 interpolated[0][3], Double.NaN,
                                 interpolated[1][3], Double.NaN,
                                 false, false, false, false);

    }

}
