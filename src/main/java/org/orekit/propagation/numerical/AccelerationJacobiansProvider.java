/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.numerical;

import org.orekit.errors.OrekitException;
import org.orekit.forces.Parameterizable;
import org.orekit.propagation.SpacecraftState;

/**
 * Interface for computing acceleration jacobians, for the sake of {@link PartialDerivativesEquations
 * partial derivatives equations}.
 * @author Luc Maisonobe
 */
public interface AccelerationJacobiansProvider extends Parameterizable {

    /** Compute acceleration derivatives with respect to state parameters.
     * @param s spacecraft state
     * @param dAccdPos acceleration derivatives with respect to position
     * @param dAccdVel acceleration derivatives with respect to velocity
     * @param dAccdM acceleration derivatives with respect to mass (may be null when
     * the caller does not need the derivatives with respect to mass)
     * @exception OrekitException if derivatives cannot be computed
     */
    void addDAccDState(SpacecraftState s, double[][] dAccdPos, double[][] dAccdVel, double[] dAccdM)
        throws OrekitException;

    /** Compute acceleration derivatives with respect to additional parameters.
     * @param s spacecraft state
     * @param paramName name of the parameter with respect to which derivatives are required
     * @param dAccdParam acceleration derivatives with respect to specified parameters
     * @exception OrekitException if derivatives cannot be computed
     */
    void addDAccDParam(SpacecraftState s, String paramName, double[] dAccdParam)
        throws OrekitException;

}
