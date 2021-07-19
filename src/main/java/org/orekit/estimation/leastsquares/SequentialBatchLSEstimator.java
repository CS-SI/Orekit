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
package org.orekit.estimation.leastsquares;

import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;

/**
 * Sequential least squares estimator for orbit determination.
 * <p>
 * This class extends {@link BatchLSEstimator}. It uses the result of a previous
 * optimization with its {@link Evaluation} and re-estimate the orbit with new measures
 * and the previous Evaluation.
 * </p>
 * <p>
 * When an orbit has already been estimated and measures are given, it is not efficient
 * to re-optimize the whole problem. Only considering the new measures while optimizing
 * will neither give good results as the old measures will not be taken into account.
 * Thus, a sequential estimator is used to estimate the orbit, which uses the old results
 * of the estimation (with the old evaluation) and the new measures.
 * </p>
 *
 * @author Julie Bayard
 * @since 11.0
 */
public class SequentialBatchLSEstimator extends BatchLSEstimator {

    /**
     * Simple constructor.
     * <p>
     * If multiple {@link PropagatorBuilder propagator builders} are set up, the
     * orbits of several spacecrafts will be used simultaneously. This is useful
     * if the propagators share some model or measurements parameters (typically
     * pole motion, prime meridian correction or ground stations positions).
     * </p>
     * <p>
     * Setting up multiple {@link PropagatorBuilder propagator builders} is also
     * useful when inter-satellite measurements are used, even if only one of
     * the orbit is estimated and the other ones are fixed. This is typically
     * used when very high accuracy GNSS measurements are needed and the
     * navigation bulletins are not considered accurate enough and the
     * navigation constellation must be propagated numerically.
     * </p>
     * <p>
     * The solver used for sequential least squares problem is a
     * {@link SequentialGaussNewtonOptimizer sequential Gauss Newton optimizer}.
     * </p>
     *
     * @param sequentialOptimizer solver for sequential least squares problem
     * @param propagatorBuilder builders to use for propagation.
     */
    public SequentialBatchLSEstimator(final SequentialGaussNewtonOptimizer sequentialOptimizer,
                                      final OrbitDeterminationPropagatorBuilder... propagatorBuilder) {
        super(sequentialOptimizer, propagatorBuilder);
    }

}
