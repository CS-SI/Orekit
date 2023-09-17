/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;

/** Metadata for covariance history.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeCovarianceHistoryMetadata extends CommentsContainer {

    /** Covariance identification number. */
    private String covID;

    /** Identification number of previous covariance. */
    private String covPrevID;

    /** Basis of this covariance time history data. */
    private String covBasis;

    /** Identification number of the covariance determination or simulation upon which this covariance is based. */
    private String covBasisID;

    /** Reference frame of the covariance. */
    private FrameFacade covReferenceFrame;

    /** Covariance element set type. */
    private AttitudeCovarianceType covType;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public AttitudeCovarianceHistoryMetadata() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(covType, AttitudeCovarianceHistoryMetadataKey.COV_TYPE.name());
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

    /** Get covariance element set type.
     * @return covariance element set type
     */
    public AttitudeCovarianceType getCovType() {
        return covType;
    }

    /** Set covariance element set type.
     * @param covType covariance element set type
     */
    public void setCovType(final AttitudeCovarianceType covType) {
        refuseFurtherComments();
        this.covType = covType;
    }

}
