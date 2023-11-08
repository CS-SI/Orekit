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
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on turn around range measurements.
 * @author Luc Maisonobe
 * @since 9.0
 */
public class OnBoardAntennaTurnAroundRangeModifier implements EstimationModifier<TurnAroundRange> {

    /** Position of the Antenna Phase Center in satellite frame. */
    private final Vector3D antennaPhaseCenter;

    /** Simple constructor.
     * @param antennaPhaseCenter position of the Antenna Phase Center in satellite frame
     */
    public OnBoardAntennaTurnAroundRangeModifier(final Vector3D antennaPhaseCenter) {
        this.antennaPhaseCenter = antennaPhaseCenter;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TurnAroundRange> estimated) {

        // the participants are primary station at emission, spacecraft during leg 1,
        // secondary station at rebound, spacecraft during leg 2, primary station at reception
        final TimeStampedPVCoordinates[] participants      = estimated.getParticipants();
        final Vector3D                   pPrimaryEmission  = participants[0].getPosition();
        final AbsoluteDate               transitDateLeg1   = participants[1].getDate();
        final Vector3D                   pSecondaryRebound = participants[2].getPosition();
        final AbsoluteDate               transitDateLeg2   = participants[3].getDate();
        final Vector3D                   pPrimaryReception = participants[4].getPosition();

        // transforms from spacecraft to inertial frame at transit dates
        final SpacecraftState refState              = estimated.getStates()[0];
        final SpacecraftState transitStateLeg1      = refState.shiftedBy(transitDateLeg1.durationFrom(refState.getDate()));
        final StaticTransform spacecraftToInertLeg1 = transitStateLeg1.toStaticTransform().getInverse();
        final SpacecraftState transitStateLeg2      = refState.shiftedBy(transitDateLeg2.durationFrom(refState.getDate()));
        final StaticTransform spacecraftToInertLeg2 = transitStateLeg2.toStaticTransform().getInverse();

        // compute the geometrical value of the turn-around range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraftLeg1 = spacecraftToInertLeg1.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftLeg2 = spacecraftToInertLeg2.transformPosition(Vector3D.ZERO);
        final double turnAroundRangeUsingSpacecraftCenter =
                        0.5 * (Vector3D.distance(pPrimaryEmission,  pSpacecraftLeg1) +
                               Vector3D.distance(pSpacecraftLeg1,   pSecondaryRebound)   +
                               Vector3D.distance(pSecondaryRebound, pSpacecraftLeg2) +
                               Vector3D.distance(pSpacecraftLeg2,   pPrimaryReception));

        // compute the geometrical value of the range replacing
        // the spacecraft positions with antenna phase center positions
        final Vector3D pAPCLeg1 = spacecraftToInertLeg1.transformPosition(antennaPhaseCenter);
        final Vector3D pAPCLeg2 = spacecraftToInertLeg2.transformPosition(antennaPhaseCenter);
        final double turnAroundRangeUsingAntennaPhaseCenter =
                        0.5 * (Vector3D.distance(pPrimaryEmission,  pAPCLeg1)          +
                               Vector3D.distance(pAPCLeg1,          pSecondaryRebound) +
                               Vector3D.distance(pSecondaryRebound, pAPCLeg2)          +
                               Vector3D.distance(pAPCLeg2,          pPrimaryReception));

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += turnAroundRangeUsingAntennaPhaseCenter - turnAroundRangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

}
