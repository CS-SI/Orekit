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

import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.EventHandler;

/** Bridge between {@link org.orekit.propagation.events.EventDetector events} and
 * {@link ObservedMeasurement measurements}.
 * @author Luc Maisonobe
 * @since 8.0
 */
class MeasurementHandler implements EventHandler<DateDetector> {

    /** Least squares model. */
    private final Model model;

    /** Underlying measurement. */
    private final ObservedMeasurement<?> measurement;

    /** Index of the first measurement component in the model. */
    private final int index;

    /** Simple constructor.
     * @param model least squares model
     * @param measurement underlying measurement
     * @param index index of the first measurement component in the model
     */
    MeasurementHandler(final Model model, final ObservedMeasurement<?> measurement, final int index) {
        this.model       = model;
        this.measurement = measurement;
        this.index       = index;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final DateDetector detector,
                                final boolean increasing)
        throws OrekitException {

        // fetch the evaluated measurement to the estimator
        model.fetchEvaluatedMeasurement(index, measurement.estimate(model.getIterationsCount(),
                                                                    model.getEvaluationsCount(),
                                                                    s));

        return Action.CONTINUE;

    }

}
