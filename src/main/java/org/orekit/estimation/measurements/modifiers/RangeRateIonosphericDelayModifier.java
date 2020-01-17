/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Class modifying theoretical range-rate measurement with ionospheric delay.
 * The effect of ionospheric correction on the range-rate is directly computed
 * through the computation of the ionospheric delay difference with respect to
 * time.
 *
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivates are computed
 * using automatic differentiation.
 * </p>
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRateIonosphericDelayModifier implements EstimationModifier<RangeRate> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Coefficient for measurment configuration (one-way, two-way). */
    private final double fTwoWay;

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range-rate measurement method.
     * @param freq frequency of the signal in Hz
     * @param twoWay Flag indicating whether the measurement is two-way.
     */
    public RangeRateIonosphericDelayModifier(final IonosphericModel model,
                                             final double freq, final boolean twoWay) {
        ionoModel = model;
        frequency = freq;

        if (twoWay) {
            fTwoWay = 2.;
        } else {
            fTwoWay = 1.;
        }
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
        // delay in meters
        return fTwoWay * (delay2 - delay1) / dt;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param <T> type of the elements
     * @param station station
     * @param state spacecraft state
     * @param parameters ionospheric model parameters
     * @return the measurement error due to Ionosphere
     */
    private <T extends RealFieldElement<T>> T rangeRateErrorIonosphericModel(final GroundStation station,
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
        // delay in meters
        return delay2.subtract(delay1).divide(dt).multiply(fTwoWay);
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] rangeRateErrorJacobianState(final double[] derivatives, final int freeStateParameters) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        for (int i = 0; i < freeStateParameters; i++) {
            // First element is the value of the delay
            finiteDifferencesJacobian[0][i] = derivatives[i + 1];
        }
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

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeRateErrorIonosphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative =
                        Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt ionospheric model parameters
    */
    private double[] rangeRateErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0                               -> value of the delay
        // 1 ... freeStateParameters       -> derivatives of the delay wrt state
        // freeStateParameters + 1 ... n   -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - 1 - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[1 + freeStateParameters + i];
        }

        return rangeError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<RangeRate> estimated) {

        final RangeRate       measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final IonosphericDSConverter converter =
                new IonosphericDSConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(ionoModel);
        final DerivativeStructure[] dsParameters = converter.getParameters(dsState, ionoModel);
        final DerivativeStructure dsDelay = rangeRateErrorIonosphericModel(station, dsState, dsParameters);
        final double[] derivatives = dsDelay.getAllDerivatives();

        // update estimated derivatives with Jacobian of the measure wrt state
        final double[][] djac = rangeRateErrorJacobianState(derivatives, converter.getFreeStateParameters());
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelaydP    = rangeRateErrorParameterDerivative(derivatives, converter.getFreeStateParameters());
                parameterDerivative += dDelaydP[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index = index + 1;
            }

        }

        for (final ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                          station.getEastOffsetDriver(),
                                                          station.getNorthOffsetDriver(),
                                                          station.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeRateErrorParameterDerivative(station, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // update estimated value taking into account the ionospheric delay.
        // The ionospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + dsDelay.getValue();
        estimated.setEstimatedValue(newValue);

    }

}
