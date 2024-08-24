/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.iirv;

import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ephemeris segment from an IIRV file. Each IIRV file (i.e. {@link IIRVMessage}) is defined as containing only one
 * {@link IIRVSegment}.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class IIRVSegment implements EphemerisFile.EphemerisSegment<TimeStampedPVCoordinates> {

    /** Gravitational parameter (m^3/s^2). */
    private final double mu;

    /** Number of samples to use in interpolation. */
    private final int interpolationSamples;

    /** Cartesian derivatives filter: IIRV always contains position & velocity data. */
    private final CartesianDerivativesFilter cartesianDerivativesFilter;

    /** Year of the first vector in the file (day of year, but not year itself, is embedded within an IIRV message). */
    private final int startYear;

    /** IIRV message consisting of sequential {@link IIRVVector} instances, sorted by {@link org.orekit.files.iirv.terms.SequenceNumberTerm}. */
    private final IIRVMessage iirvMessage;

    /**
     * Constructs a {@link IIRVSegment} instance  with default values.
     * <p>
     * Default gravitational parameter is {@link Constants#IERS96_EARTH_MU}. Default number of
     * interpolation samples is 7.
     *
     * @param startYear   Year associated with the beginning of the IIRV message
     * @param iirvMessage IIRV message consisting of sequential {@link IIRVVector} instances, sorted by
     *                    {@link org.orekit.files.iirv.terms.SequenceNumberTerm}.
     */
    public IIRVSegment(final int startYear, final IIRVMessage iirvMessage) {
        this(Constants.IERS96_EARTH_MU, 7, startYear, iirvMessage);
    }

    /**
     * Constructs a {@link IIRVSegment} instance.
     *
     * @param mu                   gravitational parameter (m^3/s^2)
     * @param interpolationSamples number of samples to use in interpolation
     * @param startYear            Year associated with the beginning of the IIRV message
     * @param iirvMessage          IIRV message consisting of sequential {@link IIRVVector} instances, sorted by
     *                             {@link org.orekit.files.iirv.terms.SequenceNumberTerm}.
     */
    public IIRVSegment(final double mu, final int interpolationSamples, final int startYear, final IIRVMessage iirvMessage) {
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.cartesianDerivativesFilter = CartesianDerivativesFilter.USE_PV;
        this.startYear = startYear;
        this.iirvMessage = iirvMessage;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return iirvMessage.getVectors().get(0).getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return interpolationSamples;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return cartesianDerivativesFilter;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return getCoordinates().get(0).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return getCoordinates().get(getCoordinates().size() - 1).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public List<TimeStampedPVCoordinates> getCoordinates() {
        int year = startYear;
        final List<IIRVVector> iirvVectors = iirvMessage.getVectors();

        final ArrayList<TimeStampedPVCoordinates> coordinates = new ArrayList<>();
        coordinates.add(iirvVectors.getFirst().getTimeStampedPVCoordinates(year));

        for (int i = 1; i < iirvVectors.size(); i++) {
            final IIRVVector prev = iirvVectors.get(i - 1);
            final IIRVVector next = iirvVectors.get(i);

            // Increase the year counter if the previous day is greater than the current day
            if (prev.getDayOfYear().value() > next.getDayOfYear().value()) {
                year++;
            }
            coordinates.add(next.getTimeStampedPVCoordinates(year));
        }
        return Collections.unmodifiableList(coordinates);
    }

    /**
     * Gets the IIRV message for this segment.
     *
     * @return IIRV message for this segment
     */
    public IIRVMessage getIIRVMessage() {
        return iirvMessage;
    }

    /**
     * Gets the start year for this segment.
     *
     * @return start year for this segment.
     */
    public int getStartYear() {
        return startYear;
    }
}
