/* Copyright 2002-2026 Mark Rutten
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
package org.orekit.estimation.measurements.generation;

import java.util.Map;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.BistaticRange;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link BistaticRange} measurements.
 * @author Pascal Parraud
 * @author Mark Rutten
 * @since 11.2
 */
public class BistaticRangeBuilder extends AbstractBistaticBuilder<BistaticRange> {

    /** Constructor with default signal travel time model.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param emitter emitter observer
     * @param receiver receiver observer, from which measurement is performed
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public BistaticRangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                final Observer emitter, final Observer receiver,
                                final double sigma, final double baseWeight,
                                final ObservableSatellite satellite) {
        this(noiseSource, emitter, receiver, sigma, baseWeight, new SignalTravelTimeModel(), satellite);
    }

    /** Constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param emitter emitter observer
     * @param receiver receiver observer, from which measurement is performed
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this builder
     * @since 14.0
     */
    public BistaticRangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                final Observer emitter, final Observer receiver,
                                final double sigma, final double baseWeight,
                                final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(noiseSource, emitter, receiver, sigma, baseWeight, signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected BistaticRange buildObserved(final AbsoluteDate date,
                                          final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new BistaticRange(getEmitter(), getReceiver(), date, 0.0, getTheoreticalStandardDeviation()[0],
                getBaseWeight()[0], getSignalTravelTimeModel(), getSatellites()[0]);
    }

}
