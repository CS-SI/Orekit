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

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

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
 * org.orekit.propagation.numerical.FieldNumericalPropagator numerical propagator}. Users provide the
 * equations as an implementation of this interface and register it to the propagator thanks to
 * its {@link org.orekit.propagation.numerical.FieldNumericalPropagator#addAdditionalEquations(FieldAdditionalEquations)}
 * method. Several such objects can be registered with each numerical propagator, but it is
 * recommended to gather in the same object the sets of parameters which equations can interact
 * on each others states.
 * </p>
 * <p>
 * The additional parameters are gathered in a simple p array. The additional equations compute
 * the pDot array, which is the time-derivative of the p array. Since the additional parameters
 * p may also have an influence on the equations of motion themselves that should be accumulated
 * to the main state derivatives (for example an equation linked to a complex thrust model may
 * induce an acceleration and a mass change), the {@link #computeDerivatives(FieldSpacecraftState,
 * CalculusFieldElement[]) computeDerivatives} method can return a double array that will be
 * <em>added</em> to the main state derivatives. This means these equations can be used as an
 * additional force model if needed. If the additional parameters have no influence at all on
 * the main spacecraft state, a null reference may be returned.
 * </p>
 * <p>
 * This interface is the numerical (read not already integrated) counterpart of
 * the {@link org.orekit.propagation.FieldAdditionalStateProvider} interface.
 * It allows to append various additional state parameters to any {@link
 * org.orekit.propagation.numerical.FieldNumericalPropagator numerical propagator}.
 * </p>
 * @see AbstractIntegratedPropagator
 * @see org.orekit.propagation.FieldAdditionalStateProvider
 * @author Luc Maisonobe
 * @deprecated as of 11.1, replaced by {@link FieldAdditionalDerivativesProvider}
 */
@Deprecated
public interface FieldAdditionalEquations<T extends CalculusFieldElement<T>> {

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    String getName();

    /**
     * Initialize the equations at the start of propagation.
     *
     * <p>
     * This method will be called once at propagation start,
     * before any calls to {@link #computeDerivatives(FieldSpacecraftState , CalculusFieldElement[])}.
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
    default void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
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
     */
    T[] computeDerivatives(FieldSpacecraftState<T> s,  T[] pDot);

}
