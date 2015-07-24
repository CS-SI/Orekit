/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.utils.FiniteDifferenceUtils;
import org.orekit.estimation.measurements.utils.StateFunction;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.models.earth.TroposphericDelayModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;

public class RangeRateTroposphericDelayModifier implements EvaluationModifier {
    /** Tropospheric delay model. */
    private final TroposphericDelayModel tropoModel;

    /**
     * Constructor.
     * @param model  Tropospheric delay model
     */
    public RangeRateTroposphericDelayModifier(final TroposphericDelayModel model) {
        tropoModel = model;
    }

    /**
     * Simple Constructor.
     */
    public RangeRateTroposphericDelayModifier() {
        tropoModel = SaastamoinenModel.getStandardModel();
    }

    /** Compute the measurement error due to Ionosphere.
     *
     * @param state spacecraft state
     * @return the measurement error due to Ionosphere
     */
    public double rangeRateErrorIonosphericModel(final SpacecraftState state)
    {
        return 0;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) throws OrekitException
    {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 1e-3; // s

        // station altitude AMSL in meters
        final double height = station.getBaseFrame().getPoint().getAltitude();

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPVCoordinates().getPosition();
        final double elevation1 = station.getBaseFrame().getElevation(position,
                                                                      state.getFrame(),
                                                                      state.getDate());
        // tropospheric delay
        final double d1 = tropoModel.calculatePathDelay(elevation1, height);

        // propagate spacecraft state forward by dt
        final SpacecraftState state2 = state.shiftedBy(dt);

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position2 = state2.getPVCoordinates().getPosition();
        final double elevation2 =
                station.getBaseFrame().getElevation(position2,
                                                    state2.getFrame(),
                                                    state2.getDate());

        // tropospheric delay dt after
        final double d2 = tropoModel.calculatePathDelay(elevation2, height);

        return (d2 - d1) / dt;
    }

    /** Compute the Jacobian of the delay term wrt state.
     *
     * @param station station
     * @param state spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeRateErrorJacobianState(final GroundStation station,
                                               final SpacecraftState state,
                                               final double delay) throws OrekitException
    {
        // TODO For now, we compute by finite differences. We would need to have the derivative of the
        // delay (whichever models) with respect to some basic parameters (height, latitude, longitude).
        // We would then only need to compute, here, the derivatives of the station parameters wrt those basic parameters.
        final double[][] jacobian =
                        FiniteDifferenceUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                return new double[]{rangeRateErrorTroposphericModel(station, state)};
                            }
                        }, 1, OrbitType.CARTESIAN,
                        PositionAngle.TRUE, 10.0, 3).value(state);

        return jacobian;
    }

    /** Compute the Jacobian of the delay term wrt parameters.
     *
     * @param station station
     * @param state spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeRateErrorJacobianParameter(final GroundStation station,
                                                   final SpacecraftState state,
                                                   final double delay) throws OrekitException
    {
        // TODO For now, we compute by finite differences. We would need to have the derivative of the
        // delay (whichever models) with respect to some basic parameters (height, latitude, longitude).
        // We would then only need to compute, here, the derivatives of the station parameters wrt those basic parameters.
        final double[][] jacobian =
                        FiniteDifferenceUtils.differentiate(new MultivariateVectorFunction() {
                                public double[] value(final double[] point) throws OrekitExceptionWrapper {
                                    try {

                                        final double[] savedParameter = station.getValue();

                                        // evaluate range with a changed station position
                                        station.setValue(point);
                                        final double[] result = new double[] {
                                                        rangeRateErrorTroposphericModel(station,
                                                                                    state)};

                                        station.setValue(savedParameter);
                                        return result;

                                    } catch (OrekitException oe) {
                                        throw new OrekitExceptionWrapper(oe);
                                    }
                                }
                            }, 1, 3, 20.0, 20.0, 20.0).value(station.getValue());

        return jacobian;
    }

    @Override
    public List<Parameter> getSupportedParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void modify(final Evaluation evaluation)
        throws OrekitException {
        final RangeRate measure = (RangeRate) evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangeRateErrorTroposphericModel(station, state);

        // update measurement value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue;
        newValue[0] = newValue[0] + delay;
        evaluation.setValue(newValue);

        // update measurement derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeRateErrorJacobianState(station,
                                      state,
                                      delay);
        final double[][] stateDerivatives = evaluation.getStateDerivatives();
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        evaluation.setStateDerivatives(stateDerivatives);


        if (station.isEstimated()) {
            // update measurement derivatives with jacobian of the measure wrt station parameters
            final double[][] djacdp = rangeRateErrorJacobianParameter(station,
                                                                  state,
                                                                  delay);
            final double[][] parameterDerivatives = evaluation.getParameterDerivatives(station.getName());
            for (int irow = 0; irow < parameterDerivatives.length; ++irow) {
                for (int jcol = 0; jcol < parameterDerivatives[0].length; ++jcol) {
                    parameterDerivatives[irow][jcol] += djacdp[irow][jcol];
                }
            }
            evaluation.setParameterDerivatives(station.getName(), parameterDerivatives);
        }
    }
}
