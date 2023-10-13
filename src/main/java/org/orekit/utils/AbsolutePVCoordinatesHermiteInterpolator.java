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
package org.orekit.utils;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;

import java.util.List;

/**
 * Class using a Hermite interpolator to interpolate absolute position-velocity-acceleration coordinates.
 * <p>
 * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation points
 * (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
 * and numerical problems (including NaN appearing).
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see HermiteInterpolator
 * @see AbsolutePVCoordinates
 */
public class AbsolutePVCoordinatesHermiteInterpolator extends AbstractTimeInterpolator<AbsolutePVCoordinates> {

    /** Filter for derivatives from the sample to use in interpolation. */
    private final CartesianDerivativesFilter filter;

    /** Output frame for the interpolated instance. */
    private final Frame outputFrame;

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param outputFrame frame for the interpolated instance
     */
    public AbsolutePVCoordinatesHermiteInterpolator(final Frame outputFrame) {
        this(DEFAULT_INTERPOLATION_POINTS, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputFrame,
             CartesianDerivativesFilter.USE_PVA);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param outputFrame frame for the interpolated instance
     */
    public AbsolutePVCoordinatesHermiteInterpolator(final int interpolationPoints, final Frame outputFrame) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputFrame, CartesianDerivativesFilter.USE_PVA);
    }

    /**
     * Constructor with default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s).
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param outputFrame frame for the interpolated instance
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public AbsolutePVCoordinatesHermiteInterpolator(final int interpolationPoints, final Frame outputFrame,
                                                    final CartesianDerivativesFilter filter) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputFrame, filter);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputFrame frame for the interpolated instance
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public AbsolutePVCoordinatesHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                                    final Frame outputFrame, final CartesianDerivativesFilter filter) {
        super(interpolationPoints, extrapolationThreshold);
        this.outputFrame = outputFrame;
        this.filter      = filter;
    }

    /** Get the filter for derivatives from the sample to use in interpolation.
     * @return filter for derivatives from the sample to use in interpolation.
     */
    public CartesianDerivativesFilter getFilter() {
        return filter;
    }

    /** Get output frame for the interpolated instance.
     * @return output frame for the interpolated instance
     */
    public Frame getOutputFrame() {
        return outputFrame;
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
    protected AbsolutePVCoordinates interpolate(final InterpolationData interpolationData) {

        // Get date
        final AbsoluteDate date = interpolationData.getInterpolationDate();

        // Get sample
        final List<AbsolutePVCoordinates> sample = interpolationData.getNeighborList();

        // Set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Add sample points
        switch (filter) {
            case USE_P:
                // Populate sample with position data, ignoring velocity
                sample.forEach(pv -> {
                    final Vector3D position = pv.getPosition();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray());
                });
                break;
            case USE_PV:
                // Populate sample with position and velocity data
                sample.forEach(pv -> {
                    final Vector3D position = pv.getPosition();
                    final Vector3D velocity = pv.getVelocity();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray(), velocity.toArray());
                });
                break;
            case USE_PVA:
                // Populate sample with position, velocity and acceleration data
                sample.forEach(pv -> {
                    final Vector3D position     = pv.getPosition();
                    final Vector3D velocity     = pv.getVelocity();
                    final Vector3D acceleration = pv.getAcceleration();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray(), velocity.toArray(), acceleration.toArray());
                });
                break;
            default:
                // this should never happen
                throw new OrekitInternalError(null);
        }

        // interpolate
        final double[][] pva = interpolator.derivatives(0.0, 2);

        // build a new interpolated instance
        return new AbsolutePVCoordinates(outputFrame, date, new Vector3D(pva[0]), new Vector3D(pva[1]),
                                         new Vector3D(pva[2]));
    }
}
