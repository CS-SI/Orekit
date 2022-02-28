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

import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for covariance matrix data.
 * @author Melina Vanel
 * @since 11.2
 */
public class CovarianceMatrix extends CommentsContainer {
    /** Object covariance matrix [1,1]. */
    private double CRR;

    /** Object covariance matrix [2,1]. */
    private double CTR;

    /** Object covariance matrix [2,2]. */
    private double CTT;

    /** Object covariance matrix [3,1]. */
    private double CNR;

    /** Object covariance matrix [3,2]. */
    private double CNT;

    /** Object covariance matrix [3,3]. */
    private double CNN;

    /** Object covariance matrix [4,1]. */
    private double CRdotR;

    /** Object covariance matrix [4,2]. */
    private double CRdotT;

    /** Object covariance matrix [4,3]. */
    private double CRdotN;

    /** Object covariance matrix [4,4]. */
    private double CRdotRdot;

    /** Object covariance matrix [5,1]. */
    private double CTdotR;

    /** Object covariance matrix [5,2]. */
    private double CTdotT;

    /** Object covariance matrix [5,3]. */
    private double CTdotN;

    /** Object covariance matrix [5,4]. */
    private double CTdotRdot;

    /** Object covariance matrix [5,5]. */
    private double CTdotTdot;

    /** Object covariance matrix [6,1]. */
    private double CNdotR;

    /** Object covariance matrix [6,2]. */
    private double CNdotT;

    /** Object covariance matrix [6,3]. */
    private double CNdotN;

    /** Object covariance matrix [6,4]. */
    private double CNdotRdot;

    /** Object covariance matrix [6,5]. */
    private double CNdotTdot;

    /** Object covariance matrix [6,6]. */
    private double CNdotNdot;

    /** Object covariance matrix [7,1]. */
    private double CDRGR;

    /** Object covariance matrix [7,2]. */
    private double CDRGT;

    /** Object covariance matrix [7,3]. */
    private double CDRGN;

    /** Object covariance matrix [7,4]. */
    private double CDRGRdot;

    /** Object covariance matrix [7,5]. */
    private double CDRGTdot;

    /** Object covariance matrix [7,6]. */
    private double CDRGNdot;

    /** Object covariance matrix [7,7]. */
    private double  CDRGDRG;

    /** Object covariance matrix [8,1]. */
    private double  CSRPR;

    /** Object covariance matrix [8,2]. */
    private double CSRPT;

    /** Object covariance matrix [8,3]. */
    private double  CSRPN;

    /** Object covariance matrix [8,4]. */
    private double CSRPRdot;

    /** Object covariance matrix [8,5]. */
    private double CSRPTdot;

    /** Object covariance matrix [8,6]. */
    private double CSRPNdot;

    /** Object covariance matrix [8,7]. */
    private double CSRPDRG;

    /** Object covariance matrix [8,9]. */
    private double CSRPSRP;

    /** Object covariance matrix [9,1]. */
    private double CTHRR;

    /** Object covariance matrix [9,2]. */
    private double CTHRT;

    /** Object covariance matrix [9,3]. */
    private double CTHRN;

    /** Object covariance matrix [9,4]. */
    private double CTHRRdot;

    /** Object covariance matrix [9,5]. */
    private double CTHRTdot;

    /** Object covariance matrix [9,6]. */
    private double CTHRNdot;

    /** Object covariance matrix [9,7]. */
    private double CTHRDRG;

    /** Object covariance matrix [9,8]. */
    private double CTHRSRP;

    /** Object covariance matrix [9,9]. */
    private double CTHRTHR;

