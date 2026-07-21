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

import java.util.Arrays;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundObserver;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TrackingCoordinates;

/**
 * Class modifying theoretical turn-around TurnAroundRange measurement with
 * tropospheric delay.
 * <p>
 * The effect of tropospheric correction on the TurnAroundRange is directly
 * computed
 * through the computation of the tropospheric delay.
 * </p>
 * <p>
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in
 * the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 * </p>
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TurnAroundRangeTroposphericDelayModifier implements EstimationModifier<TurnAroundRange> {

    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     * @param model Tropospheric delay model appropriate for the current
     *              TurnAroundRange measurement method.
     * @since 12.1
     */
    public TurnAroundRangeTroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "troposphere";
    }

    /** Compute the measurement error due to Troposphere.
     * @param observer object that observes signal
     * @param state    estimated spacecraft state
     * @return the measurement error due to Troposphere
     */
    private double rangeErrorTroposphericModel(final Observer observer, final SpacecraftState state) {
        //

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // tracking
            final TrackingCoordinates trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates.getElevation() > 0) {
                // Delay in meters
                final AbsoluteDate date = state.getDate();
                return tropoModel.pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(date),
                        tropoModel.getParameters(date), date).getDelay();
            }

            return 0;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }

    /** Compute the measurement error due to Troposphere.
     * @param <T>        type of the element
     * @param observer   object that observes signal
     * @param state      estimated spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    private <T extends CalculusFieldElement<T>> T rangeErrorTroposphericModel(final Observer observer,
            final FieldSpacecraftState<T> state,
            final T[] parameters) {

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // Field
            final FieldAbsoluteDate<T> date = state.getDate();
            final Field<T> field = date.getField();
            final T zero = field.getZero();

            final FieldTrackingCoordinates<T> trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates.getElevation().getReal() > 0) {
                // Delay in meters
                return tropoModel.pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(date),
                        parameters, date).getDelay();
            }

            return zero;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }

    /**
     * Compute the Jacobian of the delay term wrt state using
     * automatic differentiation.
     * @param derivatives tropospheric delay derivatives
     * @return Jacobian of the delay wrt state
     */
    private double[][] rangeErrorJacobianState(final double[] derivatives) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        System.arraycopy(derivatives, 0, finiteDifferencesJacobian[0], 0, 6);
        return finiteDifferencesJacobian;
    }

    /** Compute the derivative of the delay term wrt parameters.
     * @param observer measurement observer
     * @param driver  driver for the observer offset parameter
     * @param state   spacecraft state
     * @return derivative of the delay wrt observer offset parameter
     */
    private double rangeErrorParameterDerivative(final Observer observer,
            final ParameterDriver driver,
            final SpacecraftState state) {

        final ParameterFunction rangeErrorDerivative = Differentiation.differentiate((parameterDriver, date) -> rangeErrorTroposphericModel(observer, state),
                3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver, state.getDate());

    }

    /** Compute the derivative of the delay term wrt parameters using
     * automatic differentiation.
     * @param derivatives         tropospheric delay derivatives
     * @param freeStateParameters dimension of the state.
     * @return derivative of the delay wrt tropospheric model parameters
     */
    private double[] rangeErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n -> derivatives of the delay wrt tropospheric
        // parameters
        return Arrays.copyOfRange(derivatives, freeStateParameters, derivatives.length);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TurnAroundRange> estimated) {

        final TurnAroundRange measurement = estimated.getObservedMeasurement();
        final Observer        primaryObserver   = measurement.getPrimaryObserver();
        final Observer        secondaryObserver = measurement.getSecondaryObserver();
        final SpacecraftState state = estimated.getStates()[0];

        // Update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the TurnAroundRange.
        final double[] newValue = estimated.getEstimatedValue();
        final double primaryDelay = rangeErrorTroposphericModel(primaryObserver, state);
        final double secondaryDelay = rangeErrorTroposphericModel(secondaryObserver, state);
        newValue[0] = newValue[0] + primaryDelay + secondaryDelay;
        estimated.modifyEstimatedValue(this, newValue);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<TurnAroundRange> estimated) {
        final TurnAroundRange measurement = estimated.getObservedMeasurement();
        final Observer        primaryObserver   = measurement.getPrimaryObserver();
        final Observer        secondaryObserver = measurement.getSecondaryObserver();
        final SpacecraftState state = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final ModifierGradientConverter converter = new ModifierGradientConverter(state, 6,
                new FrameAlignedProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(tropoModel);
        final Gradient[] gParameters = converter.getParametersAtStateDate(gState, tropoModel);
        final Gradient primaryGDelay = rangeErrorTroposphericModel(primaryObserver, gState, gParameters);
        final Gradient secondaryGDelay = rangeErrorTroposphericModel(secondaryObserver, gState, gParameters);
        final double[] primaryDerivatives = primaryGDelay.getGradient();
        final double[] secondaryDerivatives = secondaryGDelay.getGradient();

        final double[][] primaryDjac = rangeErrorJacobianState(primaryDerivatives);
        final double[][] secondaryDjac = rangeErrorJacobianState(secondaryDerivatives);
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += primaryDjac[irow][jcol] + secondaryDjac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int indexPrimary = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt
                // tropospheric parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    final double[] derivatives = rangeErrorParameterDerivative(primaryDerivatives,
                            converter.getFreeStateParameters());
                    parameterDerivative += derivatives[indexPrimary];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    indexPrimary += 1;
                }
            }

        }

        int indexSecondary = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt
                // tropospheric parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    final double[] derivatives = rangeErrorParameterDerivative(secondaryDerivatives,
                            converter.getFreeStateParameters());
                    parameterDerivative += derivatives[indexSecondary];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    indexSecondary += 1;
                }
            }

        }

        // Update derivatives with respect to primary observer position
        for (final ParameterDriver driver : primaryObserver.getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += rangeErrorParameterDerivative(primaryObserver, driver, state);
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update derivatives with respect to secondary observer position
        for (final ParameterDriver driver : secondaryObserver.getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += rangeErrorParameterDerivative(secondaryObserver, driver, state);
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the TurnAroundRange.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + primaryGDelay.getReal() + secondaryGDelay.getReal();
        estimated.modifyEstimatedValue(this, newValue);

    }

}
