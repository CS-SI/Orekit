/* Copyright 2002-2021 CS GROUP
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

/**
 * Container for Attitude Parameter Message data.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmData implements Data {

    /** General comments block. */
    private final CommentsContainer commentsBlock;

    /** Quaternion block. */
    private final ApmQuaternion quaternionBlock;

    /** Euler angles block. */
    private final Euler eulerBlock;

    /** Spin-stabilized block. */
    private final SpinStabilized spinStabilizedBlock;

    /** Spacecraft parameters block. */
    private final SpacecraftParameters spacecraftParameters;

    /** Maneuvers. */
    private final List<Maneuver> maneuvers;

    /** Simple constructor.
     * @param commentsBlock general comments block
     * @param quaternionBlock quaternion logical block
     * @param eulerBlock Euler angles logicial block (may be null)
     * @param spinStabilizedBlock spin-stabilized logical block (may be null)
     * @param spacecraftParameters spacecraft parameters logical block (may be null)
     */
    public ApmData(final CommentsContainer commentsBlock,
                   final ApmQuaternion quaternionBlock,
                   final Euler eulerBlock,
                   final SpinStabilized spinStabilizedBlock,
                   final SpacecraftParameters spacecraftParameters) {
        this.commentsBlock        = commentsBlock;
        this.quaternionBlock      = quaternionBlock;
        this.eulerBlock           = eulerBlock;
        this.spinStabilizedBlock  = spinStabilizedBlock;
        this.spacecraftParameters = spacecraftParameters;
        this.maneuvers            = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        quaternionBlock.validate(version);
        if (eulerBlock != null) {
            eulerBlock.validate(version);
        }
        if (spinStabilizedBlock != null) {
            spinStabilizedBlock.validate(version);
        }
        if (spacecraftParameters != null) {
            spacecraftParameters.validate(version);
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

    /** Get the spin-stabilized logical block.
     * @return spin-stabilized block (may be null)
     */
    public SpinStabilized getSpinStabilizedBlock() {
        return spinStabilizedBlock;
    }

    /** Get the spacecraft parameters logical block.
     * @return spacecraft parameters block (may be null)
     */
    public SpacecraftParameters getSpacecraftParametersBlock() {
        return spacecraftParameters;
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
