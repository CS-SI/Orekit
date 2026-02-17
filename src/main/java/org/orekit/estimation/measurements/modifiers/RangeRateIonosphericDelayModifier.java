/* Copyright 2002-2026 CS GROUP
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
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range-rate measurement with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the range-rate is directly computed
 * through the computation of the ionospheric delay difference with respect to
 * time.
 * </p>
 * <p>
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * </p>
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivates are computed
 * using automatic differentiation.
 * </p>
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRateIonosphericDelayModifier extends BaseRangeRateIonosphericDelayModifier implements EstimationModifier<RangeRate> {

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
        super(model, freq);

        if (twoWay) {
            fTwoWay = 2.;
        } else {
            fTwoWay = 1.;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double rangeRateErrorIonosphericModel(final Observer observer, final SpacecraftState state) {
        return fTwoWay * super.rangeRateErrorIonosphericModel(observer, state);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T rangeRateErrorIonosphericModel(final Observer observer,
                                                                                   final FieldSpacecraftState<T> state,
                                                                                   final T[] parameters) {
        return super.rangeRateErrorIonosphericModel(observer, state, parameters).multiply(fTwoWay);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<RangeRate> estimated) {

        final RangeRate  measurement = estimated.getObservedMeasurement();
        final Observer   observer    = measurement.getObserver();

        RangeModifierUtil.modifyWithoutDerivatives(estimated, observer,
                                                   this::rangeRateErrorIonosphericModel,
                                                   this);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<RangeRate> estimated) {

        final RangeRate       measurement = estimated.getObservedMeasurement();
        final Observer        observer    = measurement.getObserver();
        final SpacecraftState state       = estimated.getStates()[0];

        RangeModifierUtil.modify(estimated, getIonoModel(),
                                 new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                 observer,
                                 this::rangeRateErrorIonosphericModel,
                                 this::rangeRateErrorIonosphericModel,
                                 this);


    }

}
