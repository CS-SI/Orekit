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
public class OrbitCovarianceHistoryMetadata extends CommentsContainer {

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
    private double covConfidence;

    /** Covariance element set type. */
    private OrbitElementsType covType;

    /** Covariance ordering. */
    private Ordering covOrdering;

    /** Units of covariance element set. */
    private List<Unit> covUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    public OrbitCovarianceHistoryMetadata(final AbsoluteDate epochT0) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        covBasis          = "PREDICTED";
        covReferenceFrame = new FrameFacade(null, null,
                                            OrbitRelativeFrame.TNW_INERTIAL, null,
                                            OrbitRelativeFrame.TNW_INERTIAL.name());
        covFrameEpoch     = epochT0;
        covScaleMin       = Double.NaN;
        covScaleMax       = Double.NaN;
        covConfidence     = Double.NaN;
        covType           = OrbitElementsType.CARTPV;
        covOrdering       = Ordering.LTM;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        if (covUnits != null) {
            Unit.ensureCompatible(covType.toString(), covType.getUnits(), false, covUnits);
        }
    }

    /** Get covariance identification number.
     * @return covariance identification number
     */
    public String getCovID() {
        return covID;
    }

    /** Set covariance identification number.
     * @param covID covariance identification number
     */
    public void setCovID(final String covID) {
        refuseFurtherComments();
        this.covID = covID;
    }

    /** Get identification number of previous covariance.
     * @return identification number of previous covariance
     */
    public String getCovPrevID() {
        return covPrevID;
    }

    /** Set identification number of previous covariance.
     * @param covPrevID identification number of previous covariance
     */
    public void setCovPrevID(final String covPrevID) {
        refuseFurtherComments();
        this.covPrevID = covPrevID;
    }

    /** Get identification number of next covariance.
     * @return identification number of next covariance
     */
    public String getCovNextID() {
        return covNextID;
    }

    /** Set identification number of next covariance.
     * @param covNextID identification number of next covariance
     */
    public void setCovNextID(final String covNextID) {
        refuseFurtherComments();
        this.covNextID = covNextID;
    }

    /** Get basis of this covariance time history data.
     * @return basis of this covariance time history data
     */
    public String getCovBasis() {
        return covBasis;
    }

    /** Set basis of this covariance time history data.
     * @param covBasis basis of this covariance time history data
     */
    public void setCovBasis(final String covBasis) {
        refuseFurtherComments();
        this.covBasis = covBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this covariance is based.
     * @return identification number of the orbit determination or simulation upon which this covariance is based
     */
    public String getCovBasisID() {
        return covBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this covariance is based.
     * @param covBasisID identification number of the orbit determination or simulation upon which this covariance is based
     */
    public void setCovBasisID(final String covBasisID) {
        refuseFurtherComments();
        this.covBasisID = covBasisID;
    }

    /** Get reference frame of the covariance.
     * @return reference frame of the covariance
     */
    public FrameFacade getCovReferenceFrame() {
        return covReferenceFrame;
    }

    /** Set reference frame of the covariance.
     * @param covReferenceFrame the reference frame to be set
     */
    public void setCovReferenceFrame(final FrameFacade covReferenceFrame) {
        refuseFurtherComments();
        this.covReferenceFrame = covReferenceFrame;
    }

    /** Get epoch of the {@link #getCovReferenceFrame() covariance reference frame}.
     * @return epoch of the {@link #getCovReferenceFrame() covariance reference frame}
     */
    public AbsoluteDate getCovFrameEpoch() {
        return covFrameEpoch;
    }

    /** Set epoch of the {@link #getCovReferenceFrame() covariance reference frame}.
     * @param covFrameEpoch epoch of the {@link #getCovReferenceFrame() covariance reference frame}
     */
    public void setCovFrameEpoch(final AbsoluteDate covFrameEpoch) {
        refuseFurtherComments();
        this.covFrameEpoch = covFrameEpoch;
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
    public void setCovConfidence(final double covConfidence) {
        this.covConfidence = covConfidence;
    }

    /** Get the measure of confidence in covariance error matching reality.
     * @return measure of confidence in covariance error matching reality
     */
    public double getCovConfidence() {
        return covConfidence;
    }

    /** Get covariance element set type.
     * @return covariance element set type
     */
    public OrbitElementsType getCovType() {
        return covType;
    }

    /** Set covariance element set type.
     * @param covType covariance element set type
     */
    public void setCovType(final OrbitElementsType covType) {
        refuseFurtherComments();
        this.covType = covType;
    }

    /** Get covariance ordering.
     * @return covariance ordering
     */
    public Ordering getCovOrdering() {
        return covOrdering;
    }

    /** Set covariance ordering.
     * @param covOrdering covariance ordering
     */
    public void setCovOrdering(final Ordering covOrdering) {
        refuseFurtherComments();
        this.covOrdering = covOrdering;
    }

    /** Get covariance element set units.
     * @return covariance element set units
     */
    public List<Unit> getCovUnits() {
        return covUnits;
    }

    /** Set covariance element set units.
     * @param covUnits covariance element set units
     */
    public void setCovUnits(final List<Unit> covUnits) {
        refuseFurtherComments();
        this.covUnits = covUnits;
    }

}
