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
package org.orekit.files.ccsds.ndm.cdm;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for XYZ covariance matrix data. This class as a RealMatrix as
 * attribute which can be acces with getXYZCovariaxMatrix method. Beware that
 * there are thus 2 ways to modify the XYZ covariance : setC... ( setCxx,
 * setCyx ...) which should be prioritized and getXYZCovariaxMatrix.setEntry(row, col, value).
 * <p> The XYZ Covariance Matrix is only provided if {@link CdmMetadataKey#ALT_COV_TYPE} is {@link AltCovarianceType#XYZ}, otherwise
 * its terms will return NaN. </p>
 * <p> When available, the matrix is given in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
 * are mandatory. The remaining elements will return NaN if not provided.</p>
 */
public class XYZCovariance extends CommentsContainer {

    /** XYZ covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Flag indicating whether the alternate covariance type set in the CDM Object metadata section is XYZ. */
    private boolean covXYZset;

    /** Simple constructor. To update matrix value there are 2 ways to modify the XYZ
     * covariance : setC... ( setCxx, setCyx ...) which should be prioritized and
     * getXYZCovariaxMatrix.setEntry(row, col, value).
     * <p> The XYZ Covariance Matrix is only provided if {@link CdmMetadataKey#ALT_COV_TYPE} is {@link AltCovarianceType#XYZ}, otherwise
     * its terms will return NaN. </p>
     * <p> When available, the matrix is given in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
     * are mandatory. The remaining elements will return NaN if not provided.</p>
     * @param covXYZset Flag indicating whether the alternate covariance type set in the CDM Object metadata section is XYZ.
     */
    public XYZCovariance(final boolean covXYZset) {
        this.covXYZset = covXYZset;
        covarianceMatrix = MatrixUtils.createRealMatrix(9, 9);
        for (int i = 0; i < covarianceMatrix.getRowDimension(); ++i) {
            for (int j = 0; j <= i; ++j) {
                covarianceMatrix.setEntry(i, j, Double.NaN);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);

        // Conditional on ALT_COV_TYPE = XYZ
        if (!isCovXYZset()) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, CdmMetadataKey.ALT_COV_TYPE);
        }

        // We only check values that are mandatory in a cdm file
        checkNotNaN(getCxx(),              XYZCovarianceKey.CX_X.name());
        checkNotNaN(getCyx(),              XYZCovarianceKey.CY_X.name());
        checkNotNaN(getCyy(),              XYZCovarianceKey.CY_Y.name());
        checkNotNaN(getCzx(),              XYZCovarianceKey.CZ_X.name());
        checkNotNaN(getCzy(),              XYZCovarianceKey.CZ_Y.name());
        checkNotNaN(getCzz(),              XYZCovarianceKey.CZ_Z.name());
        checkNotNaN(getCxdotx(),           XYZCovarianceKey.CXDOT_X.name());
        checkNotNaN(getCxdoty(),           XYZCovarianceKey.CXDOT_Y.name());
        checkNotNaN(getCxdotz(),           XYZCovarianceKey.CXDOT_Z.name());
        checkNotNaN(getCxdotxdot(),        XYZCovarianceKey.CXDOT_XDOT.name());
        checkNotNaN(getCydotx(),           XYZCovarianceKey.CYDOT_X.name());
        checkNotNaN(getCydoty(),           XYZCovarianceKey.CYDOT_Y.name());
        checkNotNaN(getCydotz(),           XYZCovarianceKey.CYDOT_Z.name());
        checkNotNaN(getCydotxdot(),        XYZCovarianceKey.CYDOT_XDOT.name());
        checkNotNaN(getCydotydot(),        XYZCovarianceKey.CYDOT_YDOT.name());
        checkNotNaN(getCzdotx(),           XYZCovarianceKey.CZDOT_X.name());
        checkNotNaN(getCzdoty(),           XYZCovarianceKey.CZDOT_Y.name());
        checkNotNaN(getCzdotz(),           XYZCovarianceKey.CZDOT_Z.name());
        checkNotNaN(getCzdotxdot(),        XYZCovarianceKey.CZDOT_XDOT.name());
        checkNotNaN(getCzdotydot(),        XYZCovarianceKey.CZDOT_YDOT.name());
        checkNotNaN(getCzdotzdot(),        XYZCovarianceKey.CZDOT_ZDOT.name());
    }

    /** Set an entry in the XYZ covariance matrix.
     * <p>
     * Both m(j, k) and m(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 5 (inclusive)
     * @param k column index (must be between 0 and 5 (inclusive)
     * @param entry value of the matrix entry
     */
    public void setCovarianceMatrixEntry(final int j, final int k, final double entry) {
        covarianceMatrix.setEntry(j, k, entry);
        covarianceMatrix.setEntry(k, j, entry);
    }

    /**
     * Get the XYZ covariance matrix.
     * <p> The XYZ Covariance Matrix is only provided if {@link CdmMetadataKey#ALT_COV_TYPE} is {@link AltCovarianceType#XYZ}, otherwise
     * its terms will return NaN. </p>
     * <p> When available, the matrix is given in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
     * are mandatory. The remaining elements will return NaN if not provided.</p>
     * @return the XYZ covariance matrix
     */
    public RealMatrix getXYZCovarianceMatrix() {
        return covarianceMatrix;
    }

    /**
     * Get the object [1,1] in covariance matrix (with index starting at 1).
     * @return the object [1,1] in covariance matrix (in m²)
     */
    public double getCxx() {
        return covarianceMatrix.getEntry(0, 0);
    }

    /**
     * Set the object [1,1] in covariance matrix (with index starting at 1).
     * @param CXX = object [1,1] in covariance matrix (in m²)
     */
    public void setCxx(final double CXX) {
        refuseFurtherComments();

        // Conditional on ALT_COV_TYPE = XYZ
        if (!isCovXYZset()) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, CdmMetadataKey.ALT_COV_TYPE);
        }

        setCovarianceMatrixEntry(0, 0, CXX);
    }

    /**
     * Get the object [2,1] in covariance matrix (with index starting at 1).
     * @return the object [2,1] in covariance matrix (in m²)
     */
    public double getCyx() {
        return covarianceMatrix.getEntry(1, 0);
    }

    /**
     * Set the object [2,1] in covariance matrix (with index starting at 1).
     * @param CYX = object [2,1] in covariance matrix (in m²)
     */
    public void setCyx(final double CYX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(1, 0, CYX);
    }

    /**
     * Get the object [2,2] in covariance matrix (with index starting at 1).
     * @return the object [2,2] in covariance matrix (in m²)
     */
    public double getCyy() {
        return covarianceMatrix.getEntry(1, 1);
    }

    /**
     * Set the object [2,2] in covariance matrix (with index starting at 1).
     * @param CYY = object [2,2] in covariance matrix (in m²)
     */
    public void setCyy(final double CYY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(1, 1, CYY);
    }

    /**
     * Get the object [3,1] in covariance matrix (with index starting at 1).
     * @return the object [3,1] in covariance matrix (in m²)
     */
    public double getCzx() {
        return covarianceMatrix.getEntry(2, 0);
    }

    /**
     * Set the object [3,1] in covariance matrix (with index starting at 1).
     * @param CZX = object [3,1] in covariance matrix (in m²)
     */
    public void setCzx(final double CZX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 0, CZX);
    }

    /**
     * Get the object [3,2] in covariance matrix (with index starting at 1).
     * @return the object [3,2] in covariance matrix (in m²)
     */
    public double getCzy() {
        return covarianceMatrix.getEntry(2, 1);
    }

    /**
     * Set the object [3,2] in covariance matrix (with index starting at 1).
     * @param CZY = object [3,2] in covariance matrix (in m²)
     */
    public void setCzy(final double CZY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 1, CZY);
    }

    /**
     * Get the object [3,3] in covariance matrix (with index starting at 1).
     * @return the object [3,3] in covariance matrix (in m²)
     */
    public double getCzz() {
        return covarianceMatrix.getEntry(2, 2);
    }

    /**
     * Set the object [3,3] in covariance matrix (with index starting at 1).
     * @param CZZ = object [3,3] in covariance matrix (in m²)
     */
    public void setCzz(final double CZZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 2, CZZ);
    }

    /**
     * Get the object [4,1] in covariance matrix (with index starting at 1).
     * @return the object [4,1] in covariance matrix (in m²/s)
     */
    public double getCxdotx() {
        return covarianceMatrix.getEntry(3, 0);
    }

    /**
     * Set the object [4,1] in covariance matrix (with index starting at 1).
     * @param CXdotX = object [4,1] in covariance matrix (in m²/s)
     */
    public void setCxdotx(final double CXdotX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 0, CXdotX);
    }

    /**
     * Get the object [4,2] in covariance matrix (with index starting at 1).
     * @return the object [4,2] in covariance matrix (in m²/s)
     */
    public double getCxdoty() {
        return covarianceMatrix.getEntry(3, 1);
    }

    /**
     * Set the object [4, 2] in covariance matrix (with index starting at 1).
     * @param CXdotY = object [4, 2] in covariance matrix (in m²/s)
     */
    public void setCxdoty(final double CXdotY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 1, CXdotY);
    }

    /**
     * Get the object [4, 3] in covariance matrix (with index starting at 1) .
     * @return the object [4, 3] in covariance matrix (in m²/s)
     */
    public double getCxdotz() {
        return covarianceMatrix.getEntry(3, 2);
    }

    /**
     * Set the object [4, 3] in covariance matrix (with index starting at 1).
     * @param CXdotZ = object [4,3] in covariance matrix (in m²/s)
     */
    public void setCxdotz(final double CXdotZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 2, CXdotZ);
    }

    /**
     * Get the object [4, 4] in covariance matrix (with index starting at 1).
     * @return the object [4, 4] in covariance matrix (in m²/s²)
     */
    public double getCxdotxdot() {
        return covarianceMatrix.getEntry(3, 3);
    }

    /**
     * Set the object [4, 4] in covariance matrix (with index starting at 1).
     * @param CXdotXdot = object [4, 4] in covariance matrix (in m²/s²)
     */
    public void setCxdotxdot(final double CXdotXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 3, CXdotXdot);
    }

    /**
     * Get the object [5, 1] in covariance matrix (with index starting at 1).
     * @return the object [5, 1] in covariance matrix (in m²/s)
     */
    public double getCydotx() {
        return covarianceMatrix.getEntry(4, 0);
    }

    /**
     * Set the object [5,1] in covariance matrix (with index starting at 1).
     * @param CYdotX = object [5,1] in covariance matrix (in m²/s)
     */
    public void setCydotx(final double CYdotX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 0, CYdotX);
    }

    /**
     * Get the object [5,2] in covariance matrix (with index starting at 1).
     * @return the object [5,2] in covariance matrix (in m²/s)
     */
    public double getCydoty() {
        return covarianceMatrix.getEntry(4, 1);
    }

    /**
     * Set the object [5,2] in covariance matrix (with index starting at 1).
     * @param CYdotY = object [5,2] in covariance matrix (in m²/s)
     */
    public void setCydoty(final double CYdotY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 1, CYdotY);
    }

    /**
     * Get the object [5,3] in covariance matrix (with index starting at 1).
     * @return the object [5,3] in covariance matrix (in m²/s)
     */
    public double getCydotz() {
        return covarianceMatrix.getEntry(4, 2);
    }

    /**
     * Set the object [5,3] in covariance matrix (with index starting at 1).
     * @param CYdotZ = object [5,3] in covariance matrix (in m²/s)
     */
    public void setCydotz(final double CYdotZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 2, CYdotZ);
    }

    /**
     * Get the object [5,4] in covariance matrix (with index starting at 1).
     * @return the object [5,4] in covariance matrix (in m²/s²)
     */
    public double getCydotxdot() {
        return covarianceMatrix.getEntry(4, 3);
    }

    /**
     * Set the object [5,4] in covariance matrix (with index starting at 1).
     * @param CYdotXdot = object [5,4] in covariance matrix (in m²/s²)
     */
    public void setCydotxdot(final double CYdotXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 3, CYdotXdot);
    }

    /**
     * Get the object [5,5] in covariance matrix (with index starting at 1).
     * @return the object [5,5] in covariance matrix (in m²/s²)
     */
    public double getCydotydot() {
        return covarianceMatrix.getEntry(4, 4);
    }

    /**
     * Set the object [5,5] in covariance matrix (with index starting at 1).
     * @param CYdotYdot = object [5,5] in covariance matrix (in m²/s²)
     */
    public void setCydotydot(final double CYdotYdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 4, CYdotYdot);
    }

    /**
     * Get the object [6,1] in covariance matrix (with index starting at 1).
     * @return the object [6,1] in covariance matrix (in m²/s)
     */
    public double getCzdotx() {
        return covarianceMatrix.getEntry(5, 0);
    }

    /**
     * Set the object [6,1] in covariance matrix (with index starting at 1).
     * @param CZdotX = object [6,1] in covariance matrix (in m²/s)
     */
    public void setCzdotx(final double CZdotX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 0, CZdotX);
    }

    /**
     * Get the object [6,2] in covariance matrix (with index starting at 1).
     * @return the object [6,2] in covariance matrix (in m²/s)
     */
    public double getCzdoty() {
        return covarianceMatrix.getEntry(5, 1);
    }

    /**
     * Set the object [6,2] in covariance matrix (with index starting at 1).
     * @param CZdotY = object [6,2] in covariance matrix (in m²/s)
     */
    public void setCzdoty(final double CZdotY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 1, CZdotY);
    }

    /**
     * Get the object [6,3] in covariance matrix (with index starting at 1).
     * @return the object [6,3] in covariance matrix (in m²/s)
     */
    public double getCzdotz() {
        return covarianceMatrix.getEntry(5, 2);
    }

    /**
     * Set the object [6,3] in covariance matrix (with index starting at 1).
     * @param CZdotZ = object [6,3] in covariance matrix (in m²/s)
     */
    public void setCzdotz(final double CZdotZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 2, CZdotZ);
    }

    /**
     * Get the object [6,4] in covariance matrix (with index starting at 1).
     * @return the object [6,4] in covariance matrix (in m²/s²)
     */
    public double getCzdotxdot() {
        return covarianceMatrix.getEntry(5, 3);
    }

    /**
     * Set the object [6,4] in covariance matrix (with index starting at 1).
     * @param CZdotXdot = object [6,4] in covariance matrix (in m²/s²)
     */
    public void setCzdotxdot(final double CZdotXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 3, CZdotXdot);
    }

    /**
     * Get the object [6,5] in covariance matrix (with index starting at 1).
     * @return the object [6,5] in covariance matrix (in m²/s²)
     */
    public double getCzdotydot() {
        return covarianceMatrix.getEntry(5, 4);
    }

    /**
     * Set the object [6,5] in covariance matrix (with index starting at 1).
     * @param CZdotYdot = object [6,5] in covariance matrix (in m²/s²)
     */
    public void setCzdotydot(final double CZdotYdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 4, CZdotYdot);
    }

    /**
     * Get the object [6,6] in covariance matrix (with index starting at 1).
     * @return the object [6,6] in covariance matrix (in m²/s²)
     */
    public double getCzdotzdot() {
        return covarianceMatrix.getEntry(5, 5);
    }

    /**
     * Set the object [6,6] in covariance matrix (with index starting at 1).
     * @param CZdotZdot = object [6,6] in covariance matrix (in m²/s²)
     */
    public void setCzdotzdot(final double CZdotZdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 5, CZdotZdot);
    }

    /**
     * Get the object [7,1] in covariance matrix (with index starting at 1).
     * @return the object [7,1] in covariance matrix (in m³/kg)
     */
    public double getCdrgx() {
        return covarianceMatrix.getEntry(6, 0);
    }

    /**
     * Set the object [7,1] in covariance matrix (with index starting at 1).
     * @param CDRGX = object [7,1] in covariance matrix (in m³/kg)
     */
    public void setCdrgx(final double CDRGX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 0, CDRGX);
    }

    /**
     * Get the object [7,2] in covariance matrix.
     * @return the object [7,2] in covariance matrix (in m³/kg)
     */
    public double getCdrgy() {
        return covarianceMatrix.getEntry(6, 1);
    }

    /**
     * Set the object [7,2] in covariance matrix (with index starting at 1).
     * @param CDRGY = object [7,2] in covariance matrix (in m³/kg)
     */
    public void setCdrgy(final double CDRGY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 1, CDRGY);
    }

    /**
     * Get the object [7,3] in covariance matrix (with index starting at 1).
     * @return the object [7,3] in covariance matrix (in m³/kg)
     */
    public double getCdrgz() {
        return covarianceMatrix.getEntry(6, 2);
    }

    /**
     * Set the object [7,3] in covariance matrix (with index starting at 1).
     * @param CDRGZ = object [7,3] in covariance matrix (in m³/kg)
     */
    public void setCdrgz(final double CDRGZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 2, CDRGZ);
    }

    /**
     * Get the object [7,4] in covariance matrix (with index starting at 1).
     * @return the object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgxdot() {
        return covarianceMatrix.getEntry(6, 3);
    }

    /**
     * Set the object [7,4] in covariance matrix (with index starting at 1).
     * @param CDRGXdot = object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgxdot(final double CDRGXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 3, CDRGXdot);
    }

    /**
     * Get the object [7,5] in covariance matrix (with index starting at 1).
     * @return the object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgydot() {
        return covarianceMatrix.getEntry(6, 4);
    }

    /**
     * Set the object [7,5] in covariance matrix (with index starting at 1).
     * @param CDRGYdot = object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgydot(final double CDRGYdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 4, CDRGYdot);
    }

    /**
     * Get the object [7,6] in covariance matrix (with index starting at 1).
     * @return the object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgzdot() {
        return covarianceMatrix.getEntry(6, 5);
    }

    /**
     * Set the object [7,6] in covariance matrix (with index starting at 1).
     * @param CDRGZdot = object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgzdot(final double CDRGZdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 5, CDRGZdot);
    }

    /**
     * Get the object [7,7] in covariance matrix (with index starting at 1).
     * @return the object [7,7] in covariance matrix (in m⁴/kg²)
     */
    public double getCdrgdrg() {
        return covarianceMatrix.getEntry(6, 6);
    }

    /**
     * Set the object [7,7] in covariance matrix (with index starting at 1).
     * @param CDRGDRG = object [7,7] in covariance matrix (in m⁴/kg²)
     */
    public void setCdrgdrg(final double CDRGDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 6, CDRGDRG);
    }

    /**
     * Get the object [8,1] in covariance matrix (with index starting at 1).
     * @return the object [8,1] in covariance matrix (in m³/kg)
     */
    public double getCsrpx() {
        return covarianceMatrix.getEntry(7, 0);
    }

    /**
     * Set the object [8,1] in covariance matrix (with index starting at 1).
     * @param CSRPX = object [8,1] in covariance matrix (in m³/kg)
     */
    public void setCsrpx(final double CSRPX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 0, CSRPX);
    }

    /**
     * Get the object [8,2] in covariance matrix (with index starting at 1).
     * @return the object [8,2] in covariance matrix (in m³/kg)
     */
    public double getCsrpy() {
        return covarianceMatrix.getEntry(7, 1);
    }

    /**
     * Set the object [8,2] in covariance matrix (with index starting at 1).
     * @param CSRPY = object [8,2] in covariance matrix (in m³/kg)
     */
    public void setCsrpy(final double CSRPY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 1, CSRPY);
    }

    /**
     * Get the object [8,3] in covariance matrix (with index starting at 1).
     * @return the object [8,3] in covariance matrix (in m³/kg)
     */
    public double getCsrpz() {
        return covarianceMatrix.getEntry(7, 2);
    }

    /**
     * Set the object [8,3] in covariance matrix (with index starting at 1).
     * @param CSRPZ = object [8,3] in covariance matrix (in m³/kg)
     */
    public void setCsrpz(final double CSRPZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 2, CSRPZ);
    }

    /**
     * Get the object [8,4] in covariance matrix (with index starting at 1).
     * @return the object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrpxdot() {
        return covarianceMatrix.getEntry(7, 3);
    }

    /**
     * Set the object [8,4] in covariance matrix (with index starting at 1).
     * @param CSRPXdot = object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrpxdot(final double CSRPXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 3, CSRPXdot);
    }

    /**
     * Get the object [8,5] in covariance matrix (with index starting at 1).
     * @return the object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrpydot() {
        return covarianceMatrix.getEntry(7, 4);
    }

    /**
     * Set the object [8,5] in covariance matrix (with index starting at 1).
     * @param CSRPYdot = object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrpydot(final double CSRPYdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 4, CSRPYdot);
    }

    /**
     * Get the object [8,6] in covariance matrix (with index starting at 1).
     * @return the object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrpzdot() {
        return covarianceMatrix.getEntry(7, 5);
    }

    /**
     * Set the object [8,6] in covariance matrix (with index starting at 1).
     * @param CSRPZdot = object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrpzdot(final double CSRPZdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 5, CSRPZdot);
    }

    /**
     * Get the object [8,7] in covariance matrix (with index starting at 1).
     * @return the object [8,7] in covariance matrix (in m⁴/kg²)
     */
    public double getCsrpdrg() {
        return covarianceMatrix.getEntry(7, 6);
    }

    /**
     * Set the object [8,7] in covariance matrix (with index starting at 1).
     * @param CSRPDRG = object [8,7] in covariance matrix (in m⁴/kg²)
     */
    public void setCsrpdrg(final double CSRPDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 6, CSRPDRG);
    }

    /**
     * Get the object [8,8] in covariance matrix (with index starting at 1).
     * @return the object [8,8] in covariance matrix (in m⁴/kg²)
     */
    public double getCsrpsrp() {
        return covarianceMatrix.getEntry(7, 7);
    }

    /**
     * Set the object [8,8] in covariance matrix (with index starting at 1).
     * @param CSRPSRP = object [8,8] in covariance matrix (in m⁴/kg²)
     */
    public void setCsrpsrp(final double CSRPSRP) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 7, CSRPSRP);
    }

    /**
     * Get the object [9,1] in covariance matrix (with index starting at 1).
     * @return the object [9,1] in covariance matrix (in m²/s²)
     */
    public double getCthrx() {
        return covarianceMatrix.getEntry(8, 0);
    }

    /**
     * Set the object [9,1] in covariance matrix (with index starting at 1).
     * @param CTHRX = object [9,1] in covariance matrix (in m²/s²)
     */
    public void setCthrx(final double CTHRX) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 0, CTHRX);
    }

    /**
     * Get the object [9,2] in covariance matrix (with index starting at 1).
     * @return the object [9,2] in covariance matrix (in m²/s²)
     */
    public double getCthry() {
        return covarianceMatrix.getEntry(8, 1);
    }

    /**
     * Set the object [9,2] in covariance matrix (with index starting at 1).
     * @param CTHRY = object [9,2] in covariance matrix (in m²/s²)
     */
    public void setCthry(final double CTHRY) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 1, CTHRY);
    }

    /**
     * Get the object [9,3] in covariance matrix (with index starting at 1).
     * @return the object [9,3] in covariance matrix (in m²/s²)
     */
    public double getCthrz() {
        return covarianceMatrix.getEntry(8, 2);
    }

    /**
     * Set the object [9,3] in covariance matrix (with index starting at 1).
     * @param CTHRZ = object [9,3] in covariance matrix (in m²/s²)
     */
    public void setCthrz(final double CTHRZ) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 2, CTHRZ);
    }

    /**
     * Get the object [9,4] in covariance matrix (with index starting at 1).
     * @return the object [9,4] in covariance matrix (in m²/s³)
     */
    public double getCthrxdot() {
        return covarianceMatrix.getEntry(8, 3);
    }

    /**
     * Set the object [9,4] in covariance matrix (with index starting at 1).
     * @param CTHRXdot = object [9,4] in covariance matrix (in m²/s³)
     */
    public void setCthrxdot(final double CTHRXdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 3, CTHRXdot);
    }

    /**
     * Get the object [9,5] in covariance matrix (with index starting at 1).
     * @return the object [9,5] in covariance matrix (in m²/s³)
     */
    public double getCthrydot() {
        return covarianceMatrix.getEntry(8, 4);
    }

    /**
     * Set the object [9,5] in covariance matrix (with index starting at 1).
     * @param CTHRYdot = object [9,5] in covariance matrix (in m²/s³)
     */
    public void setCthrydot(final double CTHRYdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 4, CTHRYdot);
    }

    /**
     * Get the object [9,6] in covariance matrix (with index starting at 1).
     * @return the object [9,6] in covariance matrix (in m²/s³)
     */
    public double getCthrzdot() {
        return covarianceMatrix.getEntry(8, 5);
    }

    /**
     * Set the object [9,6] in covariance matrix (with index starting at 1).
     * @param CTHRZdot = object [9,6] in covariance matrix (in m²/s³)
     */
    public void setCthrzdot(final double CTHRZdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 5, CTHRZdot);
    }

    /**
     * Get the object [9,7] in covariance matrix (with index starting at 1).
     * @return the object [9,7] in covariance matrix (in m³/(kg.s²))
     */
    public double getCthrdrg() {
        return covarianceMatrix.getEntry(8, 6);
    }

    /**
     * Set the object [9,7] in covariance matrix (with index starting at 1).
     * @param CTHRDRG = object [9,7] in covariance matrix (in m³/(kg.s²))
     */
    public void setCthrdrg(final double CTHRDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 6, CTHRDRG);
    }

    /**
     * Get the object [9,8] in covariance matrix (with index starting at 1).
     * @return the object [9,8] in covariance matrix (in m³/(kg.s²))
     */
    public double getCthrsrp() {
        return covarianceMatrix.getEntry(8, 7);
    }

    /**
     * Set the object [9,8] in covariance matrix (with index starting at 1).
     * @param CTHRSRP = object [9,8] in covariance matrix (in m³/(kg.s²))
     */
    public void setCthrsrp(final double CTHRSRP) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 7, CTHRSRP);
    }

    /**
     * Get the object [9,9] in covariance matrix (with index starting at 1).
     * @return the object [9,9] in covariance matrix (in m²/s⁴)
     */
    public double getCthrthr() {
        return covarianceMatrix.getEntry(8, 8);
    }

    /**
     * Set the object [9,9] in covariance matrix (with index starting at 1).
     * @param CTHRTHR = object [9,9] in covariance matrix (in m²/s⁴)
     */
    public void setCthrthr(final double CTHRTHR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 8, CTHRTHR);
    }

    /** Get the flag indicating whether the alternate covariance type set in the CDM Object metadata section is XYZ.
     * @return the covXYZset
     */
    public boolean isCovXYZset() {
        return covXYZset;
    }
}
