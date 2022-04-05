/* Copyright 2002-2022 CS GROUP
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
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.BistaticRangeRate;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Class modifying theoretical bistatic range-rate measurement with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the bistatic range-rate is directly computed
 * through the computation of the ionospheric delay difference with respect to time.
 * </p><p>
 * The ionospheric delay depends on the frequency of the signal.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class BistaticRangeRateIonosphericDelayModifier implements EstimationModifier<BistaticRangeRate> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range-rate measurement method.
     * @param freq frequency of the signal in Hz
     */
    public BistaticRangeRateIonosphericDelayModifier(final IonosphericModel model,
                                                     final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Ionosphere
     */
    private double rangeRateErrorIonosphericModel(final GroundStation station, final SpacecraftState state) {
        final double dt = 10; // s
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final double delay1 = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters());
        // propagate spacecraft state forward by dt
        final SpacecraftState state2 = state.shiftedBy(dt);
        // ionospheric delay dt after in meters
        final double delay2 = ionoModel.pathDelay(state2, baseFrame, frequency, ionoModel.getParameters());
        // delay in meters per second
        return (delay2 - delay1) / dt;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param <T> type of the elements
     * @param station station
     * @param state spacecraft state
     * @param parameters ionospheric model parameters
     * @return the measurement error due to Ionosphere
     */
    private <T extends CalculusFieldElement<T>> T rangeRateErrorIonosphericModel(final GroundStation station,
                                                                                 final FieldSpacecraftState<T> state,
                                                                                 final T[] parameters) {
        final double dt = 10; // s
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final T delay1 = ionoModel.pathDelay(state, baseFrame, frequency, parameters);
        // propagate spacecraft state forward by dt
        final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);
        // ionospheric delay dt after in meters
        final T delay2 = ionoModel.pathDelay(state2, baseFrame, frequency, parameters);
        // delay in meters per second
        return delay2.subtract(delay1).divide(dt);
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] rangeRateErrorJacobianState(final double[] derivatives) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        System.arraycopy(derivatives, 0, finiteDifferencesJacobian[0], 0, 6);
        return finiteDifferencesJacobian;
    }

    /** Compute the derivative of the delay term wrt parameters.
    *
    * @param station ground station
    * @param driver driver for the station offset parameter
    * @param state spacecraft state
    * @return derivative of the delay wrt station offset parameter
    */
    private double rangeRateErrorParameterDerivative(final GroundStation station,
                                                     final ParameterDriver driver,
                                                     final SpacecraftState state) {

        final ParameterFunction rangeRateError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeRateErrorIonosphericModel(station, state);
            }
        };

        final ParameterFunction rangeRateErrorDerivative =
                        Differentiation.differentiate(rangeRateError, 3, 10.0 * driver.getScale());

        return rangeRateErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt ionospheric model parameters
    */
    private double[] rangeRateErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] rangeRateError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeRateError[i] = derivatives[freeStateParameters + i];
        }

        return rangeRateError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<BistaticRangeRate> estimated) {

        final BistaticRangeRate measurement = estimated.getObservedMeasurement();
        final GroundStation     emitter     = measurement.getEmitterStation();
        final GroundStation     receiver    = measurement.getReceiverStation();
        final SpacecraftState   state       = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final IonosphericGradientConverter converter =
                new IonosphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(ionoModel);
        final Gradient[] gParameters = converter.getParameters(gState, ionoModel);

        final Gradient delayUp = rangeRateErrorIonosphericModel(emitter, gState, gParameters);
        final double[] derivativesUp = delayUp.getGradient();

        final Gradient delayDown = rangeRateErrorIonosphericModel(receiver, gState, gParameters);
        final double[] derivativesDown = delayDown.getGradient();

        // update estimated derivatives with Jacobian of the measure wrt state
        final double[][] djacUp   = rangeRateErrorJacobianState(derivativesUp);
        final double[][] djacDown = rangeRateErrorJacobianState(derivativesDown);
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djacUp[irow][jcol];
                stateDerivatives[irow][jcol] += djacDown[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                double parameterDerivative  = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelayUpdP   = rangeRateErrorParameterDerivative(derivativesUp,
                                                                                converter.getFreeStateParameters());
                parameterDerivative += dDelayUpdP[index];
                final double[] dDelayDowndP = rangeRateErrorParameterDerivative(derivativesDown,
                                                                                converter.getFreeStateParameters());
                parameterDerivative += dDelayDowndP[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index++;
            }

        }

        for (final ParameterDriver driver : Arrays.asList(emitter.getEastOffsetDriver(),
                                                          emitter.getNorthOffsetDriver(),
                                                          emitter.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeRateErrorParameterDerivative(emitter, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        for (final ParameterDriver driver : Arrays.asList(receiver.getClockOffsetDriver(),
                                                          receiver.getEastOffsetDriver(),
                                                          receiver.getNorthOffsetDriver(),
                                                          receiver.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeRateErrorParameterDerivative(receiver, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // update estimated value taking into account the ionospheric delay.
        // The ionospheric delay is directly added to the measurement.
        final double[] newValue = oldValue.clone();
        newValue[0] += delayUp.getValue();
        newValue[0] += delayDown.getValue();
        estimated.setEstimatedValue(newValue);

    }

}
