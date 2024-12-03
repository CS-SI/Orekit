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

import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.KalmanFilterSmoother;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.utils.ParameterDriver;

/**
 * Implementation of a Kalman filter to perform orbit determination.
 * <p>
 * The filter uses a {@link PropagatorBuilder} to initialize its reference trajectory.
 * The Kalman estimator can be used with a {@link NumericalPropagator}, {@link TLEPropagator},
 * {@link BrouwerLyddanePropagator}, {@link EcksteinHechlerPropagator}, {@link KeplerianPropagator},
 * or {@link Ephemeris}.
 * </p>
 * <p>
 * Kalman estimation using a {@link DSSTPropagator semi-analytical orbit propagator} must be done using
 * the {@link SemiAnalyticalKalmanEstimator}.
 * </p>
 * <p>
 * The estimated parameters are driven by {@link ParameterDriver} objects. They are of 3 different types:<ol>
 *   <li><b>Orbital parameters</b>:The position and velocity of the spacecraft, or, more generally, its orbit.<br>
 *       These parameters are retrieved from the reference trajectory propagator builder when the filter is initialized.</li>
 *   <li><b>Propagation parameters</b>: Some parameters modelling physical processes (SRP or drag coefficients etc...).<br>
 *       They are also retrieved from the propagator builder during the initialization phase.</li>
 *   <li><b>Measurements parameters</b>: Parameters related to measurements (station biases, positions etc...).<br>
 *       They are passed down to the filter in its constructor.</li>
 * </ol>
 * <p>
 * The total number of estimated parameters is m, the size of the state vector.
 * </p>
 * <p>
 * The Kalman filter implementation used is provided by the underlying mathematical library Hipparchus.
 * All the variables seen by Hipparchus (states, covariances, measurement matrices...) are normalized
 * using a specific scale for each estimated parameters or standard deviation noise for each measurement components.
 * </p>
 *
 * <p>A {@link KalmanEstimator} object is built using the {@link KalmanEstimatorBuilder#build() build}
 * method of a {@link KalmanEstimatorBuilder}.</p>
 *
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 9.2
 */
public class KalmanSmoother extends AbstractSequentialEstimator {

    /** Underlying estimator. */
    private final AbstractSequentialEstimator estimator;

    /** Smoother. */
    private final KalmanFilterSmoother<MeasurementDecorator> smoother;

    /** Constructor.
     * @param estimator the underlying (forward) Kalman or unscented estimator
     */
    public KalmanSmoother(final AbstractSequentialEstimator estimator) {
        super(estimator.getMatrixDecomposer(), estimator.getBuilders());

        this.estimator = estimator;
        this.smoother = new KalmanFilterSmoother<>(estimator.getKalmanFilter(), getMatrixDecomposer());
    }

    @Override
    protected KalmanFilter<MeasurementDecorator> getKalmanFilter() {
        return smoother;
    }

    @Override
    protected SequentialModel getProcessModel() {
        return estimator.getProcessModel();
    }

    @Override
    protected KalmanEstimation getKalmanEstimation() {
        return estimator.getKalmanEstimation();
    }

}
