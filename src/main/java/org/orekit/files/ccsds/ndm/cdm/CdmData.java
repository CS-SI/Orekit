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

import java.util.List;

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;

/**
 * Container for Conjunction Data Message data.
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmData implements Data {

    /** General comments block. */
    private final CommentsContainer commentsBlock;

    /** Quaternion block. */
    private ODParameters ODParametersBlock;

    /** Euler angles block. */
    private AdditionalParameters additionalParametersBlock;

    /** Spin-stabilized block. */
    private final StateVector stateVectorBlock;

    /** Spacecraft parameters block. */
    private RTNCovariance covarianceMatrixBlock;

    /** XYZ covariance block. */
    private final XYZCovariance xyzCovarianceMatrixBlock;

    /** Sigma/Eigenvectors covariance block. */
    private final SigmaEigenvectorsCovariance sig3EigVec3CovarianceBlock;

    /** Type of alternate covariance, if present. */
    private AltCovarianceType altCovarianceType;

    /** Additional Covariance Metadata block. */
    private AdditionalCovarianceMetadata additionalCovMetadata;

    /** The block containing the user defined parameters. */
    private UserDefined userDefinedBlock;





     /** Default constructor.
     * @param commentsBlock general comments block
     * @param ODParametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param xyzCovarianceBlock XYZ covariance matrix block
     * @param sig3EigVec3CovarianceBlock sigma/eigenvector covariance block
     * @param altCovarianceType type of alternate covariance
     * @param additionalCovMetadata additional covariance metadata
     */
    private CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODParametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final XYZCovariance xyzCovarianceBlock,
                   final SigmaEigenvectorsCovariance sig3EigVec3CovarianceBlock,
                   final AltCovarianceType altCovarianceType,
                   final AdditionalCovarianceMetadata additionalCovMetadata) {
        this.commentsBlock             = commentsBlock;
        this.ODParametersBlock         = ODParametersBlock;
        this.additionalParametersBlock = additionalParametersBlock;
        this.stateVectorBlock               = stateVectorBlock;
        this.covarianceMatrixBlock          = covarianceMatrixBlock;
        this.xyzCovarianceMatrixBlock       = xyzCovarianceBlock;
        this.sig3EigVec3CovarianceBlock     = sig3EigVec3CovarianceBlock;
        this.altCovarianceType              = altCovarianceType;
        this.additionalCovMetadata          = additionalCovMetadata;
        this.userDefinedBlock               = null;
    }

     /**  Constructor with RTN covariance.
     * @param commentsBlock general comments block
     * @param ODParametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block

     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODParametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock) {
        this(commentsBlock, ODParametersBlock, additionalParametersBlock, stateVectorBlock,
             covarianceMatrixBlock, null, null, null, null);
    }

     /**  Constructor with RTN covariance.
     * @param commentsBlock general comments block
     * @param ODParametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param additionalCovMetadata additional covariance metadata
     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODParametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final AdditionalCovarianceMetadata additionalCovMetadata) {
        this(commentsBlock, ODParametersBlock, additionalParametersBlock, stateVectorBlock,
             covarianceMatrixBlock, null, null, null, additionalCovMetadata);
    }

     /**  Constructor with RTN and XYZ covariance.
     * @param commentsBlock general comments block
     * @param ODParametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param xyzCovarianceBlock XYZ covariance matrix block
     * @param additionalCovMetadata additional covariance metadata
     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODParametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final XYZCovariance xyzCovarianceBlock,
                   final AdditionalCovarianceMetadata additionalCovMetadata) {
        this(commentsBlock, ODParametersBlock, additionalParametersBlock, stateVectorBlock,
             covarianceMatrixBlock, xyzCovarianceBlock, null, AltCovarianceType.XYZ, additionalCovMetadata);
    }

     /**  Constructor with RTN and sigma/eigenvector covariance.
     * @param commentsBlock general comments block
     * @param ODParametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param sig3EigVec3CovarianceBlock sigma/eigenvector covariance block
     * @param additionalCovMetadata additional covariance metadata
     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODParametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final SigmaEigenvectorsCovariance sig3EigVec3CovarianceBlock,
                   final AdditionalCovarianceMetadata additionalCovMetadata) {
        this(commentsBlock, ODParametersBlock, additionalParametersBlock, stateVectorBlock,
             covarianceMatrixBlock, null, sig3EigVec3CovarianceBlock, AltCovarianceType.CSIG3EIGVEC3, additionalCovMetadata);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (ODParametersBlock != null) {
            ODParametersBlock.validate(version);
        }
        if (additionalParametersBlock != null) {
            additionalParametersBlock.validate(version);
        }
        stateVectorBlock.validate(version);

        // covariance options
        if (altCovarianceType == null) {
            covarianceMatrixBlock.validate(version);
        } else if (altCovarianceType == AltCovarianceType.XYZ) {
            xyzCovarianceMatrixBlock.validate(version);
        } else if (altCovarianceType == AltCovarianceType.CSIG3EIGVEC3) {
            sig3EigVec3CovarianceBlock.validate(version);
        }

    }

    /** Get the comments.
     * @return comments
     */
    public List<String> getComments() {
        return commentsBlock.getComments();
    }

    /** Get the OD parameters logical block.
     * @return OD parameters block (may be null)
     */
    public ODParameters getODParametersBlock() {
        return ODParametersBlock;
    }

    /** Set the OD parameters logical block.
     * @param ODParametersBlock the OD Parameters logical block
     */
    public void setODParametersBlock(final ODParameters ODParametersBlock) {
        this.ODParametersBlock = ODParametersBlock;
    }

    /** Get the additional parameters logical block.
     * @return additional parameters block (may be null)
     */
    public AdditionalParameters getAdditionalParametersBlock() {
        return additionalParametersBlock;
    }

    /** Set the additional parameters logical block.
     * @param additionalParametersBlock the additional parameters logical block
     */
    public void setAdditionalParametersBlock(final AdditionalParameters additionalParametersBlock) {
        this.additionalParametersBlock = additionalParametersBlock;
    }

    /** Get the state vector logical block.
     * @return state vector block
     */
    public StateVector getStateVectorBlock() {
        return stateVectorBlock;
    }

    /** Get the covariance matrix logical block.
     * <p> The RTN Covariance Matrix is provided in the 9×9 Lower Triangular Form. All parameters of the 6×6 position/velocity submatrix
     * are mandatory. The remaining elements will return NaN if not provided. </p>
     * @return covariance matrix block
     */
    public RTNCovariance getRTNCovarianceBlock() {
        return covarianceMatrixBlock;
    }

    /** Get the Covariance Matrix in the XYZ Coordinate Frame (defined by value of {@link CdmMetadataKey#ALT_COV_REF_FRAME}).
     * <p> This block is not mandatory and on condition that {@link CdmMetadataKey#ALT_COV_TYPE} = {@link AltCovarianceType#XYZ}.
     * <p> This method will return null if the block is not defined in the CDM. </p>
     * @return XYZ covariance matrix block
     */
    public XYZCovariance getXYZCovarianceBlock() {
        return xyzCovarianceMatrixBlock;
    }

    /** Get the Sigma / Eigenvector covariance logical block.
     * <p> This block is not mandatory and on condition that {@link CdmMetadataKey#ALT_COV_TYPE} = {@link AltCovarianceType#CSIG3EIGVEC3}.
     * <p> This method will return null if the block is not defined in the CDM. </p>
     * @return the Sigma / Eigenvector covariance block
     */
    public SigmaEigenvectorsCovariance getSig3EigVec3CovarianceBlock() {
        return sig3EigVec3CovarianceBlock;
    }

    /** Get the additional covariance metadata logical block.
     * <p> This method will return null if the block is not defined in the CDM. </p>
     * @return the additional covariance metadata logical block
     */
    public AdditionalCovarianceMetadata getAdditionalCovMetadataBlock() {
        return additionalCovMetadata;
    }

    /** Set the additional covariance metadata logical block.
     * @param covarianceMatrixBlock the additional covariance metadata logical block
     */
    public void setCovarianceMatrixBlock(final RTNCovariance covarianceMatrixBlock) {
        this.covarianceMatrixBlock = covarianceMatrixBlock;
    }

    /** Get the user defined logical block.
     * <p> This method will return null if the block is not defined in the CDM. </p>
     * @return the additional covariance metadata logical block
     */
    public UserDefined getUserDefinedBlock() {
        return userDefinedBlock;
    }

     /** Set the user defined logical block.
     * <p> This block is added at the end of the CDM parsing as common to both Object 1 and 2. </p>
     * @param userDefinedBlock the user defined block to set
     */
    public void setUserDefinedBlock(final UserDefined userDefinedBlock) {
        this.userDefinedBlock = userDefinedBlock;
    }
}

