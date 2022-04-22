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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

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
    protected double rangeRateErrorIonosphericModel(final GroundStation station, final SpacecraftState state) {
        return fTwoWay * super.rangeRateErrorIonosphericModel(station, state);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T rangeRateErrorIonosphericModel(final GroundStation station,
                                                                                   final FieldSpacecraftState<T> state,
                                                                                   final T[] parameters) {
        return super.rangeRateErrorIonosphericModel(station, state, parameters).multiply(fTwoWay);
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<RangeRate> estimated) {

        final RangeRate       measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final ModifierGradientConverter converter =
                new ModifierGradientConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(getIonoModel());
        final Gradient[] gParameters = converter.getParameters(gState, getIonoModel());
        final Gradient gDelay = rangeRateErrorIonosphericModel(station, gState, gParameters);
        final double[] derivatives = gDelay.getGradient();

        // update estimated derivatives with Jacobian of the measure wrt state
        final double[][] djac = rangeRateErrorJacobianState(derivatives);
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
        newValue[0] = newValue[0] + gDelay.getValue();
        estimated.setEstimatedValue(newValue);

    }

}
