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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for attitude interpolation.
 * <p>
 * The type of interpolation used is defined by given time stamped angular coordinates interpolator at construction.
 *
 * @param <KK> type of the field element
 *
 * @author Vincent Cucchietti
 * @see TimeStampedFieldAngularCoordinates
 * @see FieldTimeInterpolator
 */
public class FieldAttitudeInterpolator<KK extends CalculusFieldElement<KK>>
        extends AbstractFieldTimeInterpolator<FieldAttitude<KK>, KK> {

    /** Reference frame from which attitude is defined. */
    private final Frame referenceFrame;

    /** Time stamped angular coordinates interpolator. */
    private final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<KK>, KK> interpolator;

    /**
     * Constructor.
     *
     * @param referenceFrame reference frame from which attitude is defined
     * @param interpolator time stamped angular coordinates interpolator
     */
    public FieldAttitudeInterpolator(final Frame referenceFrame,
                                     final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<KK>, KK> interpolator) {
        super(interpolator.getNbInterpolationPoints(), interpolator.getExtrapolationThreshold());
        this.referenceFrame = referenceFrame;
        this.interpolator   = interpolator;
    }

    /** Get reference frame from which attitude is defined.
     * @return reference frame from which attitude is defined
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** Get time stamped angular coordinates interpolator.
     * @return time stamped angular coordinates interpolator
     */
    public FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<KK>, KK> getAngularInterpolator() {
        return interpolator;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldAttitude<KK> interpolate(final InterpolationData interpolationData) {

        // Convert sample to stream
        final Stream<FieldAttitude<KK>> sample = interpolationData.getNeighborList().stream();

        // Express all attitudes in the same reference frame
        final Stream<FieldAttitude<KK>> consistentSample =
                sample.map(attitude -> attitude.withReferenceFrame(referenceFrame));

        // Map time stamped angular coordinates
        final List<TimeStampedFieldAngularCoordinates<KK>> angularSample =
                consistentSample.map(FieldAttitude::getOrientation).collect(Collectors.toList());

        // Interpolate
        final TimeStampedFieldAngularCoordinates<KK> interpolated =
                interpolator.interpolate(interpolationData.getInterpolationDate(), angularSample);

        return new FieldAttitude<>(referenceFrame, interpolated);
    }
}
