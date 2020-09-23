/* Copyright 2002-2020 CS GROUP
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
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.gnss.InterSatellitesPhase;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link InterSatellitesPhase} measurements.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class InterSatellitesPhaseBuilder extends AbstractMeasurementBuilder<InterSatellitesPhase> {

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesPhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                       final ObservableSatellite local, final ObservableSatellite remote,
                                       final double wavelength, final double sigma, final double baseWeight) {
        super(noiseSource, sigma, baseWeight, local, remote);
        this.wavelength = wavelength;
    }

    /** {@inheritDoc} */
    @Override
    public InterSatellitesPhase build(final SpacecraftState[] states) {

        final ObservableSatellite[] satellites = getSatellites();
        final double sigma                     = getTheoreticalStandardDeviation()[0];
        final double baseWeight                = getBaseWeight()[0];
        final SpacecraftState[] relevant       = new SpacecraftState[] {
            states[satellites[0].getPropagatorIndex()],
            states[satellites[1].getPropagatorIndex()]
        };
        final SpacecraftState state            = states[satellites[0].getPropagatorIndex()];

        // create a dummy measurement
        final InterSatellitesPhase dummy = new InterSatellitesPhase(satellites[0], satellites[1], state.getDate(),
                                                                    Double.NaN, wavelength, sigma, baseWeight);
        for (final EstimationModifier<InterSatellitesPhase> modifier : getModifiers()) {
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
        double phase = dummy.estimate(0, 0, relevant).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            phase += noise[0];
        }

        // generate measurement
        final InterSatellitesPhase measurement = new InterSatellitesPhase(satellites[0], satellites[1], state.getDate(),
                                                                          phase, wavelength, sigma, baseWeight);
        for (final EstimationModifier<InterSatellitesPhase> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
