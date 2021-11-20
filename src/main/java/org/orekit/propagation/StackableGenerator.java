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
package org.orekit.propagation;

/** This interface represents one element of a stack used to build up {@link SpacecraftState states}
 * incrementally.
 * <p>
 * {@link Propagator Propagators} generate {@link SpacecraftState states} that contain at
 * least orbit, attitude, and mass. These states may however also contain {@link
 * SpacecraftState#addAdditionalState(String, double...) additional states} and {@link
 * SpacecraftState#addAdditionalStateDerivative(String, double...) derivatives}. Instances of classes
 * implementing this interface are intended to be registered to propagators so they can add these
 * additional states and derivatives incrementally after having computed the basic components
 * (orbit, attitude and mass).
 * </p>
 * <p>
 * Some additional states or derivatives may depend on previous additional states or derivatives
 * to be already available the before they can be computed. As an example, lets consider Jacobians
 * computation. We may managed the state transition matrix ∂y/∂y₀ as one additional state and
 * each column of the Jacobian matrix ∂y/∂pₘ corresponding to the partial derivatives of state
 * with respect to propagation parameter pₘ (drag coefficient, thrust, coefficient of a parametric
 * acceleration…) as separate additional states. Then the generators managing the time derivatives
 * d(∂y/∂pₘ)/dt depend on the already integrated state transition matrix ∂y/∂y₀ to be available.
 * The propagator builds the complete state incrementally, looping over the registered generators,
 * calling their {@link #generate(SpacecraftState) generate} method and pushing the generated data
 * into the state before iterating to next generator. At each iteration, it uses the {@link
 * #yield(SpacecraftState) yield} method of the candidate generator to check if its {@link
 * #generate(SpacecraftState) generate} method can be called at this iteration or if it should be
 * postponed later in the loop. This allows to satisfy dependencies requirements easily.
 * </p>
 * <p>
 * It is possible that at some stages in the propagation, a subset of the generators registered to a
 * propagator all yied and cannot {@link #generate(SpacecraftState) generate} their additional
 * state or derivative. This happens for example when some additional states are must be integrated.
 * They are managed as secondary equations in the ODE integrator, and initialized after the primary
 * equations (which correspond to orbit) have been initialized. So when the primary
 * equation are initialized, the generators that depend on the secondary state will all yield. This
 * behavior is expected. Another case occurs when users set up additional states that induces
 * dependency loop (state A depending on state B which depends on state C which depends on state A).
 * In this case, the three corresponding generators will wait for each other and indefinitely yield.
 * This second case is not normal. The propagator cannot know it in advance if as subset of generators
 * that all yield is normal or not. So at propagator level, when such a situation is detected, the
 * propagator just give up and returns the most complete state it was able to compute, without
 * generating any error. Errors will indeed not be triggered in the first case (because once the
 * primary equations have been initialized, the secondary equations will be initialized too), and
 * they will be triggered in the second case as soon as user attempts to retrieve an additional
 * state or derivative that was not added.
 * </p>
 * @see Propagator
 * @author Luc Maisonobe
 * @since 11.1
 */
public interface StackableGenerator {

    /** Get the name of the additional state or derivative {@link #generate(SpacecraftState) generated}
     * by the instance.
     * @return name of the additional state or derivative
     */
    String getName();

    /** Check if the generated data is a closed form state or a derivative that needs to be integrated.
     * @return true if the array {@link #generate(SpacecraftState) generate} returns should be
     * {@link SpacecraftState#addAdditionalState(String, double...) added as a state}, and false if it should be
     * {@link SpacecraftState#addAdditionalStateDerivative(String, double...) added as a derivative}.
     */
    default boolean isClosedForm() {
        return true;
    }

    /** Check if this generator should yield so another generator has an opportunity to add missing parts.
     * <p>
     * Decision to yield is often based on an additional state being {@link SpacecraftState#hasAdditionalState(String)
     * already available} in the provided {@code state} (but it could theoretically also depend on
     * an additional state derivative being {@link SpacecraftState#hasAdditionalStateDerivative(String)
     * already available}, or any other criterion). If for example a generator needs the state transition
     * matrix, it could implement this method as:
     * </p>
     * <pre>{@code
     * public boolean yield(final SpacecraftState state) {
     *     return !state.getAdditionalStates().containsKey("STM");
     * }
     * }</pre>
     * <p>
     * The default implementation returns {@code false}, meaning that state or derivative data can be
     * {@link #generate(SpacecraftState) generated} immediately.
     * </p>
     * @param state state to handle
     * @return true if this generator should yield so another generator has an opportunity to add missing parts
     * as the state is incrementally built up
     */
    default boolean yield(SpacecraftState state) {
        return false;
    }

    /** Generate an additional state or derivative.
     * @param state original state to use
     * @return generated state or derivative
     * @see #isClosedForm()
     */
    double[] generate(SpacecraftState state);

}
