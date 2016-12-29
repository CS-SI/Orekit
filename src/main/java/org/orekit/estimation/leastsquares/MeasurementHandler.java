/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** {@link org.orekit.propagation.sampling.OrekitStepHandler Step handler} picking up
 * {@link ObservedMeasurement measurements}.
 * @author Luc Maisonobe
 * @since 8.0
 */
class MeasurementHandler implements OrekitStepHandler {

    /** Least squares model. */
    private final Model model;

    /** Underlying measurements. */
    private final List<PreCompensation> precompensated;

    /** Number of the next measurement. */
    private int number;

    /** Index of the next measurement component in the model. */
    private int index;

    /** Simple constructor.
     * @param model least squares model
     * @param precompensated underlying measurements
     */
    MeasurementHandler(final Model model, final List<PreCompensation> precompensated) {
        this.model          = model;
        this.precompensated = precompensated;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        number = 0;
        index  = 0;
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast)
        throws OrekitException {

        while (number < precompensated.size()) {

            // consider the next measurement to handle
            final PreCompensation next = precompensated.get(number);

            if (next.getDate().compareTo(interpolator.getCurrentState().getDate()) > 0) {
                // the next date is past the end of the interpolator,
                // it will be picked-up in a future step
                if (isLast) {
                    // this should never happen
                    throw new OrekitInternalError(null);
                }
                return;
            }

            // get the observed measurement
            final ObservedMeasurement<?> observed = next.getMeasurement();

            // estimate the theoretical measurement
            final SpacecraftState         state     = interpolator.getInterpolatedState(next.getDate());
            final EstimatedMeasurement<?> estimated = observed.estimate(model.getIterationsCount(),
                                                                        model.getEvaluationsCount(),
                                                                        state);

            // fetch the evaluated measurement to the estimator
            model.fetchEvaluatedMeasurement(index, estimated);

            // prepare handling of next measurement
            ++number;
            index += observed.getDimension();

        }

    }

}
