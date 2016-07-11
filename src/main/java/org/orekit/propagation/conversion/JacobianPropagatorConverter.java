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
package org.orekit.propagation.conversion;

import java.util.List;

import org.hipparchus.analysis.MultivariateVectorFunction;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Propagator converter using the real jacobian.
 * @author Pascal Parraud
 * @since 6.0
 */
public class JacobianPropagatorConverter extends AbstractPropagatorConverter {

    /** Numerical propagator builder. */
    private final NumericalPropagatorBuilder builder;

    /** Simple constructor.
     * @param builder builder for adapted propagator, it <em>must</em>
     * be configured to generate {@link OrbitType#CARTESIAN} states
     * @param threshold absolute threshold for optimization algorithm
     * @param maxIterations maximum number of iterations for fitting
     * @exception OrekitException if the builder {@link
     * NumericalPropagatorBuilder#getOrbitType() orbit type} is not
     * {@link OrbitType#CARTESIAN}
     */
    public JacobianPropagatorConverter(final NumericalPropagatorBuilder builder,
                                       final double threshold,
                                       final int maxIterations)
        throws OrekitException {
        super(builder, threshold, maxIterations);
        if (builder.getOrbitType() != OrbitType.CARTESIAN) {
            throw new OrekitException(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED,
                                      builder.getOrbitType(), OrbitType.CARTESIAN);
        }
        this.builder = builder;
    }

    /** {@inheritDoc} */
    protected MultivariateVectorFunction getObjectiveFunction() {
        return new MultivariateVectorFunction() {

            /** {@inheritDoc} */
            public double[] value(final double[] arg)
                throws IllegalArgumentException, OrekitExceptionWrapper {
                try {
                    final double[] value = new double[getTargetSize()];

                    final NumericalPropagator prop = builder.buildPropagator(arg);

                    final int stateSize = isOnlyPosition() ? 3 : 6;
                    final List<SpacecraftState> sample = getSample();
                    for (int i = 0; i < sample.size(); ++i) {
                        final int row = i * stateSize;
                        if (prop.getInitialState().getDate().equals(sample.get(i).getDate())) {
                            // use initial state
                            fillRows(value, row, prop.getInitialState());
                        } else {
                            // use a date detector to pick up states
                            prop.addEventDetector(new DateDetector(sample.get(i).getDate()).withHandler(new EventHandler<DateDetector>() {
                                /** {@inheritDoc} */
                                @Override
                                public Action eventOccurred(final SpacecraftState state, final DateDetector detector,
                                                            final boolean increasing)
                                    throws OrekitException {
                                    fillRows(value, row, state);
                                    return row + stateSize >= getTargetSize() ? Action.STOP : Action.CONTINUE;
                                }
                            }));
                        }
                    }

                    prop.propagate(sample.get(sample.size() - 1).getDate().shiftedBy(10.0));

                    return value;

                } catch (OrekitException ex) {
                    throw new OrekitExceptionWrapper(ex);
                }
            }

        };
    }

    /** {@inheritDoc} */
    protected MultivariateJacobianFunction getModel() {
        return new MultivariateJacobianFunction() {

            /** {@inheritDoc} */
            public Pair<RealVector, RealMatrix> value(final RealVector point)
                throws IllegalArgumentException, OrekitExceptionWrapper {
                try {

                    final RealVector value    = new ArrayRealVector(getTargetSize());
                    final RealMatrix jacobian = MatrixUtils.createRealMatrix(getTargetSize(), point.getDimension());

                    final NumericalPropagator prop  = builder.buildPropagator(point.toArray());
                    final int stateSize = isOnlyPosition() ? 3 : 6;
                    final ParameterDriversList orbitalParameters = builder.getOrbitalParametersDrivers();
                    final PartialDerivativesEquations pde = new PartialDerivativesEquations("pde", prop);
                    final ParameterDriversList propagationParameters = pde.getSelectedParameters();
                    prop.setInitialState(pde.setInitialJacobians(prop.getInitialState(), stateSize));
                    final JacobiansMapper mapper  = pde.getMapper();

                    final List<SpacecraftState> sample = getSample();
                    for (int i = 0; i < sample.size(); ++i) {
                        final int row = i * stateSize;
                        if (prop.getInitialState().getDate().equals(sample.get(i).getDate())) {
                            // use initial state and Jacobians
                            fillRows(value, jacobian, row, prop.getInitialState(), stateSize,
                                     orbitalParameters, propagationParameters, mapper);
                        } else {
                            // use a date detector to pick up state and Jacobians
                            prop.addEventDetector(new DateDetector(sample.get(i).getDate()).withHandler(new EventHandler<DateDetector>() {
                                /** {@inheritDoc} */
                                @Override
                                public Action eventOccurred(final SpacecraftState state, final DateDetector detector,
                                                            final boolean increasing)
                                    throws OrekitException {
                                    fillRows(value, jacobian, row, state, stateSize,
                                             orbitalParameters, propagationParameters, mapper);
                                    return row + stateSize >= getTargetSize() ? Action.STOP : Action.CONTINUE;
                                }
                            }));
                        }
                    }

                    prop.propagate(sample.get(sample.size() - 1).getDate().shiftedBy(10.0));

                    return new Pair<RealVector, RealMatrix>(value, jacobian);

                } catch (OrekitException ex) {
                    throw new OrekitExceptionWrapper(ex);
                }
            }

        };
    }

