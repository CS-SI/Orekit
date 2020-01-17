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
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.frames.Transform;
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
    public void modify(final EstimatedMeasurement<TurnAroundRange> estimated) {

        // the participants are master station at emission, spacecraft during leg 1,
        // slave station at rebound, spacecraft during leg 2, master station at reception
        final TimeStampedPVCoordinates[] participants     = estimated.getParticipants();
        final Vector3D                   pMasterEmission  = participants[0].getPosition();
        final AbsoluteDate               transitDateLeg1  = participants[1].getDate();
        final Vector3D                   pSlaveRebound    = participants[2].getPosition();
        final AbsoluteDate               transitDateLeg2  = participants[3].getDate();
        final Vector3D                   pMasterReception = participants[4].getPosition();

        // transforms from spacecraft to inertial frame at transit dates
        final SpacecraftState refState              = estimated.getStates()[0];
        final SpacecraftState transitStateLeg1      = refState.shiftedBy(transitDateLeg1.durationFrom(refState.getDate()));
        final Transform       spacecraftToInertLeg1 = transitStateLeg1.toTransform().getInverse();
        final SpacecraftState transitStateLeg2      = refState.shiftedBy(transitDateLeg2.durationFrom(refState.getDate()));
        final Transform       spacecraftToInertLeg2 = transitStateLeg2.toTransform().getInverse();

        // compute the geometrical value of the turn-around range directly from participants positions.
        // Note that this may be different from the value returned by estimated.getEstimatedValue(),
        // because other modifiers may already have been taken into account
        final Vector3D pSpacecraftLeg1 = spacecraftToInertLeg1.transformPosition(Vector3D.ZERO);
        final Vector3D pSpacecraftLeg2 = spacecraftToInertLeg2.transformPosition(Vector3D.ZERO);
        final double turnAroundRangeUsingSpacecraftCenter =
                        0.5 * (Vector3D.distance(pMasterEmission, pSpacecraftLeg1) +
                               Vector3D.distance(pSpacecraftLeg1, pSlaveRebound)   +
                               Vector3D.distance(pSlaveRebound,   pSpacecraftLeg2) +
                               Vector3D.distance(pSpacecraftLeg2, pMasterReception));

        // compute the geometrical value of the range replacing
        // the spacecraft positions with antenna phase center positions
        final Vector3D pAPCLeg1 = spacecraftToInertLeg1.transformPosition(antennaPhaseCenter);
        final Vector3D pAPCLeg2 = spacecraftToInertLeg2.transformPosition(antennaPhaseCenter);
        final double turnAroundRangeUsingAntennaPhaseCenter =
                        0.5 * (Vector3D.distance(pMasterEmission, pAPCLeg1)      +
                               Vector3D.distance(pAPCLeg1,        pSlaveRebound) +
                               Vector3D.distance(pSlaveRebound,   pAPCLeg2)      +
                               Vector3D.distance(pAPCLeg2,        pMasterReception));

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += turnAroundRangeUsingAntennaPhaseCenter - turnAroundRangeUsingSpacecraftCenter;
        estimated.setEstimatedValue(value);

    }

}
