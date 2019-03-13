/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.gnss;

import java.util.List;

import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitInternalError;
import org.orekit.utils.ParameterDriver;

/** Decorrelation/reduction engine for {@link ModifiedLambdaSolver modified LAMBDA method}.
 * <p>
 * This class implements the modified LAMBDA method, as described in the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * @author Luc Maisonobe
 * @since 10.0
 */
class ModifiedLambdaReducer extends BaseLambdaReducer {

    /** Simple constructor.
     * @param drivers parameters drivers for ambiguities
     * @param indirection indirection array to extract ambiguity parameters
     * @param covariance full covariance matrix
     */
    ModifiedLambdaReducer(final List<ParameterDriver> drivers, final int[] indirection, final RealMatrix covariance) {
        super(drivers, indirection, covariance);
    }

    /** {@inheritDoc} */
    @Override
    protected void doDecomposition(final double[] diag, final double[] low) {
        // TODO
        throw new OrekitInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    protected void doReduction(final double[] diag, final double[] low) {
        // TODO
        throw new OrekitInternalError(null);
    }

}
