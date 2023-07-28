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

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/** Metadata for attitude state history.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeStateHistoryMetadata extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** Attitude identification number. */
    private String attID;

    /** Identification number of previous attitude. */
    private String attPrevID;

    /** Basis of this attitude state time history data. */
    private String attBasis;

    /** Identification number of the attitude determination or simulation upon which this attitude is based. */
    private String attBasisID;

    /** Rotation order for Euler angles. */
    private RotationOrder eulerRotSeq;

    /** Number of data states included (attitude components plus rates components). */
    private int nbStates;

    /** Attitude element set type. */
    private AttitudeElementsType attitudeType;

    /** Attitude rate element set type. */
    private RateElementsType rateType;

    /** Simple constructor.
     */
    public AttitudeStateHistoryMetadata() {
        endpoints = new AttitudeEndpoints();
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        endpoints.checkExternalFrame(AttitudeStateHistoryMetadataKey.REF_FRAME_A,
                                     AttitudeStateHistoryMetadataKey.REF_FRAME_B);
        checkNotNull(attitudeType, AttitudeStateHistoryMetadataKey.ATT_TYPE.name());
        final int rateSize = rateType == null ? 0 : rateType.getUnits().size();
        if (nbStates != attitudeType.getUnits().size() + rateSize) {
            throw new OrekitException(OrekitMessages.CCSDS_INCONSISTENT_NUMBER_OF_ATTITUDE_STATES,
                                      attitudeType.toString(), rateType.toString(),
                                      attitudeType.getUnits().size() + rateSize, nbStates);
        }
        if (attitudeType == AttitudeElementsType.EULER_ANGLES) {
            checkNotNull(eulerRotSeq, AttitudeStateHistoryMetadataKey.EULER_ROT_SEQ.name());
        }
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
        return endpoints;
    }

    /** Get attitude identification number.
     * @return attitude identification number
     */
    public String getAttID() {
        return attID;
    }

    /** Set attitude identification number.
     * @param attID attitude identification number
     */
    public void setAttID(final String attID) {
        refuseFurtherComments();
        this.attID = attID;
    }

    /** Get identification number of previous attitude.
     * @return identification number of previous attitude
     */
    public String getAttPrevID() {
        return attPrevID;
    }

    /** Set identification number of previous attitude.
     * @param attPrevID identification number of previous attitude
     */
    public void setAttPrevID(final String attPrevID) {
        refuseFurtherComments();
        this.attPrevID = attPrevID;
    }

    /** Get basis of this attitude state time history data.
     * @return basis of this attitude state time history data
     */
    public String getAttBasis() {
        return attBasis;
    }

    /** Set basis of this attitude state time history data.
     * @param attBasis basis of this attitude state time history data
     */
    public void setAttBasis(final String attBasis) {
        refuseFurtherComments();
        this.attBasis = attBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this attitude is based.
     * @return identification number of the orbit determination or simulation upon which this attitude is based
     */
    public String getAttBasisID() {
        return attBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this attitude is based.
     * @param attBasisID identification number of the orbit determination or simulation upon which this attitude is based
     */
    public void setAttBasisID(final String attBasisID) {
        refuseFurtherComments();
        this.attBasisID = attBasisID;
    }

    /** Get the rotation order for Euler angles.
     * @return rotation order for Euler angles
     */
    public RotationOrder getEulerRotSeq() {
        return eulerRotSeq;
    }

    /** Set the rotation order for Euler angles.
     * @param eulerRotSeq rotation order for Euler angles
     */
    public void setEulerRotSeq(final RotationOrder eulerRotSeq) {
        this.eulerRotSeq = eulerRotSeq;
    }

    /** Get the number of data states included (attitude components plus rates components).
     * @return number of data states included (attitude components plus rates components)
     */
    public int getNbStates() {
        return nbStates;
    }

    /** Set the number of data states included (attitude components plus rates components).
     * @param nbStates number of data states included (attitude components plus rates components)
     */
    public void setNbStates(final int nbStates) {
        this.nbStates = nbStates;
    }

    /** Get attitude element set type.
     * @return attitude element set type
     */
    public AttitudeElementsType getAttitudeType() {
        return attitudeType;
    }

    /** Set attitude element set type.
     * @param attitudeType attitude element set type
     */
    public void setAttitudeType(final AttitudeElementsType attitudeType) {
        refuseFurtherComments();
        this.attitudeType = attitudeType;
    }

    /** Get attitude rate element set type.
     * @return attitude rate element set type
     */
    public RateElementsType getRateType() {
        return rateType;
    }

    /** Set attitude rate element set type.
     * @param rateType attitude rate element set type
     */
    public void setRateType(final RateElementsType rateType) {
        refuseFurtherComments();
        this.rateType = rateType;
    }

}
