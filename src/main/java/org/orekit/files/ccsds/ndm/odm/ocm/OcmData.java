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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.Data;

/** Data container for Orbit Comprehensive Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OcmData implements Data {

    /** Trajectory state histories logical blocks. */
    private final List<TrajectoryStateHistory> trajectoryBlocks;

    /** Physical properties logical block. */
    private final OrbitPhysicalProperties physicBlock;

    /** Covariance logical blocks. */
    private final List<OrbitCovarianceHistory> covarianceBlocks;

    /** Maneuvers logical blocks. */
    private final List<OrbitManeuverHistory> maneuverBlocks;

    /** Perturbations logical block. */
    private final Perturbations perturbationsBlock;

    /** Orbit determination logical block. */
    private final OrbitDetermination orbitDeterminationBlock;

    /** User defined parameters logical block. */
    private final UserDefined userDefinedBlock;

    /** Simple constructor.
     * @param trajectoryBlocks trajectory state histories logical blocks (may be empty)
     * @param physicBlock physical properties logical block (may be null)
     * @param covarianceBlocks covariance logical blocks (may be empty)
     * @param maneuverBlocks maneuvers logical blocks (may be empty)
     * @param perturbationsBlock perturbations logical block (may be null)
     * @param orbitDeterminationBlock orbit determination logical block (may be null)
     * @param userDefinedBlock user defined parameters logical block (may be null)
     */
    public OcmData(final List<TrajectoryStateHistory> trajectoryBlocks,
                   final OrbitPhysicalProperties           physicBlock,
                   final List<OrbitCovarianceHistory>      covarianceBlocks,
                   final List<OrbitManeuverHistory>        maneuverBlocks,
                   final Perturbations                perturbationsBlock,
                   final OrbitDetermination           orbitDeterminationBlock,
                   final UserDefined                  userDefinedBlock) {
        this.trajectoryBlocks         = trajectoryBlocks;
        this.physicBlock              = physicBlock;
        this.covarianceBlocks         = covarianceBlocks;
        this.maneuverBlocks           = maneuverBlocks;
        this.perturbationsBlock       = perturbationsBlock;
        this.orbitDeterminationBlock  = orbitDeterminationBlock;
        this.userDefinedBlock         = userDefinedBlock;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (trajectoryBlocks != null) {
            for (final TrajectoryStateHistory osh : trajectoryBlocks) {
                osh.getMetadata().validate(version);
            }
        }
        if (physicBlock != null) {
            physicBlock.validate(version);
        }
        if (covarianceBlocks != null) {
            for (final OrbitCovarianceHistory ch : covarianceBlocks) {
                ch.getMetadata().validate(version);
            }
        }
        if (maneuverBlocks != null) {
            for (final OrbitManeuverHistory mh : maneuverBlocks) {
                mh.getMetadata().validate(version);
            }
        }
        if (perturbationsBlock != null) {
            perturbationsBlock.validate(version);
        }
        if (orbitDeterminationBlock != null) {
            orbitDeterminationBlock.validate(version);
        }
        if (userDefinedBlock != null) {
            userDefinedBlock.validate(version);
        }
    }

    /** Get trajectory state histories logical blocks.
     * @return trajectory state histories logical blocks (may be null)
     * @since 12.0
     */
    public List<TrajectoryStateHistory> getTrajectoryBlocks() {
        return trajectoryBlocks;
    }

    /** Get physical properties logical block.
     * @return physical properties logical block (may be null)
     */
    public OrbitPhysicalProperties getPhysicBlock() {
        return physicBlock;
    }

    /** Get covariance logical blocks.
     * @return covariance logical blocks (may be null)
     */
    public List<OrbitCovarianceHistory> getCovarianceBlocks() {
        return covarianceBlocks;
    }

    /** Get maneuvers logical blocks.
     * @return maneuvers logical block (may be null)
     */
    public List<OrbitManeuverHistory> getManeuverBlocks() {
        return maneuverBlocks;
    }

    /** Get perturbations logical block.
     * @return perturbations logical block (may be null)
     */
    public Perturbations getPerturbationsBlock() {
        return perturbationsBlock;
    }

    /** Get orbit determination logical block.
     * @return orbit determination logical block (may be null)
     */
    public OrbitDetermination getOrbitDeterminationBlock() {
        return orbitDeterminationBlock;
    }

    /** Get user defined parameters logical block.
     * @return user defined parameters logical block (may be null)
     */
    public UserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

}
