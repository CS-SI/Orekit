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
package org.orekit.propagation.numerical;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.linear.Array2DRowFieldMatrix;
import org.hipparchus.linear.FieldDecompositionSolver;
import org.hipparchus.linear.FieldLUDecomposition;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.FieldOrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * FieldSpacecraftState<T>#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see org.orekit.propagation.numerical.PartialDerivativesEquations
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see FieldSpacecraftState<T>#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class FieldJacobiansMapper<T extends RealFieldElement<T>> {

    /** Name. */
    private String name;

    /** State vector dimension without additional parameters
     * (either 6 or 7 depending on mass being included or not). */
    private final int stateDimension;

    /** Number of Parameters. */
    private final int parameters;

    /** FieldOrbit<T> type. */
    private final FieldOrbitType fieldOrbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /**Field used by default.*/
    private final Field<T> field;
    /** Simple constructor.
     * @param field Field used by default
     * @param name name of the Jacobians
     * @param stateDimension dimension of the state (either 6 or 7 depending on mass
     * being included or not)
     * @param parameters number of parameters
     * @param FieldOrbitType FieldOrbit<T> type
     * @param angleType position angle type
     */
    FieldJacobiansMapper(final Field<T> field, final String name, final int stateDimension, final int parameters,
                    final FieldOrbitType FieldOrbitType, final PositionAngle angleType) {
        this.field          = field;
        this.name           = name;
        this.stateDimension = stateDimension;
        this.parameters     = parameters;
        this.fieldOrbitType      = FieldOrbitType;
        this.angleType      = angleType;
    }

    /** Get the name of the partial Jacobians.
     * @return name of the Jacobians
     */
    public String getName() {
        return name;
    }

    /** Compute the length of the one-dimensional additional state array needed.
     * @return length of the one-dimensional additional state array
     */
    public int getAdditionalStateDimension() {
        return stateDimension * (stateDimension + parameters);
    }

    /** Get the state vector dimension.
     * @return state vector dimension
     */
    public int getStateDimension() {
        return stateDimension;
    }

    /** Get the number of parameters.
     * @return number of parameters
     */
    public int getParameters() {
        return parameters;
    }

    /** Get the conversion Jacobian between state parameters and cartesian parameters.
     * @param state spacecraft state
     * @return conversion Jacobian
     */
    private T[][] getdYdC(final FieldSpacecraftState<T> state) {

        final T[][] dYdC = MathArrays.buildArray(field, stateDimension, stateDimension);

        // make sure the state is in the desired FieldOrbit<T> type
        final FieldOrbit<T> fieldOrbit = fieldOrbitType.convertType(state.getOrbit());

        // compute the Jacobian, taking the position angle type into account
        fieldOrbit.getJacobianWrtCartesian(angleType, dYdC);
        for (int i = 6; i < stateDimension; ++i) {
            dYdC[i][i] = field.getOne();
        }

        return dYdC;

    }

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method converts the Jacobians to cartesian parameters and put the converted data
     * in the one-dimensional {@code p} array.
     * </p>
     * @param state spacecraft state
     * @param dY1dY0 Jacobian of current state at time t₁
     * with respect to state at some previous time t₀
     * @param dY1dP Jacobian of current state at time t₁
     * with respect to parameters (may be null if there are no parameters)
     * @param p placeholder where to put the one-dimensional additional state
     * @see #getStateJacobian(FieldSpacecraftState<T>, double[][])
     */
    void setInitialJacobians(final FieldSpacecraftState<T> state, final T[][] dY1dY0,
                             final T[][] dY1dP, final T[] p) {

        // set up a converter between state parameters and cartesian parameters
        final FieldMatrix<T> dY1dC1 = new Array2DRowFieldMatrix<T>(getdYdC(state), false);
        final FieldDecompositionSolver<T> solver = new FieldLUDecomposition<T>(dY1dC1).getSolver();

        // convert the provided state Jacobian to cartesian parameters
        final FieldMatrix<T> dC1dY0 = solver.solve(new Array2DRowFieldMatrix<T>(dY1dY0, false));

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < stateDimension; ++i) {
            for (int j = 0; j < stateDimension; ++j) {
                p[index++] = dC1dY0.getEntry(i, j);
            }
        }

        if (parameters > 0) {
            // convert the provided state Jacobian to cartesian parameters
            final FieldMatrix<T> dC1dP = solver.solve(new Array2DRowFieldMatrix<T>(dY1dP, false));

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < stateDimension; ++i) {
                for (int j = 0; j < parameters; ++j) {
                    p[index++] = dC1dP.getEntry(i, j);
                }
            }
        }

    }

    /** Get the Jacobian with respect to state from a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code state} and put it in the
     * {@code dYdY0} array.
     * </p>
     * @param state spacecraft state
     * @param dYdY0 placeholder where to put the Jacobian with respect to state
     * @exception OrekitException if state does not contain the Jacobian additional state
     * @see #getParametersJacobian(FieldSpacecraftState<T>, double[][])
     */
    public void getStateJacobian(final FieldSpacecraftState<T> state,  final T[][] dYdY0)
        throws OrekitException {

        // get the conversion Jacobian between state parameters and cartesian parameters
        final T[][] dYdC = getdYdC(state);

        // extract the additional state
        final T[] p = state.getAdditionalState(name);

        // compute dYdY0 = dYdC * dCdY0, without allocating new arrays
        for (int i = 0; i < stateDimension; i++) {
            final T[] rowC = dYdC[i];
            final T[] rowD = dYdY0[i];
            for (int j = 0; j < stateDimension; ++j) {
                T sum = field.getZero();
                int pIndex = j;
                for (int k = 0; k < stateDimension; ++k) {
                    sum = sum.add(rowC[k].multiply(p[pIndex]));
                    pIndex += stateDimension;
                }
                rowD[j] = sum;
            }
        }

    }

    /** Get theJacobian with respect to parameters from a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code p} array and put it in the
     * {@code dYdP} array.
     * </p>
     * <p>
     * If no parameters have been set in the constructor, the method returns immediately and
     * does not reference {@code dYdP} which can safely be null in this case.
     * </p>
     * @param state spacecraft state
     * @param dYdP placeholder where to put the Jacobian with respect to parameters
     * @exception OrekitException if state does not contain the Jacobian additional state
     * @see #getStateJacobian(FieldSpacecraftState<T>, double[][])
     */
    public void getParametersJacobian(final FieldSpacecraftState<T> state, final T[][] dYdP)
        throws OrekitException {

        if (parameters > 0) {

            // get the conversion Jacobian between state parameters and cartesian parameters
            final T[][] dYdC = getdYdC(state);

            // extract the additional state
            final T[] p = state.getAdditionalState(name);

            // compute dYdP = dYdC * dCdP, without allocating new arrays
            for (int i = 0; i < stateDimension; i++) {
                final T[] rowC = dYdC[i];
                final T[] rowD = dYdP[i];
                for (int j = 0; j < parameters; ++j) {
                    T sum = field.getZero();
                    int pIndex = j + stateDimension * stateDimension;
                    for (int k = 0; k < stateDimension; ++k) {
                        sum = sum.add(rowC[k].multiply(p[pIndex]));
                        pIndex += parameters;
                    }
                    rowD[j] = sum;
                }
            }

        }

    }

}
