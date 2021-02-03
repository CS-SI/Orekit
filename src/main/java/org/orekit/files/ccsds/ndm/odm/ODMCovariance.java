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

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;

/** Container for covariance matrix}.
 * @author sports
 * @since 6.1
 */
public class ODMCovariance extends CommentsContainer implements Data {

    /** Labels for matrix row/columns. */
    private static final String[] LABELS = {
        "X", "Y", "Z", "X_DOT", "Y_DOT", "Z_DOT"
    };

    /** Coordinate system for covariance matrix, for Local Orbital Frames. */
    private LOFType covRefLofType;

    /** Coordinate system for covariance matrix, for absolute frames.
     * If not given it is set equal to refFrame. */
    private Frame covRefFrame;

    /** Position/Velocity covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Create an empty data set.
     * @param refFrame reference frame from metadata
     */
    public ODMCovariance(final Frame refFrame) {
        covRefFrame      = refFrame;
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
        for (int i = 0; i < covarianceMatrix.getRowDimension(); ++i) {
            for (int j = 0; j <= i; ++j) {
                if (Double.isNaN(covarianceMatrix.getEntry(i, j))) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                              "C" + LABELS[i] + "_" + LABELS[j]);
                }
            }
        }
    }

    /** Get coordinate system for covariance matrix, for Local Orbital Frames.
     * <p>
     * The value returned is null if the covariance matrix is given in an
     * absolute frame rather than a Local Orbital Frame. In this case, the
     * method {@link #getCovRefFrame()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix, or null if the
     * covariance matrix is given in an absolute frame rather than a Local
     * Orbital Frame
     */
    public LOFType getCovRefLofType() {
        return covRefLofType;
    }

    /** Set coordinate system for covariance matrix, for Local Orbital Frames.
     * @param covRefLofType the coordinate system to be set
     */
    void setCovRefLofType(final LOFType covRefLofType) {
        refuseFurtherComments();
        this.covRefLofType = covRefLofType;
        this.covRefFrame   = null;
    }

    /** Get coordinate system for covariance matrix, for absolute frames.
     * <p>
     * The value returned is null if the covariance matrix is given in a
     * Local Orbital Frame rather than an absolute frame. In this case, the
     * method {@link #getCovRefLofType()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix
     */
    public Frame getCovRefFrame() {
        return covRefFrame;
    }

    /** Set coordinate system for covariance matrix.
     * @param covRefFrame the coordinate system to be set
     */
    void setCovRefFrame(final Frame covRefFrame) {
        refuseFurtherComments();
        this.covRefLofType = null;
        this.covRefFrame   = covRefFrame;
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
    void setCovarianceMatrixEntry(final int j, final int k, final double entry) {
        refuseFurtherComments();
        covarianceMatrix.setEntry(j, k, entry);
        covarianceMatrix.setEntry(k, j, entry);
    }

}
