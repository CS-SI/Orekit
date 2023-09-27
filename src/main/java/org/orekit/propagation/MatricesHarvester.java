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
package org.orekit.propagation;

import java.util.List;

import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;

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

    /** Set up reference state.
     * <p>
     * This method is called whenever the global propagation reference state changes.
     * This corresponds to the start of propagation in batch least squares orbit determination
     * or at prediction step for each measurement in Kalman filtering. Its goal is to allow
     * the harvester to compute some internal data. Analytical models like TLE use it to
     * compute analytical derivatives, semi-analytical models like DSST use it to compute
     * short periodic terms, numerical models do not use it at all.
     * </p>
     * @param reference reference state to set
     */
    void setReferenceState(SpacecraftState reference);

    /** Extract state transition matrix from state.
     * @param state spacecraft state
     * @return state transition matrix, with semantics consistent with propagation,
     * or null if no state transition matrix is available
     * {@link org.orekit.orbits.OrbitType orbit type}.
     */
    RealMatrix getStateTransitionMatrix(SpacecraftState state);

    /** Get the Jacobian with respect to propagation parameters.
     * @param state spacecraft state
     * @return Jacobian with respect to propagation parameters, or null
     * if there are no parameters
     */
    RealMatrix getParametersJacobian(SpacecraftState state);

    /** Get the names of the parameters in the matrix returned by {@link #getParametersJacobian}.
     * <p>
     * Beware that the names of the parameters are fully known only once all force models have
     * been set up and their parameters properly selected. Applications that retrieve the matrices
     * harvester first and select the force model parameters to retrieve afterwards (but obviously
     * before starting propagation) must take care to wait until the parameters have been set up
     * before they call this method. Calling the method too early would return wrong results.
     * </p>
     * <p>
     * The names are returned in the Jacobians matrix columns order
     * </p>
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     */
    List<String> getJacobiansColumnsNames();

    /**
     * Get the orbit type used for the matrix computation.
     * @return the orbit type used for the matrix computation
     */
    OrbitType getOrbitType();

    /**
     * Get the position angle used for the matrix computation.
     * <p>
     * Irrelevant if {@link #getOrbitType()} returns {@link OrbitType#CARTESIAN}.
     * </p>
     * @return the position angle used for the matrix computation
     */
    PositionAngleType getPositionAngleType();

}
