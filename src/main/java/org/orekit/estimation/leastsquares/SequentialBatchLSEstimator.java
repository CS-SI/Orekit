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
package org.orekit.estimation.leastsquares;

import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer;
import org.orekit.propagation.conversion.PropagatorBuilder;

/**
 * Sequential least squares estimator for orbit determination.
 * <p>
 * When an orbit has already been estimated and new measurements are given, it is not efficient
 * to re-optimize the whole problem. Only considering the new measures while optimizing
 * will neither give good results as the old measurements will not be taken into account.
 * Thus, a sequential estimator is used to estimate the orbit, which uses the old results
 * of the estimation and the new measurements.
 * <p>
 * In order to perform a sequential optimization, the user must configure a
 * {@link org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer SequentialGaussNewtonOptimizer}.
 * Depending if its input data are an empty {@link Evaluation}, a complete <code>Evaluation</code>
 * or an a priori state and covariance, different configuration are possible.
 * <p>
 * <b>1. No input data from a previous estimation</b>
 * <p>
 * Then, the {@link SequentialBatchLSEstimator} can be used like a {@link BatchLSEstimator}
 * to perform the estimation. The user can initialize the <code>SequentialGaussNewtonOptimizer</code>
 * using the default constructor.
 * <p>
 * <code>final SequentialGaussNewtonOptimizer optimizer = new SequentialGaussNewtonOptimizer();</code>
 * <p>
 * By default, a {@link QRDecomposer} is used as decomposition algorithm. In addition, normal
 * equations are not form. It is possible to update these two default configurations by using:
 * <ul>
 *   <li>{@link org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer#withDecomposer(MatrixDecomposer) withDecomposer} method:
 *       <code>optimizer.withDecomposer(newDecomposer);</code>
 *   </li>
 *   <li>{@link org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer#withFormNormalEquations(boolean) withFormNormalEquations} method:
 *       <code>optimizer.withFormNormalEquations(newFormNormalEquations);</code>
 *   </li>
 * </ul>
 * <p>
 * <b>2. Initialization using a previous <code>Evalutation</code></b>
 * <p>
 * In this situation, it is recommended to use the second constructor of the optimizer class.
 * <p>
 * <code>final SequentialGaussNewtonOptimizer optimizer = new SequentialGaussNewtonOptimizer(decomposer,
 *                                                                                           formNormalEquations,
 *                                                                                           evaluation);
 * </code>
 * <p>
 * Using this constructor, the user can directly configure the MatrixDecomposer and set the flag for normal equations
 * without calling the two previous presented methods.
 * <p>
 * <i>Note:</i> This constructor can also be used to perform the initialization of <b>1.</b>
 * In this case, the <code>Evaluation evaluation</code> is <code>null</code>.
 * <p>
 * <b>3. Initialization using an a priori estimated state and covariance</b>
 * <p>
 * These situation is a classical satellite operation need. Indeed, a classical action is to use
 * the results of a previous orbit determination (estimated state and covariance) performed a day before,
 * to improve the initialization and the results of an orbit determination performed the current day.
 * In this situation, the user can initialize the <code>SequentialGaussNewtonOptimizer</code>
 * using the default constructor.
 * <p>
 * <code>final SequentialGaussNewtonOptimizer optimizer = new SequentialGaussNewtonOptimizer();</code>
 * <p>
 * The MatrixDecomposer and the flag about normal equations can again be updated using the two previous
 * presented methods. The a priori state and covariance matrix can be set using:
 * <ul>
 *   <li>{@link org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer#withAPrioriData(org.hipparchus.linear.RealVector, org.hipparchus.linear.RealMatrix) withAPrioriData} method:
 *       <code>optimizer.withAPrioriData(aPrioriState, aPrioriCovariance);</code>
 *   </li>
 * </ul>
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
     * {@link org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer
     * sequential Gauss Newton optimizer}.
     * Details about how initialize it are given in the class JavaDoc.
     * </p>
     *
     * @param sequentialOptimizer solver for sequential least squares problem
     * @param propagatorBuilder builders to use for propagation.
     */
    public SequentialBatchLSEstimator(final SequentialGaussNewtonOptimizer sequentialOptimizer,
                                      final PropagatorBuilder... propagatorBuilder) {
        super(sequentialOptimizer, propagatorBuilder);
    }

}
