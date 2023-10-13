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

import org.orekit.frames.Frame;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.TimeStampedAngularCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for attitude interpolation.
 * <p>
 * The type of interpolation used is defined by given time stamped angular coordinates interpolator at construction.
 *
 * @author Vincent Cucchietti
 * @see TimeStampedAngularCoordinates
 * @see TimeInterpolator
 */
public class AttitudeInterpolator extends AbstractTimeInterpolator<Attitude> {

    /** Reference frame from which attitude is defined. */
    private final Frame referenceFrame;

    /** Time stamped angular coordinates interpolator. */
    private final TimeInterpolator<TimeStampedAngularCoordinates> interpolator;

    /**
     * Constructor.
     *
     * @param referenceFrame reference frame from which attitude is defined
     * @param interpolator time stamped angular coordinates interpolator
     */
    public AttitudeInterpolator(final Frame referenceFrame,
                                final TimeInterpolator<TimeStampedAngularCoordinates> interpolator) {
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
    public TimeInterpolator<TimeStampedAngularCoordinates> getAngularInterpolator() {
        return interpolator;
    }

    /** {@inheritDoc} */
    @Override
    protected Attitude interpolate(final InterpolationData interpolationData) {

        // Convert sample to stream
        final Stream<Attitude> sample = interpolationData.getNeighborList().stream();

        // Express all attitudes in the same reference frame
        final Stream<Attitude> consistentSample =
                sample.map(attitude -> attitude.withReferenceFrame(referenceFrame));

        // Map time stamped angular coordinates
        final List<TimeStampedAngularCoordinates> angularSample =
                consistentSample.map(Attitude::getOrientation).collect(Collectors.toList());

        // Interpolate
        final TimeStampedAngularCoordinates interpolated = interpolator.interpolate(interpolationData.getInterpolationDate(),
                                                                                    angularSample);

        return new Attitude(referenceFrame, interpolated);
    }
}
