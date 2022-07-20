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

import java.util.List;

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
    private final ODParameters ODparametersBlock;

    /** Euler angles block. */
    private final AdditionalParameters additionalParametersBlock;

    /** Spin-stabilized block. */
    private final StateVector stateVectorBlock;

    /** Spacecraft parameters block. */
    private final RTNCovariance covarianceMatrixBlock;

    /** XYZ covariance block. */
    private final XYZCovariance xyzCovarianceMatrixBlock;

    /** Sigma/Eigenvectors covariance block. */
    private final SigmaEigenvectorsCovariance sig3eigvec3CovarianceBlock;

    /** Type of alternate covariance, if present. */
    private AltCovarianceType altCovarianceType;





     /** Default constructor.
     * @param commentsBlock general comments block
     * @param ODparametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param xyzCovarianceBlock XYZ covariance matrix block
     * @param sig3eigvec3CovarianceBlock sigma/eigenvector covariance block
     * @param altCovarianceType type of alternate covariance
     */
    private CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODparametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final XYZCovariance xyzCovarianceBlock,
                   final SigmaEigenvectorsCovariance sig3eigvec3CovarianceBlock,
                   final AltCovarianceType altCovarianceType) {
        this.commentsBlock                  = commentsBlock;
        this.ODparametersBlock              = ODparametersBlock;
        this.additionalParametersBlock      = additionalParametersBlock;
        this.stateVectorBlock               = stateVectorBlock;
        this.covarianceMatrixBlock          = covarianceMatrixBlock;
        this.xyzCovarianceMatrixBlock       = xyzCovarianceBlock;
        this.sig3eigvec3CovarianceBlock     = sig3eigvec3CovarianceBlock;
        this.altCovarianceType              = altCovarianceType;
    }

     /**  Constructor with RTN covariance.
     * @param commentsBlock general comments block
     * @param ODparametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block

     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODparametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock) {
        this(commentsBlock, ODparametersBlock, additionalParametersBlock, stateVectorBlock, covarianceMatrixBlock, null, null, null);
    }

     /**  Constructor with RTN and XYZ covariance.
     * @param commentsBlock general comments block
     * @param ODparametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param xyzCovarianceBlock XYZ covariance matrix block
     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODparametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final XYZCovariance xyzCovarianceBlock) {
        this(commentsBlock, ODparametersBlock, additionalParametersBlock, stateVectorBlock, covarianceMatrixBlock, xyzCovarianceBlock, null, AltCovarianceType.XYZ);
    }

     /**  Constructor with RTN and sigma/eigenvector covariance.
     * @param commentsBlock general comments block
     * @param ODparametersBlock OD parameters block (may be null)
     * @param additionalParametersBlock additionnal parameters block (may be null)
     * @param stateVectorBlock state vector block
     * @param covarianceMatrixBlock covariance matrix in RTN coordinates frame block
     * @param sig3eigvec3CovarianceBlock sigma/eigenvector covariance block
     */
    public CdmData(final CommentsContainer commentsBlock,
                   final ODParameters ODparametersBlock,
                   final AdditionalParameters additionalParametersBlock,
                   final StateVector stateVectorBlock,
                   final RTNCovariance covarianceMatrixBlock,
                   final SigmaEigenvectorsCovariance sig3eigvec3CovarianceBlock) {
        this(commentsBlock, ODparametersBlock, additionalParametersBlock, stateVectorBlock, covarianceMatrixBlock, null, sig3eigvec3CovarianceBlock, AltCovarianceType.CSIG3EIGVEC3);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        if (ODparametersBlock != null) {
            ODparametersBlock.validate(version);
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
            sig3eigvec3CovarianceBlock.validate(version);
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
        return ODparametersBlock;
    }

    /** Get the additional parameters logical block.
     * @return additional parameters block (may be null)
     */
    public AdditionalParameters getAdditionalParametersBlock() {
        return additionalParametersBlock;
    }

    /** Get the state vector logical block.
     * @return state vector block
     */
    public StateVector getStateVectorBlock() {
        return stateVectorBlock;
    }

    /** Get the covariance matrix logical block.
     * <p> This block is mandatory. </p>
     * @return covariance matrix block
     */
    public RTNCovariance getRTNCovarianceBlock() {
        return covarianceMatrixBlock;
    }

    /** Get the Covariance Matrix in the XYZ Coordinate Frame (defined by value of {@link CdmMetadataKey#ALT_COV_REF_FRAME}).
     * <p> This block is not mandatory and on condition that {@link CdmMetadataKey#ALT_COV_TYPE} = {@link AltCovarianceType#XYZ}. </p>
     * @return XYZ covariance matrix block
     */
    public XYZCovariance getXYZCovarianceBlock() {
        return xyzCovarianceMatrixBlock;
    }

    /** Get the Sigma / Eigenvector covariance logical block.
     * <p> This block is not mandatory and on condition that {@link CdmMetadataKey#ALT_COV_TYPE} = {@link AltCovarianceType#CSIG3EIGVEC3}. </p>
     * @return the Sigma / Eigenvector covariance block
     */
    public SigmaEigenvectorsCovariance getSig3Eigvec3CovarianceBlock() {
        return sig3eigvec3CovarianceBlock;
    }
}
