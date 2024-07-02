/* Copyright 2002-2024 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import java.util.Map;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.generation.AbstractMeasurementBuilder;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link Phase} measurements.
 * @author Luc Maisonobe
 * @since 10.1
 */
public class PhaseBuilder extends AbstractMeasurementBuilder<Phase> {

    /** Cache for ambiguities.
     * @since 12.1
     */
    private final AmbiguityCache cache;

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public PhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                        final GroundStation station, final double wavelength,
                        final double sigma, final double baseWeight,
                        final ObservableSatellite satellite,
                        final AmbiguityCache cache) {
        super(noiseSource, sigma, baseWeight, satellite);
        this.station    = station;
        this.wavelength = wavelength;
        this.cache      = cache;
    }

    /** {@inheritDoc} */
    @Override
    protected Phase buildObserved(final AbsoluteDate date,
                                  final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new Phase(station, date, Double.NaN, wavelength,
                         getTheoreticalStandardDeviation()[0],
                         getBaseWeight()[0], getSatellites()[0], cache);
    }

}
