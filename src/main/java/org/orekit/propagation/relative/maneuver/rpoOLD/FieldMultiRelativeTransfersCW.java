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
package org.orekit.propagation.relative.maneuver.rpoOLD;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireRendezVous;
import org.orekit.propagation.relative.maneuver.FieldClohessyWiltshireManeuver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;
/**
 * Class to compute multi relative transfers based on the Clohessy-Wiltshire equations in order to achieve a defined sequence.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldMultiRelativeTransfersCW <T extends CalculusFieldElement<T>> extends FieldAbstractMultiRelativeTransfers<T> {

    /**
     * Builds a new MultiRelativeTransfers using the Clohessy-Wiltshire equations.
     *
     * @param targetOrbit orbit of the target satellite.
     * @param waypoints   list of waypoints to be covered.
     */
    public FieldMultiRelativeTransfersCW(final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldOrbit<T> targetOrbit) {
        super(waypoints, targetOrbit, LOFType.QSW);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Yamanaka-Ankersen equations.
     * NB: waypoints must be expressed in the YamanakaAnkersen Local Orbital Frame (LVLH CCSDS).
     *
     * @return list of TwoImpulseTransfer.
     */
    public List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers() {
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit().toOrbit(), getLofType().getName());
        return computeMultiRelativeTransfers(qswFrame);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Clohessy-Wiltshire equations.
     *
     * @param waypointsFrame Frame in which the waypoints are expressed.
     * @return list of TwoImpulseTransfer.
     */
    public List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers(final Frame waypointsFrame) {
        final List<FieldTwoImpulseTransfer<T>> multiTwoImpulseTransfer = new ArrayList<>();
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), LOFType.QSW, getTargetOrbit().toOrbit(), "QSW LOF ");
        final List<TimeStampedFieldPVCoordinates<T>> waypointsInQSW = new ArrayList<>();
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = getWaypoints();
        for (TimeStampedFieldPVCoordinates<T> waypoint : waypoints) {
            waypointsInQSW.add(waypointsFrame.getTransformTo(qswFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        for (int i = 0; i < waypointsInQSW.size() - 1; i++) {
            final FieldTwoImpulseTransfer<T> transfer = (new FieldClohessyWiltshireRendezVous<T>()).computeRendezVous(waypointsInQSW.get(i), waypointsInQSW.get(i + 1), qswFrame, getTargetOrbit());
            multiTwoImpulseTransfer.add(transfer);
        }
        return multiTwoImpulseTransfer;
    }

    @Override
    public List<FieldClohessyWiltshireManeuver<T>> computeRelativeManeuvers(final FieldRelativeProvider<T> cwProvider) {
        final List<FieldTwoImpulseTransfer<T>> transfers = computeMultiRelativeTransfers();
        final List<FieldClohessyWiltshireManeuver<T>> maneuvers = new ArrayList<>();
        for (FieldTwoImpulseTransfer<T> transfer : transfers) {
            final FieldEventDetector<T> firstImpulseTrigger = new FieldDateDetector<>(transfer.getPvt1BeforeMan().getDate());
            final FieldEventDetector<T> secondImpulseTrigger = new FieldDateDetector<>(transfer.getPvt2AfterMan().getDate());
            final FieldVector3D<T> deltaV1 = transfer.getDeltaV1();
            final FieldVector3D<T> deltaV2 = transfer.getDeltaV2();
            final FieldClohessyWiltshireManeuver<T> maneuver1 = new FieldClohessyWiltshireManeuver<>(firstImpulseTrigger, deltaV1, (FieldClohessyWiltshireProvider<T>) cwProvider);
            final FieldClohessyWiltshireManeuver<T> maneuver2 = new FieldClohessyWiltshireManeuver<>(secondImpulseTrigger, deltaV2, (FieldClohessyWiltshireProvider<T>) cwProvider);
            maneuvers.add(maneuver1);
            maneuvers.add(maneuver2);
        }
        return maneuvers;
    }
}
