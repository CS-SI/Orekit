/* Copyright 2002-2022 CS GROUP
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
 * Container for covariance matrix data.
 * @author Melina Vanel
 * @since 11.2
 */
public class RTNCovariance extends CommentsContainer {
    /** Object covariance matrix [1,1]. */
    private double crr;

    /** Object covariance matrix [2,1]. */
    private double ctr;

    /** Object covariance matrix [2,2]. */
    private double ctt;

    /** Object covariance matrix [3,1]. */
    private double cnr;

    /** Object covariance matrix [3,2]. */
    private double cnt;

    /** Object covariance matrix [3,3]. */
    private double cnn;

    /** Object covariance matrix [4,1]. */
    private double crdotr;

    /** Object covariance matrix [4,2]. */
    private double crdott;

    /** Object covariance matrix [4,3]. */
    private double crdotn;

    /** Object covariance matrix [4,4]. */
    private double crdotrdot;

    /** Object covariance matrix [5,1]. */
    private double ctdotr;

    /** Object covariance matrix [5,2]. */
    private double ctdott;

    /** Object covariance matrix [5,3]. */
    private double ctdotn;

    /** Object covariance matrix [5,4]. */
    private double ctdotrdot;

    /** Object covariance matrix [5,5]. */
    private double ctdottdot;

    /** Object covariance matrix [6,1]. */
    private double cndotr;

    /** Object covariance matrix [6,2]. */
    private double cndott;

    /** Object covariance matrix [6,3]. */
    private double cndotn;

    /** Object covariance matrix [6,4]. */
    private double cndotrdot;

    /** Object covariance matrix [6,5]. */
    private double cndottdot;

    /** Object covariance matrix [6,6]. */
    private double cndotndot;

    /** Object covariance matrix [7,1]. */
    private double cdrgr;

    /** Object covariance matrix [7,2]. */
    private double cdrgt;

    /** Object covariance matrix [7,3]. */
    private double cdrgn;

    /** Object covariance matrix [7,4]. */
    private double cdrgrdot;

    /** Object covariance matrix [7,5]. */
    private double cdrgtdot;

    /** Object covariance matrix [7,6]. */
    private double cdrgndot;

    /** Object covariance matrix [7,7]. */
    private double  cdrgdrg;

    /** Object covariance matrix [8,1]. */
    private double  csrpr;

    /** Object covariance matrix [8,2]. */
    private double csrpt;

    /** Object covariance matrix [8,3]. */
    private double csrpn;

    /** Object covariance matrix [8,4]. */
    private double csrprdot;

    /** Object covariance matrix [8,5]. */
    private double csrptdot;

    /** Object covariance matrix [8,6]. */
    private double csrpndot;

    /** Object covariance matrix [8,7]. */
    private double csrpdrg;

    /** Object covariance matrix [8,9]. */
    private double csrpsrp;

    /** Object covariance matrix [9,1]. */
    private double cthrr;

    /** Object covariance matrix [9,2]. */
    private double cthrt;

    /** Object covariance matrix [9,3]. */
    private double cthrn;

    /** Object covariance matrix [9,4]. */
    private double cthrrdot;

    /** Object covariance matrix [9,5]. */
    private double cthrtdot;

    /** Object covariance matrix [9,6]. */
    private double cthrndot;

    /** Object covariance matrix [9,7]. */
    private double cthrdrg;

    /** Object covariance matrix [9,8]. */
    private double cthrsrp;

    /** Object covariance matrix [9,9]. */
    private double cthrthr;