    /** Simple constructor.
     */
    public CovarianceMatrix() {
        CRR             = Double.NaN;
        CTR             = Double.NaN;
        CTT             = Double.NaN;
        CNR             = Double.NaN;
        CNT             = Double.NaN;
        CNN             = Double.NaN;
        CRdotR          = Double.NaN;
        CRdotT          = Double.NaN;
        CRdotN          = Double.NaN;
        CRdotRdot       = Double.NaN;
        CTdotR          = Double.NaN;
        CTdotT          = Double.NaN;
        CTdotN          = Double.NaN;
        CTdotRdot       = Double.NaN;
        CTdotTdot       = Double.NaN;
        CNdotR          = Double.NaN;
        CNdotT          = Double.NaN;
        CNdotN          = Double.NaN;
        CNdotRdot       = Double.NaN;
        CNdotTdot       = Double.NaN;
        CNdotNdot       = Double.NaN;
        CDRGR           = Double.NaN;
        CDRGT           = Double.NaN;
        CDRGN           = Double.NaN;
        CDRGRdot        = Double.NaN;
        CDRGTdot        = Double.NaN;
        CDRGNdot        = Double.NaN;
        CDRGDRG         = Double.NaN;
        CSRPR           = Double.NaN;
        CSRPT           = Double.NaN;
        CSRPN           = Double.NaN;
        CSRPRdot        = Double.NaN;
        CSRPTdot        = Double.NaN;
        CSRPNdot        = Double.NaN;
        CSRPDRG         = Double.NaN;
        CSRPSRP         = Double.NaN;
        CTHRR           = Double.NaN;
        CTHRT           = Double.NaN;
        CTHRN           = Double.NaN;
        CTHRRdot        = Double.NaN;
        CTHRTdot        = Double.NaN;
        CTHRNdot        = Double.NaN;
        CTHRDRG         = Double.NaN;
        CTHRSRP         = Double.NaN;
        CTHRTHR         = Double.NaN;

    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        // We only check values that are mandatory in a cdm file
        checkNotNaN(CRR,              CovarianceMatrixKey.CR_R);
        checkNotNaN(CTR,              CovarianceMatrixKey.CT_R);
        checkNotNaN(CTT,              CovarianceMatrixKey.CT_T);
        checkNotNaN(CNR,              CovarianceMatrixKey.CN_R);
        checkNotNaN(CNT,              CovarianceMatrixKey.CN_T);
        checkNotNaN(CNN,              CovarianceMatrixKey.CN_N);
        checkNotNaN(CRdotR,           CovarianceMatrixKey.CRDOT_R);
        checkNotNaN(CRdotT,           CovarianceMatrixKey.CRDOT_T);
        checkNotNaN(CRdotN,           CovarianceMatrixKey.CRDOT_N);
        checkNotNaN(CRdotRdot,        CovarianceMatrixKey.CRDOT_RDOT);
        checkNotNaN(CTdotR,           CovarianceMatrixKey.CTDOT_R);
        checkNotNaN(CTdotT,           CovarianceMatrixKey.CTDOT_T);
        checkNotNaN(CTdotN,           CovarianceMatrixKey.CTDOT_N);
        checkNotNaN(CTdotRdot,        CovarianceMatrixKey.CTDOT_RDOT);
        checkNotNaN(CTdotTdot,        CovarianceMatrixKey.CTDOT_TDOT);
        checkNotNaN(CNdotR,           CovarianceMatrixKey.CNDOT_R);
        checkNotNaN(CNdotT,           CovarianceMatrixKey.CNDOT_T);
        checkNotNaN(CNdotN,           CovarianceMatrixKey.CNDOT_N);
        checkNotNaN(CNdotRdot,        CovarianceMatrixKey.CNDOT_RDOT);
        checkNotNaN(CNdotTdot,        CovarianceMatrixKey.CNDOT_TDOT);
        checkNotNaN(CNdotNdot,        CovarianceMatrixKey.CNDOT_NDOT);

    }

    /**
     * Get the object [1,1] in covariance matrix.
     * @return the object [1,1] in covariance matrix
     */
    public double getCRR() {
        return CRR;
    }

    /**
     * Set the object [1,1] in covariance matrix.
     * @param CRR = object [1,1] in covariance matrix
     */
    public void setCRR(final double CRR) {
        refuseFurtherComments();
        this.CRR = CRR;
    }

