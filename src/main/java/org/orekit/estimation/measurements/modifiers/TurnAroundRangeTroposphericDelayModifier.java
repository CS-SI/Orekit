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
import org.orekit.estimation.measurements.TurnAroundRange;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

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

    /** Get the station height above mean sea level.
     *
     * @param station  ground station (or measuring station)
     * @return the measuring station height above sea level, m
     */
    private double getStationHeightAMSL(final GroundStation station) {
        // FIXME height should be computed with respect to geoid WGS84+GUND = EGM2008 for example
        final double height = station.getBaseFrame().getPoint().getAltitude();
        return height;
    }

    /** Get the station height above mean sea level.
    * @param <T> type of the elements
    * @param field field of the elements
    * @param station  ground station (or measuring station)
    * @return the measuring station height above sea level, m
    */
    private <T extends RealFieldElement<T>> T getStationHeightAMSL(final Field<T> field,
                                                                   final GroundStation station) {
        // FIXME heigth should be computed with respect to geoid WGS84+GUND = EGM2008 for example
        final T height = station.getBaseFrame().getPoint(field).getAltitude();
        return height;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    private double rangeErrorTroposphericModel(final GroundStation station, final SpacecraftState state) {
        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

        // only consider measures above the horizon
        if (elevation > 0) {
            // altitude AMSL in meters
            final double height = getStationHeightAMSL(station);

            // Delay in meters
            final double delay = tropoModel.pathDelay(elevation, height, tropoModel.getParameters(), state.getDate());

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
    private <T extends RealFieldElement<T>> T rangeErrorTroposphericModel(final GroundStation station,
                                                                          final FieldSpacecraftState<T> state,
                                                                          final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        //
        final FieldVector3D<T> position = state.getPVCoordinates().getPosition();
        final T dsElevation             = station.getBaseFrame().getElevation(position,
                                                                              state.getFrame(),
                                                                              state.getDate());

        // only consider measures above the horizon
        if (dsElevation.getReal() > 0) {
            // altitude AMSL in meters
            final T height = getStationHeightAMSL(field, station);

            // Delay in meters
            final T delay = tropoModel.pathDelay(dsElevation, height, parameters, state.getDate());

            return delay;
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
    private double[][] rangeErrorJacobianState(final double[] derivatives, final int freeStateParameters) {
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
    private double rangeErrorParameterDerivative(final GroundStation station,
                                                 final ParameterDriver driver,
                                                 final SpacecraftState state) {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) {
                return rangeErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative = Differentiation.differentiate(rangeError, 3, 10.0 * driver.getScale());

        return rangeErrorDerivative.value(driver);

    }

    /** Compute the derivative of the delay term wrt parameters using
    * automatic differentiation.
    *
    * @param derivatives tropospheric delay derivatives
    * @param freeStateParameters dimension of the state.
    * @return derivative of the delay wrt tropospheric model parameters
    */
    private double[] rangeErrorParameterDerivative(final double[] derivatives, final int freeStateParameters) {
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
    public void modify(final EstimatedMeasurement<TurnAroundRange> estimated) {
        final TurnAroundRange measurement   = estimated.getObservedMeasurement();
        final GroundStation   masterStation = measurement.getMasterStation();
        final GroundStation   slaveStation  = measurement.getSlaveStation();
        final SpacecraftState state         = estimated.getStates()[0];

        final double[] oldValue = estimated.getEstimatedValue();

        // Update estimated derivatives with Jacobian of the measure wrt state
        final TroposphericDSConverter converter =
                new TroposphericDSConverter(state, 6, new InertialProvider(state.getFrame()));
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(tropoModel);
        final DerivativeStructure[] dsParameters = converter.getParameters(dsState, tropoModel);
        final DerivativeStructure masterDSDelay = rangeErrorTroposphericModel(masterStation, dsState, dsParameters);
        final DerivativeStructure slaveDSDelay = rangeErrorTroposphericModel(slaveStation, dsState, dsParameters);
        final double[] masterDerivatives = masterDSDelay.getAllDerivatives();
        final double[] slaveDerivatives  = masterDSDelay.getAllDerivatives();

        final double[][] masterDjac = rangeErrorJacobianState(masterDerivatives, converter.getFreeStateParameters());
        final double[][] slaveDjac  = rangeErrorJacobianState(slaveDerivatives, converter.getFreeStateParameters());
        final double[][] stateDerivatives = estimated.getStateDerivatives(0);
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += masterDjac[irow][jcol] + slaveDjac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(0, stateDerivatives);

        int indexMaster = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] derivatives = rangeErrorParameterDerivative(masterDerivatives, converter.getFreeStateParameters());
                parameterDerivative += derivatives[indexMaster];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                indexMaster += 1;
            }

        }

        int indexSlave = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt tropospheric parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                final double[] derivatives = rangeErrorParameterDerivative(slaveDerivatives, converter.getFreeStateParameters());
                parameterDerivative += derivatives[indexSlave];
                estimated.setParameterDerivatives(driver, parameterDerivative);
                indexSlave += 1;
            }

        }

        // Update derivatives with respect to master station position
        for (final ParameterDriver driver : Arrays.asList(masterStation.getClockOffsetDriver(),
                                                          masterStation.getEastOffsetDriver(),
                                                          masterStation.getNorthOffsetDriver(),
                                                          masterStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeErrorParameterDerivative(masterStation, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // Update derivatives with respect to slave station position
        for (final ParameterDriver driver : Arrays.asList(slaveStation.getEastOffsetDriver(),
                                                          slaveStation.getNorthOffsetDriver(),
                                                          slaveStation.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeErrorParameterDerivative(slaveStation, driver, state);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

        // Update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the TurnAroundRange.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + masterDSDelay.getReal() + slaveDSDelay.getReal();
        estimated.setEstimatedValue(newValue);

    }

}
