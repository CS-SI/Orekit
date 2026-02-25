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
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.lambert.LambertRendezVous;
import org.orekit.propagation.relative.maneuver.RelativeManeuver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link MultiRelativeTransfer} for the Lambert theory.
 */
public class MultiRelativeTransfersLambert extends AbstractMultiRelativeTransfers {
    /**
     * Builds a new MultiRelativeTransfers using the Lambert equations.
     *
     * @param targetOrbit orbit of the target satellite.
     * @param waypoints   list of waypoints to be covered.
     */
    public MultiRelativeTransfersLambert(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit) {
        super(waypoints, initialVelocity, targetOrbit, LOFType.QSW);
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Lambert equations.
     * NB: waypoints must be expressed in the ClohessyWiltshire Local Orbital Frame (QSW).
     *
     * @return list of TwoImpulseTransfer.
     */
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers() {
        return computeMultiRelativeTransfers(new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName()));
    }

    /**
     * Compute the list of TwoImpulseTransfer to achieve the rpo maneuver sequence using Lambert equations.
     *
     * @param waypointsFrame Frame in which the waypoints are expressed.
     * @return list of TwoImpulseTransfer.
     */
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers(final Frame waypointsFrame) {
        final List<TwoImpulseTransfer> twoImpulseTransfers = new ArrayList<>();
        final LocalOrbitalFrame qswFrame = new LocalOrbitalFrame(getTargetOrbit().getFrame(), getLofType(), getTargetOrbit(), getLofType().getName());
        final List<TimeStampedPVCoordinates> waypointsInQSW = new ArrayList<>();
        for (TimeStampedPVCoordinates waypoint : getWaypoints()) {
            waypointsInQSW.add(waypointsFrame.getTransformTo(qswFrame, waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        for (int i = 0; i < waypointsInQSW.size() - 1; i++) {
            final TwoImpulseTransfer transfer = LambertRendezVous.computeRendezVous(waypointsInQSW.get(i), waypointsInQSW.get(i + 1), qswFrame, new KeplerianPropagator(getTargetOrbit()));
            twoImpulseTransfers.add(transfer);
        }
        return twoImpulseTransfers;
    }

    @Override
    public List<RelativeManeuver> computeRelativeManeuvers(final RelativeProvider relativeProvider) {
        return null;// TODO can't be done with RelativeManeuver 'cause it's ImpulseManeuvers
    }
}
