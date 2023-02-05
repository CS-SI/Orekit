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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.conversion.EphemerisPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/** Builder for a Kalman filter estimator.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanEstimatorBuilder {

    /** Decomposer to use for the correction phase. */
    private MatrixDecomposer decomposer;

    /** Builders for propagators. */
    private List<PropagatorBuilder> propagatorBuilders;

    /** Estimated measurements parameters. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Process noise matrices providers. */
    private List<CovarianceMatrixProvider> processNoiseMatricesProviders;

    /** Process noise matrix provider for measurement parameters. */
    private CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Default constructor.
     *  Set an extended Kalman filter, with linearized covariance prediction.
     */
    public KalmanEstimatorBuilder() {
        this.decomposer                      = new QRDecomposer(1.0e-15);
        this.propagatorBuilders              = new ArrayList<>();
        this.estimatedMeasurementsParameters = new ParameterDriversList();
        this.processNoiseMatricesProviders   = new ArrayList<>();
        this.measurementProcessNoiseMatrix   = null;
    }

    /** Construct a {@link KalmanEstimator} from the data in this builder.
     * <p>
     * Before this method is called, {@link #addPropagationConfiguration(PropagatorBuilder,
     * CovarianceMatrixProvider) addPropagationConfiguration()} must have been called
     * at least once, otherwise configuration is incomplete and an exception will be raised.
     * </p>
     * @return a new {@link KalmanEstimator}.
     */
    public KalmanEstimator build() {
        final int n = propagatorBuilders.size();
        if (n == 0) {
            throw new OrekitException(OrekitMessages.NO_PROPAGATOR_CONFIGURED);
        }
        return new KalmanEstimator(decomposer, propagatorBuilders, processNoiseMatricesProviders,
                                   estimatedMeasurementsParameters, measurementProcessNoiseMatrix);
    }

    /** Configure the matrix decomposer.
     * @param matrixDecomposer decomposer to use for the correction phase
     * @return this object.
     */
    public KalmanEstimatorBuilder decomposer(final MatrixDecomposer matrixDecomposer) {
        decomposer = matrixDecomposer;
        return this;
    }

    /** Add a propagation configuration.
     * <p>
     * This method must be called once for each propagator to managed with the
     * {@link KalmanEstimator Kalman estimator}. The propagators order in the
     * Kalman filter will be the call order.
     * </p>
     * <p>
     * The {@code provider} should return a matrix with dimensions and ordering
     * consistent with the {@code builder} configuration. The first 6 rows/columns
     * correspond to the 6 orbital parameters. The remaining elements correspond
     * to the subset of propagation parameters that are estimated, in the
     * same order as propagatorBuilder.{@link
     * org.orekit.propagation.conversion.PropagatorBuilder#getPropagationParametersDrivers()
     * getPropagationParametersDrivers()}.{@link org.orekit.utils.ParameterDriversList#getDrivers()
     * getDrivers()} (but filtering out the non selected drivers).
     * </p>
     * @param builder  The propagator builder to use in the Kalman filter.
     * @param provider The process noise matrices provider to use, consistent with the builder.
     *                 This parameter can be equal to {@code null} if the input builder is
     *                 an {@link EphemerisPropagatorBuilder}. Indeed, for ephemeris based estimation
     *                 only measurement parameters are estimated. Therefore, the covariance related
     *                 to dynamical parameters can be null.
     * @return this object.
     * @see CovarianceMatrixProvider#getProcessNoiseMatrix(org.orekit.propagation.SpacecraftState,
     * org.orekit.propagation.SpacecraftState) getProcessNoiseMatrix(previous, current)
     */
    public KalmanEstimatorBuilder addPropagationConfiguration(final PropagatorBuilder builder,
                                                              final CovarianceMatrixProvider provider) {
        propagatorBuilders.add(builder);
        processNoiseMatricesProviders.add(provider);
        return this;
    }

    /** Configure the estimated measurement parameters.
     * <p>
     * If this method is not called, no measurement parameters will be estimated.
     * </p>
     * @param estimatedMeasurementsParams The estimated measurements' parameters list.
     * @param provider covariance matrix provider for the estimated measurement parameters
     * @return this object.
     * @since 10.3
     */
    public KalmanEstimatorBuilder estimatedMeasurementsParameters(final ParameterDriversList estimatedMeasurementsParams,
                                                                  final CovarianceMatrixProvider provider) {
        estimatedMeasurementsParameters = estimatedMeasurementsParams;
        measurementProcessNoiseMatrix   = provider;
        return this;
    }

}
