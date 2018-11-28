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
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.propagation.SpacecraftState;


/** Builder for {@link TurnAroundRange} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class TurnAroundRangeBuilder extends AbstractMeasurementBuilder<TurnAroundRange> {

    /** Master ground station from which measurement is performed. */
    private final GroundStation masterStation;

    /** Slave ground station reflecting the signal. */
    private final GroundStation slaveStation;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param masterStation ground station from which measurement is performed
     * @param slaveStation ground station reflecting the signal
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public TurnAroundRangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                  final GroundStation masterStation, final GroundStation slaveStation,
                                  final double sigma, final double baseWeight,
                                  final int propagatorIndex) {
        super(noiseSource, sigma, baseWeight, propagatorIndex);
        this.masterStation = masterStation;
        this.slaveStation  = slaveStation;
    }

    /** {@inheritDoc} */
    @Override
    public TurnAroundRange build(final SpacecraftState... states) {

        final int propagatorIndex   = getPropagatorsIndices()[0];
        final double sigma          = getTheoreticalStandardDeviation()[0];
        final double baseWeight     = getBaseWeight()[0];
        final SpacecraftState state = states[propagatorIndex];

        // create a dummy measurement
        final TurnAroundRange dummy = new TurnAroundRange(masterStation, slaveStation, state.getDate(), Double.NaN, sigma, baseWeight, propagatorIndex);

        // estimate the perfect value of the measurement
        double range = dummy.estimate(0, 0, states).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            range += noise[0];
        }

        // generate measurement
        return new TurnAroundRange(masterStation, slaveStation, state.getDate(), range, sigma, baseWeight, propagatorIndex);

    }

}
