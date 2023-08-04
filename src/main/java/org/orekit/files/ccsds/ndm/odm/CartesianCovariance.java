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

package org.orekit.files.ccsds.ndm.odm;

import java.util.function.Supplier;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.time.AbsoluteDate;

/** Container for OPM/OMM/OCM Cartesian covariance matrix.
 * @author sports
 * @since 6.1
 */
public class CartesianCovariance extends CommentsContainer implements Data {

    /** Labels for matrix row/columns. */
    private static final String[] LABELS = {
        "X", "Y", "Z", "X_DOT", "Y_DOT", "Z_DOT"
    };

    /** Supplier for default reference frame. */
    private final Supplier<FrameFacade> defaultFrameSupplier;

    /** Matrix epoch. */
    private AbsoluteDate epoch;

    /** Reference frame in which data are given. */
    private FrameFacade referenceFrame;

    /** Position/Velocity covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Create an empty data set.
     * @param defaultFrameSupplier supplier for default reference frame
     * if no frame is specified in the CCSDS message
     */
    public CartesianCovariance(final Supplier<FrameFacade> defaultFrameSupplier) {
        this.defaultFrameSupplier = defaultFrameSupplier;
        covarianceMatrix = MatrixUtils.createRealMatrix(6, 6);
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
        checkNotNull(epoch, CartesianCovarianceKey.EPOCH.name());
        for (int i = 0; i < covarianceMatrix.getRowDimension(); ++i) {
            for (int j = 0; j <= i; ++j) {
                if (Double.isNaN(covarianceMatrix.getEntry(i, j))) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                              "C" + LABELS[i] + "_" + LABELS[j]);
                }
            }
        }
    }

    /** Get matrix epoch.
     * @return matrix epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set matrix epoch.
     * @param epoch matrix epoch
     */
    public void setEpoch(final AbsoluteDate epoch) {
        refuseFurtherComments();
        this.epoch = epoch;
    }

    /**
     * Get the reference frame.
     *
     * @return The reference frame specified by the {@code COV_REF_FRAME} keyword
     * or inherited from metadata
     */
    public FrameFacade getReferenceFrame() {
        return referenceFrame == null ? defaultFrameSupplier.get() : referenceFrame;
    }

    /** Set the reference frame in which data are given.
     * @param referenceFrame the reference frame to be set
     */
    public void setReferenceFrame(final FrameFacade referenceFrame) {
        refuseFurtherComments();
        this.referenceFrame = referenceFrame;
    }

    /** Get the Position/Velocity covariance matrix.
     * @return the Position/Velocity covariance matrix
     */
    public RealMatrix getCovarianceMatrix() {
        return covarianceMatrix;
    }

    /** Set an entry in the Position/Velocity covariance matrix.
     * <p>
     * Both m(j, k) and m(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 5 (inclusive)
     * @param k column index (must be between 0 and 5 (inclusive)
     * @param entry value of the matrix entry
     */
    public void setCovarianceMatrixEntry(final int j, final int k, final double entry) {
        refuseFurtherComments();
        covarianceMatrix.setEntry(j, k, entry);
        covarianceMatrix.setEntry(k, j, entry);
    }

}
