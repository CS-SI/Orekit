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

import java.util.List;

import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.EstimationUtils;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.StateFunction;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.models.earth.TroposphericModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range-rate measurements with tropospheric delay.
 * The effect of tropospheric correction on the range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to
 * time.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 7.1
 */
public class RangeRateTroposphericDelayModifier implements EvaluationModifier<RangeRate> {
    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** */
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
        // FIXME Il faut la hauteur par rapport au geoide WGS84+GUND = EGM2008 par exemple
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
                                                  final SpacecraftState state) throws OrekitException
    {
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
                                               final double delay) throws OrekitException
    {
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
        final GroundStation stationParameter = station;

        final double[][] finiteDifferencesJacobian =
                        EstimationUtils.differentiate(new MultivariateVectorFunction() {
                                public double[] value(final double[] point) throws OrekitExceptionWrapper {
                                    try {
                                        final double[] savedParameter = stationParameter.getValue();

                                        stationParameter.setValue(point);

                                        final double value = rangeRateErrorTroposphericModel(stationParameter, state);

                                        stationParameter.setValue(savedParameter);

                                        return new double[]{value };

                                    } catch (OrekitException oe) {
                                        throw new OrekitExceptionWrapper(oe);
                                    }
                                }
                            }, 1, 3, 10.0, 10.0, 10.0).value(stationParameter.getValue());

        return finiteDifferencesJacobian;
    }

    @Override
    public List<Parameter> getSupportedParameters() {
        return null;
    }

    @Override
    public void modify(final Evaluation<RangeRate> evaluation)
        throws OrekitException {
        final RangeRate measure = evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangeRateErrorTroposphericModel(station, state);

        // update measurement value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
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
            // by simply adding the jacobian the delay term.
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
