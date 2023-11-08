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
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for RTN covariance matrix data. This class as a RealMatrix as
 * attribute which can be acces with getRTNCovariaxMatrix method. Beware that
 * there are thus 2 ways to modify the RTN covariance : setC... ( setCrr,
 * setCtr ...) which should be prioritized and getRTNCovariaxMatrix.setEntry(row, col, value).
 * <p> The RTN Covariance Matrix is provided in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
 * are mandatory. The remaining elements will return NaN if not provided. </p>
 * @author Melina Vanel
 * @since 11.2
 */
public class RTNCovariance extends CommentsContainer {

    /** RTN covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Simple constructor. To update matrix value there are 2 ways to modify the RTN
     * covariance : setC... ( setCrr, setCtr ...) which should be prioritized and
     * getRTNCovariaxMatrix.setEntry(row, col, value).
     * <p> The RTN Covariance Matrix is provided in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
     * are mandatory. The remaining elements will return NaN if not provided. </p>
     */
    public RTNCovariance() {
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
        // We only check values that are mandatory in a cdm file
        checkNotNaN(getCrr(),              RTNCovarianceKey.CR_R.name());
        checkNotNaN(getCtr(),              RTNCovarianceKey.CT_R.name());
        checkNotNaN(getCtt(),              RTNCovarianceKey.CT_T.name());
        checkNotNaN(getCnr(),              RTNCovarianceKey.CN_R.name());
        checkNotNaN(getCnt(),              RTNCovarianceKey.CN_T.name());
        checkNotNaN(getCnn(),              RTNCovarianceKey.CN_N.name());
        checkNotNaN(getCrdotr(),           RTNCovarianceKey.CRDOT_R.name());
        checkNotNaN(getCrdott(),           RTNCovarianceKey.CRDOT_T.name());
        checkNotNaN(getCrdotn(),           RTNCovarianceKey.CRDOT_N.name());
        checkNotNaN(getCrdotrdot(),        RTNCovarianceKey.CRDOT_RDOT.name());
        checkNotNaN(getCtdotr(),           RTNCovarianceKey.CTDOT_R.name());
        checkNotNaN(getCtdott(),           RTNCovarianceKey.CTDOT_T.name());
        checkNotNaN(getCtdotn(),           RTNCovarianceKey.CTDOT_N.name());
        checkNotNaN(getCtdotrdot(),        RTNCovarianceKey.CTDOT_RDOT.name());
        checkNotNaN(getCtdottdot(),        RTNCovarianceKey.CTDOT_TDOT.name());
        checkNotNaN(getCndotr(),           RTNCovarianceKey.CNDOT_R.name());
        checkNotNaN(getCndott(),           RTNCovarianceKey.CNDOT_T.name());
        checkNotNaN(getCndotn(),           RTNCovarianceKey.CNDOT_N.name());
        checkNotNaN(getCndotrdot(),        RTNCovarianceKey.CNDOT_RDOT.name());
        checkNotNaN(getCndottdot(),        RTNCovarianceKey.CNDOT_TDOT.name());
        checkNotNaN(getCndotndot(),        RTNCovarianceKey.CNDOT_NDOT.name());
    }

    /** Set an entry in the RTN covariance matrix.
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
     * Get the RTN covariance matrix.
     * <p> The RTN Covariance Matrix is provided in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
     * are mandatory. The remaining elements will return NaN if not provided. </p>
     * @return the RTN covariance matrix
     */
    public RealMatrix getRTNCovarianceMatrix() {
        return covarianceMatrix;
    }

    /**
     * Get the object [1,1] in covariance matrix (with index starting at 1).
     * @return the object [1,1] in covariance matrix (in m²)
     */
    public double getCrr() {
        return covarianceMatrix.getEntry(0, 0);
    }

    /**
     * Set the object [1,1] in covariance matrix (with index starting at 1).
     * @param CRR = object [1,1] in covariance matrix (in m²)
     */
    public void setCrr(final double CRR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(0, 0, CRR);
    }

    /**
     * Get the object [2,1] in covariance matrix (with index starting at 1).
     * @return the object [2,1] in covariance matrix (in m²)
     */
    public double getCtr() {
        return covarianceMatrix.getEntry(1, 0);
    }

