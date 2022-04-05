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
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.BistaticRangeRate;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Class modifying theoretical bistatic range-rate measurements with tropospheric delay.
 * <p>
 * The effect of tropospheric correction on the bistatic range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to time.
 * <p></p>
 * Tropospheric delay is not frequency dependent for signals up to 15 GHz.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class BistaticRangeRateTroposphericDelayModifier implements EstimationModifier<BistaticRangeRate> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model Tropospheric delay model appropriate for the current range-rate measurement method.
     */
    public BistaticRangeRateTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.
        final double dt = 10; // s

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation1 = station.getBaseFrame().getElevation(position,
                                                                      state.getFrame(),
                                                                      state.getDate());

        // only consider measures above the horizon
        if (elevation1 > 0) {
            // tropospheric delay in meters
            final double d1 = tropoModel.pathDelay(elevation1, station.getBaseFrame().getPoint(),
                                                   tropoModel.getParameters(), state.getDate());

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();

            // elevation
            final double elevation2 = station.getBaseFrame().getElevation(position2,
                                                                          state2.getFrame(),
                                                                          state2.getDate());

            // tropospheric delay dt after
            final double d2 = tropoModel.pathDelay(elevation2, station.getBaseFrame().getPoint(),
                                                   tropoModel.getParameters(), state2.getDate());

            // delay in meters per second
            return (d2 - d1) / dt;
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
    public <T extends CalculusFieldElement<T>> T rangeRateErrorTroposphericModel(final GroundStation station,
                                                                                 final FieldSpacecraftState<T> state,
                                                                                 final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // spacecraft position and elevation as seen from the ground station
        final FieldVector3D<T> position = state.getPVCoordinates().getPosition();
        final T elevation1              = station.getBaseFrame().getElevation(position,
                                                                              state.getFrame(),
                                                                              state.getDate());

        // only consider measures above the horizon
        if (elevation1.getReal() > 0) {
            // tropospheric delay in meters
            final T d1 = tropoModel.pathDelay(elevation1, station.getBaseFrame().getPoint(field),
                                              parameters, state.getDate());

            // propagate spacecraft state forward by dt
            final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final FieldVector3D<T> position2     = state2.getPVCoordinates().getPosition();

            // elevation
            final T elevation2 = station.getBaseFrame().getElevation(position2,
                                                                     state2.getFrame(),
                                                                     state2.getDate());


            // tropospheric delay dt after
            final T d2 = tropoModel.pathDelay(elevation2, station.getBaseFrame().getPoint(field),
                                              parameters, state2.getDate());

            // delay in meters per second
            return (d2.subtract(d1)).divide(dt);
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
                return rangeRateErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeRateErrorDerivative =
                        Differentiation.differentiate(rangeRateError, 3, 10.0 * driver.getScale());

        return rangeRateErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] rangeRateErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt tropospheric parameters
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
        return tropoModel.getParametersDrivers();
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
        final TroposphericGradientConverter converter =
                new TroposphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(tropoModel);
        final Gradient[] gParameters = converter.getParameters(gState, tropoModel);

        final Gradient delayUp = rangeRateErrorTroposphericModel(emitter, gState, gParameters);
        final double[] derivativesUp = delayUp.getGradient();

        final Gradient delayDown = rangeRateErrorTroposphericModel(receiver, gState, gParameters);
        final double[] derivativesDown = delayDown.getGradient();

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
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
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

        // update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the measurement.
        final double[] newValue = oldValue.clone();
        newValue[0] += delayUp.getValue();
        newValue[0] += delayDown.getValue();
        estimated.setEstimatedValue(newValue);

    }

}