    /**
     * Get the object [2,1] in covariance matrix.
     * @return the object [2,1] in covariance matrix
     */
    public double getCTR() {
        return CTR;
    }

    /**
     * Set the object [2,1] in covariance matrix.
     * @param CTR = object [2,1] in covariance matrix
     */
    public void setCTR(final double CTR) {
        refuseFurtherComments();
        this.CTR = CTR;
    }

    /**
     * Get the object [2,2] in covariance matrix.
     * @return the object [2,2] in covariance matrix
     */
    public double getCTT() {
        return CTT;
    }

    /**
     * Set the object [2,2] in covariance matrix.
     * @param CTT = object [2,2] in covariance matrix
     */
    public void setCTT(final double CTT) {
        refuseFurtherComments();
        this.CTT = CTT;
    }

    /**
     * Get the object [3,1] in covariance matrix.
     * @return the object [3,1] in covariance matrix
     */
    public double getCNR() {
        return CNR;
    }

    /**
     * Set the object [3,1] in covariance matrix.
     * @param CNR = object [3,1] in covariance matrix
     */
    public void setCNR(final double CNR) {
        refuseFurtherComments();
        this.CNR = CNR;
    }

    /**
     * Get the object [3,2] in covariance matrix.
     * @return the object [3,2] in covariance matrix
     */
    public double getCNT() {
        return CNT;
    }

    /**
     * Set the object [3,2] in covariance matrix.
     * @param CNT = object [3,2] in covariance matrix
     */
    public void setCNT(final double CNT) {
        refuseFurtherComments();
        this.CNT = CNT;
    }

    /**
     * Get the object [3,3] in covariance matrix.
     * @return the object [3,3] in covariance matrix
     */
    public double getCNN() {
        return CNN;
    }

    /**
     * Set the object [3,3] in covariance matrix.
     * @param CNN = object [3,3] in covariance matrix
     */
    public void setCNN(final double CNN) {
        refuseFurtherComments();
        this.CNN = CNN;
    }

    /**
     * Get the object [4,1] in covariance matrix.
     * @return the object [4,1] in covariance matrix
     */
    public double getCRdotR() {
        return CRdotR;
    }

    /**
     * Set the object [4,1] in covariance matrix.
     * @param CRdotR = object [4,1] in covariance matrix
     */
    public void setCRdotR(final double CRdotR) {
        refuseFurtherComments();
        this.CRdotR = CRdotR;
    }

    /**
     * Get the object [4,2] in covariance matrix.
     * @return the object [4,2] in covariance matrix
     */
    public double getCRdotT() {
        return CRdotT;
    }

    /**
     * Set the object [4,2] in covariance matrix.
     * @param CRdotT = object [4,2] in covariance matrix
     */
    public void setCRdotT(final double CRdotT) {
        refuseFurtherComments();
        this.CRdotT = CRdotT;
    }

    /**
     * Get the object [4,3] in covariance matrix.
     * @return the object [4,3] in covariance matrix
     */
    public double getCRdotN() {
        return CRdotN;
    }

    /**
     * Set the object [4,3] in covariance matrix.
     * @param CRdotN = object [4,3] in covariance matrix
     */
    public void setCRdotN(final double CRdotN) {
        refuseFurtherComments();
        this.CRdotN = CRdotN;
    }

    /**
     * Get the object [4,4] in covariance matrix.
     * @return the object [4,4] in covariance matrix
     */
    public double getCRdotRdot() {
        return CRdotRdot;
    }

    /**
     * Set the object [4,4] in covariance matrix.
     * @param CRdotRdot = object [4,4] in covariance matrix
     */
    public void setCRdotRdot(final double CRdotRdot) {
        refuseFurtherComments();
        this.CRdotRdot = CRdotRdot;
    }

