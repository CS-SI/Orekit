/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.TimeSpanMap.Span;

/** Class modifying theoretical turn-around TurnAroundRange measurement with tropospheric delay.
 * The effect of tropospheric correction on the TurnAroundRange is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TurnAroundRangeTroposphericDelayModifier implements EstimationModifier<TurnAroundRange> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current TurnAroundRange measurement method.
     */
    public TurnAroundRangeTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    private double rangeErrorTroposphericModel(final GroundStation station, final SpacecraftState state) {
        //
        final Vector3D position = state.getPosition();

        // elevation
        final double elevation =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation > 0) {
            // Delay in meters
            final double delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(), tropoModel.getParameters(state.getDate()), state.getDate());

            return delay;
        }

        return 0;
    }

    /** Compute the measurement error due to Troposphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    private <T extends CalculusFieldElement<T>> T rangeErrorTroposphericModel(final GroundStation station,
                                                                          final FieldSpacecraftState<T> state,
                                                                          final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        //
        final FieldVector3D<T> position = state.getPosition();
        final T dsElevation             =
                        station.getBaseFrame().getTrackingCoordinates(position,  state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (dsElevation.getReal() > 0) {
            // Delay in meters
            final T delay = tropoModel.pathDelay(dsElevation, station.getBaseFrame().getPoint(field), parameters, state.getDate());

            return delay;
        }

        return zero;
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] rangeErrorJacobianState(final double[] derivatives) {
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
    private double rangeErrorParameterDerivative(final GroundStation station,
                                                 final ParameterDriver driver,
                                                 final SpacecraftState state) {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                return rangeErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative = Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver, state.getDate());

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] rangeErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0 ... freeStateParameters - 1 -> derivatives of the delay wrt state
        // freeStateParameters ... n     -> derivatives of the delay wrt tropospheric parameters
        final int dim = derivatives.length - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[freeStateParameters + i];
        }

        return rangeError;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TurnAroundRange> estimated) {

        final TurnAroundRange measurement      = estimated.getObservedMeasurement();
        final GroundStation   primaryStation   = measurement.getPrimaryStation();
        final GroundStation   secondaryStation = measurement.getSecondaryStation();
        final SpacecraftState state            = estimated.getStates()[0];

        // Update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the TurnAroundRange.
        final double[] newValue       = estimated.getEstimatedValue();
        final double   primaryDelay   = rangeErrorTroposphericModel(primaryStation, state);
        final double   secondaryDelay = rangeErrorTroposphericModel(secondaryStation, state);
        newValue[0] = newValue[0] + primaryDelay + secondaryDelay;
        estimated.setEstimatedValue(newValue);

    }
    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<TurnAroundRange> estimated) {
        final TurnAroundRange measurement      = estimated.getObservedMeasurement();
        final GroundStation   primaryStation   = measurement.getPrimaryStation();
        final GroundStation   secondaryStation = measurement.getSecondaryStation();
        final SpacecraftState state            = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final ModifierGradientConverter converter =
                new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame()));
        final FieldSpacecraftState<Gradient> gState = converter.getState(tropoModel);
        final Gradient[] gParameters = converter.getParametersAtStateDate(gState, tropoModel);
        final Gradient   primaryGDelay        = rangeErrorTroposphericModel(primaryStation, gState, gParameters);
        final Gradient   secondaryGDelay      = rangeErrorTroposphericModel(secondaryStation, gState, gParameters);
        final double[]   primaryDerivatives   = primaryGDelay.getGradient();
        final double[]   secondaryDerivatives = secondaryGDelay.getGradient();

        final double[][] primaryDjac      = rangeErrorJacobianState(primaryDerivatives);
        final double[][] secondaryDjac    = rangeErrorJacobianState(secondaryDerivatives);
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
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    final double[] derivatives = rangeErrorParameterDerivative(primaryDerivatives, converter.getFreeStateParameters());
                    parameterDerivative += derivatives[indexPrimary];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    indexPrimary += 1;
                }
            }

        }

        int indexSecondary = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    final double[] derivatives = rangeErrorParameterDerivative(secondaryDerivatives, converter.getFreeStateParameters());
                    parameterDerivative += derivatives[indexSecondary];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                    indexSecondary += 1;
                }
            }

        }

        // Update derivatives with respect to primary station position
        for (final ParameterDriver driver : Arrays.asList(primaryStation.getClockOffsetDriver(),
                                                          primaryStation.getEastOffsetDriver(),
                                                          primaryStation.getNorthOffsetDriver(),
                                                          primaryStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += rangeErrorParameterDerivative(primaryStation, driver, state);
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update derivatives with respect to secondary station position
        for (final ParameterDriver driver : Arrays.asList(secondaryStation.getEastOffsetDriver(),
                                                          secondaryStation.getNorthOffsetDriver(),
                                                          secondaryStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    double parameterDerivative = estimated.getParameterDerivatives(driver, span.getStart())[0];
                    parameterDerivative += rangeErrorParameterDerivative(secondaryStation, driver, state);
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative);
                }
            }
        }

        // Update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the TurnAroundRange.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + primaryGDelay.getReal() + secondaryGDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
