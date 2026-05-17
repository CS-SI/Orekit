/* Copyright 2002-2026 CS GROUP
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
import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;

/** {@link org.orekit.propagation.sampling.OrekitStepHandler Step handler} picking up
 * {@link ObservedMeasurement measurements} for both {@link SemiAnalyticalUnscentedKalmanEstimator} and {@link SemiAnalyticalKalmanEstimator}.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @author Julie Bayard
 * @author Maxime Journot
 * @since 11.3
 */
public class SemiAnalyticalMeasurementHandler implements OrekitStepHandler {

    /** Index of the next measurement component in the model. */
    private int index;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Kalman model. */
    private final SemiAnalyticalProcess model;

    /** Kalman Filter. */
    private final KalmanFilter<MeasurementDecorator> filter;

    /** Underlying measurements. */
    private final List<ObservedMeasurement<?>> observedMeasurements;

    /** Flag indicating if the handler is used by a unscented kalman filter. */
    private final boolean isUnscented;

    /** DSST propagators for multi-satellite support. */
    private final DSSTPropagator[] propagators;

    /** Simple constructor.
     * <p>
     * Using this constructor, the Kalman filter is supposed to be extended.
     * </p>
     * @param model semi-analytical kalman model
     * @param filter kalman filter instance
     * @param observedMeasurements list of observed measurements
     * @param referenceDate reference date
     * @see #SemiAnalyticalMeasurementHandler(SemiAnalyticalProcess, KalmanFilter, List, AbsoluteDate, boolean)
     */
    public SemiAnalyticalMeasurementHandler(final SemiAnalyticalProcess model,
                                  final KalmanFilter<MeasurementDecorator> filter,
                                  final List<ObservedMeasurement<?>> observedMeasurements,
                                  final AbsoluteDate referenceDate) {
        this(model, filter, observedMeasurements, referenceDate, false, null);
    }

    /** Simple constructor.
     * @param model semi-analytical kalman model
     * @param filter kalman filter instance
     * @param observedMeasurements list of observed measurements
     * @param referenceDate reference date
     * @param isUnscented true if the Kalman filter is unscented
     * @since 11.3.2
     */
    public SemiAnalyticalMeasurementHandler(final SemiAnalyticalProcess model,
                                  final KalmanFilter<MeasurementDecorator> filter,
                                  final List<ObservedMeasurement<?>> observedMeasurements,
                                  final AbsoluteDate referenceDate,
                                  final boolean isUnscented) {
        this(model, filter, observedMeasurements, referenceDate, isUnscented, null);
    }

    /** Simple constructor with propagators for multi-satellite support.
     * @param model semi-analytical kalman model
     * @param filter kalman filter instance
     * @param observedMeasurements list of observed measurements
     * @param referenceDate reference date
     * @param isUnscented true if the Kalman filter is unscented
     * @param propagators DSST propagators for multi-satellite support
     */
    public SemiAnalyticalMeasurementHandler(final SemiAnalyticalProcess model,
                                  final KalmanFilter<MeasurementDecorator> filter,
                                  final List<ObservedMeasurement<?>> observedMeasurements,
                                  final AbsoluteDate referenceDate,
                                  final boolean isUnscented,
                                  final DSSTPropagator[] propagators) {
        this.model                = model;
        this.filter               = filter;
        this.observedMeasurements = observedMeasurements;
        this.referenceDate        = referenceDate;
        this.isUnscented          = isUnscented;
        this.propagators          = propagators;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.index = 0;
        // Initialize short periodic terms for all satellites
        if (propagators != null) {
            for (int k = 0; k < propagators.length; k++) {
                final SpacecraftState initialState = propagators[k].getInitialState();
                if (model instanceof SemiAnalyticalKalmanModel) {
                    ((SemiAnalyticalKalmanModel) model).initializeShortPeriodicTerms(initialState, k);
                    ((SemiAnalyticalKalmanModel) model).updateShortPeriods(initialState, k);
                } else {
                    model.initializeShortPeriodicTerms(initialState);
                    model.updateShortPeriods(initialState);
                }
            }
        } else {
            model.initializeShortPeriodicTerms(s0);
            model.updateShortPeriods(s0);
        }
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

                final ObservedMeasurement<?> measurement = observedMeasurements.get(index);
                final AbsoluteDate measDate = measurement.getDate();

                // Update predicted spacecraft state(s)
                if (propagators != null && model instanceof SemiAnalyticalKalmanModel) {
                    // Multi-satellite: update all propagator states using interpolated state
                    final SemiAnalyticalKalmanModel kalmanModel = (SemiAnalyticalKalmanModel) model;
                    final SpacecraftState interpolatedState = interpolator.getInterpolatedState(measDate);
                    for (int k = 0; k < propagators.length; k++) {
                        kalmanModel.updateNominalSpacecraftState(interpolatedState, k);
                    }
                } else {
                    // Single satellite (backward compatibility)
                    model.updateNominalSpacecraftState(interpolator.getInterpolatedState(measDate));
                }

                // Process the current observation
                final MeasurementDecorator decorated = isUnscented ?
                        KalmanEstimatorUtil.decorateUnscented(measurement, referenceDate) :
                            KalmanEstimatorUtil.decorate(measurement, referenceDate);
                final ProcessEstimate estimate = filter.estimationStep(decorated);

                // Finalize the estimation
                model.finalizeEstimation(measurement, estimate);

            } catch (MathRuntimeException mrte) {
                throw new OrekitException(mrte);
            }

            // Increment the measurement index
            index += 1;

        }

        // Reset the initial state of the propagator
        model.finalizeOperationsObservationGrid();

    }

}
