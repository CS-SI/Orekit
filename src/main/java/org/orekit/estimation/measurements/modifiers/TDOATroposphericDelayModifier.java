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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Class modifying theoretical TDOA measurements with tropospheric delay.
 * <p>
 * The effect of tropospheric correction on the TDOA is a time delay computed
 * directly from the difference in tropospheric delays for each downlink.<br/>
 * Tropospheric delay is not frequency dependent for signals up to 15 GHz.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOATroposphericDelayModifier implements EstimationModifier<TDOA> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model tropospheric model appropriate for the current TDOA measurement method.
     */
    public TDOATroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere (s)
     */
    private double timeErrorTroposphericModel(final GroundStation station, final SpacecraftState state) {
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

        // only consider measurements above the horizon
        if (elevation > 0) {
            // Delay in meters
            final double delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(),
                                                      tropoModel.getParameters(), state.getDate());
            // return delay in seconds
            return delay / Constants.SPEED_OF_LIGHT;
        }

        return 0;
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere (s)
     */
    private <T extends CalculusFieldElement<T>> T timeErrorTroposphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // elevation
        final FieldVector3D<T> pos = state.getPVCoordinates().getPosition();
        final T elevation          = station.getBaseFrame().getElevation(pos,
                                                                         state.getFrame(),
                                                                         state.getDate());

        // only consider measurements above the horizon
        if (elevation.getReal() > 0) {
            // delay in meters
            final T delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(field),
                                                 parameters, state.getDate());
            // return delay in seconds
            return delay.divide(Constants.SPEED_OF_LIGHT);
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
                return timeErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction timeErrorDerivative =
                        Differentiation.differentiate(timeError, 3, 10.0 * driver.getScale());

        return timeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] timeErrorParameterDerivative(final double[] derivatives,
                                                  final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt tropospheric parameters
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
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<TDOA> estimated) {
        final TDOA            measurement   = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();
        final SpacecraftState state         = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final TroposphericGradientConverter converter =
                new TroposphericGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(tropoModel);
        final Gradient[] gParameters       = converter.getParameters(gState, tropoModel);
        final Gradient   primeGDelay       = timeErrorTroposphericModel(primeStation, gState, gParameters);
        final Gradient   secondGDelay      = timeErrorTroposphericModel(secondStation, gState, gParameters);
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
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
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

        // Update derivatives with respect to prime station position
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

        // Update derivatives with respect to second station position
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

        // Update estimated value taking into account the tropospheric delay for each downlink.
        // The tropospheric time delay is directly applied to the TDOA.
        final double[] newValue = oldValue.clone();
        newValue[0] -= primeGDelay.getReal();
        newValue[0] += secondGDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
