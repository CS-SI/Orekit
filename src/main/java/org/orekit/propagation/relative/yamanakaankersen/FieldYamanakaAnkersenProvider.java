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
package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.relative.FieldAbstractRelativeProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This additional state provider implements the Yamanaka-Ankersen equations of relative motion to propagate the
 * relative orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Yamanaka-Ankersen equations are valid for any eccentric orbits.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's LVLH
 * CCSDS local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a greater eccentricity, or
 * other
 * perturbations. It is however a good and analytical (very fast) way to approximate the relative motion of two
 * spacecraft with the target in eccentric orbit.</p>
 * <p>The class {@link FieldYamanakaAnkersenRendezVous} allows the analytical computation of 2-maneuver rendez-vous
 * transfers.</p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldYamanakaAnkersenProvider<T extends CalculusFieldElement<T>> extends FieldAbstractRelativeProvider<T> {

    /**
     * Builds a YamanakaAnkersenProvider from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit orbit of the target
     */
    @DefaultDataContext
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit) {
        this(targetOrbit, new TimeStampedFieldPVCoordinates<>(targetOrbit.getA().getField(),
                                                              new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                           Vector3D.ZERO,
                                                                                           Vector3D.ZERO)));
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit         orbit of the target
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit,
                                         final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof) {
        this(targetOrbit, initialChaserPVTLof, YamanakaAnkersenProvider.DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             orbit of the target
     * @param initialChaserPVTLof     Chaser PVT in the target's LVLH_CCSDS local orbital frame
     * @param additionalEquationsName Additional equations name
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit,
                                         final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof,
                                         final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, YamanakaAnkersenProvider.LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit      Target orbit. Should be circular for better results
     * @param initialChaserPVT Chaser PVT in given frame
     * @param inputPVTFrame    Input frame for the initial chaser PVT
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit,
                                         final TimeStampedFieldPVCoordinates<T> initialChaserPVT,
                                         final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVT, inputPVTFrame, YamanakaAnkersenProvider.DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit             Target orbit. Should be circular for better results
     * @param initialChaserPVT        Chaser PVT in given Frame
     * @param inputPVTFrame           Input frame for the initial chaser PVT
     * @param additionalEquationsName Additional equations name
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit,
                                         final TimeStampedFieldPVCoordinates<T> initialChaserPVT,
                                         final Frame inputPVTFrame, final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVT, inputPVTFrame, additionalEquationsName, YamanakaAnkersenProvider.LOF_TYPE);
    }

    /**
     * Returns same orbit but at different true anomaly.
     *
     * @param trueAnomaly new true anomaly of the target
     */
    @Override
    public void setTargetTrueAnomaly(final T trueAnomaly) {
        final FieldKeplerianOrbit<T> orbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        setTargetOrbit(new FieldKeplerianOrbit<>(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPeriapsisArgument(),
                                                 orbit.getRightAscensionOfAscendingNode(), trueAnomaly,
                                                 PositionAngleType.TRUE, orbit.getFrame(), orbit.getDate(),
                                                 orbit.getMu()));
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Get the chaser state relative to a target, in target's LVLH CCSDS LOF.
     * </p>
     */
    @Override
    public TimeStampedFieldPVCoordinates<T> extractChaserPVT(final FieldSpacecraftState<T> targetState) {

        // Time since initial PVT was given
        final T timeSinceEpoch = targetState.getDate().durationFrom(getInitialChaserPVTLof().getDate());

        // Target SMA and eccentricity
        final T a = targetState.getOrbit().getA();
        final T e = targetState.getOrbit().getE();

        // Initial and final target anomalies
        final FieldKeplerianOrbit<T> initialOrbit =
                        (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        final FieldKeplerianOrbit<T> currentOrbit =
                        (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(targetState.getOrbit());
        final T initialTrueAnomaly = initialOrbit.getTrueAnomaly();
        final T trueAnomaly = currentOrbit.getTrueAnomaly();
        final T mu = targetState.getOrbit().getMu();

        // Get the Yamanaka-Ankersen state transition matrices
        final FieldYamanakaAnkersenMatrices<T> yaMatrices =
                        (new FieldYamanakaAnkersenEquations<T>()).computeMatrices(timeSinceEpoch, a, e,
                                                                                  initialTrueAnomaly, trueAnomaly, mu);
        // Apply them to the initial chaser PVT to get the current chaser PVT in target's LOF
        return yaMatrices.transform(getInitialChaserPVTLof(), initialTrueAnomaly, trueAnomaly, e, a, mu);
    }
}
