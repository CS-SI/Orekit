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
package org.orekit.utils;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

/** Utility class for differentiating various kinds of functions.
 * @author Luc Maisonobe
 * @since 8.0
 */
public class Differentiation {

    /** Factory for the DerivativeStructure instances. */
    private static final DSFactory FACTORY = new DSFactory(1, 1);

    /** Private constructor for utility class.
     */
    private Differentiation() {
    }

    /** Differentiate a scalar function using finite differences.
     * @param function function to differentiate
     * @param nbPoints number of points used for finite differences
     * @param step step for finite differences, in <em>physical</em> units
     * @return scalar function evaluating to the derivative of the original function
     * @since 9.3
     */
    public static ParameterFunction differentiate(final ParameterFunction function,
                                                  final int nbPoints, final double step) {

        return new ParameterFunction() {

            /** Finite differences differentiator to use. */
            private final FiniteDifferencesDifferentiator differentiator  =
                            new FiniteDifferencesDifferentiator(nbPoints, step);

            /** {@inheritDoc} */
            @Override
            public double value(final ParameterDriver driver, final AbsoluteDate date) {

                final UnivariateFunction uf = new UnivariateFunction() {
                    /** {@inheritDoc} */
                    @Override
                    public double value(final double value) {
                        final double saved = driver.getValue(date);
                        driver.setValue(value, date);
                        final double functionValue = function.value(driver, date);
                        driver.setValue(saved, date);
                        return functionValue;
                    }
                };

                final DerivativeStructure dsParam = FACTORY.variable(0, driver.getValue(date));
                final DerivativeStructure dsValue = differentiator.differentiate(uf).value(dsParam);
                return dsValue.getPartialDerivative(1);

            }
        };

    }

    /** Differentiate a vector function using finite differences.
     * @param function function to differentiate
     * @param dimension dimension of the vector value of the function
     * @param provider attitude provider to use for modified states
     * @param orbitType type used to map the orbit to a one dimensional array
     * @param positionAngleType type of the position angle used for orbit mapping to array
     * @param dP user specified position error, used for step size computation for finite differences
     * @param nbPoints number of points used for finite differences
     * @return matrix function evaluating to the Jacobian of the original function
     */
    public static StateJacobian differentiate(final StateFunction function, final int dimension,
                                              final AttitudeProvider provider,
                                              final OrbitType orbitType, final PositionAngleType positionAngleType,
                                              final double dP, final int nbPoints) {
        return new StateJacobian() {

            @Override
            public double[][] value(final SpacecraftState state) {
                final double[] tolerances =
                        NumericalPropagator.tolerances(dP, state.getOrbit(), orbitType)[0];
                final double[][] jacobian = new double[dimension][6];
                for (int j = 0; j < 6; ++j) {

                    // compute partial derivatives with respect to state component j
                    final UnivariateVectorFunction componentJ =
                            new StateComponentFunction(j, function, provider, state,
                                                       orbitType, positionAngleType);
                    final FiniteDifferencesDifferentiator differentiator =
                            new FiniteDifferencesDifferentiator(nbPoints, tolerances[j]);
                    final UnivariateDifferentiableVectorFunction differentiatedJ =
                            differentiator.differentiate(componentJ);

                    final DerivativeStructure[] c = differentiatedJ.value(FACTORY.variable(0, 0.0));

                    // populate the j-th column of the Jacobian
                    for (int i = 0; i < dimension; ++i) {
                        jacobian[i][j] = c[i].getPartialDerivative(1);
                    }

                }

                return jacobian;

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
        private final PositionAngleType positionAngleType;

        /** Base state, of which only one component will change. */
        private final SpacecraftState baseState;

        /** Attitude provider to use for modified states. */
        private final AttitudeProvider provider;

        /** Simple constructor.
         * @param index component index in the mapped orbit array
         * @param f state-dependent function
         * @param provider attitude provider to use for modified states
         * @param baseState base state, of which only one component will change
         * @param orbitType type used to map the orbit to a one dimensional array
         * @param positionAngleType type of the position angle used for orbit mapping to array
         */
        StateComponentFunction(final int index, final StateFunction f,
                               final AttitudeProvider provider, final SpacecraftState baseState,
                               final OrbitType orbitType, final PositionAngleType positionAngleType) {
            this.index         = index;
            this.f             = f;
            this.provider      = provider;
            this.orbitType     = orbitType;
            this.positionAngleType = positionAngleType;
            this.baseState     = baseState;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final double x) {
            final double[] array = new double[6];
            final double[] arrayDot = new double[6];
            orbitType.mapOrbitToArray(baseState.getOrbit(), positionAngleType, array, arrayDot);
            array[index] += x;
            final Orbit orbit = orbitType.mapArrayToOrbit(array, arrayDot,
                    positionAngleType,
                                                          baseState.getDate(),
                                                          baseState.getMu(),
                                                          baseState.getFrame());
            final SpacecraftState state =
                    new SpacecraftState(orbit,
                                        provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
                                        baseState.getMass());
            return f.value(state);
        }

    }

}


