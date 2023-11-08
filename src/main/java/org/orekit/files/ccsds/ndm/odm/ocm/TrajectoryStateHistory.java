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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Trajectory state history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TrajectoryStateHistory implements EphemerisFile.EphemerisSegment<TimeStampedPVCoordinates> {

    /** Metadata. */
    private final TrajectoryStateHistoryMetadata metadata;

    /** Trajectory states. */
    private final List<TrajectoryState> states;

    /** Gravitational parameter in m³/s². */
    private final double mu;

    /** Central body.
     * @since 12.0
     */
    private final OneAxisEllipsoid body;

    /** Simple constructor.
     * @param metadata metadata
     * @param states orbital states
     * @param body central body (may be null if {@link TrajectoryStateHistoryMetadata#getTrajType() type}
     * is <em>not</em> {@link OrbitElementsType#GEODETIC})
     * @param mu gravitational parameter in m³/s²
     */
    public TrajectoryStateHistory(final TrajectoryStateHistoryMetadata metadata,
                                  final List<TrajectoryState> states,
                                  final OneAxisEllipsoid body, final double mu) {
        this.metadata = metadata;
        this.states   = states;
        this.mu       = mu;
        this.body     = body;
    }

    /** Get metadata.
     * @return metadata
     */
    public TrajectoryStateHistoryMetadata getMetadata() {
        return metadata;
    }

    /** Get the trajectory states.
     * @return trajectory states
     */
    public List<TrajectoryState> getTrajectoryStates() {
        return Collections.unmodifiableList(states);
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** Get central body.
     * @return central body
     * @since 12.0
     */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        final Frame frame = metadata.getTrajReferenceFrame().asFrame();
        if (frame == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME,
                                      metadata.getTrajReferenceFrame().getName());
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return metadata.getInterpolationDegree() + 1;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return states.get(0).getAvailableDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return states.get(0).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return states.get(states.size() - 1).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public List<TimeStampedPVCoordinates> getCoordinates() {
        return states.stream().map(os -> os.toCartesian(body, mu)).collect(Collectors.toList());
    }

}
