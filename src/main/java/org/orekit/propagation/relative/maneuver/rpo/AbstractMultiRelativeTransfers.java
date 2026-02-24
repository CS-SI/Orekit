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
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.maneuver.RelativeManeuver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for MultiRelativeTransfers to compute a list of relative maneuvers to be performed in order to achieve a relative scenario.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class AbstractMultiRelativeTransfers implements MultiRelativeTransfer {

    /**
     * List of points reached by the chaser.
     */
    private final List<TimeStampedPVCoordinates> waypoints;

    /**
     * Target's orbit.
     */
    private final Orbit targetOrbit;

    /**
     * LOF Type used by the relative motion model.
     */
    private final LOFType lofType;

    /**
     * Initial Velocity at the beginning of the transfers.
     */
    private final Vector3D initialVelocity;

    /**
     * Builds a MultiRelativeTransfers object.
     * @param waypoints list of waypoints of the successive transfers.
     * @param initialVelocity initial velocity at the beginning of the transfers.
     * @param targetOrbit orbit of the target.
     * @param lofType local orbital frame used by the model.
     */
    protected AbstractMultiRelativeTransfers(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit, final LOFType lofType) {
        this.waypoints = waypoints;
        this.targetOrbit = targetOrbit;
        this.lofType = lofType;
        this.initialVelocity =  initialVelocity;
    }

    /**
     * Get the waypoints.
     *
     * @return waypoints.
     */
    public List<TimeStampedPVCoordinates> getWaypoints() {
        return this.waypoints;
    }

    /**
     * Get the target orbit.
     *
     * @return targetOrbit.
     */
    public Orbit getTargetOrbit() {
        return this.targetOrbit;
    }

    /**
     * Get the LOF Type used by the model.
     *
     * @return lofType
     */
    public LOFType getLofType() {
        return this.lofType;
    }

    /**
     * Get the initial velocity at the beginning of the transfers.
     * @return initial velocity.
     */
    public Vector3D getInitialVelocity() { return initialVelocity; }

    @Override
    public List<TwoImpulseTransfer> computeMultiRelativeTransfers() {
        return computeMultiRelativeTransfers(new LocalOrbitalFrame(getTargetOrbit().getFrame(), lofType, getTargetOrbit(), lofType.getName()));
    }

    @Override
    public abstract List<TwoImpulseTransfer> computeMultiRelativeTransfers(Frame waypointsFrame);

    @Override
    public abstract List<? extends RelativeManeuver> computeRelativeManeuvers(RelativeProvider relativeProvider);

    @Override
    public List<ImpulseManeuver> computeImpulseManeuvers(final Frame frame, final double Isp) {
        final List<TwoImpulseTransfer> transfers = computeMultiRelativeTransfers();
        final LocalOrbitalFrame localFrame = new LocalOrbitalFrame(frame, getLofType(), getTargetOrbit(), getLofType().getName());
        final List<ImpulseManeuver> maneuvers = new ArrayList<>();
        //Transform the maneuvers from Local Orbital Frame to Inertial frame.
        for (TwoImpulseTransfer transfer: transfers) {
            final EventDetector firstImpulseTrigger = new DateDetector(transfer.getPvt1BeforeMan().getDate());
            final EventDetector secondImpulseTrigger = new DateDetector(transfer.getPvt2AfterMan().getDate());

            final TimeStampedPVCoordinates pvt1BeforeMan = transfer.getPvt1BeforeMan();
            final TimeStampedPVCoordinates pvt1AfterMan = transfer.getPvt1();
            final TimeStampedPVCoordinates pvt1BeforeManInertial = localFrame.getTransformTo(frame, pvt1BeforeMan.getDate()).transformPVCoordinates(pvt1BeforeMan);
            final TimeStampedPVCoordinates pvt1AfterManInertial = localFrame.getTransformTo(frame, pvt1AfterMan.getDate()).transformPVCoordinates(pvt1AfterMan);
            final Vector3D deltaV1Inertial = pvt1AfterManInertial.getVelocity().subtract(pvt1BeforeManInertial.getVelocity());

            final TimeStampedPVCoordinates pvt2BeforeMan = transfer.getPvt2();
            final TimeStampedPVCoordinates pvt2AfterMan = transfer.getPvt2AfterMan();
            final TimeStampedPVCoordinates pvt2BeforeManInertial = localFrame.getTransformTo(frame, pvt2BeforeMan.getDate()).transformPVCoordinates(pvt2BeforeMan);
            final TimeStampedPVCoordinates pvt2AfterManInertial = localFrame.getTransformTo(frame, pvt2AfterMan.getDate()).transformPVCoordinates(pvt2AfterMan);
            final Vector3D deltaV2Inertial = pvt2AfterManInertial.getVelocity().subtract(pvt2BeforeManInertial.getVelocity());

            final ImpulseManeuver maneuverInertial1 = new ImpulseManeuver(firstImpulseTrigger, deltaV1Inertial, Isp);
            final ImpulseManeuver maneuverInertial2 = new ImpulseManeuver(secondImpulseTrigger, deltaV2Inertial, Isp);

            maneuvers.add(maneuverInertial1);
            maneuvers.add(maneuverInertial2);
        }
        return maneuvers;
    }
}
