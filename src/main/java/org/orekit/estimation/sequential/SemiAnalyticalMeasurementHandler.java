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

import java.util.List;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
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
    private AbsoluteDate referenceDate;

    /** Kalman model. */
    private final SemiAnalyticalProcess model;

    /** Kalman Filter. */
    private final KalmanFilter<MeasurementDecorator> filter;

    /** Underlying measurements. */
    private final List<ObservedMeasurement<?>> observedMeasurements;

    /** Flag indicating if the handler is used by a unscented kalman filter. */
    private final boolean isUnscented;

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
        this(model, filter, observedMeasurements, referenceDate, false);
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
        this.model                = model;
        this.filter               = filter;
        this.observedMeasurements = observedMeasurements;
        this.referenceDate        = referenceDate;
        this.isUnscented          = isUnscented;
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

                // Update predicted spacecraft state
                model.updateNominalSpacecraftState(interpolator.getInterpolatedState(observedMeasurements.get(index).getDate()));

                // Process the current observation
                final MeasurementDecorator decorated = isUnscented ?
                        KalmanEstimatorUtil.decorateUnscented(observedMeasurements.get(index), referenceDate) :
                            KalmanEstimatorUtil.decorate(observedMeasurements.get(index), referenceDate);
                final ProcessEstimate estimate = filter.estimationStep(decorated);

                // Finalize the estimation
                model.finalizeEstimation(observedMeasurements.get(index), estimate);

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
