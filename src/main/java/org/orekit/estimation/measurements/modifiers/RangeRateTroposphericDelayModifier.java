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
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range-rate measurements with tropospheric delay.
 * The effect of tropospheric correction on the range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to
 * time.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRateTroposphericDelayModifier extends BaseRangeRateTroposphericDelayModifier implements EstimationModifier<RangeRate> {

    /** Two-way measurement factor. */
    private final double fTwoWay;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     * @param tw     Flag indicating whether the measurement is two-way.
     */
    public RangeRateTroposphericDelayModifier(final DiscreteTroposphericModel model, final boolean tw) {
        super(model);
        if (tw) {
            fTwoWay = 2.;
        } else {
            fTwoWay = 1.;
        }
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    @Override
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) {
        return fTwoWay * super.rangeRateErrorTroposphericModel(station, state);
    }


    /** Compute the measurement error due to Troposphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    @Override
    public <T extends CalculusFieldElement<T>> T rangeRateErrorTroposphericModel(final GroundStation station,
                                                                                 final FieldSpacecraftState<T> state,
                                                                                 final T[] parameters) {
        return super.rangeRateErrorTroposphericModel(station, state, parameters).multiply(fTwoWay);
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
        final FieldSpacecraftState<Gradient> gState = converter.getState(getTropoModel());
        final Gradient[] gParameters = converter.getParameters(gState, getTropoModel());
        final Gradient gDelay = rangeRateErrorTroposphericModel(station, gState, gParameters);
        final double[] derivatives = gDelay.getGradient();

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
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] dDelaydP    = rangeRateErrorParameterDerivative(derivatives, converter.getFreeStateParameters());
                parameterDerivative += dDelaydP[index];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                index += 1;
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

        // update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + gDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
