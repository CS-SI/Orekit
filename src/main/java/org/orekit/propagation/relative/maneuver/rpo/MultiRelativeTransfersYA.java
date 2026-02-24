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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.maneuver.YamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenRendezVous;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiRelativeTransfersYA extends AbstractMultiRelativeTransfers {

    /**
     * Propagator of the target.
     */
    private final Propagator targetPropagator;

    /**
     * Builds a new MultiRelativeTransfers using the Yamanaka-Ankersen equations.
     *
     * @param targetOrbit      orbit of the target satellite.
     * @param waypoints        list of waypoints to be covered.
     * @param initialVelocity initial velocity of chaser.
     * @param targetPropagator propagator of the target.
     */
    public MultiRelativeTransfersYA(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit, final Propagator targetPropagator) {
        super(waypoints, initialVelocity, targetOrbit, LOFType.LVLH_CCSDS);
        this.targetPropagator = targetPropagator;
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Yamanaka-Ankersen equations.
     *
     * @param waypointsFrame Frame in which the waypoints are expressed.
     * @return list of TwoImpulseTransfer.
     */
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers(final Frame waypointsFrame) {
        final LocalOrbitalFrame LVLHCCSDSFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        final List<TimeStampedPVCoordinates> waypointsInLVLH_CCSDS = new ArrayList<>();
        for (TimeStampedPVCoordinates waypoint : getWaypoints()) {
            waypointsInLVLH_CCSDS.add(waypointsFrame.getTransformTo(LVLHCCSDSFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        final List<TwoImpulseTransfer> multiTwoImpulseTransfer = new ArrayList<>();
        KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        for (int i = 0; i < waypointsInLVLH_CCSDS.size() - 1; i++) {
            final LocalOrbitalFrame lofUpdated = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), orbit, getLofType().getName());
            final TwoImpulseTransfer transfer = YamanakaAnkersenRendezVous.computeRendezVous(waypointsInLVLH_CCSDS.get(i), waypointsInLVLH_CCSDS.get(i + 1), lofUpdated, orbit, targetPropagator);
            multiTwoImpulseTransfer.add(transfer);
            final SpacecraftState propagated = targetPropagator.propagate(waypointsInLVLH_CCSDS.get(i + 1).getDate());
            final double trueAnomaly = ((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagated.getOrbit())).getTrueAnomaly();
            orbit = new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), waypointsInLVLH_CCSDS.get(i + 1).getDate(), orbit.getMu());
        }
        return multiTwoImpulseTransfer;
    }

    @Override
    public List<YamanakaAnkersenManeuver> computeRelativeManeuvers(final RelativeProvider cwProvider) {
        final LocalOrbitalFrame lvlhFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        return computeRelativeManeuvers(lvlhFrame, cwProvider);
    }

    public List<YamanakaAnkersenManeuver> computeRelativeManeuvers(final Frame waypointsFrame, final RelativeProvider yaProvider) {
        final List<YamanakaAnkersenManeuver> maneuvers = new ArrayList<>();
        final LocalOrbitalFrame lvlhFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        final List<TimeStampedPVCoordinates> waypointsInLVLH_CCSDS = new ArrayList<>();
        for (TimeStampedPVCoordinates waypoint: getWaypoints()) {
            waypointsInLVLH_CCSDS.add(waypointsFrame.getTransformTo(lvlhFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        Vector3D velocityBeforeManeuver = waypointsFrame.getTransformTo(lvlhFrame, getWaypoints().get(0).getDate()).
                transformPVCoordinates(new PVCoordinates(getWaypoints().get(0).getPosition(), getInitialVelocity())).getVelocity();
        KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(getTargetOrbit());
        yaProvider.setTargetOrbit(orbit);
        for (int i = 0; i < waypointsInLVLH_CCSDS.size() - 1; i++) {
            final LocalOrbitalFrame lofUpdated = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), orbit, getLofType().getName());
            // Define Current waypoint and next waypoint in QSW
            final TimeStampedPVCoordinates currentWaypoint = waypointsInLVLH_CCSDS.get(i);
            final TimeStampedPVCoordinates nextWaypoint = waypointsInLVLH_CCSDS.get(i + 1);
            // Define Date Detector to trigger the maneuver
            final EventDetector firstImpulseTrigger = new DateDetector(currentWaypoint.getDate());
            // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
            final Vector3D velocityAfterManeuver = YamanakaAnkersenRendezVous.computeRendezVous(currentWaypoint, nextWaypoint, lofUpdated, orbit, targetPropagator).
                    getPvt1().getVelocity();
            // Compute the impulsion at the current waypoint to reach the next waypoint.
            final Vector3D deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
            // Create the Yamanaka-Ankersen maneuver and add it to the list.
            final YamanakaAnkersenManeuver maneuver = new YamanakaAnkersenManeuver(firstImpulseTrigger, deltaV, (YamanakaAnkersenProvider) yaProvider);
            maneuvers.add(maneuver);
            // Create a Yamanaka-Ankersen provider to propagate to the next waypoint.
            final YamanakaAnkersenProvider yaProviderManeuver = new YamanakaAnkersenProvider(orbit, new TimeStampedPVCoordinates(currentWaypoint.getDate(), currentWaypoint.getPosition(), velocityAfterManeuver), "Provider_" + i);
            targetPropagator.addAdditionalDataProvider(yaProviderManeuver);
            // Propagate the waypoint.
            final SpacecraftState propagated = targetPropagator.propagate(nextWaypoint.getDate());
            // Update the target orbit and the velocity before the next maneuver.
            final double trueAnomaly = ((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagated.getOrbit())).getTrueAnomaly();
            orbit = new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), nextWaypoint.getDate(), orbit.getMu());
            velocityBeforeManeuver = new Vector3D(Arrays.copyOfRange(yaProviderManeuver.getAdditionalData(propagated), 3, 6));
        }
        return maneuvers;
    }
}
