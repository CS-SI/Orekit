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
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.relative.AbstractRelativeProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This additional state provider implements the Yamanaka-Ankersen equations of relative motion to propagate the relative
 * orbit of a chaser spacecraft around the target spacecraft whose orbit is being propagated.</p>
 * <p>Note 1: the Yamanaka-Ankersen equations are derived in LVLH CCSDS Frame whereas Clohessy-Wiltshire equations are derived in local QSW Frame.</p>
 * <p>Note 2: the additional state returned is a PV of the chaser at the given time <em>expressed in the target's LVLH CCSDS local orbital frame.</em></p>
 * <p>The actual orbit of a chaser around the target will be different when considering a greater eccentricity, or other perturbations.
 * It is however a good and analytical (very fast) way to approximate the relative motion of two spacecraft with the target in eccentric orbit.</p>
 * <p>The class {@link YamanakaAnkersenRendezVous} allows the analytical computation of 2-maneuver rendez-vous transfers.</p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 */

public class YamanakaAnkersenProvider extends AbstractRelativeProvider {

    /**
     * Default additional equations name.
     */
    public static final String DEFAULT_ADDITIONAL_EQUATIONS_NAME = "Yamanaka-Ankersen chaser state in target's LVLH CCSDS LOF";

    /** Default Local Orbital Frame. */
    public static final LOFType LOF_TYPE = LOFType.LVLH_CCSDS;

    /**
     * Builds a YamanakaAnkersenProvider from the target orbit and an all-zero PVT for the chaser.
     * @param targetOrbit orbit of the target.
     */
    @DefaultDataContext
    public YamanakaAnkersenProvider(final Orbit targetOrbit) {
        super(targetOrbit, new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, Vector3D.ZERO, Vector3D.ZERO), LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an all-zero PVT for the chaser.
     * @param targetOrbit orbit of the target.
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame.
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof) {
        super(targetOrbit, initialChaserPVTLof, DEFAULT_ADDITIONAL_EQUATIONS_NAME, LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser.
     * @param targetOrbit orbit of the target.
     * @param initialChaserPVTLof Chaser PVT in the target's LVLH_CCSDS local orbital frame.
     * @param additionalEquationsName Additional equations name.
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof, final String additionalEquationsName) {
        super(targetOrbit, initialChaserPVTLof, additionalEquationsName, LOF_TYPE);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     * @param targetOrbit Target orbit. Should be circular for better results.
     * @param initialChaserPVTLof Chaser PVT in given frame.
     * @param inputPVTFrame Input frame for the initial chaser PVT.
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVTLof, final Frame inputPVTFrame) {
        this(targetOrbit, initialChaserPVTLof, inputPVTFrame, DEFAULT_ADDITIONAL_EQUATIONS_NAME);
    }

    /**
     * Builds a new YamanakaAnkersenProvider object from the target orbit and an initial PVT of the chaser expressed in the given input frame.
     * @param targetOrbit Target orbit. Should be circular for better results.
     * @param initialChaserPVT Chaser PVT in given Frame.
     * @param inputPVTFrame Input frame for the initial chaser PVT.
     * @param additionalEquationsName Additional equations name.
     */
    public YamanakaAnkersenProvider(final Orbit targetOrbit, final TimeStampedPVCoordinates initialChaserPVT, final Frame inputPVTFrame, final String additionalEquationsName) {
        super(targetOrbit, additionalEquationsName, LOF_TYPE);
        // Transform PV from input frame to inertial frame of target Orbit.
        final Transform inputFrameToInertial = inputPVTFrame.getTransformTo(targetOrbit.getFrame(), targetOrbit.getDate());
        final TimeStampedPVCoordinates pvInertial = inputFrameToInertial.transformPVCoordinates(initialChaserPVT);
        // Transform and set PV from inertial to target's LOF.
        setInitialChaserPVTLof(LOF_TYPE.transformFromInertial(targetOrbit.getDate(), targetOrbit.getPVCoordinates()).transformPVCoordinates(pvInertial));
    }

    // Return the same orbit but at a different true anomaly.
    @Override
    public void setTargetTrueAnomaly(final double trueAnomaly) {
        final KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        setTargetOrbit(new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), orbit.getDate(), orbit.getMu()));
    }

    /**
     * Get the chaser state relative to a target, in target's LVLH LOF.
     *
     * @param spacecraftState spacecraft state to which additional data should correspond
     * @return chaser cartesian state in target's LVLH Local Orbital Frame.
     */
    @Override
    public double[] getAdditionalData(final SpacecraftState spacecraftState) {

        final double timeSinceEpoch = spacecraftState.getDate().durationFrom(getInitialChaserPVTLof().getDate());
        final double a = spacecraftState.getOrbit().getA();
        final double E = spacecraftState.getOrbit().getE();

        final KeplerianOrbit initialOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        final KeplerianOrbit currentOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(spacecraftState.getOrbit());
        final double initialTrueAnomaly = initialOrbit.getTrueAnomaly();

        final double trueAnomaly = currentOrbit.getTrueAnomaly();
        final double mu = spacecraftState.getOrbit().getMu();

        final YamanakaAnkersenMatrices yaMatrices = YamanakaAnkersenEquations.computeMatrices(timeSinceEpoch, a, E, initialTrueAnomaly, trueAnomaly, mu);
        final TimeStampedPVCoordinates pvt = yaMatrices.transform(getInitialChaserPVTLof(), initialTrueAnomaly, trueAnomaly, E, a, mu);

        return new double[] {
                pvt.getPosition().getX(), pvt.getPosition().getY(), pvt.getPosition().getZ(),
                pvt.getVelocity().getX(), pvt.getVelocity().getY(), pvt.getVelocity().getZ(),
        };
    }
}

