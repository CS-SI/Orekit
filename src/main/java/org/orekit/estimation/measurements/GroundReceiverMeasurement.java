/* Copyright 2002-2025 CS GROUP
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

import java.util.Collections;

import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Base class modeling a measurement where receiver is a ground station.
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 12.0
 * @param <T> type of the measurement
 */
public abstract class GroundReceiverMeasurement<T extends ObservedMeasurement<T>> extends SignalBasedMeasurement<T> {

    /** Ground station that receives signal from satellite. */
    private final GroundStation station;

    /** Simple constructor for scalar measurements.
     * @param station ground station from which measurement is performed
     * @param isTwoWay flag indicating whether it is a two-way measurement
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param observedValue observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    protected GroundReceiverMeasurement(final GroundStation station, final boolean isTwoWay, final AbsoluteDate date,
                                        final double[] observedValue, final double[] sigma, final double[] baseWeight,
                                        final SignalTravelTimeModel signalTravelTimeModel,
                                        final ObservableSatellite satellite) {
        super(date, isTwoWay, observedValue, sigma, baseWeight, signalTravelTimeModel, Collections.singletonList(satellite));

        addParametersDrivers(station.getParametersDrivers());

        this.station = station;
    }

    /** Get the ground station that receives the signal.
     * @return ground station
     */
    public final GroundStation getReceiverStation() {
        return station;
    }
}
