/* Copyright 2023 Luc Maisonobe
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
import java.util.function.ToDoubleFunction;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.gnss.OneWayGNSSPhase;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Builder for {@link OneWayGNSSPhase} measurements.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class OneWayGNSSPhaseBuilder extends AbstractMeasurementBuilder<OneWayGNSSPhase> {

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Satellite which receives the signal and performs the measurement. */
    private final ObservableSatellite local;

    /** Satellite which simply emits the signal. */
    private final ObservableSatellite remote;

    /** Clock model of the remote satellite that provides clock offset. */
    private ToDoubleFunction<AbsoluteDate> remoteClockModel;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param remoteClockModel clock model of the remote satellite that provides clock offset
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public OneWayGNSSPhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                  final ObservableSatellite local, final ObservableSatellite remote,
                                  final ToDoubleFunction<AbsoluteDate> remoteClockModel,
                                  final double wavelength, final double sigma, final double baseWeight) {
        super(noiseSource, sigma, baseWeight, local, remote);
        this.wavelength       = wavelength;
        this.local            = local;
        this.remote           = remote;
        this.remoteClockModel = remoteClockModel;
    }

    /** {@inheritDoc} */
    @Override
    public OneWayGNSSPhase build(final AbsoluteDate date, final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        final double sigma               = getTheoreticalStandardDeviation()[0];
        final double baseWeight          = getBaseWeight()[0];
        final SpacecraftState[] relevant = new SpacecraftState[] {
            interpolators.get(local).getInterpolatedState(date),
            interpolators.get(remote).getInterpolatedState(date)
        };
        final double offset              = remoteClockModel.applyAsDouble(date);

        // create a dummy measurement
        final OneWayGNSSPhase dummy = new OneWayGNSSPhase(interpolators.get(remote), offset, date,
                                                          Double.NaN, wavelength, sigma, baseWeight, local);
        for (final EstimationModifier<OneWayGNSSPhase> modifier : getModifiers()) {
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
        final OneWayGNSSPhase measurement = new OneWayGNSSPhase(interpolators.get(remote), offset, date,
                                                                phase, wavelength, sigma, baseWeight, local);
        for (final EstimationModifier<OneWayGNSSPhase> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
