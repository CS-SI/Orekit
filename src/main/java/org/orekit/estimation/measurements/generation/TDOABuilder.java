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

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link TDOA} measurements.
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOABuilder extends AbstractBireceiverBuilder<TDOA> {

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param primeObserver observer that gives the date of the measurement
     * @param secondObserver observer that gives the measurement value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public TDOABuilder(final CorrelatedRandomVectorGenerator noiseSource,
                       final Observer primeObserver, final Observer secondObserver,
                       final double sigma, final double baseWeight,
                       final ObservableSatellite satellite) {
        this(noiseSource, primeObserver, secondObserver, sigma, baseWeight, new SignalTravelTimeModel(), satellite);
    }

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param primeObserver observer that gives the date of the measurement
     * @param secondObserver observer that gives the measurement value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     * @since 14.0
     */
    public TDOABuilder(final CorrelatedRandomVectorGenerator noiseSource,
                       final Observer primeObserver, final Observer secondObserver,
                       final double sigma, final double baseWeight,
                       final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(noiseSource, primeObserver, secondObserver, sigma, baseWeight, signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected TDOA buildObserved(final AbsoluteDate date,
                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new TDOA(getPrimeObserver(), getSecondObserver(), date, Double.NaN, getTheoreticalStandardDeviation()[0],
                        getBaseWeight()[0], getSignalTravelTimeModel(), getSatellites()[0]);
    }

}