    /**
     * Get the object [5,1] in covariance matrix.
     * @return the object [5,1] in covariance matrix
     */
    public double getCTdotR() {
        return CTdotR;
    }

    /**
     * Set the object [5,1] in covariance matrix.
     * @param CTdotR = object [5,1] in covariance matrix
     */
    public void setCTdotR(final double CTdotR) {
        refuseFurtherComments();
        this.CTdotR = CTdotR;
    }

    /**
     * Get the object [5,2] in covariance matrix.
     * @return the object [5,2] in covariance matrix
     */
    public double getCTdotT() {
        return CTdotT;
    }

    /**
     * Set the object [5,2] in covariance matrix.
     * @param CTdotT = object [5,2] in covariance matrix
     */
    public void setCTdotT(final double CTdotT) {
        refuseFurtherComments();
        this.CTdotT = CTdotT;
    }

    /**
     * Get the object [5,3] in covariance matrix.
     * @return the object [5,3] in covariance matrix
     */
    public double getCTdotN() {
        return CTdotN;
    }

    /**
     * Set the object [5,3] in covariance matrix.
     * @param CTdotN = object [5,3] in covariance matrix
     */
    public void setCTdotN(final double CTdotN) {
        refuseFurtherComments();
        this.CTdotN = CTdotN;
    }

    /**
     * Get the object [5,4] in covariance matrix.
     * @return the object [5,4] in covariance matrix
     */
    public double getCTdotRdot() {
        return CTdotRdot;
    }

    /**
     * Set the object [5,4] in covariance matrix.
     * @param CTdotRdot = object [5,4] in covariance matrix
     */
    public void setCTdotRdot(final double CTdotRdot) {
        refuseFurtherComments();
        this.CTdotRdot = CTdotRdot;
    }

    /**
     * Get the object [5,5] in covariance matrix.
     * @return the object [5,5] in covariance matrix
     */
    public double getCTdotTdot() {
        return CTdotTdot;
    }

    /**
     * Set the object [5,5] in covariance matrix.
     * @param CTdotTdot = object [5,5] in covariance matrix
     */
    public void setCTdotTdot(final double CTdotTdot) {
        refuseFurtherComments();
        this.CTdotTdot = CTdotTdot;
    }

    /**
     * Get the object [6,1] in covariance matrix.
     * @return the object [6,1] in covariance matrix
     */
    public double getCNdotR() {
        return CNdotR;
    }

    /**
     * Set the object [6,1] in covariance matrix.
     * @param CNdotR = object [6,1] in covariance matrix
     */
    public void setCNdotR(final double CNdotR) {
        refuseFurtherComments();
        this.CNdotR = CNdotR;
    }

    /**
     * Get the object [6,2] in covariance matrix.
     * @return the object [6,2] in covariance matrix
     */
    public double getCNdotT() {
        return CNdotT;
    }

    /**
     * Set the object [6,2] in covariance matrix.
     * @param CNdotT = object [6,2] in covariance matrix
     */
    public void setCNdotT(final double CNdotT) {
        refuseFurtherComments();
        this.CNdotT = CNdotT;
    }

    /**
     * Get the object [6,3] in covariance matrix.
     * @return the object [6,3] in covariance matrix
     */
    public double getCNdotN() {
        return CNdotN;
    }

    /**
     * Set the object [6,3] in covariance matrix.
     * @param CNdotN = object [6,3] in covariance matrix
     */
    public void setCNdotN(final double CNdotN) {
        refuseFurtherComments();
        this.CNdotN = CNdotN;
    }

    /**
     * Get the object [6,4] in covariance matrix.
     * @return the object [6,4] in covariance matrix
     */
    public double getCNdotRdot() {
        return CNdotRdot;
    }

    /**
     * Set the object [6,4] in covariance matrix.
     * @param CNdotRdot = object [6,4] in covariance matrix
     */
    public void setCNdotRdot(final double CNdotRdot) {
        refuseFurtherComments();
        this.CNdotRdot = CNdotRdot;
    }

