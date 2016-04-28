/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/** Interface representing the Jacobian of a vector function depending on {@link SpacecraftState}.
 * @see EstimationUtils#differentiate(StateFunction, int, org.orekit.orbits.OrbitType, org.orekit.orbits.PositionAngle, double, int)
 * @see StateFunction
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface StateJacobian {

    /** Evaluate the Jacobian of the function.
     * @param state spacecraft state as the sole free parameter of the function.
     * @return Jacobian matric
     * @throws OrekitException if evaluation cannot be performed
     */
    double[][] value(SpacecraftState state) throws OrekitException;

}
