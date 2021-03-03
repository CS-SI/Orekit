/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Metadata for covariance history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CovarianceHistoryMetadata extends CommentsContainer {

    /** Covariance identification number. */
    private String covID;

    /** Identification number of previous covariance. */
    private String covPrevID;

    /** Identification number of next covariance. */
    private String covNextID;

    /** Basis of this covariance time history data. */
    private String covBasis;

    /** Identification number of the covariance determination or simulation upon which this covariance is based. */
    private String covBasisID;

    /** Reference frame of the covariance. */
    private FrameFacade covReferenceFrame;

    /** Epoch of the covariance reference frame. */
    private AbsoluteDate covFrameEpoch;

    /** Minimum scale factor to apply to achieve realism. */
    private double covScaleMin;

    /** Maximum scale factor to apply to achieve realism. */
    private double covScaleMax;

    /** Measure of confidence in covariance error matching reality. */
    private String covConfidence;

    /** Covariance element set type. */
    private ElementsType covType;

    /** Units of covariance element set. */
    private List<Unit> covUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    CovarianceHistoryMetadata(final AbsoluteDate epochT0) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        covBasis            = "PREDICTED";
        covReferenceFrame   = new FrameFacade(null, null,
                                              OrbitRelativeFrame.TNW_INERTIAL, null,
                                              OrbitRelativeFrame.TNW_INERTIAL.name());
        covFrameEpoch       = epochT0;
        covScaleMin         = 1.0;
        covScaleMax         = 1.0;
        covType             = ElementsType.CARTPV;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(covUnits, CovarianceHistoryMetadataKey.COV_UNITS);
        covType.checkUnits(covUnits);
    }

    /** Get covariance identification number.
     * @return covariance identification number
     */
    public String getCovID() {
        return covID;
    }

    /** Set covariance identification number.
     * @param orbID covariance identification number
     */
    void setCovID(final String orbID) {
        refuseFurtherComments();
        this.covID = orbID;
    }

    /** Get identification number of previous covariance.
     * @return identification number of previous covariance
     */
    public String getCovPrevID() {
        return covPrevID;
    }

    /** Set identification number of previous covariance.
     * @param orbPrevID identification number of previous covariance
     */
    void setCovPrevID(final String orbPrevID) {
        refuseFurtherComments();
        this.covPrevID = orbPrevID;
    }

    /** Get identification number of next covariance.
     * @return identification number of next covariance
     */
    public String getCovNextID() {
        return covNextID;
    }

    /** Set identification number of next covariance.
     * @param orbNextID identification number of next covariance
     */
    void setCovNextID(final String orbNextID) {
        refuseFurtherComments();
        this.covNextID = orbNextID;
    }

    /** Get basis of this covariance time history data.
     * @return basis of this covariance time history data
     */
    public String getCovBasis() {
        return covBasis;
    }

    /** Set basis of this covariance time history data.
     * @param orbBasis basis of this covariance time history data
     */
    void setCovBasis(final String orbBasis) {
        refuseFurtherComments();
        this.covBasis = orbBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this covariance is based.
     * @return identification number of the orbit determination or simulation upon which this covariance is based
     */
    public String getCovBasisID() {
        return covBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this covariance is based.
     * @param orbBasisID identification number of the orbit determination or simulation upon which this covariance is based
     */
    void setCovBasisID(final String orbBasisID) {
        refuseFurtherComments();
        this.covBasisID = orbBasisID;
    }

    /** Get reference frame of the covariance.
     * @return reference frame of the covariance
     */
    public FrameFacade getCovReferenceFrame() {
        return covReferenceFrame;
    }

    /** Set reference frame of the covariance.
     * @param orbReferenceFrame the reference frame to be set
     */
    void setCovReferenceFrame(final FrameFacade orbReferenceFrame) {
        refuseFurtherComments();
        this.covReferenceFrame = orbReferenceFrame;
    }

    /** Get epoch of the {@link #getCovRefFrame() covariance reference frame}.
     * @return epoch of the {@link #getCovRefFrame() covariance reference frame}
     */
    public AbsoluteDate getCovFrameEpoch() {
        return covFrameEpoch;
    }

    /** Set epoch of the {@link #getCovRefFrame() covariance reference frame}.
     * @param orbFrameEpoch epoch of the {@link #getCovRefFrame() covariance reference frame}
     */
    void setCovFrameEpoch(final AbsoluteDate orbFrameEpoch) {
        refuseFurtherComments();
        this.covFrameEpoch = orbFrameEpoch;
    }

    /** Set the minimum scale factor to apply to achieve realism.
     * @param covScaleMin minimum scale factor to apply to achieve realism
     */
    public void setCovScaleMin(final double covScaleMin) {
        this.covScaleMin = covScaleMin;
    }

    /** Get the minimum scale factor to apply to achieve realism.
     * @return minimum scale factor to apply to achieve realism
     */
    public double getCovScaleMin() {
        return covScaleMin;
    }

    /** Set the maximum scale factor to apply to achieve realism.
     * @param covScaleMax maximum scale factor to apply to achieve realism
     */
    public void setCovScaleMax(final double covScaleMax) {
        this.covScaleMax = covScaleMax;
    }

    /** Get the maximum scale factor to apply to achieve realism.
     * @return maximum scale factor to apply to achieve realism
     */
    public double getCovScaleMax() {
        return covScaleMax;
    }

    /** Set the measure of confidence in covariance error matching reality.
     * @param covConfidence measure of confidence in covariance error matching reality
     */
    public void setCovConfidence(final String covConfidence) {
        this.covConfidence = covConfidence;
    }

    /** Get the measure of confidence in covariance error matching reality.
     * @return measure of confidence in covariance error matching reality
     */
    public String getCovConfidence() {
        return covConfidence;
    }

    /** Get covariance element set type.
     * @return covariance element set type
     */
    public ElementsType getCovType() {
        return covType;
    }

    /** Set covariance element set type.
     * @param orbType covariance element set type
     */
    void setCovType(final ElementsType orbType) {
        refuseFurtherComments();
        this.covType = orbType;
    }

    /** Get covariance element set units.
     * @return covariance element set units
     */
    public List<Unit> getCovUnits() {
        return covUnits;
    }

    /** Set covariance element set units.
     * @param orbUnits covariance element set units
     */
    void setCovUnits(final List<Unit> orbUnits) {
        refuseFurtherComments();
        this.covUnits = orbUnits;
    }

}
