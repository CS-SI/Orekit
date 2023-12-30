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
package org.orekit.propagation.integration;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Provider for additional derivatives.
 *
 * <p>
 * In some cases users may need to integrate some problem-specific equations along with
 * classical spacecraft equations of motions. One example is optimal control in low
 * thrust where adjoint parameters linked to the minimized Hamiltonian must be integrated.
 * Another example is formation flying or rendez-vous which use the Clohessy-Whiltshire
 * equations for the relative motion.
 * </p>
 * <p>
 * This interface allows users to add such equations to a {@link
 * org.orekit.propagation.numerical.NumericalPropagator numerical propagator} or a {@link
 * org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSST propagator}. Users provide the
 * equations as an implementation of this interface and register it to the propagator thanks to
 * its {@link AbstractIntegratedPropagator#addAdditionalDerivativesProvider(AdditionalDerivativesProvider)}
 * method. Several such objects can be registered with each numerical propagator, but it is
 * recommended to gather in the same object the sets of parameters which equations can interact
 * on each others states.
 * </p>
 * <p>
 * This interface is the numerical (read not already integrated) counterpart of
 * the {@link org.orekit.propagation.AdditionalStateProvider} interface.
 * It allows to append various additional state parameters to any {@link
 * org.orekit.propagation.numerical.NumericalPropagator numerical propagator} or {@link
 * org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSST propagator}.
 * </p>
 * @see org.orekit.propagation.integration.AbstractIntegratedPropagator
 * @author Luc Maisonobe
 * @since 11.1
 */
public interface AdditionalDerivativesProvider {

    /** Get the name of the additional derivatives (which will become state once integrated).
     * @return name of the additional state (names containing "orekit"
     * with any case are reserved for the library internal use)
     */
    String getName();

    /** Get the dimension of the generated derivative.
     * @return dimension of the generated
     */
    int getDimension();

    /** Initialize the generator at the start of propagation.
     * @param initialState initial state information at the start of propagation
     * @param target       date of propagation
     */
    default void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // nothing by default
    }

    /** Check if this provider should yield so another provider has an opportunity to add missing parts.
     * <p>
     * Decision to yield is often based on an additional state being {@link SpacecraftState#hasAdditionalState(String)
     * already available} in the provided {@code state} (but it could theoretically also depend on
     * an additional state derivative being {@link SpacecraftState#hasAdditionalStateDerivative(String)
     * already available}, or any other criterion). If for example a provider needs the state transition
     * matrix, it could implement this method as:
     * </p>
     * <pre>{@code
     * public boolean yields(final SpacecraftState state) {
     *     return !state.getAdditionalStates().containsKey("STM");
     * }
     * }</pre>
     * <p>
     * The default implementation returns {@code false}, meaning that derivative data can be
     * {@link #combinedDerivatives(SpacecraftState) computed} immediately.
     * </p>
     * @param state state to handle
     * @return true if this provider should yield so another provider has an opportunity to add missing parts
     * as the state is incrementally built up
     */
    default boolean yields(SpacecraftState state) {
        return false;
    }

    /** Compute the derivatives related to the additional state (and optionally main state increments).
     * @param s current state information: date, kinematics, attitude, and
     * additional states this equations depend on (according to the
     * {@link #yields(SpacecraftState) yields} method)
     * @return computed combined derivatives, which may include some incremental
     * coupling effect to add to main state derivatives
     * @since 11.2
     */
    CombinedDerivatives combinedDerivatives(SpacecraftState s);

}
