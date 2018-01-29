/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.estimation.sequential;

import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.sequential.KalmanEstimator.FilterType;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/** Builder for a Kalman filter estimator.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.1
 */
public class KalmanEstimatorBuilder {

    /** Filter type. */
    private FilterType filterType;

    /** Builder for propagator. */
    private NumericalPropagatorBuilder propagatorBuilder;

    /** Estimated measurements parameters. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Initial covariance matrix. */
    private RealMatrix initialCovarianceMatrix;

    /** Process noise matrix.
     * Second moment of the process noise. Often named Q.
     */
    private RealMatrix processNoiseMatrix;

    /** Default constructor.
     *  Set an extended Kalman filter, with linearized covariance prediction.
     */
    public KalmanEstimatorBuilder() {
        this.filterType                      = FilterType.EXTENDED;
        this.propagatorBuilder               = null;
        this.estimatedMeasurementsParameters = null;
        this.initialCovarianceMatrix         = null;
        this.processNoiseMatrix              = null;
    }

    // Deprecated
    /** Construct a {@link KalmanEstimatorReal} from the data in this builder.
     * @return a new {@link KalmanEstimatorReal}.
     * @throws OrekitException
     */
    public KalmanEstimatorReal buildReal()
                    throws OrekitException {
        // FIXME: Check the sizes of matrix. Or do it inside the filter
        return new KalmanEstimatorReal(propagatorBuilder,
                                         estimatedMeasurementsParameters,
                                         initialCovarianceMatrix,
                                         processNoiseMatrix,
                                         filterType);
    }
    
    /** Construct a {@link KalmanEstimatorReal} from the data in this builder.
     * @return a new {@link KalmanEstimatorReal}.
     * @throws OrekitException
     */
    public KalmanEstimatorNormalized buildNormalized()
                    throws OrekitException {
        // FIXME: Check the sizes of matrix. Or do it inside the filter
        return new KalmanEstimatorNormalized(propagatorBuilder,
                                             estimatedMeasurementsParameters,
                                             initialCovarianceMatrix,
                                             processNoiseMatrix,
                                             filterType);
    }
    //Deprecated
    

    /** Construct a {@link KalmanEstimatorReal} from the data in this builder.
     * @return a new {@link KalmanEstimatorReal}.
     * @throws OrekitException
     */
    public KalmanEstimator build()
                    throws OrekitException {
        // FIXME: Check the sizes of matrix. Or do it inside the filter
        return new KalmanEstimator(propagatorBuilder,
                                   estimatedMeasurementsParameters,
                                   initialCovarianceMatrix,
                                   processNoiseMatrix,
                                   filterType);
    }

    /** Configure whether Kalman filter type will be EXTENDED or SIMPLE.
     * @param newFilterType Whether to build an EXTENDED or SIMPLE Kalman filter.
     * @return this object.
     */
    public KalmanEstimatorBuilder filterType(final FilterType newFilterType) {
        filterType = newFilterType;
        return this;
    }

    /** Configure the propagator builder.
     * @param propagatorBuilder The propagator builder to use in the Kalman filter.
     * @return this object.
     */
    public KalmanEstimatorBuilder builder(final NumericalPropagatorBuilder propBuilder) {
        propagatorBuilder = propBuilder;
        return this;
    }

    /** Configure the estimated measurement parameters.
     * @param estimatedMeasurementsParams The estimated measurements' parameters list.
     * @return this object.
     *
     */
    public KalmanEstimatorBuilder estimatedMeasurementsParameters(final ParameterDriversList estimatedMeasurementsParams) {
        estimatedMeasurementsParameters = estimatedMeasurementsParams;
        return this;
    }

    /** Configure the initial covariance matrix.
     * @param initialP The initial covariance matrix to use.
     * @return this object.
     */
    public KalmanEstimatorBuilder initialCovarianceMatrix(final RealMatrix initialP) {
        initialCovarianceMatrix = initialP;
        return this;
    }

    /** Configure the process noise matrix.
     * @param Q The process noise matrix to use.
     * @return this object.
     */
    public KalmanEstimatorBuilder processNoiseMatrix(final RealMatrix Q) {
        processNoiseMatrix = Q;
        return this;
    }
}