    /**
     * Get the object [6,5] in covariance matrix.
     * @return the object [6,5] in covariance matrix
     */
    public double getCNdotTdot() {
        return CNdotTdot;
    }

    /**
     * Set the object [6,5] in covariance matrix.
     * @param CNdotTdot = object [6,5] in covariance matrix
     */
    public void setCNdotTdot(final double CNdotTdot) {
        refuseFurtherComments();
        this.CNdotTdot = CNdotTdot;
    }

    /**
     * Get the object [6,6] in covariance matrix.
     * @return the object [6,6] in covariance matrix
     */
    public double getCNdotNdot() {
        return CNdotNdot;
    }

    /**
     * Set the object [6,6] in covariance matrix.
     * @param CNdotNdot = object [6,6] in covariance matrix
     */
    public void setCNdotNdot(final double CNdotNdot) {
        refuseFurtherComments();
        this.CNdotNdot = CNdotNdot;
    }

    /**
     * Get the object [7,1] in covariance matrix.
     * @return the object [7,1] in covariance matrix
     */
    public double getCDRGR() {
        return CDRGR;
    }

    /**
     * Set the object [7,1] in covariance matrix.
     * @param CDRGR = object [7,1] in covariance matrix
     */
    public void setCDRGR(final double CDRGR) {
        refuseFurtherComments();
        this.CDRGR = CDRGR;
    }

    /**
     * Get the object [7,2] in covariance matrix.
     * @return the object [7,2] in covariance matrix
     */
    public double getCDRGT() {
        return CDRGT;
    }

    /**
     * Set the object [7,2] in covariance matrix.
     * @param CDRGT = object [7,2] in covariance matrix
     */
    public void setCDRGT(final double CDRGT) {
        refuseFurtherComments();
        this.CDRGT = CDRGT;
    }

    /**
     * Get the object [7,3] in covariance matrix.
     * @return the object [7,3] in covariance matrix
     */
    public double getCDRGN() {
        return CDRGN;
    }

    /**
     * Set the object [7,3] in covariance matrix.
     * @param CDRGN = object [7,3] in covariance matrix
     */
    public void setCDRGN(final double CDRGN) {
        refuseFurtherComments();
        this.CDRGN = CDRGN;
    }

    /**
     * Get the object [7,4] in covariance matrix.
     * @return the object [7,4] in covariance matrix
     */
    public double getCDRGRdot() {
        return CDRGRdot;
    }

    /**
     * Set the object [7,4] in covariance matrix.
     * @param CDRGRdot = object [7,4] in covariance matrix
     */
    public void setCDRGRdot(final double CDRGRdot) {
        refuseFurtherComments();
        this.CDRGRdot = CDRGRdot;
    }

    /**
     * Get the object [7,5] in covariance matrix.
     * @return the object [7,5] in covariance matrix
     */
    public double getCDRGTdot() {
        return CDRGTdot;
    }

    /**
     * Set the object [7,5] in covariance matrix.
     * @param CDRGTdot = object [7,5] in covariance matrix
     */
    public void setCDRGTdot(final double CDRGTdot) {
        refuseFurtherComments();
        this.CDRGTdot = CDRGTdot;
    }

    /**
     * Get the object [7,6] in covariance matrix.
     * @return the object [7,6] in covariance matrix
     */
    public double getCDRGNdot() {
        return CDRGNdot;
    }

    /**
     * Set the object [7,6] in covariance matrix.
     * @param CDRGNdot = object [7,6] in covariance matrix
     */
    public void setCDRGNdot(final double CDRGNdot) {
        refuseFurtherComments();
        this.CDRGNdot = CDRGNdot;
    }

    /**
     * Get the object [7,7] in covariance matrix.
     * @return the object [7,7] in covariance matrix
     */
    public double getCDRGDRG() {
        return CDRGDRG;
    }

