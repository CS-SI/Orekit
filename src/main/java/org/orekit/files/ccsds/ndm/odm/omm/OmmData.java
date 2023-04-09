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

package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.KeplerianElementsKey;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.Data;

/**
 * Container for Orbit Mean-elements Message data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OmmData implements Data {

    /** Keplerian elements block. */
    private final KeplerianElements keplerianElementsBlock;

    /** Spacecraft parameters block. */
    private final SpacecraftParameters spacecraftParameters;

    /** TLE block. */
    private final OmmTle tleBlock;

    /** Covariance matrix logical block being read. */
    private final CartesianCovariance covarianceBlock;

    /** User defined parameters. */
    private final UserDefined userDefinedBlock;

    /** Mass. */
    private final double mass;

    /** Simple constructor.
     * @param keplerianElementsBlock Keplerian elements logical block
     * @param spacecraftParameters spacecraft parameters logical block (may be null)
     * @param tleBlock TLE logical block (may be null)
     * @param covarianceBlock covariance matrix logical block (may be null)
     * @param userDefinedBlock user-defined logical block
     * @param mass mass (always defined, even if there is no {@code spacecraftParameters} block
     */
    public OmmData(final KeplerianElements keplerianElementsBlock,
                   final SpacecraftParameters spacecraftParameters,
                   final OmmTle tleBlock,
                   final CartesianCovariance covarianceBlock,
                   final UserDefined userDefinedBlock,
                   final double mass) {
        this.keplerianElementsBlock = keplerianElementsBlock;
        this.spacecraftParameters   = spacecraftParameters;
        this.tleBlock               = tleBlock;
        this.covarianceBlock        = covarianceBlock;
        this.userDefinedBlock       = userDefinedBlock;
        this.mass                   = mass;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (keplerianElementsBlock == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                      KeplerianElementsKey.EPOCH);
        }
        keplerianElementsBlock.validate(version);
        if (spacecraftParameters != null) {
            spacecraftParameters.validate(version);
        }
        if (tleBlock == null) {
            // semi-major axis was not checked above, we do it now
            keplerianElementsBlock.checkNotNaN(keplerianElementsBlock.getA(),
                                               KeplerianElementsKey.SEMI_MAJOR_AXIS.name());
        } else {
            // in OMM with TLE block, only mean motion is allowed, not semi-major axis
            keplerianElementsBlock.checkNotNaN(keplerianElementsBlock.getMeanMotion(),
                                               KeplerianElementsKey.MEAN_MOTION.name());
            tleBlock.validate(version);
        }
        if (covarianceBlock != null) {
            covarianceBlock.setEpoch(keplerianElementsBlock.getEpoch());
            covarianceBlock.validate(version);
        }
        if (userDefinedBlock != null) {
            userDefinedBlock.validate(version);
        }
    }

    /** Get the Keplerian elements logical block.
     * @return Keplerian elements block
     */
    public KeplerianElements getKeplerianElementsBlock() {
        return keplerianElementsBlock;
    }

    /** Get the spacecraft parameters logical block.
     * @return spacecraft parameters block (may be null)
     */
    public SpacecraftParameters getSpacecraftParametersBlock() {
        return spacecraftParameters;
    }

    /** Get the TLE logical block.
     * @return TLE block
     */
    public OmmTle getTLEBlock() {
        return tleBlock;
    }

    /** Get the covariance matrix logical block.
     * @return covariance matrix block (may be null)
     */
    public CartesianCovariance getCovarianceBlock() {
        return covarianceBlock;
    }

    /** Get the user defined parameters logical block.
     * @return user defined parameters block (may be null)
     */
    public UserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

    /** Get the mass.
     * @return mass
     */
    public double getMass() {
        return mass;
    }

}