    /** Fill up a few value rows (either 6 or 3 depending on velocities used or not).
     * @param value values array
     * @param row first row index
     * @param state spacecraft state
     * @exception OrekitException if Jacobians matrices cannot be retrieved
     */
    private void fillRows(final double[] value, final int row, final SpacecraftState state)
        throws OrekitException {
        final PVCoordinates pv = state.getPVCoordinates(getFrame());
        value[row    ] = pv.getPosition().getX();
        value[row + 1] = pv.getPosition().getY();
        value[row + 2] = pv.getPosition().getZ();
        if (!isOnlyPosition()) {
            value[row + 3] = pv.getVelocity().getX();
            value[row + 4] = pv.getVelocity().getY();
            value[row + 5] = pv.getVelocity().getZ();
        }
    }

    /** Fill up a few Jacobian rows (either 6 or 3 depending on velocities used or not).
     * @param value values vector
     * @param jacobian Jacobian matrix
     * @param row first row index
     * @param state spacecraft state
     * @param stateSize state size
     * @param orbitalParameters drivers for the orbital parameters
     * @param propagationParameters drivers for the propagation model parameters
     * @param mapper state mapper
     * @exception OrekitException if Jacobians matrices cannot be retrieved
     */
    private void fillRows(final RealVector value, final RealMatrix jacobian, final int row,
                          final SpacecraftState state, final int stateSize,
                          final ParameterDriversList orbitalParameters,
                          final ParameterDriversList propagationParameters,
                          final JacobiansMapper mapper)
        throws OrekitException {

        // value part
        final PVCoordinates pv = state.getPVCoordinates(getFrame());
        value.setEntry(row,     pv.getPosition().getX());
        value.setEntry(row + 1, pv.getPosition().getY());
        value.setEntry(row + 2, pv.getPosition().getZ());
        if (!isOnlyPosition()) {
            value.setEntry(row + 3, pv.getVelocity().getX());
            value.setEntry(row + 4, pv.getVelocity().getY());
            value.setEntry(row + 5, pv.getVelocity().getZ());
        }

        // Jacobian part
        final double[][] dYdY0 = new double[mapper.getStateDimension()][mapper.getStateDimension()];
        final double[][] dYdP  = new double[mapper.getStateDimension()][mapper.getParameters()];
        mapper.getStateJacobian(state, dYdY0);
        mapper.getParametersJacobian(state, dYdP);
        for (int k = 0; k < stateSize; k++) {
            int index = 0;
            for (int j = 0; j < orbitalParameters.getNbParams(); ++j) {
                final ParameterDriver driver = orbitalParameters.getDrivers().get(j);
                if (driver.isSelected()) {
                    jacobian.setEntry(row + k, index++, dYdY0[k][j] * driver.getScale());
                }
            }
            for (int j = 0; j < propagationParameters.getNbParams(); ++j) {
                final ParameterDriver driver = propagationParameters.getDrivers().get(j);
                jacobian.setEntry(row + k, index++, dYdP[k][j] * driver.getScale());
            }
        }

    }

}

