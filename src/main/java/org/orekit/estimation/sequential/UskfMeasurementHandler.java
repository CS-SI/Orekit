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
import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** {@link org.orekit.propagation.sampling.OrekitStepHandler Step handler} picking up
 * {@link ObservedMeasurement measurements} for the {@link SemiAnalyticalUnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 */
public class UskfMeasurementHandler implements MultiSatStepHandler {

    /** Least squares model. */
    private final SemiAnalyticalUnscentedKalmanModel model;

    /** DSST propagator builder. */
    private final List<DSSTPropagatorBuilder> builders;

    /** DSST propagator. */
    private final List<Propagator> propagators;

    /** Extended Kalman Filter. */
    private final UnscentedKalmanFilter<MeasurementDecorator> filter;

    /** Underlying measurements. */
    private final List<ObservedMeasurement<?>> observedMeasurements;

    /** Index of the next measurement component in the model. */
    private int index;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Sigma points of the current step. */
    private RealVector[] sigmaPoints;

    /** Simple constructor.
     * @param model semi-analytical kalman model
     * @param filter kalman filter instance
     * @param observedMeasurements list of observed measurements
     * @param referenceDate reference date
     * @param propagators propagators
     * @param builders builders
     * @param sigmaPoints sigma points
     */
    public UskfMeasurementHandler(final SemiAnalyticalUnscentedKalmanModel model,
                                  final UnscentedKalmanFilter<MeasurementDecorator> filter,
                                  final List<ObservedMeasurement<?>> observedMeasurements,
                                  final AbsoluteDate referenceDate,
                                  final List<Propagator> propagators,
                                  final List<DSSTPropagatorBuilder> builders,
                                  final RealVector[] sigmaPoints) {
        this.model                = model;
        this.filter               = filter;
        this.observer             = model.getObserver();
        this.observedMeasurements = observedMeasurements;
        this.referenceDate        = referenceDate;
        this.propagators          = propagators;
        this.builders             = builders;
        this.sigmaPoints          = sigmaPoints;
    }


    public void init(final List<SpacecraftState> initialStates, final AbsoluteDate t) {
        this.index = 0;
        // Initialize short periodic terms.
        for (int i = 0; i < initialStates.size(); i++) {
            model.initializeShortPeriodicTerms(initialStates.get(i), propagators.get(i));
            model.updateShortPeriods(initialStates.get(i), builders.get(i));
        }

    }

    @Override
    public void handleStep(final List<OrekitStepInterpolator> interpolators) {
        // TODO Auto-generated method stub
        final AbsoluteDate currentDate = interpolators.get(0).getCurrentState().getDate();

        for (int i = 0; i < interpolators.size(); i++) {
            // Update the short period terms with the current MEAN state
            model.updateShortPeriods(interpolators.get(i).getCurrentState(), builders.get(i));
            // Process the measurements between previous step and current step
        }    
            while (index < observedMeasurements.size() && (observedMeasurements.get(index).getDate().compareTo(currentDate) < 0 || index == observedMeasurements.size()-1)  ) {

                try {

                    for (int i = 0; i < interpolators.size(); i++) {
                        // Update the nominal state with the interpolated parameters
                        SpacecraftState s = interpolators.get(i).getInterpolatedState(observedMeasurements.get(index).getDate());
                        model.updateNominalSpacecraftState(s, builders.get(i), i);
                    }
                    // Process the current observation
                    ProcessEstimate estimate = filter.predictionAndCorrectionStep(decorate(observedMeasurements.get(index)), sigmaPoints);
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


        // Update the sigmaPoints
        sigmaPoints = filter.unscentedTransform();


        // Reset the initial state of the propagators with sigma points
        model.finalizeOperationsObservationGrid(sigmaPoints);

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

