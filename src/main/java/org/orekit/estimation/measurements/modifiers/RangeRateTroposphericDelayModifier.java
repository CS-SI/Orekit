/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.EstimationUtils;
import org.orekit.estimation.ParameterFunction;
import org.orekit.estimation.StateFunction;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.models.earth.TroposphericModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
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
public class RangeRateTroposphericDelayModifier implements EstimationModifier<RangeRate> {

    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Two-way measurement factor. */
    private final double fTwoWay;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     * @param tw     Flag indicating whether the measurement is two-way.
     */
    public RangeRateTroposphericDelayModifier(final TroposphericModel model, final boolean tw) {
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

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state)
        throws OrekitException {
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
            final double d1 = tropoModel.pathDelay(elevation1, height);

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();

            // elevation
            final double elevation2 = station.getBaseFrame().getElevation(position2,
                                                                          state2.getFrame(),
                                                                          state2.getDate());

            // tropospheric delay dt after
            final double d2 = tropoModel.pathDelay(elevation2, height);

            return fTwoWay * (d2 - d1) / dt;
        }

        return 0;
    }


    /** Compute the Jacobian of the delay term wrt state.
     *
     * @param station station
     * @param refstate spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeRateErrorJacobianState(final GroundStation station,
                                                   final SpacecraftState refstate,
                                                   final double delay)
        throws OrekitException {
        final double[][] finiteDifferencesJacobian =
                        EstimationUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                try {
                                    // evaluate target's elevation with a changed target position
                                    final double value = rangeRateErrorTroposphericModel(station, state);

                                    return new double[] {value };

                                } catch (OrekitException oe) {
                                    throw new OrekitExceptionWrapper(oe);
                                }
                            }
                        }, 1, OrbitType.CARTESIAN,
                        PositionAngle.TRUE, 15.0, 3).value(refstate);

        return finiteDifferencesJacobian;
    }

    /** Compute the derivative of the delay term wrt parameters.
    *
    * @param station ground station
    * @param driver driver for the station offset parameter
    * @param state spacecraft state
    * @param delay current ionospheric delay
    * @return derivative of the delay wrt station offset parameter
    * @throws OrekitException  if frames transformations cannot be computed
    */
    private double rangeRateErrorParameterDerivative(final GroundStation station,
                                                     final ParameterDriver driver,
                                                     final SpacecraftState state,
                                                     final double delay)
        throws OrekitException {

        final ParameterFunction rangeError = new ParameterFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver parameterDriver) throws OrekitException {
                return rangeRateErrorTroposphericModel(station, state);
            }
        };

        final ParameterFunction rangeErrorDerivative =
                        EstimationUtils.differentiate(rangeError, driver, 3, 10.0);

        return rangeErrorDerivative.value(driver);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<RangeRate> estimated)
        throws OrekitException {
        final RangeRate       measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getState();

        final double[] oldValue = estimated.getEstimatedValue();

        final double delay = rangeRateErrorTroposphericModel(station, state);

        // update estimated value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + delay;
        estimated.setEstimatedValue(newValue);

        // update estimated derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeRateErrorJacobianState(station,
                                      state,
                                      delay);
        final double[][] stateDerivatives = estimated.getStateDerivatives();
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        estimated.setStateDerivatives(stateDerivatives);

        for (final ParameterDriver driver : Arrays.asList(station.getEastOffsetDriver(),
                                                          station.getNorthOffsetDriver(),
                                                          station.getZenithOffsetDriver())) {
            if (driver.isSelected()) {
                // update estimated derivatives with derivative of the modification wrt station parameters
                double parameterDerivative = estimated.getParameterDerivatives(driver)[0];
                parameterDerivative += rangeRateErrorParameterDerivative(station, driver, state, delay);
                estimated.setParameterDerivatives(driver, parameterDerivative);
            }
        }

    }

}