    /**
     * Set the object [7,7] in covariance matrix.
     * @param CDRGDRG = object [7,7] in covariance matrix
     */
    public void setCDRGDRG(final double CDRGDRG) {
        refuseFurtherComments();
        this.CDRGDRG = CDRGDRG;
    }

    /**
     * Get the object [8,1] in covariance matrix.
     * @return the object [8,1] in covariance matrix
     */
    public double getCSRPR() {
        return CSRPR;
    }

    /**
     * Set the object [8,1] in covariance matrix.
     * @param CSRPR = object [8,1] in covariance matrix
     */
    public void setCSRPR(final double CSRPR) {
        refuseFurtherComments();
        this.CSRPR = CSRPR;
    }

    /**
     * Get the object [8,2] in covariance matrix.
     * @return the object [8,2] in covariance matrix
     */
    public double getCSRPT() {
        return CSRPT;
    }

    /**
     * Set the object [8,2] in covariance matrix.
     * @param CSRPT = object [8,2] in covariance matrix
     */
    public void setCSRPT(final double CSRPT) {
        refuseFurtherComments();
        this.CSRPT = CSRPT;
    }

    /**
     * Get the object [8,3] in covariance matrix.
     * @return the object [8,3] in covariance matrix
     */
    public double getCSRPN() {
        return CSRPN;
    }

    /**
     * Set the object [8,3] in covariance matrix.
     * @param CSRPN = object [8,3] in covariance matrix
     */
    public void setCSRPN(final double CSRPN) {
        refuseFurtherComments();
        this.CSRPN = CSRPN;
    }

    /**
     * Get the object [8,4] in covariance matrix.
     * @return the object [8,4] in covariance matrix
     */
    public double getCSRPRdot() {
        return CSRPRdot;
    }

    /**
     * Set the object [8,4] in covariance matrix.
     * @param CSRPRdot = object [8,4] in covariance matrix
     */
    public void setCSRPRdot(final double CSRPRdot) {
        refuseFurtherComments();
        this.CSRPRdot = CSRPRdot;
    }

    /**
     * Get the object [8,5] in covariance matrix.
     * @return the object [8,5] in covariance matrix
     */
    public double getCSRPTdot() {
        return CSRPTdot;
    }

    /**
     * Set the object [8,5] in covariance matrix.
     * @param CSRPTdot = object [8,5] in covariance matrix
     */
    public void setCSRPTdot(final double CSRPTdot) {
        refuseFurtherComments();
        this.CSRPTdot = CSRPTdot;
    }

    /**
     * Get the object [8,6] in covariance matrix.
     * @return the object [8,6] in covariance matrix
     */
    public double getCSRPNdot() {
        return CSRPNdot;
    }

    /**
     * Set the object [8,6] in covariance matrix.
     * @param CSRPNdot = object [8,6] in covariance matrix
     */
    public void setCSRPNdot(final double CSRPNdot) {
        refuseFurtherComments();
        this.CSRPNdot = CSRPNdot;
    }

    /**
     * Get the object [8,7] in covariance matrix.
     * @return the object [8,7] in covariance matrix
     */
    public double getCSRPDRG() {
        return CSRPDRG;
    }

    /**
     * Set the object [8,7] in covariance matrix.
     * @param CSRPDRG = object [8,7] in covariance matrix
     */
    public void setCSRPDRG(final double CSRPDRG) {
        this.CSRPDRG = CSRPDRG;
    }

    /**
     * Get the object [8,8] in covariance matrix.
     * @return the object [8,8] in covariance matrix
     */
    public double getCSRPSRP() {
        return CSRPSRP;
    }

    /**
     * Set the object [8,8] in covariance matrix.
     * @param CSRPSRP = object [8,8] in covariance matrix
     */
    public void setCSRPSRP(final double CSRPSRP) {
        this.CSRPSRP = CSRPSRP;
    }

    /**
     * Get the object [9,1] in covariance matrix.
     * @return the object [9,1] in covariance matrix
     */
    public double getCTHRR() {
        return CTHRR;
    }

