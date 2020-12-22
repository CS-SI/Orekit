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
package org.orekit.files.general;

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.RealFieldElement;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegment;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegmentData;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegmentMetadata;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * An {@link AttitudeProvider} based on an {@link AttitudeEphemerisSegment}.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class EphemerisSegmentAttitudeProvider implements BoundedAttitudeProvider {

    /** Reference frame from which attitude is computed. */
    private final Frame referenceFrame;

    /** Start date. */
    private final AbsoluteDate start;

    /** End date. */
    private final AbsoluteDate end;

    /** Derivation order. */
    private final AngularDerivativesFilter filter;

    /** Cached attitude table. */
    private final transient ImmutableTimeStampedCache<TimeStampedAngularCoordinates> table;

    /**
     * Constructor.
     * @param metadata attitude metadata
     * @param data attitude data
     */
    public EphemerisSegmentAttitudeProvider(final AttitudeEphemerisSegmentMetadata metadata,
                                            final AttitudeEphemerisSegmentData data) {
        this.referenceFrame = metadata.getReferenceFrame();
        this.start          = metadata.getStart();
        this.end            = metadata.getStop();
        this.filter         = data.getAvailableDerivatives();
        this.table          =  new ImmutableTimeStampedCache<TimeStampedAngularCoordinates>(metadata.getInterpolationSamples(),
                                                                                            data.getAngularCoordinates());
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) {

        // Get attitudes sample on which interpolation will be performed
        final List<TimeStampedAngularCoordinates> attitudeSample = table.getNeighbors(date).collect(Collectors.toList());

        // Interpolate attitude data
        final TimeStampedAngularCoordinates interpolatedAttitude =
                TimeStampedAngularCoordinates.interpolate(date, filter, attitudeSample);

        // Build the interpolated attitude
        return new Attitude(referenceFrame, interpolatedAttitude);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {

        // Get attitudes sample on which interpolation will be performed
        final List<TimeStampedFieldAngularCoordinates<T>> attitudeSample = table.getNeighbors(date.toAbsoluteDate()).
                        map(ac -> new TimeStampedFieldAngularCoordinates<>(date.getField(), ac)).
                        collect(Collectors.toList());

        // Interpolate attitude data
        final TimeStampedFieldAngularCoordinates<T> interpolatedAttitude =
                TimeStampedFieldAngularCoordinates.interpolate(date, filter, attitudeSample);

        // Build the interpolated attitude
        return new FieldAttitude<>(referenceFrame, interpolatedAttitude);

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return start;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return end;
    }

}
