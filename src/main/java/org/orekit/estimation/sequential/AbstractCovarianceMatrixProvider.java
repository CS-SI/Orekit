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
package org.orekit.estimation.sequential;

import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.SpacecraftState;

/** Abstract provider handling a predefined initial covariance matrix.
 * <p>
 * This class always provides a predefined initial noise matrix.
 * </p>
 * @author Luc Maisonobe
 * @since 9.2
 */
public abstract class AbstractCovarianceMatrixProvider implements CovarianceMatrixProvider {

    /** Initial process noise. */
    private final RealMatrix initialNoiseMatrix;

    /** Simple constructor.
     * @param initialNoiseMatrix initial process noise
     */
    protected AbstractCovarianceMatrixProvider(final RealMatrix initialNoiseMatrix) {
        this.initialNoiseMatrix = initialNoiseMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getInitialCovarianceMatrix(final SpacecraftState initial) {
        return initialNoiseMatrix;
    }

}
