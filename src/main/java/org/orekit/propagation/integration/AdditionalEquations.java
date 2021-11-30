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
package org.orekit.propagation.integration;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This interface allows users to add their own differential equations to a numerical propagator.
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
 * its {@link org.orekit.propagation.integration.AbstractIntegratedPropagator#addAdditionalEquations(AdditionalEquations)}
 * method. Several such objects can be registered with each numerical propagator, but it is
 * recommended to gather in the same object the sets of parameters which equations can interact
 * on each others states.
 * </p>
 * <p>
 * The additional parameters are gathered in a simple p array. The additional equations compute
 * the pDot array, which is the time-derivative of the p array. Since the additional parameters
 * p may also have an influence on the equations of motion themselves that should be accumulated
 * to the main state derivatives (for example an equation linked to a complex thrust model may
 * induce an acceleration and a mass change), the {@link #computeDerivatives(SpacecraftState, double[])
 * computeDerivatives} method can return a double array that will be
 * <em>added</em> to the main state derivatives. This means these equations can be used as an
 * additional force model if needed. If the additional parameters have no influence at all on
 * the main spacecraft state, a null reference may be returned.
 * </p>
 * <p>
 * This interface is the numerical (read not already integrated) counterpart of
 * the {@link org.orekit.propagation.AdditionalStateProvider} interface.
 * It allows to append various additional state parameters to any {@link
 * org.orekit.propagation.numerical.NumericalPropagator numerical propagator} or {@link
 * org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSST propagator}.
 * </p>
 * @see AbstractIntegratedPropagator
 * @see org.orekit.propagation.AdditionalStateProvider
 * @author Luc Maisonobe
 */
public interface AdditionalEquations {

    /** Get the name of the additional state.
     * @return name of the additional state (names containing "orekit"
     * with any case are reserved for the library internal use)
     */
    String getName();

    /** Get the dimension of the generated derivative.
     * @return dimension of the generated
     */
    default int getDimension() {
        // FIXME: as of 11.1 there is a default implementation that intentionally returns a wrong (negative) size
        // the default implementation should be removed in 12.0
        return -1;
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
     * public boolean yield(final SpacecraftState state) {
     *     return !state.getAdditionalStates().containsKey("STM");
     * }
     * }</pre>
     * <p>
     * The default implementation returns {@code false}, meaning that derivative data can be
     * {@link #derivatives(SpacecraftState) computed} immediately.
     * </p>
     * @param state state to handle
     * @return true if this provider should yield so another provider has an opportunity to add missing parts
     * as the state is incrementally built up
     * @since 11.1
     */
    default boolean yield(SpacecraftState state) {
        return false;
    }

    /**
     * Initialize the equations at the start of propagation.
     *
     * <p>
     * This method will be called once at propagation start,
     * before any calls to {@link #computeDerivatives(SpacecraftState, double[])}.
     * </p>
     *
     * <p>
     * The default implementation of this method does nothing.
     * </p>
     *
     * @param initialState initial state information at the start of propagation.
     * @param target       date of propagation. Not equal to {@code
     *                     initialState.getDate()}.
     */
    default void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // nothing by default
    }

    /** Compute the derivatives related to the additional state parameters.
     * <p>
     * When this method is called, the spacecraft state contains the main
     * state (orbit, attitude and mass), all the states provided through
     * the {@link org.orekit.propagation.AdditionalStateProvider additional
     * state providers} registered to the propagator, and the additional state
     * integrated using this equation. It does <em>not</em> contains any other
     * states to be integrated alongside during the same propagation.
     * </p>
     * @param s current state information: date, kinematics, attitude, and
     * additional state
     * @param pDot placeholder where the derivatives of the additional parameters
     * should be put
     * @return cumulative effect of the equations on the main state (may be null if
     * equations do not change main state at all)
     * @deprecated as of 11.1, replaced by {@link #derivatives(SpacecraftState)}
     */
    @Deprecated
    default double[] computeDerivatives(SpacecraftState s,  double[] pDot) {
        return null;
    }

    /** Compute the derivatives related to the additional state parameters.
     * @param s current state information: date, kinematics, attitude, and
     * additional states this equations depend on (according to the
     * {@link #yield(SpacecraftState) yield} method)
     * @return computed derivatives
     * @since 11.1
     */
    default double[] derivatives(SpacecraftState s) {
        // FIXME: as of 11.1 there is a default implementation that delegates
        // to computeDerivatives. This default implementation should be removed when
        // computeDerivatives is removed in 12.0
        final double[] pDot = new double[getDimension()];
        computeDerivatives(s, pDot);
        return pDot;
    }

}
