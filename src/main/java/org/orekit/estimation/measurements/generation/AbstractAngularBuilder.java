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

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.signal.SignalTravelTimeModel;

/** Abstract builder for angular measurements based involving one receiver.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractAngularBuilder<T extends ObservedMeasurement<T>> extends AbstractSignalBasedBuilder<T> {

    /** Zero value for initial dummy measurement. */
    static final double[] ZERO = { 0.0, 0.0 };

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     */
    protected AbstractAngularBuilder(final CorrelatedRandomVectorGenerator noiseSource, final GroundStation station,
                                     final double[] sigma, final double[] baseWeight,
                                     final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, signalTravelTimeModel, satellite);
        this.station = station;
    }

    /**
     * Getter for station.
     * @return station
     */
    public GroundStation getStation() {
        return station;
    }

}
