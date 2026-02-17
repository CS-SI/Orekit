/* Copyright 2025-2026 Hawkeye 360 (HE360)
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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

import java.util.Collections;

import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/**
 * Class modeling a twice-received measurement using a primary and secondary observer.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted by the observed spacecraft</li>
 * <li>Received by the first observer</li>
 * <li>Received by the second observer</li>
 * </ul>
 * The date of the measurement corresponds to the reception on of the signal by the first observer.
 * <p>
 * The motion of the observers and the spacecraft during the signal flight time are taken into account.
 * </p>
 *
 * @author Brianna Aubin
 * @since 14.0
 */
abstract class DualReceiverMeasurement<T extends AbstractMeasurement<T>> extends SignalBasedMeasurement<T> {

    /**
     * First observer to receive signal.  Determines measurement date.
     */
    private final Observer primeObserver;

    /**
     * Second Observer to receive signal.  Determines measurement value.
     */
    private final Observer secondObserver;
    /**
     * Simple constructor.
     *
     * @param primeObserver         observer from which transmission is performed
     * @param secondObserver        observer from which measurement is performed
     * @param date                  date of the measurement
     * @param value                 observed value
     * @param sigma                 theoretical standard deviation
     * @param baseWeight            base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite             satellite related to this measurement
     * @since 14.0
     */
    protected DualReceiverMeasurement(final Observer primeObserver, final Observer secondObserver,
                                      final AbsoluteDate date,
                                      final double[] value, final double[] sigma, final double[] baseWeight,
                                      final SignalTravelTimeModel signalTravelTimeModel,
                                      final ObservableSatellite satellite) {
        super(date, false, value, sigma, baseWeight, signalTravelTimeModel, Collections.singletonList(satellite));

        // Add the parameters for the receiver
        addParametersDrivers(primeObserver.getParametersDrivers());
        addParametersDrivers(secondObserver.getParametersDrivers());

        // Set emitter
        this.primeObserver  = primeObserver;
        this.secondObserver = secondObserver;
    }

    /** Get the prime ground station, the one that receives the signal first.
     * @return prime ground station
     * @deprecated as of 14.0, replaced by {@link #getPrimeObserver()}
     */
    @Deprecated
    public GroundStation getPrimeStation() {
        if (!(primeObserver instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) primeObserver;
    }

    /** Get the prime observer, the one that receives the signal first.
     * @return prime observer
     * @since 14.0
     */
    public Observer getPrimeObserver() {
        return primeObserver;
    }

    /** Get the second ground station, the one that receives the signal first.
     * @return second ground station
     * @deprecated as of 14.0, replaced by {@link #getSecondObserver()}
     */
    @Deprecated
    public GroundStation getSecondStation() {
        if (!(secondObserver instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) secondObserver;
    }

    /** Get the second observer, the one that gives the measurement.
     * @return second observer
     * @since 14.0
     */
    public Observer getSecondObserver() {
        return secondObserver;
    }
}
