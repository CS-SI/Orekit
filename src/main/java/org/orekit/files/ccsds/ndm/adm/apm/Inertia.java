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

package org.orekit.files.ccsds.ndm.adm.apm;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.CommonPhysicalProperties;

/** Inertia.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class Inertia extends CommonPhysicalProperties {

    /** Inertia reference frame. */
    private FrameFacade frame;

    /** Inertia matrix. */
    private RealMatrix inertiaMatrix;

    /** Simple constructor.
     */
    public Inertia() {
        inertiaMatrix = MatrixUtils.createRealMatrix(new double[][] {
            { Double.NaN, Double.NaN, Double.NaN },
            { Double.NaN, Double.NaN, Double.NaN },
            { Double.NaN, Double.NaN, Double.NaN }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        if (version >= 2.0) {
            checkNotNull(frame, InertiaKey.INERTIA_REF_FRAME.name());
        }
        checkNotNaN(inertiaMatrix.getEntry(0, 0), InertiaKey.IXX.name());
        checkNotNaN(inertiaMatrix.getEntry(1, 1), InertiaKey.IYY.name());
        checkNotNaN(inertiaMatrix.getEntry(2, 2), InertiaKey.IZZ.name());
        checkNotNaN(inertiaMatrix.getEntry(0, 1), InertiaKey.IXY.name());
        checkNotNaN(inertiaMatrix.getEntry(0, 2), InertiaKey.IXZ.name());
        checkNotNaN(inertiaMatrix.getEntry(1, 2), InertiaKey.IYZ.name());
    }

    /** Set frame in which inertia is specified.
     * @param frame frame in which inertia is specified
     */
    public void setFrame(final FrameFacade frame) {
        this.frame = frame;
    }

    /** Get frame in which inertia is specified.
     * @return frame in which inertia is specified
     */
    public FrameFacade getFrame() {
        return frame;
    }

    /** Get the inertia matrix.
     * @return the inertia matrix
     */
    public RealMatrix getInertiaMatrix() {
        return inertiaMatrix;
    }

    /** Set an entry in the inertia matrix.
     * <p>
     * Both I(j, k) and I(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 3 (inclusive)
     * @param k column index (must be between 0 and 3 (inclusive)
     * @param entry value of the matrix entry
     */
    public void setInertiaMatrixEntry(final int j, final int k, final double entry) {
        refuseFurtherComments();
        inertiaMatrix.setEntry(j, k, entry);
        inertiaMatrix.setEntry(k, j, entry);
    }

}
