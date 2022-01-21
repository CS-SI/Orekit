/* Copyright 2002-2022 CS GROUP
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

import java.util.List;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.ExtendedKalmanFilter;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** {@link org.orekit.propagation.sampling.OrekitStepHandler Step handler} picking up
 * {@link ObservedMeasurement measurements} for the {@link SemiAnalyticalKalmanEstimator}.
 * @author Julie Bayard
 * @author Bryan Cazabonne
 * @author Maxime Journot
 * @since 11.1
 */
public class EskfMeasurementHandler implements OrekitStepHandler {

    /** Least squares model. */
    private final SemiAnalyticalKalmanModel model;

    /** Extended Kalman Filter. */
    private final ExtendedKalmanFilter<MeasurementDecorator> filter;

    /** Underlying measurements. */
    private final List<ObservedMeasurement<?>> observedMeasurements;

    /** Index of the next measurement component in the model. */
    private int index;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Simple constructor.
     * @param model semi-analytical kalman model
     * @param filter kalman filter instance
     * @param observedMeasurements list of observed measurements
     * @param referenceDate reference date
     */
    public EskfMeasurementHandler(final SemiAnalyticalKalmanModel model,
                                  final ExtendedKalmanFilter<MeasurementDecorator> filter,
                                  final List<ObservedMeasurement<?>> observedMeasurements,
                                  final AbsoluteDate referenceDate) {
        this.model                = model;
        this.filter               = filter;
        this.observer             = model.getObserver();
        this.observedMeasurements = observedMeasurements;
        this.referenceDate        = referenceDate;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.index = 0;
        // Initialize short periodic terms.
        model.initializeShortPeriodicTerms(s0);
        model.updateShortPeriods(s0);
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final OrekitStepInterpolator interpolator) {

        // Current date
        final AbsoluteDate currentDate = interpolator.getCurrentState().getDate();

        // Update the short period terms with the current MEAN state
        model.updateShortPeriods(interpolator.getCurrentState());

        // Process the measurements between previous step and current step
        while (index < observedMeasurements.size() && observedMeasurements.get(index).getDate().compareTo(currentDate) < 0) {

            try {

                // Update the norminal state with the interpolated parameters
                model.updateNominalSpacecraftState(interpolator.getInterpolatedState(observedMeasurements.get(index).getDate()));

                // Process the current observation
                final ProcessEstimate estimate = filter.estimationStep(decorate(observedMeasurements.get(index)));

                // Finalize the estimation
                model.finalizeEstimation(observedMeasurements.get(index), estimate);

                // Call the observer if the user add one
                if (observer != null) {
                    observer.evaluationPerformed(model);
                }

            } catch (MathRuntimeException mrte) {
                throw new OrekitException(mrte);
            }

            // Increment the measurement index
            index += 1;

        }

        // Reset the initial state of the propagator
        model.finalizeOperationsObservationGrid();

    }

    /** Decorate an observed measurement.
     * <p>
     * The "physical" measurement noise matrix is the covariance matrix of the measurement.
     * Normalizing it consists in applying the following equation: Rn[i,j] =  R[i,j]/σ[i]/σ[j]
     * Thus the normalized measurement noise matrix is the matrix of the correlation coefficients
     * between the different components of the measurement.
     * </p>
     * @param observedMeasurement the measurement
     * @return decorated measurement
     */
    private MeasurementDecorator decorate(final ObservedMeasurement<?> observedMeasurement) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // Indeed, the "physical" measurement noise matrix is the covariance matrix of the measurement
        // Normalizing it leaves us with the matrix of the correlation coefficients
        final RealMatrix covariance;
        if (observedMeasurement instanceof PV) {
            // For PV measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final PV pv = (PV) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(pv.getCorrelationCoefficientsMatrix());
        } else if (observedMeasurement instanceof Position) {
            // For Position measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final Position position = (Position) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(position.getCorrelationCoefficientsMatrix());
        } else {
            // For other measurements we do not have a covariance matrix.
            // Thus the correlation coefficients matrix is an identity matrix.
            covariance = MatrixUtils.createRealIdentityMatrix(observedMeasurement.getDimension());
        }

        return new MeasurementDecorator(observedMeasurement, covariance, referenceDate);

    }

}
