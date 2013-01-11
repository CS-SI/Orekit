/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Propagator converter using finite differences to compute the jacobian.
 * @author Pascal Parraud
 * @since 6.0
 */
public class FiniteDifferencePropagatorConverter extends AbstractPropagatorConverter {

    /** Function computing position/velocity at sample points. */
    private final ObjectiveFunction objectiveFunction;

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
        this.objectiveFunction = new ObjectiveFunction();
    }

    /** {@inheritDoc} */
    protected DifferentiableMultivariateVectorFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    /** Internal class for computing position/velocity at sample points. */
    private class ObjectiveFunction implements DifferentiableMultivariateVectorFunction {

        /** {@inheritDoc} */
        public double[] value(final double[] arg)
            throws IllegalArgumentException, OrekitExceptionWrapper {
            try {
                final Propagator propagator = builder.buildPropagator(getDate(), arg);
                final double[] eval = new double[getTargetSize()];
                int k = 0;
                for (SpacecraftState state : getSample()) {
                    final PVCoordinates pv = propagator.getPVCoordinates(state.getDate(), getFrame());
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

            } catch (OrekitException ex) {
                throw new OrekitExceptionWrapper(ex);
            }
        }

        /** {@inheritDoc} */
        public MultivariateMatrixFunction jacobian() {
            return new MultivariateMatrixFunction() {

                /** {@inheritDoc} */
                public double[][] value(final double[] arg)
                    throws IllegalArgumentException, OrekitExceptionWrapper {
                    final double[][] jacob = new double[getTargetSize()][arg.length];
                    final double[] eval = ObjectiveFunction.this.value(arg);
                    final double[] arg1 = new double[arg.length];
                    double increment = 0;
                    for (int j = 0; j < arg.length; j++) {
                        System.arraycopy(arg, 0, arg1, 0, arg.length);
                        increment = FastMath.sqrt(Precision.EPSILON) * FastMath.abs(arg[j]);
                        if (increment <= Precision.SAFE_MIN) {
                            increment = FastMath.sqrt(Precision.EPSILON);
                        }
                        arg1[j] += increment;
                        final double[] eval1 = ObjectiveFunction.this.value(arg1);
                        for (int t = 0; t < eval.length; t++) {
                            jacob[t][j] = (eval1[t] - eval[t]) / increment;
                        }
                    }

                    return jacob;
                }

            };
        }

    }

}

