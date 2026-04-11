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
package org.orekit.estimation.measurements.generation;

import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Observer;
import org.orekit.signal.SignalTravelTimeModel;

/** Abstract builder for measurements based on two signal receivers.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractBireceiverBuilder<T extends ObservedMeasurement<T>> extends AbstractSignalBasedBuilder<T> {

    /** Prime ground observer. */
    private final Observer primeObserver;

    /** Second ground observer. */
    private final Observer secondObserver;

    /** Simple constructor.
     * @param primeObserver observer that gives the date of the measurement
     * @param secondObserver observer that gives the measurement
     * @param measurementQuality measurement quality as used in estimation
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     */
    protected AbstractBireceiverBuilder(final Observer primeObserver, final Observer secondObserver,
                                        final MeasurementQuality measurementQuality,
                                        final SignalTravelTimeModel signalTravelTimeModel,
                                        final ObservableSatellite satellite) {
        super(measurementQuality, signalTravelTimeModel, satellite);
        this.primeObserver  = primeObserver;
        this.secondObserver = secondObserver;
    }

    /**
     * Getter for prime observer.
     * @return observer
     */
    public Observer getPrimeObserver() {
        return primeObserver;
    }

    /**
     * Getter for second observer.
     * @return observer
     */
    public Observer getSecondObserver() {
        return secondObserver;
    }

}
