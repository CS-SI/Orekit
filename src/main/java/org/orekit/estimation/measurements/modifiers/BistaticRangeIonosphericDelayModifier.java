/* Copyright 2002-2022 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.BistaticRange;
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

import java.util.Arrays;
import java.util.List;

/**
 * Class modifying theoretical bistatic range measurement with ionospheric delay.
 * The effect of ionospheric correction on the range is directly computed
 * through the computation of the ionospheric delay.
 * <p>
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivates are computed
 * using automatic differentiation.
 * </p>
 *
 * @author Maxime Journot
 * @author Joris Olympio
 * @author Mark Rutten
 * @since 11.2
 */
public class BistaticRangeIonosphericDelayModifier implements EstimationModifier<BistaticRange> {

    /**
     * Ionospheric delay model.
     */
    private final IonosphericModel ionoModel;

    /**
     * Frequency [Hz].
     */
    private final double frequency;

    /**
     * Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range measurement method.
     * @param freq  frequency of the signal in Hz
     */
    public BistaticRangeIonosphericDelayModifier(final IonosphericModel model,
                                                 final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /**
     * Compute the measurement error due to ionosphere.
     *
     * @param station station
     * @param state   spacecraft state
     * @return the measurement error due to ionosphere
     */
    private double rangeErrorIonosphericModel(final GroundStation station,
                                              final SpacecraftState state) {
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters());
        return delay;
    }

    /**
     * Compute the measurement error due to ionosphere.
     *
     * @param <T>        type of the element
     * @param station    station
     * @param state      spacecraft state
     * @param parameters ionospheric model parameters
     * @return the measurement error due to ionosphere
     */
    private <T extends CalculusFieldElement<T>> T rangeErrorIonosphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final T delay = ionoModel.pathDelay(state, baseFrame, frequency, parameters);
        return delay;
    }

    /**
     * Compute the Jacobian of the delay term wrt state using
     * automatic differentiation.
     *
     * @param derivatives ionospheric delay derivatives
     * @return Jacobian of the delay wrt state
     */
    private double[][] rangeErrorJacobianState(final double[] derivatives) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        System.arraycopy(derivatives, 0, finiteDifferencesJacobian[0], 0, 6);
        return finiteDifferencesJacobian;
    }


    /**
     * Compute the derivative of the delay term wrt parameters.
     *
     * @param station ground station
     * @param driver  driver for the station offset parameter
     * @param state   spacecraft state
     * @param delay   current ionospheric delay
     * @return derivative of the delay wrt station offset parameter
     */
    private double rangeErrorParameterDerivative(final GroundStation station,
                                                 final ParameterDriver driver,
                                                 final SpacecraftState state,
                                                 final double delay) {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeErrorIonosphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative =
                Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver);

    }

    /**
     * Compute the derivative of the delay term wrt parameters using
     * automatic differentiation.
     *
     * @param derivatives         ionospheric delay derivatives
     * @param freeStateParameters dimension of the state.
     * @return derivative of the delay wrt ionospheric model parameters
     */
    private double[] rangeErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[freeStateParameters + i];
        }

        return rangeError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modify(final EstimatedMeasurement<BistaticRange> estimated) {
        final BistaticRange measurement = estimated.getObservedMeasurement();
        final GroundStation receiver = measurement.getReceiverStation();
        final GroundStation transmitter = measurement.getEmitterStation();
        final SpacecraftState state = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final IonosphericGradientConverter converter =
                new IonosphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(ionoModel);
        final Gradient[] gParameters = converter.getParameters(gState, ionoModel);
        final Gradient gReceiverDelay = rangeErrorIonosphericModel(receiver, gState, gParameters);
        final Gradient gTransmitterDelay = rangeErrorIonosphericModel(transmitter, gState, gParameters);
        final double[] derivReceiver = gReceiverDelay.getGradient();
        final double[] derivTransmitter = gTransmitterDelay.getGradient();

        final double[][] djacReceiver = rangeErrorJacobianState(derivReceiver);
        final double[][] djacTransmitter = rangeErrorJacobianState(derivTransmitter);

        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djacReceiver[irow][jcol] + djacTransmitter[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelaydPReceiver = rangeErrorParameterDerivative(derivReceiver, converter.getFreeStateParameters());
                final double[] dDelaydPTransmitter = rangeErrorParameterDerivative(derivTransmitter, converter.getFreeStateParameters());
                parameterDerivative += dDelaydPReceiver[index] + dDelaydPTransmitter[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index = index + 1;
            }

        }

        for (final ParameterDriver driver : Arrays.asList(receiver.getClockOffsetDriver(),
                receiver.getEastOffsetDriver(),
                receiver.getNorthOffsetDriver(),
                receiver.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeErrorParameterDerivative(receiver, driver, state, gReceiverDelay.getValue());
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        for (final ParameterDriver driver : Arrays.asList(transmitter.getClockOffsetDriver(),
                transmitter.getEastOffsetDriver(),
                transmitter.getNorthOffsetDriver(),
                transmitter.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeErrorParameterDerivative(transmitter, driver, state, gTransmitterDelay.getValue());
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }


        // update estimated value taking into account the ionospheric delay.
        // The ionospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + gReceiverDelay.getValue() + gTransmitterDelay.getValue();
        estimated.setEstimatedValue(newValue);

    }

}
