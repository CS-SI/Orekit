/* Copyright 2002-2023 CS GROUP
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
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical TDOA measurements with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the TDOA is a time delay computed
 * directly from the difference in ionospheric delays for each downlink.
 * </p><p>
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
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters(state.getDate()));
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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TDOA> estimated) {

        final TDOA measurement              = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();

        TDOAModifierUtil.modifyWithoutDerivatives(estimated,  primeStation, secondStation, this::timeErrorIonosphericModel);

    }

    @Override
    public void modify(final EstimatedMeasurement<TDOA> estimated) {

        final TDOA measurement              = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();
        final SpacecraftState state         = estimated.getStates()[0];

        TDOAModifierUtil.modify(estimated, ionoModel,
                                new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                primeStation, secondStation,
                                this::timeErrorIonosphericModel,
                                this::timeErrorIonosphericModel);

    }

}
