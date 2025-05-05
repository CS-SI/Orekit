/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.gnss.InterSatellitesOneWayRangeRate;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.util.Map;

/** Builder for {@link org.orekit.estimation.measurements.gnss.InterSatellitesOneWayRangeRate} measurements.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class InterSatellitesOneWayRangeRateBuilder
    extends AbstractMeasurementBuilder<InterSatellitesOneWayRangeRate> {

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesOneWayRangeRateBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                                 final ObservableSatellite local, final ObservableSatellite remote,
                                                 final double sigma, final double baseWeight) {
        super(noiseSource, sigma, baseWeight, local, remote);
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatellitesOneWayRangeRate buildObserved(final AbsoluteDate date,
                                                           final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new InterSatellitesOneWayRangeRate(getSatellites()[0], getSatellites()[1],
                                                  date, Double.NaN,
                                                  getTheoreticalStandardDeviation()[0],
                                                  getBaseWeight()[0]);
    }

}
