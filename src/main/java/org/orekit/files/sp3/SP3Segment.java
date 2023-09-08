/* Copyright Luc Maisonobe
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;

/** One segment of an {@link SP3Ephemeris}.
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Segment implements EphemerisFile.EphemerisSegment<SP3Coordinate> {

    /** Standard gravitational parameter in m³ / s². */
    private final double mu;

    /** Reference frame. */
    private final Frame frame;

    /** Number of points to use for interpolation. */
    private final int interpolationSamples;

    /** Available derivatives. */
    private final CartesianDerivativesFilter filter;

    /** Ephemeris Data. */
    private final List<SP3Coordinate> coordinates;

    /** Simple constructor.
     * @param mu standard gravitational parameter to use for creating
     * {@link org.orekit.orbits.Orbit Orbits} from the ephemeris data.
     * @param frame reference frame
     * @param interpolationSamples number of points to use for interpolation
     * @param filter available derivatives
     */
    public SP3Segment(final double mu, final Frame frame,
                      final int interpolationSamples, final CartesianDerivativesFilter filter) {
        this.mu                   = mu;
        this.frame                = frame;
        this.interpolationSamples = interpolationSamples;
        this.filter               = filter;
        this.coordinates          = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return coordinates.get(0).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return coordinates.get(coordinates.size() - 1).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return interpolationSamples;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return filter;
    }

    /** {@inheritDoc} */
    @Override
    public List<SP3Coordinate> getCoordinates() {
        return Collections.unmodifiableList(this.coordinates);
    }

    /** Adds a new P/V coordinate.
     * @param coord the P/V coordinate of the satellite
     */
    public void addCoordinate(final SP3Coordinate coord) {
        coordinates.add(coord);
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getPropagator() {
        return EphemerisFile.EphemerisSegment.super.getPropagator();
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getPropagator(final AttitudeProvider attitudeProvider) {
        return EphemerisFile.EphemerisSegment.super.getPropagator(attitudeProvider);
    }

}
