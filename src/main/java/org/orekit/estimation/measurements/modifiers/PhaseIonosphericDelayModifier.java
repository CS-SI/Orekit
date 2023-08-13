/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.TimeSpanMap.Span;

/**
 * Class modifying theoretical phase measurement with ionospheric delay.
 * The effect of ionospheric correction on the phase is directly computed
 * through the computation of the ionospheric delay.
 * @author David Soulard
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class PhaseIonosphericDelayModifier implements EstimationModifier<Phase> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model  Ionospheric delay model appropriate for the current range measurement method.
     * @param freq frequency of the signal in Hz
     */
    public PhaseIonosphericDelayModifier(final IonosphericModel model,
                                         final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** Compute the measurement error due to ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere
     */
    private double phaseErrorIonosphericModel(final GroundStation station,
                                              final SpacecraftState state) {

        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        final double wavelength  = Constants.SPEED_OF_LIGHT / frequency;
        // delay in meters
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters(state.getDate()));
        return delay / wavelength;
    }

    /** Compute the measurement error due to ionosphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters ionospheric model parameters at state date
     * @return the measurement error due to ionosphere
     */
    private <T extends CalculusFieldElement<T>> T phaseErrorIonosphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {

        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        final double wavelength  = Constants.SPEED_OF_LIGHT / frequency;
        // delay in meters
        final T delay = ionoModel.pathDelay(state, baseFrame, frequency, parameters);
        return delay.divide(wavelength);
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] phaseErrorJacobianState(final double[] derivatives, final int freeStateParameters) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        for (int i = 0; i < freeStateParameters; i++) {
            finiteDifferencesJacobian[0][i] = derivatives[i];
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
    private double phaseErrorParameterDerivative(final GroundStation station,
                                                 final ParameterDriver driver,
                                                 final SpacecraftState state) {
        final ParameterFunction phaseError = (parameterDriver, date) -> phaseErrorIonosphericModel(station, state);
        final ParameterFunction phaseErrorDerivative =
                        Differentiation.differentiate(phaseError, 3, 10.0 * driver.getScale());
        return phaseErrorDerivative.value(driver, state.getDate());

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives ionospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt ionospheric model parameters
    */
    private double[] phaseErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt ionospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] phaseError = new double[dim];

        for (int i = 0; i < dim; i++) {
            phaseError[i] = derivatives[freeStateParameters + i];
        }

        return phaseError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Phase> estimated) {

        final Phase           measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        // Update estimated value taking into account the ionospheric delay.
        // The ionospheric delay is directly subtracted to the phase.
        final double[] newValue = estimated.getEstimatedValue();
        final double delay = phaseErrorIonosphericModel(station, state);
        newValue[0] = newValue[0] - delay;
        estimated.setEstimatedValue(newValue);

    }

    @Override
    public void modify(final EstimatedMeasurement<Phase> estimated) {

        final Phase           measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        // Compute ionospheric delay (the division by the wavelength is performed)
        final ModifierGradientConverter converter =
                        new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(ionoModel);
        final Gradient[] gParameters = converter.getParametersAtStateDate(gState, ionoModel);
        final Gradient gDelay = phaseErrorIonosphericModel(station, gState, gParameters);
        final double[] derivatives = gDelay.getGradient();

        // Update state derivatives
        final double[][] djac = phaseErrorJacobianState(derivatives, converter.getFreeStateParameters());
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] -= djac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        // Update ionospheric parameter derivatives
        int index = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    // update estimated derivatives with derivative of the modification wrt ionospheric parameters
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    final double[] dDelaydP    = phaseErrorParameterDerivative(derivatives, converter.getFreeStateParameters());
                    parameterDerivative -= dDelaydP[index];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    index = index + 1;
                }
            }

        }

        // Update station parameter derivatives
        for (final ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                          station.getEastOffsetDriver(),
                                                          station.getNorthOffsetDriver(),
                                                          station.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    // update estimated derivatives with derivative of the modification wrt station parameters
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative -= phaseErrorParameterDerivative(station, driver, state);
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update estimated value taking into account the ionospheric delay.
        modifyWithoutDerivatives(estimated);

    }

}

