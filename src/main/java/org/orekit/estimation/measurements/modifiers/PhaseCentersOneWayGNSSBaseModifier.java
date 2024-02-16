/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.estimation.measurements.modifiers;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.frames.StaticTransform;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on inter-satellites phase measurements.
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public class PhaseCentersOneWayGNSSBaseModifier<T extends AbstractMeasurement<T>> {

    /** Link offset model. */
    private final PhaseCentersOffsetComputer link;

    /** Attitude provider of the emitting satellite. */
    private final AttitudeProvider attitudeProvider;

    /** Simple constructor.
     * @param receiverPattern pattern for receiver satellite
     * @param emitterPattern  pattern for emitter satellite
     * @param attitudeProvider attitude provider of the emitting satellite
     */
    public PhaseCentersOneWayGNSSBaseModifier(final FrequencyPattern receiverPattern,
                                              final FrequencyPattern emitterPattern,
                                              final AttitudeProvider attitudeProvider) {
        this.link             = new PhaseCentersOffsetComputer(emitterPattern, receiverPattern);
        this.attitudeProvider = attitudeProvider;
    }

    /** Compute distance modification for one way measurement.
     * @param estimated estimated measurement to modify
     * @return distance modification to add to raw measurement
     */
    public double oneWayDistanceModification(final EstimatedMeasurementBase<T> estimated) {

        // The participants are remote satellite at emission, local satellite at reception
        final TimeStampedPVCoordinates[] phaseParticipants  = estimated.getParticipants();
        final AbsoluteDate               phaseEmissionDate  = phaseParticipants[0].getDate();
        final AbsoluteDate               phaseReceptionDate = phaseParticipants[1].getDate();

        // Transforms from spacecraft to inertial frame at reception date
        final SpacecraftState refStateLocal              = estimated.getStates()[0];
        final SpacecraftState receptionState             = refStateLocal.shiftedBy(phaseReceptionDate.durationFrom(refStateLocal.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();

        // Orbit of the remote satellite
        final Orbit orbitRemote = new CartesianOrbit(phaseParticipants[0], refStateLocal.getFrame(), receptionState.getMu());

        // Transforms from spacecraft to inertial frame at emission date
        final SpacecraftState refStateRemote            = new SpacecraftState(orbitRemote,
                                                                              attitudeProvider.getAttitude(orbitRemote,
                                                                                                           orbitRemote.getDate(),
                                                                                                           orbitRemote.getFrame()));
        final SpacecraftState emissionState             = refStateRemote.shiftedBy(phaseEmissionDate.durationFrom(refStateRemote.getDate()));
        final StaticTransform emissionSpacecraftToInert = emissionState.toStaticTransform().getInverse();

        // compute offset due to phase centers
        return link.offset(emissionSpacecraftToInert, receptionSpacecraftToInert);
    }

}
