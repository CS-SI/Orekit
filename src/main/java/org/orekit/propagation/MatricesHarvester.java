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

import org.hipparchus.linear.RealMatrix;

/** Interface for extracting State Transition Matrices and Jacobians matrices from {@link SpacecraftState spacecraft state}.
 * <p>
 * The State Transition Matrix and Jacobians matrices with respect to propagation parameters are stored in the state
 * as {@link SpacecraftState#getAdditionalState(String) additional states}. Each propagator and support classes has
 * its own way to handle it. The interface leverages these differences which are implementation details and provides
 * a higher level access to these matrices, regardless of haw they were computed and stored.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 */
public interface MatricesHarvester {

    /** Extract state transition matrix from state.
     * @param state spacecraft state
     * @return state transition matrix, with semantics consistent with propagation
     * {@link org.orekit.orbits.OrbitType orbit type}.
     */
    RealMatrix getStateTransitionMatrix(SpacecraftState state);

    /** Get the Jacobian with respect to propagation parameters.
     * @param state spacecraft state
     * @return Jacobian with respect to propagation parameters, or null
     * if there are no parameters
     */
    RealMatrix getParametersJacobian(SpacecraftState state);

}