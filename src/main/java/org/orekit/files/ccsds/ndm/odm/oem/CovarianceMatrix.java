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

package org.orekit.files.ccsds.ndm.odm.oem;

import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;

/** The CovarianceMatrix class represents a covariance matrix and its metadata: epoch and frame.
 * @author sports
 */
public class CovarianceMatrix {

    /** Covariance matrix. */
    private RealMatrix matrix;

    /** Epoch relative to the covariance matrix. */
    private AbsoluteDate epoch;

    /** Coordinate system for covariance matrix, for Local Orbital Frames. */
    private LOFType lofType;

    /** Coordinate system for covariance matrix, for absolute frames.
     * If not given it is set equal to refFrame. */
    private Frame frame;

    /** Covariance Matrix constructor.
     * @param epoch the epoch
     * @param lofType coordinate system for covariance matrix, for Local Orbital Frames
     * @param frame coordinate system for covariance matrix, for absolute frames
     * @param lastMatrix the covariance matrix
     */
    public CovarianceMatrix(final AbsoluteDate epoch,
                            final LOFType lofType, final Frame frame,
                            final RealMatrix lastMatrix) {
        this.matrix  = lastMatrix;
        this.epoch   = epoch;
        this.lofType = lofType;
        this.frame   = frame;
    }

    /** Get the covariance matrix.
     * @return the covariance matrix
     */
    public RealMatrix getMatrix() {
        return matrix;
    }

    /** Get the epoch relative to the covariance matrix.
     * @return the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Get coordinate system for covariance matrix, for Local Orbital Frames.
     * <p>
     * The value returned is null if the covariance matrix is given in an
     * absolute frame rather than a Local Orbital Frame. In this case, the
     * method {@link #getFrame()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix, or null if the
     * covariance matrix is given in an absolute frame rather than a Local
     * Orbital Frame
     */
    public LOFType getLofType() {
        return lofType;
    }

    /** Get coordinate system for covariance matrix, for absolute frames.
     * <p>
     * The value returned is null if the covariance matrix is given in a
     * Local Orbital Frame rather than an absolute frame. In this case, the
     * method {@link #getLofType()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix
     */
    public Frame getFrame() {
        return frame;
    }

}
