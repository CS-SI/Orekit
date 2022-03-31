/* Copyright 2002-2022 CS GROUP
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
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link TDOA} measurements.
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOABuilder extends AbstractMeasurementBuilder<TDOA> {

    /** Prime ground station. */
    private final GroundStation primeStation;

    /** Second ground station. */
    private final GroundStation secondStation;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation ground station that gives the measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public TDOABuilder(final CorrelatedRandomVectorGenerator noiseSource,
                       final GroundStation primeStation,
                       final GroundStation secondStation,
                       final double sigma, final double baseWeight,
                       final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, satellite);
        this.primeStation  = primeStation;
        this.secondStation = secondStation;
    }

    /** {@inheritDoc} */
    @Override
    public TDOA build(final SpacecraftState[] states) {

        final ObservableSatellite satellite = getSatellites()[0];
        final double sigma                  = getTheoreticalStandardDeviation()[0];
        final double baseWeight             = getBaseWeight()[0];
        final SpacecraftState[] relevant    = new SpacecraftState[] { states[satellite.getPropagatorIndex()] };

        // create a dummy measurement
        final TDOA dummy = new TDOA(primeStation, secondStation, relevant[0].getDate(),
                                    Double.NaN, sigma, baseWeight, satellite);
        for (final EstimationModifier<TDOA> modifier : getModifiers()) {
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
        double tdoa = dummy.estimate(0, 0, relevant).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            tdoa += noise[0];
        }

        // generate measurement
        final TDOA measurement = new TDOA(primeStation, secondStation, relevant[0].getDate(),
                                          tdoa, sigma, baseWeight, satellite);
        for (final EstimationModifier<TDOA> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
