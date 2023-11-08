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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/** Builder for an Unscented Kalman filter estimator.
 * <p>
 * The builder is generalized to accept any {@link PropagatorBuilder}.
 * Howerver, it is absolutely not recommended to use a {@link DSSTPropagatorBuilder}.
 * A specific {@link SemiAnalyticalUnscentedKalmanEstimatorBuilder semi-analytical
 * unscented Kalman Filter} is implemented and shall be used.
 * </p>
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class UnscentedKalmanEstimatorBuilder {

    /** Decomposer to use for the correction phase. */
    private MatrixDecomposer decomposer;

    /** Builders for propagators. */
    private List<PropagatorBuilder> propagatorBuilders;

    /** Estimated measurements parameters. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Process noise matrix providers. */
    private List<CovarianceMatrixProvider> processNoiseMatrixProviders;

    /** Process noise matrix provider for measurement parameters. */
    private CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Unscend transform provider. */
    private UnscentedTransformProvider utProvider;

    /** Default constructor.
     *  Set an Unscented Kalman filter.
     */
    public UnscentedKalmanEstimatorBuilder() {
        this.decomposer                      = new QRDecomposer(1.0e-15);
        this.propagatorBuilders              = new ArrayList<>();
        this.estimatedMeasurementsParameters = new ParameterDriversList();
        this.processNoiseMatrixProviders     = new ArrayList<>();
        this.measurementProcessNoiseMatrix   = null;
        this.utProvider                      = null;
    }

    /** Construct a {@link UnscentedKalmanEstimator} from the data in this builder.
     * <p>
     * Before this method is called, {@link #addPropagationConfiguration(PropagatorBuilder,
     * CovarianceMatrixProvider) addPropagationConfiguration()} must have been called
     * at least once, otherwise configuration is incomplete and an exception will be raised.
     * <p>
     * In addition, the {@link #unscentedTransformProvider(UnscentedTransformProvider)
     * unscentedTransformProvider()} must be called to configure the unscented transform
     * provider use during the estimation process, otherwise configuration is
     * incomplete and an exception will be raised.
     * </p>
     * @return a new {@link UnscentedKalmanEstimator}.
     */
    public UnscentedKalmanEstimator build() {
        if (propagatorBuilders.size() == 0) {
            throw new OrekitException(OrekitMessages.NO_PROPAGATOR_CONFIGURED);
        }
        if (utProvider == null) {
            throw new OrekitException(OrekitMessages.NO_UNSCENTED_TRANSFORM_CONFIGURED);
        }
        return new UnscentedKalmanEstimator(decomposer, propagatorBuilders, processNoiseMatrixProviders,
                                            estimatedMeasurementsParameters, measurementProcessNoiseMatrix,
                                            utProvider);

    }

    /** Configure the matrix decomposer.
     * @param matrixDecomposer decomposer to use for the correction phase
     * @return this object.
     */
    public UnscentedKalmanEstimatorBuilder decomposer(final MatrixDecomposer matrixDecomposer) {
        decomposer = matrixDecomposer;
        return this;
    }

    /** Configure the unscented transform provider.
     * @param transformProvider unscented transform to use for the prediction phase
     * @return this object.
     */
    public UnscentedKalmanEstimatorBuilder unscentedTransformProvider(final UnscentedTransformProvider transformProvider) {
        this.utProvider = transformProvider;
        return this;
    }

    /** Add a propagation configuration.
     * <p>
     * This method must be called once for each propagator to managed with the
     * {@link UnscentedKalmanEstimator unscented kalman estimatior}. The
     * propagators order in the Kalman filter will be the call order.
     * </p>
     * <p>
     * The {@code provider} should return a matrix with dimensions and ordering
     * consistent with the {@code builder} configuration. The first 6 rows/columns
     * correspond to the 6 orbital parameters which must all be present, regardless
     * of the fact they are estimated or not. The remaining elements correspond
     * to the subset of propagation parameters that are estimated, in the
     * same order as propagatorBuilder.{@link
     * org.orekit.propagation.conversion.PropagatorBuilder#getPropagationParametersDrivers()
     * getPropagationParametersDrivers()}.{@link org.orekit.utils.ParameterDriversList#getDrivers()
     * getDrivers()} (but filtering out the non selected drivers).
     * </p>
     * @param builder The propagator builder to use in the Kalman filter.
     * @param provider The process noise matrices provider to use, consistent with the builder.
     * @see CovarianceMatrixProvider#getProcessNoiseMatrix(org.orekit.propagation.SpacecraftState,
     * org.orekit.propagation.SpacecraftState) getProcessNoiseMatrix(previous, current)
     * @return this object.
     */
    public UnscentedKalmanEstimatorBuilder addPropagationConfiguration(final PropagatorBuilder builder,
                                                                       final CovarianceMatrixProvider provider) {
        propagatorBuilders.add(builder);
        processNoiseMatrixProviders.add(provider);
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
    public UnscentedKalmanEstimatorBuilder estimatedMeasurementsParameters(final ParameterDriversList estimatedMeasurementsParams,
                                                                           final CovarianceMatrixProvider provider) {
        estimatedMeasurementsParameters = estimatedMeasurementsParams;
        measurementProcessNoiseMatrix   = provider;
        return this;
    }

}
