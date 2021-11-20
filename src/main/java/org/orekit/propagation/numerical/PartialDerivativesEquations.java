/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.integration.IntegrableAdapter;
import org.orekit.propagation.integration.IntegrableGenerator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automatically added to a {@link NumericalPropagator numerical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state can be either dimension 6
 * (orbit only) or 7 (orbit and mass).
 * </p>
 * <p>
 * The partial derivatives with respect to force models parameters has a dimension
 * equal to the number of selected parameters. Parameters selection is implemented at
 * {@link ForceModel force models} level. Users must retrieve a {@link ParameterDriver
 * parameter driver} using {@link ForceModel#getParameterDriver(String)} and then
 * select it by calling {@link ParameterDriver#setSelected(boolean) setSelected(true)}.
 * </p>
 * <p>
 * If several force models provide different {@link ParameterDriver drivers} for the
 * same parameter name, selecting any of these drivers has the side effect of
 * selecting all the drivers for this shared parameter. In this case, the partial
 * derivatives will be the sum of the partial derivatives contributed by the
 * corresponding force models. This case typically arises for central attraction
 * coefficient, which has an influence on {@link org.orekit.forces.gravity.NewtonianAttraction
 * Newtonian attraction}, {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel
 * gravity field}, and {@link org.orekit.forces.gravity.Relativity relativity}.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @deprecated as of 11.1, replaced by {@link PartialDerivatives}
 */
@Deprecated
public class PartialDerivativesEquations extends IntegrableAdapter {

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * NumericalPropagator#addIntegrableGenerator(IntegrableGenerator) addIntegrableGenerator}
     * method. So there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public PartialDerivativesEquations(final String name,
                                       final NumericalPropagator propagator) {
        super(new PartialDerivatives(name, propagator));
    }

    /** Get the selected parameters, in Jacobian matrix column order.
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so the proper list is returned.
     * </p>
     * @return selected parameters, in Jacobian matrix column order which
     * is lexicographic order
     */
    public ParameterDriversList getSelectedParameters() {
        return ((PartialDerivatives) getGenerator()).getSelectedParameters();
    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public JacobiansMapper getMapper() {
        return ((PartialDerivatives) getGenerator()).getMapper();
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
        return ((PartialDerivatives) getGenerator()).setInitialJacobians(s0);
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
        return ((PartialDerivatives) getGenerator()).setInitialJacobians(s1, dY1dY0, dY1dP);
    }

    /** Get the flag for the initialization of the state jacobian.
     * @return true if the state jacobian is initialized
     * @since 10.2
     */
    public boolean isInitialize() {
        return ((PartialDerivatives) getGenerator()).isInitialize();
    }

}
