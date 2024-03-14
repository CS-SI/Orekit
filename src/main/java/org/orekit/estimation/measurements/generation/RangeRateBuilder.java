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
package org.orekit.estimation.measurements.generation;

import java.util.Map;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link RangeRate} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class RangeRateBuilder extends AbstractMeasurementBuilder<RangeRate> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Satellite related to this builder.
     * @since 12.0
     */
    private final ObservableSatellite satellite;

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
        this.station   = station;
        this.twoway    = twoWay;
        this.satellite = satellite;
    }

    /** {@inheritDoc} */
    @Override
    public RangeRate build(final AbsoluteDate date, final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        final double sigma                  = getTheoreticalStandardDeviation()[0];
        final double baseWeight             = getBaseWeight()[0];
        final SpacecraftState[] relevant    = new SpacecraftState[] { interpolators.get(satellite).getInterpolatedState(date) };

        // create a dummy measurement
        final RangeRate dummy = new RangeRate(station, relevant[0].getDate(), Double.NaN, sigma, baseWeight, twoway, satellite);
        for (final EstimationModifier<RangeRate> modifier : getModifiers()) {
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
        double rangeRate = dummy.estimateWithoutDerivatives(0, 0, relevant).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            rangeRate += noise[0];
        }

        // generate measurement
        final RangeRate measurement = new RangeRate(station, relevant[0].getDate(), rangeRate,
                                                    sigma, baseWeight, twoway, satellite);
        for (final EstimationModifier<RangeRate> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