    /**
     * Set the object [2,1] in covariance matrix (with index starting at 1).
     * @param CTR = object [2,1] in covariance matrix (in m²)
     */
    public void setCtr(final double CTR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(1, 0, CTR);
    }

    /**
     * Get the object [2,2] in covariance matrix (with index starting at 1).
     * @return the object [2,2] in covariance matrix (in m²)
     */
    public double getCtt() {
        return covarianceMatrix.getEntry(1, 1);
    }

    /**
     * Set the object [2,2] in covariance matrix (with index starting at 1).
     * @param CTT = object [2,2] in covariance matrix (in m²)
     */
    public void setCtt(final double CTT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(1, 1, CTT);
    }

    /**
     * Get the object [3,1] in covariance matrix (with index starting at 1).
     * @return the object [3,1] in covariance matrix (in m²)
     */
    public double getCnr() {
        return covarianceMatrix.getEntry(2, 0);
    }

    /**
     * Set the object [3,1] in covariance matrix (with index starting at 1).
     * @param CNR = object [3,1] in covariance matrix (in m²)
     */
    public void setCnr(final double CNR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 0, CNR);
    }

    /**
     * Get the object [3,2] in covariance matrix (with index starting at 1).
     * @return the object [3,2] in covariance matrix (in m²)
     */
    public double getCnt() {
        return covarianceMatrix.getEntry(2, 1);
    }

    /**
     * Set the object [3,2] in covariance matrix (with index starting at 1).
     * @param CNT = object [3,2] in covariance matrix (in m²)
     */
    public void setCnt(final double CNT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 1, CNT);
    }

    /**
     * Get the object [3,3] in covariance matrix (with index starting at 1).
     * @return the object [3,3] in covariance matrix (in m²)
     */
    public double getCnn() {
        return covarianceMatrix.getEntry(2, 2);
    }

    /**
     * Set the object [3,3] in covariance matrix (with index starting at 1).
     * @param CNN = object [3,3] in covariance matrix (in m²)
     */
    public void setCnn(final double CNN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 2, CNN);
    }

    /**
     * Get the object [4,1] in covariance matrix (with index starting at 1).
     * @return the object [4,1] in covariance matrix (in m²/s)
     */
    public double getCrdotr() {
        return covarianceMatrix.getEntry(3, 0);
    }

    /**
     * Set the object [4,1] in covariance matrix (with index starting at 1).
     * @param CRdotR = object [4,1] in covariance matrix (in m²/s)
     */
    public void setCrdotr(final double CRdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 0, CRdotR);
    }

    /**
     * Get the object [4,2] in covariance matrix (with index starting at 1).
     * @return the object [4,2] in covariance matrix (in m²/s)
     */
    public double getCrdott() {
        return covarianceMatrix.getEntry(3, 1);
    }

    /**
     * Set the object [4, 2] in covariance matrix (with index starting at 1).
     * @param CRdotT = object [4, 2] in covariance matrix (in m²/s)
     */
    public void setCrdott(final double CRdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 1, CRdotT);
    }

    /**
     * Get the object [4, 3] in covariance matrix (with index starting at 1) .
     * @return the object [4, 3] in covariance matrix (in m²/s)
     */
    public double getCrdotn() {
        return covarianceMatrix.getEntry(3, 2);
    }

    /**
     * Set the object [4, 3] in covariance matrix (with index starting at 1).
     * @param CRdotN = object [4,3] in covariance matrix (in m²/s)
     */
    public void setCrdotn(final double CRdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 2, CRdotN);
    }

    /**
     * Get the object [4, 4] in covariance matrix (with index starting at 1).
     * @return the object [4, 4] in covariance matrix (in m²/s²)
     */
    public double getCrdotrdot() {
        return covarianceMatrix.getEntry(3, 3);
    }

    /**
     * Set the object [4, 4] in covariance matrix (with index starting at 1).
     * @param CRdotRdot = object [4, 4] in covariance matrix (in m²/s²)
     */
    public void setCrdotrdot(final double CRdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 3, CRdotRdot);
    }

    /**
     * Get the object [5, 1] in covariance matrix (with index starting at 1).
     * @return the object [5, 1] in covariance matrix (in m²/s)
     */
    public double getCtdotr() {
        return covarianceMatrix.getEntry(4, 0);
    }

    /**
     * Set the object [5,1] in covariance matrix (with index starting at 1).
     * @param CTdotR = object [5,1] in covariance matrix (in m²/s)
     */
    public void setCtdotr(final double CTdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 0, CTdotR);
    }

    /**
     * Get the object [5,2] in covariance matrix (with index starting at 1).
     * @return the object [5,2] in covariance matrix (in m²/s)
     */
    public double getCtdott() {
        return covarianceMatrix.getEntry(4, 1);
    }

    /**
     * Set the object [5,2] in covariance matrix (with index starting at 1).
     * @param CTdotT = object [5,2] in covariance matrix (in m²/s)
     */
    public void setCtdott(final double CTdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 1, CTdotT);
    }

    /**
     * Get the object [5,3] in covariance matrix (with index starting at 1).
     * @return the object [5,3] in covariance matrix (in m²/s)
     */
    public double getCtdotn() {
        return covarianceMatrix.getEntry(4, 2);
    }

    /**
     * Set the object [5,3] in covariance matrix (with index starting at 1).
     * @param CTdotN = object [5,3] in covariance matrix (in m²/s)
     */
    public void setCtdotn(final double CTdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 2, CTdotN);
    }

    /**
     * Get the object [5,4] in covariance matrix (with index starting at 1).
     * @return the object [5,4] in covariance matrix (in m²/s²)
     */
    public double getCtdotrdot() {
        return covarianceMatrix.getEntry(4, 3);
    }

    /**
     * Set the object [5,4] in covariance matrix (with index starting at 1).
     * @param CTdotRdot = object [5,4] in covariance matrix (in m²/s²)
     */
    public void setCtdotrdot(final double CTdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 3, CTdotRdot);
    }

    /**
     * Get the object [5,5] in covariance matrix (with index starting at 1).
     * @return the object [5,5] in covariance matrix (in m²/s²)
     */
    public double getCtdottdot() {
        return covarianceMatrix.getEntry(4, 4);
    }

    /**
     * Set the object [5,5] in covariance matrix (with index starting at 1).
     * @param CTdotTdot = object [5,5] in covariance matrix (in m²/s²)
     */
    public void setCtdottdot(final double CTdotTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 4, CTdotTdot);
    }

    /**
     * Get the object [6,1] in covariance matrix (with index starting at 1).
     * @return the object [6,1] in covariance matrix (in m²/s)
     */
    public double getCndotr() {
        return covarianceMatrix.getEntry(5, 0);
    }

    /**
     * Set the object [6,1] in covariance matrix (with index starting at 1).
     * @param CNdotR = object [6,1] in covariance matrix (in m²/s)
     */
    public void setCndotr(final double CNdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 0, CNdotR);
    }

    /**
     * Get the object [6,2] in covariance matrix (with index starting at 1).
     * @return the object [6,2] in covariance matrix (in m²/s)
     */
    public double getCndott() {
        return covarianceMatrix.getEntry(5, 1);
    }

    /**
     * Set the object [6,2] in covariance matrix (with index starting at 1).
     * @param CNdotT = object [6,2] in covariance matrix (in m²/s)
     */
    public void setCndott(final double CNdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 1, CNdotT);
    }

    /**
     * Get the object [6,3] in covariance matrix (with index starting at 1).
     * @return the object [6,3] in covariance matrix (in m²/s)
     */
    public double getCndotn() {
        return covarianceMatrix.getEntry(5, 2);
    }

    /**
     * Set the object [6,3] in covariance matrix (with index starting at 1).
     * @param CNdotN = object [6,3] in covariance matrix (in m²/s)
     */
    public void setCndotn(final double CNdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 2, CNdotN);
    }

    /**
     * Get the object [6,4] in covariance matrix (with index starting at 1).
     * @return the object [6,4] in covariance matrix (in m²/s²)
     */
    public double getCndotrdot() {
        return covarianceMatrix.getEntry(5, 3);
    }

    /**
     * Set the object [6,4] in covariance matrix (with index starting at 1).
     * @param CNdotRdot = object [6,4] in covariance matrix (in m²/s²)
     */
    public void setCndotrdot(final double CNdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 3, CNdotRdot);
    }

    /**
     * Get the object [6,5] in covariance matrix (with index starting at 1).
     * @return the object [6,5] in covariance matrix (in m²/s²)
     */
    public double getCndottdot() {
        return covarianceMatrix.getEntry(5, 4);
    }

    /**
     * Set the object [6,5] in covariance matrix (with index starting at 1).
     * @param CNdotTdot = object [6,5] in covariance matrix (in m²/s²)
     */
    public void setCndottdot(final double CNdotTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 4, CNdotTdot);
    }

    /**
     * Get the object [6,6] in covariance matrix (with index starting at 1).
     * @return the object [6,6] in covariance matrix (in m²/s²)
     */
    public double getCndotndot() {
        return covarianceMatrix.getEntry(5, 5);
    }

    /**
     * Set the object [6,6] in covariance matrix (with index starting at 1).
     * @param CNdotNdot = object [6,6] in covariance matrix (in m²/s²)
     */
    public void setCndotndot(final double CNdotNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 5, CNdotNdot);
    }

    /**
     * Get the object [7,1] in covariance matrix (with index starting at 1).
     * @return the object [7,1] in covariance matrix (in m³/kg)
     */
    public double getCdrgr() {
        return covarianceMatrix.getEntry(6, 0);
    }

    /**
     * Set the object [7,1] in covariance matrix (with index starting at 1).
     * @param CDRGR = object [7,1] in covariance matrix (in m³/kg)
     */
    public void setCdrgr(final double CDRGR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 0, CDRGR);
    }

    /**
     * Get the object [7,2] in covariance matrix.
     * @return the object [7,2] in covariance matrix (in m³/kg)
     */
    public double getCdrgt() {
        return covarianceMatrix.getEntry(6, 1);
    }

    /**
     * Set the object [7,2] in covariance matrix (with index starting at 1).
     * @param CDRGT = object [7,2] in covariance matrix (in m³/kg)
     */
    public void setCdrgt(final double CDRGT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 1, CDRGT);
    }

    /**
     * Get the object [7,3] in covariance matrix (with index starting at 1).
     * @return the object [7,3] in covariance matrix (in m³/kg)
     */
    public double getCdrgn() {
        return covarianceMatrix.getEntry(6, 2);
    }

    /**
     * Set the object [7,3] in covariance matrix (with index starting at 1).
     * @param CDRGN = object [7,3] in covariance matrix (in m³/kg)
     */
    public void setCdrgn(final double CDRGN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 2, CDRGN);
    }

    /**
     * Get the object [7,4] in covariance matrix (with index starting at 1).
     * @return the object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgrdot() {
        return covarianceMatrix.getEntry(6, 3);
    }

    /**
     * Set the object [7,4] in covariance matrix (with index starting at 1).
     * @param CDRGRdot = object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgrdot(final double CDRGRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 3, CDRGRdot);
    }

    /**
     * Get the object [7,5] in covariance matrix (with index starting at 1).
     * @return the object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgtdot() {
        return covarianceMatrix.getEntry(6, 4);
    }

    /**
     * Set the object [7,5] in covariance matrix (with index starting at 1).
     * @param CDRGTdot = object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgtdot(final double CDRGTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 4, CDRGTdot);
    }

    /**
     * Get the object [7,6] in covariance matrix (with index starting at 1).
     * @return the object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgndot() {
        return covarianceMatrix.getEntry(6, 5);
    }

    /**
     * Set the object [7,6] in covariance matrix (with index starting at 1).
     * @param CDRGNdot = object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgndot(final double CDRGNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 5, CDRGNdot);
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
    public double getCsrpr() {
        return covarianceMatrix.getEntry(7, 0);
    }

    /**
     * Set the object [8,1] in covariance matrix (with index starting at 1).
     * @param CSRPR = object [8,1] in covariance matrix (in m³/kg)
     */
    public void setCsrpr(final double CSRPR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 0, CSRPR);
    }

    /**
     * Get the object [8,2] in covariance matrix (with index starting at 1).
     * @return the object [8,2] in covariance matrix (in m³/kg)
     */
    public double getCsrpt() {
        return covarianceMatrix.getEntry(7, 1);
    }

    /**
     * Set the object [8,2] in covariance matrix (with index starting at 1).
     * @param CSRPT = object [8,2] in covariance matrix (in m³/kg)
     */
    public void setCsrpt(final double CSRPT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 1, CSRPT);
    }

    /**
     * Get the object [8,3] in covariance matrix (with index starting at 1).
     * @return the object [8,3] in covariance matrix (in m³/kg)
     */
    public double getCsrpn() {
        return covarianceMatrix.getEntry(7, 2);
    }

    /**
     * Set the object [8,3] in covariance matrix (with index starting at 1).
     * @param CSRPN = object [8,3] in covariance matrix (in m³/kg)
     */
    public void setCsrpn(final double CSRPN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 2, CSRPN);
    }

    /**
     * Get the object [8,4] in covariance matrix (with index starting at 1).
     * @return the object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrprdot() {
        return covarianceMatrix.getEntry(7, 3);
    }

    /**
     * Set the object [8,4] in covariance matrix (with index starting at 1).
     * @param CSRPRdot = object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrprdot(final double CSRPRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 3, CSRPRdot);
    }

    /**
     * Get the object [8,5] in covariance matrix (with index starting at 1).
     * @return the object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrptdot() {
        return covarianceMatrix.getEntry(7, 4);
    }

    /**
     * Set the object [8,5] in covariance matrix (with index starting at 1).
     * @param CSRPTdot = object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrptdot(final double CSRPTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 4, CSRPTdot);
    }

    /**
     * Get the object [8,6] in covariance matrix (with index starting at 1).
     * @return the object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrpndot() {
        return covarianceMatrix.getEntry(7, 5);
    }

    /**
     * Set the object [8,6] in covariance matrix (with index starting at 1).
     * @param CSRPNdot = object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrpndot(final double CSRPNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 5, CSRPNdot);
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
    public double getCthrr() {
        return covarianceMatrix.getEntry(8, 0);
    }

    /**
     * Set the object [9,1] in covariance matrix (with index starting at 1).
     * @param CTHRR = object [9,1] in covariance matrix (in m²/s²)
     */
    public void setCthrr(final double CTHRR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 0, CTHRR);
    }

    /**
     * Get the object [9,2] in covariance matrix (with index starting at 1).
     * @return the object [9,2] in covariance matrix (in m²/s²)
     */
    public double getCthrt() {
        return covarianceMatrix.getEntry(8, 1);
    }

    /**
     * Set the object [9,2] in covariance matrix (with index starting at 1).
     * @param CTHRT = object [9,2] in covariance matrix (in m²/s²)
     */
    public void setCthrt(final double CTHRT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 1, CTHRT);
    }

    /**
     * Get the object [9,3] in covariance matrix (with index starting at 1).
     * @return the object [9,3] in covariance matrix (in m²/s²)
     */
    public double getCthrn() {
        return covarianceMatrix.getEntry(8, 2);
    }

    /**
     * Set the object [9,3] in covariance matrix (with index starting at 1).
     * @param CTHRN = object [9,3] in covariance matrix (in m²/s²)
     */
    public void setCthrn(final double CTHRN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 2, CTHRN);
    }

    /**
     * Get the object [9,4] in covariance matrix (with index starting at 1).
     * @return the object [9,4] in covariance matrix (in m²/s³)
     */
    public double getCthrrdot() {
        return covarianceMatrix.getEntry(8, 3);
    }

    /**
     * Set the object [9,4] in covariance matrix (with index starting at 1).
     * @param CTHRRdot = object [9,4] in covariance matrix (in m²/s³)
     */
    public void setCthrrdot(final double CTHRRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 3, CTHRRdot);
    }

    /**
     * Get the object [9,5] in covariance matrix (with index starting at 1).
     * @return the object [9,5] in covariance matrix (in m²/s³)
     */
    public double getCthrtdot() {
        return covarianceMatrix.getEntry(8, 4);
    }

    /**
     * Set the object [9,5] in covariance matrix (with index starting at 1).
     * @param CTHRTdot = object [9,5] in covariance matrix (in m²/s³)
     */
    public void setCthrtdot(final double CTHRTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 4, CTHRTdot);
    }

    /**
     * Get the object [9,6] in covariance matrix (with index starting at 1).
     * @return the object [9,6] in covariance matrix (in m²/s³)
     */
    public double getCthrndot() {
        return covarianceMatrix.getEntry(8, 5);
    }

    /**
     * Set the object [9,6] in covariance matrix (with index starting at 1).
     * @param CTHRNdot = object [9,6] in covariance matrix (in m²/s³)
     */
    public void setCthrndot(final double CTHRNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 5, CTHRNdot);
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

}
