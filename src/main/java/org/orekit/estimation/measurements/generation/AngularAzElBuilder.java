/* Copyright 2002-2026 CS GROUP
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

import java.util.Map;

import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link AngularAzEl} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class AngularAzElBuilder extends AbstractSignalBasedBuilder<AngularAzEl> {

    /** Station performing measurement. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public AngularAzElBuilder(final GroundStation station,
                              final double[] sigma, final double[] baseWeight, final ObservableSatellite satellite) {
        this(station, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), satellite);
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param measurementQuality measurement quality as used in estimation (in Orekit, the crossed-terms
     *                           of the covariance matrix are only used by Kalman filters, not least squares)
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     * @since 14.0
     */
    public AngularAzElBuilder(final GroundStation station,
                              final MeasurementQuality measurementQuality,
                              final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(measurementQuality, signalTravelTimeModel, satellite);
        this.station = station;
    }

    /**
     * Getter for station.
     * @return station
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected AngularAzEl buildObserved(final AbsoluteDate date,
                                        final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new AngularAzEl(station, date, new double[2], getMeasurementQuality(), getSignalTravelTimeModel(), getSatellites()[0]);
    }

}
