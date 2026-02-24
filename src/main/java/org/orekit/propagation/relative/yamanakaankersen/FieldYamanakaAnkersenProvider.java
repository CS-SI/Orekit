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
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.relative.FieldAbstractRelativeProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * <p>This additional state provider implements the Yamanaka-Ankersen equations of relative motion to propagate the relative
 * orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Yamanaka-Ankersen equations are valid for any eccentric orbits.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's LVLH CCSDS local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a greater eccentricity, or other perturbations.
 * It is however a good and analytical (very fast) way to approximate the relative motion of two spacecraft with the target in eccentric orbit.</p>
 * <p>The class {@link FieldYamanakaAnkersenRendezVous} allows the analytical computation of 2-maneuver rendez-vous transfers.</p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldYamanakaAnkersenProvider<T extends CalculusFieldElement<T>> extends FieldAbstractRelativeProvider<T> {

    /**
     * Default additional equations name.
     */
    public static final String DEFAULT_ADDITIONAL_EQUATIONS_NAME = "Yamanaka-Ankersen chaser state in target's LVLH CCSDS LOF";

    /** Local Orbital Frame. */
    public static final LOFType LOF_TYPE = LOFType.LVLH_CCSDS;

    /**
     * Builds a YamanakaAnkersenProvider from the target orbit and an all-zero PVT for the chaser.
     * @param targetOrbit orbit of the target.
     */
    @DefaultDataContext
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit) {
        super(targetOrbit, LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an all-zero PVT for the chaser.
     * @param targetOrbit orbit of the target.
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame.
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit, final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof) {
        this(targetOrbit, initialChaserPVTLof, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser.
     * @param targetOrbit orbit of the target.
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame.
     * @param additionalEquationsName Additional equations name.
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit, final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof, final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     * @param targetOrbit Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof Chaser PVT in given frame.
     * @param inputPVTFrame Input frame for the initial chaser PVT.
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit, final TimeStampedFieldPVCoordinates<T> initialChaserPVTLof, final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVTLof, inputPVTFrame, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     * @param targetOrbit Target orbit. Should be circular for better results.
     * @param initialChaserPVT Chaser PVT in given Frame.
     * @param inputPVTFrame Input frame for the initial chaser PVT.
     * @param additionalEquationsName Additional equations name.
     */
    public FieldYamanakaAnkersenProvider(final FieldOrbit<T> targetOrbit, final TimeStampedFieldPVCoordinates<T> initialChaserPVT, final Frame inputPVTFrame, final String additionalEquationsName) {
        super(targetOrbit, additionalEquationsName, LOF_TYPE);
        // Transform PV from input frame to inertial frame of target Orbit.
        final FieldTransform<T> inputFrameToInertial = inputPVTFrame.getTransformTo(targetOrbit.getFrame(), targetOrbit.getDate());
        final TimeStampedFieldPVCoordinates<T> pvInertial = inputFrameToInertial.transformPVCoordinates(initialChaserPVT);
        // Transform and set PV from inertial to target's LOF.
        setInitialChaserPVTLof(LOF_TYPE.transformFromInertial(targetOrbit.getDate(), targetOrbit.getPVCoordinates()).transformPVCoordinates(pvInertial));
    }

    /**
     * Returns same orbit but at different true anomaly.
     * @param trueAnomaly new true anomaly of the target.
     */
    @Override
    public void setTargetTrueAnomaly(final T trueAnomaly) {
        final FieldKeplerianOrbit<T> orbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        setTargetOrbit(new FieldKeplerianOrbit<>(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), orbit.getDate(), orbit.getMu()));
    }

    /**
     * Get the chaser state relative to a target, in target's LVLH LOF.
     *
     * @param spacecraftState spacecraft state to which additional data should correspond
     * @return chaser cartesian state in target's LVLH Local Orbital Frame.
     */
    @Override
    public T[] getAdditionalData(final FieldSpacecraftState<T> spacecraftState) {
        final T timeSinceEpoch = spacecraftState.getDate().durationFrom(getInitialChaserPVTLof().getDate());
        final T a = spacecraftState.getOrbit().getA();
        final T E = spacecraftState.getOrbit().getE();

        final FieldKeplerianOrbit<T> initialOrbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        final FieldKeplerianOrbit<T> currentOrbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(spacecraftState.getOrbit());
        final T initialTrueAnomaly = initialOrbit.getTrueAnomaly();

        final T trueAnomaly = currentOrbit.getTrueAnomaly();
        final T mu = spacecraftState.getOrbit().getMu();

        final FieldYamanakaAnkersenMatrices<T> yaMatrices = (new FieldYamanakaAnkersenEquations<T>()).computeMatrices(timeSinceEpoch, a, E, initialTrueAnomaly, trueAnomaly, mu);
        final TimeStampedFieldPVCoordinates<T> pvt = yaMatrices.transform(getInitialChaserPVTLof(), initialTrueAnomaly, trueAnomaly, E, a, mu);

        final T[] array = MathArrays.buildArray(a.getField(), 6);
        array[0] = pvt.getPosition().getX();
        array[1] = pvt.getPosition().getY();
        array[2] = pvt.getPosition().getZ();
        array[3] = pvt.getVelocity().getX();
        array[4] = pvt.getVelocity().getY();
        array[5] = pvt.getVelocity().getZ();

        return array;
    }
}
