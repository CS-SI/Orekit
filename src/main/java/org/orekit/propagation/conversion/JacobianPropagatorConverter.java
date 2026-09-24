/* Copyright 2002-2026 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.orbits.OrbitalStateFactory;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.drivers.ParameterDriver;
import org.orekit.utils.drivers.ParameterDriversList;

/** Propagator converter using the real Jacobian.
 * @author Pascal Parraud
 * @since 6.0
 */
public class JacobianPropagatorConverter extends AbstractPropagatorConverter {

    /** Time step. */
    private static final double DT = 10.;

    /** Propagator builder. */
    private final PropagatorBuilder builder;

    /** Simple constructor.
     * @param builder builder for adapted propagator, it <em>must</em>
     * be configured to generate {@link OrbitParamsType#CARTESIAN} states
     * @param threshold absolute threshold for optimization algorithm
     * @param maxIterations maximum number of iterations for fitting
     */
    public JacobianPropagatorConverter(final PropagatorBuilder builder,
                                       final double threshold,
                                       final int maxIterations) {
        super(builder, threshold, maxIterations);
        final OrbitalStateFactory<?> factory = builder.getOrbitalStateFactory();
        if (factory.getOrbitParamsType() != OrbitParamsType.CARTESIAN) {
            throw new OrekitException(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED,
                                      factory.getOrbitParamsType(), OrbitParamsType.CARTESIAN);
        }
        this.builder = builder;
    }

    /** {@inheritDoc} */
    protected MultivariateVectorFunction getObjectiveFunction() {
        return point -> {
            final Propagator propagator  = builder.buildPropagator(point);
            final ValuesHandler handler = new ValuesHandler();
            propagator.getMultiplexer().add(handler);
            final List<SpacecraftState> sample = getSample();
            propagator.propagate(sample.getLast().getDate().shiftedBy(DT));
            return handler.value.toArray();
        };
    }

    /** {@inheritDoc} */
    protected MultivariateJacobianFunction getModel() {
        return point -> {
            final Propagator propagator  = builder.buildPropagator(point.toArray());
            final JacobianHandler handler = new JacobianHandler(propagator, point.getDimension());
            propagator.getMultiplexer().add(handler);
            final List<SpacecraftState> sample = getSample();
            propagator.propagate(sample.getLast().getDate().shiftedBy(DT));
            return new Pair<>(handler.getValue(), handler.jacobian);
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
        private final RealVector value;

        /** State size (3 or 6). */
        private final int stateSize;

        /** Number of the next measurement. */
        private int number;

        /** Index of the next component in the model. */
        private int index;

        /** Simple constructor.
         */
        ValuesHandler() {
            this.value = new ArrayRealVector(getTargetSize());
            this.stateSize = isOnlyPosition() ? 3 : 6;
        }

        /**
         * Getter for value.
         * @return value
         * @since 14.0
         */
        RealVector getValue() {
            return value;
        }

        /**
         * Getter for index.
         * @return index
         * @since 14.0
         */
        int getIndex() {
            return index;
        }

        /**
         * Getter for state size.
         * @return size
         * @since 14.0
         */
        int getStateSize() {
            return stateSize;
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

                fillRows(interpolator.getInterpolatedState(next.getDate()));

                // prepare handling of next measurement
                index += stateSize;
                ++number;

            }

        }

        /**
         * Fill vector.
         * @param state state
         */
        protected void fillRows(final SpacecraftState state) {
            final PVCoordinates pv = state.getPVCoordinates(getFrame());
            final Vector3D position = pv.getPosition();
            value.setEntry(index, position.getX());
            value.setEntry(index + 1, position.getY());
            value.setEntry(index + 2, position.getZ());
            if (!isOnlyPosition()) {
                final Vector3D velocity = pv.getVelocity();
                value.setEntry(index + 3, velocity.getX());
                value.setEntry(index + 4, velocity.getY());
                value.setEntry(index + 5, velocity.getZ());
            }
        }
    }

    /** Handler for picking up Jacobians at sample dates.
     * <p>
     * This class is heavily based on org.orekit.estimation.leastsquares.MeasurementHandler.
     * </p>
     * @since 11.1
     */
    private class JacobianHandler extends ValuesHandler {

        /** Jacobian matrix. */
        private final RealMatrix jacobian;

        /** Matrices harvester. */
        private final MatricesHarvester harvester;

        /** Simple constructor.
         * @param propagator propagator
         * @param columns number of columns of the Jacobian matrix
         */
        JacobianHandler(final Propagator propagator, final int columns) {
            super();
            this.jacobian  = MatrixUtils.createRealMatrix(getTargetSize(), columns);
            this.harvester = propagator.setupMatricesComputation("converter-partials", null, null);
        }

        /** Fill up a few Jacobian rows (either 6 or 3 depending on velocities used or not).
         * @param state spacecraft state
         */
        @Override
        protected void fillRows(final SpacecraftState state) {

            // value part
            super.fillRows(state);

            // Jacobian part
            final RealMatrix dYdY0 = harvester.getStateTransitionMatrix(state);
            final RealMatrix dYdP  = harvester.getParametersJacobian(state);
            final ParameterDriversList orbitalParameters = builder.getOrbitalStateFactory().getOrbitalParametersDrivers();
            for (int k = 0; k < getStateSize(); k++) {
                int column = 0;
                for (int j = 0; j < orbitalParameters.getNbParams(); ++j) {
                    final ParameterDriver driver = orbitalParameters.getDrivers().get(j);
                    if (driver.isSelected()) {
                        jacobian.setEntry(getIndex() + k, column++, dYdY0.getEntry(k, j) * driver.getScale());
                    }
                }
                if (dYdP != null) {
                    for (int j = 0; j < dYdP.getColumnDimension(); ++j) {
                        final String name = harvester.getJacobiansColumnsNames().get(j);
                        for (final ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
                            if (name.equals(driver.getName())) {
                                jacobian.setEntry(getIndex() + k, column++, dYdP.getEntry(k, j) * driver.getScale());
                            }
                        }
                    }
                }
            }
        }

    }

}

