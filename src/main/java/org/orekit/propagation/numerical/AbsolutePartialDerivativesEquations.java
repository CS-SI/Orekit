/* Copyright 2002-2020 CS GROUP
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.utils.ParameterDriver;

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
 * @author Vincent Mouraux
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AbsolutePartialDerivativesEquations extends PartialDerivativesEquations {

    /** Name. */
    private final String name;

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public AbsolutePartialDerivativesEquations(final String name, final NumericalPropagator propagator) {
        super(name, propagator);
        this.name = name;
    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    @Override
    public AbsoluteJacobiansMapper getMapper() {
        if (!isInitialize()) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new AbsoluteJacobiansMapper(name, getSelectedParameters());
    }

}

