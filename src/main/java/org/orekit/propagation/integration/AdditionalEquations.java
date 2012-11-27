/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;

/** This interface allows users to add their own differential equations to a numerical propagator.
 *
 * <p>
 * In some cases users may need to integrate some problem-specific equations along with
 * classical spacecraft equations of motions. One example is optimal control in low
 * thrust where adjoint parameters linked to the minimized hamiltonian must be integrated.
 * Another example is formation flying or rendez-vous which use the Clohessy-Whiltshire
 * equations for the relative motion.
 * </p>
 * <p>
 * This interface allows users to add such equations to a {@link NumericalPropagator numerical
 * propagator}. Users provide the equations as an implementation of this interface and register
 * it to the propagator thanks to its {@link
 * NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. Several such objects
 * can be registered with each numerical propagator, but it is recommended to gather in the same
 * object the sets of parameters which equations can interact on each others states.
 * </p>
 * <p>
 * The additional parameters are gathered in a simple p array. The additional equations compute
 * the pDot array, which is the time-derivative of the p array. Since the additional parameters
 * p may also have an influence on the equations of motion themselves (for example an equation
 * linked to a complex thrust model may induce an acceleration and a mass change), the same
 * {@link TimeDerivativesEquations time derivatives equations adder} already shared by all force
 * models to add their contributions is also provided to the additional equations implementation
 * object. This means these equations can be used as an additional force model if needed. If the
 * additional parameters have no influence at all on the spacecraft state, this adder can
 * simply be ignored.
 * </p>
 * <p>
 * This interface is the numerical (read not already integrated) counterpart of
 * the {@link org.orekit.propagation.analytical.AdditionalStateProvider} interface.
 * It allows to append various additional state parameters to any {@link
 * NumericalPropagator numerical propagator}.
 * </p>
 * @see NumericalPropagator
 * @see org.orekit.propagation.analytical.AdditionalStateProvider
 * @author Luc Maisonobe
 */
public interface AdditionalEquations {

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    String getName();

    /** Compute the derivatives related to the additional state parameters.
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution of the additional parameters
     * p to the orbit evolution (accelerations, mass time-derivative) should be added
     * @param p current value of the additional parameters
     * @param pDot placeholder where the derivatives of the additional parameters
     * should be put
     * @exception OrekitException if some specific error occurs
     */
    void computeDerivatives(SpacecraftState s, TimeDerivativesEquations adder,
                            double[] p, double[] pDot) throws OrekitException;

}