    /**
     * Set the object [9,1] in covariance matrix.
     * @param CTHRR = object [9,1] in covariance matrix
     */
    public void setCTHRR(final double CTHRR) {
        refuseFurtherComments();
        this.CTHRR = CTHRR;
    }

    /**
     * Get the object [9,2] in covariance matrix.
     * @return the object [9,2] in covariance matrix
     */
    public double getCTHRT() {
        return CTHRT;
    }

    /**
     * Set the object [9,2] in covariance matrix.
     * @param CTHRT = object [9,2] in covariance matrix
     */
    public void setCTHRT(final double CTHRT) {
        refuseFurtherComments();
        this.CTHRT = CTHRT;
    }

    /**
     * Get the object [9,3] in covariance matrix.
     * @return the object [9,3] in covariance matrix
     */
    public double getCTHRN() {
        return CTHRN;
    }

    /**
     * Set the object [9,3] in covariance matrix.
     * @param CTHRN = object [9,3] in covariance matrix
     */
    public void setCTHRN(final double CTHRN) {
        refuseFurtherComments();
        this.CTHRN = CTHRN;
    }

    /**
     * Get the object [9,4] in covariance matrix.
     * @return the object [9,4] in covariance matrix
     */
    public double getCTHRRdot() {
        return CTHRRdot;
    }

    /**
     * Set the object [9,4] in covariance matrix.
     * @param CTHRRdot = object [9,4] in covariance matrix
     */
    public void setCTHRRdot(final double CTHRRdot) {
        refuseFurtherComments();
        this.CTHRRdot = CTHRRdot;
    }

    /**
     * Get the object [9,5] in covariance matrix.
     * @return the object [9,5] in covariance matrix
     */
    public double getCTHRTdot() {
        return CTHRTdot;
    }

    /**
     * Set the object [9,5] in covariance matrix.
     * @param CTHRTdot = object [9,5] in covariance matrix
     */
    public void setCTHRTdot(final double CTHRTdot) {
        refuseFurtherComments();
        this.CTHRTdot = CTHRTdot;
    }

    /**
     * Get the object [9,6] in covariance matrix.
     * @return the object [9,6] in covariance matrix
     */
    public double getCTHRNdot() {
        return CTHRNdot;
    }

    /**
     * Set the object [9,6] in covariance matrix.
     * @param CTHRNdot = object [9,6] in covariance matrix
     */
    public void setCTHRNdot(final double CTHRNdot) {
        refuseFurtherComments();
        this.CTHRNdot = CTHRNdot;
    }

    /**
     * Get the object [9,7] in covariance matrix.
     * @return the object [9,7] in covariance matrix
     */
    public double getCTHRDRG() {
        return CTHRDRG;
    }

    /**
     * Set the object [9,7] in covariance matrix.
     * @param CTHRDRG = object [9,7] in covariance matrix
     */
    public void setCTHRDRG(final double CTHRDRG) {
        refuseFurtherComments();
        this.CTHRDRG = CTHRDRG;
    }

    /**
     * Get the object [9,8] in covariance matrix.
     * @return the object [9,8] in covariance matrix
     */
    public double getCTHRSRP() {
        return CTHRSRP;
    }

    /**
     * Set the object [9,8] in covariance matrix.
     * @param CTHRSRP = object [9,8] in covariance matrix
     */
    public void setCTHRSRP(final double CTHRSRP) {
        refuseFurtherComments();
        this.CTHRSRP = CTHRSRP;
    }

    /**
     * Get the object [9,9] in covariance matrix.
     * @return the object [9,9] in covariance matrix
     */
    public double getCTHRTHR() {
        return CTHRTHR;
    }

    /**
     * Set the object [9,9] in covariance matrix.
     * @param CTHRTHR = object [9,9] in covariance matrix
     */
    public void setCTHRTHR(final double CTHRTHR) {
        refuseFurtherComments();
        this.CTHRTHR = CTHRTHR;
    }

}
