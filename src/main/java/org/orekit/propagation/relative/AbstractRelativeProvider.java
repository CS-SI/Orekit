/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative;

import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Abstraction class for RelativeProvider.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class AbstractRelativeProvider implements RelativeProvider {

    /**
     * Additional equations name.
     */
    private final String additionalEquationsName;

    /**
     * Initial chaser PVT in the target's LOF.
     */
    private TimeStampedPVCoordinates initialChaserPVTLof;

    /**
     * Target's orbit.
     */
    private Orbit targetOrbit;

    /**
     * Local Orbital Frame.
     */
    private final LOF lof;

    /**
     * Builds a new RelativeProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             Target orbit
     * @param initialChaserPVTLof     Chaser PVT in the target's  local orbital frame
     * @param additionalEquationsName Additional equations name
     * @param lof                     Local Orbital Frame
     */
    public AbstractRelativeProvider(final Orbit targetOrbit,
                                    final TimeStampedPVCoordinates initialChaserPVTLof,
                                    final String additionalEquationsName,
                                    final LOF lof) {
        this.targetOrbit             = targetOrbit;
        this.initialChaserPVTLof     = initialChaserPVTLof;
        this.additionalEquationsName = additionalEquationsName;
        this.lof                     = lof;
    }

    /**
     * Builds a new RelativeProvider object from the target orbit and additionalEquationsName.
     *
     * @param targetOrbit             Target orbit
     * @param additionalEquationsName Additional equations name
     * @param lof                     Local Orbital Frame
     */
    public AbstractRelativeProvider(final Orbit targetOrbit, final String additionalEquationsName, final LOF lof) {
        // Copy input parameters
        this.targetOrbit             = targetOrbit;
        this.additionalEquationsName = additionalEquationsName;
        this.lof                     = lof;
    }

    /**
     * Builds a new RelativeProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit             Target orbit
     * @param initialChaserPVT        Chaser PVT in the input Frame
     * @param inputPVTFrame           Input frame for the initial chaser PVT
     * @param additionalEquationsName Additional equations name
     * @param lof                     Local Orbital Frame
     */
    public AbstractRelativeProvider(final Orbit targetOrbit,
                                    final TimeStampedPVCoordinates initialChaserPVT,
                                    final Frame inputPVTFrame,
                                    final String additionalEquationsName,
                                    final LOF lof) {
        // Copy input parameters
        this.targetOrbit             = targetOrbit;
        this.additionalEquationsName = additionalEquationsName;
        this.lof                     = lof;

        // Transform PV from input frame to inertial frame of target Orbit.
        final Transform inputFrameToInertial = inputPVTFrame.getTransformTo(targetOrbit.getFrame(),
                                                                            targetOrbit.getDate());
        final TimeStampedPVCoordinates pvInertial = inputFrameToInertial.transformPVCoordinates(initialChaserPVT);

        // Transform and set PV from inertial to target's LOF.
        setInitialChaserPVTLof(getLof().transformFromInertial(targetOrbit.getDate(), targetOrbit.getPVCoordinates())
                                       .transformPVCoordinates(pvInertial));
    }

    /** {@inheritDoc}. */
    @Override
    public LOF getLof() {
        return lof;
    }

    /** {@inheritDoc}. */
    @Override
    public void setInitialChaserPVTLof(final TimeStampedPVCoordinates initialChaserPVTLof) {
        this.initialChaserPVTLof = initialChaserPVTLof;
    }

    @Override
    public TimeStampedPVCoordinates getInitialChaserPVTLof() {
        return initialChaserPVTLof;
    }

    @Override
    public String getName() {
        return additionalEquationsName;
    }

    /** {@inheritDoc}. */
    @Override
    public Orbit getTargetOrbit() {
        return targetOrbit;
    }

    /** {@inheritDoc}. */
    @Override
    public void setTargetOrbit(final Orbit targetOrbit) {
        this.targetOrbit = targetOrbit;
    }
}
