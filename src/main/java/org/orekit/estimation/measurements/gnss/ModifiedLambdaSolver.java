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
import org.orekit.utils.ParameterDriver;

/** Modified LAMBDA method for integer ambiguity solving.
 * <p>
 * This class implements PJG Teunissen Least Square Ambiguity Decorrelation
 * Adjustment (LAMBDA) method, as described in the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * @author Luc Maisonobe
 * @since 10.0
 */
public class ModifiedLambdaSolver extends AmbiguitySolver {

    /** Simple constructor.
     * @param ambiguityDrivers drivers for ambiguity parameters
     */
    public ModifiedLambdaSolver(final List<ParameterDriver> ambiguityDrivers) {
        super(ambiguityDrivers);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> fixIntegerAmbiguities(final int startIndex,
                                                       final List<ParameterDriver> measurementsParametersDrivers,
                                                       final RealMatrix covariance) {

        // set up decorrelation engine for ambiguity covariances
        final List<ParameterDriver> ambiguities      = getAllAmbiguityDrivers();
        final double[]              floatAmbiguities = ambiguities.stream().mapToDouble(d -> d.getValue()).toArray();
        final int[]                 indirection      = getFreeAmbiguityIndirection(startIndex, measurementsParametersDrivers);
        final LambdaReducer         reducer          = new LambdaReducer(floatAmbiguities, indirection, covariance);

        // perform initial Lᵀ.D.L decomposition
        reducer.ltdlDecomposition();

        // perform decorrelation/reduction of covariances
        reducer.reduction();

        // perform discrete search of Integer Least Square problem

        // TODO
        return null;

    }

}
