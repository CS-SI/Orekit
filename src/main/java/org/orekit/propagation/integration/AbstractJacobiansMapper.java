/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.integration;

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriversList;

/** Base class for jacobian mapper.
 * @author Bryan Cazabonne
 * @since 10.0
 */
public abstract class AbstractJacobiansMapper implements MatricesHarvester {

    /** State dimension, fixed to 6.
     * @since 9.0
     */
    public static final int STATE_DIMENSION = 6;

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @deprecated since 11.1 replaced by {@link #AbstractJacobiansMapper(String, int)}
     */
    @Deprecated
    protected AbstractJacobiansMapper(final String name, final ParameterDriversList parameters) {
        this.name       = name;
        this.parameters = parameters;
    }

    /** Get the name of the partial Jacobians.
     * @return name of the Jacobians
     */
    public String getName() {
        return name;
    }

    /** Get the number of parameters.
     * @return number of parameters
     */
    public int getParameters() {
        return parameters.getNbParams();
    }

    /** Compute the length of the one-dimensional additional state array needed.
     * @return length of the one-dimensional additional state array
     */
    public int getAdditionalStateDimension() {
        return STATE_DIMENSION * (STATE_DIMENSION + parameters.getNbParams());
    }

    /** Not used anymore.
     * @param s spacecraft state
     * @deprecated as of 11.1, not used anymore
     */
    @Deprecated
    public void analyticalDerivatives(final SpacecraftState s) {
        // nothing by default
    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {
        // nothing by default
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final double[][] dYdY0 = new double[STATE_DIMENSION][STATE_DIMENSION];
        getStateJacobian(s, dYdY0);
        return new Array2DRowRealMatrix(dYdY0, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState s) {
        if (getParameters() == 0) {
            return null;
        } else {
            final double[][] dYdP = new double[STATE_DIMENSION][getParameters()];
            getParametersJacobian(s, dYdP);
            return new Array2DRowRealMatrix(dYdP, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        return parameters.getDrivers().stream().map(d -> d.getName()).collect(Collectors.toList());
    }

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * @param state spacecraft state
     * @param dY1dY0 Jacobian of current state at time t₁
     * with respect to state at some previous time t₀
     * @param dY1dP Jacobian of current state at time t₁
     * with respect to parameters (may be null if there are no parameters)
     * @param p placeholder where to put the one-dimensional additional state
     * @see #getStateJacobian(SpacecraftState, double[][])
     */
    public abstract void setInitialJacobians(SpacecraftState state, double[][] dY1dY0, double[][] dY1dP, double[] p);

    /** Get the Jacobian with respect to state from a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code state} and put it in the
     * {@code dYdY0} array.
     * <p>
     * @param state spacecraft state
     * @param dYdY0 placeholder where to put the Jacobian with respect to state
     * @see #getParametersJacobian(SpacecraftState, double[][])
     */
    public abstract void getStateJacobian(SpacecraftState state, double[][] dYdY0);

    /** Get the Jacobian with respect to parameters from a one-dimensional additional state array.
     * <p>
     * This method extract the data from the {@code state} and put it in the
     * {@code dYdP} array.
     * </p>
     * <p>
     * If no parameters have been set in the constructor, the method returns immediately and
     * does not reference {@code dYdP} which can safely be null in this case.
     * </p>
     * @param state spacecraft state
     * @param dYdP placeholder where to put the Jacobian with respect to parameters
     * @see #getStateJacobian(SpacecraftState, double[][])
     */
    public abstract void getParametersJacobian(SpacecraftState state, double[][] dYdP);

}
