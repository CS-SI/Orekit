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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for orbit interpolator.
 *
 * @param <KK> type of the field element
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractFieldOrbitInterpolator<KK extends CalculusFieldElement<KK>>
        extends AbstractFieldTimeInterpolator<FieldOrbit<KK>, KK> {

    /** Output inertial frame. */
    private final Frame outputInertialFrame;

    /**
     * Constructor.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputInertialFrame output inertial frame
     */
    public AbstractFieldOrbitInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                          final Frame outputInertialFrame) {
        super(interpolationPoints, extrapolationThreshold);
        checkFrameIsInertial(outputInertialFrame);
        this.outputInertialFrame = outputInertialFrame;
    }

    /** {@inheritDoc}. */
    @Override
    public FieldOrbit<KK> interpolate(final FieldAbsoluteDate<KK> interpolationDate,
                                      final Collection<FieldOrbit<KK>> sample) {

        // Convert to orbit list
        final List<Orbit> orbits = sample.stream().map(FieldOrbit::toOrbit).collect(Collectors.toList());

        // Check orbits consistency
        AbstractOrbitInterpolator.checkOrbitsConsistency(orbits);

        return super.interpolate(interpolationDate, sample);
    }

    /** Get output inertial frame.
     * @return output inertial frame
     */
    public Frame getOutputInertialFrame() {
        return outputInertialFrame;
    }

    /**
     * Check if given frame is pseudo inertial and throw an error otherwise.
     *
     * @param frame frame to check
     *
     * @throws OrekitIllegalArgumentException if given frame is not pseudo inertial
     */
    private void checkFrameIsInertial(final Frame frame) {
        if (!frame.isPseudoInertial()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, frame.getName());
        }
    }
}
