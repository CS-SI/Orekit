/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.analysis.MultivariateVectorFunction;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Propagator converter using finite differences to compute the Jacobian.
 * @author Pascal Parraud
 * @since 6.0
 */
public class FiniteDifferencePropagatorConverter extends AbstractPropagatorConverter {

    /** Propagator builder. */
    private final PropagatorBuilder builder;

    /** Simple constructor.
     * @param factory builder for adapted propagator
     * @param threshold absolute threshold for optimization algorithm
     * @param maxIterations maximum number of iterations for fitting
     */
    public FiniteDifferencePropagatorConverter(final PropagatorBuilder factory,
                                               final double threshold,
                                               final int maxIterations) {
        super(factory, threshold, maxIterations);
        this.builder = factory;
    }

    /** {@inheritDoc} */
    protected MultivariateVectorFunction getObjectiveFunction() {
        return new ObjectiveFunction();
    }

    /** {@inheritDoc} */
    protected MultivariateJacobianFunction getModel() {
        return new ObjectiveFunctionJacobian();
    }

    /** Internal class for computing position/velocity at sample points. */
    private class ObjectiveFunction implements MultivariateVectorFunction {

        /** {@inheritDoc} */
        public double[] value(final double[] arg)
            throws IllegalArgumentException, OrekitException {
            final Propagator propagator = builder.buildPropagator(arg);
            final double[] eval = new double[getTargetSize()];
            int k = 0;
            for (SpacecraftState state : getSample()) {
                final PVCoordinates pv = propagator.getPVCoordinates(state.getDate(), getFrame());
                if (Double.isNaN(pv.getMomentum().getNorm())) {
                    propagator.getPVCoordinates(state.getDate(), getFrame());
                }
                eval[k++] = pv.getPosition().getX();
                eval[k++] = pv.getPosition().getY();
                eval[k++] = pv.getPosition().getZ();
                if (!isOnlyPosition()) {
                    eval[k++] = pv.getVelocity().getX();
                    eval[k++] = pv.getVelocity().getY();
                    eval[k++] = pv.getVelocity().getZ();
                }
            }

            return eval;

        }
    }

    /** Internal class for computing position/velocity Jacobian at sample points. */
    private class ObjectiveFunctionJacobian implements MultivariateJacobianFunction {

        /** {@inheritDoc} */
        public Pair<RealVector, RealMatrix> value(final RealVector point)
            throws IllegalArgumentException, OrekitException {

            final double[] arg = point.toArray();
            final MultivariateVectorFunction f = new ObjectiveFunction();

            final double[][] jacob = new double[getTargetSize()][arg.length];
            final double[] eval = f.value(arg);
            final double[] arg1 = new double[arg.length];
            for (int j = 0; j < arg.length; j++) {
                System.arraycopy(arg, 0, arg1, 0, arg.length);
                arg1[j] += 1;
                final double[] eval1 = f.value(arg1);
                for (int t = 0; t < eval.length; t++) {
                    jacob[t][j] = eval1[t] - eval[t];
                }
            }

            return new Pair<>(MatrixUtils.createRealVector(eval),
                    MatrixUtils.createRealMatrix(jacob));

        }

    }

}

