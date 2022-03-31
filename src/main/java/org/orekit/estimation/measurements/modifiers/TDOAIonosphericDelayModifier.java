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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Class modifying theoretical TDOA measurements with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the TDOA is a time delay computed
 * directly from the difference in ionospheric delays for each downlink.<br/>
 * The ionospheric delay depends on the frequency of the signal.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOAIonosphericDelayModifier implements EstimationModifier<TDOA> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model ionospheric model appropriate for the current TDOA measurement method
     * @param freq  frequency of the signal in Hz
     */
    public TDOAIonosphericDelayModifier(final IonosphericModel model,
                                        final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** Compute the measurement error due to ionosphere on a single downlink.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere (s)
     */
    private double timeErrorIonosphericModel(final GroundStation station,
                                             final SpacecraftState state) {
        // base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters());
        // return delay in seconds
        return delay / Constants.SPEED_OF_LIGHT;
    }

    /** Compute the measurement error due to ionosphere on a single downlink.
     * @param <T> type of the elements
     * @param station station
     * @param state spacecraft state
     * @param parameters ionospheric model parameters
     * @return the measurement error due to ionosphere (s)
     */
    private <T extends CalculusFieldElement<T>> T timeErrorIonosphericModel(final GroundStation station,
                                                                            final FieldSpacecraftState<T> state,
                                                                            final T[] parameters) {
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // Delay in meters
        final T delay = ionoModel.pathDelay(state, baseFrame, frequency, parameters);
        // return delay in seconds
        return delay.divide(Constants.SPEED_OF_LIGHT);
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] timeErrorJacobianState(final double[] derivatives) {
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
    private double timeErrorParameterDerivative(final GroundStation station,
                                                final ParameterDriver driver,
                                                final SpacecraftState state) {

        final ParameterFunction timeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return timeErrorIonosphericModel(station, state);
            }
        };

        final ParameterFunction timeErrorDerivative =
                        Differentiation.differentiate(timeError, 3, 10.0 * driver.getScale());

        return timeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt ionospheric model parameters
    */
    private double[] timeErrorParameterDerivative(final double[] derivatives,
                                                  final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] timeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            timeError[i] = derivatives[freeStateParameters + i];
        }

        return timeError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modify(final EstimatedMeasurement<TDOA> estimated) {
        final TDOA measurement              = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();
        final SpacecraftState state         = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final IonosphericGradientConverter converter =
                new IonosphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(ionoModel);
        final Gradient[] gParameters       = converter.getParameters(gState, ionoModel);
        final Gradient   primeGDelay       = timeErrorIonosphericModel(primeStation, gState, gParameters);
        final Gradient   secondGDelay      = timeErrorIonosphericModel(secondStation, gState, gParameters);
        final double[]   primeDerivatives  = primeGDelay.getGradient();
        final double[]   secondDerivatives = secondGDelay.getGradient();

        final double[][] primeDjac         = timeErrorJacobianState(primeDerivatives);
        final double[][] secondDjac        = timeErrorJacobianState(secondDerivatives);
        final double[][] stateDerivatives  = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] -= primeDjac[irow][jcol];
                stateDerivatives[irow][jcol] += secondDjac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelayPrime = timeErrorParameterDerivative(primeDerivatives,
                                                                          converter.getFreeStateParameters());
                parameterDerivative -= dDelayPrime[index];
                final double[] dDelaySecond = timeErrorParameterDerivative(secondDerivatives,
                                                                           converter.getFreeStateParameters());
                parameterDerivative += dDelaySecond[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index += 1;
            }

        }

        // Update derivatives with respect to primary station position
        for (final ParameterDriver driver : Arrays.asList(primeStation.getClockOffsetDriver(),
                                                          primeStation.getEastOffsetDriver(),
                                                          primeStation.getNorthOffsetDriver(),
                                                          primeStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative -= timeErrorParameterDerivative(primeStation, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // Update derivatives with respect to secondary station position
        for (final ParameterDriver driver : Arrays.asList(secondStation.getClockOffsetDriver(),
                                                          secondStation.getEastOffsetDriver(),
                                                          secondStation.getNorthOffsetDriver(),
                                                          secondStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += timeErrorParameterDerivative(secondStation, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // Update estimated value taking into account the ionospheric delay for each downlink.
        // The ionospheric time delay is directly applied to the TDOA.
        final double[] newValue = oldValue.clone();
        newValue[0] -= primeGDelay.getReal();
        newValue[0] += secondGDelay.getReal();
        estimated.setEstimatedValue(newValue);
    }

}
