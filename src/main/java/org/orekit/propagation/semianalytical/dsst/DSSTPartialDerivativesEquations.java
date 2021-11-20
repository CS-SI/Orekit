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
package org.orekit.propagation.semianalytical.dsst;

import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.IntegrableAdapter;
import org.orekit.propagation.integration.IntegrableGenerator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.utils.ParameterDriver;

/** Set of additional equations}computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automatically added to a {@link DSSTPropagator DSST propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state are dimension 6 (orbit only).
 * </p>
 * <p>
 * The partial derivatives with respect to force models parameters has a dimension
 * equal to the number of selected parameters. Parameters selection is implemented at
 * {@link DSSTForceModel DSST force models} level. Users must retrieve a {@link ParameterDriver
 * parameter driver} by looping on all drivers using {@link DSSTForceModel#getParametersDrivers()}
 * and then select it by calling {@link ParameterDriver#setSelected(boolean) setSelected(true)}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 * @deprecated as of 11.1, replaced by {@link DSSTPartialDerivatives}
 */
@Deprecated
public class DSSTPartialDerivativesEquations extends IntegrableAdapter {

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * DSSTPropagator#addIntegrableGenerator(IntegrableGenerator)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     */
    public DSSTPartialDerivativesEquations(final String name,
                                           final DSSTPropagator propagator,
                                           final PropagationType propagationType) {
        super(new DSSTPartialDerivatives(name, propagator, propagationType));
    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public DSSTJacobiansMapper getMapper() {
        return ((DSSTPartialDerivatives) getGenerator()).getMapper();
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState,
     * double[][], double[][])} with dYdY0 set to the identity matrix and dYdP set
     * to a zero matrix.
     * </p>
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so proper matrices dimensions are used.
     * </p>
     * @param s0 initial state
     * @return state with initial Jacobians added
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) {
        return ((DSSTPartialDerivatives) getGenerator()).setInitialJacobians(s0);
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * The returned state must be added to the propagator (it is not done
     * automatically, as the user may need to add more states to it).
     * </p>
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, and the {@code dY1dP} matrix dimension <em>must</em>
     * be consistent with the selection.
     * </p>
     * @param s1 current state
     * @param dY1dY0 Jacobian of current state at time t₁ with respect
     * to state at some previous time t₀ (must be 6x6)
     * @param dY1dP Jacobian of current state at time t₁ with respect
     * to parameters (may be null if no parameters are selected)
     * @return state with initial Jacobians added
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
                                               final double[][] dY1dY0, final double[][] dY1dP) {
        return ((DSSTPartialDerivatives) getGenerator()).setInitialJacobians(s1, dY1dY0, dY1dP);
    }

}
