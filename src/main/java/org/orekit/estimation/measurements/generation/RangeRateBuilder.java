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

import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link RangeRate} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class RangeRateBuilder extends AbstractSignalBasedBuilder<RangeRate> {

    /** Observer from which measurement is performed. */
    private final Observer observer;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param observer observer from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public RangeRateBuilder(final Observer observer, final boolean twoWay,
                            final double sigma, final double baseWeight,
                            final ObservableSatellite satellite) {
        this(observer, twoWay, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(),
                satellite);
    }

    /** Simple constructor.
     * @param observer observer from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param satellite satellite related to this builder
     * @since 14.0
     */
    public RangeRateBuilder(final Observer observer, final boolean twoWay,
                            final MeasurementQuality measurementQuality, final SignalTravelTimeModel signalTravelTimeModel,
                            final ObservableSatellite satellite) {
        super(measurementQuality, signalTravelTimeModel, satellite);
        this.observer = observer;
        this.twoway  = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    protected RangeRate buildObserved(final AbsoluteDate date,
                                      final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new RangeRate(observer, date, Double.NaN, getMeasurementQuality(), twoway, getSignalTravelTimeModel(),
                getSatellites()[0]);
    }

}
