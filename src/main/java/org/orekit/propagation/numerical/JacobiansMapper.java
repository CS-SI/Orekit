/* Copyright 2010-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.Serializable;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.QRDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * OrekitStepInterpolator#getInterpolatedAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see org.orekit.propagation.numerical.PartialDerivativesEquations
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see org.orekit.propagation.sampling.OrekitStepInterpolator#getInterpolatedAdditionalState(String)
 * @see AbstractPropagator
 */
public class JacobiansMapper implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2125324068169838971L;

    /** Name. */
    private String name;

    /** State vector dimension without additional parameters
     * (either 6 or 7 depending on mass being included or not). */
    private final int stateDimension;

    /** Number of Parameters. */
    private final int parameters;

    /** Orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param stateDimension dimension of the state (either 6 or 7 depending on mass
     * being included or not)
     * @param parameters number of parameters
     * @param orbitType orbit type
     * @param angleType position angle type
     */
    JacobiansMapper(final String name, final int stateDimension, final int parameters,
                    final OrbitType orbitType, final PositionAngle angleType) {
        this.name           = name;
        this.stateDimension = stateDimension;
        this.parameters     = parameters;
        this.orbitType      = orbitType;
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
    private double[][] getdYdC(final SpacecraftState state) {

        final double[][] dYdC = new double[stateDimension][stateDimension];

        // make sure the state is in the desired orbit type
        final Orbit orbit = orbitType.convertType(state.getOrbit());

        // compute the Jacobian, taking the position angle type into account
        orbit.getJacobianWrtCartesian(angleType, dYdC);
        for (int i = 6; i < stateDimension; ++i) {
            dYdC[i][i] = 1.0;
        }

        return dYdC;

    }

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method converts the Jacobians to cartesian parameters and put the converted data
     * in the one-dimensional {@code p} array.
     * </p>
     * @param state spacecraft state
     * @param dY1dY0 Jacobian of current state at time t<sub>1</sub>
     * with respect to state at some previous time t<sub>0</sub>
     * @param dY1dP Jacobian of current state at time t<sub>1</sub>
     * with respect to parameters (may be null if there are no parameters)
     * @param p placeholder where to put the one-dimensional additional state
     * @see #getStateJacobian(double[], double[][])
     */
    void setInitialJacobians(final SpacecraftState state,final double[][] dY1dY0,
                             final double[][] dY1dP, final double[] p) {

        // set up a converter between state parameters and cartesian parameters
        final RealMatrix dY1dC1 = new Array2DRowRealMatrix(getdYdC(state), false);
        final DecompositionSolver solver = new QRDecompositionImpl(dY1dC1).getSolver();

        // convert the provided state Jacobian to cartesian parameters
        final RealMatrix dC1dY0 = solver.solve(new Array2DRowRealMatrix(dY1dY0, false));

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < stateDimension; ++i) {
            for (int j = 0; j < stateDimension; ++j) {
                p[index++] = dC1dY0.getEntry(i, j);
            }
        }

        if (parameters > 0) {
            // convert the provided state Jacobian to cartesian parameters
            final RealMatrix dC1dP = solver.solve(new Array2DRowRealMatrix(dY1dP, false));

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
     * This method extract the data from the {@code p} array and put it in the
     * {@code dYdY0} array.
     * </p>
     * @param state spacecraft state
     * @param p one-dimensional additional state (generally retrieved from {link
     * org.orekit.propagation.sampling.OrekitStepInterpolator#getInterpolatedAdditionalState(String)
     * OrekitStepInterpolator.getInterpolatedAdditionalState(getName())}
     * @param dYdY0 placeholder where to put the Jacobian with respect to state
     * @see #getParametersJacobian(double[], double[][])
     * @see org.orekit.propagation.sampling.OrekitStepInterpolator#getInterpolatedAdditionalState(String)
     */
    public void getStateJacobian(final SpacecraftState state, final double[] p, final double[][] dYdY0) {

        // get the conversion Jacobian between state parameters and cartesian parameters
        final double[][] dYdC = getdYdC(state);

        // compute dYdY0 = dYdC * dCdY0, without allocating new arrays
        for (int i = 0; i < stateDimension; i++) {
            final double[] rowC = dYdC[i];
            final double[] rowD = dYdY0[i];
            for (int j = 0; j < stateDimension; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < stateDimension; ++k) {
                    sum += rowC[k] * p[pIndex];
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
     * @param p one-dimensional additional state (generally retrieved from {link
     * org.orekit.propagation.sampling.OrekitStepInterpolator#getInterpolatedAdditionalState(String)
     * OrekitStepInterpolator.getInterpolatedAdditionalState(getName())}
     * @param dYdP placeholder where to put the Jacobian with respect to parameters
     * @see #getStateJacobian(double[], double[][])
     */
    public void getParametersJacobian(final SpacecraftState state, final double[] p, final double[][] dYdP) {

        if (parameters > 0) {

            // get the conversion Jacobian between state parameters and cartesian parameters
            final double[][] dYdC = getdYdC(state);

            // compute dYdP = dYdC * dCdP, without allocating new arrays
            for (int i = 0; i < stateDimension; i++) {
                final double[] rowC = dYdC[i];
                final double[] rowD = dYdP[i];
                for (int j = 0; j < parameters; ++j) {
                    double sum = 0;
                    int pIndex = j + stateDimension * stateDimension;
                    for (int k = 0; k < stateDimension; ++k) {
                        sum += rowC[k] * p[pIndex];
                        pIndex += parameters;
                    }
                    rowD[j] = sum;
                }
            }

        }

    }

}
