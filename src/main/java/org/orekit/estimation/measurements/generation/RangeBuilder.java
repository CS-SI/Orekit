/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.estimation.measurements.Range;
import org.orekit.propagation.SpacecraftState;


/** Builder for {@link Range} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class RangeBuilder extends AbstractMeasurementBuilder<Range> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simmple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public RangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                        final GroundStation station, final boolean twoWay,
                        final double sigma, final double baseWeight,
                        final int propagatorIndex) {
        super(noiseSource, sigma, baseWeight, propagatorIndex);
        this.station = station;
        this.twoway  = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    public Range build(final SpacecraftState... states) {

        final int propagatorIndex   = getPropagatorsIndices()[0];
        final double sigma          = getTheoreticalStandardDeviation()[0];
        final double baseWeight     = getBaseWeight()[0];
        final SpacecraftState state = states[propagatorIndex];

        // create a dummy measurement
        final Range dummy = new Range(station, twoway, state.getDate(), Double.NaN, sigma, baseWeight, propagatorIndex);

        // estimate the perfect value of the measurement
        double range = dummy.estimate(0, 0, states).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            range += noise[0];
        }

        // generate measurement
        return new Range(station, twoway, state.getDate(), range, sigma, baseWeight, propagatorIndex);

    }

}
