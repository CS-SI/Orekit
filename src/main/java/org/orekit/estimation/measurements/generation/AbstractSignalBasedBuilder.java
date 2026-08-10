/* Copyright 2002-2026 Romain Serra
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
package org.orekit.estimation.measurements.generation;

import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.signal.SignalTravelTimeModel;

/** Abstract builder for physical measurements (based on signal transmission).
 * @param <T> type of the measurement
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractSignalBasedBuilder<T extends ObservedMeasurement<T>> extends AbstractMeasurementBuilder<T> {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /** Simple constructor.
     * @param measurementQuality measurement quality as used in estimation (in Orekit, the crossed-terms
     *                           of the covariance matrix are only used by Kalman filters, not least squares)
     * @param signalTravelTimeModel signal travel time model
     * @param satellites satellites related to this builder
     */
    protected AbstractSignalBasedBuilder(final MeasurementQuality measurementQuality,
                                         final SignalTravelTimeModel signalTravelTimeModel,
                                         final ObservableSatellite[] satellites) {
        super(measurementQuality, satellites);
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /** Simple constructor for single observable satellite.
     * @param measurementQuality measurement quality as used in estimation (in Orekit, the crossed-terms
     *                           of the covariance matrix are only used by Kalman filters, not least squares)
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     */
    protected AbstractSignalBasedBuilder(final MeasurementQuality measurementQuality,
                                         final SignalTravelTimeModel signalTravelTimeModel,
                                         final ObservableSatellite satellite) {
        this(measurementQuality, signalTravelTimeModel, new ObservableSatellite[] {satellite});
    }

    /**
     * Getter for signal model.
     * @return model
     */
    public SignalTravelTimeModel getSignalTravelTimeModel() {
        return signalTravelTimeModel;
    }
}
