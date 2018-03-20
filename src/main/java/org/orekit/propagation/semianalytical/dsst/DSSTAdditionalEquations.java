/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;

public interface DSSTAdditionalEquations {

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    String getName();

    /** Compute the derivatives related to the additional state parameters.
     * <p>
     * When this method is called, the spacecraft state contains the main
     * state (orbit, attitude and mass), all the states provided through
     * the {@link org.orekit.propagation.AdditionalStateProvider additional
     * state providers} registered to the propagator, and the additional state
     * integrated using this equation. It does <em>not</em> contains any other
     * states to be integrated alongside during the same propagation.
     * </p>
     * @param <T> the type of the field elements
     * @param s current state information: date, kinematics, attitude, and
     * additional state
     * @param pDot placeholder where the derivatives of the additional parameters
     * should be put
     * @return cumulative effect of the equations on the main state (may be null if
     * equations do not change main state at all)
     * @exception OrekitException if some specific error occurs
     */

    <T extends RealFieldElement<T>> T[] computeDerivatives(FieldSpacecraftState<T> s,  T[] pDot) throws OrekitException;

}
