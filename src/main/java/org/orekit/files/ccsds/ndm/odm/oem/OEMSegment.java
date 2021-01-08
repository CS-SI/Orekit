/* Copyright 2002-2020 CS GROUP
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** The Ephemerides Blocks class contain metadata, the list of ephemerides data
 * lines and optional covariance matrices (and their metadata). The reason
 * for which the ephemerides have been separated into blocks is that the
 * ephemerides of two different blocks are not suited for interpolation.
 * @author sports
 */
public class OEMSegment extends NDMSegment<OEMMetadata, OEMData> implements EphemerisFile.EphemerisSegment {

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param metadata segment metadata
     * @param data segment data
     * @param mu gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    public OEMSegment(final OEMMetadata metadata, final OEMData data, final double mu) {
        super(metadata, data);
        this.mu = mu;
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

    /** {@inheritDoc}
     * <p>
     * This method throws an exception if the gravitational coefficient has not been set properly
     * </p>
     */
    @Override
    public double getMu() {
        if (Double.isNaN(mu)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public String getFrameCenterString() {
        return getMetadata().getCenterName();
    }

    /** {@inheritDoc} */
    @Override
    public String getFrameString() {
        return getMetadata().getFrameString();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return getMetadata().getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getInertialFrame() {
        final Frame frame = getFrame();
        if (frame.isPseudoInertial()) {
            return frame;
        }
        return getMetadata().getDataContext().getFrames().getGCRF();
    }

    /** {@inheritDoc} */
    @Override
    public String getTimeScaleString() {
        return getMetadata().getTimeSystem().toString();
    }

    /** {@inheritDoc} */
    @Override
    public TimeScale getTimeScale() {
        return getMetadata().getTimeScale();
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
