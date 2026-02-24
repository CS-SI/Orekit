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
package org.orekit.propagation.relative.maneuver.rpo;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.propagation.relative.maneuver.FieldYamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenRendezVous;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to compute multi relative transfers based on the Yamanaka-Ankersen equations in order to achieve a defined sequence.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldMultiRelativeTransfersYA<T extends CalculusFieldElement<T>> extends FieldAbstractMultiRelativeTransfers<T> {

    /**
     * Propagator of the target.
     */
    private final FieldPropagator<T> targetPropagator;

    /**
     * Builds a new MultiRelativeTransfers using the Yamanaka-Ankersen equations.
     *
     * @param targetOrbit      orbit of the target satellite.
     * @param waypoints        list of waypoints to be covered.
     * @param targetPropagator propagator of the target.
     */
    public FieldMultiRelativeTransfersYA(final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldOrbit<T> targetOrbit, final FieldPropagator<T> targetPropagator) {
        super(waypoints, targetOrbit, LOFType.LVLH_CCSDS);
        this.targetPropagator = targetPropagator;
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Yamanaka-Ankersen equations.
     * NB: waypoints must be expressed in the YamanakaAnkersen Local Orbital Frame (LVLH CCSDS).
     *
     * @return list of TwoImpulseTransfer.
     */
    public List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers() {
        final LocalOrbitalFrame LVLHCCSDSFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit().toOrbit(), getLofType().getName());
        return computeMultiRelativeTransfers(LVLHCCSDSFrame);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Yamanaka-Ankersen equations.
     *
     * @param waypointsFrame Frame in which the waypoints are expressed.
     * @return list of TwoImpulseTransfer.
     */
    public List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers(final Frame waypointsFrame) {
        final LocalOrbitalFrame LVLHCCSDSFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit().toOrbit(), getLofType().getName());
        final List<TimeStampedFieldPVCoordinates<T>> waypointsInLVLH_CCSDS = new ArrayList<>();
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = getWaypoints();
        for (TimeStampedFieldPVCoordinates<T> waypoint : waypoints) {
            waypointsInLVLH_CCSDS.add(waypointsFrame.getTransformTo(LVLHCCSDSFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        final List<FieldTwoImpulseTransfer<T>> multiTwoImpulseTransfer = new ArrayList<>();
        FieldKeplerianOrbit<T> orbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        for (int i = 0; i < waypointsInLVLH_CCSDS.size() - 1; i++) {
            final LocalOrbitalFrame lofUpdated = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), orbit.toOrbit(), getLofType().getName());
            final FieldTwoImpulseTransfer<T> transfer = (new FieldYamanakaAnkersenRendezVous<T>()).computeRendezVous(waypointsInLVLH_CCSDS.get(i), waypointsInLVLH_CCSDS.get(i + 1), lofUpdated, orbit, targetPropagator);
            multiTwoImpulseTransfer.add(transfer);
            final FieldSpacecraftState<T> propagated = targetPropagator.propagate(waypointsInLVLH_CCSDS.get(i + 1).getDate());
            final T trueAnomaly = ((FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(propagated.getOrbit())).getTrueAnomaly();
            orbit = new FieldKeplerianOrbit<>(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), waypointsInLVLH_CCSDS.get(i + 1).getDate(), orbit.getMu());
        }
        return multiTwoImpulseTransfer;
    }

    @Override
    public List<FieldYamanakaAnkersenManeuver<T>> computeRelativeManeuvers(final FieldRelativeProvider<T> yaProvider) {
        final List<FieldTwoImpulseTransfer<T>> transfers = computeMultiRelativeTransfers();
        final List<FieldYamanakaAnkersenManeuver<T>> maneuvers = new ArrayList<>();
        for (FieldTwoImpulseTransfer<T> transfer : transfers) {
            final FieldEventDetector<T> firstImpulseTrigger = new FieldDateDetector<>(transfer.getPvt1BeforeMan().getDate());
            final FieldEventDetector<T> secondImpulseTrigger = new FieldDateDetector<>(transfer.getPvt2AfterMan().getDate());
            final FieldVector3D<T> deltaV1 = transfer.getDeltaV1();
            final FieldVector3D<T> deltaV2 = transfer.getDeltaV2();
            final FieldYamanakaAnkersenManeuver<T> maneuver1 = new FieldYamanakaAnkersenManeuver<>(firstImpulseTrigger, deltaV1, (FieldYamanakaAnkersenProvider<T>) yaProvider);
            final FieldYamanakaAnkersenManeuver<T> maneuver2 = new FieldYamanakaAnkersenManeuver<>(secondImpulseTrigger, deltaV2, (FieldYamanakaAnkersenProvider<T>) yaProvider);
            maneuvers.add(maneuver1);
            maneuvers.add(maneuver2);
        }
        return maneuvers;
    }
}
