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
package org.orekit.estimation.measurements.generation;

import java.util.Map;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link RangeRate} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class RangeRateBuilder extends AbstractMeasurementBuilder<RangeRate> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public RangeRateBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                            final GroundStation station, final boolean twoWay,
                            final double sigma, final double baseWeight,
                            final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, satellite);
        this.station = station;
        this.twoway  = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    protected RangeRate buildObserved(final AbsoluteDate date,
                                      final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new RangeRate(station, date, Double.NaN,
                             getTheoreticalStandardDeviation()[0],
                             getBaseWeight()[0], twoway, getSatellites()[0]);
    }

}
