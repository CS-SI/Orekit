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
package org.orekit.propagation.numerical;

import java.util.Arrays;

import org.orekit.errors.PropagationException;
import org.orekit.orbits.EquinoctialOrbit;


/** This class sums up the contribution of several forces into
 *  orbit and mass derivatives and partial derivatives.
 * <p>
 * As of 5.0, this class is still considered experimental, so use it with care.
 * </p>
 * <p>
 * It is related to {@link NumericalPropagatorWithJacobians} like
 * {@link TimeDerivativesEquations} to {@link NumericalPropagator}.
 * </p>
 *
 * @see TimeDerivativesEquations
 * @see NumericalPropagator
 * @see NumericalPropagatorWithJacobians
 *
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class TimeDerivativesEquationsWithJacobians extends TimeDerivativesEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -1378236382967204061L;

    /** Reference to the orbital parameters jacobian array to initialize. */
    private double[][] storedDFDY;

    /** Reference to the force models parameters jacobian array to initialize. */
    private double[][] storedDFDP;

    /** Create a new instance.
     * @param orbit current orbit parameters
     */
    protected TimeDerivativesEquationsWithJacobians(final EquinoctialOrbit orbit) {
        super(orbit);
    }

    /** Initialize all derivatives and jacobians to zero.
     * @param yDot reference to the array where to put the derivatives.
     * @param dFdY placeholder array where to put the jacobian of the ODE with respect to the state vector
     * @param dFdP placeholder array where to put the jacobian of the ODE with respect to the parameters
     * @param orbit current orbit parameters
     * @exception PropagationException if the orbit evolve out of supported range
     */
    protected void initDerivativesWithJacobians(final double[] yDot,
                                                final double[][] dFdY,
                                                final double[][] dFdP,
                                                final EquinoctialOrbit orbit)
        throws PropagationException {

        initDerivatives(yDot, orbit);

        // store jacobians reference
        this.storedDFDY = dFdY;
        this.storedDFDP = dFdP;

        // initialize jacobians to zero
        for (final double[] row : storedDFDY) {
            Arrays.fill(row, 0.0);
        }

        for (final double[] row : storedDFDP) {
            Arrays.fill(row, 0.0);
        }

    }

}
