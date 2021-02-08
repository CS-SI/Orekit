/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.List;

import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** The Ephemerides Blocks class contain metadata, the list of ephemerides data
 * lines and optional covariance matrices (and their metadata). The reason
 * for which the ephemerides have been separated into blocks is that the
 * ephemerides of two different blocks are not suited for interpolation.
 * @author sports
 */
public class OEMSegment extends Segment<OEMMetadata, OEMData> implements EphemerisFile.EphemerisSegment {

    /** Simple constructor.
     * @param metadata segment metadata
     * @param data segment data
     */
    public OEMSegment(final OEMMetadata metadata, final OEMData data) {
        super(metadata, data);
    }

    /** {@inheritDoc} */
    @Override
    public List<TimeStampedPVCoordinates> getCoordinates() {
        return getData().getCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return getData().getAvailableDerivatives();
    }

    /** Get an unmodifiable view of Covariance Matrices.
     * @return unmodifiable view of Covariance Matrices
     */
    public List<CovarianceMatrix> getCovarianceMatrices() {
        return getData().getCovarianceMatrices();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return getMetadata().getFrame();
    }

    /** {@inheritDoc}
     * <p>
     * This implementation returns {@link #getFrame() defining frame}
     * if it is {@link Frame#isPseudoInertial() pseudo-inertial}, or
     * its closest {@link Frame#getParent() ancestor} that is
     * pseudo-inertial.
     * </p>
     */
    @Override
    public Frame getInertialFrame() {
        Frame frame = getFrame();
        while (!frame.isPseudoInertial()) {
            frame = frame.getParent();
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        // useable start time overrides start time if it is set
        final AbsoluteDate start = getMetadata().getUseableStartTime();
        if (start != null) {
            return start;
        } else {
            return getMetadata().getStartTime();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        // useable stop time overrides stop time if it is set
        final AbsoluteDate stop = getMetadata().getUseableStopTime();
        if (stop != null) {
            return stop;
        } else {
            return getMetadata().getStopTime();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        // From the standard it is not entirely clear how to interpret the degree.
        return getMetadata().getInterpolationDegree() + 1;
    }

}
