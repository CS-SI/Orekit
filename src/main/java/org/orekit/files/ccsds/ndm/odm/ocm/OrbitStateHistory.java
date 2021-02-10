/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Orbit state history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitStateHistory extends CommentsContainer
    implements EphemerisFile.EphemerisSegment<TimeStampedPVCoordinates> {

    /** Metadata. */
    private final OrbitStateHistoryMetadata metadata;

    /** Orbital states. */
    private final List<OrbitState> states;

    /** Gravitational parameter in m³/s². */
    private final double mu;

    /** Simple constructor.
     * @param metadata metadata
     * @param states orbital states
     * @param mu gravitational parameter in m³/s²
     */
    OrbitStateHistory(final OrbitStateHistoryMetadata metadata,
                      final List<OrbitState> states,
                      final double mu) {
        this.metadata = metadata;
        this.states   = states;
        this.mu       = mu;
    }

    /** Get metadata.
     * @return metadata
     */
    public OrbitStateHistoryMetadata getMetadata() {
        return metadata;
    }

    /** Get the orbital states.
     * @return orbital states
     */
    public List<OrbitState> getOrbitalStates() {
        return Collections.unmodifiableList(states);
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return metadata.getOrbRefFrame();
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
        return states.stream().map(os -> os.toCartesian(mu)).collect(Collectors.toList());
    }

}
