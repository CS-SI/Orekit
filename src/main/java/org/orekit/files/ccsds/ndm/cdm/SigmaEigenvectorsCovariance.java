/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;

import java.util.Arrays;

/**
 * Container for Sigma/Eigenvectors Covariance data.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * <p>
 * The positional covariance one-sigma dispersions corresponding to the major,
 * intermediate and minor eigenvalues, followed by the associated eigenvectors.
 * The data is presented on a single line (12 values separated by spaces).
 * (Condition: Mandatory if {@code ALT_COV_TYPE = CSIG3EIGVEC3})
 * </p>
 */
public class SigmaEigenvectorsCovariance extends CommentsContainer {

    /** Sigma/Eigenvectors Covariance covariance matrix. */
    private double[] csig3eigvec3;

    /** Flag indicating whether the alternate covariance type set in the CDM Object metadata section is Sigma/Eigenvectors Covariance. */
    private final boolean altCovFlag;

    /** Simple constructor.
     * <p> The Sigma/Eigenvectors Covariance data is only provided if {@link CdmMetadataKey#ALT_COV_TYPE} is {@link AltCovarianceType#CSIG3EIGVEC3}, otherwise
     * its terms will return NaN. </p>
     * @param altCovFlag Flag indicating whether the alternate covariance type set in the CDM Object metadata section is Sigma/Eigenvectors Covariance.
     */
    public SigmaEigenvectorsCovariance(final boolean altCovFlag) {
        this.altCovFlag = altCovFlag;
        csig3eigvec3 = new double[12];

        Arrays.fill(csig3eigvec3, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);

        // Conditional on ALT_COV_TYPE = Sigma/Eigenvectors Covariance
        if (!isAltCovFlagSet()) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, CdmMetadataKey.ALT_COV_TYPE);
        }

        // We only check values that are mandatory in a cdm file
        for (int i = 0; i < getCsig3eigvec3().length; i++) {
            checkNotNaN(getCsig3eigvec3()[i], SigmaEigenvectorsCovarianceKey.CSIG3EIGVEC3.name());
        }
    }


    /**
     * Get the Sigma/Eigenvectors Covariance data.
     * <p> The Sigma/Eigenvectors Covariance data is only provided if {@link CdmMetadataKey#ALT_COV_TYPE} is {@link AltCovarianceType#CSIG3EIGVEC3}, otherwise
     * its terms will return NaN. </p>
     * @return covarianceData the covariance data in the Sigma/Eigenvectors format.
     */
    public double[] getCsig3eigvec3() {
        return csig3eigvec3 == null ? null : csig3eigvec3.clone();
    }

    /**
     * Set the Sigma/Eigenvectors Covariance data.
     * @param csig3eigvec3 the covariance data in the Sigma/Eigenvectors format.
     */
    public void setCsig3eigvec3(final double[] csig3eigvec3) {
        refuseFurtherComments();

        // Conditional on ALT_COV_TYPE = Sigma/Eigenvectors Covariance
        if (!isAltCovFlagSet()) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, CdmMetadataKey.ALT_COV_TYPE);
        }

        this.csig3eigvec3 = csig3eigvec3 == null ? null : csig3eigvec3.clone();
    }

    /** Get the flag indicating whether the alternate covariance type set in the CDM Object metadata section is Sigma/Eigenvectors Covariance.
     * @return the altCovFlag
     */
    public boolean isAltCovFlagSet() {
        return altCovFlag;
    }

}
