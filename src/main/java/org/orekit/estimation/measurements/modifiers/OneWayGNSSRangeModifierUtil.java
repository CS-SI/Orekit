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
package org.orekit.estimation.measurements.modifiers;

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Observer;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.AbstractGradientConverter;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeSpanMap.Span;

/** Utility class modifying theoretical range measurement.
 * @author Maxime Journot
 * @author Joris Olympio
 * @since 11.2
 */
public class OneWayGNSSRangeModifierUtil {

    /** Private constructor for utility class.*/
    private OneWayGNSSRangeModifierUtil() {
        // not used
    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param gnssSatellite GNSS satellite signal sender
     * @param modelEffect model effect
     * @param modifier applied modifier
     * @since 12.1
     */
    public static <T extends ObservedMeasurement<T>> void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated,
                                                                                   final Observer gnssSatellite,
                                                                                   final ParametricModelEffect modelEffect,
                                                                                   final EstimationModifier<T> modifier) {

        final SpacecraftState state    = estimated.getStates()[0];
        final double[]        oldValue = estimated.getEstimatedValue();

        // update estimated value taking into account the delay. The delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        final double delay = modelEffect.evaluate(gnssSatellite, state);
        newValue[0] = newValue[0] + delay;
        estimated.modifyEstimatedValue(modifier, newValue);

    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param gnssSatellite GNSS satellite signal sender
     * @param converter gradient converter
     * @param parametricModel parametric modifier model
     * @param modelEffect model effect
     * @param modelEffectGradient model effect gradient
     * @param modifier applied modifier
     */
    public static <T extends ObservedMeasurement<T>> void modify(final EstimatedMeasurement<T> estimated,
                                                                 final ParameterDriversProvider parametricModel,
                                                                 final AbstractGradientConverter converter,
                                                                 final Observer gnssSatellite,
                                                                 final ParametricModelEffect modelEffect,
                                                                 final ParametricModelEffectGradient modelEffectGradient,
                                                                 final EstimationModifier<T> modifier) {

        final SpacecraftState state = estimated.getStates()[0];

        // update estimated derivatives with Jacobian of the measure wrt state
        final FieldSpacecraftState<Gradient> gState = converter.getState(parametricModel);
        final Gradient[] gParameters = converter.getParameters(gState, parametricModel);
        final Gradient gDelay = modelEffectGradient.evaluate(gnssSatellite, gState, gParameters);
        final double[] derivatives = gDelay.getGradient();

        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
            stateDerivatives[0][jcol] += derivatives[jcol];
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : parametricModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt modifier parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += derivatives[index + converter.getFreeStateParameters()];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    index = index + 1;
                }
            }

        }

        for (final ParameterDriver driver : Arrays.asList(gnssSatellite.getClockBiasDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += Differentiation.differentiate((d, t) -> modelEffect.evaluate(gnssSatellite, state),
                                                                     3, 10.0 * driver.getScale()).value(driver, state.getDate());
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // update estimated value taking into account the delay
        modifyWithoutDerivatives(estimated, gnssSatellite, modelEffect, modifier);

    }

}
