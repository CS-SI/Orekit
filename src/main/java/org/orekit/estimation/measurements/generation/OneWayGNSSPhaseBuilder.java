/* Copyright 2002-2024 Luc Maisonobe
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
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.estimation.measurements.gnss.AmbiguityCache;
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

    /** Cache for ambiguities.
     * @since 12.1
     */
    private final AmbiguityCache cache;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Satellite which receives the signal and performs the measurement. */
    private final ObservableSatellite local;

    /** Satellite which simply emits the signal. */
    private final ObservableSatellite remote;

    /** Clock model of the remote satellite that provides clock offset. */
    private final QuadraticClockModel remoteClockModel;

    /** Temporary builder for clock models.
     * @deprecated this is a temporary field, it will be removed in Orekit 13.0
     */
    @Deprecated
    private Function<AbsoluteDate, QuadraticClockModel> clockBuilder;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param remoteClockModel clock model of the remote satellite that provides clock offset
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @deprecated as of 12.1, replaced by {@link #OneWayGNSSPhaseBuilder(CorrelatedRandomVectorGenerator,
     * ObservableSatellite, ObservableSatellite, QuadraticClockModel,
     * double, double, double, AmbiguityCache)}
     */
    @Deprecated
    public OneWayGNSSPhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                  final ObservableSatellite local, final ObservableSatellite remote,
                                  final ToDoubleFunction<AbsoluteDate> remoteClockModel,
                                  final double wavelength, final double sigma, final double baseWeight) {
        this(noiseSource, local, remote, null, wavelength, sigma, baseWeight,
             AmbiguityCache.DEFAULT_CACHE);
        this.clockBuilder = date -> {
            final double cM = remoteClockModel.applyAsDouble(date.shiftedBy(-1));
            final double c0 = remoteClockModel.applyAsDouble(date);
            final double cP = remoteClockModel.applyAsDouble(date.shiftedBy(1));
            return new QuadraticClockModel(date, c0, 0.5 * (cP - cM), 0.5 * (cP + cM) - c0);
        };
    }

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param remoteClockModel clock model of the remote satellite that provides clock offset
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public OneWayGNSSPhaseBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                  final ObservableSatellite local, final ObservableSatellite remote,
                                  final QuadraticClockModel remoteClockModel,
                                  final double wavelength, final double sigma, final double baseWeight,
                                  final AmbiguityCache cache) {
        super(noiseSource, sigma, baseWeight, local, remote);
        this.wavelength       = wavelength;
        this.local            = local;
        this.remote           = remote;
        this.remoteClockModel = remoteClockModel;
        this.cache            = cache;
    }

    /** {@inheritDoc} */
    @Override
    public OneWayGNSSPhase build(final AbsoluteDate date,
                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        final double sigma               = getTheoreticalStandardDeviation()[0];
        final double baseWeight          = getBaseWeight()[0];
        final SpacecraftState[] relevant = new SpacecraftState[] {
            interpolators.get(local).getInterpolatedState(date),
            interpolators.get(remote).getInterpolatedState(date)
        };

        // temporary hack to build QuadraticClockModel from ToDoubleFunction<AbsoluteDate>
        // for compatibility purposes
        final QuadraticClockModel clockModel = remoteClockModel != null ?
                                               remoteClockModel :
                                               clockBuilder.apply(date);

        // create a dummy measurement
        final OneWayGNSSPhase dummy = new OneWayGNSSPhase(interpolators.get(remote),
                                                          remote.getName(),
                                                          clockModel, date,
                                                          Double.NaN, wavelength,
                                                          sigma, baseWeight, local,
                                                          cache);
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
        double phase = dummy.estimateWithoutDerivatives(relevant).getEstimatedValue()[0];

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            phase += noise[0];
        }

        // generate measurement
        final OneWayGNSSPhase measurement = new OneWayGNSSPhase(interpolators.get(remote),
                                                                remote.getName(),
                                                                clockModel, date,
                                                                phase, wavelength, sigma, baseWeight, local,
                                                                cache);
        for (final EstimationModifier<OneWayGNSSPhase> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }
        return measurement;

    }

}
