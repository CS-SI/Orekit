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
import org.orekit.models.earth.IonosphericModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range-rate measurement with ionospheric delay.
 * The effect of ionospheric correction on the range-rate is directly computed
 * through the computation of the ionospheric delay difference with respect to
 * time.
 *
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 *
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRateIonosphericDelayModifier implements EstimationModifier<RangeRate> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Coefficient for measurment configuration (one-way, two-way). */
    private final double fTwoWay;

    /** Constructor.
     *
     * @param model Ionospheric delay model appropriate for the current range-rate measurement method.
     * @param twoWay Flag indicating whether the measurement is two-way.
     */
    public RangeRateIonosphericDelayModifier(final IonosphericModel model, final boolean twoWay) {
        ionoModel = model;

        if (twoWay) {
            fTwoWay = 2.;
        } else {
            fTwoWay = 1.;
        }
    }

    /** Compute the measurement error due to Ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Ionosphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double rangeRateErrorIonosphericModel(final GroundStation station, final SpacecraftState state)
        throws OrekitException {
        // The effect of ionospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

        // only consider measures above the horizon
        if (elevation > 0) {

            // compute azimuth
            final double azimuth = station.getBaseFrame().getAzimuth(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

            // delay in meters
            final double delay1 = ionoModel.pathDelay(state.getDate(),
                                                      station.getBaseFrame().getPoint(),
                                                      elevation,
                                                      azimuth);

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();
            final double elevation2 = station.getBaseFrame().getElevation(position2,
                                                                          state2.getFrame(),
                                                                          state2.getDate());

            // compute azimuth in degrees
            final double azimuth2 = station.getBaseFrame().getAzimuth(position2,
                                                                      state2.getFrame(),
                                                                      state2.getDate());

            // ionospheric delay dt after in meters
            final double delay2 = ionoModel.pathDelay(state2.getDate(),
                                                      station.getBaseFrame().getPoint(),
                                                      elevation2,
                                                      azimuth2);

            // delay in meters
            return fTwoWay * (delay2 - delay1) / dt;
        }

        return 0;
    }

    /** Compute the Jacobian of the delay term wrt state.
     *
     * @param station station
     * @param refstate reference spacecraft state
     *
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeErrorJacobianState(final GroundStation station,
                                               final SpacecraftState refstate)
        throws OrekitException {
        final double[][] finiteDifferencesJacobian =
                        EstimationUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                try {
                                    // evaluate target's elevation with a changed target position
                                    final double value = rangeRateErrorIonosphericModel(station, state);

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
                return rangeRateErrorIonosphericModel(station, state);
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

        final double delay = rangeRateErrorIonosphericModel(station, state);

        // update estimated value taking into account the ionospheric delay.
        // The ionospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + delay;
        estimated.setEstimatedValue(newValue);

        // update estimated derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeErrorJacobianState(station,
                                      state);
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
