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
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.StateFunction;
import org.orekit.models.earth.IonosphericDelayModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range measurement with ionospheric delay.
 * The effect of ionospheric correction on the range is directly computed
 * through the computation of the ionospheric delay.
 *
 *
 * @author Joris Olympio
 * @since 7.1
 */
public class RangeRateIonosphericDelayModifier implements EvaluationModifier {
    /** utility constant to convert from radians to degrees. */
    private static double RADIANS_TO_DEGREES = 180. / Math.PI;

    /** Ionospheric delay model. */
    private final IonosphericDelayModel ionoModel;

    /**
     * Constructor.
     * @param model  Ionospheric delay model
     */
    public RangeRateIonosphericDelayModifier(final IonosphericDelayModel model) {
        ionoModel = model;
    }

    /** Compute the measurement error due to Ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Ionosphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double rangerateErrorIonosphericModel(final GroundStation station,
                                                  final SpacecraftState state) throws OrekitException
    {
        // The effect of ionospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation in degrees
        final double elevation =
                station.getBaseFrame().getElevation(position,
                                                    state.getFrame(),
                                                    state.getDate()) * RADIANS_TO_DEGREES;

        // only consider measures above the horizon
        if (elevation > 0) {

            // compute azimuth in degrees
            final double azimuth = station.getBaseFrame().getAzimuth(position,
                                                                     state.getFrame(),
                                                                     state.getDate()) * RADIANS_TO_DEGREES;

            // delay in meters
            final double delay1 = ionoModel.calculatePathDelay(state.getDate(),
                                                              station.getBaseFrame().getPoint(),
                                                              elevation,
                                                              azimuth);

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();
            final double elevation2 =
                    station.getBaseFrame().getElevation(position2,
                                                        state2.getFrame(),
                                                        state2.getDate()) * RADIANS_TO_DEGREES;

            // compute azimuth in degrees
            final double azimuth2 = station.getBaseFrame().getAzimuth(position2,
                                                                      state2.getFrame(),
                                                                      state2.getDate()) * RADIANS_TO_DEGREES;

            // ionospheric delay dt after in meters
            final double delay2 = ionoModel.calculatePathDelay(state2.getDate(),
                                                               station.getBaseFrame().getPoint(),
                                                               elevation2,
                                                               azimuth2);

            // delay in meters
            return (delay2 - delay1) / dt;
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
                                          final SpacecraftState refstate) throws OrekitException
    {
        final double[][] finiteDifferencesJacobian =
                        EstimationTestUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                try {
                                    // evaluate target's elevation with a changed target position
                                    final double value = rangerateErrorIonosphericModel(station, state);

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
     * @param delay current ionospheric delay
     * @return jacobian of the delay wrt station position
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeErrorJacobianParameter(final GroundStation station,
                                                   final SpacecraftState state,
                                                   final double delay) throws OrekitException
    {
        final GroundStation stationParameter = station;

        final double[][] finiteDifferencesJacobian =
                        EstimationTestUtils.differentiate(new MultivariateVectorFunction() {
                                public double[] value(final double[] point) throws OrekitExceptionWrapper {
                                    try {
                                        final double[] savedParameter = stationParameter.getValue();

                                        stationParameter.setValue(point);

                                        final double value = rangerateErrorIonosphericModel(stationParameter, state);

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
    public void modify(final Evaluation evaluation)
        throws OrekitException {
        final Range measure = (Range) evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangerateErrorIonosphericModel(station, state);

        // update measurement value taking into account the ionospheric delay.
        // The ionospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + delay;
        evaluation.setValue(newValue);

        // update measurement derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeErrorJacobianState(station,
                                      state);
        final double[][] stateDerivatives = evaluation.getStateDerivatives();
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        evaluation.setStateDerivatives(stateDerivatives);


        if (station.isEstimated()) {
            // update measurement derivatives with jacobian of the measure wrt station parameters
            final double[][] djacdp = rangeErrorJacobianParameter(station,
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
