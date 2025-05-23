/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.FieldODEIntegrator;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.FieldAbsolutePVCoordinates;

/**
 * This interface is the top-level abstraction to build first order integrators for propagators conversion.
 *
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public interface FieldODEIntegratorBuilder<T extends CalculusFieldElement<T>> {

    /**
     * Build a first order integrator.
     *
     * @param field field to which the elements belong
     * @param orbit reference orbit
     * @param orbitType orbit type to use
     *
     * @return a first order integrator ready to use
     */
    FieldODEIntegrator<T> buildIntegrator(Field<T> field, Orbit orbit, OrbitType orbitType);

    /**
     * Build a first order integrator.
     *
     * @param field field
     * @param orbit reference orbit
     * @param orbitType orbit type to use
     * @param angleType position angle type to use
     *
     * @return a first order integrator ready to use
     * @since 13.0
     */
    FieldODEIntegrator<T> buildIntegrator(Field<T> field, Orbit orbit, OrbitType orbitType,
                                          PositionAngleType angleType);

    /**
     * Build a first order integrator.
     *
     * @param orbit reference orbit
     * @param orbitType orbit type to use
     *
     * @return a first order integrator ready to use
     */
    FieldODEIntegrator<T> buildIntegrator(FieldOrbit<T> orbit, OrbitType orbitType);

    /**
     * Build a first order integrator. Non-orbit version.
     *
     * @param fieldAbsolutePVCoordinates absolute position-velocity
     *
     * @return a first order integrator ready to use
     * @since 12.2
     */
    FieldODEIntegrator<T> buildIntegrator(FieldAbsolutePVCoordinates<T> fieldAbsolutePVCoordinates);

    /**
     * Form a non-Field equivalent.
     * @return ODE integrator builder
     * @since 13.0
     */
    ODEIntegratorBuilder toODEIntegratorBuilder();
}
