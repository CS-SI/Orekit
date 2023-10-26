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
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.gnss.InterSatellitesPhase;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on inter-satellites phase measurements.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OnBoardAntennaInterSatellitesPhaseModifier implements EstimationModifier<InterSatellitesPhase> {

    /** Position of the Antenna Phase Center in satellite 1 frame. */
    private final Vector3D antennaPhaseCenter1;

    /** Position of the Antenna Phase Center in satellite 2 frame. */
    private final Vector3D antennaPhaseCenter2;

    /** Simple constructor.
     * @param antennaPhaseCenter1 position of the Antenna Phase Center in satellite 1 frame
     * (i.e. the satellite which receives the signal and performs the measurement)
     * @param antennaPhaseCenter2 position of the Antenna Phase Center in satellite 2 frame
     * (i.e. the satellite which simply emits the signal in the one-way
     * case, or reflects the signal in the two-way case)
     */
    public OnBoardAntennaInterSatellitesPhaseModifier(final Vector3D antennaPhaseCenter1,
                                                      final Vector3D antennaPhaseCenter2) {
        this.antennaPhaseCenter1 = antennaPhaseCenter1;
        this.antennaPhaseCenter2 = antennaPhaseCenter2;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<InterSatellitesPhase> estimated) {

        // The participants are satellite 2 at emission, satellite 1 at reception
        final TimeStampedPVCoordinates[] participants  = estimated.getParticipants();
        final AbsoluteDate               emissionDate  = participants[0].getDate();
        final AbsoluteDate               receptionDate = participants[1].getDate();

        // transforms from spacecraft to inertial frame at emission/reception dates
        final SpacecraftState localState                 = estimated.getStates()[0];
        final SpacecraftState receptionState             = localState.shiftedBy(receptionDate.durationFrom(localState.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();
        final SpacecraftState remoteState                = estimated.getStates()[1];
        final SpacecraftState emissionState              = remoteState.shiftedBy(emissionDate.durationFrom(remoteState.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // Compute the geometrical value of the inter-satellites range directly from participants positions.
        final Vector3D pSpacecraftReception = receptionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftEmission  = emissionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final double interSatellitesRangeUsingSpacecraftCenter = Vector3D.distance(pSpacecraftEmission, pSpacecraftReception);

        // Compute the geometrical value of the range replacing
        // The spacecraft positions with antenna phase center positions
        final Vector3D pAPCReception = receptionSpacecraftToInert.transformPosition(antennaPhaseCenter1);
        final Vector3D pAPCEmission  = emissionSpacecraftToInert.transformPosition(antennaPhaseCenter2);
        final double interSatellitesRangeUsingAntennaPhaseCenter = Vector3D.distance(pAPCEmission, pAPCReception);

        // Get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // Modify the phase value by applying measurement wavelength
        final double wavelength = estimated.getObservedMeasurement().getWavelength();
        value[0] += (interSatellitesRangeUsingAntennaPhaseCenter - interSatellitesRangeUsingSpacecraftCenter) / wavelength;
        estimated.setEstimatedValue(value);

    }

}
