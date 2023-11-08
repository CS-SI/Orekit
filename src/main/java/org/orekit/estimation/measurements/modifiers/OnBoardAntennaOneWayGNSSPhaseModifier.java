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
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.gnss.OneWayGNSSPhase;
import org.orekit.frames.StaticTransform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on one-way GNSS phase measurements.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OnBoardAntennaOneWayGNSSPhaseModifier implements EstimationModifier<OneWayGNSSPhase> {

    /** Position of the Antenna Phase Center in satellite 1 frame. */
    private final Vector3D antennaPhaseCenter1;

    /** Position of the Antenna Phase Center in satellite 2 frame. */
    private final Vector3D antennaPhaseCenter2;

    /** Attitude provider of the emitting satellite. */
    private final AttitudeProvider attitude;

    /** Simple constructor.
     * @param antennaPhaseCenter1 position of the Antenna Phase Center in satellite 1 frame
     * (i.e. the satellite which receives the signal and performs the measurement)
     * @param antennaPhaseCenter2 position of the Antenna Phase Center in satellite 2 frame
     * (i.e. the satellite which simply emits the signal)
     * @param attitude attitude provider of the emitting satellite
     */
    public OnBoardAntennaOneWayGNSSPhaseModifier(final Vector3D antennaPhaseCenter1,
                                                 final Vector3D antennaPhaseCenter2,
                                                 final AttitudeProvider attitude) {
        this.antennaPhaseCenter1 = antennaPhaseCenter1;
        this.antennaPhaseCenter2 = antennaPhaseCenter2;
        this.attitude            = attitude;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<OneWayGNSSPhase> estimated) {

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
        final SpacecraftState refStateRemote             = new SpacecraftState(orbitRemote,
                                                                               attitude.getAttitude(orbitRemote,
                                                                                                    orbitRemote.getDate(),
                                                                                                    orbitRemote.getFrame()));
        final SpacecraftState emissionState              = refStateRemote.shiftedBy(phaseEmissionDate.durationFrom(refStateRemote.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // Compute the geometrical value of the one-way GNSS phase directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraftReception = receptionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftEmission  = emissionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final double oneWayGNSSPhaseUsingSpacecraftCenter = Vector3D.distance(pSpacecraftEmission, pSpacecraftReception);

        // Compute the geometrical value of the phase replacing
        // the spacecraft positions with antenna phase center positions
        final Vector3D pAPCReception = receptionSpacecraftToInert.transformPosition(antennaPhaseCenter1);
        final Vector3D pAPCEmission  = emissionSpacecraftToInert.transformPosition(antennaPhaseCenter2);
        final double oneWayGNSSPhaseUsingAntennaPhaseCenter = Vector3D.distance(pAPCEmission, pAPCReception);

        // Get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // Modify the value
        final double wavelength = estimated.getObservedMeasurement().getWavelength();
        value[0] += (oneWayGNSSPhaseUsingAntennaPhaseCenter - oneWayGNSSPhaseUsingSpacecraftCenter) / wavelength;
        estimated.setEstimatedValue(value);

    }

}
