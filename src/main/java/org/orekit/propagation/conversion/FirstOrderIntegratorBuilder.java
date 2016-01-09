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
package org.orekit.propagation.conversion;

import org.apache.commons.math3.ode.AbstractIntegrator;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.Orbit;

/** This interface is the top-level abstraction to build first order integrators for propagators conversion.
 * @author Pascal Parraud
 * @since 6.0
 */
public interface FirstOrderIntegratorBuilder {

    /** Build a first order integrator.
     * @param orbit reference orbit
     * @return a first order integrator ready to use
     * @exception PropagationException if integrator cannot been built
     */
    AbstractIntegrator buildIntegrator(final Orbit orbit) throws PropagationException;

}
