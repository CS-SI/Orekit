/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Abstract class modeling a measurement based on signal transmission.
 * @author Romain Serra
 * @since 14.0
 * @param <T> type of the measurement
 */
public abstract class SignalBasedMeasurement<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Whether measurement is two-way or not (true for two-way). */
    private final boolean isTwoWay;

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /** Simple constructor for scalar measurements.
     * @param isTwoWay flag indicating whether it is a two-way measurement
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param observedValue observed value
     * @param measurementQuality measurement quality
     * @param satellites satellites related to this measurement
     */
    protected SignalBasedMeasurement(final AbsoluteDate date, final boolean isTwoWay,
                                     final double observedValue, final MeasurementQuality measurementQuality,
                                     final SignalTravelTimeModel signalTravelTimeModel,
                                     final List<ObservableSatellite> satellites) {
        this(date, isTwoWay, new double[] { observedValue }, measurementQuality, signalTravelTimeModel, satellites);
    }

    /** Simple constructor for measurements.
     * @param isTwoWay flag indicating whether it is a two-way measurement
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param observedValue observed value
     * @param measurementQuality measurement quality
     * @param satellites satellites related to this measurement
     */
    protected SignalBasedMeasurement(final AbsoluteDate date, final boolean isTwoWay,
                                     final double[] observedValue, final MeasurementQuality measurementQuality,
                                     final SignalTravelTimeModel signalTravelTimeModel,
                                     final List<ObservableSatellite> satellites) {
        super(date, observedValue, measurementQuality, satellites);
        this.isTwoWay = isTwoWay;
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Getter for the signal model.
     * @return model
     */
    public SignalTravelTimeModel getSignalTravelTimeModel() {
        return signalTravelTimeModel;
    }

    /** Check if a measurement is two-way.
     * @return true if the measurement is two-way
     */
    public boolean isTwoWay() {
        return isTwoWay;
    }
}
