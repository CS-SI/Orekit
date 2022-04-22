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

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

/** Base class modifying theoretical range-rate measurement with ionospheric delay.
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
 * @since 11.2
 */
public abstract class BaseRangeRateIonosphericDelayModifier {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range-rate measurement method.
     * @param freq frequency of the signal in Hz
     */
    protected BaseRangeRateIonosphericDelayModifier(final IonosphericModel model, final double freq) {
        this.ionoModel = model;
        this.frequency = freq;
    }

    /** Get the ionospheric delay model.
     * @return ionospheric delay model
     */
    protected IonosphericModel getIonoModel() {
        return ionoModel;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Ionosphere
     */
    protected double rangeRateErrorIonosphericModel(final GroundStation station, final SpacecraftState state) {
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
        return (delay2 - delay1) / dt;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param <T> type of the elements
     * @param station station
     * @param state spacecraft state
     * @param parameters ionospheric model parameters
     * @return the measurement error due to Ionosphere
     */
    protected <T extends CalculusFieldElement<T>> T rangeRateErrorIonosphericModel(final GroundStation station,
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
        return delay2.subtract(delay1).divide(dt);
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    *
    * @return Jacobian of the delay wrt state
    */
    protected double[][] rangeRateErrorJacobianState(final double[] derivatives) {
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
    protected double rangeRateErrorParameterDerivative(final GroundStation station,
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
    protected double[] rangeRateErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[freeStateParameters + i];
        }

        return rangeError;
    }

    /** Get the drivers for this modifier parameters.
     * @return drivers for this modifier parameters
     */
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

}
