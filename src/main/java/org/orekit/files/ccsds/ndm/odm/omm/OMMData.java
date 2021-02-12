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

package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.ODMCovariance;
import org.orekit.files.ccsds.ndm.odm.ODMKeplerianElements;
import org.orekit.files.ccsds.ndm.odm.ODMKeplerianElementsKey;
import org.orekit.files.ccsds.ndm.odm.ODMSpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.ODMUserDefined;
import org.orekit.files.ccsds.section.Data;

/**
 * Container for Orbit Mean-elements Message data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OMMData implements Data {

    /** Keplerian elements block. */
    private final ODMKeplerianElements keplerianElementsBlock;

    /** Spacecraft parameters block. */
    private final ODMSpacecraftParameters spacecraftParameters;

    /** TLE block. */
    private final OMMTLE tleBlock;

    /** Covariance matrix logical block being read. */
    private final ODMCovariance covarianceBlock;

    /** User defined parameters. */
    private final ODMUserDefined userDefinedBlock;

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
    public OMMData(final ODMKeplerianElements keplerianElementsBlock,
                   final ODMSpacecraftParameters spacecraftParameters,
                   final OMMTLE tleBlock,
                   final ODMCovariance covarianceBlock,
                   final ODMUserDefined userDefinedBlock,
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
    public void checkMandatoryEntries() {
        keplerianElementsBlock.checkMandatoryEntries();
        if (spacecraftParameters != null) {
            spacecraftParameters.checkMandatoryEntries();
        }
        if (tleBlock == null) {
            // semi-major axis was not checked above, we do it now
            keplerianElementsBlock.checkNotNaN(keplerianElementsBlock.getA(),
                                               ODMKeplerianElementsKey.SEMI_MAJOR_AXIS);
        } else {
            // in OMM with TLE block, only mean motion is allowed, not semi-major axis
            keplerianElementsBlock.checkNotNaN(keplerianElementsBlock.getMeanMotion(),
                                               ODMKeplerianElementsKey.MEAN_MOTION);
            tleBlock.checkMandatoryEntries();
        }
        if (covarianceBlock != null) {
            covarianceBlock.setEpoch(keplerianElementsBlock.getEpoch());
            covarianceBlock.checkMandatoryEntries();
        }
        if (userDefinedBlock != null) {
            userDefinedBlock.checkMandatoryEntries();
        }
        if (keplerianElementsBlock == null && tleBlock == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                      ODMKeplerianElementsKey.EPOCH);
        }
    }

    /** Get the Keplerian elements logical block.
     * @return Keplerian elements block
     */
    public ODMKeplerianElements getKeplerianElementsBlock() {
        return keplerianElementsBlock;
    }

    /** Get the spacecraft parameters logical block.
     * @return spacecraft parameters block (may be null)
     */
    public ODMSpacecraftParameters getSpacecraftParametersBlock() {
        return spacecraftParameters;
    }

    /** Get the TLE logical block.
     * @return TLE block
     */
    public OMMTLE getTLEBlock() {
        return tleBlock;
    }

    /** Get the covariance matrix logical block.
     * @return covariance matrix block (may be null)
     */
    public ODMCovariance getCovarianceBlock() {
        return covarianceBlock;
    }

    /** Get the user defined parameters logical block.
     * @return user defined parameters block (may be null)
     */
    public ODMUserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

    /** Get the mass.
     * @return mass
     */
    public double getMass() {
        return mass;
    }

}
