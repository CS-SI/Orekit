/* Copyright 2002-2021 CS GROUP
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
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.files.ccsds.utils.CcsdsFrame;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Container for covariance matrix.
 * @author sports
 * @since 6.1
 */
public class Covariance extends CommentsContainer implements Data {

    /** Labels for matrix row/columns. */
    private static final String[] LABELS = {
        "X", "Y", "Z", "X_DOT", "Y_DOT", "Z_DOT"
    };

    /** Supplier for default reference frame. */
    private final Supplier<Frame> defaultFrameSupplier;

    /** Supplier for default CCSDS reference frame. */
    private final Supplier<CcsdsFrame> defaultCCSDSFrameSupplier;

    /** Matrix epoch. */
    private AbsoluteDate epoch;

    /** Reference frame in which data are given (may be null if referenceCCSDSFrame is a LOF frame). */
    private Frame referenceFrame;

    /** Reference frame in which data are given. */
    private CcsdsFrame referenceCCSDSFrame;

    /** Position/Velocity covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Create an empty data set.
     * @param defaultFrameSupplier supplier for default reference frame
    * if no frame is specified in the CCSDS message
     * @param defaultCCSDSFrameSupplier supplier for default CCSDS reference frame
     * if no frame is specified in the CCSDS message
     */
    public Covariance(final Supplier<Frame> defaultFrameSupplier,
                         final Supplier<CcsdsFrame> defaultCCSDSFrameSupplier) {
        this.defaultFrameSupplier      = defaultFrameSupplier;
        this.defaultCCSDSFrameSupplier = defaultCCSDSFrameSupplier;
        covarianceMatrix = MatrixUtils.createRealMatrix(6, 6);
        for (int i = 0; i < covarianceMatrix.getRowDimension(); ++i) {
            for (int j = 0; j <= i; ++j) {
                covarianceMatrix.setEntry(i, j, Double.NaN);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(epoch, CovarianceKey.EPOCH);
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
     * Get the reference frame as an Orekit {@link Frame}.
     *
     * @return The reference frame specified by the {@code COV_REF_FRAME} keyword
     * or inherited from metadate
     */
    public Frame getRefFrame() {
        return useDefaultSuppliers() ? defaultFrameSupplier.get() : referenceFrame;
    }

    /**
     * Get the reference frame as a {@link CcsdsFrame}.
     *
     * @return The reference frame specified by the {@code COV_REF_FRAME} keyword
     * or inherited from metadate
     */
    public CcsdsFrame getRefCCSDSFrame() {
        return useDefaultSuppliers() ? defaultCCSDSFrameSupplier.get() : referenceCCSDSFrame;
    }

    /** Check if we should rely on default suppliers for frames.
     * @return true if we should rely on default frame suppliers
     */
    private boolean useDefaultSuppliers() {
        // we check referenceCCSDSFrame even when we want referenceFrame
        // because referenceFrame may be null when referenceCCSDSFrame has been
        // initialized with a LOF-type frame
        return referenceCCSDSFrame == null;
    }

    /** Set the reference frame in which data are given.
     * @param frame the reference frame to be set
     * @param ccsdsFrame the reference frame to be set
     */
    public void setRefFrame(final Frame frame, final CcsdsFrame ccsdsFrame) {
        refuseFurtherComments();
        this.referenceFrame      = frame;
        this.referenceCCSDSFrame = ccsdsFrame;
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
