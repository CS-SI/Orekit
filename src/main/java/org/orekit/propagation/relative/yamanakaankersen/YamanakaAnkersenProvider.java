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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.relative.AbstractRelativeProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This additional state provider implements the Yamanaka-Ankersen equations of relative motion to propagate the
 * relative orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Yamanaka-Ankersen equations are derived in LVLH CCSDS Frame whereas Clohessy-Wiltshire equations are
 * derived in local QSW Frame.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's LVLH
 * CCSDS local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a greater eccentricity, or
 * other perturbations. It is however a good and analytical (very fast) way to approximate the relative motion of two
 * spacecraft with the target in eccentric orbit.</p>
 * <p>The class {@link YamanakaAnkersenRendezVous} allows the analytical computation of 2-maneuver rendez-vous
 * transfers.</p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 */

public class YamanakaAnkersenProvider extends AbstractRelativeProvider {

    /**
     * Default additional equations name.
     */
    public static final String DEFAULT_ADDITIONAL_EQUATIONS_NAME =
                    "Yamanaka-Ankersen chaser state in target's LVLH CCSDS LOF";

    /**
     * Local Orbital Frame. Yamanaka-Ankersen equations are defined in the LVLH_CCSDS local orbital frame of the target,
     * so this provider is hardcoded to use this LOF.
     */
    public static final LOF LOF_TYPE = LOFType.LVLH_CCSDS;

    /**
     * Builds a YamanakaAnkersenProvider from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit orbit of the target
     */
    @DefaultDataContext
    public YamanakaAnkersenProvider(final Orbit targetOrbit) {
        this(targetOrbit, new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, Vector3D.ZERO, Vector3D.ZERO));
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an all-zero PVT for the chaser.
     *
     * @param targetOrbit         orbit of the target
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof) {
        this(targetOrbit, initialChaserPVTLof, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser.
     *
     * @param targetOrbit             orbit of the target
     * @param initialChaserPVTLof     Chaser PVT in the target's LVLH_CCSDS local orbital frame
     * @param additionalEquationsName Additional equations name
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof,
                                    final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in
     * the given input frame.
     *
     * @param targetOrbit      Target orbit. Should be circular for better results
     * @param initialChaserPVT Chaser PVT in given frame
     * @param inputPVTFrame    Input frame for the initial chaser PVT
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVT,
                                    final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVT, inputPVTFrame, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
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
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVT,
                                    final Frame inputPVTFrame, final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVT, inputPVTFrame, additionalEquationsName, LOF_TYPE);
    }

    /**
     * {@inheritDoc}.
     * <p>Return the same orbit but at a different true anomaly.</p>
     */
    @Override
    public void setTargetTrueAnomaly(final double trueAnomaly) {
        final KeplerianOrbit orbit = new KeplerianOrbit(getTargetOrbit());
        setTargetOrbit(new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPeriapsisArgument(),
                                          orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE,
                                          orbit.getFrame(), orbit.getDate(), orbit.getMu()));
    }

    /**
     * {@inheritDoc}.
     * <p>Get the chaser state relative to a target, in target's LVLH LOF.</p>
     */
    @Override
    public TimeStampedPVCoordinates extractChaserPVT(final SpacecraftState targetState) {

        // Time since initial PVT was given
        final double timeSinceEpoch = targetState.getDate().durationFrom(getInitialChaserPVTLof().getDate());

        // Target SMA and eccentricity
        final double a = targetState.getOrbit().getA();
        final double e = targetState.getOrbit().getE();
        final double mu = targetState.getOrbit().getMu();

        // Initial and final target anomalies
        final KeplerianOrbit initialOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        final double initialTrueAnomaly = initialOrbit.getTrueAnomaly();

        final KeplerianOrbit currentOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(targetState.getOrbit());
        final double trueAnomaly = currentOrbit.getTrueAnomaly();

        // Get the Yamanaka-Ankersen state transition matrices
        final YamanakaAnkersenMatrices yaMatrices =
                        YamanakaAnkersenEquations.computeMatrices(timeSinceEpoch, a, e, initialTrueAnomaly, trueAnomaly,
                                                                  mu);
        // Apply them to the initial chaser PVT to get the current chaser PVT in target's LOF
        return yaMatrices.transform(getInitialChaserPVTLof(), initialTrueAnomaly, trueAnomaly, e, a, mu);
    }
}

