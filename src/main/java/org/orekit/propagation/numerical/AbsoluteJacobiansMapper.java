/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.numerical;

import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Vincent Mouraux
 * @see org.orekit.propagation.numerical.PartialDerivativesEquations
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 * @since 10.2
 */
public class AbsoluteJacobiansMapper extends JacobiansMapper {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     */
    public AbsoluteJacobiansMapper(final String name, final ParameterDriversList parameters) {
        // orbit type and angle type are not used here
        super(name, parameters, null, null);
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] identity = new double[STATE_DIMENSION][STATE_DIMENSION];

        for (int i = 0; i < STATE_DIMENSION; ++i) {
            identity[i][i] = 1.0;
        }

        return identity;

    }

}
