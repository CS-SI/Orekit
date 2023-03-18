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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.time.AbsoluteDate;

/**
 * Container for Attitude Parameter Message data.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmData implements Data {

    /** General comments block. */
    private final CommentsContainer commentsBlock;

    /** Epoch of the data. */
    private final AbsoluteDate epoch;

    /** Quaternion block. */
    private final ApmQuaternion quaternionBlock;

    /** Euler angles block. */
    private final Euler eulerBlock;

    /** Angular velocity block.
     * @since 12.0
     */
    private final AngularVelocity angularVelocityBlock;

    /** Spin-stabilized block. */
    private final SpinStabilized spinStabilizedBlock;

    /** Inertia block. */
    private final Inertia inertia;

    /** Maneuvers. */
    private final List<Maneuver> maneuvers;

    /** Simple constructor.
     * @param commentsBlock general comments block
     * @param epoch epoch of the data
     * @param quaternionBlock quaternion logical block (may be null)
     * @param eulerBlock Euler angles logicial block (may be null)
     * @param angularVelocityBlock angular velocity block (may be null)
     * @param spinStabilizedBlock spin-stabilized logical block (may be null)
     * @param spacecraftParameters spacecraft parameters logical block (may be null)
     */
    public ApmData(final CommentsContainer commentsBlock,
                   final AbsoluteDate epoch,
                   final ApmQuaternion quaternionBlock,
                   final Euler eulerBlock,
                   final AngularVelocity angularVelocityBlock,
                   final SpinStabilized spinStabilizedBlock,
                   final Inertia spacecraftParameters) {
        this.commentsBlock        = commentsBlock;
        this.epoch                = epoch;
        this.quaternionBlock      = quaternionBlock;
        this.eulerBlock           = eulerBlock;
        this.angularVelocityBlock = angularVelocityBlock;
        this.spinStabilizedBlock  = spinStabilizedBlock;
        this.inertia = spacecraftParameters;
        this.maneuvers            = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (quaternionBlock != null) {
            quaternionBlock.validate(version);
        }
        if (eulerBlock != null) {
            eulerBlock.validate(version);
        }
        if (angularVelocityBlock != null) {
            angularVelocityBlock.validate(version);
        }
        if (spinStabilizedBlock != null) {
            spinStabilizedBlock.validate(version);
        }
        if (inertia != null) {
            inertia.validate(version);
        }
        for (final Maneuver maneuver : maneuvers) {
            maneuver.validate(version);
        }
    }

    /** Get the comments.
     * @return comments
     */
    public List<String> getComments() {
        return commentsBlock.getComments();
    }

    /**
     * Get the epoch of the data.
     * @return epoch the epoch
     * @since 12.0
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Get the quaternion logical block.
     * @return quaternion block
     */
    public ApmQuaternion getQuaternionBlock() {
        return quaternionBlock;
    }

    /** Get the Euler angles logical block.
     * @return Euler angles block (may be null)
     */
    public Euler getEulerBlock() {
        return eulerBlock;
    }

    /** Get the angular velocity logical block.
     * @return angular velocity block (may be null)
     * @since 12.0
     */
    public AngularVelocity getAngularVelocityBlock() {
        return angularVelocityBlock;
    }

    /** Get the spin-stabilized logical block.
     * @return spin-stabilized block (may be null)
     */
    public SpinStabilized getSpinStabilizedBlock() {
        return spinStabilizedBlock;
    }

    /** Get the inertia logical block.
     * @return inertia block (may be null)
     */
    public Inertia getInertiaBlock() {
        return inertia;
    }

    /**
     * Get the number of maneuvers present in the APM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuvers.size();
    }

    /**
     * Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<Maneuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /**
     * Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /**
     * Add a maneuver.
     * @param maneuver maneuver to be set
     */
    public void addManeuver(final Maneuver maneuver) {
        maneuvers.add(maneuver);
    }

    /**
     * Get boolean testing whether the APM contains at least one maneuver.
     * @return true if APM contains at least one maneuver
     *         false otherwise
     */
    public boolean hasManeuvers() {
        return !maneuvers.isEmpty();
    }

}