    /** RTN covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Simple constructor.
     */
    public RTNCovariance() {
        crr             = Double.NaN;
        ctr             = Double.NaN;
        ctt             = Double.NaN;
        cnr             = Double.NaN;
        cnt             = Double.NaN;
        cnn             = Double.NaN;
        crdotr          = Double.NaN;
        crdott          = Double.NaN;
        crdotn          = Double.NaN;
        crdotrdot       = Double.NaN;
        ctdotr          = Double.NaN;
        ctdott          = Double.NaN;
        ctdotn          = Double.NaN;
        ctdotrdot       = Double.NaN;
        ctdottdot       = Double.NaN;
        cndotr          = Double.NaN;
        cndott          = Double.NaN;
        cndotn          = Double.NaN;
        cndotrdot       = Double.NaN;
        cndottdot       = Double.NaN;
        cndotndot       = Double.NaN;
        cdrgr           = Double.NaN;
        cdrgt           = Double.NaN;
        cdrgn           = Double.NaN;
        cdrgrdot        = Double.NaN;
        cdrgtdot        = Double.NaN;
        cdrgndot        = Double.NaN;
        cdrgdrg         = Double.NaN;
        csrpr           = Double.NaN;
        csrpt           = Double.NaN;
        csrpn           = Double.NaN;
        csrprdot        = Double.NaN;
        csrptdot        = Double.NaN;
        csrpndot        = Double.NaN;
        csrpdrg         = Double.NaN;
        csrpsrp         = Double.NaN;
        cthrr           = Double.NaN;
        cthrt           = Double.NaN;
        cthrn           = Double.NaN;
        cthrrdot        = Double.NaN;
        cthrtdot        = Double.NaN;
        cthrndot        = Double.NaN;
        cthrdrg         = Double.NaN;
        cthrsrp         = Double.NaN;
        cthrthr         = Double.NaN;

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
        checkNotNaN(crr,              RTNCovarianceKey.CR_R);
        checkNotNaN(ctr,              RTNCovarianceKey.CT_R);
        checkNotNaN(ctt,              RTNCovarianceKey.CT_T);
        checkNotNaN(cnr,              RTNCovarianceKey.CN_R);
        checkNotNaN(cnt,              RTNCovarianceKey.CN_T);
        checkNotNaN(cnn,              RTNCovarianceKey.CN_N);
        checkNotNaN(crdotr,           RTNCovarianceKey.CRDOT_R);
        checkNotNaN(crdott,           RTNCovarianceKey.CRDOT_T);
        checkNotNaN(crdotn,           RTNCovarianceKey.CRDOT_N);
        checkNotNaN(crdotrdot,        RTNCovarianceKey.CRDOT_RDOT);
        checkNotNaN(ctdotr,           RTNCovarianceKey.CTDOT_R);
        checkNotNaN(ctdott,           RTNCovarianceKey.CTDOT_T);
        checkNotNaN(ctdotn,           RTNCovarianceKey.CTDOT_N);
        checkNotNaN(ctdotrdot,        RTNCovarianceKey.CTDOT_RDOT);
        checkNotNaN(ctdottdot,        RTNCovarianceKey.CTDOT_TDOT);
        checkNotNaN(cndotr,           RTNCovarianceKey.CNDOT_R);
        checkNotNaN(cndott,           RTNCovarianceKey.CNDOT_T);
        checkNotNaN(cndotn,           RTNCovarianceKey.CNDOT_N);
        checkNotNaN(cndotrdot,        RTNCovarianceKey.CNDOT_RDOT);
        checkNotNaN(cndottdot,        RTNCovarianceKey.CNDOT_TDOT);
        checkNotNaN(cndotndot,        RTNCovarianceKey.CNDOT_NDOT);

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
     * @return the RTN covariance matrix
     */
    public RealMatrix getRTNcovariaxMatrince() {
        return covarianceMatrix;
    }

    /**
     * Get the object [1,1] in covariance matrix.
     * @return the object [1,1] in covariance matrix (in m²)
     */
    public double getCrr() {
        return crr;
    }

    /**
     * Set the object [1,1] in covariance matrix.
     * @param CRR = object [1,1] in covariance matrix (in m²)
     */
    public void setCrr(final double CRR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(1, 1, CRR);
        this.crr = CRR;
    }

    /**
     * Get the object [2,1] in covariance matrix.
     * @return the object [2,1] in covariance matrix (in m²)
     */
    public double getCtr() {
        return ctr;
    }

    /**
     * Set the object [2,1] in covariance matrix.
     * @param CTR = object [2,1] in covariance matrix (in m²)
     */
    public void setCtr(final double CTR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 1, CTR);
        this.ctr = CTR;
    }

