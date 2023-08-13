/* Copyright 2002-2023 CS GROUP
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
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeSpanMap.Span;

/** Propagator converter using the real Jacobian.
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
     */
    public JacobianPropagatorConverter(final NumericalPropagatorBuilder builder,
                                       final double threshold,
                                       final int maxIterations) {
        super(builder, threshold, maxIterations);
        if (builder.getOrbitType() != OrbitType.CARTESIAN) {
            throw new OrekitException(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED,
                                      builder.getOrbitType(), OrbitType.CARTESIAN);
        }
        this.builder = builder;
    }

    /** {@inheritDoc} */
    protected MultivariateVectorFunction getObjectiveFunction() {
        return point -> {
            final NumericalPropagator propagator  = builder.buildPropagator(point);
            final ValuesHandler handler = new ValuesHandler();
            propagator.getMultiplexer().add(handler);
            final List<SpacecraftState> sample = getSample();
            propagator.propagate(sample.get(sample.size() - 1).getDate().shiftedBy(10.0));
            return handler.value;
        };
    }

    /** {@inheritDoc} */
    protected MultivariateJacobianFunction getModel() {
        return point -> {
            final NumericalPropagator propagator  = builder.buildPropagator(point.toArray());
            final JacobianHandler handler = new JacobianHandler(propagator, point.getDimension());
            propagator.getMultiplexer().add(handler);
            final List<SpacecraftState> sample = getSample();
            propagator.propagate(sample.get(sample.size() - 1).getDate().shiftedBy(10.0));
            return new Pair<>(handler.value, handler.jacobian);
        };
    }

    /** Handler for picking up values at sample dates.
     * <p>
     * This class is heavily based on org.orekit.estimation.leastsquares.MeasurementHandler.
     * </p>
     * @since 11.1
     */
    private class ValuesHandler implements OrekitStepHandler {

        /** Values vector. */
        private final double[] value;

        /** Number of the next measurement. */
        private int number;

        /** Index of the next component in the model. */
        private int index;

        /** Simple constructor.
         */
        ValuesHandler() {
            this.value = new double[getTargetSize()];
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            number = 0;
            index  = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator) {

            while (number < getSample().size()) {

                // Consider the next sample to handle
                final SpacecraftState next = getSample().get(number);

                // Current state date
                final AbsoluteDate currentDate = interpolator.getCurrentState().getDate();
                if (next.getDate().compareTo(currentDate) > 0) {
                    return;
                }

                final PVCoordinates pv = interpolator.getInterpolatedState(next.getDate()).getPVCoordinates(getFrame());
                value[index++] = pv.getPosition().getX();
                value[index++] = pv.getPosition().getY();
                value[index++] = pv.getPosition().getZ();
                if (!isOnlyPosition()) {
                    value[index++] = pv.getVelocity().getX();
                    value[index++] = pv.getVelocity().getY();
                    value[index++] = pv.getVelocity().getZ();
                }

                // prepare handling of next measurement
                ++number;

            }

        }

    }

    /** Handler for picking up Jacobians at sample dates.
     * <p>
     * This class is heavily based on org.orekit.estimation.leastsquares.MeasurementHandler.
     * </p>
     * @since 11.1
     */
    private class JacobianHandler implements OrekitStepHandler {

        /** Values vector. */
        private final RealVector value;

        /** Jacobian matrix. */
        private final RealMatrix jacobian;

        /** State size (3 or 6). */
        private final int stateSize;

        /** Matrices harvester. */
        private final MatricesHarvester harvester;

        /** Number of the next measurement. */
        private int number;

        /** Index of the next Jacobian component in the model. */
        private int index;

        /** Simple constructor.
         * @param propagator propagator
         * @param columns number of columns of the Jacobian matrix
         */
        JacobianHandler(final NumericalPropagator propagator, final int columns) {
            this.value     = new ArrayRealVector(getTargetSize());
            this.jacobian  = MatrixUtils.createRealMatrix(getTargetSize(), columns);
            this.stateSize = isOnlyPosition() ? 3 : 6;
            this.harvester = propagator.setupMatricesComputation("converter-partials", null, null);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            number = 0;
            index  = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator) {

            while (number < getSample().size()) {

                // Consider the next sample to handle
                final SpacecraftState next = getSample().get(number);

                // Current state date
                final AbsoluteDate currentDate = interpolator.getCurrentState().getDate();
                if (next.getDate().compareTo(currentDate) > 0) {
                    return;
                }

                fillRows(index, interpolator.getInterpolatedState(next.getDate()),
                         builder.getOrbitalParametersDrivers());

                // prepare handling of next measurement
                ++number;
                index += stateSize;

            }

        }

        /** Fill up a few Jacobian rows (either 6 or 3 depending on velocities used or not).
         * @param row first row index
         * @param state spacecraft state
         * @param orbitalParameters drivers for the orbital parameters
         */
        private void fillRows(final int row,
                              final SpacecraftState state,
                              final ParameterDriversList orbitalParameters) {

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
            final RealMatrix dYdY0 = harvester.getStateTransitionMatrix(state);
            final RealMatrix dYdP  = harvester.getParametersJacobian(state);
            for (int k = 0; k < stateSize; k++) {
                int column = 0;
                for (int j = 0; j < orbitalParameters.getNbParams(); ++j) {
                    final ParameterDriver driver = orbitalParameters.getDrivers().get(j);
                    if (driver.isSelected()) {
                        jacobian.setEntry(row + k, column++, dYdY0.getEntry(k, j) * driver.getScale());
                    }
                }
                if (dYdP != null) {
                    for (int j = 0; j < dYdP.getColumnDimension(); ++j) {
                        final String name = harvester.getJacobiansColumnsNames().get(j);
                        for (final ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {

                            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                                if (name.equals(span.getData())) {
                                    jacobian.setEntry(row + k, column++, dYdP.getEntry(k, j) * driver.getScale());
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}

