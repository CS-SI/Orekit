/* Copyright 2002-2024 CS GROUP
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

/** Provider for constant process noise matrices.
 * <p>
 * This class always provides one initial noise matrix and
 * one constant process noise matrix (both can be identical),
 * regardless of states.
 * </p>
 * @author Luc Maisonobe
 * @since 9.2
 */
public class ConstantProcessNoise extends AbstractCovarianceMatrixProvider {

    /** Constant process noise. */
    private final RealMatrix processNoiseMatrix;

    /** Simple constructor.
     * @param processNoiseMatrix constant process noise, used for both initial noise
     * and noise between previous and current state
     */
    public ConstantProcessNoise(final RealMatrix processNoiseMatrix) {
        this(processNoiseMatrix, processNoiseMatrix);
    }

    /** Simple constructor.
     * @param initialNoiseMatrix initial process noise
     * @param processNoiseMatrix constant process noise
     */
    public ConstantProcessNoise(final RealMatrix initialNoiseMatrix, final RealMatrix processNoiseMatrix) {
        super(initialNoiseMatrix);
        this.processNoiseMatrix = processNoiseMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getProcessNoiseMatrix(final SpacecraftState previous,
                                            final SpacecraftState current) {
        return processNoiseMatrix;
    }

}
