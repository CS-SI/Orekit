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
package org.orekit.propagation;

import java.io.Serializable;

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
 * @see AnalyticalPropagator
 */
public class JacobiansMapper implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6775664011040390270L;

    /** State vector dimension without additional parameters
     * (either 6 or 7 depending on mass being included or not). */
    private final int stateDimension;

    /** Number of Parameters. */
    private final int parameters;

    /** Simple constructor.
     * @param stateDimension dimension of the state (either 6 or 7 depending on mass
     * being included or not)
     * @param parameters number of parameters
     */
    public JacobiansMapper(final int stateDimension, final int parameters) {
        this.stateDimension = stateDimension;
        this.parameters     = parameters;
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

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code dYdY0} array and put it in the
     * {@code p} array.
     * </p>
     * @param dYdY0 Jacobian with respect to state
     * @param p placeholder where to put the one-dimensional additional state
     * @see #getStateJacobian(double[], double[][])
     * @see #setParametersJacobian(double[][], double[])
     */
    public void setStateJacobian(final double[][] dYdY0, final double[] p) {

        int index = 0;
        for (int i = 0; i < stateDimension; i++) {
            System.arraycopy(dYdY0[i], 0, p, index, stateDimension);
            index += stateDimension;
        }

    }

    /** Get the Jacobian with respect to state from a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code p} array and put it in the
     * {@code dYdY0} array.
     * </p>
     * @param p one-dimensional additional state
     * @param dYdY0 placeholder where to put the Jacobian with respect to state
     * @see #setStateJacobian(double[][], double[])
     * @see #getParametersJacobian(double[], double[][])
     */
    public void getStateJacobian(final double[] p, final double[][] dYdY0) {

        int index = 0;
        for (int i = 0; i < stateDimension; i++) {
            System.arraycopy(p, index, dYdY0[i], 0, stateDimension);
            index += stateDimension;
        }

    }

    /** Set theJacobian with respect to parameters into a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code dYdP} array and put it in the
     * {@code p} array.
     * </p>
     * <p>
     * If the number of parameters is zero, the method returns immediately and
     * does not reference {@code dYdP} which can safely be null in this case.
     * </p>
     * @param dYdP placeholder where to put the Jacobian with respect to parameters
     * @param p one-dimensional additional state
     * @see #setStateJacobian(double[][], double[])
     * @see #getParametersJacobian(double[], double[][])
     */
    public void setParametersJacobian(final double[][] dYdP, final double[] p) {

        if (parameters == 0) {
            return;
        }

        int index = stateDimension * stateDimension;
        for (int i = 0; i < stateDimension; i++) {
            System.arraycopy(dYdP[i], 0, p, index, parameters);
            index += parameters;
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
     * @param p one-dimensional additional state
     * @param dYdP placeholder where to put the Jacobian with respect to parameters
     * @see #setParametersJacobian(double[][], double[])
     * @see #getStateJacobian(double[], double[][])
     */
    public void getParametersJacobian(final double[] p, final double[][] dYdP) {

        if (parameters == 0) {
            return;
        }

        int index = stateDimension * stateDimension;
        for (int i = 0; i < stateDimension; i++) {
            System.arraycopy(p, index, dYdP[i], 0, parameters);
            index += parameters;
        }

    }

}
