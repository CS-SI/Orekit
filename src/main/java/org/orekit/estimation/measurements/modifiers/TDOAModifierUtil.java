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
package org.orekit.estimation.measurements.modifiers;

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeSpanMap.Span;

/** Utility class for TDOA measurements.
 * @author Pascal Parraud
 * @since 11.2
 */
class TDOAModifierUtil {

    /** Private constructor for utility class.*/
    private TDOAModifierUtil() {
        // not used
    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param primeStation prime station
     * @param secondStation second station
     * @param modelEffect model effect
     */
    public static <T extends ObservedMeasurement<T>> void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated,
                                                                                   final GroundStation primeStation,
                                                                                   final GroundStation secondStation,
                                                                                   final ParametricModelEffect modelEffect) {

        final SpacecraftState state       = estimated.getStates()[0];
        final double[]        oldValue    = estimated.getEstimatedValue();
        final double          primeDelay  = modelEffect.evaluate(primeStation, state);
        final double          secondDelay = modelEffect.evaluate(secondStation, state);

        // Update estimated value taking into account the ionospheric delay for each downlink.
        // The ionospheric time delay is directly applied to the TDOA.
        final double[] newValue = oldValue.clone();
        newValue[0] += primeDelay;
        newValue[0] -= secondDelay;
        estimated.setEstimatedValue(newValue);
    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param primeStation prime station
     * @param secondStation second station
     * @param converter gradient converter
     * @param parametricModel parametric modifier model
     * @param modelEffect model effect
     * @param modelEffectGradient model effect gradient
     */
    public static <T extends ObservedMeasurement<T>> void modify(final EstimatedMeasurement<T> estimated,
                                                                 final ParameterDriversProvider parametricModel,
                                                                 final AbstractGradientConverter converter,
                                                                 final GroundStation primeStation, final GroundStation secondStation,
                                                                 final ParametricModelEffect modelEffect,
                                                                 final ParametricModelEffectGradient modelEffectGradient) {

        final SpacecraftState state    = estimated.getStates()[0];
        final double[]        oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final FieldSpacecraftState<Gradient> gState = converter.getState(parametricModel);
        final Gradient[] gParameters       = converter.getParameters(gState, parametricModel);
        final Gradient   primeGDelay       = modelEffectGradient.evaluate(primeStation, gState, gParameters);
        final Gradient   secondGDelay      = modelEffectGradient.evaluate(secondStation, gState, gParameters);
        final double[]   primeDerivatives  = primeGDelay.getGradient();
        final double[]   secondDerivatives = secondGDelay.getGradient();

        final double[][] stateDerivatives  = estimated.getStateDerivatives(0);
        for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
            stateDerivatives[0][jcol] += primeDerivatives[jcol];
            stateDerivatives[0][jcol] -= secondDerivatives[jcol];
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : parametricModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += primeDerivatives[index + converter.getFreeStateParameters()];
                    parameterDerivative -= secondDerivatives[index + converter.getFreeStateParameters()];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    index += 1;
                }
            }

        }

        // Update derivatives with respect to primary station position
        for (final ParameterDriver driver : Arrays.asList(primeStation.getClockOffsetDriver(),
                                                          primeStation.getEastOffsetDriver(),
                                                          primeStation.getNorthOffsetDriver(),
                                                          primeStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += Differentiation.differentiate((d, t) -> modelEffect.evaluate(primeStation, state),
                                                                     3, 10.0 * driver.getScale()).value(driver, state.getDate());
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update derivatives with respect to secondary station position
        for (final ParameterDriver driver : Arrays.asList(secondStation.getClockOffsetDriver(),
                                                          secondStation.getEastOffsetDriver(),
                                                          secondStation.getNorthOffsetDriver(),
                                                          secondStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative -= Differentiation.differentiate((d, t) -> modelEffect.evaluate(secondStation, state),
                                                                     3, 10.0 * driver.getScale()).value(driver, state.getDate());
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update estimated value taking into account the ionospheric delay for each downlink.
        // The ionospheric time delay is directly applied to the TDOA.
        final double[] newValue = oldValue.clone();
        newValue[0] += primeGDelay.getReal();
        newValue[0] -= secondGDelay.getReal();
        estimated.setEstimatedValue(newValue);
    }

}
