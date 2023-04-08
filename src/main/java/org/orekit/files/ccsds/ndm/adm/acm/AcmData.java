/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.List;

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.Data;

/** Data container for Attitude Comprehensive Messages.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AcmData implements Data {

    /** Attitude state histories logical blocks. */
    private final List<AttitudeStateHistory> attitudeBlocks;

    /** Physical properties logical block. */
    private final AttitudePhysicalProperties physicBlock;

    /** Covariance logical blocks. */
    private final List<AttitudeCovarianceHistory> covarianceBlocks;

    /** Maneuvers logical blocks. */
    private final List<AttitudeManeuver> maneuverBlocks;

    /** Attitude determination logical block. */
    private final AttitudeDetermination attitudeDeterminationBlock;

    /** User defined parameters logical block. */
    private final UserDefined userDefinedBlock;

    /** Simple constructor.
     * @param attitudeBlocks attitude state histories logical blocks (may be empty)
     * @param physicBlock physical properties logical block (may be null)
     * @param covarianceBlocks covariance logical blocks (may be empty)
     * @param maneuverBlocks maneuvers logical blocks (may be empty)
     * @param attitudeDeterminationBlock attitude determination logical block (may be null)
     * @param userDefinedBlock user defined parameters logical block (may be null)
     */
    public AcmData(final List<AttitudeStateHistory>      attitudeBlocks,
                   final AttitudePhysicalProperties      physicBlock,
                   final List<AttitudeCovarianceHistory> covarianceBlocks,
                   final List<AttitudeManeuver>          maneuverBlocks,
                   final AttitudeDetermination           attitudeDeterminationBlock,
                   final UserDefined                     userDefinedBlock) {
        this.attitudeBlocks             = attitudeBlocks;
        this.physicBlock                = physicBlock;
        this.covarianceBlocks           = covarianceBlocks;
        this.maneuverBlocks             = maneuverBlocks;
        this.attitudeDeterminationBlock = attitudeDeterminationBlock;
        this.userDefinedBlock           = userDefinedBlock;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (attitudeBlocks != null) {
            for (final AttitudeStateHistory ash : attitudeBlocks) {
                ash.getMetadata().validate(version);
            }
        }
        if (physicBlock != null) {
            physicBlock.validate(version);
        }
        if (covarianceBlocks != null) {
            for (final AttitudeCovarianceHistory ch : covarianceBlocks) {
                ch.getMetadata().validate(version);
            }
        }
        if (maneuverBlocks != null) {
            for (final AttitudeManeuver m : maneuverBlocks) {
                m.validate(version);
            }
        }
        if (attitudeDeterminationBlock != null) {
            attitudeDeterminationBlock.validate(version);
        }
        if (userDefinedBlock != null) {
            userDefinedBlock.validate(version);
        }
    }

    /** Get attitude state histories logical blocks.
     * @return attitude state histories logical blocks (may be null)
     */
    public List<AttitudeStateHistory> getAttitudeBlocks() {
        return attitudeBlocks;
    }

    /** Get physical properties logical block.
     * @return physical properties logical block (may be null)
     */
    public AttitudePhysicalProperties getPhysicBlock() {
        return physicBlock;
    }

    /** Get covariance logical blocks.
     * @return covariance logical blocks (may be null)
     */
    public List<AttitudeCovarianceHistory> getCovarianceBlocks() {
        return covarianceBlocks;
    }

    /** Get maneuvers logical blocks.
     * @return maneuvers logical block (may be null)
     */
    public List<AttitudeManeuver> getManeuverBlocks() {
        return maneuverBlocks;
    }

    /** Get attitude determination logical block.
     * @return attitude determination logical block (may be null)
     */
    public AttitudeDetermination getAttitudeDeterminationBlock() {
        return attitudeDeterminationBlock;
    }

    /** Get user defined parameters logical block.
     * @return user defined parameters logical block (may be null)
     */
    public UserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

}
