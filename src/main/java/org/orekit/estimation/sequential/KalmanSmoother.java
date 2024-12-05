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
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.orekit.time.AbsoluteDate;

import java.util.List;
import java.util.stream.Collectors;

/** Implementation of a Kalman smoother to perform orbit determination.
 *
 * @author Mark Rutten
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

    public List<PhysicalEstimatedState> backwardsSmooth() {

        // Backwards smoothing step
        final List<ProcessEstimate> normalisedStates = smoother.backwardsSmooth();

        // Reference date
        final AbsoluteDate referenceDate = estimator.getReferenceDate();

        // Covariance scaling factors
        final double[] covarianceScale = getProcessModel().getScale();

        // Convert to physical states
        return normalisedStates.stream()
                .map(state -> new PhysicalEstimatedState(
                        referenceDate.shiftedBy(state.getTime()),
                        state.getState(),
                        KalmanEstimatorUtil.unnormalizeCovarianceMatrix(state.getCovariance(), covarianceScale)
                ))
                .collect(Collectors.toList());
    }
}
