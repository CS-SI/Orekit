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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireEquations;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireMatrices;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireRendezVous;
import org.orekit.propagation.relative.maneuver.ClohessyWiltshireManeuver;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link MultiRelativeTransfer} for the Clohessy-Wiltshire theory.
 */
public class MultiRelativeTransfersCW extends AbstractMultiRelativeTransfers {

    /**
     * Builds a new MultiRelativeTransfers using the Clohessy-Wiltshire equations.
     *
     * @param targetOrbit orbit of the target satellite.
     * @param initialVelocity initial velocity of the chaser.
     * @param waypoints   list of waypoints to be covered.
     */
    public MultiRelativeTransfersCW(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit) {
        super(waypoints, initialVelocity, targetOrbit, LOFType.QSW);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Clohessy-Wiltshire equations.
     * NB: waypoints must be expressed in the ClohessyWiltshire Local Orbital Frame (QSW).
     *
     * @return list of TwoImpulseTransfer.
     */
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers() {
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        return computeMultiRelativeTransfers(qswFrame);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Clohessy-Wiltshire equations.
     *
     * @param waypointsFrame Frame in which the waypoints are expressed.
     * @return list of TwoImpulseTransfer.
     */
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers(final Frame waypointsFrame) {
        final List<TwoImpulseTransfer> multiTwoImpulseTransfer = new ArrayList<>();
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        final List<TimeStampedPVCoordinates> waypointsInQSW = new ArrayList<>();
        for (TimeStampedPVCoordinates waypoint : getWaypoints()) {
            waypointsInQSW.add(waypointsFrame.getTransformTo(qswFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        for (int i = 0; i < waypointsInQSW.size() - 1; i++) {
            final TwoImpulseTransfer transfer = ClohessyWiltshireRendezVous.computeRendezVous(waypointsInQSW.get(i), waypointsInQSW.get(i + 1), qswFrame, getTargetOrbit());
            multiTwoImpulseTransfer.add(transfer);
        }
        return multiTwoImpulseTransfer;
    }

    @Override
    public List<ClohessyWiltshireManeuver> computeRelativeManeuvers(final RelativeProvider cwProvider) {
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        return computeRelativeManeuvers(qswFrame, cwProvider);
    }

    public List<ClohessyWiltshireManeuver> computeRelativeManeuvers(final Frame waypointsFrame, final RelativeProvider cwProvider) {
        final List<ClohessyWiltshireManeuver> maneuvers = new ArrayList<>();
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        final List<TimeStampedPVCoordinates> waypointsInQSW = new ArrayList<>();
        for (TimeStampedPVCoordinates waypoint: getWaypoints()) {
            waypointsInQSW.add(waypointsFrame.getTransformTo(qswFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        Vector3D velocityBeforeManeuver = waypointsFrame.getTransformTo(qswFrame, getWaypoints().get(0).getDate()).
                transformPVCoordinates(new PVCoordinates(getWaypoints().get(0).getPosition(), getInitialVelocity())).getVelocity();
        for (int i = 0; i < waypointsInQSW.size() - 1; i++) {
            // Define Current waypoint and next waypoint in QSW
            final TimeStampedPVCoordinates currentWaypoint = waypointsInQSW.get(i);
            final TimeStampedPVCoordinates nextWaypoint = waypointsInQSW.get(i + 1);
            // Define Date Detector to trigger the maneuver
            final EventDetector firstImpulseTrigger = new DateDetector(currentWaypoint.getDate());
            // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
            final Vector3D velocityAfterManeuver = ClohessyWiltshireRendezVous.computeRendezVous(currentWaypoint, nextWaypoint, qswFrame, getTargetOrbit()).getPvt1().getVelocity();
            // Compute the impulsion at the current waypoint to reach the next waypoint.
            final Vector3D deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
            // Create the Clohessy-Wiltshire maneuver and add it to the list.
            final ClohessyWiltshireManeuver maneuver = new ClohessyWiltshireManeuver(firstImpulseTrigger, deltaV, (ClohessyWiltshireProvider) cwProvider);
            maneuvers.add(maneuver);
            // Compute the velocity before the maneuver at the next waypoint.
            final double transferDuration = nextWaypoint.getDate().durationFrom(currentWaypoint.getDate());
            final ClohessyWiltshireMatrices matrices = ClohessyWiltshireEquations.computeMatrices(transferDuration, getTargetOrbit().getKeplerianMeanMotion());
            velocityBeforeManeuver = new Vector3D(matrices.getPhiVR().operate(MatrixUtils.createRealVector(currentWaypoint.getPosition().toArray()))
                    .add(matrices.getPhiVV().operate(MatrixUtils.createRealVector(velocityAfterManeuver.toArray()))).toArray());
        }
        return maneuvers;
    }
}
