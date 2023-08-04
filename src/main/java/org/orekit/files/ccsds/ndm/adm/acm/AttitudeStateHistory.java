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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.attitudes.TabulatedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Attitude state history.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeStateHistory implements AttitudeEphemerisFile.AttitudeEphemerisSegment<TimeStampedAngularCoordinates> {

    /** Metadata. */
    private final AttitudeStateHistoryMetadata metadata;

    /** Trajectory states. */
    private final List<AttitudeState> states;

    /** Simple constructor.
     * @param metadata metadata
     * @param states attitude states
     */
    public AttitudeStateHistory(final AttitudeStateHistoryMetadata metadata,
                                final List<AttitudeState> states) {
        this.metadata = metadata;
        this.states   = states;
    }

    /** Get metadata.
     * @return metadata
     */
    public AttitudeStateHistoryMetadata getMetadata() {
        return metadata;
    }

    /** Get the attitude states.
     * @return attitude states
     */
    public List<AttitudeState> getAttitudeStates() {
        return Collections.unmodifiableList(states);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getReferenceFrame() {
        final Frame frame = metadata.getEndpoints().getFrameA().asFrame();
        if (frame == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME,
                                      metadata.getEndpoints().getFrameA().getName());
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public String getInterpolationMethod() {
        return "linear";
    }

    /** {@inheritDoc} */
    @Override
    public AngularDerivativesFilter getAvailableDerivatives() {
        return states.get(0).getAvailableDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public BoundedAttitudeProvider getAttitudeProvider() {
        return new TabulatedProvider(getAngularCoordinates(), getInterpolationSamples(),
                                     getAvailableDerivatives(), getStart(), getStop(),
                                     getMetadata().getEndpoints());
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
    public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
        return states.stream().map(os -> os.toAngular(metadata.getEulerRotSeq())).collect(Collectors.toList());
    }

}
