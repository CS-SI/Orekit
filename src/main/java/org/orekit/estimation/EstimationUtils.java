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
package org.orekit.estimation;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.UnivariateVectorFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;

/** Utility class for orbit determination.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class EstimationUtils {

    /** Private constructor for utility class.
     */
    private EstimationUtils() {
    }

    /** Differentiate a vector function using finite differences.
     * @param function function to differentiate
     * @param dimension dimension of the vector value of the function
     * @param nbPoints number of points used for finite differences
     * @param steps step size along each component of the function input parameter array
     * @return matrix function evaluating to the Jacobian of the original function
     */
    public static MultivariateMatrixFunction differentiate(final MultivariateVectorFunction function,
                                                           final int dimension,
                                                           final int nbPoints, final double ... steps) {
        return new MultivariateMatrixFunction() {

            @Override
            public double[][] value(final double[] parameter) {
                final double[][] jacobian = new double[dimension][steps.length];
                for (int j = 0; j < steps.length; ++j) {

                    // compute partial derivatives with respect to parameter component j
                    final int theJ = j;
                    final FiniteDifferencesDifferentiator differentiator =
                                    new FiniteDifferencesDifferentiator(nbPoints, steps[j]);
                    final UnivariateDifferentiableVectorFunction differentiatedJ =
                                    differentiator.differentiate(new UnivariateVectorFunction() {
                                        public double[] value(final double x) {
                                            final double savedComponent = parameter[theJ];
                                            parameter[theJ] += x;
                                            final double[] result = function.value(parameter);
                                            parameter[theJ] = savedComponent;
                                            return result;
                                        }
                                    });

                    final DerivativeStructure[] c =
                                    differentiatedJ.value(new DerivativeStructure(1, 1, 0, 0.0));

                    // populate the j-th column of the Jacobian
                    for (int i = 0; i < dimension; ++i) {
                        jacobian[i][j] = c[i].getPartialDerivative(1);
                    }

                }

                return jacobian;

            }

        };
    }

    /** Differentiate a vector function using finite differences.
     * @param function function to differentiate
     * @param dimension dimension of the vector value of the function
     * @param orbitType type used to map the orbit to a one dimensional array
     * @param positionAngle type of the position angle used for orbit mapping to array
     * @param dP user specified position error, used for step size computation for finite differences
     * @param nbPoints number of points used for finite differences
     * @return matrix function evaluating to the Jacobian of the original function
     */
    public static StateJacobian differentiate(final StateFunction function, final int dimension,
                                              final OrbitType orbitType, final PositionAngle positionAngle,
                                              final double dP, final int nbPoints) {
        return new StateJacobian() {

            @Override
            public double[][] value(final SpacecraftState state) throws OrekitException {
                try {
                    final double[] tolerances =
                            NumericalPropagator.tolerances(dP, state.getOrbit(), orbitType)[0];
                    final double[][] jacobian = new double[dimension][6];
                    for (int j = 0; j < 6; ++j) {

                        // compute partial derivatives with respect to state component j
                        final UnivariateVectorFunction componentJ =
                                new StateComponentFunction(j, function, state, orbitType, positionAngle);
                        final FiniteDifferencesDifferentiator differentiator =
                                new FiniteDifferencesDifferentiator(nbPoints, tolerances[j]);
                        final UnivariateDifferentiableVectorFunction differentiatedJ =
                                differentiator.differentiate(componentJ);

                        final DerivativeStructure[] c =
                                differentiatedJ.value(new DerivativeStructure(1, 1, 0, 0.0));

                        // populate the j-th column of the Jacobian
                        for (int i = 0; i < dimension; ++i) {
                            jacobian[i][j] = c[i].getPartialDerivative(1);
                        }

                    }

                    return jacobian;

                } catch (OrekitExceptionWrapper oew) {
                    throw oew.getException();
                }
            }

        };
    }

    /** Restriction of a {@link StateFunction} to a function of a single state component.
     */
    private static class StateComponentFunction implements UnivariateVectorFunction {

        /** Component index in the mapped orbit array. */
        private final int             index;

        /** State-dependent function. */
        private final StateFunction   f;

        /** Type used to map the orbit to a one dimensional array. */
        private final OrbitType       orbitType;

        /** Tpe of the position angle used for orbit mapping to array. */
        private final PositionAngle   positionAngle;

        /** Base state, of which only one component will change. */
        private final SpacecraftState baseState;

        /** Simple constructor.
         * @param index component index in the mapped orbit array
         * @param f state-dependent function
         * @param baseState base state, of which only one component will change
         * @param orbitType type used to map the orbit to a one dimensional array
         * @param positionAngle type of the position angle used for orbit mapping to array
         */
        StateComponentFunction(final int index, final StateFunction f,
                               final SpacecraftState baseState,
                               final OrbitType orbitType, final PositionAngle positionAngle) {
            this.index         = index;
            this.f             = f;
            this.orbitType     = orbitType;
            this.positionAngle = positionAngle;
            this.baseState     = baseState;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final double x) throws OrekitExceptionWrapper {
            try {
                final double[] array = new double[6];
                orbitType.mapOrbitToArray(baseState.getOrbit(), positionAngle, array);
                array[index] += x;
                final SpacecraftState state =
                        new SpacecraftState(orbitType.mapArrayToOrbit(array,
                                                                      positionAngle,
                                                                      baseState.getDate(),
                                                                      baseState.getMu(),
                                                                      baseState.getFrame()),
                                                                      baseState.getAttitude(),
                                                                      baseState.getMass());
                return f.value(state);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

}


