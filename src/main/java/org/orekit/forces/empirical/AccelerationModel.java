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
package org.orekit.forces.empirical;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversProvider;

/** Acceleration model used by empirical force.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public interface AccelerationModel extends ParameterDriversProvider {

    /** Initialize the acceleration model at the start of the propagation.
     * <p>
     * The default implementation of this method does nothing
     * </p>
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
        // Nothing by default
    }

    /** Compute the signed amplitude of the acceleration.
     * <p>
     * The acceleration is the direction multiplied by the signed amplitude. So if
     * signed amplitude is negative, the acceleratin is towards the opposite of the
     * direction specified at construction.
     * </p>
     * @param state current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return norm of the acceleration
     */
    double signedAmplitude(SpacecraftState state, double[] parameters);

    /** Compute the signed amplitude of the acceleration.
     * <p>
     * The acceleration is the direction multiplied by the signed amplitude. So if
     * signed amplitude is negative, the acceleratin is towards the opposite of the
     * direction specified at construction.
     * </p>
     * @param state current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @param <T> type of the elements
     * @return norm of the acceleration
     */
    <T extends CalculusFieldElement<T>> T signedAmplitude(FieldSpacecraftState<T> state, T[] parameters);
}
