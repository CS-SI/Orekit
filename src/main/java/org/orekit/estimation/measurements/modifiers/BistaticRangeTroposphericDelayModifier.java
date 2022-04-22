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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.*;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

import java.util.Arrays;
import java.util.List;

/** Class modifying theoretical bistatic range measurement with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Maxime Journot
 * @author Joris Olympio
 * @author Mark Rutten
 * @since 11.2
 */
public class BistaticRangeTroposphericDelayModifier implements EstimationModifier<BistaticRange> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range measurement method.
     */
    public BistaticRangeTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    private double rangeErrorTroposphericModel(final GroundStation station, final SpacecraftState state) {
        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

        // only consider measures above the horizon
        if (elevation > 0) {
            // delay in meters
            final double delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(), tropoModel.getParameters(), state.getDate());

            return delay;
        }

        return 0;
    }

    /** Compute the measurement error due to Troposphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    private <T extends CalculusFieldElement<T>> T rangeErrorTroposphericModel(final GroundStation station,
                                                                          final FieldSpacecraftState<T> state,
                                                                          final T[] parameters) {

        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // satellite elevation
        final FieldVector3D<T> position     = state.getPVCoordinates().getPosition();
        final T elevation                   = station.getBaseFrame().getElevation(position,
                                                                                  state.getFrame(),
                                                                                  state.getDate());


        // only consider measures above the horizon
        if (elevation.getReal() > 0) {
            // delay in meters
            final T delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(field), parameters, state.getDate());

            return delay;
        }

        return zero;
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] rangeErrorJacobianState(final double[] derivatives) {
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
    private double rangeErrorParameterDerivative(final GroundStation station,
                                                 final ParameterDriver driver,
                                                 final SpacecraftState state) {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative =
                        Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] rangeErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt tropospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[freeStateParameters + i];
        }

        return rangeError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<BistaticRange> estimated) {
        final BistaticRange   measurement = estimated.getObservedMeasurement();
        final GroundStation receiver = measurement.getReceiverStation();
        final GroundStation transmitter = measurement.getEmitterStation();
        final SpacecraftState state       = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final TroposphericGradientConverter converter =
                new TroposphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(tropoModel);
        final Gradient[] gParameters = converter.getParameters(gState, tropoModel);
        final Gradient gReceiverDelay = rangeErrorTroposphericModel(receiver, gState, gParameters);
        final Gradient gTransmitterDelay = rangeErrorTroposphericModel(transmitter, gState, gParameters);
        final double[] derivReceiver = gReceiverDelay.getGradient();
        final double[] derivTransmitter = gTransmitterDelay.getGradient();

        final double[][] djacReceiver = rangeErrorJacobianState(derivReceiver);
        final double[][] djacTransmitter = rangeErrorJacobianState(derivTransmitter);

        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djacTransmitter[irow][jcol] + djacReceiver[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelaydPReceiver = rangeErrorParameterDerivative(derivReceiver, converter.getFreeStateParameters());
                final double[] dDelaydPTransmitter = rangeErrorParameterDerivative(derivTransmitter, converter.getFreeStateParameters());
                parameterDerivative += dDelaydPTransmitter[index] + dDelaydPReceiver[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index = index + 1;
            }

        }

        for (final ParameterDriver driver : Arrays.asList(transmitter.getClockOffsetDriver(),
                                                          transmitter.getEastOffsetDriver(),
                                                          transmitter.getNorthOffsetDriver(),
                                                          transmitter.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeErrorParameterDerivative(transmitter, driver, state);
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
                parameterDerivative += rangeErrorParameterDerivative(receiver, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + gTransmitterDelay.getReal() + gReceiverDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
