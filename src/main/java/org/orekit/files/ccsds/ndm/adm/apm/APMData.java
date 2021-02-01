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

import org.orekit.files.ccsds.section.Data;
import org.orekit.files.ccsds.section.Section;

/**
 * Container for Attitude Parameter Message data.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMData implements Data, Section {

    /** Quaternion block. */
    private final APMQuaternion quaternionBlock;

    /** Euler angles block. */
    private final APMEuler eulerBlock;

    /** Spin-stabilized block. */
    private final APMSpinStabilized spinStabilizedBlock;

    /** Spacecraft parameters block. */
    private final APMSpacecraftParameters spacecraftParameters;

    /** Maneuvers. */
    private List<APMManeuver> maneuvers;

    /** Simple constructor.
     * @param quaternionBlock quaternion logical block
     * @param eulerBlock Euler angles logicial block (may be null)
     * @param spinStabilizedBlock spin-stabilized logical block (may be null)
     * @param spacecraftParameters spacecraft parameters logical block (may be null)
     */
    public APMData(final APMQuaternion quaternionBlock,
                   final APMEuler eulerBlock,
                   final APMSpinStabilized spinStabilizedBlock,
                   final APMSpacecraftParameters spacecraftParameters) {
        this.quaternionBlock      = quaternionBlock;
        this.eulerBlock           = eulerBlock;
        this.spinStabilizedBlock  = spinStabilizedBlock;
        this.spacecraftParameters = spacecraftParameters;
        this.maneuvers            = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        quaternionBlock.checkMandatoryEntries();
        if (eulerBlock != null) {
            eulerBlock.checkMandatoryEntries();
        }
        if (spinStabilizedBlock != null) {
            spinStabilizedBlock.checkMandatoryEntries();
        }
        if (spacecraftParameters != null) {
            spacecraftParameters.checkMandatoryEntries();
        }
        for (final APMManeuver maneuver : maneuvers) {
            maneuver.checkMandatoryEntries();
        }
    }

    /** Get the quaternion logical block.
     * @return quaternion block
     */
    public APMQuaternion getQuaternionBlock() {
        return quaternionBlock;
    }

    /** Get the Euler angles logical block.
     * @return Euler angles block (may be null)
     */
    public APMEuler getEulerBlock() {
        return eulerBlock;
    }

    /** Get the spin-stabilized logical block.
     * @return spin-stabilized block (may be null)
     */
    public APMSpinStabilized getSpinStabilizedBlock() {
        return spinStabilizedBlock;
    }

    /** Get the spacecraft parameters logical block.
     * @return spacecraft parameters block (may be null)
     */
    public APMSpacecraftParameters getSpacecraftParametersBlock() {
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
    public List<APMManeuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /**
     * Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public APMManeuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /**
     * Add a maneuver.
     * @param maneuver maneuver to be set
     */
    public void addManeuver(final APMManeuver maneuver) {
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
