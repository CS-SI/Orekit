/* Copyright 2002-2026 CS GROUP
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

import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.gnss.AmbiguityCache;
import org.orekit.estimation.measurements.gnss.InterSatellitesPhase;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link InterSatellitesPhase} measurements.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class InterSatellitesPhaseBuilder extends AbstractSignalBasedBuilder<InterSatellitesPhase> {

    /** Cache for ambiguities.
     * @since 12.1
     */
    private final AmbiguityCache cache;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public InterSatellitesPhaseBuilder(final ObservableSatellite local, final ObservableSatellite remote,
                                       final double wavelength, final double sigma, final double baseWeight,
                                       final AmbiguityCache cache) {
        this(local, remote, wavelength, new MeasurementQuality(sigma, baseWeight),
                new SignalTravelTimeModel(), cache);
    }

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal
     * @param wavelength phase observed value wavelength (m)
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param cache from which ambiguity drive should come
     * @since 14.0
     */
    public InterSatellitesPhaseBuilder(final ObservableSatellite local, final ObservableSatellite remote,
                                       final double wavelength, final MeasurementQuality measurementQuality,
                                       final SignalTravelTimeModel signalTravelTimeModel,
                                       final AmbiguityCache cache) {
        super(measurementQuality, signalTravelTimeModel, new ObservableSatellite[] {local, remote});
        this.cache      = cache;
        this.wavelength = wavelength;
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatellitesPhase buildObserved(final AbsoluteDate date,
                                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new InterSatellitesPhase(getSatellites()[0], getSatellites()[1],
                                        date, Double.NaN, wavelength, getMeasurementQuality(),
                                        getSignalTravelTimeModel(), cache);
    }

}
