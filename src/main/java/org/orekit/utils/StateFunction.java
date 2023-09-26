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
package org.orekit.utils;

import org.orekit.propagation.SpacecraftState;

/** Interface representing a vector function depending on {@link SpacecraftState}.
 * @see Differentiation#differentiate(StateFunction, int, org.orekit.attitudes.AttitudeProvider,
 * org.orekit.orbits.OrbitType, org.orekit.orbits.PositionAngleType, double, int)
 * @see StateJacobian
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface StateFunction {

    /** Evaluate the function.
     * @param state spacecraft state as the sole free parameter of the function.
     * @return vector value of the function
     */
    double[] value(SpacecraftState state);

}
