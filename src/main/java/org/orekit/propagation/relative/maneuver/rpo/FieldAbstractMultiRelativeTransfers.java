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
import org.orekit.forces.maneuvers.FieldImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.propagation.relative.maneuver.FieldRelativeManeuver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for FieldMultiRelativeTransfers to compute a list of relative maneuvers to be performed in order to achieve a relative scenario.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class FieldAbstractMultiRelativeTransfers<T extends CalculusFieldElement<T>> implements FieldMultiRelativeTransfer<T> {
    /**
     * List of points reached by the chaser.
     */
    private final List<TimeStampedFieldPVCoordinates<T>> waypoints;

    /**
     * Target's orbit.
     */
    private final FieldOrbit<T> targetOrbit;

    /**
     * LOF Type used by the relative motion model.
     */
    private final LOFType lofType;

    /**
     * Builds a MultiRelativeTransfers object.
     * @param waypoints list of waypoints of the successive transfers.
     * @param targetOrbit orbit of the target.
     * @param lofType local orbital frame used by the model.
     */
    protected FieldAbstractMultiRelativeTransfers(final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldOrbit<T> targetOrbit, final LOFType lofType) {
        this.waypoints = waypoints;
        this.targetOrbit = targetOrbit;
        this.lofType = lofType;
    }

    /**
     * Get the waypoints.
     *
     * @return waypoints.
     */
    public List<TimeStampedFieldPVCoordinates<T>> getWaypoints() {
        return this.waypoints;
    }

    /**
     * Get the target orbit.
     *
     * @return targetOrbit.
     */
    public FieldOrbit<T> getTargetOrbit() {
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

    @Override
    public List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers() {
        return computeMultiRelativeTransfers(new LocalOrbitalFrame(getTargetOrbit().getFrame(), lofType, getTargetOrbit().toOrbit(), lofType.getName()));
    }

    @Override
    public abstract List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers(Frame waypointsFrame);

    @Override
    public abstract List<? extends FieldRelativeManeuver<T>> computeRelativeManeuvers(FieldRelativeProvider<T> relativeProvider);

    @Override
    public List<FieldImpulseManeuver<T>> computeImpulseManeuvers(final Frame frame, final T Isp) {
        final List<FieldTwoImpulseTransfer<T>> transfers = computeMultiRelativeTransfers();
        final LocalOrbitalFrame localFrame = new LocalOrbitalFrame(frame, getLofType(), getTargetOrbit().toOrbit(), getLofType().getName());
        final List<FieldImpulseManeuver<T>> maneuvers = new ArrayList<>();
        //Transform the maneuvers from Local Orbital Frame to Inertial frame.
        for (FieldTwoImpulseTransfer<T> transfer: transfers) {
            final FieldEventDetector<T> firstImpulseTrigger = new FieldDateDetector<>(transfer.getPvt1BeforeMan().getDate());
            final FieldEventDetector<T> secondImpulseTrigger = new FieldDateDetector<>(transfer.getPvt2AfterMan().getDate());

            final TimeStampedFieldPVCoordinates<T> pvt1BeforeMan = transfer.getPvt1BeforeMan();
            final TimeStampedFieldPVCoordinates<T> pvt1AfterMan = transfer.getPvt1();
            final TimeStampedFieldPVCoordinates<T> pvt1BeforeManInertial = localFrame.getTransformTo(frame, pvt1BeforeMan.getDate()).transformPVCoordinates(pvt1BeforeMan);
            final TimeStampedFieldPVCoordinates<T> pvt1AfterManInertial = localFrame.getTransformTo(frame, pvt1AfterMan.getDate()).transformPVCoordinates(pvt1AfterMan);
            final FieldVector3D<T> deltaV1Inertial = pvt1AfterManInertial.getVelocity().subtract(pvt1BeforeManInertial.getVelocity());

            final TimeStampedFieldPVCoordinates<T> pvt2BeforeMan = transfer.getPvt2();
            final TimeStampedFieldPVCoordinates<T> pvt2AfterMan = transfer.getPvt2AfterMan();
            final TimeStampedFieldPVCoordinates<T> pvt2BeforeManInertial = localFrame.getTransformTo(frame, pvt2BeforeMan.getDate()).transformPVCoordinates(pvt2BeforeMan);
            final TimeStampedFieldPVCoordinates<T> pvt2AfterManInertial = localFrame.getTransformTo(frame, pvt2AfterMan.getDate()).transformPVCoordinates(pvt2AfterMan);
            final FieldVector3D<T> deltaV2Inertial = pvt2AfterManInertial.getVelocity().subtract(pvt2BeforeManInertial.getVelocity());

            final FieldImpulseManeuver<T> maneuverInertial1 = new FieldImpulseManeuver<>(firstImpulseTrigger, deltaV1Inertial, Isp);
            final FieldImpulseManeuver<T> maneuverInertial2 = new FieldImpulseManeuver<>(secondImpulseTrigger, deltaV2Inertial, Isp);

            maneuvers.add(maneuverInertial1);
            maneuvers.add(maneuverInertial2);
        }
        return maneuvers;
    }
}
