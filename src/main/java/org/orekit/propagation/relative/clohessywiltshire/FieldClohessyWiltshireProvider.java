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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.relative.FieldAbstractRelativeProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This additional state provider implements the Clohessy-Wiltshire equations of relative motion to propagate the
 * relative
 * orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Clohessy-Wiltshire equations consider the target to be in a perfectly circular orbit around a central
 * potential.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's QSW
 * local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a small eccentricity, or other
 * perturbations.
 * It is however a good and analytical (very fast) way to approximate the relative motion of two spacecraft.</p>
 * <p>The class {@link FieldClohessyWiltshireRendezVous} allows the analytical computation of 2-maneuver rendez-vous
 * transfers.</p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldClohessyWiltshireProvider<T extends CalculusFieldElement<T>>
                extends FieldAbstractRelativeProvider<T> {


    /**
     * Target's orbit mean motion. Cached value to avoid recomputing it at every iteration.
     */
    private final T meanMotion;

    /**
     * Builds a new provider object from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit Field target orbit. Should be circular for better results
     */
    @DefaultDataContext
    public FieldClohessyWiltshireProvider(final FieldOrbit<T> targetOrbit) {
        this(targetOrbit, new TimeStampedFieldPVCoordinates<>(targetOrbit.getA().getField(),
                                                              new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                           Vector3D.ZERO,
                                                                                           Vector3D.ZERO)));
    }

    /**
     * Builds a new provider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit         Field orbit of the target. Should be circular for better results
     * @param initialChaserPVTLof Field PVT of the chaser in the Local Orbital Frame of the target
     */
    public FieldClohessyWiltshireProvider(final FieldOrbit<T> targetOrbit,
                                          final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof) {
        this(targetOrbit, initialChaserPVTLof,
             ClohessyWiltshireProvider.DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results
     * @param initialChaserPVTLof     Chaser PVT in given frame
     * @param additionalEquationsName Additional equations name
     */
    public FieldClohessyWiltshireProvider(final FieldOrbit<T> targetOrbit,
                                          final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof,
                                          final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, ClohessyWiltshireProvider.LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit      Target orbit. Should be circular for better results
     * @param initialChaserPVT Chaser PVT in the input frame
     * @param inputPVTFrame    Input frame for the initial chaser PVT
     */
    public FieldClohessyWiltshireProvider(final FieldOrbit<T> targetOrbit,
                                          final TimeStampedFieldPVCoordinates<T> initialChaserPVT,
                                          final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVT, inputPVTFrame, ClohessyWiltshireProvider.DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new ClohessyWiltshireProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results
     * @param initialChaserPVT        Chaser PVT in given frame
     * @param inputPVTFrame           Input frame for the initial chaser PVT
     * @param additionalEquationsName Additional equations name
     */
    public FieldClohessyWiltshireProvider(final FieldOrbit<T> targetOrbit,
                                          final TimeStampedFieldPVCoordinates<T> initialChaserPVT,
                                          final Frame inputPVTFrame,
                                          final String additionalEquationsName) {
        // Copy input parameters
        super(targetOrbit, initialChaserPVT, inputPVTFrame, additionalEquationsName,
              ClohessyWiltshireProvider.LOF_TYPE);
        this.meanMotion = targetOrbit.getKeplerianMeanMotion();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Get the chaser state relative to a target, in target's QSW LOF.
     * </p>
     */
    @Override
    public TimeStampedFieldPVCoordinates<T> extractChaserPVT(final FieldSpacecraftState<T> targetState) {

        // Get time since initial chaser date and compute CW transition matrices
        final T timeSinceEpoch = targetState.getDate().durationFrom(getInitialChaserPVTLof().getDate());
        final FieldClohessyWiltshireMatrices<T> cwMatrices =
                        (new FieldClohessyWiltshireEquations<T>()).computeMatrices(timeSinceEpoch,
                                                                                   meanMotion);
        // Compute PVT from Clohessy-Wiltshire equations
        return cwMatrices.transform(getInitialChaserPVTLof());
    }
}
