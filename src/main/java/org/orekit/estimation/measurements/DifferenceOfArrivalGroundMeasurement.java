/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements;

import java.util.Arrays;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Base class modeling a measurement where signal from same wave is received by two different stations.
 * @author Romain Serra
 * @since 14.0
 */
abstract class DifferenceOfArrivalGroundMeasurement<T extends ObservedMeasurement<T>> extends GroundReceiverMeasurement<T> {

    /** Ground station that receives signal from satellite. */
    private final GroundStation secondStation;

    /** Simple constructor for scalar measurements.
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation second station
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param observedValue observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    protected DifferenceOfArrivalGroundMeasurement(final GroundStation primeStation, final GroundStation secondStation,
                                                   final AbsoluteDate date, final double observedValue,
                                                   final double sigma, final double baseWeight,
                                                   final SignalTravelTimeModel signalTravelTimeModel,
                                                   final ObservableSatellite satellite) {
        super(primeStation, false, date, new double[] {observedValue}, new double[] {sigma},
                new double[] {baseWeight}, signalTravelTimeModel, satellite);

        addParametersDrivers(secondStation.getParametersDrivers());

        this.secondStation = secondStation;
    }

    /** Get the ground station that receives the signal.
     * @return ground station
     */
    public final GroundStation getPrimeStation() {
        return getReceiverStation();
    }


    /** Get the second ground station, the one that gives the measurement.
     * @return second ground station
     */
    public GroundStation getSecondStation() {
        return secondStation;
    }

    /**
     * Fill estimated measurements with value and derivatives.
     * @param quantity estimated quantity
     * @param paramIndices indices mapping parameter names to derivative indices
     * @param estimated theoretical measurement class
     */
    protected void fillEstimation(final Gradient quantity, final Map<String, Integer> paramIndices,
                                  final EstimatedMeasurement<T> estimated) {
        estimated.setEstimatedValue(quantity.getValue());

        // First order derivatives with respect to state
        final double[] derivatives = quantity.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }
    }
}
