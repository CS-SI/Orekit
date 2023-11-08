/* Copyright 2002-2023 CS GROUP
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
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.generation.AbstractMeasurementBuilder;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link Phase} measurements.
 * @author Luc Maisonobe
 * @since 10.1
 */
public class PhaseBuilder extends AbstractMeasurementBuilder<Phase> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Satellite related to this builder.
     * @since 12.0
     */
    private final ObservableSatellite satellite;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public PhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                        final GroundStation station, final double wavelength,
                        final double sigma, final double baseWeight,
                        final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, satellite);
        this.station    = station;
        this.wavelength = wavelength;
        this.satellite  = satellite;
    }

    /** {@inheritDoc} */
    @Override
    public Phase build(final AbsoluteDate date, final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        final double sigma                  = getTheoreticalStandardDeviation()[0];
        final double baseWeight             = getBaseWeight()[0];
        final SpacecraftState[] relevant    = new SpacecraftState[] { interpolators.get(satellite).getInterpolatedState(date) };

        // create a dummy measurement
        final Phase dummy = new Phase(station, relevant[0].getDate(), Double.NaN, wavelength, sigma, baseWeight, satellite);
        for (final EstimationModifier<Phase> modifier : getModifiers()) {
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
        double phase = dummy.estimateWithoutDerivatives(0, 0, relevant).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            phase += noise[0];
        }

        // generate measurement
        final Phase measurement = new Phase(station, relevant[0].getDate(), phase, wavelength, sigma, baseWeight, satellite);
        for (final EstimationModifier<Phase> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
