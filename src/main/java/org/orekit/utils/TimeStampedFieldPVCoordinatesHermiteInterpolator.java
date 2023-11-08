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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;

import java.util.stream.Stream;

/**
 * Class using a Hermite interpolator to interpolate time stamped position-velocity-acceleration coordinates.
 * <p>
 * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation points
 * (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
 * and numerical problems (including NaN appearing).
 *
 * @param <KK> type of the field element
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see FieldHermiteInterpolator
 * @see TimeStampedFieldPVCoordinates
 */
public class TimeStampedFieldPVCoordinatesHermiteInterpolator<KK extends CalculusFieldElement<KK>>
        extends AbstractFieldTimeInterpolator<TimeStampedFieldPVCoordinates<KK>, KK> {

    /** Filter for derivatives from the sample to use in interpolation. */
    private final CartesianDerivativesFilter filter;

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     */
    public TimeStampedFieldPVCoordinatesHermiteInterpolator() {
        this(DEFAULT_INTERPOLATION_POINTS);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and both time derivatives for attitude interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     */
    public TimeStampedFieldPVCoordinatesHermiteInterpolator(final int interpolationPoints) {

        this(interpolationPoints, CartesianDerivativesFilter.USE_PVA);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TimeStampedFieldPVCoordinatesHermiteInterpolator(final int interpolationPoints,
                                                            final CartesianDerivativesFilter filter) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, filter);
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
     * @param filter filter for derivatives from the sample to use in interpolation
     */
    public TimeStampedFieldPVCoordinatesHermiteInterpolator(final int interpolationPoints,
                                                            final double extrapolationThreshold,
                                                            final CartesianDerivativesFilter filter) {
        super(interpolationPoints, extrapolationThreshold);
        this.filter = filter;
    }

    /** filter for derivatives from the sample to use in interpolation.
     * @return filter for derivatives from the sample to use in interpolation
     */
    public CartesianDerivativesFilter getFilter() {
        return filter;
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
    protected TimeStampedFieldPVCoordinates<KK> interpolate(final InterpolationData interpolationData) {

        // Get interpolation date
        final FieldAbsoluteDate<KK> interpolationDate = interpolationData.getInterpolationDate();

        // Convert sample to stream
        final Stream<TimeStampedFieldPVCoordinates<KK>> sample = interpolationData.getNeighborList().stream();

        // Set up an interpolator taking derivatives into account
        final FieldHermiteInterpolator<KK> interpolator = new FieldHermiteInterpolator<>();

        // Add sample points
        switch (filter) {
            case USE_P:
                // populate sample with position data, ignoring velocity
                sample.forEach(pv -> {
                    final FieldVector3D<KK> position = pv.getPosition();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(interpolationDate),
                                                position.toArray());
                });
                break;
            case USE_PV:
                // populate sample with position and velocity data
                sample.forEach(pv -> {
                    final FieldVector3D<KK> position = pv.getPosition();
                    final FieldVector3D<KK> velocity = pv.getVelocity();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(interpolationDate),
                                                position.toArray(), velocity.toArray());
                });
                break;
            case USE_PVA:
                // populate sample with position, velocity and acceleration data
                sample.forEach(pv -> {
                    final FieldVector3D<KK> position     = pv.getPosition();
                    final FieldVector3D<KK> velocity     = pv.getVelocity();
                    final FieldVector3D<KK> acceleration = pv.getAcceleration();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(interpolationDate),
                                                position.toArray(), velocity.toArray(), acceleration.toArray());
                });
                break;
            default:
                // this should never happen
                throw new OrekitInternalError(null);
        }

        // Interpolate
        final KK[][] pva = interpolator.derivatives(interpolationDate.getField().getZero(), 2);

        // Build a new interpolated instance
        return new TimeStampedFieldPVCoordinates<>(interpolationDate, new FieldVector3D<>(pva[0]), new FieldVector3D<>(pva[1]),
                                                   new FieldVector3D<>(pva[2]));
    }
}
