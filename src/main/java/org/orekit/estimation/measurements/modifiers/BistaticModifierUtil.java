/* Copyright 2002-2024 CS GROUP
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

/** Utility class for bistatic measurements.
 * @author Pascal Parraud
 * @since 11.2
 */
class BistaticModifierUtil {

    /** Private constructor for utility class.*/
    private BistaticModifierUtil() {
        // not used
    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param emitter emitter station
     * @param receiver receiver station
     * @param modelEffect model effect
     */
    public static <T extends ObservedMeasurement<T>> void modify(final EstimatedMeasurementBase<T> estimated,
                                                                 final GroundStation emitter, final GroundStation receiver,
                                                                 final ParametricModelEffect modelEffect) {

        // update estimated value taking into account the model effect.
        // The model effect delay is directly added to the measurement.
        final SpacecraftState state    = estimated.getStates()[0];
        final double[]        newValue = estimated.getEstimatedValue().clone();
        newValue[0] += modelEffect.evaluate(emitter, state);
        newValue[0] += modelEffect.evaluate(receiver, state);
        estimated.setEstimatedValue(newValue);

    }

    /** Apply a modifier to an estimated measurement.
     * @param <T> type of the measurement
     * @param estimated estimated measurement to modify
     * @param emitter emitter station
     * @param receiver receiver station
     * @param converter gradient converter
     * @param parametricModel parametric modifier model
     * @param modelEffect model effect
     * @param modelEffectGradient model effect gradient
     */
    public static <T extends ObservedMeasurement<T>> void modify(final EstimatedMeasurement<T> estimated,
                                                                 final ParameterDriversProvider parametricModel,
                                                                 final AbstractGradientConverter converter,
                                                                 final GroundStation emitter, final GroundStation receiver,
                                                                 final ParametricModelEffect modelEffect,
                                                                 final ParametricModelEffectGradient modelEffectGradient) {

        final SpacecraftState state    = estimated.getStates()[0];

        // update estimated derivatives with Jacobian of the measure wrt state
        final FieldSpacecraftState<Gradient> gState = converter.getState(parametricModel);
        final Gradient[] gParameters = converter.getParameters(gState, parametricModel);

        final Gradient delayUp = modelEffectGradient.evaluate(emitter, gState, gParameters);
        final double[] derivativesUp = delayUp.getGradient();

        final Gradient delayDown = modelEffectGradient.evaluate(receiver, gState, gParameters);
        final double[] derivativesDown = delayDown.getGradient();

        // update estimated derivatives with Jacobian of the measure wrt state
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
            stateDerivatives[0][jcol] += derivativesUp[jcol];
            stateDerivatives[0][jcol] += derivativesDown[jcol];
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : parametricModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    // update estimated derivatives with derivative of the modification wrt model parameters
                    double parameterDerivative  = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += derivativesUp[index + converter.getFreeStateParameters()];
                    parameterDerivative += derivativesDown[index + converter.getFreeStateParameters()];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    index++;
                }
            }

        }

        for (final ParameterDriver driver : Arrays.asList(emitter.getEastOffsetDriver(),
                                                          emitter.getNorthOffsetDriver(),
                                                          emitter.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    // update estimated derivatives with derivative of the modification wrt station parameters
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += Differentiation.differentiate((d, t) -> modelEffect.evaluate(emitter, state),
                                                                         3, 10.0 * driver.getScale()).value(driver, state.getDate());
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        for (final ParameterDriver driver : Arrays.asList(receiver.getClockOffsetDriver(),
                                                          receiver.getEastOffsetDriver(),
                                                          receiver.getNorthOffsetDriver(),
                                                          receiver.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    // update estimated derivatives with derivative of the modification wrt station parameters
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += Differentiation.differentiate((d, t) -> modelEffect.evaluate(receiver, state),
                                                                         3, 10.0 * driver.getScale()).value(driver, state.getDate());
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // modify the value
        modify(estimated, emitter, receiver, modelEffect);

    }

}
