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
package org.orekit.propagation.analytical;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state.
 * <p>
 * This set of equations are automatically added to an {@link AbstractAnalyticalPropagator analytical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state are dimension 6 (orbit only).
 * </p>
 * @author Nicolas Fialton
 */
public class KeplerianPartialDerivativesEquations extends AbstractAnalyticalPartialDerivativesEquations {

    /** Propagator computing state evolution. */
    private final KeplerianPropagator propagator;

    /** Simple constructor.
     * <p>
     * Instance regrouping equations to compute derivatives.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public KeplerianPartialDerivativesEquations(final String name,
                                                final KeplerianPropagator propagator) {
        super(name, false);
        this.propagator  = propagator;
    }

    /** {@inheritDoc}. */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
                                               final double[][] dY1dY0) {

        // Check dimensions
        final int stateDim = dY1dY0.length;
        if (stateDim != 6 || stateDim != dY1dY0[0].length) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_6X6,
                                      stateDim, dY1dY0[0].length);
        }

        // store the matrices as a single dimension array
        setInitialized(true);
        final KeplerianJacobiansMapper mapper = getMapper();
        final double[] p = new double[mapper.getAdditionalStateDimension()];

        final double[][] dY1dP = new double[0][0];
        mapper.setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // set value in propagator
        return s1.addAdditionalState(getName(), p);
    }

    /** {@inheritDoc}. */
    public KeplerianJacobiansMapper getMapper() {
        if (!getInitialized()) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new KeplerianJacobiansMapper(getName(), propagator);
    }

}
