/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.InertialProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

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
public class RangeRateTroposphericDelayModifier implements EstimationModifier<RangeRate> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Two-way measurement factor. */
    private final double fTwoWay;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     * @param tw     Flag indicating whether the measurement is two-way.
     */
    public RangeRateTroposphericDelayModifier(final DiscreteTroposphericModel model, final boolean tw) {
        tropoModel = model;
        if (tw) {
            fTwoWay = 2.;
        } else {
            fTwoWay = 1.;
        }
    }

    /** Get the station height above mean sea level.
     *
     * @param station  ground station (or measuring station)
     * @return the measuring station height above sea level, m
     */
    private double getStationHeightAMSL(final GroundStation station) {
        // FIXME heigth should be computed with respect to geoid WGS84+GUND = EGM2008 for example
        final double height = station.getBaseFrame().getPoint().getAltitude();
        return height;
    }

    /** Get the station height above mean sea level.
    * @param <T> type of the element
    * @param field field of the elements
    * @param station  ground station (or measuring station)
    * @return the measuring station height above sea level, m
    */
    private <T extends RealFieldElement<T>> T getStationHeightAMSL(final Field<T> field, final GroundStation station) {
        // FIXME heigth should be computed with respect to geoid WGS84+GUND = EGM2008 for example
        final T height = station.getBaseFrame().getPoint(field).getAltitude();
        return height;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // station altitude AMSL in meters
        final double height = getStationHeightAMSL(station);

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation1 = station.getBaseFrame().getElevation(position,
                                                                      state.getFrame(),
                                                                      state.getDate());

        // only consider measures above the horizon
        if (elevation1 > 0) {
            // tropospheric delay in meters
            final double d1 = tropoModel.pathDelay(elevation1, height, tropoModel.getParameters(), state.getDate());

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();

            // elevation
            final double elevation2 = station.getBaseFrame().getElevation(position2,
                                                                          state2.getFrame(),
                                                                          state2.getDate());

            // tropospheric delay dt after
            final double d2 = tropoModel.pathDelay(elevation2, height, tropoModel.getParameters(), state2.getDate());

            return fTwoWay * (d2 - d1) / dt;
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
    public <T extends RealFieldElement<T>> T rangeRateErrorTroposphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // station altitude AMSL in meters
        final T height = getStationHeightAMSL(field, station);

        // spacecraft position and elevation as seen from the ground station
        final FieldVector3D<T> position     = state.getPVCoordinates().getPosition();
        final T elevation1                  = station.getBaseFrame().getElevation(position,
                                                                                  state.getFrame(),
                                                                                  state.getDate());

        // only consider measures above the horizon
        if (elevation1.getReal() > 0) {
            // tropospheric delay in meters
            final T d1 = tropoModel.pathDelay(elevation1, height, parameters, state.getDate());

            // propagate spacecraft state forward by dt
            final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final FieldVector3D<T> position2     = state2.getPVCoordinates().getPosition();

            // elevation
            final T elevation2 = station.getBaseFrame().getElevation(position2,
                                                                     state2.getFrame(),
                                                                     state2.getDate());


            // tropospheric delay dt after
            final T d2 = tropoModel.pathDelay(elevation2, height, parameters, state2.getDate());

            return (d2.subtract(d1)).divide(dt).multiply(fTwoWay);
        }

        return zero;
    }

    /** Compute the Jacobian of the delay term wrt state using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    *
    * @return Jacobian of the delay wrt state
    */
    private double[][] rangeRateErrorJacobianState(final double[] derivatives, final int freeStateParameters) {
        final double[][] finiteDifferencesJacobian = new double[1][6];
        for (int i = 0; i < freeStateParameters; i++) {
            // First element is the value of the delay
            finiteDifferencesJacobian[0][i] = derivatives[i + 1];
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
    private double rangeRateErrorParameterDerivative(final GroundStation station,
                                                     final ParameterDriver driver,
                                                     final SpacecraftState state) {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeRateErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative =
                        Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] rangeRateErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
        // 0                               -> value of the delay
        // 1 ... freeStateParameters       -> derivatives of the delay wrt state
        // freeStateParameters + 1 ... n   -> derivatives of the delay wrt tropospheric parameters
        final int dim = derivatives.length - 1 - freeStateParameters;
        final double[] rangeError = new double[dim];

        for (int i = 0; i < dim; i++) {
            rangeError[i] = derivatives[1 + freeStateParameters + i];
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
    public void modify(final EstimatedMeasurement<RangeRate> estimated) {
        final RangeRate       measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // update estimated derivatives with Jacobian of the measure wrt state
        final TroposphericDSConverter converter =
                new TroposphericDSConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(tropoModel);
        final DerivativeStructure[] dsParameters = converter.getParameters(dsState, tropoModel);
        final DerivativeStructure dsDelay = rangeRateErrorTroposphericModel(station, dsState, dsParameters);
        final double[] derivatives = dsDelay.getAllDerivatives();

        final double[][] djac = rangeRateErrorJacobianState(derivatives, converter.getFreeStateParameters());
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
        newValue[0] = newValue[0] + dsDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
