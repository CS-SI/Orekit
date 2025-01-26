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

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.util.Map;

/** Builder for {@link InterSatellitesRange} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatellitesRangeBuilder extends AbstractMeasurementBuilder<InterSatellitesRange> {

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesRangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                       final ObservableSatellite local, final ObservableSatellite remote,
                                       final boolean twoWay, final double sigma, final double baseWeight) {
        super(noiseSource, sigma, baseWeight, local, remote);
        this.twoway = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatellitesRange buildObserved(final AbsoluteDate date,
                                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new InterSatellitesRange(getSatellites()[0], getSatellites()[1],
                                        twoway, date, Double.NaN,
                                        getTheoreticalStandardDeviation()[0],
                                        getBaseWeight()[0]);
    }

}
