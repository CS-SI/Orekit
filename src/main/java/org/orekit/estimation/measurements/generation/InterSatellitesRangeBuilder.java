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
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link InterSatellitesRange} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatellitesRangeBuilder extends AbstractMeasurementBuilder<InterSatellitesRange> {

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param satellite1Index index of satellite 1 propagator
     * (i.e. the satellite which receives the signal and performs
     * the measurement)
     * @param satellite2Index index of satellite 2 propagator
     * (i.e. the satellite which simply emits the signal in the one-way
     * case, or reflects the signal in the two-way case)
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesRangeBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                       final int satellite1Index, final int satellite2Index,
                                       final boolean twoWay, final double sigma, final double baseWeight) {
        super(noiseSource, sigma, baseWeight, satellite1Index, satellite2Index);
        this.twoway  = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    public InterSatellitesRange build(final SpacecraftState[] states) {

        final int[] indices         = getPropagatorsIndices();
        final double sigma          = getTheoreticalStandardDeviation()[0];
        final double baseWeight     = getBaseWeight()[0];
        final SpacecraftState state = states[indices[0]];

        // create a dummy measurement
        final InterSatellitesRange dummy = new InterSatellitesRange(indices[0], indices[1], twoway, state.getDate(),
                                                                    Double.NaN, sigma, baseWeight);
        for (final EstimationModifier<InterSatellitesRange> modifier : getModifiers()) {
            dummy.addModifier(modifier);
        }

        // set a reference date for parameters missing one
        for (final ParameterDriver driver : dummy.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                final AbsoluteDate start = getStart();
                final AbsoluteDate end   = getEnd();
                driver.setReferenceDate(start.durationFrom(end) <= 0 ? start : end);
            }
        }

        // estimate the perfect value of the measurement
        double range = dummy.estimate(0, 0, states).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            range += noise[0];
        }

        // generate measurement
        final InterSatellitesRange measurement = new InterSatellitesRange(indices[0], indices[1], twoway, state.getDate(),
                                                                          range, sigma, baseWeight);
        for (final EstimationModifier<InterSatellitesRange> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
