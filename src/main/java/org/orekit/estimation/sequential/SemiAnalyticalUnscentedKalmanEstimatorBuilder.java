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

import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/** Builder for an Unscented Semi-analytical Kalman filter estimator.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class SemiAnalyticalUnscentedKalmanEstimatorBuilder {

    /** Decomposer to use for the correction phase. */
    private MatrixDecomposer decomposer;

    /** Builders for propagator. */
    private DSSTPropagatorBuilder propagatorBuilder;

    /** Estimated measurements parameters. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Process noise matrix provider. */
    private CovarianceMatrixProvider processNoiseMatrixProvider;

    /** Process noise matrix provider for measurement parameters. */
    private CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Unscend transform provider. */
    private UnscentedTransformProvider utProvider;

    /** Default constructor.
     *  Set an Unscented Semi-analytical Kalman filter.
     */
    public SemiAnalyticalUnscentedKalmanEstimatorBuilder() {
        this.decomposer                      = new QRDecomposer(1.0e-15);
        this.propagatorBuilder               = null;
        this.estimatedMeasurementsParameters = new ParameterDriversList();
        this.processNoiseMatrixProvider      = null;
        this.measurementProcessNoiseMatrix   = null;
        this.utProvider                      = null;
    }

    /** Construct a {@link SemiAnalyticalUnscentedKalmanEstimator} from the data in this builder.
     * <p>
     * Before this method is called, {@link #addPropagationConfiguration(DSSTPropagatorBuilder,
     * CovarianceMatrixProvider) addPropagationConfiguration()} must have been called
     * at least once, otherwise configuration is incomplete and an exception will be raised.
     * <p>
     * In addition, the {@link #unscentedTransformProvider(UnscentedTransformProvider)
     * unscentedTransformProvider()} must be called to configure the unscented transform
     * provider use during the estimation process, otherwise configuration is
     * incomplete and an exception will be raised.
     * </p>
     * @return a new {@link SemiAnalyticalUnscentedKalmanEstimator}.
     */
    public SemiAnalyticalUnscentedKalmanEstimator build() {
        if (propagatorBuilder == null) {
            throw new OrekitException(OrekitMessages.NO_PROPAGATOR_CONFIGURED);
        }
        if (utProvider == null) {
            throw new OrekitException(OrekitMessages.NO_UNSCENTED_TRANSFORM_CONFIGURED);
        }
        return new SemiAnalyticalUnscentedKalmanEstimator(decomposer, propagatorBuilder, processNoiseMatrixProvider,
                                                          estimatedMeasurementsParameters, measurementProcessNoiseMatrix,
                                                          utProvider);
    }

    /** Configure the matrix decomposer.
     * @param matrixDecomposer decomposer to use for the correction phase
     * @return this object.
     */
    public SemiAnalyticalUnscentedKalmanEstimatorBuilder decomposer(final MatrixDecomposer matrixDecomposer) {
        decomposer = matrixDecomposer;
        return this;
    }

    /** Configure the unscented transform provider.
     * @param transformProvider unscented transform to use for the prediction phase
     * @return this object.
     */
    public SemiAnalyticalUnscentedKalmanEstimatorBuilder unscentedTransformProvider(final UnscentedTransformProvider transformProvider) {
        this.utProvider = transformProvider;
        return this;
    }

    /** Add a propagation configuration.
     * <p>
     * This method must be called once initialize the propagator builder
     * used by the Semi-Analytical Unscented Kalman Filter.
     * </p>
     * @param builder The propagator builder to use in the Kalman filter.
     * @param provider The process noise matrices provider to use, consistent with the builder.
     * @return this object.
     */
    public SemiAnalyticalUnscentedKalmanEstimatorBuilder addPropagationConfiguration(final DSSTPropagatorBuilder builder,
                                                                                     final CovarianceMatrixProvider provider) {
        propagatorBuilder          = builder;
        processNoiseMatrixProvider = provider;
        return this;
    }

    /** Configure the estimated measurement parameters.
     * <p>
     * If this method is not called, no measurement parameters will be estimated.
     * </p>
     * @param estimatedMeasurementsParams The estimated measurements' parameters list.
     * @param provider covariance matrix provider for the estimated measurement parameters
     * @return this object.
     */
    public SemiAnalyticalUnscentedKalmanEstimatorBuilder estimatedMeasurementsParameters(final ParameterDriversList estimatedMeasurementsParams,
                                                                                         final CovarianceMatrixProvider provider) {
        estimatedMeasurementsParameters = estimatedMeasurementsParams;
        measurementProcessNoiseMatrix   = provider;
        return this;
    }

}
