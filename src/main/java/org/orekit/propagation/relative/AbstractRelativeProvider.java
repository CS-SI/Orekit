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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Abstraction class for RelativeProvider.
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class AbstractRelativeProvider implements RelativeProvider {
    /**
     * Default additional equations name.
     */
    public static final String DEFAULT_ADDITIONAL_EQUATIONS_NAME = "Relative motion chaser state in target's LOF";

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
     * Builds a new RelativeProvider object from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit Target orbit.
     * @param lof         Local Orbital Frame.
     */
    @DefaultDataContext
    public AbstractRelativeProvider(final Orbit targetOrbit, final LOF lof) {
        this(targetOrbit, new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, Vector3D.ZERO, Vector3D.ZERO), lof);
    }

    /**
     * Builds a new RelativeProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit         Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof Chaser PVT in the target's local orbital frame.
     * @param lof                 Local Orbital Frame.
     */
    public AbstractRelativeProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof, final LOF lof) {
        this(targetOrbit, initialChaserPVTLof, DEFAULT_ADDITIONAL_EQUATIONS_NAME, lof);
    }

    /**
     * Builds a new RelativeProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof     Chaser PVT in the target's  local orbital frame.
     * @param additionalEquationsName Additional equations name.
     * @param lof                     Local Orbital Frame.
     */
    public AbstractRelativeProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof, final String additionalEquationsName, final LOF lof) {
        this.targetOrbit = targetOrbit;
        this.initialChaserPVTLof = initialChaserPVTLof;
        this.additionalEquationsName = additionalEquationsName;
        this.lof = lof;
    }


    /**
     * Builds a new RelativeProvider object from the target orbit and additionalEquationsName.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results.
     * @param additionalEquationsName Additional equations name.
     * @param lof                     Local Orbital Frame.
     */
    public AbstractRelativeProvider(final Orbit targetOrbit, final String additionalEquationsName, final LOF lof) {
        // Copy input parameters
        this.targetOrbit = targetOrbit;
        this.additionalEquationsName = additionalEquationsName;
        this.lof = lof;
    }

    /**
     * Sets the initial chaser PVT in target's LOF.
     *
     * @param initialChaserPVTLof Initial chaser PVT in target's LOF.
     */
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

    /**
     * Get the target orbit.
     *
     * @return target orbit.
     */
    @Override
    public Orbit getTargetOrbit() {
        return targetOrbit;
    }

    /**
     * Set target orbit.
     *
     * @param targetOrbit orbit of the target.
     */
    @Override
    public void setTargetOrbit(final Orbit targetOrbit) {
        this.targetOrbit = targetOrbit;
    }

    /**
     * Extracts the chaser's PVT from the given {@link SpacecraftState} and returns it. It is expressed in the target's LOF.
     *
     * @param spacecraftState Target's spacecraft state.
     * @return Chaser PVT in the target's LOF.
     */
    @Override
    public TimeStampedPVCoordinates extractChaserPVT(final SpacecraftState spacecraftState) {
        final double[] chaserState = spacecraftState.getAdditionalState(additionalEquationsName);
        return new TimeStampedPVCoordinates(spacecraftState.getDate(),
                new Vector3D(chaserState[0], chaserState[1], chaserState[2]),
                new Vector3D(chaserState[3], chaserState[4], chaserState[5]));
    }

    /**
     * Extracts the chaser's PVT from the given {@link SpacecraftState} and converts it to the desired output frame.
     *
     * @param spacecraftState Target's spacecraft state.
     * @param outputFrame     Desired output frame for the chaser PVT.
     * @return Chaser PVT in desired output frame.
     */
    @Override
    public TimeStampedPVCoordinates extractChaserPVT(final SpacecraftState spacecraftState, final Frame outputFrame) {
        // Extract chaser PVT in target's LOF
        final TimeStampedPVCoordinates chaserPVTLOF = extractChaserPVT(spacecraftState);
        // Transform PVT from target's LOF to reference inertial frame.
        final Transform lofToInertial = lof.transformFromInertial(spacecraftState.getDate(), spacecraftState.getPVCoordinates()).getInverse();
        final TimeStampedPVCoordinates pvInertial = lofToInertial.transformPVCoordinates(chaserPVTLOF);
        // Transform PVT from reference inertial frame to desired output frame.
        final Transform inertialToOutputFrame = spacecraftState.getFrame().getTransformTo(outputFrame, spacecraftState.getDate());
        return inertialToOutputFrame.transformPVCoordinates(pvInertial);
    }
}
