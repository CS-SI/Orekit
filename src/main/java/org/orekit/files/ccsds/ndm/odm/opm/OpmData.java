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
package org.orekit.files.ccsds.ndm.odm.opm;

import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.KeplerianElementsKey;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.StateVector;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.Data;

/**
 * Container for Orbit Parameter Message data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OpmData implements Data {

    /** State vector block. */
    private final  StateVector stateVectorBlock;

    /** Keplerian elements block. */
    private final KeplerianElements keplerianElementsBlock;

    /** Spacecraft parameters block. */
    private final SpacecraftParameters spacecraftParametersBlock;

    /** Covariance matrix logical block being read. */
    private final CartesianCovariance covarianceBlock;

    /** Maneuvers. */
    private final List<Maneuver> maneuverBlocks;

    /** User defined parameters. */
    private final UserDefined userDefinedBlock;

    /** Mass. */
    private final double mass;

    /** Simple constructor.
     * @param stateVectorBlock state vector logical block
     * @param keplerianElementsBlock Keplerian elements logical block (may be null)
     * @param spacecraftParametersBlock spacecraft parameters logical block (may be null)
     * @param covarianceBlock covariance matrix logical block (may be null)
     * @param maneuverBlocks maneuvers block list
     * @param userDefinedBlock user-defined logical block
     * @param mass mass (always defined, even if there is no {@code spacecraftParameters} block
     */
    public OpmData(final StateVector stateVectorBlock,
                   final KeplerianElements keplerianElementsBlock,
                   final SpacecraftParameters spacecraftParametersBlock,
                   final CartesianCovariance covarianceBlock,
                   final List<Maneuver> maneuverBlocks,
                   final UserDefined userDefinedBlock,
                   final double mass) {
        this.stateVectorBlock          = stateVectorBlock;
        this.keplerianElementsBlock    = keplerianElementsBlock;
        this.spacecraftParametersBlock = spacecraftParametersBlock;
        this.covarianceBlock           = covarianceBlock;
        this.maneuverBlocks            = maneuverBlocks;
        this.userDefinedBlock          = userDefinedBlock;
        this.mass                      = mass;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        stateVectorBlock.validate(version);
        if (keplerianElementsBlock != null) {
            keplerianElementsBlock.validate(version);
            // in OPM, only semi-major axis is allowed, not mean motion
            keplerianElementsBlock.checkNotNaN(keplerianElementsBlock.getA(),
                                               KeplerianElementsKey.SEMI_MAJOR_AXIS.name());
        }
        if (spacecraftParametersBlock != null) {
            spacecraftParametersBlock.validate(version);
        }
        if (covarianceBlock != null) {
            covarianceBlock.setEpoch(stateVectorBlock.getEpoch());
            covarianceBlock.validate(version);
        }
        for (final Maneuver maneuver : maneuverBlocks) {
            maneuver.validate(version);
        }
        if (userDefinedBlock != null) {
            userDefinedBlock.validate(version);
        }
    }

    /** Get the state vector logical block.
     * @return state vector block
     */
    public StateVector getStateVectorBlock() {
        return stateVectorBlock;
    }

    /** Get the Keplerian elements logical block.
     * @return Keplerian elements block (may be null)
     */
    public KeplerianElements getKeplerianElementsBlock() {
        return keplerianElementsBlock;
    }

    /** Get the spacecraft parameters logical block.
     * @return spacecraft parameters block (may be null)
     */
    public SpacecraftParameters getSpacecraftParametersBlock() {
        return spacecraftParametersBlock;
    }

    /** Get the covariance matrix logical block.
     * @return covariance matrix block (may be null)
     */
    public CartesianCovariance getCovarianceBlock() {
        return covarianceBlock;
    }

    /** Get the mass.
     * @return mass
     */
    public double getMass() {
        return mass;
    }

    /**
     * Get the number of maneuvers present in the APM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuverBlocks.size();
    }

    /**
     * Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<Maneuver> getManeuvers() {
        return Collections.unmodifiableList(maneuverBlocks);
    }

    /**
     * Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return maneuverBlocks.get(index);
    }

    /**
     * Get boolean testing whether the APM contains at least one maneuver.
     * @return true if APM contains at least one maneuver
     *         false otherwise
     */
    public boolean hasManeuvers() {
        return !maneuverBlocks.isEmpty();
    }

    /** Get the user defined parameters logical block.
     * @return user defined parameters block (may be null)
     */
    public UserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

}
