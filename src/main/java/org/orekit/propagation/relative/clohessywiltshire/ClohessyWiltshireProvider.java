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

package org.orekit.propagation.relative.clohessywiltshire;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.relative.AbstractRelativeProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This additional state provider implements the Clohessy-Wiltshire equations of relative motion to propagate the relative
 * orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Clohessy-Wiltshire equations consider the target to be in a perfectly circular orbit around a central potential.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's QSW local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a small eccentricity, or other perturbations.
 * It is however a good and analytical (very fast) way to approximate the relative motion of two spacecraft.</p>
 * <p>The class {@link ClohessyWiltshireRendezVous} allows the analytical computation of 2-maneuver rendez-vous transfers.</p>
 *
 * @author Jérôme Tabeaud
 * @author Romain Cuvillon
 * @since 14.0
 */
public class ClohessyWiltshireProvider extends AbstractRelativeProvider {

    /**
     * Default additional equations name.
     */
    public static final String DEFAULT_ADDITIONAL_EQUATIONS_NAME = "Clohessy-Wiltshire chaser state in target's QSW LOF";

    /**
     * Local Orbital Frame.
     */
    public static final LOFType LOF_TYPE = LOFType.QSW;

    /**
     * Target's orbit mean motion. Cached value to avoid recomputing it at every iteration.
     */
    private final double meanMotion;

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit Target orbit. Should be circular for better results.
     */
    @DefaultDataContext
    public ClohessyWiltshireProvider(final Orbit targetOrbit) {
        super(targetOrbit, LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit         Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof Chaser PVT in the target's QSW local orbital frame.
     */
    public ClohessyWiltshireProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof) {
        super(targetOrbit, initialChaserPVTLof, DEFAULT_ADDITIONAL_EQUATIONS_NAME, LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof     Chaser PVT in the target's QSW local orbital frame.
     * @param additionalEquationsName Additional equations name.
     */
    public ClohessyWiltshireProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof, final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     *
     * @param targetOrbit      Target orbit. Should be circular for better results.
     * @param initialChaserPVT Chaser PVT in the target's QSW local orbital frame.
     * @param inputPVTFrame    Input frame for the initial chaser PVT.
     */
    public ClohessyWiltshireProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVT, final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVT, inputPVTFrame, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results.
     * @param initialChaserPVT        Chaser PVT in the inertial Frame.
     * @param inputPVTFrame           Input frame for the initial chaser PVT.
     * @param additionalEquationsName Additional equations name.
     */
    public ClohessyWiltshireProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVT, final Frame inputPVTFrame, final String additionalEquationsName) {
        // Copy input parameters
        super(targetOrbit, additionalEquationsName, LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
        // Transform PV from input frame to inertial frame of target Orbit.
        final Transform inputFrameToInertial = inputPVTFrame.getTransformTo(targetOrbit.getFrame(), targetOrbit.getDate());
        final TimeStampedPVCoordinates pvInertial = inputFrameToInertial.transformPVCoordinates(initialChaserPVT);
        // Transform and set PV from inertial to target's LOF.
        setInitialChaserPVTLof(LOF_TYPE.transformFromInertial(targetOrbit.getDate(), targetOrbit.getPVCoordinates()).transformPVCoordinates(pvInertial));
    }

    /**
     * Get the chaser state relative to a target, in target's QSW LOF.
     *
     * @param spacecraftState spacecraft state to which additional data should correspond
     * @return chaser cartesian state in target's QSW Local Orbital Frame.
     */
    @Override
    public double[] getAdditionalData(final SpacecraftState spacecraftState) {
        final ClohessyWiltshireMatrices cwMatrices = ClohessyWiltshireEquations.computeMatrices(spacecraftState.getDate().durationFrom(getInitialChaserPVTLof().getDate()), meanMotion);

        final TimeStampedPVCoordinates pvt = cwMatrices.transform(getInitialChaserPVTLof());

        return new double[] {
                pvt.getPosition().getX(), pvt.getPosition().getY(), pvt.getPosition().getZ(),
                pvt.getVelocity().getX(), pvt.getVelocity().getY(), pvt.getVelocity().getZ(),
        };
    }
}
