/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.forces;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.TimeDerivativesEquationsWithJacobians;

/** This interface represents a parameterized force modifying spacecraft motion.
 *
 * <p> Objects implementing this interface are intended to be added, before the propagation is started,
 * to a {@link org.orekit.propagation.numerical.NumericalPropagatorWithJacobians numerical propagator}
 * in order to compute partial derivatives of orbital parameters with respect to the
 * {@link org.orekit.propagation.numerical.NumericalPropagatorWithJacobians#selectParameters(String[]) selected}
 * force model parameters.</p>
 *
 * @see ForceModel
 *
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public interface ForceModelWithJacobians extends Parameterizable, ForceModel {

    /** Compute the contribution of the force model to the perturbing
     * acceleration and to the jacobians.
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    void addContributionWithJacobians(SpacecraftState s, TimeDerivativesEquationsWithJacobians adder)
        throws OrekitException;

}
