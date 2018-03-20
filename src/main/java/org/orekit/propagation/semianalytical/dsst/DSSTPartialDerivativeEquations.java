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

import java.util.Map;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class DSSTPartialDerivativeEquations implements DSSTAdditionalEquations {

    /** Propagator computing state evolution. */
    private final DSSTPropagator propagator;

    /** Selected parameters for Jacobian computation. */
    private ParameterDriversList selected;

    /** Parameters map. */
    private Map<ParameterDriver, Integer> map;

    /** Name. */
    private final String name;

    /** Flag for Jacobian matrices initialization. */
    private boolean initialized;

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     * @exception OrekitException if a set of equations with the same name is already present
     */
    public DSSTPartialDerivativeEquations(final String name, final DSSTPropagator propagator) throws OrekitException {
        this.name                   = name;
        this.selected               = null;
        this.map                    = null;
        this.propagator             = propagator;
        this.initialized            = false;

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T extends RealFieldElement<T>> T[] computeDerivatives(final FieldSpacecraftState<T> s, final T[] pDot)
        throws OrekitException {
        return null;
    }

}
