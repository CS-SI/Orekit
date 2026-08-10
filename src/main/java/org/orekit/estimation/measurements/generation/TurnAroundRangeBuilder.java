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
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link TurnAroundRange} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class TurnAroundRangeBuilder extends AbstractSignalBasedBuilder<TurnAroundRange> {

    /** Primary observer from which measurement is performed. */
    private final Observer primaryObserver;

    /** Secondary observer reflecting the signal. */
    private final Observer secondaryObserver;

    /** Simple constructor.
     * @param primaryObserver observer from which measurement is performed
     * @param secondaryObserver observer reflecting the signal
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public TurnAroundRangeBuilder(final Observer primaryObserver, final Observer secondaryObserver,
                                  final double sigma, final double baseWeight,
                                  final ObservableSatellite satellite) {
        this(primaryObserver, secondaryObserver, new MeasurementQuality(sigma, baseWeight),
                new SignalTravelTimeModel(), satellite);
    }

    /** Simple constructor.
     * @param primaryObserver observer from which measurement is performed
     * @param secondaryObserver observer reflecting the signal
     * @param measurementQuality measurement quality as used in estimation
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     * @since 14.0
     */
    public TurnAroundRangeBuilder(final Observer primaryObserver, final Observer secondaryObserver,
                                  final MeasurementQuality measurementQuality,
                                  final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(measurementQuality, signalTravelTimeModel, new ObservableSatellite[] {satellite});
        this.primaryObserver   = primaryObserver;
        this.secondaryObserver = secondaryObserver;
    }

    /** {@inheritDoc} */
    @Override
    protected TurnAroundRange buildObserved(final AbsoluteDate date,
                                            final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new TurnAroundRange(primaryObserver, secondaryObserver, date, Double.NaN, getTheoreticalStandardDeviation()[0],
                                   getBaseWeight()[0], getSignalTravelTimeModel(), getSatellites()[0]);
    }

}
