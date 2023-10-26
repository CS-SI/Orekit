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
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on inter-satellites range measurements.
 * @author Luc Maisonobe
 * @since 9.0
 */
public class OnBoardAntennaInterSatellitesRangeModifier implements EstimationModifier<InterSatellitesRange> {

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
    public OnBoardAntennaInterSatellitesRangeModifier(final Vector3D antennaPhaseCenter1,
                                                      final Vector3D antennaPhaseCenter2) {
        this.antennaPhaseCenter1 = antennaPhaseCenter1;
        this.antennaPhaseCenter2 = antennaPhaseCenter2;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<InterSatellitesRange> estimated) {
        if (estimated.getParticipants().length < 3) {
            modifyOneWay(estimated);
        } else {
            modifyTwoWay(estimated);
        }
    }

    /** Apply a modifier to an estimated measurement in the one-way case.
     * @param estimated estimated measurement to modify
     */
    private void modifyOneWay(final EstimatedMeasurementBase<InterSatellitesRange> estimated) {

        // the participants are satellite 2 at emission, satellite 1 at reception
        final TimeStampedPVCoordinates[] participants  = estimated.getParticipants();
        final AbsoluteDate               emissionDate  = participants[0].getDate();
        final AbsoluteDate               receptionDate = participants[1].getDate();

        // transforms from spacecraft to inertial frame at emission/reception dates
        final SpacecraftState refState1                  = estimated.getStates()[0];
        final SpacecraftState receptionState             = refState1.shiftedBy(receptionDate.durationFrom(refState1.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();
        final SpacecraftState refState2                  = estimated.getStates()[1];
        final SpacecraftState emissionState              = refState2.shiftedBy(emissionDate.durationFrom(refState2.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // compute the geometrical value of the inter-satellites range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraftReception = receptionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftEmission  = emissionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final double interSatellitesRangeUsingSpacecraftCenter =
                        Vector3D.distance(pSpacecraftEmission, pSpacecraftReception);

        // compute the geometrical value of the range replacing
        // the spacecraft positions with antenna phase center positions
        final Vector3D pAPCReception = receptionSpacecraftToInert.transformPosition(antennaPhaseCenter1);
        final Vector3D pAPCEmission  = emissionSpacecraftToInert.transformPosition(antennaPhaseCenter2);
        final double interSatellitesRangeUsingAntennaPhaseCenter =
                        Vector3D.distance(pAPCEmission, pAPCReception);

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += interSatellitesRangeUsingAntennaPhaseCenter - interSatellitesRangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

    /** Apply a modifier to an estimated measurement in the two-way case.
     * @param estimated estimated measurement to modify
     */
    private void modifyTwoWay(final EstimatedMeasurementBase<InterSatellitesRange> estimated) {

        // the participants are satellite 1 at emission, satellite 2 at transit, satellite 1 at reception
        final TimeStampedPVCoordinates[] participants  = estimated.getParticipants();
        final AbsoluteDate               emissionDate  = participants[0].getDate();
        final AbsoluteDate               transitDate   = participants[1].getDate();
        final AbsoluteDate               receptionDate = participants[2].getDate();

        // transforms from spacecraft to inertial frame at emission/reception dates
        final SpacecraftState refState1                  = estimated.getStates()[0];
        final SpacecraftState receptionState             = refState1.shiftedBy(receptionDate.durationFrom(refState1.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();
        final SpacecraftState refState2                  = estimated.getStates()[1];
        final SpacecraftState transitState               = refState2.shiftedBy(transitDate.durationFrom(refState2.getDate()));
        final StaticTransform transitSpacecraftToInert   = transitState.toStaticTransform().getInverse();
        final SpacecraftState emissionState              = refState1.shiftedBy(emissionDate.durationFrom(refState1.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // compute the geometrical value of the inter-satellites range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraftReception = receptionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftTransit   = transitSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftEmission  = emissionSpacecraftToInert.transformPosition(Vector3D.ZERO);
        final double interSatellitesRangeUsingSpacecraftCenter =
                        0.5 * (Vector3D.distance(pSpacecraftEmission, pSpacecraftTransit) +
                               Vector3D.distance(pSpacecraftTransit, pSpacecraftReception));

        // compute the geometrical value of the range replacing
        // the spacecraft positions with antenna phase center positions
        final Vector3D pAPCReception = receptionSpacecraftToInert.transformPosition(antennaPhaseCenter1);
        final Vector3D pAPCTransit   = transitSpacecraftToInert.transformPosition(antennaPhaseCenter2);
        final Vector3D pAPCEmission  = emissionSpacecraftToInert.transformPosition(antennaPhaseCenter1);
        final double interSatellitesRangeUsingAntennaPhaseCenter =
                        0.5 * (Vector3D.distance(pAPCEmission, pAPCTransit) +
                               Vector3D.distance(pAPCTransit, pAPCReception));


        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += interSatellitesRangeUsingAntennaPhaseCenter - interSatellitesRangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

}
