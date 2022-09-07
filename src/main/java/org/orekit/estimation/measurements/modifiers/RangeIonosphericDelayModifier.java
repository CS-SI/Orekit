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
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Range;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range measurement with ionospheric delay.
 * The effect of ionospheric correction on the range is directly computed
 * through the computation of the ionospheric delay.
 *
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivates are computed
 * using automatic differentiation.
 * </p>
 * @author Maxime Journot
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeIonosphericDelayModifier implements EstimationModifier<Range> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range measurement method.
     * @param freq frequency of the signal in Hz
     */
    public RangeIonosphericDelayModifier(final IonosphericModel model,
                                         final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** Compute the measurement error due to ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere
     */
    private double rangeErrorIonosphericModel(final GroundStation station,
                                              final SpacecraftState state) {
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters(state.getDate()));
        return delay;
    }

    /** Compute the measurement error due to ionosphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modify(final EstimatedMeasurement<Range> estimated) {

        final Range           measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        RangeModifierUtil.modify(estimated, ionoModel,
                                 new ModifierGradientConverter(state, 6, new InertialProvider(state.getFrame())),
                                 station,
                                 this::rangeErrorIonosphericModel,
                                 this::rangeErrorIonosphericModel);

    }

}
