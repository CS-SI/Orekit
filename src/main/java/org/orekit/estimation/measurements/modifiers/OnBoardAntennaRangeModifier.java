/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.Range;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on range measurements.
 * @author Luc Maisonobe
 * @since 9.0
 */
public class OnBoardAntennaRangeModifier implements EstimationModifier<Range> {

    /** Position of the Antenna Phase Center in satellite frame. */
    private final Vector3D antennaPhaseCenter;

    /** Simple constructor.
     * @param antennaPhaseCenter position of the Antenna Phase Center in satellite frame
     */
    public OnBoardAntennaRangeModifier(final Vector3D antennaPhaseCenter) {
        this.antennaPhaseCenter = antennaPhaseCenter;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<Range> estimated) {
        if (estimated.getObservedMeasurement().isTwoWay()) {
            modifyTwoWay(estimated);
        } else {
            modifyOneWay(estimated);
        }
    }

    /** Apply a modifier to a one-way range measurement.
     * @param estimated estimated measurement to modify
     */
    private void modifyOneWay(final EstimatedMeasurement<Range> estimated) {

        // the participants are spacecraft at emission, ground station at reception
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final AbsoluteDate               emissionDate = participants[0].getDate();
        final Vector3D                   pReception   = participants[1].getPosition();

        // transform from spacecraft to inertial frame at emission date
        final SpacecraftState refState          = estimated.getStates()[0];
        final SpacecraftState emissionState     = refState.shiftedBy(emissionDate.durationFrom(refState.getDate()));
        final Transform       spacecraftToInert = emissionState.toTransform().getInverse();

        // compute the geometrical value of the range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraft = spacecraftToInert.transformPosition(Vector3D.ZERO);
        final double rangeUsingSpacecraftCenter = Vector3D.distance(pSpacecraft, pReception);

        // compute the geometrical value of the range replacing
        // the spacecraft position with antenna phase center position
        final Vector3D pAPC = spacecraftToInert.transformPosition(antennaPhaseCenter);
        final double rangeUsingAntennaPhaseCenter = Vector3D.distance(pAPC, pReception);

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += rangeUsingAntennaPhaseCenter - rangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

    /** Apply a modifier to a two-way range measurement.
     * @param estimated estimated measurement to modify
     */
    private void modifyTwoWay(final EstimatedMeasurement<Range> estimated) {

        // the participants are ground station at emission, spacecraft, ground station at reception
        // or spacecraft, ground station at reception if oneWay
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final Vector3D                   pEmission    = participants[0].getPosition();
        final AbsoluteDate               transitDate  = participants[1].getDate();
        final Vector3D                   pReception   = participants[2].getPosition();

        // transform from spacecraft to inertial frame at transit date
        final SpacecraftState refState          = estimated.getStates()[0];
        final SpacecraftState transitState      = refState.shiftedBy(transitDate.durationFrom(refState.getDate()));
        final Transform       spacecraftToInert = transitState.toTransform().getInverse();

        // compute the geometrical value of the range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraft = spacecraftToInert.transformPosition(Vector3D.ZERO);
        final double rangeUsingSpacecraftCenter =
                        0.5 * (Vector3D.distance(pEmission, pSpacecraft) + Vector3D.distance(pSpacecraft, pReception));

        // compute the geometrical value of the range replacing
        // the spacecraft position with antenna phase center position
        final Vector3D pAPC = spacecraftToInert.transformPosition(antennaPhaseCenter);
        final double rangeUsingAntennaPhaseCenter =
                        0.5 * (Vector3D.distance(pEmission, pAPC) + Vector3D.distance(pAPC, pReception));

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += rangeUsingAntennaPhaseCenter - rangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

}
