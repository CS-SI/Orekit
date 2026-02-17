/* Copyright 2022-2026 RomainSerra
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Abstract class for one-way GNSS, scalar measurement.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractOneWayGNSS<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Observer sending measurement data. */
    private final Observer observer;

    /** Simple constructor.
     * @param observer sender of GNSS signal
     * @param date date of the measurement
     * @param observedValue observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel time delay computer
     * @param local satellite which receives the signal and perform the measurement
     */
    protected AbstractOneWayGNSS(final Observer observer, final AbsoluteDate date,
                                 final double observedValue, final double sigma, final double baseWeight,
                                 final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite local) {
        // Call super constructor
        super(date, false, new double[] {observedValue}, new double[] {sigma}, new double[] {baseWeight},
                signalTravelTimeModel, Collections.singletonList(local));
        this.observer = observer;
    }

    /** Observer object sending signal.
     * @return observer object
     */
    public final Observer getObserver() {
        return observer;
    }

    /**
     * Method filling estimated measurement.
     * @param observedValue theoretical value with automatic differentiation
     * @param indices mapping between parameter name and variable index
     * @param estimated object to fill
     */
    protected void fillDerivatives(final Gradient observedValue, final Map<String, Integer> indices,
                                   final EstimatedMeasurement<T> estimated) {
        final double[] derivatives = observedValue.getGradient();

        // Set value and state first order derivatives of the estimated measurement
        estimated.setEstimatedValue(observedValue.getValue());
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver measurementDriver : getParametersDrivers()) {
            for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(measurementDriver, span.getStart(), derivatives[index]);
                }
            }
        }

    }

}