    /**
     * Get the object [2,2] in covariance matrix.
     * @return the object [2,2] in covariance matrix (in m²)
     */
    public double getCtt() {
        return ctt;
    }

    /**
     * Set the object [2,2] in covariance matrix.
     * @param CTT = object [2,2] in covariance matrix (in m²)
     */
    public void setCtt(final double CTT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(2, 2, CTT);
        this.ctt = CTT;
    }

    /**
     * Get the object [3,1] in covariance matrix.
     * @return the object [3,1] in covariance matrix (in m²)
     */
    public double getCnr() {
        return cnr;
    }

    /**
     * Set the object [3,1] in covariance matrix.
     * @param CNR = object [3,1] in covariance matrix (in m²)
     */
    public void setCnr(final double CNR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 1, CNR);
        this.cnr = CNR;
    }

    /**
     * Get the object [3,2] in covariance matrix.
     * @return the object [3,2] in covariance matrix (in m²)
     */
    public double getCnt() {
        return cnt;
    }

    /**
     * Set the object [3,2] in covariance matrix.
     * @param CNT = object [3,2] in covariance matrix (in m²)
     */
    public void setCnt(final double CNT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 2, CNT);
        this.cnt = CNT;
    }

    /**
     * Get the object [3,3] in covariance matrix.
     * @return the object [3,3] in covariance matrix (in m²)
     */
    public double getCnn() {
        return cnn;
    }

    /**
     * Set the object [3,3] in covariance matrix.
     * @param CNN = object [3,3] in covariance matrix (in m²)
     */
    public void setCnn(final double CNN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(3, 3, CNN);
        this.cnn = CNN;
    }

    /**
     * Get the object [4,1] in covariance matrix.
     * @return the object [4,1] in covariance matrix (in m²/s)
     */
    public double getCrdotr() {
        return crdotr;
    }

    /**
     * Set the object [4,1] in covariance matrix.
     * @param CRdotR = object [4,1] in covariance matrix (in m²/s)
     */
    public void setCrdotr(final double CRdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 1, CRdotR);
        this.crdotr = CRdotR;
    }

    /**
     * Get the object [4,2] in covariance matrix.
     * @return the object [4,2] in covariance matrix (in m²/s)
     */
    public double getCrdott() {
        return crdott;
    }

    /**
     * Set the object [4,2] in covariance matrix.
     * @param CRdotT = object [4,2] in covariance matrix (in m²/s)
     */
    public void setCrdott(final double CRdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 2, CRdotT);
        this.crdott = CRdotT;
    }

    /**
     * Get the object [4,3] in covariance matrix.
     * @return the object [4,3] in covariance matrix (in m²/s)
     */
    public double getCrdotn() {
        return crdotn;
    }

    /**
     * Set the object [4,3] in covariance matrix.
     * @param CRdotN = object [4,3] in covariance matrix (in m²/s)
     */
    public void setCrdotn(final double CRdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 3, CRdotN);
        this.crdotn = CRdotN;
    }

    /**
     * Get the object [4,4] in covariance matrix.
     * @return the object [4,4] in covariance matrix (in m²/s²)
     */
    public double getCrdotrdot() {
        return crdotrdot;
    }

    /**
     * Set the object [4,4] in covariance matrix.
     * @param CRdotRdot = object [4,4] in covariance matrix (in m²/s²)
     */
    public void setCrdotrdot(final double CRdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(4, 4, CRdotRdot);
        this.crdotrdot = CRdotRdot;
    }

    /**
     * Get the object [5,1] in covariance matrix.
     * @return the object [5,1] in covariance matrix (in m²/s)
     */
    public double getCtdotr() {
        return ctdotr;
    }

    /**
     * Set the object [5,1] in covariance matrix.
     * @param CTdotR = object [5,1] in covariance matrix (in m²/s)
     */
    public void setCtdotr(final double CTdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 1, CTdotR);
        this.ctdotr = CTdotR;
    }

    /**
     * Get the object [5,2] in covariance matrix.
     * @return the object [5,2] in covariance matrix (in m²/s)
     */
    public double getCtdott() {
        return ctdott;
    }

    /**
     * Set the object [5,2] in covariance matrix.
     * @param CTdotT = object [5,2] in covariance matrix (in m²/s)
     */
    public void setCtdott(final double CTdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 2, CTdotT);
        this.ctdott = CTdotT;
    }

    /**
     * Get the object [5,3] in covariance matrix.
     * @return the object [5,3] in covariance matrix (in m²/s)
     */
    public double getCtdotn() {
        return ctdotn;
    }

    /**
     * Set the object [5,3] in covariance matrix.
     * @param CTdotN = object [5,3] in covariance matrix (in m²/s)
     */
    public void setCtdotn(final double CTdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 3, CTdotN);
        this.ctdotn = CTdotN;
    }

    /**
     * Get the object [5,4] in covariance matrix.
     * @return the object [5,4] in covariance matrix (in m²/s²)
     */
    public double getCtdotrdot() {
        return ctdotrdot;
    }

    /**
     * Set the object [5,4] in covariance matrix.
     * @param CTdotRdot = object [5,4] in covariance matrix (in m²/s²)
     */
    public void setCtdotrdot(final double CTdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 4, CTdotRdot);
        this.ctdotrdot = CTdotRdot;
    }

    /**
     * Get the object [5,5] in covariance matrix.
     * @return the object [5,5] in covariance matrix (in m²/s²)
     */
    public double getCtdottdot() {
        return ctdottdot;
    }

    /**
     * Set the object [5,5] in covariance matrix.
     * @param CTdotTdot = object [5,5] in covariance matrix (in m²/s²)
     */
    public void setCtdottdot(final double CTdotTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(5, 5, CTdotTdot);
        this.ctdottdot = CTdotTdot;
    }

    /**
     * Get the object [6,1] in covariance matrix.
     * @return the object [6,1] in covariance matrix (in m²/s)
     */
    public double getCndotr() {
        return cndotr;
    }

    /**
     * Set the object [6,1] in covariance matrix.
     * @param CNdotR = object [6,1] in covariance matrix (in m²/s)
     */
    public void setCndotr(final double CNdotR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 1, CNdotR);
        this.cndotr = CNdotR;
    }

    /**
     * Get the object [6,2] in covariance matrix.
     * @return the object [6,2] in covariance matrix (in m²/s)
     */
    public double getCndott() {
        return cndott;
    }

    /**
     * Set the object [6,2] in covariance matrix.
     * @param CNdotT = object [6,2] in covariance matrix (in m²/s)
     */
    public void setCndott(final double CNdotT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 2, CNdotT);
        this.cndott = CNdotT;
    }

    /**
     * Get the object [6,3] in covariance matrix.
     * @return the object [6,3] in covariance matrix (in m²/s)
     */
    public double getCndotn() {
        return cndotn;
    }

    /**
     * Set the object [6,3] in covariance matrix.
     * @param CNdotN = object [6,3] in covariance matrix (in m²/s)
     */
    public void setCndotn(final double CNdotN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 3, CNdotN);
        this.cndotn = CNdotN;
    }

    /**
     * Get the object [6,4] in covariance matrix.
     * @return the object [6,4] in covariance matrix (in m²/s²)
     */
    public double getCndotrdot() {
        return cndotrdot;
    }

    /**
     * Set the object [6,4] in covariance matrix.
     * @param CNdotRdot = object [6,4] in covariance matrix (in m²/s²)
     */
    public void setCndotrdot(final double CNdotRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 4, CNdotRdot);
        this.cndotrdot = CNdotRdot;
    }

    /**
     * Get the object [6,5] in covariance matrix.
     * @return the object [6,5] in covariance matrix (in m²/s²)
     */
    public double getCndottdot() {
        return cndottdot;
    }

    /**
     * Set the object [6,5] in covariance matrix.
     * @param CNdotTdot = object [6,5] in covariance matrix (in m²/s²)
     */
    public void setCndottdot(final double CNdotTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 5, CNdotTdot);
        this.cndottdot = CNdotTdot;
    }

    /**
     * Get the object [6,6] in covariance matrix.
     * @return the object [6,6] in covariance matrix (in m²/s²)
     */
    public double getCndotndot() {
        return cndotndot;
    }

    /**
     * Set the object [6,6] in covariance matrix.
     * @param CNdotNdot = object [6,6] in covariance matrix (in m²/s²)
     */
    public void setCndotndot(final double CNdotNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(6, 6, CNdotNdot);
        this.cndotndot = CNdotNdot;
    }

    /**
     * Get the object [7,1] in covariance matrix.
     * @return the object [7,1] in covariance matrix (in m³/kg)
     */
    public double getCdrgr() {
        return cdrgr;
    }

    /**
     * Set the object [7,1] in covariance matrix.
     * @param CDRGR = object [7,1] in covariance matrix (in m³/kg)
     */
    public void setCdrgr(final double CDRGR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 1, CDRGR);
        this.cdrgr = CDRGR;
    }

    /**
     * Get the object [7,2] in covariance matrix.
     * @return the object [7,2] in covariance matrix (in m³/kg)
     */
    public double getCdrgt() {
        return cdrgt;
    }

    /**
     * Set the object [7,2] in covariance matrix.
     * @param CDRGT = object [7,2] in covariance matrix (in m³/kg)
     */
    public void setCdrgt(final double CDRGT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 2, CDRGT);
        this.cdrgt = CDRGT;
    }

    /**
     * Get the object [7,3] in covariance matrix.
     * @return the object [7,3] in covariance matrix (in m³/kg)
     */
    public double getCdrgn() {
        return cdrgn;
    }

    /**
     * Set the object [7,3] in covariance matrix.
     * @param CDRGN = object [7,3] in covariance matrix (in m³/kg)
     */
    public void setCdrgn(final double CDRGN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 3, CDRGN);
        this.cdrgn = CDRGN;
    }

    /**
     * Get the object [7,4] in covariance matrix.
     * @return the object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgrdot() {
        return cdrgrdot;
    }

    /**
     * Set the object [7,4] in covariance matrix.
     * @param CDRGRdot = object [7,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgrdot(final double CDRGRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 4, CDRGRdot);
        this.cdrgrdot = CDRGRdot;
    }

    /**
     * Get the object [7,5] in covariance matrix.
     * @return the object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgtdot() {
        return cdrgtdot;
    }

    /**
     * Set the object [7,5] in covariance matrix.
     * @param CDRGTdot = object [7,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgtdot(final double CDRGTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 5, CDRGTdot);
        this.cdrgtdot = CDRGTdot;
    }

    /**
     * Get the object [7,6] in covariance matrix.
     * @return the object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCdrgndot() {
        return cdrgndot;
    }

    /**
     * Set the object [7,6] in covariance matrix.
     * @param CDRGNdot = object [7,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCdrgndot(final double CDRGNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 6, CDRGNdot);
        this.cdrgndot = CDRGNdot;
    }

    /**
     * Get the object [7,7] in covariance matrix.
     * @return the object [7,7] in covariance matrix (in m⁴/kg²)
     */
    public double getCdrgdrg() {
        return cdrgdrg;
    }

    /**
     * Set the object [7,7] in covariance matrix.
     * @param CDRGDRG = object [7,7] in covariance matrix (in m⁴/kg²)
     */
    public void setCdrgdrg(final double CDRGDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(7, 7, CDRGDRG);
        this.cdrgdrg = CDRGDRG;
    }

    /**
     * Get the object [8,1] in covariance matrix.
     * @return the object [8,1] in covariance matrix (in m³/kg)
     */
    public double getCsrpr() {
        return csrpr;
    }

    /**
     * Set the object [8,1] in covariance matrix.
     * @param CSRPR = object [8,1] in covariance matrix (in m³/kg)
     */
    public void setCsrpr(final double CSRPR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 1, CSRPR);
        this.csrpr = CSRPR;
    }

    /**
     * Get the object [8,2] in covariance matrix.
     * @return the object [8,2] in covariance matrix (in m³/kg)
     */
    public double getCsrpt() {
        return csrpt;
    }

    /**
     * Set the object [8,2] in covariance matrix.
     * @param CSRPT = object [8,2] in covariance matrix (in m³/kg)
     */
    public void setCsrpt(final double CSRPT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 2, CSRPT);
        this.csrpt = CSRPT;
    }

    /**
     * Get the object [8,3] in covariance matrix.
     * @return the object [8,3] in covariance matrix (in m³/kg)
     */
    public double getCsrpn() {
        return csrpn;
    }

    /**
     * Set the object [8,3] in covariance matrix.
     * @param CSRPN = object [8,3] in covariance matrix (in m³/kg)
     */
    public void setCsrpn(final double CSRPN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 3, CSRPN);
        this.csrpn = CSRPN;
    }

    /**
     * Get the object [8,4] in covariance matrix.
     * @return the object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrprdot() {
        return csrprdot;
    }

    /**
     * Set the object [8,4] in covariance matrix.
     * @param CSRPRdot = object [8,4] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrprdot(final double CSRPRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 4, CSRPRdot);
        this.csrprdot = CSRPRdot;
    }

    /**
     * Get the object [8,5] in covariance matrix.
     * @return the object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrptdot() {
        return csrptdot;
    }

    /**
     * Set the object [8,5] in covariance matrix.
     * @param CSRPTdot = object [8,5] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrptdot(final double CSRPTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 5, CSRPTdot);
        this.csrptdot = CSRPTdot;
    }

    /**
     * Get the object [8,6] in covariance matrix.
     * @return the object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public double getCsrpndot() {
        return csrpndot;
    }

    /**
     * Set the object [8,6] in covariance matrix.
     * @param CSRPNdot = object [8,6] in covariance matrix (in m³/(kg.s))
     */
    public void setCsrpndot(final double CSRPNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 6, CSRPNdot);
        this.csrpndot = CSRPNdot;
    }

    /**
     * Get the object [8,7] in covariance matrix.
     * @return the object [8,7] in covariance matrix (in m⁴/kg²)
     */
    public double getCsrpdrg() {
        return csrpdrg;
    }

    /**
     * Set the object [8,7] in covariance matrix.
     * @param CSRPDRG = object [8,7] in covariance matrix (in m⁴/kg²)
     */
    public void setCsrpdrg(final double CSRPDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 7, CSRPDRG);
        this.csrpdrg = CSRPDRG;
    }

    /**
     * Get the object [8,8] in covariance matrix.
     * @return the object [8,8] in covariance matrix (in m⁴/kg²)
     */
    public double getCsrpsrp() {
        return csrpsrp;
    }

    /**
     * Set the object [8,8] in covariance matrix.
     * @param CSRPSRP = object [8,8] in covariance matrix (in m⁴/kg²)
     */
    public void setCsrpsrp(final double CSRPSRP) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(8, 8, CSRPSRP);
        this.csrpsrp = CSRPSRP;
    }

    /**
     * Get the object [9,1] in covariance matrix.
     * @return the object [9,1] in covariance matrix (in m²/s²)
     */
    public double getCthrr() {
        return cthrr;
    }

    /**
     * Set the object [9,1] in covariance matrix.
     * @param CTHRR = object [9,1] in covariance matrix (in m²/s²)
     */
    public void setCthrr(final double CTHRR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 1, CTHRR);
        this.cthrr = CTHRR;
    }

    /**
     * Get the object [9,2] in covariance matrix.
     * @return the object [9,2] in covariance matrix (in m²/s²)
     */
    public double getCthrt() {
        return cthrt;
    }

    /**
     * Set the object [9,2] in covariance matrix.
     * @param CTHRT = object [9,2] in covariance matrix (in m²/s²)
     */
    public void setCthrt(final double CTHRT) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 2, CTHRT);
        this.cthrt = CTHRT;
    }

    /**
     * Get the object [9,3] in covariance matrix.
     * @return the object [9,3] in covariance matrix (in m²/s²)
     */
    public double getCthrn() {
        return cthrn;
    }

    /**
     * Set the object [9,3] in covariance matrix.
     * @param CTHRN = object [9,3] in covariance matrix (in m²/s²)
     */
    public void setCthrn(final double CTHRN) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 3, CTHRN);
        this.cthrn = CTHRN;
    }

    /**
     * Get the object [9,4] in covariance matrix.
     * @return the object [9,4] in covariance matrix (in m²/s³)
     */
    public double getCthrrdot() {
        return cthrrdot;
    }

    /**
     * Set the object [9,4] in covariance matrix.
     * @param CTHRRdot = object [9,4] in covariance matrix (in m²/s³)
     */
    public void setCthrrdot(final double CTHRRdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 4, CTHRRdot);
        this.cthrrdot = CTHRRdot;
    }

    /**
     * Get the object [9,5] in covariance matrix.
     * @return the object [9,5] in covariance matrix (in m²/s³)
     */
    public double getCthrtdot() {
        return cthrtdot;
    }

    /**
     * Set the object [9,5] in covariance matrix.
     * @param CTHRTdot = object [9,5] in covariance matrix (in m²/s³)
     */
    public void setCthrtdot(final double CTHRTdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 5, CTHRTdot);
        this.cthrtdot = CTHRTdot;
    }

    /**
     * Get the object [9,6] in covariance matrix.
     * @return the object [9,6] in covariance matrix (in m²/s³)
     */
    public double getCthrndot() {
        return cthrndot;
    }

    /**
     * Set the object [9,6] in covariance matrix.
     * @param CTHRNdot = object [9,6] in covariance matrix (in m²/s³)
     */
    public void setCthrndot(final double CTHRNdot) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 6, CTHRNdot);
        this.cthrndot = CTHRNdot;
    }

    /**
     * Get the object [9,7] in covariance matrix.
     * @return the object [9,7] in covariance matrix (in m³/(kg.s²))
     */
    public double getCthrdrg() {
        return cthrdrg;
    }

    /**
     * Set the object [9,7] in covariance matrix.
     * @param CTHRDRG = object [9,7] in covariance matrix (in m³/(kg.s²))
     */
    public void setCthrdrg(final double CTHRDRG) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 7, CTHRDRG);
        this.cthrdrg = CTHRDRG;
    }

    /**
     * Get the object [9,8] in covariance matrix.
     * @return the object [9,8] in covariance matrix (in m³/(kg.s²))
     */
    public double getCthrsrp() {
        return cthrsrp;
    }

    /**
     * Set the object [9,8] in covariance matrix.
     * @param CTHRSRP = object [9,8] in covariance matrix (in m³/(kg.s²))
     */
    public void setCthrsrp(final double CTHRSRP) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 8, CTHRSRP);
        this.cthrsrp = CTHRSRP;
    }

    /**
     * Get the object [9,9] in covariance matrix.
     * @return the object [9,9] in covariance matrix (in m²/s⁴)
     */
    public double getCthrthr() {
        return cthrthr;
    }

    /**
     * Set the object [9,9] in covariance matrix.
     * @param CTHRTHR = object [9,9] in covariance matrix (in m²/s⁴)
     */
    public void setCthrthr(final double CTHRTHR) {
        refuseFurtherComments();
        setCovarianceMatrixEntry(9, 9, CTHRTHR);
        this.cthrthr = CTHRTHR;
    }

}
