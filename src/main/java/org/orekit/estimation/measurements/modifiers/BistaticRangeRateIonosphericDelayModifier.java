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

import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.BistaticRangeRate;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical bistatic range-rate measurement with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the bistatic range-rate is directly computed
 * through the computation of the ionospheric delay difference with respect to time.
 * </p><p>
 * The ionospheric delay depends on the frequency of the signal.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class BistaticRangeRateIonosphericDelayModifier extends BaseRangeRateIonosphericDelayModifier implements EstimationModifier<BistaticRangeRate> {

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range-rate measurement method.
     * @param freq frequency of the signal in Hz
     */
    public BistaticRangeRateIonosphericDelayModifier(final IonosphericModel model, final double freq) {
        super(model, freq);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<BistaticRangeRate> estimated) {

        final BistaticRangeRate measurement = estimated.getObservedMeasurement();
        final GroundStation     emitter     = measurement.getEmitterStation();
        final GroundStation     receiver    = measurement.getReceiverStation();

        BistaticModifierUtil.modify(estimated, emitter, receiver, this::rangeRateErrorIonosphericModel);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<BistaticRangeRate> estimated) {

        final BistaticRangeRate measurement = estimated.getObservedMeasurement();
        final GroundStation     emitter     = measurement.getEmitterStation();
        final GroundStation     receiver    = measurement.getReceiverStation();
        final SpacecraftState   state       = estimated.getStates()[0];

        BistaticModifierUtil.modify(estimated, getIonoModel(),
                                    new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                    emitter, receiver,
                                    this::rangeRateErrorIonosphericModel,
                                    this::rangeRateErrorIonosphericModel);

    }

}
