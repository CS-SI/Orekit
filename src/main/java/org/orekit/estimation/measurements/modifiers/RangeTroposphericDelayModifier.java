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

import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.MultivariateVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.EstimationUtils;
import org.orekit.estimation.StateFunction;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Range;
import org.orekit.models.earth.TroposphericModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range measurement with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeTroposphericDelayModifier implements EvaluationModifier<Range> {
    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range measurement method.
     */
    public RangeTroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
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
    private double rangeErrorTroposphericModel(final GroundStation station, final SpacecraftState state)
        throws OrekitException {
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

            // delay in meters
            final double delay = tropoModel.pathDelay(elevation, height);

            // Multiply by two because it is a two-way measurment.
            return 2 * delay;
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
    private double[][] rangeErrorJacobianState(final GroundStation station, final SpacecraftState refstate)
        throws OrekitException {
        final double[][] finiteDifferencesJacobian =
                        EstimationUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                try {
                                    // evaluate target's elevation with a changed target position
                                    final double value = rangeErrorTroposphericModel(station, state);

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
     * @return jacobian of the delay wrt station position
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeErrorJacobianParameter(final GroundStation station,
                                                   final SpacecraftState state,
                                                   final double delay)
    throws OrekitException {
        final ParameterDriver positionOffsetDriver = station.getPositionOffsetDriver();

        final double[][] finiteDifferencesJacobian =
                        EstimationUtils.differentiate(new MultivariateVectorFunction() {
                                public double[] value(final double[] point) throws OrekitExceptionWrapper {
                                    try {
                                        final double[] savedParameter = positionOffsetDriver.getValue();

                                        positionOffsetDriver.setValue(point);

                                        final double value = rangeErrorTroposphericModel(station, state);

                                        positionOffsetDriver.setValue(savedParameter);

                                        return new double[] {
                                            value
                                        };

                                    } catch (OrekitException oe) {
                                        throw new OrekitExceptionWrapper(oe);
                                    }
                                }
                            }, 1, 3, 10.0, 10.0, 10.0).value(positionOffsetDriver.getValue());

        return finiteDifferencesJacobian;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final Evaluation<Range> evaluation)
        throws OrekitException {
        final Range measure = evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangeErrorTroposphericModel(station, state);

        // update measurement value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
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

        final ParameterDriver positionOffsetDriver = station.getPositionOffsetDriver();
        if (positionOffsetDriver.isEstimated()) {
            // update measurement derivatives with jacobian of the measure wrt station parameters
            final double[][] djacdp = rangeErrorJacobianParameter(station,
                                                                  state,
                                                                  delay);
            final double[][] parameterDerivatives = evaluation.getParameterDerivatives(positionOffsetDriver);
            for (int irow = 0; irow < parameterDerivatives.length; ++irow) {
                for (int jcol = 0; jcol < parameterDerivatives[0].length; ++jcol) {
                    parameterDerivatives[irow][jcol] += djacdp[irow][jcol];
                }
            }
            evaluation.setParameterDerivatives(positionOffsetDriver, parameterDerivatives);
        }
    }
}
