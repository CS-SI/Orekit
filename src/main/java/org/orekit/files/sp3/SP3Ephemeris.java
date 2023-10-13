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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;

/** Single satellite ephemeris from an {@link SP3 SP3} file.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Ephemeris implements EphemerisFile.SatelliteEphemeris<SP3Coordinate, SP3Segment> {

    /** Satellite ID. */
    private final String id;

    /** Standard gravitational parameter in m³ / s². */
    private final double mu;

    /** Reference frame. */
    private final Frame frame;

    /** Number of points to use for interpolation. */
    private final int interpolationSamples;

    /** Available derivatives. */
    private final CartesianDerivativesFilter filter;

    /** Segments. */
    private final List<SP3Segment> segments;

    /** Create an ephemeris for a single satellite.
     * @param id of the satellite.
     * @param mu standard gravitational parameter to use for creating
     * {@link org.orekit.orbits.Orbit Orbits} from the ephemeris data.
     * @param frame reference frame
     * @param interpolationSamples number of points to use for interpolation
     * @param filter available derivatives
     */
    public SP3Ephemeris(final String id, final double mu, final Frame frame,
                        final int interpolationSamples, final CartesianDerivativesFilter filter) {
        this.id                   = id;
        this.mu                   = mu;
        this.frame                = frame;
        this.interpolationSamples = interpolationSamples;
        this.filter               = filter;
        this.segments             = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return this.id;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public List<SP3Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return segments.isEmpty() ? null : segments.get(0).getStart();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return segments.isEmpty() ? null : segments.get(segments.size() - 1).getStop();
    }

    /** Get the reference frame.
     * @return reference frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the number of points to use for interpolation.
     * @return number of points to use for interpolation
     */
    public int getInterpolationSamples() {
        return interpolationSamples;
    }

    /** Get the available derivatives.
     * @return available derivatives
     */
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return filter;
    }

    /** Adds a new P/V coordinate.
     * @param coord the P/V coordinate of the satellite
     * @param maxGap maximum gap between segments
     */
    public void addCoordinate(final SP3Coordinate coord, final double maxGap) {
        final AbsoluteDate lastDate = getStop();
        final SP3Segment segment;
        if (lastDate == null || coord.getDate().durationFrom(lastDate) > maxGap) {
            // we need to create a new segment
            segment = new SP3Segment(mu, frame,  interpolationSamples, filter);
            segments.add(segment);
        } else {
            segment = segments.get(segments.size() - 1);
        }
        segment.addCoordinate(coord);
    }

}
