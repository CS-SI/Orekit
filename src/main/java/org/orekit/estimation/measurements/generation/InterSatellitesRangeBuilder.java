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

import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link InterSatellitesRange} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatellitesRangeBuilder extends AbstractSignalBasedBuilder<InterSatellitesRange> {

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesRangeBuilder(final ObservableSatellite local, final ObservableSatellite remote,
                                       final boolean twoWay, final double sigma, final double baseWeight) {
        this(local, remote, twoWay, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel());
    }

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param measurementQuality measurement quality as used in estimation
     * @param signalTravelTimeModel signal travel time model
     * @since 14.0
     */
    public InterSatellitesRangeBuilder(final ObservableSatellite local, final ObservableSatellite remote,
                                       final boolean twoWay, final MeasurementQuality measurementQuality,
                                       final SignalTravelTimeModel signalTravelTimeModel) {
        super(measurementQuality, signalTravelTimeModel, new ObservableSatellite[] { local, remote });
        this.twoway = twoWay;
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatellitesRange buildObserved(final AbsoluteDate date,
                                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new InterSatellitesRange(getSatellites()[0], getSatellites()[1], twoway, date, Double.NaN,
                                        getMeasurementQuality(), getSignalTravelTimeModel());
    }

}
