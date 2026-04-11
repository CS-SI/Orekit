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

/** Abstract builder for scalar measurements based on one signal emitter and one receiver.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractBistaticBuilder<T extends ObservedMeasurement<T>> extends AbstractSignalBasedBuilder<T> {

    /** Signal emitter. */
    private final Observer emitter;

    /** Signal receiver. */
    private final Observer receiver;

    /** Simple constructor.
     * @param emitter observer that emits the signal
     * @param receiver observer that receiver the signal at the very end of the transmissions
     * @param measurementQuality measurement quality as used in estimation
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     */
    protected AbstractBistaticBuilder(final Observer emitter, final Observer receiver,
                                      final MeasurementQuality measurementQuality,
                                      final SignalTravelTimeModel signalTravelTimeModel,
                                      final ObservableSatellite satellite) {
        super(measurementQuality, signalTravelTimeModel, satellite);
        this.emitter  = emitter;
        this.receiver = receiver;
    }

    /**
     * Getter for signal emitter.
     * @return observer
     */
    public Observer getEmitter() {
        return emitter;
    }

    /**
     * Getter for signal receiver.
     * @return observer
     */
    public Observer getReceiver() {
        return receiver;
    }

}
